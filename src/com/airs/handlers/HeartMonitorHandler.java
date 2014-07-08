/*
Copyright (C) 2013, TecVis LP, support@tecvis.co.uk

This program is free software; you can redistribute it and/or modify it
under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation as version 2.1 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this library; if not, write to the Free Software Foundation, Inc.,
59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
*/
package com.airs.handlers;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import com.airs.R;
import com.airs.helper.SerialPortLogger;
import com.airs.platform.HandlerManager;
import com.airs.platform.History;
import com.airs.platform.Sensor;
import com.airs.platform.SensorRepository;

/**
 * Class to read Zephyr HxM related sensors, specifically the HL, HP, HI sensor
 * @see Handler
 */
public class HeartMonitorHandler implements Handler, Runnable
{  
	// HMX variables
//	private static final int HMX_STX 		= 0;
//	private static final int HMX_MSG_ID 	= 1;
//	private static final int HMX_DLC 		= 2;
	private static final int HMX_BATTERY	= 8;
	private static final int HMX_PULSE		= 9;
//	private static final int HMX_DISTANCE	= 47;
	private static final int HMX_INSTANCE	= 49;
//	private static final int HMX_STRIDE		= 51;

	// initialize old sensor values with float of zero
	private int last_battery 		= 0;
	private int last_sent_battery 	= 0;
	private int last_pulse 			= 0; 
	private int last_instance		= 0; 
	
	private int time_window = 1;
			
	// We will only have one instance of connection
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mBtAdapter;
    private BluetoothDevice device;
    private BluetoothSocket mmSocket;
	private InputStream inputStream;
	private String BTAddress;
	
	// context for history
	private Context airs;
	
	// indicator for connectivity
	private boolean connected = false, tried = false, use_monitor = true, shutdown = false;
	private Semaphore pulse_semaphore 		= new Semaphore(1);	
	private Semaphore battery_semaphore 	= new Semaphore(1);	
	private Semaphore instance_semaphore 	= new Semaphore(1);	
	
	//`ACC data being read (max)
	private byte[] reading = null;
	private Thread runnable;
	
	private void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}

	private void wait(Semaphore sema)
	{
		try
		{
			sema.acquire();
		}
		catch(Exception e)
		{
		}
	}

	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's arming the semaphore
	 * @param airs Reference to the calling {@link android.content.Context}
	 */
	public HeartMonitorHandler(Context airs)
	{
		// save for later
		this.airs = airs;
		
		// should handler be disabled?
		if (HandlerManager.readRMS_b("HeartMonitorHandler::BTON", false) == false)
		{
			debug("HeartMonitorHandler::BT OFF");
			use_monitor = false;
		}    
		
		// arm semaphores
		wait(pulse_semaphore); 
		wait(battery_semaphore); 
		wait(instance_semaphore); 
	}
	
	/**
	 * Method to release all handler resources
	 * Here, we release all handler semaphores as well as close the BT socket, closing also the BT read thread
	 * @see com.airs.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		// signal shutdown
		shutdown = true;
		
		// release all semaphores for unlocking the Acquire() threads
		pulse_semaphore.release();
		pulse_semaphore.release();
		instance_semaphore.release();
		
  		if (inputStream != null)
		{
			try
			{
				inputStream.close();	
			}
			catch(Exception e)
			{
			}
		}
   		
  		if (mmSocket != null)
		{
			try
			{
				mmSocket.close();	
			}
			catch(Exception e)
			{
			}
		}
  		
  		connected = false;
	}
	
	/**
	 * Method to acquire sensor data
	 * Here, we first try to connect to the Zephyr for an initial connection (but only once!)
	 * then, it's a simple wait for the semaphores to be released
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.airs.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	synchronized public byte[] Acquire(String sensor, String query)
	{	
		// are we shutting down?
		if (shutdown == true)
			return null;
		
		// if not connected, try now!
		if (connected == false)
		{
			// if we haven't tried before, do now (so that we only do once!)
			if (tried == false)
			{
				// open port and create serial reading thread
				if (ComPortInit() == true) 
				{
				    connected = true;
					runnable = new Thread(this);
					runnable.start();
				}
				else 
				{
					debug("HeartMonitorHandler::ComPort initialization failed");
					use_monitor = false;
					// invalidate all sensors since we couldn't connect -> this will gracefully terminate any acquisition thread
					SensorRepository.setSensorStatus("HL", Sensor.SENSOR_INVALID, "Could not connect to HxM", Thread.currentThread());
					SensorRepository.setSensorStatus("HP", Sensor.SENSOR_INVALID, "Could not connect to HxM", Thread.currentThread());
					SensorRepository.setSensorStatus("HI", Sensor.SENSOR_INVALID, "Could not connect to HxM", Thread.currentThread());
					
					return null;
				}
				
				// we've tried!
				tried = true;
			}
			else
				return null;
		}
		
		// garbage collect the old stuff
		reading = null;
		
		// acquire data and send out
		try
		{			
			switch(sensor.charAt(1))
			{
				case 'L' :
					// block until semaphore available
					wait(battery_semaphore); 

					if (last_sent_battery != last_battery)
					{
						reading = new byte[6];
						reading[0] = (byte)sensor.charAt(0);
						reading[1] = (byte)sensor.charAt(1);
						reading[2] = (byte)0;
						reading[3] = (byte)0;
						reading[4] = (byte)0;
						reading[5] = (byte)(last_battery & 0xff);
						last_sent_battery = last_battery;
					}
					break;
				case 'P' :
					// block until semaphore available
					wait(pulse_semaphore); 

					reading = new byte[6];
					reading[0] = (byte)sensor.charAt(0);
					reading[1] = (byte)sensor.charAt(1);
					reading[2] = (byte)0;
					reading[3] = (byte)0;
					reading[4] = (byte)0;
					reading[5] = (byte)(last_pulse & 0xff);
					break;
				case 'I' :
					// block until semaphore available
					wait(instance_semaphore); 

					reading = new byte[6];
					reading[0] = (byte)sensor.charAt(0);
					reading[1] = (byte)sensor.charAt(1);
					reading[2] = (byte)0;
					reading[3] = (byte)0;
					reading[4] = (byte)0;
					reading[5] = (byte)(last_instance & 0xff);
					break;
				default:
					// indicate finished reading
					return null;
			}
		}
		catch (Exception e) 
		{
			debug("HeartMonitorHandler:Acquire: Exception: " + e.toString());
		}
		
		// return readings
		return reading;
	}

	/**
	 * Method to share the last value of the given sensor
	 * @param sensor String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.airs.handlers.Handler#Share(java.lang.String)
	 */
	public String Share(String sensor)
	{		
		switch(sensor.charAt(1))
		{
			case 'P' : 		    
				return "My current pulse is " + String.valueOf(last_pulse);
			case 'I' :
				return "My current instant speed is " + String.valueOf((double)last_instance/10.0f);
			case 'L' : 		    
				return "My current battery is " + String.valueOf(last_battery);
		}
		
		return null;		
	}
	
	/**
	 * Method to view historical chart of the given sensor symbol - supporting timeline for heart rate and instant speed
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.airs.handlers.Handler#History(java.lang.String)
	 */
	public void History(String sensor)
	{
		switch(sensor.charAt(1))
		{
		case 'P':
			History.timelineView(airs, "Heart rate [bpm]", "HP");
			break;
		case 'I':
			History.timelineView(airs, "Instant Speed [m/s]", "HI");
			break;
		}		
	}

	/**
	 * Method to discover the sensor symbols support by this handler
	 * As the result of the discovery, appropriate {@link com.airs.platform.Sensor} entries will be added to the {@link com.airs.platform.SensorRepository}, if the monitor is selected by the user to be used
	 * This does not mean that the sensor has been found via BT! 
	 * @see com.airs.handlers.Handler#Discover()
	 * @see com.airs.platform.Sensor
	 * @see com.airs.platform.SensorRepository
	 */
	public void Discover()
	{
		if (use_monitor == true)
		{
			SensorRepository.insertSensor(new String("HL"), new String("%"), airs.getString(R.string.HL_d), airs.getString(R.string.HL_e), new String("int"), 0, 0, 100, true, 10000, this);
			SensorRepository.insertSensor(new String("HP"), new String("bpm"), airs.getString(R.string.HP_d), airs.getString(R.string.HP_e), new String("int"), 0, 0, 200, true, 1000, this);
			SensorRepository.insertSensor(new String("HI"), new String("m/s"), airs.getString(R.string.HI_d), airs.getString(R.string.HI_e), new String("int"), -1, 0, 200, true, 1000, this);
		}
	}

	/**
	 * Initialize the serial port with parameter of COM_PORT and BAUDRATE
	 * @return success or not
	 */
	private boolean ComPortInit() 
	{
		// this phone version read the BT address through the RMS entry and connects to it
		try 
		{
			BTAddress = null;

	        // Get the local Bluetooth adapter
	        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	        
	        // if there's no BT adapter, return without putting sensors in repository
	        if (mBtAdapter == null) 
	        	return false;

			time_window = HandlerManager.readRMS_i("HeartMonitorHandler::Timewindow", 5);

			// read BT address from RMS
			BTAddress = HandlerManager.readRMS("HeartMonitorHandler::BTStore", "00:07:80:5A:3E:7E");

			// now get remote device for connection
			device = mBtAdapter.getRemoteDevice(BTAddress);
			
			// this is how it should be done with proper pairing 
			mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
			
			// now connect
            mmSocket.connect();  

			if (mmSocket != null)
				inputStream = mmSocket.getInputStream();
			else
				return false;
		} 
		catch (Exception e) 
		{
			debug("ComPort initialization failed!");
			return false;
		}

		if (mmSocket != null && inputStream != null) 
			debug("HeartMonitorTalker::ComPortInit:ComPort initialized");
		else
			return false;
		

		return true;
	}


	private byte readfromBT()
	{
		int BT = -1;
		
		// read as long as we can't get anything useful
		do
		{
			try
			{
				do
				{
					// read single byte
					BT = inputStream.read();
				}while(BT == -1);				
			}
			catch(Exception e)
			{
				try
				{
					mmSocket.close();
					inputStream.close();
				}
				catch(Exception e2)
				{
					
				}
				mmSocket = null;
				inputStream = null;
			}
			
			// if input stream does not exist anymore, try to reconnect
			if (inputStream == null)
			{
				try
				{
					// wait until reconnection
					Thread.sleep(15000);
					
					// now get remote device for connection
					device = mBtAdapter.getRemoteDevice(BTAddress);
					
					// this is how it should be done with proper pairing 
					mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
					
					// now connect
		            mmSocket.connect();  
	
					if (mmSocket != null)
						inputStream = mmSocket.getInputStream();
					else
						mmSocket.close();
				}
				catch(Exception e)
				{
					mmSocket = null;
					inputStream = null;
				}
			}
		}while (BT==-1);
		
		return (byte)BT;
	}
	/**
	 * 
	 * This thread keeps on reading the BT serial port  
	*/
	public void run() 
	{
		int i;
		byte header;
		byte endOfMessage;
		byte[] payload = null;
		int payload_length;
		int averaging = 0;
		int battery=0, pulse=0, instance=0;
		int current_instance;
		
		if (inputStream==null)
		{
			debug("No input stream: halt pump");
			return;
		}
				
		while (connected == true) 
		{		
			// try to read from serial port
			try 
			{
			    // read the header information
                do 
                {
                    header = readfromBT(); 
                    // check for first byte (0x02) and message ID (0x26)
                    if (header == (byte)0x02)
                    {
                        header = readfromBT(); 

                        if (header == (byte)0x26) 
                        {
                            header = readfromBT(); 

                            // extract payload length
                        	payload_length = header;
                        	if (payload_length != 0)
                        		break;
                        }
                    }
                }while(true);
				
                // now generate payload packet
                payload = new byte[payload_length];
                // read rest of header
				for (i=0;i<payload_length;i++)
                    payload[i] = readfromBT();
			    	
		    	// read checksum
				readfromBT(); 

		    	// read end of message
                endOfMessage = readfromBT(); 

                // proper end of message?
                if (endOfMessage == (byte)0x03)
                {	
                	int current_pulse = (int)(payload[HMX_PULSE] & 0xff);
                	
                	// filter too low and too high values
                	if (current_pulse > 30 & current_pulse<230)
                	{
	                	// now read values
	                	battery 	+= (int)(payload[HMX_BATTERY] & 0xff);
	                	pulse 		+= current_pulse;
	                	current_instance	= (int)(payload[HMX_INSTANCE] & 0xff) | (int)(payload[HMX_INSTANCE+1] & 0xff)<<8;
	                	instance    += (int)Math.floor((double)current_instance/25.6f);             	
	        			// now increase averaging window
	        			averaging++;	        			
                	}
                }                				    	   	
			} 
			catch (Exception e) 
			{
				debug("HeartMonitorHandler::Failed to read serial data: " + e.toString());
				
				// invalidate all sensors since reading failed -> this will gracefully terminate any acquisition thread
				SensorRepository.setSensorStatus("HL", Sensor.SENSOR_INVALID, "HxM disconnected", null);
				SensorRepository.setSensorStatus("HP", Sensor.SENSOR_INVALID, "HxM disconnected", null);
				SensorRepository.setSensorStatus("HI", Sensor.SENSOR_INVALID, "HxM disconnected", null);

				// release semaphores for picking up the values
		    	pulse_semaphore.release();
		    	battery_semaphore.release();
		    	instance_semaphore.release();

				return;
			}	
			
			// reached averaging window length?
			if (averaging == time_window)
			{
				// determine average values
				last_battery  = battery/averaging;
				last_pulse    = pulse/averaging;
				last_instance = instance/averaging;
				
				// reset counters
				averaging = 0;
				battery = pulse = instance = 0;

				// release semaphores for picking up the values
		    	pulse_semaphore.release();
		    	battery_semaphore.release();
		    	instance_semaphore.release();
			}
		}
	}
}
