/*
Copyright (C) 2004-2006 Nokia Corporation
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
package com.airs;

import java.util.*;
import java.io.*;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateFormat;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

import com.airs.helper.SerialPortLogger;
import com.airs.platform.HandlerManager;
import com.airs.platform.Sensor;
import com.airs.platform.SensorRepository;

/**
 * @author trossen
 * @date April 28, 2006
 * 
 * Purpose: implements local application. In our case, it is a simple display of sensor data
 */
public class AIRS_local extends Service
{
	// states for handler 
	public static final int REFRESH_VALUES 		= 1;
	public static final int SHOW_NOTIFICATION 	= 2;
	public static final int BATTERY_KILL 		= 3;
    public static final String LINE = "LINE";
    public static final String TEXT = "TEXT";

    public  boolean show_values=false;
    private HandlerThread[] threads = null;
    private int no_threads = 0;
    public  Context airs = null;
	private int	no_values = 0;
    private boolean localStore_b;
    private boolean localDisplay_b;
    private int Reminder_i;
    private boolean Vibrate, Lights;
    private String LightCode;
    private NotificationManager mNotificationManager;
    private int BatteryKill_i;
    private boolean Wakeup_b;
	private String url = "AIRS_values";
	private File mconn, path;
	private BufferedOutputStream os2;
	private int numberSensors = 0;
    private ListView sensors;
    public boolean discovered = false, running = false, restarted = false, started = false, start = false, paused = false, registered = false;
    private ArrayAdapter<String> mSensorsArrayAdapter;
    public ArrayAdapter<String> mValuesArrayAdapter;
    // This is the object that receives interactions from clients
    private final IBinder mBinder = new LocalBinder();
    private VibrateThread Vibrator;
    private WakeLock wl;
    // database variables
    static public AIRS_database database_helper;
    static public SQLiteDatabase airs_storage;
    
	// private thread for reading the sensor handlers
	 private class HandlerThread implements Runnable
	 {
		 	Sensor current;
		 	String line;
		 	String values_output = null;
		 	long values_time;
		 	int number_values = 0;
		 	public Thread thread;
		 	private boolean interrupted = false, pause = false;;
		 			 	
			protected void sleep(long millis) 
			{
				try 
				{
					Thread.sleep(millis);
				} 
				catch (InterruptedException ignore) 
				{
					interrupted = true;
				}
			}
			/***********************************************************************
			 Function    : HandlerThread()
			 Input       : current sensor, number for sensor in UI element
			 Output      :
			 Return      :
			 Description : stores parameters of this query
			***********************************************************************/
			HandlerThread(Sensor current, int j)
			{
				// copy parameters 
				this.current = current;
				line = Integer.toString(j);
				values_output = new String(current.Symbol + " : - [" + current.Unit + "]");
				
				(thread = new Thread(this)).start();
			}

			// return true if sensor has historical data
			public boolean hasHistory()
			{
				return current.hasHistory;
			}
			
			public String share()
			{
				return current.handler.Share(current.Symbol);
			}

			public String info()
			{
				Calendar cal = Calendar.getInstance();
				
				cal.setTimeInMillis(values_time);
				return new String("'" + current.Description + "' sensed " + number_values + " times\nLast sensing at " + DateFormat.format("dd MMM yyyy k:mm:ss", cal) + " with value :\n" + values_output);
			}
			
			private void output(String text)
			{
				this.output(text, false);
			}
			
			private void output(String text, boolean refresh)
			{				
				// save output for later
				if (refresh == false)
				{
					values_output = new String(text);	// store output for later
					number_values++;					// count number of sensed values
					// store timestamp
					values_time = System.currentTimeMillis();
				}
				
				// shall values been shown?
				if (show_values == true)
				{
					if (text != null)
					{
				        Message msg = mHandler.obtainMessage(REFRESH_VALUES);
				        Bundle bundle = new Bundle();
				        bundle.putString(TEXT, text);
						bundle.putString(LINE, line);
						msg.setData(bundle);
				        mHandler.sendMessage(msg);
					}
				}
			}
			
			public void refresh()
			{
				output(values_output, true);
			}

			/***********************************************************************
			 Function    : run()
			 Input       : 
			 Output      :
			 Return      :
			 Description : thread for resolving a query - to be started by the 
			 			   Acquisition component (usually in the callback for a dialog)!!
			 			   if sending NOTIFY fails, thread returns, i.e., ends
			 			   NOTIFY could fail due to termination of dialog (e.g., BYE) 
			***********************************************************************/
			public void run() 
			{
				 byte[] sensor_data=null;
		 		 int    integer;
		 		 double scaler;
		 		 int 	i;
		 		 String fileOut = null, fileIMG = null;

		 		 scaler = 1;
		 		 if (current.scaler>0)
		 			 for (i=0;i<current.scaler;i++)
		 				 scaler *=10;
		 		 else
		 			 for (i=current.scaler;i<0;i++)
		 				 scaler /=10;
		 			
		 		 try
		 		 {
			 		 while(interrupted == false)
			 		 {	 
			 			// pause while told to
			 			while(pause == true)
			 				sleep(500);

			    		// acquire latest value
		    			sensor_data = current.handler.Acquire(current.Symbol, null);
		    			// anything?
	    				if (sensor_data!=null)
	    				{
	    					// here we handle int/float sensor values
			    			if(current.type.equals("int") || current.type.equals("float"))
			    			{
			    				// do long int first
			    				integer = ((int)(sensor_data[2] & 0xFF) << 24) 
			     		         | ((int)(sensor_data[3] & 0xff) << 16) 
			      				 | ((int)(sensor_data[4] & 0xFF) << 8) 
			      				 | ((int)sensor_data[5] & 0xFF);
			    				
				    			// set text item in value field
			    				if (localDisplay_b == true)
			    				{
				    				if (current.scaler != 0)
				    					output(current.Symbol + " : " + String.valueOf((double)integer*scaler) + " [" + current.Unit + "]");
				    				else
				    					output(current.Symbol + " : " + String.valueOf(integer) + " [" + current.Unit + "]");
			    				}
			    				else
			    				{
			    					no_values++;
			    					output("# of values : " + String.valueOf(no_values));
			    				}
			    				
			    				// need to store locally?
			    				if (localStore_b == true)
			    				{
			    					if (current.scaler !=0)
			    						fileOut = new String("'"+ String.valueOf(System.currentTimeMillis()) + "','" + current.Symbol + "','" + String.valueOf((double)integer*scaler) + "'");
			    					else
			    						fileOut = new String("'" + String.valueOf(System.currentTimeMillis()) + "','" + current.Symbol + "','" + String.valueOf(integer) + "'");
			    				}
			    			}
			    			
	    					// here we handle txt sensor values
			    			if(current.type.equals("txt") || current.type.equals("str"))
			    			{
				    			// set text item in value field
			    				if (localDisplay_b == true)
			    				{
			    					output(current.Symbol + " : " + new String(sensor_data, 2, sensor_data.length - 2) + " [" + current.Unit + "]");
			    				}
			    				else
			    				{
			    					no_values++;
			    					output("# of values : " + String.valueOf(no_values));
			    				}			    					
	
			    				// need to store locally?
			    				if (localStore_b == true)
		    				    	fileOut = new String("'" + String.valueOf(System.currentTimeMillis()) + "','" + current.Symbol + "','" + new String(sensor_data, 2, sensor_data.length - 2) + "'");
			    			}
	
		   					// here we handle img and arr sensor values
			    			if(current.type.equals("img") || current.type.equals("arr"))
			    			{
				    			// set text item in value field, here only the length of the sensor value field
			    				if (localDisplay_b == true)
			    				{
			    					output(current.Symbol + " : " + Integer.toString(sensor_data.length));
			    				}
			    				else
			    				{
			    					no_values++;
			    					output("# of values : " + String.valueOf(no_values));
			    				}			    					
	
			    				// need to store locally?
			    				if (localStore_b == true)
			    				{
			    				    try 
			    				    {
			    				    	fileIMG = new String(url + String.valueOf(System.currentTimeMillis()) + "_" + current.Symbol + ".jpg" );
			    				    	// open file with read/write - otherwise, it will through a security exception (no idea why)
			    				        mconn = new File(path, fileIMG);
			    			    		os2 = new BufferedOutputStream(new FileOutputStream(mconn, true));
	
			    			    		// store sensor data
			    			    		os2.write(sensor_data, 2, sensor_data.length-2);
			    			    		os2.close();
			    			    		// store filename in recording file
			    				    	fileOut = new String("'" + String.valueOf(System.currentTimeMillis()) + "','" + current.Symbol + "','" + fileIMG + "'");
	
			    				    } 
			    				    catch(Exception e)
			    				    {
			    			    		debug("Exception in opening file connection");
			    				    }
			    				}
			    			}
			    			
			    			// anything to write to file?
			    			if (fileOut != null)
			    			{
		    				    try
		    				    {
		    				    	execStorage("INSERT into airs_values (Timestamp, Symbol, Value) VALUES ("+ fileOut + ")");
		    				    }
		    					catch(Exception e) 
		    					{    					
		    					}	
			    			}
	    				}
	    				
	    				// are we waiting for the next poll?
	    				if (current.polltime>0)
	    					sleep(current.polltime);
			 		 }
				}
				catch(Exception e)
				{
					debug("HandlerThread: interrupted and terminating 1..." + current.Symbol);
					return;
				}
				
				debug("HandlerThread: interrupted and terminating 2..."  + current.Symbol);
			}
		}
    
	/**
	 * Sleep function 
	 * @param millis
	 */
	protected static void sleep(long millis) 
	{
		try 
		{
			Thread.sleep(millis);
		} 
		catch (InterruptedException ignore) 
		{
		}
	}
    
	protected static void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}

    public class LocalBinder extends Binder 
    {
    	AIRS_local getService() 
        {
            return AIRS_local.this;
        }
    }
    
	@Override
	public IBinder onBind(Intent intent) 
	{
		SerialPortLogger.debug("AIRS_local::bound service!");
		return mBinder;
	}
		
	@Override
	public void onLowMemory()
	{
		SerialPortLogger.debugForced("AIRS_local::low memory warning from system - about to get killed!");
	}

	@Override
	public void onCreate() 
	{
		SerialPortLogger.debugForced("AIRS_local::created service!");

		// let's see if we need to restart
		Restart(true);
	}

	public synchronized void execStorage(String query)
	{
		if (airs_storage == null)
			return;
		
		synchronized(airs_storage)
		{
	    	airs_storage.beginTransaction();
		    try
		    {
		    	airs_storage.execSQL(query);
		    	airs_storage.setTransactionSuccessful();
		    }
		    finally
		    {
		    	airs_storage.endTransaction();
		    }
		}
	}

	private void start_AIRS_local()
	{
		// find out whether or not to remind of running
		Reminder_i = HandlerManager.readRMS_i("Reminder", 0) * 1000;
		Vibrate    = HandlerManager.readRMS_b("Vibrator", true);
		Lights     = HandlerManager.readRMS_b("Lights", true);
		LightCode  = HandlerManager.readRMS("LightCode", "00ff00");

		// find out whether or not to kill based on battery condition
		BatteryKill_i = HandlerManager.readRMS_i("BatteryKill", 0);

		// find out whether or not to wakeup the sensing on user activity
		Wakeup_b = HandlerManager.readRMS_b("Wakeup", false);

		// find out whether or not to store locally
		localStore_b = HandlerManager.readRMS_b("LocalStore", true);

		// find out whether or not to display locally
		localDisplay_b = HandlerManager.readRMS_b("localDisplay", true);
		
		// if to store locally -> try to open write file on memory card
		if (localStore_b == true)
		{
		    try 
		    {	            
	            // get database
	            database_helper = new AIRS_database(this.getApplicationContext());
//	            airs_storage = database_helper.getWritableDatabase();
	            airs_storage = SQLiteDatabase.openDatabase(this.getDatabasePath(database_helper.DATABASE_NAME).toString(), null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
		    } 
		    catch(Exception e)
		    {
	    		debug("AIRS_local::Exception in opening file connection");
		    	localStore_b = false;
		    }
		}
	}

	@Override
	public void onDestroy() 
	{
		int i;
		
		SerialPortLogger.debug("AIRS_local::destroyed service!");

	   	 // is local storage ongoing -> close file!
	   	 if (localStore_b == true)
	   	 {
	   		 try
	   		 {
			    airs_storage.close();
	   		 }
	   		 catch(Exception e)
	   		 {	    			 
	   		 }
	   	 } 
	   	     	 
	   	 // kill Handlers and threads
	   	 if (started == true)
	   	 {
	   		 try
	   		 {
	   			SerialPortLogger.debug("AIRS_local::terminating HandlerThreads");
		   		 // interrupt all threads for terminated
		   		 for (i = 0; i<no_threads;i++)
		   			 if (threads[i] != null)
		   				 threads[i].thread.interrupt();

		   		 // if Vibrator is running, stop it!
		   		 if (Vibrator!=null)
		   		 {
		   			Vibrator.stop = true;
		   			Vibrator.thread.interrupt(); 
		   		 }
	   		 }
	   		 catch(Exception e)
	   		 {
	   			 debug("AIRS_local::Exception when terminating Handlerthreads!");
	   		 }
	   		 
	   		 HandlerManager.destroyHandlers();	

		   	 // create wake lock if held
		   	 if (wl != null)
		   		 if (wl.isHeld() == true)
		   			 wl.release();
		   	 
		   	 // if registered for screen activity or battery level -> unregister
		   	 if (registered == true)
		         unregisterReceiver(mReceiver);	   	 
	   	 }

	   	 // and kill persistent flag
         HandlerManager.writeRMS_b("AIRS_local::running", false);
                  
         // kill internal flag
         running = false;
	}
	
	@Override
	// the sequence of calling is:
	// 1. startService()
	// 2. bindService()
	// 3. set start = true and call startService() again
	// 4. service.Discover() -> sets discovered == true
	// 5. startService() again for measurements
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		SerialPortLogger.debugForced("AIRS_local::started service ID " + Integer.toString(startId));
		
		// return if intent is null -> service was restarted
		if (intent == null)
		{
			// let's see if we need to restart
			Restart(true);
			
			return Service.START_STICKY;
		}
		
		// sensing running?
		if (running == true)
			return Service.START_STICKY;

		// handlers created?
		if (start == true && started == false)
		{
			// create handlers
			HandlerManager.createHandlers(this.getApplicationContext());	
			started = true;
			return Service.START_STICKY;
		}

		// not yet discovered?
		if (discovered == false)
			return Service.START_STICKY;
		
		// start the measurements if discovered
		running = startMeasurements(false);

		return Service.START_STICKY;
	}

	// ask threads to refresh latest value
	 public void refresh_values()
	 {
		 int i;
		 
		 for (i=0; i<no_threads;i++)
			 threads[i].refresh();
	 }

	// ask threads to pause
	 public void pause_threads()
	 {
		 int i;
		 
		 for (i=0; i<no_threads;i++)
			 threads[i].pause = true;
		 // signal paused sate
		 paused = true;
	 }

	 // show info for sensor entry
	 public String share(int j)
	 {
		return threads[j].share();
	 }

	 // show info for sensor entry
	 public void show_info(int j)
	 {
		if (threads[j].hasHistory() == true)
			threads[j].current.handler.History(threads[j].current.Symbol);
		else
		{
			String info = threads[j].info();
	  		Toast.makeText(getApplicationContext(), info, Toast.LENGTH_LONG).show();
		}
	 }

	// ask threads to pause
	 public void resume_threads()
	 {
		 int i;
		 
		 for (i=0; i<no_threads;i++)
			 threads[i].pause = false;
		 // signal paused sate
		 paused = false;
	 }

	 public void selectall()
	 {
		 int i;
		 
    	 for (i=0;i<numberSensors;i++)
    		 sensors.setItemChecked(i, true);
     }
	
	 public void unselectall()
	 {    	 
		 int i;

		 // clear checkboxes in sensor list
    	 for (i=0;i<numberSensors;i++)
    		 sensors.setItemChecked(i, false);
     }

	 public void sensor_info()
	 {
		 float scaler;
		 int i;
	 	 Sensor current = SensorRepository.root_sensor;
	 	 String infoText = new String();
 		 
	 	 while(current != null)
		 {
	 		 scaler = 1;
	 		 if (current.scaler>0)
	 			 for (i=0;i<current.scaler;i++)
	 				 scaler *=10;
	 		 else
	 			 for (i=current.scaler;i<0;i++)
	 				 scaler /=10;

    		infoText = infoText + current.Symbol + " : " + current.Description + " [" + current.Unit + "] from " + (float)current.min*scaler + " to " + (float)current.max*scaler + " \n\n"; 
	        current = current.next;
	     }
	 		
	 	 AlertDialog.Builder builder = new AlertDialog.Builder(airs);	
	 	 builder.setMessage(infoText)
    		       .setTitle("Sensor Repository")
    		       .setNeutralButton("OK", new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
 		                dialog.cancel();
    		           }
    		       });	
	 	 AlertDialog alert = builder.create();
	 	 alert.show();	 		
	 }
	 
	 // main thread to run, acquires values, stores them locally and displays them (if wanted)
	 public boolean startMeasurements(boolean restarted)
	 {
		 int i, j;
		 Sensor current = null;
		 
		 // count values to be displayed		 
		 current = SensorRepository.root_sensor;
		 i= j= 0 ;
		 while(current != null)
		 {
	    	if (sensors.isItemChecked(i) == true)
		    	j++;
	    	i++;
	        current = current.next;
	     }
		 
		 if (j == 0)
		 {
     		Toast.makeText(getApplicationContext(), "You need to enable at least one sensor before starting the measurements!", Toast.LENGTH_LONG).show();
     		return false;
		 }

		 // create notification
		 Notification notification = new Notification(R.drawable.icon, "Started AIRS", System.currentTimeMillis());

		 // create pending intent for starting the activity
		 PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, AIRS_measurements.class),  Intent.FLAG_ACTIVITY_NEW_TASK);
		 // has it been started or restarted?
		 if (restarted == false)
			 notification.setLatestEventInfo(getApplicationContext(), "AIRS Local Sensing", "...is running since...", contentIntent);
		 else
			 notification.setLatestEventInfo(getApplicationContext(), "AIRS Local Sensing", "...is restarted since...", contentIntent);
		 // set the time again for ICS
		 notification.when = System.currentTimeMillis();
		 // don't allow clearing the notification
		 notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		 startForeground(R.string.app_name, notification);
		 
         // store start timestamp
         HandlerManager.writeRMS_l("AIRS_local::time_started", System.currentTimeMillis());
		 
		 // Find and set up the ListView for values
		 mValuesArrayAdapter = new ArrayAdapter<String>(this, R.layout.sensor_list);

		 // create handler threads for measurements
		 no_threads = j;
		 threads = new HandlerThread[j];

 		 // do I need local display of values?
 		 if (localDisplay_b == true)
 		 {		 			 
 			 for (i=0;i<sensors.getCount();i++)
 			 {
 				if (sensors.isItemChecked(i) == true)
 				{
 	 				current = SensorRepository.findSensor(mSensorsArrayAdapter.getItem(i).substring(0,2));
	 				if (current != null)
	 				{
				    	if (current.type.equals("float") || current.type.equals("int") || current.type.equals("txt") || current.type.equals("str") )
					        mValuesArrayAdapter.add(current.Symbol + " : - [" + current.Unit + "]");
				    	else
					        mValuesArrayAdapter.add(current.Symbol + " : - ");			    		
	 				}
 				}
 			 } 			 
 		 }
 		 else	// if no individual values, show at least number of values acquired and memory available   
 			 mValuesArrayAdapter.add("# of values : - ");
 		  		 
		 // now create measurement threads
		 for (i=0, j=0;i<sensors.getCount();i++)
		 {
			current = SensorRepository.findSensor(mSensorsArrayAdapter.getItem(i).substring(0,2));
			if (current != null)
			{
				if (sensors.isItemChecked(i) == true)
				{
 		    		// create reading thread
 		    		if (localDisplay_b == true)
 		    			threads[j] = new HandlerThread(current, j);
 		    		else
 		    			threads[j] = new HandlerThread(current, 0);
 	    			j++;
 	    			// save setting in RMS
 	                HandlerManager.writeRMS("AIRS_local::" + current.Symbol, "On");
 				}
		    	else
	               HandlerManager.writeRMS("AIRS_local::" + current.Symbol, "Off");
			}
		 } 			  		 
 		 		 
		 if (Reminder_i>0)
			 Vibrator = new VibrateThread();
		 
		 // if wakeup by user activity -> register receiver for screen on/off
		 if (Wakeup_b == true)
		 {
			 IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
			 registerReceiver(mReceiver, filter);
			 filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			 registerReceiver(mReceiver, filter);
			 registered = true;
		 }
		 else	// otherwise get wake lock for keeping running all the time!
		 {
			 // create new wakelock
			 PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
			 
			 wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AIRS Local Lock");
			 wl.acquire();
		 }
		 
		 // need battery monitor?
		 if (BatteryKill_i > 0)
		 {
			 // register intent for watching battery
			 IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			 registerReceiver(mReceiver, filter);			 
			 registered = true;
		 }

		 // store persistently that AIRS is running
         HandlerManager.writeRMS_b("AIRS_local::running", true);
         
         // mark date now as having values
         Calendar cal = Calendar.getInstance();
         int year = cal.get(Calendar.YEAR);
         int month = cal.get(Calendar.MONTH) + 1;
         int day = cal.get(Calendar.DAY_OF_MONTH);
         try
         {
	         execStorage("INSERT into airs_dates (Year, Month, Day, Types) VALUES ('"+ String.valueOf(year) +  "','" + String.valueOf(month) + "','" + String.valueOf(day) + "','1')");	
         }
         catch(Exception e)
         {
        	 execStorage("CREATE TABLE airs_dates (Year INT, Month INT, Day INT, Types INT);");
	         execStorage("INSERT into airs_dates (Year, Month, Day, Types) VALUES ('"+ String.valueOf(year) +  "','" + String.valueOf(month) + "','" + String.valueOf(day) + "','1')");	
         }

		 return true;
	 }
	 
	 public void Discover(AIRS_local_tab airs)
	 {
			int i;
			String sensor_setting;
			Sensor current;

			this.airs = airs;
			
			// start other stuff
			start_AIRS_local();

			airs.setContentView(R.layout.sensors);
     
	        sensors 	= (ListView)airs.findViewById(R.id.sensorList);
	        sensors.setItemsCanFocus(false); 
		    sensors.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	        mSensorsArrayAdapter = new ArrayAdapter<String>(airs, android.R.layout.simple_list_item_multiple_choice);
	        
	        // Find and set up the ListView for paired devices
	        sensors.setAdapter(mSensorsArrayAdapter);

	        // do discovery on all handlers
			SensorRepository.deleteSensor();
		    // run through all handlers and discover locally first
		    for (i=0; i<HandlerManager.max_handlers;i++)
		    {
		        // is there any handler entry?
		        if (HandlerManager.handlers[i] != null)
		            // call discovery function of handler 
		            HandlerManager.handlers[i].Discover();
		    }
		    		    
		    // now build actual forms
		    current = SensorRepository.root_sensor;
		    i= 0 ;
		    while(current != null)
		    {
	    		// add new sensor choice field
		        mSensorsArrayAdapter.add(current.Symbol + " (" + current.Description + ")");

		    	// count sensors
		    	i++;
		        current = current.next;
		    }
		    
		    // sort list alphabetically
	        mSensorsArrayAdapter.sort(new Comparator<String>() {
	        	public int compare(String object1, String object2) {
	        		return object1.compareTo(object2);
	        	};
	        });
	        // notify ListView of data set change!
	        mSensorsArrayAdapter.notifyDataSetChanged();
	        
		    // save number of sensors for later!
		    numberSensors = i;

		    // set checked state based on saved settings
		    for (i=0;i<numberSensors;i++)
		    {
 				current = SensorRepository.findSensor(mSensorsArrayAdapter.getItem(i).substring(0,2));
 				
 				if (current != null)
 				{
			    	// try to read RMS
			    	sensor_setting = HandlerManager.readRMS("AIRS_local::" + current.Symbol, "Off");
			    	// set selected index to setting in RMS
			    	if (sensor_setting.compareTo("On") == 0)
			    		sensors.setItemChecked(i, true);
			    	else
			    		sensors.setItemChecked(i, false);
 				}
		    }
		    
			// signal that it is discovered
			discovered = true;
	 }	

	 private void ReDiscover()
	 {
			int i;
			String sensor_setting;
			Sensor current;
			
			airs = this.getApplicationContext();

			// start other stuff
			start_AIRS_local();
     
	        sensors 	= (ListView)new ListView(this.getApplicationContext());
	        sensors.setItemsCanFocus(false); 
		    sensors.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	        mSensorsArrayAdapter = new ArrayAdapter<String>(this.getApplicationContext(), android.R.layout.simple_list_item_multiple_choice);
	        // Find and set up the ListView for paired devices
	        sensors.setAdapter(mSensorsArrayAdapter);

	        // do discovery on all handlers
			SensorRepository.deleteSensor();
		    // run through all handlers and discover locally first
		    for (i=0; i<HandlerManager.max_handlers;i++)
		    {
		        // is there any handler entry?
		        if (HandlerManager.handlers[i] != null)
		            // call discovery function of handler 
		            HandlerManager.handlers[i].Discover();
		    }
		    		    
		    // now build actual forms
		    current = SensorRepository.root_sensor;
		    i= 0 ;
		    while(current != null)
		    {
	    		// add new sensor choice field
		        mSensorsArrayAdapter.add(current.Symbol + " (" + current.Description + ")");

		    	// count sensors
		    	i++;
		        current = current.next;
		    }
		    
		    // sort list alphabetically
	        mSensorsArrayAdapter.sort(new Comparator<String>() {
	        	public int compare(String object1, String object2) {
	        		return object1.compareTo(object2);
	        	};
	        });
	        // notify ListView of data set change!
	        mSensorsArrayAdapter.notifyDataSetChanged();
	        
		    // save number of sensors for later!
		    numberSensors = i;

		    // set checked state based on saved settings
		    for (i=0;i<numberSensors;i++)
		    {
 				current = SensorRepository.findSensor(mSensorsArrayAdapter.getItem(i).substring(0,2));
 				
 				if (current != null)
 				{
			    	// try to read RMS
			    	sensor_setting = HandlerManager.readRMS("AIRS_local::" + current.Symbol, "Off");
			    	// set selected index to setting in RMS
			    	if (sensor_setting.compareTo("On") == 0)
			    		sensors.setItemChecked(i, true);
			    	else
			    		sensors.setItemChecked(i, false);
 				}
		    }

			// signal that it is discovered
			discovered = true;
	 }	
	 
	 public void Restart(boolean check)
	 {
		boolean p_running;
		SharedPreferences   settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

		// return if it's already running
		if (running == true)
			return;
		
		try
		{
			p_running	= settings.getBoolean("AIRS_local::running", false);
			// should the service been running and was it therefore re-started?
			if (p_running == check)
			{
				SerialPortLogger.debugForced("AIRS_local::service was running before, trying to restart recording!");

				// set debug state
		   		SerialPortLogger.setDebugging(settings.getBoolean("Debug", false));

				// create handlers
				HandlerManager.createHandlers(this.getApplicationContext());	
				started = true;

				SerialPortLogger.debugForced("AIRS_local::re-created handlers");

				// re-discover the sensors
				ReDiscover();
				
				SerialPortLogger.debugForced("AIRS_local::re-discovered sensors");

				// start the measurements as being restarted, i.e., it takes the latest discovery and selection that is stored persistently
				running = startMeasurements(check);

				SerialPortLogger.debugForced("AIRS_local::re-started measurements");
				
				// restart service in order to make it service
		        startService(new Intent(this, AIRS_local.class));
		        
				SerialPortLogger.debugForced("AIRS_local::re-started service to make it sticky");
			}

		}
		catch(Exception e)
		{
			SerialPortLogger.debugForced("AIRS_local::Restart():ERROR " +  "Exception: " + e.toString());
		}		 
	 }
	 
	 // Vibrate watchdog
	 private class VibrateThread implements Runnable
	 {
		 public Thread thread;
		 public boolean stop = false;
		 
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

		 VibrateThread()
		 {
			// save thread for later to stop 
			(thread = new Thread(this)).start();			 
		 }
		 
		 public void run()
		 {
			 long vibration[] = {0,200,0};
			 				
			 // get notification manager
			 mNotificationManager = (NotificationManager)airs.getSystemService(Context.NOTIFICATION_SERVICE); 

			 while (stop == false)
			 {		
				 // sleep for the agreed time
			     sleep(Reminder_i);

			     // prepare notification to user
			     Notification notif = new Notification();
			     notif.when              = System.currentTimeMillis(); 
			     notif.flags			|= Notification.FLAG_ONLY_ALERT_ONCE;
			     if (Vibrate == true)
			    	 notif.vibrate			 = vibration;
				 
				 if (Lights == true)
				 {
	                notif.ledARGB   = 0xff000000 | Integer.valueOf(LightCode, 16); 
	                notif.flags     |= Notification.FLAG_SHOW_LIGHTS; 
				 }
	              
				 // now shoot off alert
				 mNotificationManager.notify(0, notif);   
				 sleep(750);
				 
				 // and cancel
	             mNotificationManager.cancel(0);
			 }
		 }
	 }
	 
	 // The Handler that gets information back from the other threads, updating the values for the UI
	 public final Handler mHandler = new Handler() 
     {
        @Override
        public void handleMessage(Message msg) 
        {
        	String position;
        	int j;
        	
            switch (msg.what) 
            {
            case REFRESH_VALUES:
            	// parse line from message
            	j = Integer.parseInt(msg.getData().getString(LINE));
            	// refresh appropriate line with given text
		        mValuesArrayAdapter.setNotifyOnChange(false);
	            position = mValuesArrayAdapter.getItem(j);
	            mValuesArrayAdapter.insert(msg.getData().getString(TEXT), j);
	            mValuesArrayAdapter.remove(position);
	            mValuesArrayAdapter.notifyDataSetChanged();
	            break;  
            case SHOW_NOTIFICATION:
               	Toast.makeText(getApplicationContext(), msg.getData().getString(TEXT), Toast.LENGTH_LONG).show();   
               	break;
            case BATTERY_KILL:
            	// stop foreground service
            	stopForeground(true);
            	
            	// now create new notification
            	NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
       		 	Notification notification = new Notification(R.drawable.icon, "Killed AIRS", System.currentTimeMillis());
       		 	Intent notificationIntent = new Intent(getApplicationContext(), AIRS_tabs.class);
       		 	PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
       			notification.setLatestEventInfo(getApplicationContext(), "AIRS Local Sensing", "...has been killed at " + Integer.toString(BatteryKill_i) + "% battery...", contentIntent);
       			
       			// give full fanfare
       			notification.flags |= Notification.DEFAULT_SOUND | Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
       			notification.ledARGB = 0xffff0000;
       			notification.ledOffMS = 1000;
       			notification.ledOnMS = 1000;
       			
       			mNotificationManager.notify(17, notification);
       			
       			// stop service now!
            	stopSelf();
            	break;
            default:  
            	break;
            }
        }
     };
     
     public final BroadcastReceiver mReceiver = new BroadcastReceiver() 
     {
         @Override
         public void onReceive(Context context, Intent intent) 
         {
        	 int Battery = 100;
        	 
        	 // screen off -> pause sensing
        	 if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
        		 pause_threads();
        	 // screen on -> resume sensing
        	 if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
        		 resume_threads();     
             
        	 // if battery changed...
             if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) 
             {
 	            int rawlevel = intent.getIntExtra("level", -1);
 	            int scale = intent.getIntExtra("scale", -1);
 	            if (rawlevel >= 0 && scale > 0) 
 	                Battery = (rawlevel * 100) / scale;
 	            
 	            // need to trigger battery kill action?
 	            if (Battery < BatteryKill_i)
 	            {
			        Message msg = mHandler.obtainMessage(BATTERY_KILL);
			        mHandler.sendMessage(msg);
 	            }
             }

         }
     };     
}
