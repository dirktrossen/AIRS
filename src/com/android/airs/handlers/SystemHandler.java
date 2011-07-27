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
import android.os.BatteryManager;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

/**
 * @author trossen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SystemHandler implements Handler
{
	private Context nors;
	private int oldBattery = -1;
	private int Battery = 0;
	private int oldRAM = 0;
	private int ScreenOn = 0;
	private int oldScreenOn = -1;
	private int battery_charging = 0;
	private int oldbattery_charging = -1;
	private int headset = 0, oldheadset = -1;
	private String caller = null, callee = null, smsReceived = null;
	private Semaphore battery_semaphore 	= new Semaphore(1);
	private Semaphore screen_semaphore 		= new Semaphore(1);
	private Semaphore charger_semaphore 	= new Semaphore(1);
	private Semaphore headset_semaphore 	= new Semaphore(1);
	private Semaphore caller_semaphore 		= new Semaphore(1);
	private Semaphore callee_semaphore 		= new Semaphore(1);
	private Semaphore received_semaphore 	= new Semaphore(1);

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
		byte[] readings = null;
		int reading_value = 0;
		boolean read;
		
		read = false;

		// battery level?
		if(sensor.compareTo("Ba") == 0)
		{
			wait(battery_semaphore); // block until semaphore available

			// any difference in value?
			if (Battery != oldBattery)
			{
				read = true;
				reading_value = Battery; 
				oldBattery = Battery;
			}
		}

		// battery discharging?
		if(sensor.compareTo("Bc") == 0)
		{
			wait(charger_semaphore); // block until semaphore available

			// any difference in value?
			if (battery_charging != oldbattery_charging)
			{
				read = true;
				reading_value = battery_charging; 
				oldbattery_charging = battery_charging;
			}
		}

		// screen on/off?
		if(sensor.compareTo("Sc") == 0)
		{
			wait(screen_semaphore); // block until semaphore available

			// any difference in value?
			if (ScreenOn != oldScreenOn)
			{
				read = true;
				reading_value = ScreenOn; 
				oldScreenOn = ScreenOn;
			}
		}

		// headset plugged/unplugged?
		if(sensor.compareTo("HS") == 0)
		{
			wait(headset_semaphore); // block until semaphore available

			// any difference in value?
			if (headset != oldheadset)
			{
				read = true;
				reading_value = headset; 
				oldheadset = headset;
			}
		}

		// received SMS?
		if(sensor.compareTo("SR") == 0)
		{
			wait(received_semaphore);  // block until semaphore available

			// any difference in value?
			if (smsReceived != null)
			{
				// create Stringbuffer with sms number being sent
			    StringBuffer buffer = new StringBuffer("SR");
			    buffer.append(smsReceived);
			    smsReceived = null;
	    		return buffer.toString().getBytes();
			}
		}

		// incoming call?
		if(sensor.compareTo("IC") == 0)
		{
			wait(caller_semaphore); // block until semaphore available

			// any difference in value?
			if (caller != null)
			{
				// create reading buffer with caller number
			    StringBuffer buffer = new StringBuffer("IC");
			    buffer.append(caller);
			    caller = null;
	    		return buffer.toString().getBytes();
			}
		}

		// outgoing call?
		if(sensor.compareTo("OC") == 0)
		{
			wait(callee_semaphore); // block until semaphore available

			// any difference in value?
			if (callee !=null)
			{
				// create reading buffer with callee number
			    StringBuffer buffer = new StringBuffer("OC");
			    buffer.append(callee);
			    callee = null;
	    		return buffer.toString().getBytes();
			}
		}

		// RAM available?
		if(sensor.compareTo("Rm") == 0)
		{
			try
			{
				reading_value = (int) Runtime.getRuntime().freeMemory();;
				// any difference in value?
				if (reading_value != oldRAM)
				{
					read = true;
					oldRAM = reading_value;
				}
			}
			catch(Exception err)
			{
			}
		}

		// anything read?
		if (read == true)
		{
			readings = new byte[6];
			readings[0] = (byte)sensor.charAt(0);
			readings[1] = (byte)sensor.charAt(1);
			readings[2] = (byte)((reading_value>>24) & 0xff);
			readings[3] = (byte)((reading_value>>16) & 0xff);
			readings[4] = (byte)((reading_value>>8) & 0xff);
			readings[5] = (byte)(reading_value & 0xff);
			return readings;
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
		try
		{
			SensorRepository.insertSensor(new String("Ba"), new String("%"), new String("Battery Level"), new String("int"), 0, 0, 100, 0, this);	    
			SensorRepository.insertSensor(new String("Bc"), new String("boolean"), new String("Battery charging"), new String("int"), 0, 0, 1, 0, this);	    
			SensorRepository.insertSensor(new String("Rm"), new String("RAM"), new String("VM Memory available"), new String("int"), 0, 0, 512000000, 10000, this);	    
			SensorRepository.insertSensor(new String("Sc"), new String("Screen"), new String("Screen on/off"), new String("int"), 0, 0, 1, 0, this);	    
			SensorRepository.insertSensor(new String("HS"), new String("Headset"), new String("Headset plug state"), new String("int"), 0, 0, 1, 0, this);	    
	    	SensorRepository.insertSensor(new String("IC"), new String("Number"), new String("Incoming Call"), new String("txt"), 0, 0, 1, 0, this);	    
	    	SensorRepository.insertSensor(new String("OC"), new String("Number"), new String("Outgoing Call"), new String("txt"), 0, 0, 1, 0, this);	    
	    	SensorRepository.insertSensor(new String("SR"), new String("SMS"), new String("Received SMS"), new String("txt"), 0, 0, 1, 0, this);	    
		}
		catch(Exception err)
		{
		}
	}
	
	public SystemHandler(Context nors)
	{
		this.nors = nors;
		try
		{
			// charge the semaphores to block at next call!
			battery_semaphore.acquire(); 
			screen_semaphore.acquire(); 
			charger_semaphore.acquire(); 
			headset_semaphore.acquire(); 
			caller_semaphore.acquire(); 
			callee_semaphore.acquire(); 
			received_semaphore.acquire(); 
			
			// check intents and set booleans for discovery
			IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	        nors.registerReceiver(SystemReceiver, intentFilter);
	        intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
	        nors.registerReceiver(SystemReceiver, intentFilter);
	        intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
	        nors.registerReceiver(SystemReceiver, intentFilter);
	        intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
	        nors.registerReceiver(SystemReceiver, intentFilter);
	        intentFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
	        nors.registerReceiver(SystemReceiver, intentFilter);
	        intentFilter = new IntentFilter("android.intent.action.NEW_OUTGOING_CALL");
	        nors.registerReceiver(SystemReceiver, intentFilter);
	        intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
	        // since SMS_RECEIVED is sent via ordered broadcast, we have to make sure that we 
	        // receive this with highest priority in case a receiver aborts the broadcast!
	        intentFilter.setPriority(1000000);		
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
            if (Intent.ACTION_BATTERY_CHANGED.compareTo(action) == 0) 
            {
	            int rawlevel = intent.getIntExtra("level", -1);
	            int scale = intent.getIntExtra("scale", -1);
	            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
	            if (rawlevel >= 0 && scale > 0) 
	                Battery = (rawlevel * 100) / scale;
	            if (plugged==0)
	            	battery_charging = 0;
	            else
	            	battery_charging = 1;
				battery_semaphore.release();		// release semaphore
				charger_semaphore.release();		// release semaphore
				return;
            }
            
            // when screen is gone off
            if (Intent.ACTION_SCREEN_OFF.compareTo(action) == 0) 
            {
            	ScreenOn = 0;
				screen_semaphore.release();			// release semaphore
				return;
            }

            // when screen is gone on
            if (Intent.ACTION_SCREEN_ON.compareTo(action) == 0) 
            {
            	ScreenOn = 1;
				screen_semaphore.release();			// release semaphore
				return;
            }

            // when headset is plugged in/out
            if (Intent.ACTION_HEADSET_PLUG.compareTo(action) == 0) 
            {
            	headset = intent.getIntExtra("state", -1);
				headset_semaphore.release();			// release semaphore
				return;
            }

            // when incoming call
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.compareTo(action) == 0) 
            {
        		String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        		if (TelephonyManager.EXTRA_STATE_RINGING.equals(state))
        		{
        			caller = new String(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)); 
    				caller_semaphore.release();			// release semaphore
        		}
        		return;
            }

            // when outgoing call
            if (action.compareTo("android.intent.action.NEW_OUTGOING_CALL") == 0) 
            {
        		callee = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
				callee_semaphore.release();			// release semaphore
				return;
            }

            // when incoming SMS
            if (action.compareTo("android.provider.Telephony.SMS_RECEIVED") == 0) 
            {
                Bundle extras = intent.getExtras();
                if (extras == null)
                    return;

                Object[] pdus = (Object[]) extras.get("pdus");

                // get first PDU to extract originating address
                SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[0]);
                smsReceived = new String(message.getOriginatingAddress() + ":" + message.getMessageBody());
                
                // more than one PDU?
                if (pdus.length >1)
	                for (int i = 1; i < pdus.length; i++) 
	                {
	                	// only take message body of the additional PDUs
	                    message = SmsMessage.createFromPdu((byte[]) pdus[i]);
	                    smsReceived = smsReceived.concat(message.getMessageBody());
	                }
                
				received_semaphore.release();			// release semaphore
            } 
        }
    };
}
