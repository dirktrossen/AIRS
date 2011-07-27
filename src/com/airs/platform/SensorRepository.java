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
 * Purpose: repository of all locally available sensors
 */
public class SensorRepository 
{
    static public  Sensor root_sensor = null;
    static private Sensor new_one = null;
    
    
    static public void deleteSensor()
    {
        root_sensor = null;
    }
    
    // inserts new sensor in list
    static synchronized public void insertSensor(String Symbol, String Unit, String Description, String type, int scaler, int min, int max, int poll, Handler handler)
    {
        // make copy of sensor object
        new_one = new Sensor(Symbol, Unit, Description, type, scaler, min, max, poll, handler);
        // first sensor?
        if (root_sensor == null)
            root_sensor = new_one;
        else
        {
            new_one.next = root_sensor;
            root_sensor = new_one;
        }
    }
    
    // finds sensor based on Symbol and returns handler serving sensor
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

    // finds sensor based on Symbol and returns handler serving sensor
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
    
    // finds sensor based on Symbol and returns handler serving sensor
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
