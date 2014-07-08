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

import com.airs.R;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Class to implement the AIRS accessibility service
 */
public class NotificationHandlerService extends AccessibilityService
{	
	boolean started = true;
	
	/**
	 * Called when an accessibility event occurs - here, we check the particular component packages that fired the event, filtering out the ones we support
	 * @param event Reference to the fired {android.view.accessibility.AccessibilityEvent}
	 * @see android.accessibilityservice.AccessibilityService#onAccessibilityEvent(android.view.accessibility.AccessibilityEvent)
	 */
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) 
	{
	    if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) 
	    {
	    	// get notification shown
	    	Notification notification = (Notification)event.getParcelableData();
		    	
	    	if (notification != null)
	    	{    	
		    	// now parse the specific packages we support
		    	// start with GTalk
		    	if (event.getPackageName().toString().compareTo("com.google.android.talk") == 0)
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
		    	if (event.getPackageName().toString().compareTo("com.spotify.music") == 0)
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
	}
	
	/**
	 * Called when the service is started (usually after boot). We register the events we are interested in (change of notification) and also register to the start broadcast event, sent by AIRS
	 * Originally, we set the package filters to something that does not exist, so that we minimise the firing of the callback
	 * After the start broadcast event will be received, the proper package names will be set for recording
	 * @see android.accessibilityservice.AccessibilityService#onServiceConnected()
	 */
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
	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
	    info.packageNames = new String[] {"com.airs.helpers" };
//	    info.packageNames = new String[] {"com.skype.raider", "com.google.android.gsf" };

	    setServiceInfo(info);   
	}

	/*
	 * Called when interrupting the service
	 * @see android.accessibilityservice.AccessibilityService#onInterrupt()
	 */
	@Override
	public void onInterrupt() 
	{
	}
	
	/*
	 * Called when destroying the service
	 * @see android.app.Service#onDestroy()
	 */
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
            	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
            	    info.packageNames = new String[] {"com.skype.raider", "com.google.android.talk", "com.spotify.music"};
            	    setServiceInfo(info);
            	    
            	    started = true;
            	}
            	else	// or stop it?
            	{
            	    AccessibilityServiceInfo info = new AccessibilityServiceInfo();
            	    info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
            	    info.notificationTimeout = 100;
            	    info.feedbackType = AccessibilityServiceInfo.FEEDBACK_VISUAL;
            	    info.packageNames = new String[] {"com.airs.helpers" };
            	    setServiceInfo(info);   
            	    
            	    started = false;
            	}
            }
        }
    };
}

