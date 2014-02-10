package com.airs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class AIRS_restart extends BroadcastReceiver
{
	// preferences
    private SharedPreferences settings;

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
        	if (action.equals("android.intent.action.PACKAGE_REPLACED"))
	        {
        		// get default preferences
                settings = PreferenceManager.getDefaultSharedPreferences(context);

                if (intent.getDataString().contains("com.airs"))
                {
        			Log.e("AIRS", "AIRS was updated!");
	        		if (settings.getBoolean("AIRS_local::running", false) == true)
	        		{	
	        		    // start service and connect to it -> then discover the sensors
	        	        context.getApplicationContext().startService(new Intent(context, AIRS_local.class));
	        			Log.e("AIRS", "Restart AIRS since it was running when updated!");
	        		}
                }
	        }
    	}
    }
}
