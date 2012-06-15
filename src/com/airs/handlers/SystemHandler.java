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

import java.util.List;
import java.util.concurrent.Semaphore;

import com.airs.helper.SerialPortLogger;
import com.airs.platform.HandlerManager;
import com.airs.platform.History;
import com.airs.platform.SensorRepository;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

/**
 * @author trossen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SystemHandler implements com.airs.handlers.Handler
{
	public static final int INIT_BATTERY 		= 1;
	public static final int INIT_SCREEN 		= 2;
	public static final int INIT_HEADSET 		= 3;
	public static final int INIT_PHONESTATE 	= 4;
	public static final int INIT_OUTGOINGCALL 	= 5;
	public static final int INIT_SMSRECEIVED 	= 6;

	private Context airs;
	private int oldBattery = -1;
	private int Battery = 0;
	private int voltage = 0;
	private int old_voltage = -1;
	private int temperature = 0;
	private int old_temperature = -1;
	private int oldRAM = 0;
	private int ScreenOn = 0;
	private int oldScreenOn = -1;
	private int battery_charging = 0;
	private int oldbattery_charging = -1;
	private int headset = 0, oldheadset = -1;
	private String caller = null, callee = null, smsReceived = null;
	private int polltime = 5000;
	private ActivityManager am;
	private boolean startedBattery = false, startedScreen = false, startedHeadset = false;
	private boolean startedPhoneState = false, startedOutgoingCall = false, startedSMSReceived = false;
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
		boolean read = false, task_first;
		int i;
		
		read = false;

		// battery level?
		if(sensor.compareTo("Ba") == 0)
		{
			// has Battery been started?
			if (startedBattery == false)
			{
				// send message to handler thread to start Battery
		        Message msg = mHandler.obtainMessage(INIT_BATTERY);
		        mHandler.sendMessage(msg);	
			}

			wait(battery_semaphore); // block until semaphore available

			// any difference in value?
			if (Battery != oldBattery)
			{
				read = true;
				reading_value = Battery; 
				oldBattery = Battery;
			}
		}

		// battery voltage?
		if(sensor.compareTo("BV") == 0)
		{
			// has Battery been started?
			if (startedBattery == false)
			{
				// send message to handler thread to start Battery
		        Message msg = mHandler.obtainMessage(INIT_BATTERY);
		        mHandler.sendMessage(msg);	
			}

			wait(battery_semaphore); // block until semaphore available

			// any difference in value?
			if (voltage != old_voltage)
			{
				read = true;
				reading_value = voltage; 
				old_voltage = voltage;
			}
		}

		// battery temperature?
		if(sensor.compareTo("BM") == 0)
		{
			// has Battery been started?
			if (startedBattery == false)
			{
				// send message to handler thread to start Battery
		        Message msg = mHandler.obtainMessage(INIT_BATTERY);
		        mHandler.sendMessage(msg);	
			}

			wait(battery_semaphore); // block until semaphore available

			// any difference in value?
			if (temperature != old_temperature)
			{
				read = true;
				reading_value = temperature; 
				old_temperature = temperature;
			}
		}

		// battery discharging?
		if(sensor.compareTo("Bc") == 0)
		{
			// has Battery been started?
			if (startedBattery == false)
			{
				// send message to handler thread to start Battery
		        Message msg = mHandler.obtainMessage(INIT_BATTERY);
		        mHandler.sendMessage(msg);	
			}

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
			// has Screen been started?
			if (startedScreen == false)
			{
				// send message to handler thread to start Screen
		        Message msg = mHandler.obtainMessage(INIT_SCREEN);
		        mHandler.sendMessage(msg);	
			}

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
			// has Headset been started?
			if (startedHeadset == false)
			{
				// send message to handler thread to start Headset
		        Message msg = mHandler.obtainMessage(INIT_HEADSET);
		        mHandler.sendMessage(msg);	
			}

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
			// has SMS received been started?
			if (startedSMSReceived == false)
			{
				// send message to handler thread to start SMS receive
		        Message msg = mHandler.obtainMessage(INIT_SMSRECEIVED);
		        mHandler.sendMessage(msg);	
			}

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
			// has PhoneState been started?
			if (startedPhoneState == false)
			{
				// send message to handler thread to start PhoneState
		        Message msg = mHandler.obtainMessage(INIT_PHONESTATE);
		        mHandler.sendMessage(msg);	
			}

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
			// has Outgoing Call been started?
			if (startedOutgoingCall == false)
			{
				// send message to handler thread to start Outgoing Call
		        Message msg = mHandler.obtainMessage(INIT_OUTGOINGCALL);
		        mHandler.sendMessage(msg);	
			}

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
				MemoryInfo mi = new MemoryInfo();
				am.getMemoryInfo(mi);
				reading_value = (int)(mi.availMem / 1024L);
				
//				reading_value = (int) Runtime.getRuntime().freeMemory();;
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

		// Recently visited/started tasks?
		if(sensor.compareTo("TR") == 0)
		{
			try
			{
				List<ActivityManager.RunningTaskInfo> tasks;
				ActivityManager.RunningTaskInfo tinfo;
				String task;
				PackageManager pm = airs.getPackageManager();
				
				// get current tasks running
				tasks = am.getRunningTasks(100);

				// none???
				if (tasks == null)
					return null;
				
				// now create list
				StringBuffer buffer = new StringBuffer("TR");
		
				// start with first task
				task_first=true;
				
				for (i=0; i<tasks.size(); i++)
				{
					tinfo = tasks.get(i);
					
					// is there at least one task running?
					if (tinfo.numRunning > 0)
					{
		            	// first task? -> then no \n at the end of it!
		            	if (task_first == true)
		            		task_first = false;
		            	else
		        	        buffer.append("\n");
	
		            	// get package name
		            	task = tinfo.baseActivity.getPackageName();
		            	try
		            	{
		            		ApplicationInfo ai = pm.getApplicationInfo(task, 0);
		            		task = (String)pm.getApplicationLabel(ai);
		            		if (task != null)
		            			buffer.append(task);
		            	}
		            	catch(Exception e)
		            	{
		            	}
					}
				}
					
	    		return buffer.toString().getBytes();
			}
			catch(Exception err)
			{
				return null;
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
	 Function    : Share()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public synchronized String Share(String sensor)
	{		
		// battery level?
		if(sensor.compareTo("Ba") == 0)
			return "The current battery is " + String.valueOf(oldBattery) + " %";
		
		// battery voltage?
		if(sensor.compareTo("BV") == 0)
			return "The current battery voltage is " + String.valueOf(old_voltage) + " mV";

		// battery temperature?
		if(sensor.compareTo("BM") == 0)
			return "The current battery temperature is " + String.valueOf(old_temperature) + " C";

		// battery discharging?
		if(sensor.compareTo("Bc") == 0)
			if (oldbattery_charging ==1)
				return "The battery is currently charging";
			else
				return "The battery is currently not charging";

		// current RAM?
		if(sensor.compareTo("Rm") == 0)
			return "The currently available RAM is " + String.valueOf(oldRAM) + " kByte";

		// headset plugged/unplugged?
		if(sensor.compareTo("HS") == 0)
			if (oldheadset ==1)
				return "The headset is currently plugged in";
			else
				return "The headset is currently plugged in";

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
		// battery level?
		if(sensor.compareTo("Ba") == 0)
			History.timelineView(airs, "Battery level [%]", "Ba");
		
		// battery voltage?
		if(sensor.compareTo("BV") == 0)
			History.timelineView(airs, "Battery Voltage [mV]", "BV");

		// battery temperature?
		if(sensor.compareTo("BM") == 0)
			History.timelineView(airs, "Battery Temperature [C]", "BM");

		// current RAM?
		if(sensor.compareTo("Rm") == 0)
			History.timelineView(airs, "RAM [kByte]", "Rm");
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
		SensorRepository.insertSensor(new String("Ba"), new String("%"), new String("Battery Level"), new String("int"), 0, 0, 100, true, 0, this);	    
		SensorRepository.insertSensor(new String("BV"), new String("mV"), new String("Battery Voltage"), new String("int"), 0, 0, 10, true, 0, this);	    
		SensorRepository.insertSensor(new String("Bc"), new String("boolean"), new String("Battery charging"), new String("int"), 0, 0, 1, false, 0, this);	    
		SensorRepository.insertSensor(new String("BM"), new String("C"), new String("Battery Temperature"), new String("int"), -1, 0, 100, true, 0, this);	    
		SensorRepository.insertSensor(new String("Rm"), new String("RAM"), new String("Memory available"), new String("int"), 0, 0, 512000000, true, polltime, this);	    
		SensorRepository.insertSensor(new String("Sc"), new String("Screen"), new String("Screen on/off"), new String("int"), 0, 0, 1, false, 0, this);	    
		SensorRepository.insertSensor(new String("HS"), new String("Headset"), new String("Headset plug state"), new String("int"), 0, 0, 1, false, 0, this);	    
    	SensorRepository.insertSensor(new String("IC"), new String("Number"), new String("Incoming Call"), new String("txt"), 0, 0, 1, false, 0, this);	    
    	SensorRepository.insertSensor(new String("OC"), new String("Number"), new String("Outgoing Call"), new String("txt"), 0, 0, 1, false, 0, this);	    
    	SensorRepository.insertSensor(new String("SR"), new String("SMS"), new String("Received SMS"), new String("txt"), 0, 0, 1, false, 0, this);	    
    	SensorRepository.insertSensor(new String("TR"), new String("Tasks"), new String("Running tasks"), new String("txt"), 0, 0, 1, false, polltime, this);	    	    	
	}
	
	public SystemHandler(Context airs)
	{
		// read polltime
		polltime  = HandlerManager.readRMS_i("SystemSensorsHandler::SystemPoll", 5) * 1000;

		this.airs = airs;
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

	        // get ActivityManager for list of tasks
		    am  = (ActivityManager) airs.getSystemService(Context.ACTIVITY_SERVICE); 			// if something returned, enter sensor value

		}
		catch(Exception e)
		{
			SerialPortLogger.debugForced("Semaphore!!!!");
		}
	}
	
	public void destroyHandler()
	{
		if (startedBattery == true || startedScreen==true || startedHeadset == true || startedPhoneState == true || startedOutgoingCall == true || startedSMSReceived == true)
			airs.unregisterReceiver(SystemReceiver);
	}
	
	private String getContactByNumber(String number)
	{
		try
		{
			// Form an array specifying which columns to return. 
			String[] projection = new String[] {
			                             Contacts._ID,
			                             Contacts.DISPLAY_NAME
			                          };
			
			// lookup URI for callee
			Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

			// query contact database
			Cursor cur = airs.getContentResolver().query(uri, projection, null, null, null);
			
			// move to first returned element
			if (cur.moveToFirst() == true)
			{
    			String name = cur.getString(cur.getColumnIndex(Contacts.DISPLAY_NAME));
    			
    			if (name != null)
    				return name;
			}
			else
				return "---";
		}
		catch(Exception e)
		{
			return "---";
		}
		
		return "---";
	}
	
	// The Handler that gets information back from the other threads, initializing phone sensors
	// We use a handler here to allow for the Acquire() function, which runs in a different thread, to issue an initialization of the invidiual sensors
	// since registerListener() can only be called from the main Looper thread!!
	private final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {     
    	   IntentFilter intentFilter;
    	   
           switch (msg.what) 
           {
           case INIT_BATTERY:
	   		   	intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	   		   	airs.registerReceiver(SystemReceiver, intentFilter);
	   		   	startedBattery = true;
	   		   	break;  
           case INIT_SCREEN:
	   	        intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		        airs.registerReceiver(SystemReceiver, intentFilter);
		        intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		        airs.registerReceiver(SystemReceiver, intentFilter);
	   		   	startedScreen = true;
		        break;  
           case INIT_HEADSET:
	   	        intentFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
		        airs.registerReceiver(SystemReceiver, intentFilter);
	   		   	startedHeadset = true;
		        break;
           case INIT_PHONESTATE:
	   	        intentFilter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
		        airs.registerReceiver(SystemReceiver, intentFilter);
	   		   	startedPhoneState = true;
		        break;
           case INIT_OUTGOINGCALL:
	   	        intentFilter = new IntentFilter("android.intent.action.NEW_OUTGOING_CALL");
		        airs.registerReceiver(SystemReceiver, intentFilter);
	   		   	startedOutgoingCall = true;
		        break;
           case INIT_SMSRECEIVED:
	   	        intentFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		        // since SMS_RECEIVED is sent via ordered broadcast, we have to make sure that we 
		        // receive this with highest priority in case a receiver aborts the broadcast!
		        intentFilter.setPriority(1000000);		
		        airs.registerReceiver(SystemReceiver, intentFilter);
	   		   	startedSMSReceived = true;
		        break;
           default:  
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

            // When battery changed...
            if (Intent.ACTION_BATTERY_CHANGED.compareTo(action) == 0) 
            {
	            int rawlevel = intent.getIntExtra("level", -1);
	            int scale = intent.getIntExtra("scale", -1);
	            voltage = intent.getIntExtra("voltage", -1);
	            temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
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
        			try
        			{
	        			caller = new String(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)); 
	        			
	        			// append caller display name, if available
	        			if (caller != null)
	        				caller = caller.concat(":" + getContactByNumber(caller));        
	        			else
	        				caller = caller.concat(":" + "---");        
        			}
        			catch(Exception e)
        			{
        				caller = new String("unknown:---");
        			}
        				
    				caller_semaphore.release();			// release semaphore
        		}
        		return;
            }

            // when outgoing call
            if (action.compareTo("android.intent.action.NEW_OUTGOING_CALL") == 0) 
            {
            	try
            	{
	        		callee = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
	        		
	    			// append caller display name, if available
        			if (callee != null)
        				callee = callee.concat(":" + getContactByNumber(callee));        
        			else
        				callee = callee.concat(":" + "---");    
            	}
    			catch(Exception e)
    			{
    				callee = new String("unknown:---");
    			}
    			
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
                String Address = message.getOriginatingAddress();
                
                smsReceived = new String(Address + ":" + getContactByNumber(Address) + ":" + message.getMessageBody());
                
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
