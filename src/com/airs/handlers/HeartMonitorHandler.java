/*
Copyright (C) 2008-2011, Dirk Trossen, airs@dirk-trossen.de

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

import java.io.IOException;
import java.io.InputStream;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import com.airs.helper.SerialPortLogger;
import com.airs.platform.HandlerManager;
import com.airs.platform.SensorRepository;

public class HeartMonitorHandler implements Handler, Runnable
{  
	// initialize old sensor values with float of zero
	private int last_battery 	= 0;
	private int last_pulse 		= 0; 
	private int last_accX  		= 0; 
	private int last_accY 		= 0; 
	private int last_accZ 		= 0;
	private int last_button		= 0;
	private int last_sent_battery= 0;

	// header lengths
	private final static int HEADER_LENGTH 			= 6;
	private final static int DATA_HEADER_LENGTH 	= 5;
	
	// data lengths
	private final static int ECG_MAX 	= 300;
	private final static int ECG_MIN	= 150;
	private final static int ACC_MAX 	= 225;
	
	// samples length for DataProcessor
	private int sample_ECG, sample_ACC;

	// indicator if it is 3D accelerometer data
	private boolean ACC_3D = false;
	
	// pulse determination variables
    private static final int PEAK_THRESHOLD = 70;
    private int startVoltage = 1000;
    private int lastVoltage = 1000;
    private int lastPeak = -1;
    private int peakToPeakSamples = 0;
    private boolean rememberPeaks = false;
    private boolean overThreshold = false;
    private int currentSample = ECG_MAX;
	
	// We will only have one instance of connection
//    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter mBtAdapter;
    private BluetoothSocket mmSocket;
	private InputStream inputStream;

	// indicator for connectivity
	private boolean connected = false;
	
	// the next two booleans are used for read/write coordination between Acquire() and DataProcessor()
	// indicator for reading values
	private boolean read_values = false; 
	// indicator for processing
	private boolean write_values = false; 

	// header fields
	private byte Stream_Header[]  = new byte[HEADER_LENGTH];
	private byte Data_Header[]    = new byte[DATA_HEADER_LENGTH];
	
	// ECG data being read (max) as double buffer
	private byte ECGDataIn[][] = new byte[2][ECG_MAX];
	private int  currentECG_buffer = 0;
	private int  currentECG_index = 0;
	private boolean ECG_overflown = false;
	
	//`ACC data being read (max)
	private byte ACCDataIn[]  = new byte[ACC_MAX];
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
		try 
		{
			Thread.sleep(millis);
		} 
		catch (InterruptedException ignore) 
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
	public HeartMonitorHandler(Context nors)
	{
		// open port and create serial reading thread
		if (ComPortInit() == true) 
		{
		    connected = true;
			runnable = new Thread(this);
			runnable.start();
		}
		else 
			debug("Can not initialize com with heartmonitor;");
	}
	
	public void destroyHandler()
	{
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
		String Accreadings = null;
		int i;
	
		// if not connected, return null pointer
		if (connected == false)
			return null;

		// writing values at the moment -> wait!
		while (write_values == true)
			sleep(100);
		
		// now indicate reading values
		read_values = true;
		
		// garbage collect the old stuff
		reading = null;
		
		// acquire data and send out
		try
		{			
			switch(sensor.charAt(1))
			{
				case 'A' :
					Accreadings = new String(sensor);
					Accreadings = Accreadings.concat(Double.toString(last_accX) + ":" + Double.toString(last_accY) + ":" + Float.toString(last_accZ));
					reading = Accreadings.getBytes();
					Accreadings = null;
					break;
				case 'B' :
					// only sent if different
					if (last_button == 1)
					{
						reading = new byte[6];
						reading[0] = (byte)sensor.charAt(0);
						reading[1] = (byte)sensor.charAt(1);					
						reading[2] = (byte)0;
						reading[3] = (byte)0;
						reading[4] = (byte)0;
						reading[5] = (byte)(last_button & 0xff);
						last_button = 0;
					}
					break;
				case 'L' :
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
					reading = new byte[6];
					reading[0] = (byte)sensor.charAt(0);
					reading[1] = (byte)sensor.charAt(1);
					reading[2] = (byte)0;
					reading[3] = (byte)0;
					reading[4] = (byte)0;
					reading[5] = (byte)(last_pulse & 0xff);
					break;
				case 'X' :
					reading = new byte[6];
					reading[0] = (byte)sensor.charAt(0);
					reading[1] = (byte)sensor.charAt(1);
					reading[2] = (byte)((last_accX>>24) & 0xff);
					reading[3] = (byte)((last_accX>>16) & 0xff);
					reading[4] = (byte)((last_accX>>8) & 0xff);
					reading[5] = (byte)(last_accX & 0xff);
					break;
				case 'Y' :
					reading = new byte[6];
					reading[0] = (byte)sensor.charAt(0);
					reading[1] = (byte)sensor.charAt(1);
					reading[2] = (byte)((last_accY>>24) & 0xff);
					reading[3] = (byte)((last_accY>>16) & 0xff);
					reading[4] = (byte)((last_accY>>8) & 0xff);
					reading[5] = (byte)(last_accY & 0xff);
					break;
				case 'Z' :
					reading = new byte[6];
					reading[0] = (byte)sensor.charAt(0);
					reading[1] = (byte)sensor.charAt(1);
					reading[2] = (byte)((last_accZ>>24) & 0xff);
					reading[3] = (byte)((last_accZ>>16) & 0xff);
					reading[4] = (byte)((last_accZ>>8) & 0xff);
					reading[5] = (byte)(last_accZ & 0xff);
					break;
				case 'E':
					reading = new byte[2+ECG_MAX];
					reading[0] = (byte)sensor.charAt(0);
					reading[1] = (byte)sensor.charAt(1);
					for (i=0;i<ECG_MAX;i++)
						reading[i+2] = ECGDataIn[1-currentECG_buffer][i];
					break;
				default:
					// indicate finished reading
					read_values = false;
					return null;
			}
		}
		catch (Exception e) 
		{
			debug("HeartMonitorHandler:Acquire: Exception: " + e.toString());
		}
		
		// indicate finished reading
		read_values = false;

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
	public synchronized String Share(String sensor)
	{		
		switch(sensor.charAt(1))
		{
			case 'T' :
			case 'N' :
				return null;
		}
		
		return null;		
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
		if (connected == true)
		{
			SensorRepository.insertSensor(new String("HB"), new String(""), new String("event button"), new String("int"), 0, 0, 1, 1000, this);
			SensorRepository.insertSensor(new String("HL"), new String("%"), new String("battery level"), new String("int"), 0, 0, 100, 10000, this);
			SensorRepository.insertSensor(new String("HE"), new String("[]"), new String("ECG"), new String("arr"), 0, 0, 255, 1000, this);
			SensorRepository.insertSensor(new String("HP"), new String("beat"), new String("Pulse"), new String("int"), 0, 0, 200, 2500, this);
			SensorRepository.insertSensor(new String("HX"), new String("g"), new String("Accelerometer X"), new String("int"), -2, -500, 500, 1000, this);
			SensorRepository.insertSensor(new String("HY"), new String("g"), new String("Accelerometer Y"), new String("int"), -2, -500, 500, 1000, this);
			SensorRepository.insertSensor(new String("HZ"), new String("g"), new String("Accelerometer Z"), new String("int"), -2, -500, 500, 1000, this);
			SensorRepository.insertSensor(new String("HA"), new String("g"), new String("Accel.  (X,Y,Z)"), new String("str"), 0, -500, 500, 1000, this);
		}
	}

	/**
	 * Initialize the serial port with parameter of COM_PORT and BAUDRATE
	 * @return success or not
	 */
	private boolean ComPortInit() 
	{
		// Setup your own port you want to listen and write here;
		// this phone version read the BT address through the RMS entry and connects to it
		try 
		{
			String BTAddress = null;

			// should handler be disabled?
			if (HandlerManager.readRMS_b("HeartMonitorHandler::BTON", false) == false)
			{
				debug("HeartMonitorHandler::BT OFF");
			    return false;
			}    

	        // Get the local Bluetooth adapter
	        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	        
	        // if there's no BT adapter, return without putting sensors in repository
	        if (mBtAdapter == null) 
	        	return false;

			// read BT address from RMS
			BTAddress = HandlerManager.readRMS("HeartMonitorHandler::BTStore", "00:14:C5:A1:01:EC");

			BluetoothDevice device = mBtAdapter.getRemoteDevice(BTAddress);

			// use the unofficial method createInsecureRfcommSocket() to avoid the PIN pairing dialog with the AliveTec 
			java.lang.reflect.Method  m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
			mmSocket = (BluetoothSocket)m.invoke(device, 1);
			
			// this is how it should be done with proper pairing -> does not work with AliveTec!
//			mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
			// now connect
            mmSocket.connect();            

			if (mmSocket != null)
				inputStream = mmSocket.getInputStream();
			else
				return false;
		} 
		catch (Exception e) 
		{
			SerialPortLogger.debugUI("ComPort initialization failed", 2000);
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
		int i, j, data_length;
		byte checksum[] = new byte[1];
		byte ECG_value[] = new byte[1];
		int missing;
		
		if (inputStream==null)
		{
			debug("No input stream: halt pump");
			return;
		}
				
		while (true) 
		{
			// try to read from serial port
			try 
			{
			    // read the total header information
                // START PACKET HEADER
                // find sync bytes (1. + 2. byte)
                do {
                    inputStream.read(Stream_Header, 0, 1);
                    // check for first byte (0)
                    if (Stream_Header[0] == (byte)0) 
                    {
                        inputStream.read(Stream_Header, 1, 1);
                        // check for second byte (254)
                        if (Stream_Header[1] == (byte)0xfe) 
                            break;
                        else
                            continue;
                    } 
                    else
                        continue;
                }while(true);
				
                // read rest of header
				for (i=2;i<HEADER_LENGTH;i++)
                    inputStream.read(Stream_Header, i, 1);

				// values read currently? -> wait since we will manipulate common buffers!
				while (read_values == true)
					sleep(100);
				
				// now indicate writing
				write_values = true;

				// read number of data blocks given in header
				for (j=0;j<(int)Stream_Header[5];j++)
				{
				    // read the ECG header information
					for (i=0;i<DATA_HEADER_LENGTH;i++)
	                    inputStream.read(Data_Header, i, 1);

					// length of data from data header of the packet minus the length of the data header
					data_length = ((int)(Data_Header[1] & 0xFF) << 8) | ((int)Data_Header[2] & 0xFF);
					data_length -= DATA_HEADER_LENGTH;
					
					switch((int)Data_Header[0] & 0xff)
					{
					// ECG?
					case 0xaa:					    		
			    		sample_ECG = data_length;

						// how many samples/s?
						if (Data_Header[3] == (byte)0x02)
							currentSample = ECG_MAX;
						else
							currentSample = ECG_MIN;
						
						// does the reading still fill into the double buffer?
						if (sample_ECG+currentECG_index<ECG_MAX)
						{
							for (i=0;i<sample_ECG;i++, currentECG_index++)
							{
								// read value
			                    inputStream.read(ECG_value, 0, 1);
			                    // and store in buffer
			                    ECGDataIn[currentECG_buffer][currentECG_index] = ECG_value[0];
							}
							// ECG has not overflown
							ECG_overflown = false;
						}
						else // reading does not fit into one buffer, need to fill other one too
						{
							try
							{
							// signal the ECG has overflown in buffer
							ECG_overflown = true;
							// read until first buffer is full
							missing = ECG_MAX - currentECG_index;
							for (i=0;i<missing;i++, currentECG_index++)
							{
								// read value
			                    inputStream.read(ECG_value, 0, 1);
			                    // and store in buffer
			                    ECGDataIn[currentECG_buffer][currentECG_index] = ECG_value[0];
							}	
							// start from buffer beginning
							currentECG_index = 0;
							// switch buffer
							currentECG_buffer = 1-currentECG_buffer;
							// and read missing bytes
							for (i=0;i<sample_ECG - missing;i++, currentECG_index++)
							{
								// read value
			                    inputStream.read(ECG_value, 0, 1);
			                    // and store in buffer
			                    ECGDataIn[currentECG_buffer][currentECG_index] = ECG_value[0];
							}	
							}catch(Exception e)
							{
								SerialPortLogger.debugForced("Something's going wrong in index");
							}
						}
				    	break;
				    // ACC 2D?
					case 0x55:
						ACC_3D = false;
			    		sample_ACC = data_length;
					
						for (i=0;i<sample_ACC;i++)
		                    inputStream.read(ACCDataIn, i, 1);
						break;
					case 0x56:
						ACC_3D = true;
			    		sample_ACC = data_length;
			    		
						for (i=0;i<sample_ACC;i++)
		                    inputStream.read(ACCDataIn, i, 1);
						break;
					default:
						throw new IOException("run:wrong data header ID:");
					}						
				}
			    	
		    	// read checksum
                inputStream.read(checksum, 0, 1);
					    	   	
				// process payload
				try
				{
				    DataProcessor();
				}
				catch (Exception e) 
				{
					debug("BytesPumper: Error in decomposing = " + e.toString());
				}
			} 
			catch (Exception e) 
			{
				debug("HeartMonitorHandler::Failed to read serial data: " + e.toString());
				return;
			}	
			
			// now indicate that writing is finished
			write_values = false;
		}
	}

	// analyze the data for all commands
	private void DataProcessor() 
	{		
		// read battery value from stream header (0...200)
		last_battery = (int)(Stream_Header[2] & 0xff) / 2;
		// determine status of event button
		if ((int)(Stream_Header[3] & 0x10) != 0)
			last_button = 1;
		
		// determine pulse rate from ECG 
		Determine_Pulse();
		
		// determine accelerometer data
		Determine_Acc();
		
		// now indicate finished writing
		write_values = false;
	}
	
	// Determine pulse rate by looking for peaks in ECG
	private void Determine_Pulse()
	{
		int i, voltage, ECG_index;
		
		// has the last input overflown the buffer -> use last buffer then
		if (ECG_overflown == true)
			ECG_index = 1-currentECG_buffer;
		else
			return;	// otherwise return until current buffer will have overflown
//			ECG_index = currentECG_buffer;
		
		for (i=0;i<currentSample;i++)
		{
			// read voltage
			voltage = (int)ECGDataIn[ECG_index][i];
			
			// peak?
	        if((lastVoltage > voltage) && overThreshold) 
	        {
	            if(lastPeak != -1) 
	            {
	                double heartbeatA = 60D * ((double)currentSample/peakToPeakSamples);
	                peakToPeakSamples = 0;
	                last_pulse = (int)heartbeatA;
	            }
	            lastPeak = lastVoltage;
	            rememberPeaks = true;
	            overThreshold = false;
	        }
	        
	        if(voltage <= lastVoltage) 
	            startVoltage = voltage;
	        else 
	        	if((voltage - startVoltage) > PEAK_THRESHOLD) 
	        		overThreshold = true;
	        
	        lastVoltage = voltage;
	        if(rememberPeaks)
	            peakToPeakSamples++;
		}
	}
	
	// Determine acc
	private void Determine_Acc()
	{
		int i, values;
        double acceleX;
        double acceleY;
        double acceleZ;
        int accX = 0 , accY = 0, accZ = 0;
        double step;

        if (ACC_3D == false)
        {
        	step = (double)4 / (double)255;
        	values = 2;
        }
        else
        {
        	step = (double)5.4 / (double)255;
        	values = 3;
        }
        
        // add up all values
        for (i=0;i<sample_ACC;i+=values)
        {
        	accX += ACCDataIn[i];
        	accY += ACCDataIn[i+1];
        	if (values == 3)
        		accZ += ACCDataIn[i+2];
        }      
        
        // average value in samples
        accX /= sample_ACC/values;
        accY /= sample_ACC/values;
        accZ /= sample_ACC/values;
       
        // compute acceleration, measured in "g""
    	acceleX = (double)accX * step; 
    	acceleY = (double)accY * step; 
    	acceleZ = (double)accZ * step;    
    	
    	last_accX = (int)(acceleX * 100);
    	last_accY = (int)(acceleY * 100);
    	last_accZ = (int)(acceleZ * 100);
	}
}
