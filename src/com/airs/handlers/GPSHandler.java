/*
Copyright (C) 2010-2011, Dirk Trossen, airs@dirk-trossen.de
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
package com.airs.handlers;

import java.util.concurrent.Semaphore;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.airs.helper.SerialPortLogger;
import com.airs.helper.Waker;
import com.airs.platform.HandlerManager;
import com.airs.platform.History;
import com.airs.platform.Sensor;
import com.airs.platform.SensorRepository;

// this handler has been separated from the standard location handler 
// in order to prevent blocking of the other sensor types when not being able to get a GPS fix!
public class GPSHandler implements com.airs.handlers.Handler, Runnable
{
	public static final int INIT_GPS 	= 1;
	public static final int KILL_GPS 	= 2;
	public static final int RESET_AGPS 	= 3;

	Context airs;
	// are these there?
	private boolean enableGPS = false, startedGPS = false, useWifi = false;
	private boolean   shutdown = false;
	// polltime
	private int 		polltime = 10000, updatemeter = 100;
	// sensor data
    private double Longitude = -1, Latitude = -1, Altitude = -1, Speed, Bearing; 
    private double oldLongitude = -1, oldLatitude = -1, oldAltitude = -1;
    private Location oldLocation = null;
    private long oldTime;
    private long agpsForce;
    private boolean agps_download = false;
	private String[] adaptiveWifis;
	private boolean  adaptiveWifi = false, nearby = false;
	private Thread runnable;
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
		Waker.sleep(millis);
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
 	    // are we shutting down?
		if (shutdown == true)
			return null;

		// send handler message for reset AGPS?
		if (agps_download == false && agpsForce != 0)
		{
	        mHandler.sendMessage(mHandler.obtainMessage(RESET_AGPS));	
	        agps_download = true;
		}

		// need to start GPS?
		if (startedGPS == false && nearby == false)
		{
			// send message to handler thread to start GPS
	        Message msg = mHandler.obtainMessage(INIT_GPS);
	        mHandler.sendMessage(msg);	 
	        
	        // wait until done
	        while (startedGPS == false)
	        	sleep(100);
		}

		// need to start the adaptive WiFi thread?
		if (adaptiveWifi == true && runnable == null)
		{
			runnable = new Thread(this);
			runnable.start();
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
	public String Share(String sensor)
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
	 Function    : History()
	 Input       : sensor input for specific history views
	 Output      :
	 Return      :
	 Description : calls historical views
	***********************************************************************/
	public void History(String sensor)
	{
		// now read the sensor values
		switch(sensor.charAt(1))
		{
		case 'A':
		    History.timelineView(airs, "GPS altitude [m]", sensor);
		    break;
		case 'S':
		    History.timelineView(airs, "GPS speed [m/s]", sensor);
		    break;
		case 'I':
			History.mapView(airs, "GPS traces", sensor);
			break;
		default:
			return;
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
			   SensorRepository.insertSensor(new String("GO"), new String("degrees"), new String("GPS longitude"), new String("int"), -5, -18000000, 18000000, false, 0, this);
			   SensorRepository.insertSensor(new String("GL"), new String("degrees"), new String("GPS latitude"), new String("int"), -5, -18000000, 18000000, false, 0, this);
			   SensorRepository.insertSensor(new String("GA"), new String("m"), new String("GPS altitude"), new String("int"), -1, -200, 150000, true, 0, this);
			   SensorRepository.insertSensor(new String("GI"), new String("txt"), new String("GPS info"), new String("str"), 0, 0, 1, true, 0, this);
			   SensorRepository.insertSensor(new String("GC"), new String("degrees"), new String("GPS course"), new String("int"), -1, 0, 3600, false, 0, this);
			   SensorRepository.insertSensor(new String("GS"), new String("m/s"), new String("GPS speed"), new String("int"), -1, 0, 10000, true, 0, this);
		}		
	}
	
	public GPSHandler(Context activity)
	{
		// store for later
		airs = activity;
		
		// get preferences
		polltime	= HandlerManager.readRMS_i("LocationHandler::LocationPoll", 30) * 1000;
		updatemeter	= HandlerManager.readRMS_i("LocationHandler::LocationUpdate", 100);
		// read whether or not we need to enable GPS
		enableGPS = HandlerManager.readRMS_b("LocationHandler::GPSON", false);
		useWifi = HandlerManager.readRMS_b("LocationHandler::UseWifi", false);
		agpsForce = HandlerManager.readRMS_i("LocationHandler::AGPSForce", 3) * 3600*1000;
		adaptiveWifi = HandlerManager.readRMS_b("LocationHandler::AdaptiveGPS", false);
		String storedWifis = HandlerManager.readRMS("LocationHandler::AdaptiveGPS_WiFis", null);
		
		agps_download = false;
		
		// if no GPS wanted, return
		if (enableGPS == false)
			return;
		
		// retrieve individual WiFis now, if adaptation is wanted
		if (adaptiveWifi == true)
			if (storedWifis != null)	
				adaptiveWifis = storedWifis.split("::");

		try
		{
			mReceiver = new LocationReceiver();
			manager = (LocationManager)airs.getSystemService(Context.LOCATION_SERVICE);
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
		// release all semaphores for unlocking the Acquire() threads
		longitude_semaphore.release();
		latitude_semaphore.release();
		altitude_semaphore.release();
		bearing_semaphore.release();
		speed_semaphore.release();
		full_semaphore.release();
		
		// signal shutdown
		shutdown = true;

		// remove location updates if they were started
		if (startedGPS == true)
			manager.removeUpdates(mReceiver);
		
		// disrupt adaptive GPS thread
		if (runnable != null)
			runnable.interrupt();
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
				if (startedGPS == false)
				{
					SensorRepository.setSensorStatus(sensor, Sensor.SENSOR_SUSPEND, "adaptive GPS", Thread.currentThread());
					return null;
				}				
				value = (int)(Longitude * 100000);
				read = true;
				break;
			case 'L':
				wait(latitude_semaphore); 
				if (startedGPS == false)
				{
					SensorRepository.setSensorStatus(sensor, Sensor.SENSOR_SUSPEND, "adaptive GPS", Thread.currentThread());
					return null;
				}
				
				value = (int)(Latitude * 100000);
				read = true;
				break;
			case 'A':
				wait(altitude_semaphore); 
				if (startedGPS == false)
				{
					SensorRepository.setSensorStatus(sensor, Sensor.SENSOR_SUSPEND, "adaptive GPS", Thread.currentThread());
					return null;
				}
				
				value = (int)(Altitude * 10);
				read = true;
				break;
			case 'S':
				wait(speed_semaphore); 
				if (startedGPS == false)
				{
					SensorRepository.setSensorStatus(sensor, Sensor.SENSOR_SUSPEND, "adaptive GPS", Thread.currentThread());
					return null;
				}
				value = (int)(Speed * 10);
				read = true;
				break;
			case 'C':
				wait(bearing_semaphore); 
				if (startedGPS == false)
				{
					SensorRepository.setSensorStatus(sensor, Sensor.SENSOR_SUSPEND, "adaptive GPS", Thread.currentThread());
					return null;
				}
				value = (int)(Bearing * 10);
				read = true;
				break;
			case 'I':
				wait(full_semaphore); 
				if (startedGPS == false)
				{
					SensorRepository.setSensorStatus(sensor, Sensor.SENSOR_SUSPEND, "adaptive GPS", Thread.currentThread());
					return null;
				}
				GPSreadings = new StringBuffer(sensor);
				GPSreadings.append(Double.toString(Longitude) + ":" + Double.toString(Latitude) + ":" + Double.toString(Altitude));
				return GPSreadings.toString().getBytes().clone();
			default:
				break;
			}

			// anything read?
			if (read == true)
			{
				reading = new byte[6];
				reading[0] = (byte)sensor.charAt(0);
				reading[1] = (byte)sensor.charAt(1);
				reading[2] = (byte)((value>>24) & 0xff);
				reading[3] = (byte)((value>>16) & 0xff);
				reading[4] = (byte)((value>>8) & 0xff);
				reading[5] = (byte)(value & 0xff);
				return reading.clone();
			}
		}
		catch(Exception e)
		{
			return null;
		}
		
		return null;
	}
	
	/**
	 * 
	 * This thread keeps on listening to WiFis 
	*/
	public void run() 
	{
		byte [] data;
		int i, j;
		boolean found;
		StringBuffer SSID;
		Sensor wifi = SensorRepository.findSensor("WI");
		WifiHandler handler = (WifiHandler)wifi.handler;		// get handle to WiFi handler
		
		try
		{
			// is the WI sensor in the repository?
			if (wifi != null)
			{
				while(shutdown == false)
				{
					// wait for WiFi handler to have found something 
					wait(handler.nearby_semaphore);
					
					// read current WiFi APs directly from handler
					SSID = handler.SSID_reading;
					
					// anything there?
					if (SSID != null)
					{
						data = SSID.toString().getBytes();
						
						// create string with readings
						String wifis = new String(data, 2, data.length - 2);
						
						// any WiFis around?
						if (wifis != null)
						{
							String APs[] = wifis.split("\n");
							
							found = false;
							// look for discovered AP in list of selected WiFis of settings
							for (i=0;i<APs.length && found == false;i++)
								for (j=0;j<adaptiveWifis.length && found == false;j++)
									if (APs[i].compareTo(adaptiveWifis[j]) == 0)
										found = true;
								
							// is device near a WiFi that is selected for suppressing GPS?
							if (found == true)
							{
								nearby = true;
								
								// if GPS is started, stop it
								if (startedGPS == true)
								{
									mHandler.sendMessage(mHandler.obtainMessage(KILL_GPS));

									startedGPS = false;
									
									// now release the semaphores to return the Acquire() threads
									longitude_semaphore.release(); 
									latitude_semaphore.release(); 
									altitude_semaphore.release(); 
									speed_semaphore.release(); 
									bearing_semaphore.release(); 
									full_semaphore.release(); 
									
									Log.e("Storica", "Found nearby wifi");
								}								
							}
							else	// signal that we are not nearby anything -> GPS will start at the next round again
							{
								nearby = false;
								
								if (startedGPS == false)
								{
									mHandler.sendMessage(mHandler.obtainMessage(INIT_GPS));	
									
									startedGPS = true;
							
									SensorRepository.setSensorStatus("GO", Sensor.SENSOR_VALID, null, null);
									SensorRepository.setSensorStatus("GL", Sensor.SENSOR_VALID, null, null);
									SensorRepository.setSensorStatus("GA", Sensor.SENSOR_VALID, null, null);
									SensorRepository.setSensorStatus("GS", Sensor.SENSOR_VALID, null, null);
									SensorRepository.setSensorStatus("GC", Sensor.SENSOR_VALID, null, null);
									SensorRepository.setSensorStatus("GI", Sensor.SENSOR_VALID, null, null);
									
									// re-arm semaphores
									longitude_semaphore.tryAcquire(); 
									latitude_semaphore.tryAcquire(); 
									altitude_semaphore.tryAcquire(); 
									speed_semaphore.tryAcquire(); 
									bearing_semaphore.tryAcquire(); 
									full_semaphore.tryAcquire(); 
									
									Log.e("Storica", "Did not find nearby wifi");
								}							
							}
						}
					}					
				}				
			}
		}
		catch(Exception e)
		{
			return;
		}
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
        	   // are we shutting down?
        	   if (shutdown == true)
        		   return;

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
           case KILL_GPS:
        	   // are we shutting down?
        	   if (shutdown == true)
        		   return;

        	   if (manager!=null)
        		   manager.removeUpdates(mReceiver);
        	   
    		   // signal starting to Acquire() thread
    		   startedGPS = false;
	           break;  
           case RESET_AGPS:
        	   try
        	   {
        	    Bundle bundle = new Bundle();
		        manager.sendExtraCommand("gps", "force_xtra_injection", bundle);
		        manager.sendExtraCommand("gps", "force_time_injection", bundle);
        	   }
        	   catch(Exception e)
        	   {
        	   }
        	   // do again repeatedly
        	   mHandler.sendMessageDelayed(mHandler.obtainMessage(RESET_AGPS), agpsForce);
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
        	boolean changed = false;
        	        	
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
					
					// don't do anything if we get a null reading for some reason
					if (Longitude == 0.0f && Latitude == 0.0f)
						return;
					
					// any speed information?
					if (location.hasSpeed())
					{
						Speed		= (double)location.getSpeed();
						speed_semaphore.release(); 
					}

					// any bearing information?
					if (location.hasBearing())
					{
						Bearing		= (double)location.getBearing();
						bearing_semaphore.release();
					}
					
					// at least one value different than old one?
					if (Longitude != oldLongitude)
					{
						oldLongitude 	= Longitude;
						longitude_semaphore.release(); 
						changed = true;
					}
					
					// at least one value different than old one?
					if (Latitude != oldLatitude)
					{
						oldLatitude 	= Latitude;
						latitude_semaphore.release(); 
						changed = true;
					}
					
					// at least one value different than old one?
					if (Altitude != oldAltitude)
					{
						oldAltitude 	= Altitude;	
						altitude_semaphore.release(); 
						changed = true;
					}
				
					// has at least one changed ?
					if (changed == true)
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
