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

import com.airs.platform.HandlerManager;
import com.airs.platform.SensorRepository;

/**
 * @author trossen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RandomHandler implements Handler
{
	private Random random = null;
	// create field that holds acquisition data
	private byte[] readings = new byte[6];
	private int polltime=5000;
	
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
	    SensorRepository.insertSensor(new String("Rd"), new String("ticks"), new String("Random Number"), new String("int"), 0, 0, 65535, polltime, this);	    
	}
	
	public RandomHandler(Context nors)
	{
		// read polltime
		polltime = HandlerManager.readRMS_i("RandomHandler::SamplingRate", 5) * 1000;
	}
	
	public void destroyHandler()
	{
	}
}
