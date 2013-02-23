/*
Copyright (C) 2013, Dirk Trossen, airs@dirk-trossen.de

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

import com.airs.platform.SensorRepository;

public class NotificationHandler implements Handler
{
	private Context airs;
	private Semaphore notify_semaphore 	= new Semaphore(1);
	String notify_text;
	
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
	public synchronized byte[] Acquire(String sensor, String query)
	{
		StringBuffer readings = new StringBuffer("NO");
		
		if(sensor.compareTo("NO") == 0)
		{
			wait(notify_semaphore);
			readings.append(notify_text);
			
			return readings.toString().getBytes();
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
		return null;		
	}
	
	/***********************************************************************
	 Function    : History()
	 Input       : sensor input for specific history views
	 Output      :
	 Return      :
	 Description : calls historical views
	***********************************************************************/
	public synchronized void History(String sensor)
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
	    SensorRepository.insertSensor(new String("NO"), new String("text"), new String("Notification"), new String("txt"), 0, 0, 1, false, 0, this);	    
	}
	
	public NotificationHandler(Context airs)
	{
		this.airs = airs;
		
		// arm semaphore
		wait(notify_semaphore); 
		
		// register for any input from the accessbility service
		IntentFilter intentFilter = new IntentFilter("com.airs.accessibility");
        airs.registerReceiver(SystemReceiver, intentFilter);
        
        // now broadcast the start of the accessibility service
		Intent intent = new Intent("com.airs.accessibility.start");
		intent.putExtra("start", true);		
		airs.sendBroadcast(intent);			
	}

	public void destroyHandler()
	{
		// release all semaphores for unlocking the Acquire() threads
		notify_semaphore.release();

		// unregister the broadcast receiver
		airs.unregisterReceiver(SystemReceiver);

        // now broadcast the stop of the accessibility service
		Intent intent = new Intent("com.airs.accessibility.start");
		intent.putExtra("start", false);
		airs.sendBroadcast(intent);				
	}
	
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // if anything sent from the accessbility service
            if (action.equals("com.airs.accessibility")) 
            {
            	// get mood from intent
            	notify_text = intent.getStringExtra("NotifyText");

            	notify_semaphore.release();		// release semaphore
            }
        }
    };
}
