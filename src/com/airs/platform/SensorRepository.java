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
 * Maintains a repository of all locally available sensors
 */
public class SensorRepository 
{
	/**
	 * Reference to the first {@link Sensor} in the repository
	 */
    static public  Sensor root_sensor = null;
    static private Sensor new_one = null;
    
    /**
     * Delete all sensors (by simply setting the first sensor reference to null}
     */
    static public void deleteSensor()
    {
        root_sensor = null;
    }
    
    /**
     * Inserts new sensor in list, calling appropriate {@link Sensor} instance
     * @param Symbol String of the sensor symbol (used for retrieving sensors)
     * @param Unit String of the sensor unit (used in titles of visualisations)
     * @param Description String of the sensor description (used in titles of visualisations)
     * @param type String with the type of the sensor (used in the recording thread to handle data differently)
     * @param scaler Scaler of the values as an exponent of 10
     * @param min Minimum value of the sensor, if supported, expressed with the scaler in mind
     * @param max Maximum value of the sensor, if supported, expressed with the scaler in mind
     * @param hasHistory Flag if {@link Handler} implementing the sensor supports history for it
     * @param poll Polling time in millisecond - if zero, the handler will block properly when called frequently, e.g., realising the recording through a callback
     * @param handler Reference to the {@link Handler} implementing the sensor
     */
    static synchronized public void insertSensor(String Symbol, String Unit, String Description, String Explanation, String type, int scaler, int min, int max, boolean hasHistory, int poll, Handler handler)
    {
        // make copy of sensor object
        new_one = new Sensor(Symbol, Unit, Description, Explanation, type, scaler, min, max, hasHistory, poll, handler);
        // first sensor?
        if (root_sensor == null)
            root_sensor = new_one;
        else
        {
            new_one.next = root_sensor;
            root_sensor = new_one;
        }
    }
    
    /**
     * Finds sensor based on Symbol and sets valid flag
     * @param Symbol String to sensor symbol trying to find
     * @param status Status of sensor after being found
     * @param reason Status string of sensor after being found 
     * @param thread Reference to {@link java.lang.Runnable} of the acquisition thread being used right now
     */
    static synchronized public void setSensorStatus(String Symbol, int status, String reason, Runnable thread)
    {
        Sensor current = root_sensor;
        
        while(current != null)
        {
            if (current.Symbol.compareTo(Symbol) == 0)
            {
                current.status = status;
                current.statusString = reason;
                if (thread != null)
                	current.acquire_thread = thread;
            }
            current = current.next;
        }
    }

    /**
     * Finds sensor based on Symbol and returns its current status
     * @param Symbol String of the sensor symbol to be found
     * @return status of the sensor, SENSOR_INVALID if nothing is found
     */
    static synchronized public int getSensorStatus(String Symbol)
    {
        Sensor current = root_sensor;
        
        while(current != null)
        {
            if (current.Symbol.compareTo(Symbol) == 0)
                return current.status;
            current = current.next;
        }
        
        return Sensor.SENSOR_INVALID;
    }

    /**
     * Finds sensor based on Symbol and returns handler serving sensor
     * @param Symbol String of the sensor symbol to be found
     * @return Reference to the {@link Handler} that implements the sensor symbol
     */
    static synchronized public Handler findHandler(String Symbol)
    {
        Sensor current = root_sensor;
        
        while(current != null)
        {
            if (current.Symbol.compareTo(Symbol) == 0)
                return current.handler;
            current = current.next;
        }
        return null;
    }

    /**
     * Finds sensor based on Symbol and returns reference to the {@link Sensor}
     * @param Symbol String of the sensor symbol to be found
     * @return Reference to the {@link Sensor} of this symbol
     */
    static synchronized public Sensor findSensor(String Symbol)
    {
        Sensor current = root_sensor;
        
        while(current != null)
        {
            if (current.Symbol.compareTo(Symbol) == 0)
                return current;
            current = current.next;
        }
        return null;
    }
    
    /**
     * Finds sensor based on Symbol and returns its scaler
     * @param Symbol String of the sensor symbol to be found
     * @return Scaler of the sensor being found
     */
    static synchronized public int findScaler(String Symbol)
    {
        Sensor current = root_sensor;
        
        while(current != null)
        {
            if (current.Symbol.compareTo(Symbol) == 0)
                return current.scaler;
            current = current.next;
        }
        return 0;
    }
}
