/*
Copyright (C) 2008-2011, Dirk Trossen, airs@dirk-trossen.de
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
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.os.Handler;
import android.os.Message;

import com.airs.R;
import com.airs.platform.HandlerManager;
import com.airs.platform.History;
import com.airs.platform.SensorRepository;

/** 
 * Class to read internal phone sensors, specifically the Az, Pi, Ro, PR, LI, PU, TM, HU sensor
 * @see Handler
 */
public class PhoneSensorHandler implements com.airs.handlers.Handler
{
	private static final int INIT_LIGHT 		= 1;
	private static final int INIT_PROXIMITY 	= 2;
	private static final int INIT_ORIENTATION 	= 3;
	private static final int CLOSE_ORIENTATION 	= 4;
	private static final int INIT_PRESSURE 		= 5;
	private static final int INIT_TEMPERATURE	= 6;
	private static final int INIT_HUMIDITY 		= 7;

	private Context nors;
	private boolean sensor_enable = false;
	private boolean startedOrientation = false, startedLight = false, startedProximity = false, startedPressure = false, startedTemperature = false, startedHumidity = false;
	private SensorManager sensorManager;
	private android.hardware.Sensor Orientation, Proximity, Light, Pressure, Temperature, Humidity;
	// polltime for sensor
	private int polltime = 10000, polltime2 = 10000, polltime3 = 10000;
	// sensor values
	private float azimuth, roll, pitch, proximity, light, pressure, temperature, humidity;
	private float azimuth_old, roll_old, pitch_old, proximity_old, light_old, pressure_old, temperature_old, humidity_old;
	private Semaphore humidity_semaphore 	= new Semaphore(1);
	private Semaphore temperature_semaphore = new Semaphore(1);
	private Semaphore pressure_semaphore 	= new Semaphore(1);
	private Semaphore light_semaphore 		= new Semaphore(1);
	private Semaphore proximity_semaphore 	= new Semaphore(1);
	private Semaphore azimuth_semaphore 	= new Semaphore(1);
	private Semaphore roll_semaphore 		= new Semaphore(1);
	private Semaphore pitch_semaphore 		= new Semaphore(1);
	private boolean shutdown = false;
	
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

	/**
	 * Method to acquire sensor data
	 * For each sensor, we will fire a handler event to start the sensor reading, wait for the semaphore, then read the latest sensor value and fire another event to unregister the sensor callback -> this saves processing!
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.airs.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public byte[] Acquire(String sensor, String query)
	{
		boolean read = false;
		int value = 0;
		byte [] readings = null;
		boolean value_waiting;
		
		// anything there?
		if (sensor_enable == true)
		{
			// see which sensors are requested
			if (sensor.equals("Az") == true)
			{	
				// is there a value waiting?
				value_waiting = azimuth_semaphore.tryAcquire();
				
				// if not, switch on listener
				if (value_waiting == false)
				{
					// has Azimuth been started?
					if (startedOrientation == false)
					{
						// send message to handler thread to start orientation
				        Message msg = mHandler.obtainMessage(INIT_ORIENTATION);
				        mHandler.sendMessage(msg);	
					}
	
					wait(azimuth_semaphore); 
				}
				if (azimuth != azimuth_old)
				{
					read = true;
					value = (int)(azimuth*10);
					azimuth_old = azimuth;
				}
				
				// now toggle event listener off, if necessary
				if (value_waiting == false)
			        mHandler.sendMessage(mHandler.obtainMessage(CLOSE_ORIENTATION));						
			}
			
			if (read == false)
				if (sensor.equals("Pi") == true)
				{		
					// is there a value waiting?
					value_waiting = pitch_semaphore.tryAcquire();
					
					// if not, switch on listener
					if (value_waiting == false)
					{
						// has Pitch been started?
						if (startedOrientation == false)
						{
							// send message to handler thread to start GPS
					        Message msg = mHandler.obtainMessage(INIT_ORIENTATION);
					        mHandler.sendMessage(msg);	
						}
	
						wait(pitch_semaphore); 
					}
					if (pitch != pitch_old)
					{
						read = true;
						value = (int)(pitch*10);
						pitch_old = pitch;
					}
					
					// now toggle event listener off, if necessary
					if (value_waiting == false)
				        mHandler.sendMessage(mHandler.obtainMessage(CLOSE_ORIENTATION));						
				}

			if (read == false)
				if (sensor.equals("Ro") == true)
				{		
					// is there a value waiting?
					value_waiting = roll_semaphore.tryAcquire();
					
					// if not, switch on listener
					if (value_waiting == false)
					{
						// has Roll been started?
						if (startedOrientation == false)
						{
							// send message to handler thread to start GPS
					        Message msg = mHandler.obtainMessage(INIT_ORIENTATION);
					        mHandler.sendMessage(msg);	
						}
	
						wait(roll_semaphore); 
					}
					if (roll != roll_old)
					{
						read = true;
						value = (int)(roll*10);
						roll_old = roll;
					}

					// now toggle event listener off, if necessary
					if (value_waiting == false)
				        mHandler.sendMessage(mHandler.obtainMessage(CLOSE_ORIENTATION));						
				}	
			
			if (read == false)
				if (sensor.equals("PR") == true)
				{					
					// has Proximity been started?
					if (startedProximity == false)
					{
						proximity_semaphore.drainPermits();
						// send message to handler thread to start proximity
				        Message msg = mHandler.obtainMessage(INIT_PROXIMITY);
				        mHandler.sendMessage(msg);	
					}

					wait(proximity_semaphore); 
					if (proximity != proximity_old)
					{
						read = true;
						value = (int)(proximity*10);
						proximity_old = proximity;
					}
					
					// now toggle event listener off
			        Message msg = mHandler.obtainMessage(INIT_PROXIMITY);
			        mHandler.sendMessage(msg);						
				}		
			
			if (read == false)
				if (sensor.equals("LI") == true)
				{					
					// has Light been started?
					if (startedLight == false)
					{
						light_semaphore.drainPermits();
						// send message to handler thread to start light
				        Message msg = mHandler.obtainMessage(INIT_LIGHT);
				        mHandler.sendMessage(msg);	
					}

					wait(light_semaphore); 
					if (light != light_old)
					{
						read = true;
						value = (int)(light*10);
						light_old = light;
					}
					
					// now toggle event listener off
			        Message msg = mHandler.obtainMessage(INIT_LIGHT);
			        mHandler.sendMessage(msg);						
				}
			
			if (read == false)
				if (sensor.equals("PU") == true)
				{					
					// has Pressure been started?
					if (startedPressure == false)
					{
						pressure_semaphore.drainPermits();
						// send message to handler thread to start pressure
				        Message msg = mHandler.obtainMessage(INIT_PRESSURE);
				        mHandler.sendMessage(msg);	
					}

					wait(pressure_semaphore); 
					if (pressure != pressure_old)
					{
						read = true;
						value = (int)(pressure*10);
						pressure_old = pressure;
					}
					
					// now toggle event listener off
			        Message msg = mHandler.obtainMessage(INIT_PRESSURE);
			        mHandler.sendMessage(msg);						
				}	
			
			if (read == false)
				if (sensor.equals("HU") == true)
				{					
					// has Pressure been started?
					if (startedHumidity == false)
					{
						humidity_semaphore.drainPermits();
						// send message to handler thread to start pressure
				        Message msg = mHandler.obtainMessage(INIT_HUMIDITY);
				        mHandler.sendMessage(msg);	
					}

					wait(humidity_semaphore); 
					if (humidity != humidity_old)
					{
						read = true;
						value = (int)(humidity*10);
						humidity_old = humidity;
					}
					
					// now toggle event listener off
			        Message msg = mHandler.obtainMessage(INIT_HUMIDITY);
			        mHandler.sendMessage(msg);						
				}	
			
			if (read == false)
				if (sensor.equals("TM") == true)
				{					
					// has Pressure been started?
					if (startedTemperature == false)
					{
						temperature_semaphore.drainPermits();
						// send message to handler thread to start pressure
				        Message msg = mHandler.obtainMessage(INIT_TEMPERATURE);
				        mHandler.sendMessage(msg);	
					}

					wait(temperature_semaphore); 
					if (temperature != temperature_old)
					{
						read = true;
						value = (int)(temperature*10);
						temperature_old = temperature;
					}
					
					// now toggle event listener off
			        Message msg = mHandler.obtainMessage(INIT_TEMPERATURE);
			        mHandler.sendMessage(msg);						
				}	
		}
		
		// anything to report?
		if (read == true)
		{
			readings = new byte[6];
			readings[0] = (byte)sensor.charAt(0);
			readings[1] = (byte)sensor.charAt(1);
			readings[2] = (byte)((value>>24) & 0xff);
			readings[3] = (byte)((value>>16) & 0xff);
			readings[4] = (byte)((value>>8) & 0xff);
			readings[5] = (byte)(value & 0xff);
		}

		return readings;		
	}
	
	/**
	 * Method to share the last value of the given sensor
	 * @param sensor String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.airs.handlers.Handler#Share(java.lang.String)
	 */
	public String Share(String sensor)
	{		
		// see which sensors are requested
		if (sensor.equals("Az") == true)
			return "The current azimuth is " + String.valueOf(azimuth) + " degrees!";
		
		if (sensor.equals("Pi") == true)
			return "The current pitch is " + String.valueOf(pitch) + " degrees!";

		if (sensor.equals("Ro") == true)
			return "The current roll is " + String.valueOf(roll) + " degrees!";

		if (sensor.equals("PR") == true)
			return "The current proximity is " + String.valueOf(proximity);

		if (sensor.equals("LI") == true)
			return "The current light is " + String.valueOf(light) + " lux!";
		
		if (sensor.equals("PU") == true)
			return "The current pressure is " + String.valueOf(pressure) + " hPa!";

		if (sensor.equals("TM") == true)
			return "The current temperature is " + String.valueOf(temperature) + " C!";

		if (sensor.equals("HU") == true)
			return "The current rel. humidity is " + String.valueOf(humidity) + "%!";

		return null;		
	}
	
	/**
	 * Method to view historical chart of the given sensor symbol
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.airs.handlers.Handler#History(java.lang.String)
	 */
	public void History(String sensor)
	{
		// see which sensors are requested
		if (sensor.equals("Az") == true)
			History.timelineView(nors, "Azimuth [degrees]", sensor);
		
		if (sensor.equals("Pi") == true)
			History.timelineView(nors, "Pitch [degrees]", "Pi");

		if (sensor.equals("Ro") == true)
			History.timelineView(nors, "Roll [degrees]", "Ro");

		if (sensor.equals("LI") == true)
			History.timelineView(nors, "Light [Lux]", "LI");
		
		if (sensor.equals("PU") == true)
			History.timelineView(nors, "Pressure [hPa]", "PU");

		if (sensor.equals("TM") == true)
			History.timelineView(nors, "Temperature [C]", "TM");

		if (sensor.equals("HU") == true)
			History.timelineView(nors, "rel. Humidity [%]", "HU");
	}

	
	/**
	 * Method to discover the sensor symbols support by this handler
	 * As the result of the discovery, appropriate {@link com.airs.platform.Sensor} entries will be added to the {@link com.airs.platform.SensorRepository}, if sensors can be used and depending on which sensors are available on the device
	 * @see com.airs.handlers.Handler#Discover()
	 * @see com.airs.platform.Sensor
	 * @see com.airs.platform.SensorRepository
	 */
	public void Discover()
	{
		if (sensor_enable == true)
		{
		   if (Orientation != null)
		   {
			   SensorRepository.insertSensor(new String("Az"), new String("degrees"), nors.getString(R.string.AZ_d), nors.getString(R.string.AZ_e), new String("int"), -1, 0, 3600, true, polltime, this);
			   SensorRepository.insertSensor(new String("Pi"), new String("degrees"), nors.getString(R.string.PI_d), nors.getString(R.string.PI_e), new String("int"), -1, -1800, 1800, true, polltime, this);
			   SensorRepository.insertSensor(new String("Ro"), new String("degrees"), nors.getString(R.string.RO_d), nors.getString(R.string.RO_e), new String("int"), -1, -900, 900, true, polltime, this);	
		   }
		   if (Proximity != null)
			   SensorRepository.insertSensor(new String("PR"), new String("-"), nors.getString(R.string.PR_d), nors.getString(R.string.PR_e), new String("int"), -1, 0, 1000, false, polltime2, this);	
		   if (Light != null)
			   SensorRepository.insertSensor(new String("LI"), new String("Lux"), nors.getString(R.string.LI_d), nors.getString(R.string.LI_e), new String("int"), -1, 0, 50000, true, polltime3, this);	
		   if (Pressure != null)
			   SensorRepository.insertSensor(new String("PU"), new String("hPa"), nors.getString(R.string.PU_d), nors.getString(R.string.PU_e), new String("int"), -1, 0, 50000, true, polltime3, this);	
		   if (Temperature != null)
			   SensorRepository.insertSensor(new String("TM"), new String("C"), nors.getString(R.string.TM_d), nors.getString(R.string.TM_e), new String("int"), -1, 0, 50000, true, polltime3, this);	
		   if (Humidity != null)
			   SensorRepository.insertSensor(new String("HU"), new String("%"), nors.getString(R.string.HU_d), nors.getString(R.string.HU_e), new String("int"), -1, 0, 50000, true, polltime3, this);	
		}
	}
	
	/*
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's reading the preferences for the different polling intervals, checking the various sensors and arming the semaphores
	 * @param nors Reference to the calling {@link android.content.Context}
	 */
	public PhoneSensorHandler(Context activity)
	{
		nors = activity;
		
		// read polltime
		polltime  = HandlerManager.readRMS_i("PhoneSensorsHandler::OrientationPoll", 5) * 1000;
		polltime2 = HandlerManager.readRMS_i("PhoneSensorsHandler::ProximityPoll", 5) * 1000;
		polltime3 = HandlerManager.readRMS_i("PhoneSensorsHandler::EnvironmentalPoll", 10) * 1000;
		
		// try to open sensor services
		try
		{
			sensorManager = (SensorManager)nors.getSystemService(Context.SENSOR_SERVICE);
			Orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			Proximity 	= sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
			Light		= sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
			Pressure	= sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
			Temperature	= sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
			Humidity	= sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
			sensor_enable = true;
			// arm semaphores
			wait(humidity_semaphore); 
			wait(temperature_semaphore); 
			wait(pressure_semaphore); 
			wait(light_semaphore); 
			wait(proximity_semaphore); 
			wait(azimuth_semaphore); 
			wait(roll_semaphore); 
			wait(pitch_semaphore); 
		}
		catch(Exception e)
		{
			sensor_enable = false;
		}
	}
	
	/**
	 * Method to release all handler resources
	 * Here, we unregister the sensor listener, if registered, and release all semaphores
	 * @see com.airs.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		shutdown = true;
		// release all semaphores for unlocking the Acquire() threads
		humidity_semaphore.release();
		temperature_semaphore.release();
		pressure_semaphore.release();
		light_semaphore.release();
		proximity_semaphore.release();
		azimuth_semaphore.release();
		roll_semaphore.release();
		pitch_semaphore.release();

		if (startedLight == true || startedProximity == true || startedOrientation == true || startedPressure == true || startedTemperature == true || startedHumidity == true)
			sensorManager.unregisterListener(sensorlistener);	
	}

	// The Handler that gets information back from the other threads, initializing phone sensors
	// We use a handler here to allow for the Acquire() function, which runs in a different thread, to issue an initialization of the invidiaul sensors
	// since registerListener() can only be called from the main Looper thread!!
	private final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {   
    	   if (shutdown == true)
    		   return;
    	   
           switch (msg.what) 
           {
           case INIT_HUMIDITY:
        	   if (startedHumidity == false)
        		   startedHumidity = sensorManager.registerListener(sensorlistener, Humidity, SensorManager.SENSOR_DELAY_NORMAL);
        	   else
        	   {
        		   sensorManager.unregisterListener(sensorlistener, Humidity);
        		   startedHumidity = false;
        	   }
	           break;  
           case INIT_TEMPERATURE:
        	   if (startedTemperature == false)
        		   startedTemperature = sensorManager.registerListener(sensorlistener, Temperature, SensorManager.SENSOR_DELAY_NORMAL);
        	   else
        	   {
        		   sensorManager.unregisterListener(sensorlistener, Temperature);
        		   startedTemperature = false;
        	   }
	           break;  
           case INIT_PRESSURE:
        	   if (startedPressure == false)
        		   startedPressure = sensorManager.registerListener(sensorlistener, Pressure, SensorManager.SENSOR_DELAY_NORMAL);
        	   else
        	   {
        		   sensorManager.unregisterListener(sensorlistener, Pressure);
        		   startedPressure = false;
        	   }
	           break;  
           case INIT_LIGHT:
        	   if (startedLight == false)
        		   startedLight = sensorManager.registerListener(sensorlistener, Light, SensorManager.SENSOR_DELAY_NORMAL);
        	   else
        	   {
        		   sensorManager.unregisterListener(sensorlistener, Light);
        		   startedLight = false;
        	   }
	           break;  
           case INIT_PROXIMITY:
        	   if (startedProximity == false)
        		   startedProximity = sensorManager.registerListener(sensorlistener, Proximity, SensorManager.SENSOR_DELAY_NORMAL);
        	   else
        	   {
        		   sensorManager.unregisterListener(sensorlistener, Proximity);
        		   startedProximity = false;        		   
        	   }
	           break;  
           case INIT_ORIENTATION:
        	   if (startedOrientation == false)
        		   startedOrientation = sensorManager.registerListener(sensorlistener, Orientation, SensorManager.SENSOR_DELAY_NORMAL);
	           break;  
           case CLOSE_ORIENTATION:
        	   if (startedOrientation == true)
        	   {
	    		   sensorManager.unregisterListener(sensorlistener, Orientation);
	    		   startedOrientation = false;        		   
        	   }
	           break;  
           default:  
           	break;
           }
       }
    };

	
    private SensorEventListener sensorlistener = new SensorEventListener() 
    {
    	public void	 onSensorChanged(SensorEvent event)    
    	{
    		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION)
    		{
				 azimuth=event.values[0];
				 pitch=event.values[1];
				 roll=event.values[2];	

				 // now release the semaphores
				 azimuth_semaphore.release(); 
				 roll_semaphore.release(); 
				 pitch_semaphore.release(); 
    		}

    		if (event.sensor.getType() == Sensor.TYPE_PROXIMITY)
    		{
				 proximity=event.values[0];
				 // now release the semaphores
				 proximity_semaphore.release(); 
    		}
    		if (event.sensor.getType() == Sensor.TYPE_LIGHT)
    		{
				 light=event.values[0];
				 // now release the semaphores
				 light_semaphore.release(); 
    		}
    		if (event.sensor.getType() == Sensor.TYPE_PRESSURE)
    		{
				 pressure=event.values[0];
				 // now release the semaphores
				 pressure_semaphore.release(); 
    		}
    		if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE)
    		{
				 temperature=event.values[0];
				 // now release the semaphores
				 temperature_semaphore.release(); 
    		}
    		if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY)
    		{
				 humidity=event.values[0];
				 // now release the semaphores
				 humidity_semaphore.release(); 
    		}
       	}

		public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) 
		{
		}
    };  
}
