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
import android.widget.Toast;

/**
 * @author trossen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WeatherHandler implements com.airs.handlers.Handler, Runnable
{
	public static final int INIT_GPS 		= 1;
	public static final int KILL_GPS 		= 2;
	public static final int TEXT_OUT 		= 3;
	public static final int INIT_RECEIVER 	= 4;
	String text_message;
	
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
	private Thread 	  runnable = null;
	private boolean	  running = true;
	private int		  polltime = 10000;
	private int		  updatemeter = 1000;
	long oldtime = 0;
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
		byte[] readings = null;

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

	/***********************************************************************
	 Function    : History()
	 Input       : sensor input for specific history views
	 Output      :
	 Return      :
	 Description : calls historical views
	***********************************************************************/
	public synchronized void History(String sensor)
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
		if (weather_enabled == true)
		{
			SensorRepository.insertSensor(new String("VT"), new String("C"), new String("Temperature (C)"), new String("int"), 0, -50, 100, true, 0, this);	    
			SensorRepository.insertSensor(new String("VF"), new String("F"), new String("Temperature (F)"), new String("int"), 0, -50, 100, true, 0, this);	    
			SensorRepository.insertSensor(new String("VH"), new String("%"), new String("Humidity"), new String("int"), 0, 0, 100, true, 0, this);	    
			SensorRepository.insertSensor(new String("VC"), new String("txt"), new String("Weather Conditions"), new String("str"), 0, 0, 1, false, 0, this);	    
			SensorRepository.insertSensor(new String("VW"), new String("txt"), new String("Wind"), new String("str"), 0, 0, 1, false, 0, this);	    
			SensorRepository.insertSensor(new String("VI"), new String("txt"), new String("Combined Weather info"), new String("str"), 0, 0, 1, true, 0, this);	    
		}
	}
	
	// run discovery in separate thread
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
//			            URL url = new URL("http://www.google.com/ig/api?weather=,,," + Integer.toString((int)(Latitude * 1000000)) + "," + Integer.toString((int)(Longitude * 1000000)));
		            URL url = new URL("http://free.worldweatheronline.com/feed/weather.ashx?q="+Double.toString(Latitude) + "," + Double.toString(Longitude) + "&format=xml&num_of_days=1&key=0f86de2f9c161417123108");
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

	
	public WeatherHandler(Context nors)
	{
		this.nors = nors;

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

    		// save current time and set so that first Acquire() will discover
    		oldtime = System.currentTimeMillis() - polltime;     
    		
    		// get location manager
			manager = (LocationManager)this.nors.getSystemService(Context.LOCATION_SERVICE);
			if (manager != null)
				mReceiver = new LocationReceiver();
			
			// get connectivity manager for checking connectivity
			cm = (ConnectivityManager)this.nors.getSystemService(Context.CONNECTIVITY_SERVICE);
			
			// arm semaphores
			wait(location_semaphore); 
			wait(temp_c_semaphore); 
			wait(temp_f_semaphore); 
			wait(hum_semaphore); 
			wait(cond_semaphore); 
			wait(wind_semaphore); 
			wait(info_semaphore); 
			wait(connectivity_semaphore); 

			// everything ok to be used
			weather_enabled = true;
		}
		catch(Exception e)
		{
			// something went wrong
			weather_enabled = false;
		}
	}
	
	public void destroyHandler()
	{
		// signal thread to close down
		running = false;
		// wake up thread
		try
		{
			runnable.interrupt();
		}
		catch(Exception e)
		{
		}
		
		if (startedLocation == true)
			manager.removeUpdates(mReceiver);
		
		if (connectivity_listener == true)
			nors.unregisterReceiver(ConnectivityReceiver);

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
        		   try
        		   {
        			   manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, (float)0, mReceiver);  
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
           case TEXT_OUT:
        	   Toast.makeText(nors, text_message, Toast.LENGTH_LONG).show();
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

    public class ExampleHandler extends DefaultHandler
    {    	 
    	private boolean current_cond = false;

        @Override
        public void startDocument() throws SAXException 
        {
        	current_cond = false;
        }

 
        /** Gets be called on opening tags like:
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

