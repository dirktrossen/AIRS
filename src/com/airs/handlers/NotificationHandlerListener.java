/*
Copyright (C) 2014, TecVis LP, support@tecvis.co.uk

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

import com.airs.R;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * Class to implement the AIRS notification listener service
 */
@SuppressLint("NewApi")
public class NotificationHandlerListener extends NotificationListenerService
{	
	boolean started = true;
	
	/**
	 * Called when a notification is posted - here, we check the particular component packages that fired the event, filtering out the ones we support
	 * @param sbn Reference to the posted notification
	 */
	@Override
	public void onNotificationPosted(StatusBarNotification sbn) 
	{
		// if we don't gather, we don't do anything here!
		if (started == false)
			return;
		
    	// get notification shown
    	Notification notification = (Notification)sbn.getNotification();
	    	
    	if (notification != null)
    	{    	
	    	// now parse the specific packages we support
	    	// start with GTalk
	    	if (sbn.getPackageName().toString().compareTo("com.google.android.talk") == 0)
	    	{
		        // now broadcast the capturing of the accessibility service to the handler
				Intent intent = new Intent("com.airs.accessibility");
				intent.putExtra("NotifyText", "gtalk::" + notification.tickerText);		
				sendBroadcast(intent);		    	
	    	}
	    	
	    	// anything from Skype?
	    	if (sbn.getPackageName().toString().compareTo("com.skype.raider") == 0)
	    	{
		        // now broadcast the capturing of the accessibility service to the handler
				Intent intent = new Intent("com.airs.accessibility");
				intent.putExtra("NotifyText", "skype::Message from " + notification.tickerText);		
				sendBroadcast(intent);		    	
	    	}
	    	// anything from Spotify?
	    	if (sbn.getPackageName().toString().compareTo("com.spotify.music") == 0)
	    	{
		        // now broadcast the capturing of the accessibility service to the handler	    		
	    		// anything delivered?
	    		if (notification.tickerText != null)
	    		{
	    			// split information in tokens
	    			String tokens[] = notification.tickerText.toString().split(getString(R.string.accessibility_spotify));
	    			    			
	    			// try other '-', if previous one did not work
	    			if (tokens.length != 2)
	    				tokens = notification.tickerText.toString().split("-");
	    			
	    			if (tokens.length == 2)
	    			{
		    			// signal as play state changed event
						Intent intent = new Intent("com.android.music.playstatechanged");
						
						intent.putExtra("track", tokens[0].trim());		
						intent.putExtra("artist", tokens[1].trim());							
						intent.putExtra("album", "");		
						sendBroadcast(intent);	
					}
	    			else
	    				Log.e("AIRS", "Can't find token in '" + notification.tickerText +"'");
	    		}				
	    	}
    	}
	}

	/**
	 * Called when a notification is removed but we don't care about this
	 * @param sbn Reference to the posted notification
	 */
	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) 
	{
	}
	
	/**
	 * Called when the service is started (usually after boot). We register the events we are interested in (change of notification) and also register to the start broadcast event, sent by AIRS
	 * Originally, we set the package filters to something that does not exist, so that we minimise the firing of the callback
	 * After the start broadcast event will be received, the proper package names will be set for recording
	 * @see android.accessibilityservice.AccessibilityService#onServiceConnected()
	 */
	@Override
    public void onCreate() 
	{
        super.onCreate();

        // register for any input from the accessbility service
		IntentFilter intentFilter = new IntentFilter("com.airs.accessibility.start");
        registerReceiver(SystemReceiver, intentFilter);

        // we haven't started the gathering yet
        started = false;
	}
	
	/*
	 * Called when destroying the service
	 * @see android.app.Service#onDestroy()
	 */
    @Override
    public void onDestroy() 
    {
        super.onDestroy();
        
        unregisterReceiver(SystemReceiver);
    }
    
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // if anything sent from the accessbility service
            if (action.equals("com.airs.accessibility.start")) 
            {
            	// start the gathering?
            	if (intent.getBooleanExtra("start", true) == true)
            	    started = true;
            	else	// or stop it?
            	    started = false;
            }
        }
    };
}

