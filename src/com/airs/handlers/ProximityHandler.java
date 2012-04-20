/*
Copyright (C) 2011, Dirk Trossen, airs@dirk-trossen.de

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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Vector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Environment;

import com.airs.helper.SerialPortLogger;
import com.airs.platform.HandlerManager;
import com.airs.platform.SensorRepository;

/**
 * @author trossen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ProximityHandler implements Handler
{
	private Context		nors;
    private BluetoothAdapter mBtAdapter = null;
	private int sampling_rate;
	private boolean discoverable = true;
	private boolean bt_enabled 	= true;
	private boolean bt_ask		 	= true;
	private boolean 	 bt_finished	= false;
	private boolean 	 bt_registered  = false;
	private boolean		 proximityFound = false;
	private boolean 	 last_found = false;
	private boolean 	 recording = false;
	private MediaRecorder recorder;
	private String media_file;
	// create field that holds acquisition data
	private int polltime=5000;
	private long oldtime = 0;
	private Vector<String>	proximityDevices;

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
		// try to discover when it's time to do so
		if (oldtime+polltime<System.currentTimeMillis())
		{
			discover();
			oldtime = System.currentTimeMillis();
		}

		// found device and not recording yet?
		if (proximityFound == true && recording == false)
			startRecording();

		// not finding device that started recording?
		if (proximityFound == false && recording == true)
		{
			// stop recording
			stopRecording();
			// and fire event 
		    StringBuffer buffer = new StringBuffer("PS");
		    buffer.append(media_file);
		    // store old callee number
    		return buffer.toString().getBytes();
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
	
		    SensorRepository.insertSensor(new String("PS"), new String("file"), new String("Proximity Sampling"), new String("txt"), 0, 0, 1, false, polltime, this);	    
		}
		catch(Exception e)
		{
	        SerialPortLogger.debug("ProximityHandler::cannot get localDevice()");
			bt_enabled = false;
		}
	}
	
	public ProximityHandler(Context nors)
	{
		int i, reading;
		boolean available = true;
		
		this.nors = nors;
		// read polltime
		polltime 		= HandlerManager.readRMS_i("ProximityHandler::samplingpoll", 5) * 1000;
		sampling_rate 	= HandlerManager.readRMS_i("ProximityHandler::SamplingRate", 8000);
		bt_enabled 		= HandlerManager.readRMS_b("BeaconHandler::BTON", true);
		// should ask before enabling?
		bt_ask 			= HandlerManager.readRMS_b("ProximityHandler::BTONAsk", false);
		discoverable	= HandlerManager.readRMS_b("ProximityHandler::BTdiscoverable", true);
		
		// add MAC addresses from file into vector
		proximityDevices = new Vector<String>();
		try
		{
			// open file
			File devices = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/NORS_values", "PS.conf");
			BufferedInputStream buf = new BufferedInputStream(new FileInputStream(devices.toString()));			
			byte[] line = new byte[40];
			
			// read now line for line
			i = 0;
			do
			{
				reading = buf.read();
				if (reading == -1)
					available = false;
				if (reading != (int)'\n')
					line[i++] = (byte)reading;
				else
				{
					proximityDevices.add(new String(line, 0, i));
					SerialPortLogger.debug("found device in file:" + new String(line, 0, i));
					i = 0;
				}
			}while(available == true);
		}
		catch(Exception e)
		{
			// add some default devices
			proximityDevices.add("E8:E5:D6:4F:93:E4");
			proximityDevices.add("BC:47:60:A6:C7:35");
			proximityDevices.add("E0:F8:47:0F:97:AD");
		}
	}
	
	public void destroyHandler()
	{
		if (recording == true)
			stopRecording();
		if (mBtAdapter != null)
			mBtAdapter.cancelDiscovery();
		if (bt_registered == true)
			nors.unregisterReceiver(mReceiver);
	}
	
	private void startRecording()
	{
		recording = true;
		// create media recorder and start
		try
		{
			 recorder = new MediaRecorder();
			 recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			 recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
			 recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			 recorder.setAudioEncodingBitRate(sampling_rate);
			 media_file = String.valueOf(System.currentTimeMillis()) + ".3gp";
			 File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/NORS_values", media_file);
			 recorder.setOutputFile(file.toString());
			 recorder.prepare();
			 recorder.start();   // Recording is now started
		}
		catch(Exception e)
		{
			recording = false;
		}
	}

	private void stopRecording()
	{
		if (recording == true)
		{
				// stop recorder and release resources
			 recorder.stop();
			 recorder.reset();
			 recorder.release(); 
			 recording = false;
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
    	    
        if (discoverable == true)
	        if (mBtAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) 
	        {
	            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	            discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
	            nors.startActivity(discoverableIntent);
	        }

		bt_registered = true;

		// currently nothing is found
		last_found = false;
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
        bt_finished = false;
        while (bt_finished == false)
        	sleep(200);
        
        // indicate last found round!
        proximityFound = last_found;
        
        // unregister broadcast receiver
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
        	int i;
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) 
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getAddress()!=null)
                	// search through list of MAC addresses we want to find
                	for(i=0;i<proximityDevices.size();i++)
                		if (proximityDevices.elementAt(i).equals(device.getAddress()))
                			last_found = true;
            } 
            else 
            	if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) 
            		bt_finished = true;
        }
    };
}

