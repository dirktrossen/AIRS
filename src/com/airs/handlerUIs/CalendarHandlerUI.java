/*
Copyright (C) 2011, Dirk Trossen, airs@dirk-trossen.de

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
package com.airs.handlerUIs;

import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceActivity;
import android.provider.CalendarContract;

import com.airs.*;
import com.airs.helper.ListPreferenceMultiSelect;

public class CalendarHandlerUI implements HandlerUI
{
    // BT stuff
	public HandlerEntry init()    
	{				
		HandlerEntry entry = new HandlerEntry();
		entry.name = new String("Calendar");
		entry.description = new String("Events from your available calendars");
		entry.resid = R.drawable.calendar;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefscalendar;
	}

	public String About()
	{
	    String AboutText = new String(
	    		"Retrieves calendar entries from your available calendars!\n"+
	    		"You can select the calendars that AIRS should monitor. The calendars are polled frequently for upcoming events.");
	    
		return AboutText;
	}
	
	public String AboutTitle()
	{
		return "Calendar";
	}

	public void configurePreference(PreferenceActivity prefs)
	{	
		int i;
		String[] fields = {CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars._ID};
    	Cursor eventCursor;

		// try to find the preference we want to configure
		ListPreferenceMultiSelect list = (ListPreferenceMultiSelect)prefs.findPreference("CalendarHandler::Calendar_names");
		if (list==null)
			return;

		try
		{
			// use different way of accessing calendar for Honeycomb
			if (Build.VERSION.SDK_INT>=11)
				eventCursor =  prefs.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, fields, null, null, null);
			else
				eventCursor =  prefs.getContentResolver().query(Uri.parse("content://com.android.calendar/calendars"), (new String[] { "displayName", "_id"}), null, null, null);
			
	
		    if (eventCursor == null)
		    {
				list.setEntries(null);
				list.setEntryValues(null);
		    	return;
		    }
		    
		    if (eventCursor.getCount() == 0)
		    {
				list.setEntries(null);
				list.setEntryValues(null);
		    	return;
		    }
		    
		    eventCursor.moveToFirst();
		    
	    	CharSequence[] calendars = new CharSequence[eventCursor.getCount()];
	    	CharSequence[] ids = new CharSequence[eventCursor.getCount()];
	
	    	// collect all calendars
		    for(i=0;i<eventCursor.getCount();i++)
		    {
		    	calendars[i] = eventCursor.getString(0);
		    	ids[i] = eventCursor.getString(1);
		    	
		    	eventCursor.moveToNext();
		    }
	
		    // set calendar names as entries in list
			list.setEntries(calendars);
			// set calendar IDs as entries in preference
			list.setEntryValues(ids);
		}
		catch(Exception e)
		{
			list.setEntries(null);
			list.setEntryValues(null);			
		}
	}
}
