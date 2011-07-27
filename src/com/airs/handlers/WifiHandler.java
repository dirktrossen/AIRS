/*
Copyright (C) 2004-2006 Nokia Corporation
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
package com.airs.handlers;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.telephony.PhoneStateListener;

import com.airs.helper.SerialPortLogger;
import com.airs.platform.HandlerManager;
import com.airs.platform.SensorRepository;

public class WifiHandler extends PhoneStateListener implements Handler
{
	Context nors;
	// phone state classes
	private WifiManager wm;
	private ConnectivityManager cm;
	private WifiLock wifi_lock;

	// are these there?
	private boolean enable = false, enableWIFI = false, sleepWIFI = false;
	// polltime
	private int			polltime = 5000;
	// sensor data
	private byte[] reading = null;
    
	private boolean Wifi_reading = false, Wifi_scanning = false;
	private boolean MAC_read = false, SSID_read = false, RSSI_read=false, WLAN_read = false;
	private boolean wifi_first = true;
	private StringBuffer MAC_reading;	
	private StringBuffer SSID_reading;
	private StringBuffer RSSI_reading;
	private StringBuffer WLAN_reading;
	private int old_wifi_connected = -1, wifi_connected;

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
		// acquire data and send out
		reading = null;
		try
		{
			if (enable == true)
				WLANReading(sensor);
		}
		catch (Exception e) 
		{
			debug("WifiHandler:Acquire: Exception: " + e.toString());
		}
		
		return reading;		
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
		// need to test for wifi at some point!!
	    if (enable == true)
	    {		
			SensorRepository.insertSensor(new String("WF"), new String("txt"), new String("WLAN info"), new String("txt"), 0, 0, 1, polltime, this);
			SensorRepository.insertSensor(new String("WI"), new String("SSID"), new String("WLAN SSID"), new String("txt"), 0, 0, 1, polltime, this);	
			SensorRepository.insertSensor(new String("WM"), new String("MAC"), new String("WLAN MAC address"), new String("txt"), 0, 0, 1, polltime, this);
			SensorRepository.insertSensor(new String("WS"), new String("dBm"), new String("Signal strength"), new String("txt"), 0, 0, 1, polltime, this);
			SensorRepository.insertSensor(new String("WC"), new String("boolean"), new String("WLAN connected"), new String("int"), 0, 0, 1, polltime, this);
		}	    
	}
	
	public WifiHandler(Context nors)
	{
		this.nors = nors;
		
		polltime	= HandlerManager.readRMS_i("LocationHandler::WifiPoll", 20) * 1000;

		// read whether or not we need to enable Wifi
		enableWIFI = HandlerManager.readRMS_b("LocationHandler::WIFION", false);

		// read whether or not we need to have Wifi sleep
		sleepWIFI = HandlerManager.readRMS_b("LocationHandler::WIFISleep", false);
		
		// try getting wifi manager
		try
		{
			// now get connectivity manager
			cm = (ConnectivityManager) nors.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm == null)
				return;
			
			wm = (WifiManager)nors.getSystemService(Context.WIFI_SERVICE);
			if (wm != null)
			{
				// create wifi lock
				wifi_lock = wm.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, "NORSWifiLock");
				 
				// is Wifi switched off and shall we switch it on?
				if (wm.isWifiEnabled() == false && enableWIFI == true)
					wm.setWifiEnabled(true);

				// Register Broadcast Receiver
				nors.registerReceiver(WifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
				enable = true;
				
				// start first scan here!
				wm.startScan();
				Wifi_scanning = true;
			}			
		}
		catch(Exception e)
		{
			enable = false;
		}
	}
	
	public void destroyHandler()
	{
		// unregister listeners		
		if (enable == true)
		{
			// unregister receiver
			nors.unregisterReceiver(WifiReceiver);
			// release wifi lock
			if (wifi_lock != null)
	          if (wifi_lock.isHeld() == true) 
	              wifi_lock.release();
		}
	}
	
	// do readings via WifiManager
	synchronized private void WLANReading(String sensor)
	{
		boolean dont_scan = false;
				
		// block until allowed to read values
	    while (Wifi_reading == true)
	    	sleep(100);
	    
	    Wifi_reading = true;

	    // construct answers
		switch(sensor.charAt(1))
		{
		case 'M':
			if (MAC_read==true)
			{
				reading = MAC_reading.toString().getBytes();
				MAC_read=false;
			}
			break;
		case 'I':
			if (SSID_read==true)
			{
				reading = SSID_reading.toString().getBytes();
				SSID_read=false;
			}
			break;
		case 'S':
			if (RSSI_read==true)
			{
				reading = RSSI_reading.toString().getBytes();
				RSSI_read=false;
			}
			break;
		case 'F':
			if (WLAN_read==true)
			{
				reading = WLAN_reading.toString().getBytes();
				WLAN_read=false;
			}
			break;
		case 'C':
			if (cm!=null)
			{
				// get network info for wifi
				NetworkInfo mWifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				
				// check if it is connected
				if (mWifi != null)
				{
					if (mWifi.isConnected())
						wifi_connected = 1;
					else
						wifi_connected = 0;
				}
				else
					wifi_connected = 0;
				
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
				
				// don't scan networks
				dont_scan = true;
			}
			break;
		default:
			reading = null;
		}
		
		// allow for writing/reading to sensor fields
		Wifi_reading = false;

		// shall we scan and is previous scan over?
		if (dont_scan==false && Wifi_scanning==false)
		{
			// is Wifi switched off and shall we switch it on?
			if (wm.isWifiEnabled() == false && enableWIFI == true)
				wm.setWifiEnabled(true);
		
			// if wifi is not locked, do so to prevent it from sleeping!
			if (sleepWIFI == false)
				if(wifi_lock.isHeld() == false)
		            wifi_lock.acquire();
		        
			// start next scan here!
			wm.startScan();
			Wifi_scanning = true;
		}
	}

	private final BroadcastReceiver WifiReceiver = new BroadcastReceiver() 
	{
	
	  @Override
	  public void onReceive(Context c, Intent intent) 
	  {
		int i;
		ScanResult result;
		
		// block any possible Acquire() thread from reading the values
	    while (Wifi_reading == true)
	    	sleep(100);
	    
	    Wifi_reading = true;
	    
	    List<ScanResult> results = wm.getScanResults();
	    
	    // scanning is over
	    Wifi_scanning = false;
	    // nothing scanned?
	    if (results.size()==0)
	    {
		    Wifi_reading = false;	   
	    	return;
	    }
	    
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
	    
	    // all four values have been read
	    WLAN_read = MAC_read = SSID_read = RSSI_read = true;
	    
	    // the Acquire() thread can now access!
	    Wifi_reading = false;
	  }
	};
}

