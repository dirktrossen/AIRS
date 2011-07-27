/*
Copyright (C) 2011, Dirk Trossen, nors@dirk-trossen.de

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
package com.android.airs.handlers;

import java.util.concurrent.Semaphore;

import com.android.airs.helper.SerialPortLogger;
import com.android.airs.platform.SensorRepository;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;

/**
 * @author trossen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class EventButtonHandler implements Handler
{
	private Context nors;
	private int Event = 0;
	private Semaphore event_semaphore 	= new Semaphore(1);
	private byte[] readings = new byte[6];
	private Vibrator vibrator;

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
	 Function    : Acquire()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public byte[] Acquire(String sensor, String query)
	{
		long[] pattern = {0l, 450l, 250l, 450l, 250l, 450l};
		// battery level?
		if(sensor.compareTo("EB") == 0)
		{
			wait(event_semaphore); // block until semaphore available -> fired

			if (Event == 1)
			{
				readings[0] = (byte)sensor.charAt(0);
				readings[1] = (byte)sensor.charAt(1);
				readings[2] = (byte)((Event>>24) & 0xff);
				readings[3] = (byte)((Event>>16) & 0xff);
				readings[4] = (byte)((Event>>8) & 0xff);
				readings[5] = (byte)(Event & 0xff);
				Event = 0;

				// vibrate with pattern
				vibrator.vibrate(pattern, -1);
		        
				return readings;
			}
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
		SensorRepository.insertSensor(new String("EB"), new String("boolean"), new String("Event button widget"), new String("int"), 0, 0, 1, 0, this);	    
	}
	
	public EventButtonHandler(Context nors)
	{
		this.nors = nors;
		try
		{
			// charge the semaphores to block at next call!
			wait(event_semaphore); 

			// get system service for Vibrator
			vibrator = (Vibrator)nors.getSystemService(Context.VIBRATOR_SERVICE);
			
			// check intents and set booleans for discovery
			IntentFilter intentFilter = new IntentFilter("com.android.nors.event_button");
	        nors.registerReceiver(SystemReceiver, intentFilter);
		}
		catch(Exception e)
		{
			SerialPortLogger.debugForced("Semaphore!!!!");
		}
	}
	
	public void destroyHandler()
	{
		nors.unregisterReceiver(SystemReceiver);
	}
		
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // When battery changed...
            if (action.equals("com.android.nors.event_button")) 
            {
            	Event = 1;
				event_semaphore.release();		// release semaphore
				return;
            }
        }
    };
}

