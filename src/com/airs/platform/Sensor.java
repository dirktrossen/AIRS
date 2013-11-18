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
 * Sensor class, holding the various pieces of the discovered sensor
 * To be used in repository
 */
public class Sensor 
{
	/**
	 * Sensor is valid
	 */
	public static final int SENSOR_VALID 		= 0;
	/**
	 * Sensor is invalid, e.g., when BT pulse cannot be found when AIRS is started
	 */
	public static final int SENSOR_INVALID		= 1;
	/** Sensor is currently suspended, such as when BT pulse temporarily disconnects
	 */
	public static final int SENSOR_SUSPEND		= 2;

	/**
	 * points to handler serving this sensor
	 */
	public Handler 	handler;	
    // entries according to SSI for now
	/**
	 * symbol of the sensor (see online manual for all supported sensor symbols for now)
	 */
    public String  	Symbol;				
    /**
     * unit of the sensor
     */
    public String  	Unit;		
    /**
     * description of the sensor
     */
    public String  	Description;
    /**
     * longer explanation of the sensor
     */
    public String  	Explanation;
    /**
     * data type of the sensor (int, float, txt, str, arr)
     */
    public String	type;	
    /**
     * scaler of the sensor
     */
    public int		scaler;		
    /**
     * min value of sensor reading (if supported)
     */
    public int		min;	
    /**
     * max value of sensor reading (if supported)
     */
    public int 		max;
    /**
     * does this sensor support history?
     */
    public boolean  hasHistory;
    /**
     * polling timer - if zero, the sensor is a callback sensor and will block appropriately when called during recording
     */
    public int		polltime;
    /**
     * status of the timer (valid, invalid, suspended)
     */
    public int	    status;		
    /**
     * String describing the status further
     */
    public String	statusString;
    /**
     * time when sensor was saved in airs_sensor_used for cases where sensors are removed by Storica
     */
    public long time_saved;
    private byte[]  sensor_data = null;
    private byte[]  reading = null;
    private long    last_read;
    /**
     * Flag if sensor has already been discovered or not
     */
    public boolean	discovered;
    /**
     * Reference to current {@link java.lang.Runnable} that implements the acquisition for this sensor
     */
    public Runnable acquire_thread;

    /**
     * Reference to the next sensor in the repository list
     */
    public Sensor next;
    
    /**
     * Constructor, setting all values appropriately to default values
     */
    Sensor()
    {
        Symbol = null;
        Unit = null;
        Description = null;
        Explanation = null;
        type = null;
        scaler = 0;
        next = null;
        min = max = 0;
    }
    
    /**
     * Initialize Sensor based on discovered sensor description 
     * @param s String of the sensor symbol (used for retrieving sensors)
     * @param u String of the sensor unit (used in titles of visualisations)
     * @param d String of the sensor description (used in titles of visualisations)
     * @param t String with the type of the sensor (used in the recording thread to handle data differently)
     * @param sc Scaler of the values as an exponent of 10
     * @param mi Minimum value of the sensor, if supported, expressed with the scaler in mind
     * @param ma Maximum value of the sensor, if supported, expressed with the scaler in mind
     * @param history Flag if {@link Handler} implementing the sensor supports history for it
     * @param poll Polling time in millisecond - if zero, the handler will block properly when called frequently, e.g., realising the recording through a callback
     * @param h Reference to the {@link Handler} implementing the sensor
     */
    Sensor(String s, String u, String d, String e, String t, int sc, int mi, int ma, boolean history, int poll, Handler h)
    {
        handler = h;
        Symbol = new String(s);
        Unit   = new String(u);
        Description = new String(d);
        Explanation = new String(e);
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
        status = SENSOR_VALID;
        statusString = null;
    }
    
    /**
     * Synchronised acquire method to allow for multiple queries accessing the same sensor values concurrently, i.e., this is the point of data acquisition
     * @param query String with the Sensor query
     * @return byte array with the reading in the format array[0]+array[1] = sensor symbol and array[2]+array[3]+... = data of the sensor
     */
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
    
    /**
     * For some sensors allow setting polltime
     * @param poll polling time in milliseconds
     */
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
