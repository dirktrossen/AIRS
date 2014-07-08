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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.airs.helper.SerialPortLogger;
import com.airs.helper.Waker;
import com.airs.platform.HandlerManager;
import com.airs.platform.History;
import com.airs.platform.SensorRepository;
import com.airs.*;

/** 
 * Class to read audio-related sensors, specifically the BN, BD, BT sensor
 * @see Handler
 */
public class BeaconHandler implements Handler, Runnable 
{
	// BT stuff
	private Context		nors;
    private BluetoothAdapter mBtAdapter = null;
    private int			no_devices = 0;
	private byte[] 		no_readings = new byte[6];
	private List<String> device_list;
	// beacon data
	private StringBuffer reading = null;
	private boolean 	 bt_enabled 	= true;
	private boolean 	 bt_ask		 	= true;
	private boolean 	 bt_registered  = false;
	private boolean 	 bt_registered2 = false;
	private boolean 	 bt_first		= false;
	private Thread 		 runnable = null;
	private boolean		 running = false, shutdown = false;
	private Semaphore BT_semaphore 	= new Semaphore(1);
	private Semaphore BN_semaphore 	= new Semaphore(1);
	private Semaphore finished_semaphore 	= new Semaphore(1);
	
//	private char EOL = 13;

	// config data
	private int polltime = 15000;
	private long oldtime = 0;

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
	 * Here, start BT discovery thread in case it hasn't run yet
	 * @param sensor String of the sensor symbol
	 * @param query String of the query to be fulfilled - not used here
	 * @see com.airs.handlers.Handler#Acquire(java.lang.String, java.lang.String)
	 */
	public synchronized byte[] Acquire(String sensor, String query)
	{		
		int i;
		StringBuffer devices = null;

		// are we shutting down?
		if (shutdown == true)
			return null;

		// Discovery thread started?
		if (runnable == null)
		{
			running = true;
			runnable = new Thread(this);
			runnable.start();	
		}
		
		switch(sensor.charAt(1))
		{
			case 'T' :
				// wait until new reading available
				wait(BT_semaphore); 
		    	if (reading != null)
		    		return reading.toString().getBytes();
			    else
			    	return null;
			case 'N' :
				// wait until new reading available
				wait(BN_semaphore); 
				no_readings[0] = (byte)sensor.charAt(0);
				no_readings[1] = (byte)sensor.charAt(1);
				no_readings[2] = (byte)((no_devices>>24) & 0xff);
				no_readings[3] = (byte)((no_devices>>16) & 0xff);
				no_readings[4] = (byte)((no_devices>>8) & 0xff);
				no_readings[5] = (byte)(no_devices & 0xff);
				
				return no_readings;	
			case 'D':
				// otherwise read list and create reading buffer
				devices = new StringBuffer("BD");

				// if there's no device connected, we're finished!
				if (device_list.size() == 0)
					return devices.toString().getBytes();
				
				synchronized(device_list)
				{
					boolean first_one = true;
					for (i=0;i<device_list.size();i++)
					{
		            	// first device? -> then no \n at the end of it!
		            	if (first_one == true)
		            		first_one = false;
		            	else
		        	        devices.append("\n");
			
		                // append the BluetoothDevice object from the list
		                devices.append(device_list.get(i));
					}
					
		    		return devices.toString().getBytes();

				}
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
		switch(sensor.charAt(1))
		{
			case 'T' :
			case 'N' :
				return "There are currently " + String.valueOf(no_devices) + " BT devices around me!";
		}
		
		return null;		
	}

	/**
	 * Method to view historical chart of the given sensor symbol
	 * @param sensor String of the symbol for which the history is being requested
	 * @see com.airs.handlers.Handler#History(java.lang.String)
	 */
	public void History(String sensor)
	{
		if (sensor.charAt(1) == 'N')
			History.timelineView(nors, "BT devices [#]", "BN");
	}

	/**
	 * BT Discovery thread, being executed every polltime seconds, using the sleep() function to wait
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
				discover();
			}
			else
				sleep(polltime - (now - oldtime));
		}
	}
	
	/**
	 * Method to discover the sensor symbols support by this handler
	 * As the result of the discovery, appropriate {@link com.airs.platform.Sensor} entries will be added to the {@link com.airs.platform.SensorRepository}
	 * Here, we also check if the BT adapter is available and also enable it, in case it is disabled and it is configured (by the user) to switch BT on
	 * Furthermore, we register the receivers for getting BT connect and disconnects messages
	 * @see com.airs.handlers.Handler#Discover()
	 * @see com.airs.platform.Sensor
	 * @see com.airs.platform.SensorRepository
	 */
	public void Discover()
	{
		try
		{
	        // Get the local Bluetooth adapter
	        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	        
	        // if there's no BT adapter, return without putting sensors in repository
	        if (mBtAdapter == null) 
	        	return;
	        
	        // and if there's no BT enabled, see if it is to be turned on automatically
	        if (mBtAdapter.isEnabled()==false)
	        {
	        	// does user wants BT to be enabled?
	        	if (bt_enabled == false)
	        		return;
	        	// shall we ask before enabling?
				if (bt_ask==true)
				{
					Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
					nors.startActivity(enableIntent);
				}
				else
					mBtAdapter.enable();
	        }
	        		        
		    // if it's there, add sensor
			SensorRepository.insertSensor(new String("BT"), new String("MAC"), nors.getString(R.string.BT_d), nors.getString(R.string.BT_e), new String("txt"), 0, 0, 1, false, 0, this);	    
			SensorRepository.insertSensor(new String("BN"), new String("#"), nors.getString(R.string.BN_d), nors.getString(R.string.BN_e), new String("int"), 0, 0, 50, true, 0, this);	    
			SensorRepository.insertSensor(new String("BD"), new String("MAC"), nors.getString(R.string.BD_d), nors.getString(R.string.BD_e), new String("txt"), 0, 0, 50, true, polltime, this);	 
			
			// now register for the connected device intents
			IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		    IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		    IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		    nors.registerReceiver(mReceiver2, filter1);
		    nors.registerReceiver(mReceiver2, filter2);
		    nors.registerReceiver(mReceiver2, filter3);
		    
		}
		catch(Exception e)
		{
	        SerialPortLogger.debug("BeaconHandler::cannot get localDevice()");
			bt_enabled = false;
		}
	}
	
	/**
	 * Constructor, allocating all necessary resources for the handler
	 * Here, reading the various RMS values of the preferences and arming the semaphores
	 * @param nors Reference to the calling {@link android.content.Context}
	 */
	public BeaconHandler(Context nors)
	{		
		// store for later
		this.nors = nors;

		// read whether or not we need to enable Beacon
		bt_enabled = HandlerManager.readRMS_b("BeaconHandler::BTON", false);

		// should ask before enabling?
		bt_ask = HandlerManager.readRMS_b("BeaconHandler::BTONAsk", false);

		polltime = HandlerManager.readRMS_i("BeaconHandler::Poll", 30) * 1000;
		
		// save current time and set so that first Acquire() will discover but substract a bit more to give BT time to fire up
		oldtime = System.currentTimeMillis();
		
		// create device list
		device_list = new ArrayList<String>();
		
		// arm the semaphores now
		wait(BT_semaphore); 
		wait(BN_semaphore); 
		wait(finished_semaphore); 
	}
	
	/**
	 * Method to release all handler resources
	 * Here, we interrupt the discovery thread and stop any ongoing BT discovery
	 * @see com.airs.handlers.Handler#destroyHandler()
	 */
	public void destroyHandler()
	{
		// we are shutting down !
		shutdown = true;
		
		// release all semaphores for unlocking the Acquire() threads
		BT_semaphore.release();
		BN_semaphore.release();

		// signal thread to close down
		if (running == true)
		{
			running = false;
			runnable.interrupt();
		}

		// cancel any discovery
		if (mBtAdapter != null)
			if (mBtAdapter.isDiscovering() == true)
				mBtAdapter.cancelDiscovery();
		
		// cancel any discovery wait!
		finished_semaphore.release();
		
//		if (bt_registered == true)
//		{
//			try
//			{
//				nors.unregisterReceiver(mReceiver);
//			}
//			catch(Exception e)
//			{
//				bt_registered = false;
//			}
//		}
		
		// are we listening to connected devices?
		if (bt_registered2 == true)
			nors.unregisterReceiver(mReceiver2);
	}
	
    private void discover()
    {
    	if (bt_registered == false)
    	{
	        // Register for broadcasts when a device is discovered
	        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	        nors.registerReceiver(mReceiver, filter);
	
	        // Register for broadcasts when discovery has finished
	        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	        nors.registerReceiver(mReceiver, filter);
	    	      
			bt_registered = true;
    	}

		bt_first = true;

	    no_devices = 0;
	    reading = new StringBuffer("BT");	
        // start discovery
		try
		{
	        // Request discover from BluetoothAdapter
	        mBtAdapter.startDiscovery();
		}
        catch (Exception e) 
        {
            return;
        }
        
        // sleep until finished
		wait(finished_semaphore); 

        // signal availability of data
        BT_semaphore.release();
        BN_semaphore.release();
        
        // unregister broadcast receiver
        if (bt_registered == true)
        {
        	try
        	{
        		nors.unregisterReceiver(mReceiver);
        	}
        	catch(Exception e)
        	{
        	}
        }
        
		// cancel any discovery that might be running due to multi-threading
		if (mBtAdapter != null)
			if (mBtAdapter.isDiscovering() == true)
				mBtAdapter.cancelDiscovery();

		bt_registered = false;
    }	 
    
    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) 
            {
            	// first device? -> then no \n at the end of it!
            	if (bt_first == true)
            		bt_first = false;
            	else
        	        reading.append("\n");
	
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getName()!=null)
                	reading.append(device.getAddress() + "::" + device.getName().replaceAll("'","''"));
                else
                	reading.append(device.getAddress() + ":: ");
    	        no_devices ++;
            }
            
        	if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) 
        		finished_semaphore.release();
        	
        }
    };
    
    // The BroadcastReceiver that listens for connected devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver2 = new BroadcastReceiver() 
    {
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int i;

            String entry;
            
            // If it's already paired, skip it, because it's been listed already
            if (device.getName()!=null)
            	entry = new String(device.getAddress() + "::" + device.getName());
            else
            	entry = new String(device.getAddress() + ":: ");

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) 
            {
            	// add connected device to list
            	device_list.add(entry);
            }
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) 
            {
            	// remove disconnected device from list
            	for (i=0;i<device_list.size();i++)
            		if (device_list.get(i).compareTo(entry) == 0)
            			device_list.remove(i);
            }           
        }
    };
}
