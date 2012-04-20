/*
Copyright (C) 2005-2006 Nokia Corporation
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
package com.airs.platform;

import com.airs.handlers.*;

/**
 * @author trossen
 * @date Oct 13, 2005
 * 
 * Purpose: Sensor class, to be used in repository
 */
public class Sensor 
{
    public Handler 	handler;			// points to handler serving this sensor
    // entries according to SSI for now
    public String  	Symbol;				// symbol of the sensor
    public String  	Unit;				// unit of the sensor
    public String  	Description;		// descrption of the sensor
    public String	type;				// data type of the sensor
    public int		scaler;				// scaler of the sensor
    public int		min;				// min and max value of sensor reading
    public int 		max;
    public boolean  hasHistory;			// does this sensor support history?
    public int		polltime;			// polling timer
    byte[]  sensor_data = null;
    byte[]  reading = null;
    long    last_read;
    boolean	discovered;
    
    // linked list
    public Sensor next;
    
    Sensor()
    {
        Symbol = null;
        Unit = null;
        Description = null;
        type = null;
        scaler = 0;
        next = null;
        min = max = 0;
        discovered = false;
    }
    
    // initialize Sensor based on discovered sensor description 
    Sensor(String s, String u, String d, String t, int sc, int mi, int ma, boolean history, int poll, Handler h)
    {
        handler = h;
        Symbol = new String(s);
        Unit   = new String(u);
        Description = new String(d);
        type = new String(t);
        scaler = sc;
        next = null;
        min = mi;
        max = ma;
        hasHistory = history;
        polltime = poll;
        // our last read value is now minus polltime -> when accessing get_value() the first time, it will acquire initial value right away
        last_read = System.currentTimeMillis() - polltime;
        sensor_data = null;
        discovered = false;
    }
    // acquire method to allow for multiple queries accessing the same sensor values concurrently, 
    // i.e., this is the point of data acquisition
    synchronized public byte[] get_value(String query)
    {
    	// current time
    	long now = System.currentTimeMillis();
    	
    	// is last data still valid? -> then return last read value
    	if (now<last_read+polltime)
    		return sensor_data;
    	else		// otherwise read new sensor values
    	{
    		// dereference for garbage collector
    		sensor_data = null;
    		// get sensor reading -> written C++ like on purpose!
    		reading = handler.Acquire(Symbol, query);
    		// store time for polling
    		last_read = now;
    		if (reading != null)
    		{
	    		// now allocate own memory and copy
	    		sensor_data = new byte[reading.length];
				// copy into sensor_data field
				System.arraycopy(reading, 0, sensor_data, 0, reading.length);
	    		return sensor_data;
    		}
    		else
    			return null;
    	}
    }
    
    // for some sensors allow setting polltime
    public void set_polltime(int poll)
    {
    	// only change polltime if new time is smaller than the one set, i.e., set polltime to minimal requested
    	if (poll<polltime)
    	{
	    	polltime = poll;
	        // our last read value is now minus polltime -> when accessing get_value() the first time, it will acquire initial value right away
	        last_read = System.currentTimeMillis() - polltime;
    	}
    }
}
