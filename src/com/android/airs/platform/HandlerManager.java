/*
Copyright (C) 2005-2006 Nokia Corporation
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
package com.android.airs.platform;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.android.airs.handlers.*;
import com.android.airs.helper.*;
/**
 * @author trossen
 * @date Oct 13, 2005
 * 
 * Purpose: initializes the handlers and stores them in static variable
 * later being used to point to in sensor repository
 */
public class HandlerManager 
{
    // currently we have maximal 3 handlers
    static public Handler handlers[] = new Handler[14];
    public final static int max_handlers = 14;
    static int inst_handlers = 0; 
    static private SharedPreferences settings;
    static private Editor editor;

	protected static void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}
	
	static public boolean createHandlers(Context nors)
	{		
	   // store pointer to preferences
       settings = PreferenceManager.getDefaultSharedPreferences(nors);
       editor = settings.edit();

       inst_handlers = 0;
	   handlers[0] = handlers[1] = handlers[2] = handlers[3] = handlers[4] = handlers[5] = handlers[6] = handlers[7] = handlers[8] = handlers[9] = handlers[10] = handlers [11] = handlers[12] = handlers[13] = null;

	   // here the handlers are inserted in the field
	   // the rule is that raw sensors are inserted first before aggregated sensors are inserted.
	   // this is due to the discovery mechanism since aggregated sensor handlers usually check the availablity of the raw sensor in order to become 'visible'	   
	   handlers[inst_handlers++]  = new RandomHandler(nors);
	   handlers[inst_handlers++]  = new ProximityHandler(nors);	   
	   handlers[inst_handlers++]  = new BeaconHandler(nors);
	   handlers[inst_handlers++]  = new WifiHandler(nors);
	   handlers[inst_handlers++]  = new CellHandler(nors);
	   handlers[inst_handlers++]  = new GPSHandler(nors);	   
	   handlers[inst_handlers++]  = new EventButtonHandler(nors);
	   handlers[inst_handlers++]  = new AudioHandler(nors);
	   handlers[inst_handlers++]  = new HeartMonitorHandler(nors);
	   handlers[inst_handlers++]  = new SystemHandler(nors);
	   handlers[inst_handlers++]  = new PhoneSensorHandler(nors);
	   
	   return true;
	}
	
	static public void destroyHandlers()
	{
	   int i = 0;
	   
	   for (i=0;i<inst_handlers;i++)
		   handlers[i].destroyHandler();
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

	// read string from RMS for persistency
	static public int readRMS_i(String store, int defaultint)
	{
		int value = 0;
		
		try
		{
			String read = settings.getString(store, Integer.toString(defaultint));
			value	= Integer.parseInt(read);
		}
		catch(Exception e)
		{
			SerialPortLogger.debug("ERROR " +  "Exception: " + e.toString());
		}
		return value;
	}

	// read string from RMS for persistency
	static public boolean readRMS_b(String store, boolean defaultint)
	{
		boolean value = false;
		
		try
		{
			value	= settings.getBoolean(store, defaultint);
		}
		catch(Exception e)
		{
			SerialPortLogger.debug("ERROR " +  "Exception: " + e.toString());
		}
		return value;
	}

	// write string to RMS
	static public void writeRMS(String store, String value)
	{
		try
		{
			// put string into store
            editor.putString(store, value);
            
            // finally commit to storing values!!
            editor.commit();
		}
		catch(Exception e)
		{
			SerialPortLogger.debug("ERROR " +  "Exception: " + e.toString());
		}
	}
}
