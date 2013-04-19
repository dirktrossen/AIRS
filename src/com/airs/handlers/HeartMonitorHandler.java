/*
Copyright (C) 2008-2013, Dirk Trossen, airs@dirk-trossen.de

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

import com.airs.helper.SerialPortLogger;
import com.airs.helper.Waker;
import com.airs.platform.HandlerManager;
import com.airs.platform.History;
import com.airs.platform.Sensor;
import com.airs.platform.SensorRepository;

public class HeartMonitorHandler implements Handler, Runnable
{  
	// HMX variables
	public static final int HMX_STX 		= 0;
	public static final int HMX_MSG_ID 		= 1;
	public static final int HMX_DLC 		= 2;
	public static final int HMX_BATTERY		= 8;
	public static final int HMX_PULSE		= 9;
	public static final int HMX_DISTANCE	= 47;
	public static final int HMX_INSTANCE	= 49;
	public static final int HMX_STRIDE		= 51;

	// initialize old sensor values with float of zero
	private int last_battery 		= 0;
	private int last_sent_battery 	= 0;
	private int last_pulse 			= 0; 
	private int last_instance		= 0; 
	
	private int time_window = 1;
			
	// We will only have one instance of connection
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mBtAdapter;
    private BluetoothSocket mmSocket;
	private InputStream inputStream;

	// context for history
	private Context airs;
	
	// indicator for connectivity
	private boolean connected = false, tried = false, use_monitor = true;
	private Semaphore pulse_semaphore 		= new Semaphore(1);	
	private Semaphore battery_semaphore 	= new Semaphore(1);	
	private Semaphore instance_semaphore 	= new Semaphore(1);	
	
	//`ACC data being read (max)
	private byte[] reading = null;
	private Thread runnable;
	
	protected void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}

	/**
	 * Sleep function 
	 * @param millis
	 */
	protected void sleep(long millis) 
	{
		Waker.sleep(millis);
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

	/***********************************************************************
	 Function    : HeartMonitorHandler()
	 Input       : 
	 Output      :
	 Return      :
	 Description : 	reads RMS and check if version 1.0 is wanted or not
	***********************************************************************/
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
	
	public void destroyHandler()
	{
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
	
	/***********************************************************************
	 Function    : Acquire()
	 Input       : 
	 Output      :
	 Return      :
	 Description : 	actual Acquisition function, reading sensors and waiting for
	                result 
	***********************************************************************/
	synchronized public byte[] Acquire(String sensor, String query)
	{	
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

	/***********************************************************************
	 Function    : Share()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
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
	
	/***********************************************************************
	 Function    : History()
	 Input       : sensor input for specific history views
	 Output      :
	 Return      :
	 Description : calls historical views
	***********************************************************************/
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

	/***********************************************************************
	 Function    : Discover()
	 Input       : 
	 Output      : string with discovery information
	 Return      : 
	 Description : provides discovery information of this particular acquisition 
	 			   module, hardcoded 
	***********************************************************************/
	public void Discover()
	{
		if (use_monitor == true)
		{
			SensorRepository.insertSensor(new String("HL"), new String("%"), new String("Device battery level"), new String("int"), 0, 0, 100, true, 10000, this);
			SensorRepository.insertSensor(new String("HP"), new String("bpm"), new String("Pulse"), new String("int"), 0, 0, 200, true, 1000, this);
			SensorRepository.insertSensor(new String("HI"), new String("m/s"), new String("Instant Speed"), new String("int"), -1, 0, 200, true, 1000, this);
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
			String BTAddress = null;

	        // Get the local Bluetooth adapter
	        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	        
	        // if there's no BT adapter, return without putting sensors in repository
	        if (mBtAdapter == null) 
	        	return false;

			time_window = HandlerManager.readRMS_i("HeartMonitorHandler::Timewindow", 5);

			// read BT address from RMS
			BTAddress = HandlerManager.readRMS("HeartMonitorHandler::BTStore", "00:07:80:5A:3E:7E");

			// now get remote device for connection
			BluetoothDevice device = mBtAdapter.getRemoteDevice(BTAddress);
			
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


	/**
	 * 
	 * This thread keeps on reading the BT serial port  
	*/
	public void run() 
	{
		int i;
		byte[] header = new byte[3];
		byte[] checksum = new byte[1];
		byte[] endOfMessage = new byte[1];
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
                    inputStream.read(header, 0, 1);
                    // check for first byte (0x02) and message ID (0x26)
                    if (header[0] == (byte)0x02)
                    {
                        inputStream.read(header, 0, 1);

                        if (header[0] == (byte)0x26) 
                        {
                            inputStream.read(header, 0, 1);

                            // extract payload length
                        	payload_length = header[0];
                        	if (payload_length != 0)
                        		break;
                        }
                    }
                }while(true);
				
                // now generate payload packet
                payload = new byte[payload_length];
                // read rest of header
				for (i=0;i<payload_length;i++)
                    inputStream.read(payload, i, 1);
			    	
		    	// read checksum
                inputStream.read(checksum, 0, 1);

		    	// read end of message
                inputStream.read(endOfMessage, 0, 1);
                // proper end of message?
                if (endOfMessage[0] == (byte)0x03)
                {	
                	int current_pulse = (int)payload[HMX_PULSE];
                	
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
