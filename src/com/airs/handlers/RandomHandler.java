/*
Copyright (C) 2005-2006 Nokia Corporation, Contact: Dirk Trossen, airs@dirk-trossen.de
Copyright (C) 2010-2011, Dirk Trossen, airs@dirk-trossen.de

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

import java.util.Random;

import android.content.Context;

import com.airs.R;
import com.airs.platform.HandlerManager;
import com.airs.platform.History;
import com.airs.platform.SensorRepository;

/** 
 * Class to read random number generator sensors, specifically the Rd sensor
 * @see Handler
 */
public class RandomHandler implements Handler
{
	private Context airs;
	private Random random = null;
	// create field that holds acquisition data
	private byte[] readings = new byte[6];
	private int polltime=5000;
	
	/**
	 * Method to acquire sensor data
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.airs.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public synchronized byte[] Acquire(String sensor, String query)
	{
		short random_value = 0;
		
		if(sensor.compareTo("Rd") == 0)
		{
			if (random == null)
				random = new Random();
			random_value = (short)random.nextInt();
		}
		
		readings[0] = (byte)sensor.charAt(0);
		readings[1] = (byte)sensor.charAt(1);
		readings[2] = (byte)0;
		readings[3] = (byte)0;
		readings[4] = (byte)((random_value>>8) & 0xff);
		readings[5] = (byte)(random_value & 0xff);
		
		return readings;		
	}
	
	/**
	 * Method to share the last value of the given sensor - here doing nothing
	 * @param sensor String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.airs.handlers.Handler#Share(java.lang.String)
	 */
	public synchronized String Share(String sensor)
	{		
		return null;		
	}
	
	/**
	 * Method to view historical chart of the given sensor symbol - here doing nothing
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.airs.handlers.Handler#History(java.lang.String)
	 */
	public synchronized void History(String sensor)
	{
		History.timelineView(airs, "Random [-]", "Rd");
	}
	
	/**
	 * Method to discover the sensor symbols support by this handler
	 * As the result of the discovery, appropriate {@link com.airs.platform.Sensor} entries will be added to the {@link com.airs.platform.SensorRepository}
	 * @see com.airs.handlers.Handler#Discover()
	 * @see com.airs.platform.Sensor
	 * @see com.airs.platform.SensorRepository
	 */
	public void Discover()
	{
	    SensorRepository.insertSensor(new String("Rd"), new String("ticks"), airs.getString(R.string.RD_d), airs.getString(R.string.RD_e), new String("int"), 0, 0, 65535, true, polltime, this);	    
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's reading the interval from the preferences
	 * @param airs Reference to the calling {@link android.content.Context}
	 */
	public RandomHandler(Context airs)
	{
		this.airs = airs;
		// read polltime
		polltime = HandlerManager.readRMS_i("RandomHandler::SamplingRate", 5) * 1000;
	}
	
	/**
	 * Method to release all handler resources - doing nothing here
	 * @see com.airs.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
	}
}
