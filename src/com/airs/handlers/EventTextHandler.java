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

import android.app.Activity;
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

public class EventTextHandler extends Activity implements com.airs.handlers.Handler
{
	public static final int SHOW_TOAST 	= 1;

	private Context airs;
	private String Event, old_Event;
	private Semaphore event_semaphore 	= new Semaphore(1);
	private boolean registered = false;
	
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
		StringBuffer readings;

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
		if (old_Event != null)
			return "My last event text was " + old_Event + "!";
		else
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
		SensorRepository.insertSensor(new String("ET"), new String("Event"), new String("Event text"), new String("str"), 0, 0, 1, false, 0, this);	    
	}
	
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
	
	public void destroyHandler()
	{
		if (registered == true)
		{
			Event = null;
			airs.unregisterReceiver(SystemReceiver);
		}
	}
	
	public final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {        	
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