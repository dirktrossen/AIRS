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

import android.content.Context;
import android.media.*;

public class AudioHandler implements Handler
{
	Context airs;
	// beacon data
	private byte [] AS_reading;
	private byte [] AF_reading;
	// configuration data
	private int polltime = 5000;
	private final int CENTRE_POINT = 32768;
	
	// audio data
	private long    frequency = -1;
	private long    level = -1;
	private int		sample_rate = 8000;
	private int		AA_adjust = 3;
	
	// availability of player
	private boolean available = false;
	private boolean havePlayer = false;

	// Player for audio capture
	private AudioRecord p;
	private short[] output = null;

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

	/***********************************************************************
	 Function    : Acquire()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public synchronized byte[] Acquire(String sensor, String query)
	{
		// acquire data and send out
		try
		{
			switch(sensor.charAt(1))
			{
				case 'S' :
					Amplitude(sensor);
					return AS_reading;
				case 'F' :
					Frequency(sensor);
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
	
	/***********************************************************************
	 Function    : Share()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : return humand readable sharing string
	***********************************************************************/
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
			case 'S' :
				History.timelineView(airs, "Sound Pressure Level [dB]", "AS");
				break;
			default:
				History.timelineView(airs, "Frequency [Hz]", "AF");
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
		// return right away if no player could be created in constructor!
		if (available == false)
			return;
		
		// here some midlet property check as to whether or not audio capture is supported
		SensorRepository.insertSensor(new String("AF"), new String("Hz"), new String("Estimated Freq."), new String("int"), 0, 0, 15000, true, polltime, this);
		SensorRepository.insertSensor(new String("AS"), new String("dB"), new String("Sound Pressure Level"), new String("int"), -2, 0, 1200, true, polltime, this);
	}
	
	public AudioHandler(Context nors)
	{
		// store for later
		airs = nors;
		
		// now read polltime for audio sampling
		polltime = HandlerManager.readRMS_i("AudioHandler::samplingpoll", 5) * 1000;

		// now read frequency for sampling
		sample_rate = HandlerManager.readRMS_i("AudioHandler::SamplingRate", 8000);

		// now read adjustment for AA reading
		AA_adjust = HandlerManager.readRMS_i("AudioHandler::AA_adjust", 3);

		// start player and see if it's there!
		p = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, sample_rate * 4);
		if (p != null)
			if (p.getState() == AudioRecord.STATE_INITIALIZED)
			{
				available = true;	
				// reserve memory for recorded output
				output = new short[sample_rate];
				// release player again until needed
				p.release();
				p = null;
			}
	}
	
	public void destroyHandler()
	{
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
	synchronized private void Amplitude(String sensor)
	{
		long level_new = 0, ampl;
		double level_d;
		int i, recorded, read;
				
		// if somebody else is having the player resource, wait until finished
		while (havePlayer == true)
			sleep(100);
		// now we have the player!
		havePlayer = true;
		
		// remove last reading
		AS_reading = null;
		
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
					
					// try to read from AudioRecord player until sample_rate buffer is full!
				    i = 0;
				    while (i < sample_rate)
				    {
				    	read = sample_rate/1000;
				    	if (read>sample_rate-i)
				    		read = sample_rate-i;
				    	recorded = p.read(output, i, read);
				    	// error in reading?
				    	if (recorded == AudioRecord.ERROR_INVALID_OPERATION)
				    	{
				    		debug("AudioHandler:invalid operation!");
				    		AS_reading = null;
				    		havePlayer = false;
				    		return;
				    	}
				    	i += recorded;
				    }
				    // stop and release player again
				    p.stop();
				    p.release();
				    p = null;
				    
			    	level_new = 0;
			    	// count changes in sign
			    	for (i=0;i<output.length;i++)
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
			    		 
					// did anything change in the position?
					if (level_new != level)
					{
						// take positioning info and place in reading field
						AS_reading = new byte[4 + 2];
						AS_reading[0] = (byte)sensor.charAt(0);
						AS_reading[1] = (byte)sensor.charAt(1);
						AS_reading[2] = (byte)((level_new>>24) & 0xff);
						AS_reading[3] = (byte)((level_new>>16) & 0xff);
						AS_reading[4] = (byte)((level_new>>8) & 0xff);
						AS_reading[5] = (byte)(level_new & 0xff);
						// now remember old frequency
						level = level_new;
					}
					else
						AS_reading = null;		
				}
		}
		catch(Exception e)
		{
    		debug("AudioHandler:Exception when requesting AudioRecord!");
			AS_reading = null;
		}	
		
		// don't need player anymore
		havePlayer = false;
	}

	// do frequency determination of sample
	synchronized private void Frequency(String sensor)
	{
		long frequency_new = 0;
		int i, recorded, read, level=0;
		boolean sign = false;
		
		// if somebody else is having the player resource, wait until finished
		while (havePlayer == true)
			sleep(100);
		
		havePlayer = true;
		
		// remove last reading
		AF_reading = null;
		
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
					
					// try to read from AudioRecord player until sample_rate buffer is full!
				    i = 0;
				    while (i < sample_rate)
				    {
				    	read = sample_rate/1000;
				    	if (read>sample_rate-i)
				    		read = sample_rate-i;
				    	recorded = p.read(output, i, read);
				    	// error in reading?
				    	if (recorded == AudioRecord.ERROR_INVALID_OPERATION)
				    	{
				    		debug("AudioHandler:invalid operation!");
				    		AF_reading = null;
				    		havePlayer = false;
				    		return;
				    	}
				    	i += recorded;
				    }
				    // stop and release player again
				    p.stop();
				    p.release();

				    // count changes in sign
			     	for (i=0;i<output.length;i++)
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
			    	level /= output.length;
			    	  	
			    	// now adjust frequency to sample rate
			    	frequency_new = ((frequency_new * (long)sample_rate)/(long)output.length)/2;
			    	// volume too low?
			    	if (level<10)
			    		frequency_new = 0;
					// did anything change in the frequency?
					if (frequency_new != frequency)
					{
						// take frequency info and place in reading field
						AF_reading = new byte[4 + 2];
						AF_reading[0] = (byte)sensor.charAt(0);
						AF_reading[1] = (byte)sensor.charAt(1);
						AF_reading[2] = (byte)((frequency_new>>24) & 0xff);
						AF_reading[3] = (byte)((frequency_new>>16) & 0xff);
						AF_reading[4] = (byte)((frequency_new>>8) & 0xff);
						AF_reading[5] = (byte)(frequency_new & 0xff);
						// now remember old frequency
						frequency = frequency_new;
					}
					else
						AF_reading = null;		
				}
		}
		catch(Exception e)
		{
    		debug("AudioHandler:Exception when requesting AudioRecord!");
			AF_reading = null;
		}
		
		// don't need player anymore
		havePlayer = false;
	}
}
