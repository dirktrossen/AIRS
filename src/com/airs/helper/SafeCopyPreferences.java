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
package com.airs.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Class to safely copy an entire preference set over
 * 'Safely' here is in regards to preserving certain preference entries that should not be copied over
 * This function is used by {@link com.airs.AIRS_shortcut}, {@link com.airs.AIRS_record_tab} and {@link com.airs.AIRS_configure_remote} to handle shortcuts
 *
 */
public class SafeCopyPreferences 
{
	/**
	 * Copy the preferences in a safe way, as explained in the Class description
	 * @param context Reference to the calling {@link android.content.Context}
	 * @param shortcutFile File that is copied as the new preference file 
	 */
	static public void copyPreferences(Context context, File shortcutFile)
	{
        long synctime;
        int version, i, own_events;
        boolean tables, tables2, first_start, copy_template;
        String music, storedWifis, lastSMS;
        SharedPreferences settings;

    	// get default preferences and write something in it
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        // get values that should not be overwritten!
        synctime = settings.getLong("SyncTimestamp", 0);
        lastSMS = settings.getString("SystemHandler::lastSeen", "");
        version = settings.getInt("Version", 0);	
        tables = settings.getBoolean("AIRS_local::TablesExists", false);	
        tables2 = settings.getBoolean("AIRS_local::Tables2Exists", false);
        first_start = settings.getBoolean("AIRS_local::first_start", false);
        copy_template = settings.getBoolean("AIRS_local::copy_template", false);
        music = settings.getString("MusicPlayerHandler::Music", "");
		storedWifis = settings.getString("LocationHandler::AdaptiveGPS_WiFis", "");
		
		try
		{
	        // read all entries related to event annotations
			own_events = Integer.parseInt(settings.getString("EventButtonHandler::MaxEventDescriptions", "5"));
			if (own_events<1)
				own_events = 5;
			if (own_events>50)
				own_events = 50;
		}
		catch(Exception e)
		{
			own_events = 5;
		}

		String event_selected_entry = settings.getString("EventButtonHandler::EventSelected", "");
		String[] event = new String[own_events];			
		for (i=0;i<own_events;i++)
			event[i]	= settings.getString("EventButtonHandler::Event"+Integer.toString(i), "");
        
        // now also copy the template into the default settings!
    	File preferenceFile = new File(context.getFilesDir(), "../shared_prefs/com.airs_preferences.xml");
    	// if file does not exist use a path that is usually used by GalaxyS in 2.3!!!
    	if (preferenceFile.exists() == false)
    		preferenceFile = new File("/dbdata/databases/com.airs/shared_prefs/com.airs_preferences.xml");

        try
        {
        	FileInputStream srcStream = new FileInputStream(shortcutFile);
        	FileOutputStream dstStream = new FileOutputStream(preferenceFile);
            FileChannel src = srcStream.getChannel();
            FileChannel dst = dstStream.getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            srcStream.close();
            dstStream.close();
        }
        catch(Exception e)
        {
        	Log.e("AIRS", "Could not copy template into default settings!");
        }
        
    	// get default preferences and write something in it
        settings = PreferenceManager.getDefaultSharedPreferences(context);

        // get default preferences
		Editor editor = settings.edit();
		
		// write certain back in order for them to not be overwritten!
		editor.putLong("SyncTimestamp", synctime);
		editor.putString("SystemHandler::lastSeen", lastSMS);
		editor.putInt("Version", version);
		editor.putBoolean("AIRS_local::TablesExists", tables);
		editor.putBoolean("AIRS_local::Tables2Exists", tables2);
		editor.putBoolean("AIRS_local::first_start", first_start);
		editor.putBoolean("AIRS_local::copy_template", copy_template);
		editor.putString("MusicPlayerHandler::Music", music);
		editor.putString("LocationHandler::AdaptiveGPS_WiFis", storedWifis);
		
		// put back all entries related to event annotations
		for (i=0;i<own_events;i++)
			editor.putString("EventButtonHandler::Event"+Integer.toString(i), event[i]);
		editor.putString("EventButtonHandler::EventSelected", event_selected_entry);

		editor.commit(); 
	}
}
