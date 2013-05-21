/*
Copyright (C) 2013, Dirk Trossen, support@tecvis.co.uk

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

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class NotificationHandlerService extends AccessibilityService
{	
	boolean started = true;
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) 
	{
		Log.v("AIRS", "NotificationAccessibility: Got event = " + String.valueOf(event.getEventType()));
	    if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) 
	    {
	    	// get notification shown
	    	Notification notification = (Notification)event.getParcelableData();
			Log.v("AIRS", "AIRS:NotificationAccessibility: Got event for package " + event.getPackageName().toString());
	    	
	    	// now parse the specific packages we support
	    	// start with GTalk
	    	if (event.getPackageName().toString().compareTo("com.google.android.gsf") == 0)
	    	{
		        // now broadcast the capturing of the accessibility service to the handler
				Intent intent = new Intent("com.airs.accessibility");
				intent.putExtra("NotifyText", "gtalk::" + notification.tickerText);		
				sendBroadcast(intent);		    	
	    	}
	    	
	    	// anything from Skype?
	    	if (event.getPackageName().toString().compareTo("com.skype.raider") == 0)
	    	{
		        // now broadcast the capturing of the accessibility service to the handler
				Intent intent = new Intent("com.airs.accessibility");
				intent.putExtra("NotifyText", "skype::Message from " + notification.tickerText);		
				sendBroadcast(intent);		    	
	    	}
	    	// anything from Spotify?
	    	if (event.getPackageName().toString().compareTo("com.spotify.mobile.android.ui") == 0)
	    	{
		        // now broadcast the capturing of the accessibility service to the handler
	    		Log.v("Notification", "Spotify : " + notification.tickerText);
	    		
	    		// anything delivered?
	    		if (notification.tickerText != null)
	    		{
	    			// split information in tokens
	    			String tokens[] = notification.tickerText.toString().split(" - ");
	    			
	    			// signal as play state changed event
					Intent intent = new Intent("com.android.music.playstatechanged");
					
					// sorry, it only provides artist and track
					intent.putExtra("artist", tokens[1]);							
					intent.putExtra("track", tokens[0]);		
					intent.putExtra("album", "");		
					sendBroadcast(intent);		    	
	    		}				
	    	}
	    }
	}
	
	@Override
	protected void onServiceConnected() 
	{	    
		// register for any input from the accessbility service
		IntentFilter intentFilter = new IntentFilter("com.airs.accessibility.start");
      registerReceiver(SystemReceiver, intentFilter);
        
        // now switch off initially
	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
	    info.notificationTimeout = 100;
	    info.feedbackType = AccessibilityEvent.TYPES_ALL_MASK;
	    info.packageNames = new String[] {"com.airs.helpers" };
//	    info.packageNames = new String[] {"com.skype.raider", "com.google.android.gsf" };

	    setServiceInfo(info);   
	}

	@Override
	public void onInterrupt() 
	{
	}
	
    @Override
    public void onDestroy() 
    {
        super.onDestroy();
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
            	{
            	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
            	    info.notificationTimeout = 100;
            	    info.feedbackType = AccessibilityEvent.TYPES_ALL_MASK;
            	    info.packageNames = new String[] {"com.skype.raider", "com.google.android.gsf", "com.spotify.mobile.android.ui"};
            	    setServiceInfo(info);
            	    
            	    started = true;
            	}
            	else	// or stop it?
            	{
            	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
            	    info.notificationTimeout = 100;
            	    info.feedbackType = AccessibilityEvent.TYPES_ALL_MASK;
            	    info.packageNames = new String[] {"com.airs.helpers" };
            	    setServiceInfo(info);   
            	    
            	    started = false;
            	}
            }
        }
    };
}

