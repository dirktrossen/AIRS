/*
Copyright (C) 2011, Dirk Trossen, airs@dirk-trossen.de

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

import java.util.concurrent.Semaphore;

import com.airs.helper.SerialPortLogger;
import com.airs.platform.SensorRepository;

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
	private String Event, old_Event;
	private Semaphore event_semaphore 	= new Semaphore(1);
	private Vibrator vibrator;
	private boolean registered = false;

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
		StringBuffer readings;

		// event button?
		if(sensor.compareTo("EB") == 0)
		{
			// not yet registered -> then do so!!
			if (registered == false)
			{
				// check intents and set booleans for discovery
				IntentFilter intentFilter = new IntentFilter("com.airs.eventbutton");
		        nors.registerReceiver(SystemReceiver, intentFilter);
		        intentFilter = new IntentFilter("com.airs.eventselected");
		        nors.registerReceiver(SystemReceiver, intentFilter);
		        registered = true;
			}

			wait(event_semaphore); // block until semaphore available -> fired

			if (Event != null)
			{
				// prepare readings
				readings = new StringBuffer(sensor);
				readings.append(Event);
					
				// vibrate with pattern
				vibrator.vibrate(pattern, -1);
				
				// store mood
				old_Event = null;
				old_Event = new String(Event);
				
				// garbage collect mood string
				Event = null;
		        
				return readings.toString().getBytes();
			}
			else 
				return null;
		}
		
		return null;
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
		if (old_Event != null)
			return "My last event was " + old_Event + "!";
		else
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
		SensorRepository.insertSensor(new String("EB"), new String("Event"), new String("Event button widget"), new String("str"), 0, 0, 1, 0, this);	    
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

            // if mood button widget has been pressed -> start Activity for selecting mood icon
            if (action.equals("com.airs.eventbutton")) 
            {
    			try
    			{
    	            Intent startintent = new Intent(nors, EventButton_selector.class);
    	            startintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	            nors.startActivity(startintent);          
    			}
    			catch(Exception e)
    			{
    			}
    			return;
            }

            // If event button pressed, signal to Acquire()
            if (action.equals("com.airs.eventselected")) 
            {
            	// get mood from intent
            	Event = intent.getStringExtra("Event");

            	event_semaphore.release();		// release semaphore
				return;
            }
        }
    };
}

