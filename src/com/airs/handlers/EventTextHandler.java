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

import java.util.concurrent.Semaphore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.airs.R;
import com.airs.helper.SerialPortLogger;
import com.airs.platform.SensorRepository;

/** 
 * Class to read text sharing sensor, specifically the ET sensor
 * @see Handler
 */
public class EventTextHandler implements com.airs.handlers.Handler
{
	private static final int SHOW_TOAST 	= 1;

	private Context airs;
	private String Event, old_Event;
	private Semaphore event_semaphore 	= new Semaphore(1);
	private boolean registered = false, shutdown = false;
	
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
	 * Method to acquire sensor data
	 * Here, we register the broadcast receiver to receive the shared text notification, if not done before
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.airs.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public byte[] Acquire(String sensor, String query)
	{
		StringBuffer readings;

		// are we shutting down?
		if (shutdown == true)
			return null;

		// event button?
		if(sensor.compareTo("ET") == 0)
		{
			// not yet registered -> then do so!!
			if (registered == false)
			{
				// check intents and set booleans for discovery
				IntentFilter intentFilter = new IntentFilter("com.airs.eventtext");
		        airs.registerReceiver(SystemReceiver, intentFilter);
		        registered = true;
			}
			
			// block until text input is available -> fired
			wait(event_semaphore); 

			// anything delivered?
			if (Event != null)
			{
				// show toast message to user
		        mHandler.sendMessage(mHandler.obtainMessage(SHOW_TOAST));	 

				// prepare readings
				readings = new StringBuffer(sensor);
				readings.append(Event);
					
				// store shared text
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
	
	/**
	 * Method to share the last value of the given sensor
	 * @param sensor String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.airs.handlers.Handler#Share(java.lang.String)
	 */
	public String Share(String sensor)
	{		
		if (old_Event != null)
			return "My last event text was " + old_Event + "!";
		else
			return null;
	}

	/**
	 * Method to view historical chart of the given sensor symbol - doing nothing in this handler
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.airs.handlers.Handler#History(java.lang.String)
	 */
	public void History(String sensor)
	{
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
		SensorRepository.insertSensor(new String("ET"), new String("Event"), airs.getString(R.string.ET_d), airs.getString(R.string.ET_e), new String("str"), 0, 0, 1, false, 0, this);	    
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's only arming the semaphore
	 * @param airs Reference to the calling {@link android.content.Context}
	 */
	public EventTextHandler(Context airs)
	{
		this.airs = airs;

		try
		{
			// charge the semaphores to block at next call!
			wait(event_semaphore); 
		}
		catch(Exception e)
		{
			SerialPortLogger.debugForced("Semaphore!!!!");
		}
	}
	
	/**
	 * Method to release all handler resources
	 * Here, we unregister the broadcast receiver
	 * @see com.airs.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		// we are shutting down!
		shutdown = true;
		
		if (registered == true)
		{
			Event = null;
			airs.unregisterReceiver(SystemReceiver);
		}
	}
	
	private final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {       
    	   // we are shutting down
    	   if (shutdown == true)
    		   return;
    	   
           switch (msg.what) 
           {
           case SHOW_TOAST:
        	   Toast.makeText(airs.getApplicationContext(), airs.getString(R.string.Recorded_shared_text), Toast.LENGTH_LONG).show();     
        	   break;
           }
       }
    };
    
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // If event text sent, signal to Acquire()
            if (action.equals("com.airs.eventtext")) 
            {
            	// get mood from intent
            	Event = intent.getStringExtra("Text");

            	event_semaphore.release();		// release semaphore
				return;
            }
        }
    };
}