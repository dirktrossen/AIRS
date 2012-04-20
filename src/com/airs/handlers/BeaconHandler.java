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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.airs.helper.SerialPortLogger;
import com.airs.platform.HandlerManager;
import com.airs.platform.History;
import com.airs.platform.SensorRepository;

public class BeaconHandler implements Handler, Runnable 
{
	// BT stuff
	private Context		nors;
    private BluetoothAdapter mBtAdapter = null;
    private int			no_devices = 0;
	private byte[] 		no_readings = new byte[6];
	// beacon data
	private StringBuffer reading = null;
	private boolean      new_current = false;
	private boolean      new_current_no = false;
	private boolean 	 bt_enabled 	= true;
	private boolean 	 bt_ask		 	= true;
	private boolean 	 bt_finished	= false;
	private boolean 	 bt_registered  = false;
	private boolean 	 bt_first		= false;
	private Thread 		 runnable = null;
	private boolean		 running = true;
	
	// historical data
	private History history_BN = new History(History.TYPE_INT);

//	private char EOL = 13;

	// config data
	String Poll = null;
	int polltime = 15000;
	long oldtime = 0;

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

		if (runnable == null)
		{
			runnable = new Thread(this);
			runnable.start();	
		}
		
		switch(sensor.charAt(1))
		{
			case 'T' :
				// new reading available
			    if (new_current == true)
			    {
			    	// declare invalid and return current one
			    	new_current = false;
			    	if (reading != null)
			    		return reading.toString().getBytes();
			    }
			    else
			    	return null;
			case 'N' :
				if (new_current_no == true)
				{
					new_current_no = false;
					no_readings[0] = (byte)sensor.charAt(0);
					no_readings[1] = (byte)sensor.charAt(1);
					no_readings[2] = (byte)((no_devices>>24) & 0xff);
					no_readings[3] = (byte)((no_devices>>16) & 0xff);
					no_readings[4] = (byte)((no_devices>>8) & 0xff);
					no_readings[5] = (byte)(no_devices & 0xff);
					
					history_BN.push(no_devices);
					return no_readings;		
				}
				else
					return null;
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
		switch(sensor.charAt(1))
		{
			case 'T' :
			case 'N' :
				return "There are currently " + String.valueOf(no_devices) + " BT devices around me!";
		}
		
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
		if (sensor.charAt(1) == 'N')
			history_BN.timelineView(nors, "BT devices [#]", 0);
	}

	// run discovery in separate thread
	public void run() 
	{
		while(running==true)
		{
			// try to discover when it's time to do so
			if (oldtime+polltime<System.currentTimeMillis())
			{
				oldtime = System.currentTimeMillis();
				discover();
			}
			
			sleep(1000);
		}
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
			SensorRepository.insertSensor(new String("BT"), new String("MAC"), new String("BT Devices"), new String("txt"), 0, 0, 1, false, polltime, this);	    
			SensorRepository.insertSensor(new String("BN"), new String("#"), new String("BT Devices"), new String("int"), 0, 0, 50, true, polltime, this);	    
		}
		catch(Exception e)
		{
	        SerialPortLogger.debug("BeaconHandler::cannot get localDevice()");
			bt_enabled = false;
		}
	}
	
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
		oldtime = System.currentTimeMillis() - polltime + 2500;
	}
	
	public void destroyHandler()
	{
		// signal thread to close down
		running = false;
		
		// cancel any discovery
		bt_finished = true;
		if (mBtAdapter != null)
			mBtAdapter.cancelDiscovery();
		if (bt_registered == true)
		{
			nors.unregisterReceiver(mReceiver);
			bt_registered = false;
		}
	}
	
    private void discover()
    {
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        nors.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        nors.registerReceiver(mReceiver, filter);
    	      
		bt_registered = true;

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
        
        // periodically sleep until finished
        bt_first = true;
        bt_finished = false;
        while (bt_finished == false)
        	sleep(200);

        // signal availability of data
        new_current = new_current_no = true;
        
        // unregister broadcast receiver
        if (bt_registered == true)
        	nors.unregisterReceiver(mReceiver);
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
                	reading.append(device.getAddress() + "::" + device.getName());
                else
                	reading.append(device.getAddress() + ":: ");
    	        no_devices ++;
            } 
            else 
            	if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) 
            	{
            		bt_finished = true;
            	}
        }
    };
}
