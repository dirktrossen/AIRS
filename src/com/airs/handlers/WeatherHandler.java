/*
Copyright (C) 2011-2012, Dirk Trossen, airs@dirk-trossen.de

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

import java.net.URL;
import java.util.concurrent.Semaphore;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.airs.R;
import com.airs.helper.Waker;
import com.airs.platform.HandlerManager;
import com.airs.platform.History;
import com.airs.platform.SensorRepository;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Class to read weather related sensors, specifically the VT, VF, VH, VC, VW, VI sensor
 * @see Handler
 */
public class WeatherHandler implements com.airs.handlers.Handler, Runnable
{
	private static final int INIT_GPS 		= 1;
	private static final int KILL_GPS 		= 2;
	private static final int INIT_RECEIVER 	= 4;
	
	private Context nors;
	private boolean weather_enabled;
	
	private int temperature_c, temperature_f = 0;
	private int humidity = 0;
	private String condition;
	private String wind;
	// XML stuff
	private ExampleHandler myWeatherHandler;
	private XMLReader XMLreader;
	private SAXParser sp;
	// threading for XML polling
	private Thread runnable = null;
	private boolean	  running = true;
	private boolean   shutdown = false;
	private int		  polltime = 10000;
	private int		  updatemeter = 1000;
	// location stuff
	private LocationManager manager;
	private LocationListener mReceiver;
    private double Longitude = 0, Latitude = 0; 
    private boolean startedLocation = false;
    private boolean first_fix = true, movedAway = false, connectivity_listener = false;
    private Location old_location;
    // connectvity stuff
    private ConnectivityManager cm;
	// semaphores for multi-threading
	private Semaphore location_semaphore 	= new Semaphore(1);
	private Semaphore temp_c_semaphore 		= new Semaphore(1);
	private Semaphore temp_f_semaphore 		= new Semaphore(1);
	private Semaphore hum_semaphore		 	= new Semaphore(1);
	private Semaphore cond_semaphore	 	= new Semaphore(1);
	private Semaphore wind_semaphore	 	= new Semaphore(1);
	private Semaphore info_semaphore	 	= new Semaphore(1);
	private Semaphore connectivity_semaphore= new Semaphore(1);

	// boolean for which element is parsed
	private boolean temp_c_element;
	private boolean temp_f_element;
	private boolean hum_element;
	private boolean cond_element;
	private boolean wind_element;
	/**
	 * Sleep function 
	 * @param millis
	 */
	private void sleep(long millis) 
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
	
	/**
	 * Method to acquire sensor data
	 * Here, we start the thread for retrieving the weather and switch on the GPS, if not done before
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.airs.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public byte[] Acquire(String sensor, String query)
	{		
		byte[] readings = null;

 	    // are we shutting down?
		if (shutdown == true)
			return null;
		
		// no thread started yet?
		if (runnable == null)
		{
			runnable = new Thread(this);
			runnable.start();	
			
			// initialise receiver for connectivity change
			Message msg = mHandler.obtainMessage(INIT_RECEIVER);
			mHandler.sendMessage(msg);		
		}
		
		// temperature in Celcius
		if(sensor.compareTo("VT") == 0)
		{
			// wait for semaphore
			wait(temp_c_semaphore); 
				
			readings = new byte[6];
			readings[0] = (byte)sensor.charAt(0);
			readings[1] = (byte)sensor.charAt(1);
			readings[2] = (byte)((temperature_c>>24) & 0xff);
			readings[3] = (byte)((temperature_c>>16) & 0xff);
			readings[4] = (byte)((temperature_c>>8) & 0xff);
			readings[5] = (byte)(temperature_c & 0xff);

			return readings;
		}

		// temperature in Farenheit
		if(sensor.compareTo("VF") == 0)
		{
			// wait for semaphore
			wait(temp_f_semaphore); 
				
			readings = new byte[6];
			readings[0] = (byte)sensor.charAt(0);
			readings[1] = (byte)sensor.charAt(1);
			readings[2] = (byte)((temperature_f>>24) & 0xff);
			readings[3] = (byte)((temperature_f>>16) & 0xff);
			readings[4] = (byte)((temperature_f>>8) & 0xff);
			readings[5] = (byte)(temperature_f & 0xff);

			return readings;
		}
		
		// Humidity
		if(sensor.compareTo("VH") == 0)
		{
			// wait for semaphore
			wait(hum_semaphore); 
				
			readings = new byte[6];
			readings[0] = (byte)sensor.charAt(0);
			readings[1] = (byte)sensor.charAt(1);
			readings[2] = (byte)((humidity>>24) & 0xff);
			readings[3] = (byte)((humidity>>16) & 0xff);
			readings[4] = (byte)((humidity>>8) & 0xff);
			readings[5] = (byte)(humidity & 0xff);

			return readings;
		}

		// Conditions
		if(sensor.compareTo("VC") == 0)
		{
			// wait for semaphore
			wait(cond_semaphore); 

			String vc = new String("VC" + condition);		
			return vc.getBytes();
		}
		
		// Wind Conditions
		if(sensor.compareTo("VW") == 0)
		{
			// wait for semaphore
			wait(wind_semaphore); 

			String vw = new String("VW" + wind);		
			return vw.getBytes();
		}

		// All information together
		if(sensor.compareTo("VI") == 0)
		{
			// wait for semaphore
			wait(info_semaphore); 

			String vi = new String("VI" + Double.toString(Latitude) + ":" + Double.toString(Longitude) + ":" + Integer.toString(temperature_c) + ":" + Integer.toString(temperature_f)  + ":" + Integer.toString(humidity) + ":" + condition + ":" + wind);		
			return vi.getBytes();
		}
		return null;
	}
	
	/**
	 * Method to share the last value of the given sensor
	 * @param sensor String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.airs.handlers.Handler#Share(java.lang.String)
	 */
	public String Share(String sensor)
	{		
		// temperature in Celcius
		if(sensor.compareTo("VT") == 0)
			return "The current temperature is " + String.valueOf(temperature_c) + " C" + "\nRecorded at " + "http://maps.google.com?q=(" + String.valueOf(Latitude) + "," + String.valueOf(Longitude) + ")";

		// temperature in Farenheit
		if(sensor.compareTo("VF") == 0)
			return "The current temperature is " + String.valueOf(temperature_f) + " F" + "\nRecorded at " + "http://maps.google.com?q=(" + String.valueOf(Latitude) + "," + String.valueOf(Longitude) + ")";
		
		// Humidity
		if(sensor.compareTo("VH") == 0)
			return "The current humidity is " + String.valueOf(humidity) + " %" + "\nRecorded at " + "http://maps.google.com?q=(" + String.valueOf(Latitude) + "," + String.valueOf(Longitude) + ")";

		// Conditions
		if(sensor.compareTo("VC") == 0)
			return "The current weather condition is " + condition + "\nRecorded at " + "http://maps.google.com?q=(" + String.valueOf(Latitude) + "," + String.valueOf(Longitude) + ")";
		
		// Wind Conditions
		if(sensor.compareTo("VW") == 0)
			return "The current wind condition is " + wind + "\nRecorded at " + "http://maps.google.com?q=(" + String.valueOf(Latitude) + "," + String.valueOf(Longitude) + ")";

		// All information together
		if(sensor.compareTo("VI") == 0)
			return "The current weather condition is " + String.valueOf(temperature_c) + " C (" + String.valueOf(temperature_f) + " F) with a humidity of " + String.valueOf(humidity) + " %, conditions '" + condition + "' and " + wind + " wind conditions!" + "\nRecorded at " + "http://maps.google.com?q=(" + String.valueOf(Latitude) + "," + String.valueOf(Longitude) + ")";

		return null;		
	}

	/**
	 * Method to view historical chart of the given sensor symbol
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.airs.handlers.Handler#History(java.lang.String)
	 */
	public void History(String sensor)
	{
		// temperature in Celcius
		if(sensor.compareTo("VT") == 0)
			History.timelineView(nors, "Temperature [C]", "VT");

		// temperature in Farenheit
		if(sensor.compareTo("VF") == 0)
			History.timelineView(nors, "Temperature [F]", "VF");
		
		// Humidity
		if(sensor.compareTo("VH") == 0)
			History.timelineView(nors, "Humidity [&]", "VH");
		
		// Weather info
		if(sensor.compareTo("VI") == 0)
			History.mapView(nors, "Weather on the map", "VI");

	}
	
	/**
	 * Method to discover the sensor symbols support by this handler, if the weather is supported
	 * As the result of the discovery, appropriate {@link com.airs.platform.Sensor} entries will be added to the {@link com.airs.platform.SensorRepository}
	 * @see com.airs.handlers.Handler#Discover()
	 * @see com.airs.platform.Sensor
	 * @see com.airs.platform.SensorRepository
	 */
	public void Discover()
	{
		if (weather_enabled == true)
		{
			SensorRepository.insertSensor(new String("VT"), new String("C"), nors.getString(R.string.VT_d), nors.getString(R.string.VT_e), new String("int"), 0, -50, 100, true, 0, this);	    
			SensorRepository.insertSensor(new String("VF"), new String("F"), nors.getString(R.string.VF_d), nors.getString(R.string.VF_e), new String("int"), 0, -50, 100, true, 0, this);	    
			SensorRepository.insertSensor(new String("VH"), new String("%"), nors.getString(R.string.VH_d), nors.getString(R.string.VH_e), new String("int"), 0, 0, 100, true, 0, this);	    
			SensorRepository.insertSensor(new String("VC"), new String("txt"), nors.getString(R.string.VC_d), nors.getString(R.string.VC_e), new String("str"), 0, 0, 1, false, 0, this);	    
			SensorRepository.insertSensor(new String("VW"), new String("txt"), nors.getString(R.string.VW_d), nors.getString(R.string.VW_e), new String("str"), 0, 0, 1, false, 0, this);	    
			SensorRepository.insertSensor(new String("VI"), new String("txt"), nors.getString(R.string.VI_d), nors.getString(R.string.VI_e), new String("str"), 0, 0, 1, true, 0, this);	    
		}
	}
	
	/**
	 * run weather retrieval in separate thread
	 * @see java.lang.Runnable#run()
	 */
	public void run() 
	{
			InputSource input;
			int sleeptime = polltime; 
			long started = 0, ended = 0;
			boolean startReading = true;
			boolean firstReading = true;
	
			// run until destroyed
			while(running==true)
			{
				// sleep until next reading if it is not the first reading
				if (firstReading == false)
					sleep(sleeptime);
				
				// if killed during sleeping then return now!
				if (running == false)
					return;
				
				firstReading = false;
				
				// store timestamp when start reading
				if (startReading == true)
				{
					started = System.currentTimeMillis();
					startReading = false;
				}
	
				// only do location check etc if there's any connectivity to retrieve the weather!
				wait(connectivity_semaphore); 

				// if killed during sleeping then return now!
				if (running == false)
					return;

				// get current weather conditions
				try
				{

					// try to get current location
					if (manager!=null)
					{
					   // send message to handler thread to start GPS
				       Message msg = mHandler.obtainMessage(INIT_GPS);
				       mHandler.sendMessage(msg);	
	        		   // wait for update
	        		   wait(location_semaphore);
	        		   // now unregister again -> saves power
				       msg = mHandler.obtainMessage(KILL_GPS);
				       mHandler.sendMessage(msg);	
					}
					
					// if we moved, try to get weather
					if (movedAway == true)
					{
						// request update for that long,lat pair!
	//		            URL url = new URL("http://www.google.com/ig/api?weather=,,," + Integer.toString((int)(Latitude * 1000000)) + "," + Integer.toString((int)(Longitude * 1000000)));
	//		            URL url = new URL("http://free.worldweatheronline.com/feed/weather.ashx?q="+Double.toString(Latitude) + "," + Double.toString(Longitude) + "&format=xml&num_of_days=1&key=0f86de2f9c161417123108");
			            URL url = new URL("http://api.worldweatheronline.com/free/v1/weather.ashx?q="+Double.toString(Latitude) + "," + Double.toString(Longitude) + "&format=xml&num_of_days=1&key=st4dghppmrfbtcrhwggn76u8");
			            // 51914540,900690");

			            /* Parse the xml-data from our URL. */
			            input = new InputSource(url.openStream());
			            XMLreader.parse(input);	 
			            input = null;	
					}
		            
		            // get current timestamp to see how long the weather reading took all along
					ended = System.currentTimeMillis();
					// if there's still something to wait until next weather reading, do so
					if ((int)(ended - started) < polltime)
						sleeptime = polltime - (int)(ended - started);
					else
						sleeptime = 1;	// otherwise start right away
					
					// store start timestamp the next time around
					startReading = true;
				}
				catch(Exception e)
				{
					// try again to read/locate in 15 secs
					sleeptime = 15000;
				}
			}		
	}

	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's arming the semaphore and retrieving the reference to the XMLParser as well as a reference to the {@link android.location.LocationManager}
	 * @param airs Reference to the calling {@link android.content.Context}
	 */
	public WeatherHandler(Context airs)
	{
		this.nors = airs;

		// read polltime from preferences
		polltime = HandlerManager.readRMS_i("WeatherHandler::WeatherPoll", 10) * 1000 * 60;
		updatemeter	= HandlerManager.readRMS_i("WeatherHandler::LocationUpdate", 1000);

		try
		{
            /* Get a SAXParser from the SAXPArserFactory. */
            SAXParserFactory spf = SAXParserFactory.newInstance();
            sp = spf.newSAXParser();

            /* Get the XMLReader of the SAXParser we created. */
            XMLreader = sp.getXMLReader();
            /* Create a new ContentHandler and apply it to the XML-Reader*/
            myWeatherHandler = new ExampleHandler();
            XMLreader.setContentHandler(myWeatherHandler);

			// arm semaphores
			wait(temp_c_semaphore); 
			wait(temp_f_semaphore); 
			wait(hum_semaphore); 
			wait(cond_semaphore); 
			wait(wind_semaphore); 
			wait(info_semaphore); 
			wait(location_semaphore); 
			wait(connectivity_semaphore); 

    		// get location manager
			manager = (LocationManager)this.nors.getSystemService(Context.LOCATION_SERVICE);
			if (manager != null)
				mReceiver = new LocationReceiver();
			
			// get connectivity manager for checking connectivity
			cm = (ConnectivityManager)this.nors.getSystemService(Context.CONNECTIVITY_SERVICE);
			
			// everything ok to be used
			weather_enabled = true;
		}
		catch(Exception e)
		{
			// something went wrong
			weather_enabled = false;
		}
	}
	
	/**
	 * Method to release all handler resources
	 * Here, we unregister the location receiver, shut down the retrieval thread, unregister the connectivity receiver and release all semaphores
	 * @see com.airs.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		// signal shutdown
		shutdown = true;
		
		// release all semaphores for unlocking the Acquire() threads	
		temp_c_semaphore.release();
		temp_f_semaphore.release();
		hum_semaphore.release();
		cond_semaphore.release();
		wind_semaphore.release();
		info_semaphore.release();

		// release connectivity semaphore
		connectivity_semaphore.release();

		// signal thread to close down
		running = false;
		
		// kill thread
		if (runnable != null)
			runnable.interrupt();
		
		if (startedLocation == true)
			manager.removeUpdates(mReceiver);
		
		if (connectivity_listener == true)
			nors.unregisterReceiver(ConnectivityReceiver);
	}
		   
	// The Handler that gets information back from the other threads, initializing GPS
	// We use a handler here to allow for the Acquire() function, which runs in a different thread, to issue an initialization of the GPS
	// since requestLocationUpdates() can only be called from the main Looper thread!!
	private final Handler mHandler = new Handler() 
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

    		   // request location updates
        	   if (manager!=null)
        	   {
        		   try
        		   {     
        			   manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, (float)0, mReceiver);  
        		   }
        		   catch(Exception e)
        		   {
        		   }
        		   startedLocation = true;
        	   }
	           break;  
           case KILL_GPS:
    		   // request location updates
        	   if (manager!=null)
        	   {
        		   manager.removeUpdates(mReceiver);  
        		   startedLocation = false;
        	   }
	           break;  
           case INIT_RECEIVER:
        	   IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
	   		   nors.registerReceiver(ConnectivityReceiver, intentFilter);
	   		   connectivity_listener = true;
	   		   break;
           default:  
           	break;
           }
       }
    };

    private class ExampleHandler extends DefaultHandler
    {    	 
    	private boolean current_cond = false;

        @Override
        public void startDocument() throws SAXException 
        {
        	current_cond = false;
        }

 
        /** Gets to be called on opening tags like:
         * <tag>
         * Can provide attribute(s), when xml was like:
         * <tag attribute="attributeValue">*/
        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException 
        {
        		// check for entering current conditions part of message
	            if (localName.equals("current_condition")) 
	            {
					current_cond = true;
	            }
	            
                if (localName.equals("temp_C") && current_cond==true) 
                	temp_c_element = true;
                if (localName.equals("temp_F") && current_cond==true) 
                	temp_f_element = true;
                if (localName.equals("humidity") && current_cond==true) 
                	hum_element = true;
                if (localName.equals("weatherDesc") && current_cond==true) 
                	cond_element = true;
                if (localName.equals("windspeedMiles") && current_cond==true) 
                	wind_element = true;
        }
        
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException 
        {
			if (localName.equals("current_condition"))
			{
				current_cond = false;
                // signal possible Acquire() thread since we should have read everything now!
				info_semaphore.release(); 
			}
        }
        
        @Override 
        public void characters(char ch[], int start, int length) 
        { 
          String chars = new String(ch, start, length); 
          chars = chars.trim(); 

          if (temp_c_element == true) 
          {
              temperature_c = Integer.parseInt(chars);
              // signal possible Acquire() thread
              temp_c_semaphore.release(); 
              // clear element flag
              temp_c_element = false;
          }
          if (temp_f_element == true) 
          {
              temperature_f = Integer.parseInt(chars);
              // signal possible Acquire() thread
              temp_f_semaphore.release(); 
              // clear element flag
              temp_f_element = false;
          }
          if (hum_element == true) 
          {
        	  humidity = Integer.parseInt(chars);
              // signal possible Acquire() thread
              hum_semaphore.release(); 
              // clear element flag
              hum_element = false;
          }
          if (cond_element == true) 
          {
        	  condition = chars;
              // signal possible Acquire() thread
              cond_semaphore.release(); 
              // clear element flag
              cond_element = false;
          }
          if (wind_element == true) 
          {
        	  wind = chars + "mph";
              // signal possible Acquire() thread
              wind_semaphore.release(); 
              // clear element flag
              wind_element = false;
          }
        } 
    }

	private final BroadcastReceiver ConnectivityReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // When connectivity changed...
            if (ConnectivityManager.CONNECTIVITY_ACTION.compareTo(action) == 0) 
            {
            	// check connectivity
    			NetworkInfo netInfo = cm.getActiveNetworkInfo();
    			if (netInfo != null)
    				if (netInfo.isConnected() == true)
    					connectivity_semaphore.release();			// release semaphore
    				else
    					connectivity_semaphore.drainPermits();		// drain semaphore

				return;
            }
        }
	};
	
    private class LocationReceiver implements LocationListener 
    {
        public void	 onLocationChanged(Location location)        
        {
        	if (location != null)
        	{				
				Longitude 	= location.getLongitude();
				Latitude 	= location.getLatitude();

				// shall we check for moving?
				if (updatemeter>0)
				{
					// is this the first fix?
					if (first_fix == false)
					{
						// have we moved?
						if (location.distanceTo(old_location)>updatemeter)
						{
							movedAway = true;
							old_location = location;
						}
						else
							movedAway = false;
					}
					else
					{
						first_fix = false;
						old_location = location;
						movedAway = true;
					}
				}
				else
					// we're always moving since we're always updating!
					movedAway = true;
				
				// now release the semaphores
				location_semaphore.release(); 
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

