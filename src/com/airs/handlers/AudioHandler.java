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
import com.airs.platform.HandlerManager;
import com.airs.platform.SensorRepository;

import android.content.Context;
import android.media.*;

public class AudioHandler implements Handler
{
	// beacon data
	private byte [] AA_reading;
	private byte [] AF_reading;
	// configuration data
	private int polltime = 5000;
	private final int CENTRE_POINT = 32768;
	
	// audio data
	private long    frequency = -1;
	private long    level = -1;
	private int		sample_rate = 8000;
	
	// availability of player
	private boolean available = false;
	private boolean havePlayer = false;

	// Player for audio capture
	private AudioRecord p;
	private short[] output = null;
	
	// constants for log10 calculation

	// Log10 constant 
	private double LOG10 = 2.302585092994045684;
	// ln(0.5) constant
	private double LOGdiv2 = -0.6931471805599453094;


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

	// log helper functions  
	private double _log(double x)
	  {
		  double f=0.0, y1, y2, y, k;
		  int appendix=0, i;
	    
		  if(!(x>0.))
			  return Double.NaN;

		  while(x>0.0 && x<=1.0)
		  {
			  x*=2.0;
			  appendix++;
		  }
		  x/=2.0;
		  appendix--;

		  y1=x-1.;
		  y2=x+1.;
		  y=y1/y2;
		  k=y;
		  y2=k*y;

		  for(i=1; i<50; i+=2)
		  {
		      f+=k/i;
		      k*=y2;
		  }

		  f*=2.0;
		  for(i=0; i<appendix; i++)
			  f+=LOGdiv2;
		  
		  return f;
	  }
   
	private double log10(double x)
	   {
	     if(!(x>0.))
	       return Double.NaN;
	     //
	     if(x==1.0)
	       return 0.0;
	     // Argument of _log must be (0; 1]
	     if (x>1.)
	     {
	       x=1/x;
	       return -_log(x)/LOG10;
	     };
	     //
	     return _log(x)/LOG10;
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
				case 'A' :
					Amplitude(sensor);
					return AA_reading;
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
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public synchronized String Share(String sensor)
	{
		// acquire data and send out
		try
		{
			switch(sensor.charAt(1))
			{
				case 'A' :
					return "My current ambient sound level is at "+String.valueOf((double)level / 100) + " dBm";
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
		SensorRepository.insertSensor(new String("AF"), new String("Hz"), new String("Estimated Freq."), new String("int"), 0, 0, 15000, polltime, this);
		SensorRepository.insertSensor(new String("AA"), new String("dBm"), new String("Avg Amplitude"), new String("int"), -2, -2100, 0, polltime, this);
	}
	
	public AudioHandler(Context nors)
	{
		// now read polltime for audio sampling
		polltime = HandlerManager.readRMS_i("AudioHandler::samplingpoll", 5) * 1000;

		// now read frequency for sampling
		sample_rate = HandlerManager.readRMS_i("AudioHandler::SamplingRate", 8000);

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
			}
	}
	
	public void destroyHandler()
	{
		if (p != null)
		{
			p.stop();
			p.release();
			p = null;
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
		AA_reading = null;
		
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
				    		AA_reading = null;
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
			    		ampl = (long)(CENTRE_POINT-Math.abs(output[i]));
			    		// now ampl*ampl for field force
		    			level_new += ampl * ampl;
			    	}
			    	
			    	// compute decible through 10*log10 (A1^2/A0^2)
			    	level_d = 10 * log10(((double)level_new / (double)output.length)/(double)(CENTRE_POINT * CENTRE_POINT));
			    	level_new = (long)(level_d*100);		    	
			    		 
					// did anything change in the position?
					if (level_new != level)
					{
						// take positioning info and place in reading field
						AA_reading = new byte[4 + 2];
						AA_reading[0] = (byte)sensor.charAt(0);
						AA_reading[1] = (byte)sensor.charAt(1);
						AA_reading[2] = (byte)((level_new>>24) & 0xff);
						AA_reading[3] = (byte)((level_new>>16) & 0xff);
						AA_reading[4] = (byte)((level_new>>8) & 0xff);
						AA_reading[5] = (byte)(level_new & 0xff);
						// now remember old frequency
						level = level_new;
					}
					else
						AA_reading = null;		
				}
		}
		catch(Exception e)
		{
    		debug("AudioHandler:Exception when requesting AudioRecord!");
			AA_reading = null;
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
