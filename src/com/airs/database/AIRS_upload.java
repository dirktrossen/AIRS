package com.airs.database;

import com.airs.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

public class AIRS_upload extends BroadcastReceiver
{
	private static final int HOURLY		= 60*60*1000;
	private static final int DAYLY		= 24*60*60*1000;
	private static final int WEEKLY		= 7*24*60*60*1000;

	// preferences
    private static SharedPreferences settings;


	public static void setTimer(Context context)
	{		
		boolean upload;
		long synctime;
		int status;
		
		// get default preferences
        settings = PreferenceManager.getDefaultSharedPreferences(context);

	    if((status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)) == ConnectionResult.SUCCESS)
	    {
	        // get timestamp of last sync
	        synctime = settings.getLong("SyncTimestamp", 0);
	        switch(Integer.valueOf(settings.getString("UploadFrequency", "0")))
	        {
	        case 0:
	        	upload = false;
	        	break;
	        case 1:
	        	synctime += HOURLY;
	        	upload = true;
	        	break;
	        case 10:
	        	synctime += DAYLY;
	        	upload = true;
	        	break;
	        case 20:
	        	synctime += WEEKLY;
	        	upload = true;
	        	break;
	    	default:
	        	upload = false;
	    		break;
	        }
	
	        if (upload == true)
	        {
	        	// if new synctime has passed already, so next sync in 5s
	        	if (synctime < System.currentTimeMillis())
	        		synctime = System.currentTimeMillis() + 5000;
	        	
			 	Intent notificationIntent = new Intent(context, AIRS_upload.class);
			 	PendingIntent pi = PendingIntent.getBroadcast(context, 0, notificationIntent, 0);
			 	AlarmManager am =  (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
			 	// first cancel any pending intent
			 	am.cancel(pi);
			 	// then set another one
			 	am.set(AlarmManager.RTC_WAKEUP, synctime, pi);
			 	
	    		Time timeStamp = new Time();
	    		timeStamp.set(synctime);
	
			 	Log.e("AIRS", "Set alarm event at " + timeStamp.format("%H:%M:%S on %d.%m.%Y"));
	        }
	    }
	    else
	    	Toast.makeText(context.getApplicationContext(), GooglePlayServicesUtil.getErrorString(status), Toast.LENGTH_LONG).show();
	}
		
	/** Called when the receiver is fired 
     * @param context a pointer to the {@link android.content.Context} of the application
     * @param intent a pointer to the originating {@link android.content.Intent}
     */
    @Override
    public void onReceive(Context context, Intent intent) 
    {
    	String action = intent.getAction();
    	    	
    	// need to set the timer?
        if (action != null)
        {
        	if (action.equals("android.intent.action.BOOT_COMPLETED"))
	        {
        		Log.e("AIRS", "Set timer after boot!");
		  		// set timer again!
		  		setTimer(context); 
	        }
    	}
        else
        {
        	// start upload service
            context.startService(new Intent(context, AIRS_upload_service.class));          
        }        
    }
    

}
