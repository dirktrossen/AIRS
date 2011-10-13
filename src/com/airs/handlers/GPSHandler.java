/*
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

import java.util.concurrent.Semaphore;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.airs.helper.SerialPortLogger;
import com.airs.platform.HandlerManager;
import com.airs.platform.SensorRepository;

// this handler has been separated from the standard location handler 
// in order to prevent blocking of the other sensor types when not being able to get a GPS fix!
public class GPSHandler implements com.airs.handlers.Handler
{
	public static final int INIT_GPS = 1;

	Context nors;
	// are these there?
	private boolean enableGPS = false, startedGPS = false, useWifi = false;
	// polltime
	private int 		polltime = 10000, updatemeter = 100;
	// sensor data
    private double Longitude = 0, Latitude = 0, Altitude = 0, Speed, Bearing; 
    private double oldLongitude = -1, oldLatitude = -1, oldAltitude = -1;
    private Location oldLocation = null;
    private long oldTime;
	// for GPS
	private LocationManager manager;
	private LocationListener mReceiver;
	private Semaphore longitude_semaphore 	= new Semaphore(1);
	private Semaphore latitude_semaphore 	= new Semaphore(1);
	private Semaphore altitude_semaphore 	= new Semaphore(1);
	private Semaphore speed_semaphore 		= new Semaphore(1);
	private Semaphore bearing_semaphore 	= new Semaphore(1);
	private Semaphore full_semaphore 		= new Semaphore(1);

	protected void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}
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
	
	private void wait(Semaphore sema)
	{
		try
		{
			sema.acquire();
		}
		catch(Exception e)
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
	public byte[] Acquire(String sensor, String query)
	{
		// has GPS been started?
		if (startedGPS == false)
		{
			// send message to handler thread to start GPS
	        Message msg = mHandler.obtainMessage(INIT_GPS);
	        mHandler.sendMessage(msg);	
	        // wait for starting GPS
	        while (startedGPS == false)
	        	sleep(100);
		}

		// acquire data and send out
		if (enableGPS == true)
			return GPSReading(sensor);
		else
			return null;		
	}
	
	/***********************************************************************
	 Function    : Share()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public synchronized String Share(String sensor)
	{		
		// now read the sensor values
		switch(sensor.charAt(1))
		{
		case 'O':
		    return "My current longitude is " + String.valueOf(Longitude) + " degrees";
		case 'L':
		    return "My current latitude is " + String.valueOf(Latitude) + " degrees";
		case 'A':
		    return "My current altitude is " + String.valueOf(Altitude) + " meters";
		case 'S':
		    return "My current speed is " + String.valueOf(Speed) + " km/h";
		case 'C':
		    return "My current bearing is " + String.valueOf(Bearing) + " degrees";
		case 'I':
		    return "My current location is http://maps.google.com?q=(" + String.valueOf(Latitude) + "," + String.valueOf(Longitude) + ")";
		default:
			return null;
		}
	}
	
	/***********************************************************************
	 Function    : Discover()
	 Input       : 
	 Output      : string with discovery information
	 Return      : 
	 Description : provides discovery information of this particular acquisition 
	 			   module 
	***********************************************************************/
	public void Discover()
	{
		if (enableGPS == true)
		{
			   SensorRepository.insertSensor(new String("GO"), new String("degrees"), new String("GPS longitude"), new String("int"), -5, -18000000, 18000000, 0, this);
			   SensorRepository.insertSensor(new String("GL"), new String("degrees"), new String("GPS latitude"), new String("int"), -5, -18000000, 18000000, 0, this);
			   SensorRepository.insertSensor(new String("GA"), new String("m"), new String("GPS altitude"), new String("int"), -1, -200, 150000, 0, this);
			   SensorRepository.insertSensor(new String("GI"), new String("txt"), new String("GPS info"), new String("str"), 0, 0, 1, 0, this);
			   SensorRepository.insertSensor(new String("GC"), new String("degrees"), new String("GPS course"), new String("int"), -1, 0, 3600, 0, this);
			   SensorRepository.insertSensor(new String("GS"), new String("m/s"), new String("GPS speed"), new String("int"), -1, 0, 10000, 0, this);
		}		
	}
	
	public GPSHandler(Context activity)
	{
		// store for later
		nors = activity;
		
		// get preferences
		polltime	= HandlerManager.readRMS_i("LocationHandler::LocationPoll", 30) * 1000;
		updatemeter	= HandlerManager.readRMS_i("LocationHandler::LocationUpdate", 100);
		// read whether or not we need to enable GPS
		enableGPS = HandlerManager.readRMS_b("LocationHandler::GPSON", false);
		useWifi = HandlerManager.readRMS_b("LocationHandler::UseWifi", false);
		
		if (enableGPS == false)
			return;
			
		try
		{
			mReceiver = new LocationReceiver();
			manager = (LocationManager)nors.getSystemService(Context.LOCATION_SERVICE);
			enableGPS = true;
			// arm semaphores
			wait(longitude_semaphore); 
			wait(latitude_semaphore); 
			wait(altitude_semaphore); 
			wait(bearing_semaphore); 
			wait(speed_semaphore); 
			wait(full_semaphore); 
		}
		catch (Exception e) 
		{
			SerialPortLogger.debug("GPSHandler:Constructor: Exception: " + e.toString());
			enableGPS = false;
		}
	}
	
	public void destroyHandler()
	{
		// remove location updates if they were started
		if (startedGPS == true)
			manager.removeUpdates(mReceiver);
	}

	// read GPS via location API!
	private byte[] GPSReading(String sensor)
	{
		int value = 0;
		boolean read = false;
		StringBuffer GPSreadings = null;
		byte[] reading = null;
		
		try
		{			
			// now read the sensor values
			switch(sensor.charAt(1))
			{
			case 'O':
				wait(longitude_semaphore); 
				value = (int)(Longitude * 100000);
				read = true;
				break;
			case 'L':
				wait(latitude_semaphore); 
				value = (int)(Latitude * 100000);
				read = true;
				break;
			case 'A':
				wait(altitude_semaphore); 
				value = (int)(Altitude * 10);
				read = true;
				break;
			case 'S':
				wait(speed_semaphore); 
				value = (int)(Speed * 10);
				read = true;
				break;
			case 'C':
				wait(bearing_semaphore); 
				value = (int)(Bearing * 10);
				read = true;
				break;
			case 'I':
				wait(full_semaphore); 
				// at least one value different than old one?
				if (Longitude != oldLongitude || Latitude != oldLatitude || Altitude != oldAltitude)
				{
					GPSreadings = new StringBuffer(sensor);
					GPSreadings.append(Double.toString(Longitude) + ":" + Double.toString(Latitude) + ":" + Double.toString(Altitude));
					
					oldLongitude 	= Longitude;
					oldLatitude 	= Latitude;
					oldAltitude 	= Altitude;
				}
				break;
			default:
				GPSreadings = null;
				break;
			}

			// any GPS readings?
			if (GPSreadings != null)
			{
				reading = GPSreadings.toString().getBytes();
				GPSreadings = null;					
				return reading;
			}
			if (read == true)
			{
				reading = new byte[6];
				reading[0] = (byte)sensor.charAt(0);
				reading[1] = (byte)sensor.charAt(1);
				reading[2] = (byte)((value>>24) & 0xff);
				reading[3] = (byte)((value>>16) & 0xff);
				reading[4] = (byte)((value>>8) & 0xff);
				reading[5] = (byte)(value & 0xff);
				return reading;
			}
		}
		catch(Exception e)
		{
			return null;
		}
		
		return null;
	}
		
	// The Handler that gets information back from the other threads, initializing GPS
	// We use a handler here to allow for the Acquire() function, which runs in a different thread, to issue an initialization of the GPS
	// since requestLocationUpdates() can only be called from the main Looper thread!!
	public final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {        	
           switch (msg.what) 
           {
           case INIT_GPS:
        	   if (manager!=null)
        	   {
        		   // request location updates
        		   manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, polltime, (float)updatemeter, mReceiver);  
        		   if (useWifi == true)
            		   manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, polltime, (float)updatemeter, mReceiver);  
        		   // signal starting to Acquire() thread
        		   startedGPS = true;
        	   }
	           break;  
           default:  
           	break;
           }
       }
    };

    private class LocationReceiver implements LocationListener 
    {
        public void	 onLocationChanged(Location location)        
        {
        	boolean validLocation = true;
        	long speed;
			// get current timestamp
        	long newTime = System.currentTimeMillis();
        	float elapsed;
        	
        	if (location != null)
        	{
        		// is there an old location?
        		if (oldLocation != null)
        		{
        			// time elapsed since last reading
        			elapsed = (float)(newTime-oldTime)/1000;
        			// speed in km/h
        			speed = (long)(((oldLocation.distanceTo(location)/elapsed)*3600)/1000);

        			// let's assume we're not flying
        			if (speed>1000)
        				validLocation = false;
        		}
        		
        		// do we have a valid location?
        		if (validLocation == true)
        		{
					Longitude 	= location.getLongitude();
					Latitude 	= location.getLatitude();
					Altitude 	= location.getAltitude();
					if (location.hasSpeed())
						Speed		= (double)location.getSpeed();
					if (location.hasBearing())
						Bearing		= (double)location.getBearing();
					
					// now release the semaphores
					longitude_semaphore.release(); 
					latitude_semaphore.release(); 
					altitude_semaphore.release(); 
					speed_semaphore.release(); 
					bearing_semaphore.release(); 
					full_semaphore.release(); 
					
					// save current location for later
					oldLocation = location;
					oldTime = newTime;
        		}
        	}
        }
        
        public void onProviderDisabled(String provider)
        {
        	
        }
        public void	 onProviderEnabled(String provider)
        {
        	
        }
        public void	 onStatusChanged(String provider, int status, Bundle extras)
        {
        	
        }
    };
}
