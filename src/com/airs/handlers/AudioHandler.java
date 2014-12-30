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

import com.airs.helper.SerialPortLogger;
import com.airs.helper.Waker;
import com.airs.platform.HandlerManager;
import com.airs.platform.SensorRepository;
import com.airs.platform.History;

import com.airs.*;
import android.content.Context;
import android.media.*;

/** 
 * Class to read audio-related sensors, specifically the AS and AF sensor
 * @see Handler
 */
public class AudioHandler implements Handler
{
	private final int SIZE_SHORT = Short.SIZE/8;
	private Context airs;
	// beacon data
	private byte [] AS_reading = new byte[6];
	private byte [] AF_reading = new byte[6];
	// configuration data
	private int polltime = 5000;
	private final int CENTRE_POINT = 32768;
	
	// audio data
	private long    level = -1;
	private int		sample_rate = 8000;
	private int 	bufferSize;
	private int		AA_adjust = 3;
	
	// availability of player
	private boolean available = false;
	private boolean havePlayer = false;
	private boolean shutdown = false;
	
	// Player for audio capture
	private AudioRecord p;
	private short[] output = null;

	private void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}
	
	/**
	 * Sleep function 
	 * @param millis
	 */
	private void sleep(long millis) 
	{
		Waker.sleep(millis);
	}

	/**
	 * Method to acquire sensor data
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.airs.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public synchronized byte[] Acquire(String sensor, String query)
	{
		// are we shutting down?
		if (shutdown == true)
			return null;

		// acquire data and send out
		try
		{
			switch(sensor.charAt(1))
			{
				case 'S' :
					if (Amplitude(sensor) == true)
						return AS_reading;
				case 'F' :
					if (Frequency(sensor) == true)
						return AF_reading;
				default:
					return null;
			}
		}
		catch (Exception e) 
		{
			debug("AudioHandler::Acquire: Exception: " + e.toString());
			return null;
		}
	}
	
	/**
	 * Method to share the last value of the given sensor
	 * @param sensor String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.airs.handlers.Handler#Share(java.lang.String)
	 */
	public String Share(String sensor)
	{
		// acquire data and send out
		try
		{
			switch(sensor.charAt(1))
			{
				case 'S' :
					return "My current ambient sound level is at "+String.valueOf((double)level / 100) + " dB";
				default:
					return null;
			}
		}
		catch (Exception e) 
		{
			debug("AudioHandler::Acquire: Exception: " + e.toString());
			return null;
		}
	}
	
	/**
	 * Method to view historical chart of the given sensor symbol
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.airs.handlers.Handler#History(java.lang.String)
	 */
	public void History(String sensor)
	{
		switch(sensor.charAt(1))
		{
			case 'S' :
				History.timelineView(airs, "Sound Pressure Level [dB]", "AS");
				break;
			default:
				History.timelineView(airs, "Frequency [Hz]", "AF");
				break;
		}
	}
	
	/**
	 * Method to discover the sensor symbols support by this handler
	 * As the result of the discovery, appropriate {@link com.airs.platform.Sensor} entries will be added to the {@link com.airs.platform.SensorRepository}
	 * @see com.airs.handlers.Handler#Discover()
	 * @see com.airs.platform.Sensor
	 * @see com.airs.platform.SensorRepository
	 */
	public void Discover()
	{
		// return right away if no player could be created in constructor!
		if (available == false)
			return;
		
		// here some midlet property check as to whether or not audio capture is supported
		SensorRepository.insertSensor(new String("AF"), new String("Hz"), airs.getString(R.string.AF_d), airs.getString(R.string.AF_e), new String("int"), 0, 0, 15000, true, polltime, this);
		SensorRepository.insertSensor(new String("AS"), new String("dB"), airs.getString(R.string.AS_d), airs.getString(R.string.AS_e), new String("int"), -2, 0, 1200, true, polltime, this);
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, reading the various RMS values of the preferences
	 * Then, determining the minimal buffer size for the recording
	 * Then, creating an AudioPlayer just to see if it works (tear it down again right after creation)
	 * @param airs Reference to the calling {@link android.content.Context}
	 */
	public AudioHandler(Context airs)
	{
		boolean buffer_error = false;
		
		// store for later
		this.airs = airs;
		
		// now read polltime for audio sampling
		polltime = HandlerManager.readRMS_i("AudioHandler::samplingpoll", 5) * 1000;

		// now read frequency for sampling
		sample_rate = HandlerManager.readRMS_i("AudioHandler::SamplingRate", 8000);

		// now read adjustment for AA reading
		AA_adjust = HandlerManager.readRMS_i("AudioHandler::AA_adjust", 3);

		// determine required buffer size
		bufferSize = AudioRecord.getMinBufferSize(sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
				
		if (bufferSize<sample_rate * SIZE_SHORT)
			bufferSize = sample_rate * SIZE_SHORT;
		
		// start player and see if it's there!
		do
		{
			try
			{
			p = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
			if (p != null)
				if (p.getState() == AudioRecord.STATE_INITIALIZED)
				{
					available = true;	
					// reserve memory for recorded output
					output = new short[sample_rate];
					// release player again until needed
					p.release();
					p = null;
					buffer_error = false;
				}
			}
			catch(Exception e)
			{
				buffer_error = true;
				// increase buffer size by 10%
				bufferSize += bufferSize/10;
			}
		}while(buffer_error == true);
	}
	
	/**
	 * Method to release all handler resources
	 * Here, we stop any playing AudioPlayer and release its resources
	 * @see com.airs.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		// we are shutting down!
		shutdown = true;
		
		try
		{
			if (p != null)
			{
				if (p.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
					p.stop();
				p.release();
				p = null;
			}
		}
		catch(Exception e)
		{
		}
	}
	
	// do level determination of sample
	synchronized private boolean Amplitude(String sensor)
	{
		long level_new = 0, ampl;
		double level_d;
		int i, offset = 0, recorded, read;
		boolean returnValue = false;
				
		// if somebody else is having the player resource, wait until finished
		while (havePlayer == true)
			sleep(100);
		// now we have the player!
		havePlayer = true;

		// now record
		try
		{	
			// get player resources
			p = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
			if (p != null)
				if (p.getState() == AudioRecord.STATE_INITIALIZED)
				{
					// are we shutting down?
					if (shutdown == true)
						return false;
					
					// start recording
					p.startRecording();
					
					// try to read from AudioRecord player until sample_rate buffer is full, i.e., one second of data!
			    	level_new = 0;			    	
			    	// try to always read the buffer size we gave originally
			    	read = sample_rate;
			    	// read until nothing is left to be read
				    while (read>0)
				    {
				    	// now read
				    	recorded = p.read(output, offset, read);
				    	// error in reading?
				    	if (recorded == AudioRecord.ERROR_INVALID_OPERATION)
				    	{
				    		debug("AudioHandler:invalid operation!");
				    		havePlayer = false;
				    		p.stop();
				    		p.release();
				    		p = null;
				    		return false;
				    	}
				    	else
				    	{

					    	// now substract the already recorded bytes
					    	read -= recorded;
					    	// compute offset for next batch
					    	offset += recorded;
				    	}
				    }
				    // stop and release player again
				    p.stop();
				    p.release();
				    p = null;
				    
			    	// count changes in sign
			    	for (i=0;i<sample_rate;i++)
			    	{
			    		// given that the PCM encoding delivers unsigned SHORTS, we need to convert the amplitude around the 32768 centre point!
//			    		ampl = (long)(CENTRE_POINT-Math.abs(output[i]));		    		
			    		ampl = (long)Math.abs(output[i]);
			    		
			    		// now ampl*ampl for field force
		    			level_new += ampl * ampl;
			    	}
			    	
			    	// compute decibel through 10*log10 (A1^2/A0^2)
			    	level_d = 10 * Math.log10((double)level_new / (double)output.length) + (double)AA_adjust;
			    	level_new = (long)(level_d*100);		    	
			    		 
					// take positioning info and place in reading field
					AS_reading[0] = (byte)sensor.charAt(0);
					AS_reading[1] = (byte)sensor.charAt(1);
					AS_reading[2] = (byte)((level_new>>24) & 0xff);
					AS_reading[3] = (byte)((level_new>>16) & 0xff);
					AS_reading[4] = (byte)((level_new>>8) & 0xff);
					AS_reading[5] = (byte)(level_new & 0xff);
					
					returnValue = true;
				}
		}
		catch(Exception e)
		{
    		debug("AudioHandler:Exception when requesting AudioRecord!");
		}	
		
		// don't need player anymore
		havePlayer = false;
		
		// nothing happened when we got here
		return returnValue;
	}

	// do frequency determination of sample
	synchronized private boolean Frequency(String sensor)
	{
		long frequency_new = 0;
		int i, recorded, read, offset = 0, level=0;
		boolean sign = false;
		boolean returnValue = false;

		// if somebody else is having the player resource, wait until finished
		while (havePlayer == true)
			sleep(100);
		
		havePlayer = true;
		
		// now record
		try
		{
			// get player resources
			p = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, sample_rate * 4);
			if (p != null)
				if (p.getState() == AudioRecord.STATE_INITIALIZED)
				{
					// start recording
					p.startRecording();
					
					// are we shutting down?
					if (shutdown == true)
						return false;

			    	// try to always read the buffer size we gave originally
			    	read = sample_rate;
			    	// read until nothing is left to be read
				    while (read>0)
				    {
				    	// now read
				    	recorded = p.read(output, offset, read);
				    	// error in reading?
				    	if (recorded == AudioRecord.ERROR_INVALID_OPERATION)
				    	{
				    		debug("AudioHandler:invalid operation!");
				    		havePlayer = false;
				    		return false;
				    	}
				    	else
				    	{
					    	// now substract the already recorded bytes
					    	read -= recorded;
					    	// compute offset for next batch
					    	offset += recorded;
				    	}
				    }
				    // stop and release player again
				    p.stop();
				    p.release();
				    p = null;

				    // count changes in sign
			     	for (i=0;i<sample_rate;i++)
			    	{
			    		if (output[i]<0 && sign == true)
			    		{	
			    			frequency_new++;
			    			sign = false;
			    		}
			    		else
				    		if (output[i]>=0 && sign == false)
				    		{	
				    			frequency_new++;
				    			sign = true;
				    		}
			    		// count amplitude level
		    			level += CENTRE_POINT-(long)(Math.abs((int)output[i]));			    	
			    	}
				    // now determine level
			    	level /= sample_rate;
			    	  	
			    	// volume too low?
			    	if (level<10)
			    		frequency_new = 0;
			    	else
				    	// now determine frequency by half of all counted spikes
				    	frequency_new /= 2;

			    	// take frequency info and place in reading field
					AF_reading = new byte[4 + 2];
					AF_reading[0] = (byte)sensor.charAt(0);
					AF_reading[1] = (byte)sensor.charAt(1);
					AF_reading[2] = (byte)((frequency_new>>24) & 0xff);
					AF_reading[3] = (byte)((frequency_new>>16) & 0xff);
					AF_reading[4] = (byte)((frequency_new>>8) & 0xff);
					AF_reading[5] = (byte)(frequency_new & 0xff);
					
					returnValue = true;
				}
		}
		catch(Exception e)
		{
    		debug("AudioHandler:Exception when requesting AudioRecord!");
		}
		
		// don't need player anymore
		havePlayer = false;
		
		return returnValue;
	}
}
