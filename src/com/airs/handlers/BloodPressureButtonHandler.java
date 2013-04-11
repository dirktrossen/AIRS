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

import com.airs.helper.SerialPortLogger;
import com.airs.helper.Waker;
import com.airs.platform.SensorRepository;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BloodPressureButtonHandler implements Handler
{
	private Context airs;
	private Semaphore event_semaphore 	= new Semaphore(1);
	private boolean registered = false;
	private StringBuffer BP_reading;
	private String  blood_pressure;
	
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
	 Function    : Acquire()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public byte[] Acquire(String sensor, String query)
	{
		BP_reading = new StringBuffer("BT");	

		// not yet registered -> then do so!!
		if (registered == false)
		{
			// check intents and set booleans for discovery
			IntentFilter intentFilter = new IntentFilter("com.airs.bloodpressurebutton");
	        airs.registerReceiver(SystemReceiver, intentFilter);
	        intentFilter = new IntentFilter("com.airs.bloodpressure");
	        airs.registerReceiver(SystemReceiver, intentFilter);
	        registered = true;
		}

		wait(event_semaphore); // block until semaphore available -> fired

		// now append reading itself
		BP_reading.append(blood_pressure);

		return BP_reading.toString().getBytes();
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
		return "My last blood pressure was " + blood_pressure;
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
		SensorRepository.insertSensor(new String("BP"), new String("mmHg"), new String("Blood pressure"), new String("txt"), 0, 0, 1, false, 0, this);	    
	}
	
	public BloodPressureButtonHandler(Context airs)
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
			airs.unregisterReceiver(SystemReceiver);
		}
	}
		
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // if mood button widget has been pressed -> start Activity for selecting mood icon
            if (action.equals("com.airs.bloodpressurebutton")) 
            {
    			try
    			{
    	            Intent startintent = new Intent(airs, BloodPressureButton_selector.class);
    	            startintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	            airs.startActivity(startintent);          
    			}
    			catch(Exception e)
    			{
    			}
    			return;
            }

            // If event button pressed, signal to Acquire()
            if (action.equals("com.airs.bloodpressure")) 
            {
            	// get mood from intent
            	blood_pressure = intent.getStringExtra("BloodPressure");

            	event_semaphore.release();		// release semaphore
				return;
            }
        }
    };
}

