/*
Copyright (C) 2006 Nokia Corporation
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

import android.app.AlertDialog;
import android.content.*;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.TextView;

import com.airs.handlerUIs.*;
import com.airs.helper.SerialPortLogger;

/**
 * @author trossen
 * @date Mar 13, 2006
 * 
 * Purpose: initializes the handler UIs and stores them in static variable
 * later being used to point to in N_RSA_GW
 */
public class HandlerUIManager 
{
	// currently we have maximal 9 handlers
    static HandlerUI handlers[] = new HandlerUI[12];
    public final static int max_handlers = 12; 
    // preferences
    static private SharedPreferences settings;
    static private Context airs;
    
	protected static void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}
	
	static boolean createHandlerUIs(Context activity)
	{
		int index = 0;
		
		airs = activity;

		// store pointer to preferences
        settings = PreferenceManager.getDefaultSharedPreferences(activity);
		
		// initialize handlers
	   handlers[0] = handlers[1] = handlers[2] = handlers[3] = handlers[4] = handlers[5] = handlers[6] = handlers[7]= handlers[8] = handlers[9] = handlers[10] = null;
   
	   // verify the proper availability for the resources required to implement the desired functionality
	   handlers[index++] = new DeviceInfoHandlerUI();
	   handlers[index++] = new EnvironmentalSensorsUI();
	   handlers[index++] = new AudioHandlerUI();
	   handlers[index++] = new LocationHandlerUI();
	   handlers[index++] = new HeartMonitorHandlerUI();
	   handlers[index++] = new BeaconHandlerUI();
	   handlers[index++] = new CalendarHandlerUI();
	   handlers[index++] = new EventButtonHandlerUI();
	   handlers[index++] = new MediaWatcherHandlerUI();
	   
	   return true;
	}
	
	// read string from RMS for persistency
	static public String readRMS(String store, String defaultString)
	{
		String value = null;
		
		try
		{
			value	= settings.getString(store, defaultString);
		}
		catch(Exception e)
		{
			SerialPortLogger.debug("ERROR " +  "Exception: " + e.toString());
		}
		return value;
	}
	
	static public void AboutDialog(String title, String text)
	{				
		// Linkify the message
	    final SpannableString s = new SpannableString(text);
	    Linkify.addLinks(s, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);

		AlertDialog.Builder builder = new AlertDialog.Builder(airs);
		builder.setTitle(title)
			   .setMessage(s)
			   .setIcon(R.drawable.about)
		       .setNeutralButton("OK", new DialogInterface.OnClickListener() 
		       {
		           public void onClick(DialogInterface dialog, int id) 
		           {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();
		alert.show();
		
		// Make the textview clickable. Must be called after show()
	    ((TextView)alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
	}
}
