/*
Copyright (C) 2004-2006 Nokia Corporation
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

import java.util.List;
import java.util.concurrent.Semaphore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;

import com.airs.R;
import com.airs.helper.SerialPortLogger;
import com.airs.helper.Waker;
import com.airs.platform.HandlerManager;
import com.airs.platform.SensorRepository;

/** 
 * Class to read WiFi related sensors, specifically the WF, WI, WM, WS, WC sensor
 * @see Handler
 */
public class WifiHandler extends PhoneStateListener implements com.airs.handlers.Handler, Runnable
{
	private static final int INIT_WIFI = 1;

	private Context nors;
	// phone state classes
	private WifiManager wm;
	private WifiLock wifi_lock;

	// are these there?
	private boolean enable = false, enableWIFI = false, sleepWIFI = false, initialized = false;
	// polltime
	private int			polltime = 5000;
	private long  oldtime = 0;

	private boolean wifi_first = true, Wifi_scanning = false;
	/**
	 * current MAC reading
	 */
	public StringBuffer MAC_reading;	
	/**
	 * current SSID reading
	 */
	public StringBuffer SSID_reading;
	/**
	 * current RSSI reading
	 */
	public StringBuffer RSSI_reading;
	/**
	 * current WLAN reading
	 */
	public StringBuffer WLAN_reading;
	private int old_wifi_connected = -1, wifi_connected;
	/**
	 * semaphore for nearby thread - released by GPS handler
	 */
	public Semaphore nearby_semaphore 		= new Semaphore(1);
	/**
	 * semaphore for MAC reading - released by GPS handler
	 */
	public Semaphore mac_semaphore 			= new Semaphore(1);
	/**
	 * semaphore for SSID reading - released by GPS handler
	 */
	public Semaphore ssid_semaphore 		= new Semaphore(1);
	/**
	 * semaphore for RSSI reading - released by GPS handler
	 */
	public Semaphore rssi_semaphore 		= new Semaphore(1);
	/**
	 * semaphore for WiFi combined reading - released by GPS handler
	 */
	public Semaphore wlan_semaphore 		= new Semaphore(1);
	/**
	 * semaphore for WiFi connected reading - released by GPS handler
	 */
	public Semaphore connected_semaphore 	= new Semaphore(1);
	private Thread 		 runnable = null;
	private boolean		 running = false, shutdown = false;

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
	 * Initialise WiFi scanning thread, if not done before
	 * Also initialise the WC sensor thread
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.airs.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public byte[] Acquire(String sensor, String query)
	{	
		byte[] reading = null;

		// are we shutting down?
		if (shutdown == true)
			return null;

		// has WiFi been started?
		if (initialized == false)
		{
			// send message to handler thread to start WiFi
	        mHandler.sendMessage(mHandler.obtainMessage(INIT_WIFI));	
	        // wait for starting wifi
	        while (initialized == false)
	        	sleep(100);
		}
		
		// Discovery thread started for anything but WC?
		if (runnable == null && sensor.compareTo("WC") != 0)
		{
			running = true;
			runnable = new Thread(this);
			runnable.start();	
		}

		switch(sensor.charAt(1))
		{
		case 'M':
			wait(mac_semaphore); 

			reading = MAC_reading.toString().getBytes();
			break;
		case 'I':
			wait(ssid_semaphore); 

			reading = SSID_reading.toString().getBytes();
			break;
		case 'S':
			wait(rssi_semaphore); 

			reading = RSSI_reading.toString().getBytes();
			break;
		case 'F':
			wait(wlan_semaphore); 

			reading = WLAN_reading.toString().getBytes();
			break;
		case 'C':
			wait(connected_semaphore); 
						
			// did value change since last time?
			if (wifi_connected != old_wifi_connected)
			{
				reading = new byte[6];
				reading[0] = (byte)sensor.charAt(0);
				reading[1] = (byte)sensor.charAt(1);
				reading[2] = (byte)((wifi_connected>>24) & 0xff);
				reading[3] = (byte)((wifi_connected>>16) & 0xff);
				reading[4] = (byte)((wifi_connected>>8) & 0xff);
				reading[5] = (byte)(wifi_connected & 0xff);
				old_wifi_connected = wifi_connected;
			}
			else
				reading = null;
			break;
		default:
			reading = null;
		}	
		
		return reading;		
	}
	
	/** 
	 * Method to share the last value of the given sensor - here doing nothing
	 * @param sensor String of the sensor symbol to be shared
	 * @return human-readable string of the last sensor value
	 * @see com.airs.handlers.Handler#Share(java.lang.String)
	 */
	public String Share(String sensor)
	{		

		return null;		
	}
	
	/**
	 * Method to view historical chart of the given sensor symbol - here doing nothing
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.airs.handlers.Handler#History(java.lang.String)
	 */
	public void History(String sensor)
	{
	}

	/**
	 * Method to discover the sensor symbols support by this handler
	 * As the result of the discovery, appropriate {@link com.airs.platform.Sensor} entries will be added to the {@link com.airs.platform.SensorRepository}, if WiFi available
	 * @see com.airs.handlers.Handler#Discover()
	 * @see com.airs.platform.Sensor
	 * @see com.airs.platform.SensorRepository
	 */
	public void Discover()
	{
	    if (enable == true)
	    {		
			SensorRepository.insertSensor(new String("WF"), new String("txt"), nors.getString(R.string.WF_d), nors.getString(R.string.WF_e), new String("txt"), 0, 0, 1, false, 0, this);
			SensorRepository.insertSensor(new String("WI"), new String("SSID"), nors.getString(R.string.WI_d), nors.getString(R.string.WI_e), new String("txt"), 0, 0, 1, false, 0, this);	
			SensorRepository.insertSensor(new String("WM"), new String("MAC"), nors.getString(R.string.WM_d), nors.getString(R.string.WM_e), new String("txt"), 0, 0, 1, false, 0, this);
			SensorRepository.insertSensor(new String("WS"), new String("dBm"), nors.getString(R.string.WS_d), nors.getString(R.string.WS_e), new String("txt"), 0, 0, 1, false, 0, this);
			SensorRepository.insertSensor(new String("WC"), new String("boolean"), nors.getString(R.string.WC_d), nors.getString(R.string.WC_e), new String("int"), 0, 0, 1, false, 0, this);
		}	    
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, it's reading the preference settings for the polltime and enabling WiFi
	 * Then, it's getting a reference to the {@link android.net.wifi.WifiManager} and finally, it's arming the semaphores
	 * @param nors Reference to the calling {@link android.content.Context}
	 */
	public WifiHandler(Context nors)
	{
		this.nors = nors;
		
		polltime	= HandlerManager.readRMS_i("LocationHandler::WifiPoll", 30) * 1000;

		// read whether or not we need to enable Wifi
		enableWIFI = HandlerManager.readRMS_b("LocationHandler::WIFION", false);

		// read whether or not we need to have Wifi sleep
		sleepWIFI = HandlerManager.readRMS_b("LocationHandler::WIFISleep", false);
		
		// try getting wifi manager
		try
		{
			// now get wifi manager
			wm = (WifiManager)nors.getSystemService(Context.WIFI_SERVICE);
			if (wm!=null)
				enable = true;
			else
				enable = false;
			
			// arm semaphores
			wait(nearby_semaphore); 
			wait(mac_semaphore); 
			wait(ssid_semaphore); 
			wait(rssi_semaphore); 
			wait(wlan_semaphore); 
			wait(connected_semaphore); 
			
			// save current time and set so that first Acquire() will discover
			oldtime = System.currentTimeMillis() - polltime;
		}
		catch(Exception e)
		{
			enable = false;
		}
	}
	
	/**
	 * Method to release all handler resources
	 * Here, we release all semaphores, then shut down any acquisition threads and release the wifi lock, if held
	 * @see com.airs.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		// we are shutting down!
		shutdown = true;
		
		// release semaphores to unlock any Acquire() threads
		mac_semaphore.release(); 
		ssid_semaphore.release(); 
		rssi_semaphore.release(); 
		wlan_semaphore.release(); 
		connected_semaphore.release(); 

		// signal thread to close down
		if (running == true)
		{
			runnable.interrupt();
			running = false;
		}
		
		// unregister listeners		
		if (initialized == true)
			nors.unregisterReceiver(WifiReceiver);
		
		// release wifi lock
		if (wifi_lock != null)
          if (wifi_lock.isHeld() == true) 
              wifi_lock.release();
		
		// now release the semaphore for the adaptive GPS thread
		nearby_semaphore.release(); 
	}
	
	/**
	 * run WiFi discovery in separate thread
	 * @see java.lang.Runnable#run()
	 */
	public void run() 
	{
		long now;
		
		while(running==true)
		{
			now = System.currentTimeMillis();
			
			// try to discover when it's time to do so
			if (oldtime+polltime<=now)
			{
				oldtime = now;
				// shall we scan and is previous scan over?
				if (Wifi_scanning==false)
				{
					// is Wifi switched off?
					if (wm.isWifiEnabled() == false)
					{
						// create empty readings for next round
						MAC_reading 	= new StringBuffer("WM");	
						SSID_reading 	= new StringBuffer("WI");
						RSSI_reading 	= new StringBuffer("WS");
					    WLAN_reading	= new StringBuffer("WF");
					    
					    
					    // signal that reading is done
					    nearby_semaphore.release();
						mac_semaphore.release(); 
						rssi_semaphore.release(); 
						ssid_semaphore.release(); 
						wlan_semaphore.release(); 	

					}
					else
					{
						// if wifi is not locked, do so to prevent it from sleeping!
						if (sleepWIFI == false)
							if(wifi_lock.isHeld() == false)
					            wifi_lock.acquire();
					        
						// start next scan here!
						if (wm.startScan() == true)
							Wifi_scanning = true;
					}
				}
			}
			else
			{
				try
				{
					Thread.sleep(oldtime + polltime - now);
				}
				catch(Exception e)
				{
					// if we got interrupted, it's either for closing down (handled by while()) or for re-scanning due to disconnect
					oldtime = now - polltime;
					Wifi_scanning = false;
				}
			}
		}
		
		SerialPortLogger.debug("WifiHandler::scanning thread - terminating");
	}

	// We use a handler here to allow for the Acquire() function, which runs in a different thread, to issue an initialization of the WiFi
	private final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {      
    	   // we are shutting down!
    	   if (shutdown == true)
    		   return;
    	   
           switch (msg.what) 
           {
           case INIT_WIFI:
   			if (wm != null)
			{
				// create wifi lock if not sleeping enabled
   				if (sleepWIFI == false)
   					wifi_lock = wm.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "NORSWifiLock");
				 
				// is Wifi switched off and shall we switch it on?
				if (wm.isWifiEnabled() == false && enableWIFI == true)
				{
					try
					{
						wm.setWifiEnabled(true);
					}
					catch(Exception e)
					{
					}
				}
				// Register Broadcast Receivers
				nors.registerReceiver(WifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
				nors.registerReceiver(WifiReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
				nors.registerReceiver(WifiReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

				// signal to Acquire()
				initialized = true;
			}
   			break;  
           default:  
           	break;
           }
       }
    };

	private final BroadcastReceiver WifiReceiver = new BroadcastReceiver() 
	{	
	  @Override
	  public void onReceive(Context c, Intent intent) 
	  {
		int i;
		ScanResult result;

		// network adapter state changed?
		if (intent.getAction().compareTo(WifiManager.WIFI_STATE_CHANGED_ACTION) == 0)
		{
			// is it disabled -> re-scan!
			if (wm.isWifiEnabled() == false)
				if (runnable!=null)
					runnable.interrupt();
		}
		
		// connection state changed?
		if (intent.getAction().compareTo(WifiManager.NETWORK_STATE_CHANGED_ACTION) == 0)
		{
			NetworkInfo info = (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			  
			// get connection state
			if (info.getState().equals(NetworkInfo.State.CONNECTED))
				wifi_connected = 1;
			else
			{
				// re-scan!
				if (runnable!=null)
					runnable.interrupt();
				wifi_connected = 0;
			}

			// release semaphore to unlock Acquire()
			connected_semaphore.release(); 
		}

		// scan results back?
		if (intent.getAction().compareTo(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) == 0)
		{
			List<ScanResult> results = wm.getScanResults();
	    
		    // scanning is over
		    Wifi_scanning = false;
		    
			MAC_reading 	= new StringBuffer("WM");	
			SSID_reading 	= new StringBuffer("WI");
			RSSI_reading 	= new StringBuffer("WS");
		    WLAN_reading	= new StringBuffer("WF");
	    
		    wifi_first = true;
		    
		    // run through all results
		    for (i=0; i<results.size();i++) 
		    {
	        	// first device? -> then no \n at the end of it!
	        	if (wifi_first == true)
	        		wifi_first = false;
	        	else
	        	{
	        		MAC_reading.append("\n");
	        		SSID_reading.append("\n");
	        		RSSI_reading.append("\n");
	        		WLAN_reading.append("\n");
	        	}
	
		    	// get i-th result from list
		    	result = results.get(i);
		    	
		    	MAC_reading.append(result.BSSID);
		    	SSID_reading.append(result.SSID);
		    	RSSI_reading.append(String.valueOf(result.level));
		    	WLAN_reading.append(result.BSSID + ":" + result.SSID + ":" + String.valueOf(result.level));
		    }
		    
			// now release the semaphores
			nearby_semaphore.release(); 
			mac_semaphore.release(); 
			rssi_semaphore.release(); 
			ssid_semaphore.release(); 
			wlan_semaphore.release(); 	
		}
	  }
	};
}
