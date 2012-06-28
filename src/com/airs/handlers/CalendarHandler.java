/*
Copyright (C) 2012, Dirk Trossen, airs@dirk-trossen.de

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

import com.airs.platform.HandlerManager;
import com.airs.platform.SensorRepository;

import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Instances;

public class CalendarHandler implements Handler
{
	Context airs;
	// configuration data
	private int polltime = 60000;
	private long currentRead = 0;
	private String[] calendars;
	private boolean no_calendars = false;
	
	// calendar data
	StringBuffer reading;
	
	/**
	 * Sleep function 
	 * @param millis
	 */
	protected void sleep(long millis) 
	{
		try 
		{
			Thread.sleep(millis);
		} 
		catch (InterruptedException ignore) 
		{
		}
	}

	/***********************************************************************
	 Function    : Acquire()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public synchronized byte[] Acquire(String sensor, String query)
	{
		// acquire data and send out
		reading = null;
		reading = new StringBuffer(sensor);
		
		getEntry();
		
		if (reading != null)
			return reading.toString().getBytes();
		else
			return null;
	}
	
	/***********************************************************************
	 Function    : Share()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : return humand readable sharing string
	***********************************************************************/
	public synchronized String Share(String sensor)
	{
		return null;
	}
	
	/***********************************************************************
	 Function    : History()
	 Input       : sensor input for specific history views
	 Output      :
	 Return      :
	 Description : calls historical views
	***********************************************************************/
	public synchronized void History(String sensor)
	{
		
	}
	
	/***********************************************************************
	 Function    : Discover()
	 Input       : 
	 Output      : string with discovery information
	 Return      : 
	 Description : provides discovery information of this particular acquisition 
	 			   module, hardcoded 
	***********************************************************************/
	public void Discover()
	{
		if (no_calendars == true)
			SensorRepository.insertSensor(new String("CA"), new String("-"), new String("Calendar entry"), new String("str"), 0, 0, 1, false, polltime, this);
	}
	
	public CalendarHandler(Context nors)
	{
		String storedCalendars;
		// store for later
		airs = nors;

		// retrieve clendars
		SharedPreferences  settings = PreferenceManager.getDefaultSharedPreferences(nors);
		storedCalendars = settings.getString("CalendarHandler::Calendar_names", null);
		// retrieve individual calendars now
		calendars = storedCalendars.split("::");
		
		if (calendars == null)
			no_calendars = false;
		else
			no_calendars = true; 

		// now read polltime for audio sampling
		polltime = HandlerManager.readRMS_i("CalendarHandler::samplingpoll", 60) * 1000;

	}
	
	public void destroyHandler()
	{
	}
	
	private void getEntry()
	{
		boolean first = false, calendar = false;
		String title, location, ID;
		long begin, end;
		String[] fields = {Instances.TITLE, Instances.BEGIN, Instances.END, Instances.EVENT_LOCATION, Instances.CALENDAR_ID};
		long now = System.currentTimeMillis();
		int i, j, calendar_no = calendars.length;
		Cursor eventCursor;

		currentRead = now - polltime;

		// query anything between now-polltime and now
		if (Build.VERSION.SDK_INT>=11)
			eventCursor =  CalendarContract.Instances.query(airs.getContentResolver(), fields, currentRead, now + 24*60*60*1000);
		else
		{
			Uri.Builder builder = Uri.parse("content://com.android.calendar/instances/when").buildUpon();
	        ContentUris.appendId(builder, currentRead);
	        ContentUris.appendId(builder, now + 24*60*60*1000);
	        eventCursor = airs.getContentResolver().query(builder.build(),
	                new String[] { "title", "begin", "end", "eventLocation", "calendar_id"}, null, null, null); 
		}
		// walk through all returns
	    eventCursor.moveToFirst();
		for (i=0;i<eventCursor.getCount();i++) 
		{
		    title = eventCursor.getString(0);
		    begin = eventCursor.getLong(1);
		    end   = eventCursor.getLong(2);
		    location = eventCursor.getString(3);
		    ID 	  = eventCursor.getString(4); 
		    

		    calendar = false;
		    // see if the entry is from the calendars I want
		    for (j=0;j<calendar_no;j++)
		    	if (calendars[j].compareTo(ID) == 0)
		    	{
		    		calendar = true;
		    		break;
		    	}
		    
		    if (calendar == true)
			    // is this something new?
			    if (begin > currentRead && begin<now)
			    {
					if (first == false)
					{
						reading.append(title + ":" + location + ":" + String.valueOf(begin) + ":" + String.valueOf(end));
						first = true;
					}
					else
						reading.append("\n" + title + ":" + location + ":" + String.valueOf(begin) + ":" + String.valueOf(end));			
			    }	
		    
		    eventCursor.moveToNext();
		}
		
		// free data
		eventCursor.close();
		
		// have we read anything? -> if not, delete reading string
		if (first == false)
			reading = null;
	}
}
