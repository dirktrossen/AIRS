/*
Copyright (C) 2006 Nokia Corporation
Copyright (C) 2008-2011, Dirk Trossen, nors@dirk-trossen.de

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
package com.android.airs;

import android.app.AlertDialog;
import android.content.*;
import android.preference.PreferenceManager;
import com.android.airs.handlerUIs.*;
import com.android.airs.helper.*;

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
    static HandlerUI handlers[] = new HandlerUI[11];
    public final static int max_handlers = 11; 
    // preferences
    static private SharedPreferences settings;
    static private AIRS airs;
    
	protected static void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}
	
	static boolean createHandlerUIs(AIRS activity)
	{
		int index = 0;
		
		airs = activity;

		// store pointer to preferences
        settings = PreferenceManager.getDefaultSharedPreferences(activity);
		
		// initialize handlers
	   handlers[0] = handlers[1] = handlers[2] = handlers[3] = handlers[4] = handlers[5] = handlers[6] = handlers[7]= handlers[8] = handlers[9] = handlers[10] = null;
   
	   // verify the proper availability for the resources required to implement the desired functionality
	   handlers[index++] = new PhoneSensorsHandlerUI();
	   handlers[index++] = new LocationHandlerUI();
//	   handlers[index++] = new CameraHandlerUI();
	   handlers[index++] = new AudioHandlerUI();
	   handlers[index++] = new ProximityHandlerUI();
	   handlers[index++] = new HeartMonitorHandlerUI();
	   handlers[index++] = new BeaconHandlerUI();
	   handlers[index++] = new RandomHandlerUI();
	   
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
		AlertDialog.Builder builder = new AlertDialog.Builder(airs);
		builder.setTitle(title)
			   .setMessage(text)
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
	}
}
