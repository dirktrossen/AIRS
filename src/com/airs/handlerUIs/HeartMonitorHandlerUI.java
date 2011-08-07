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
package com.airs.handlerUIs;

import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

import com.airs.HandlerEntry;
import com.airs.*;

public class HeartMonitorHandlerUI implements HandlerUI
{
    // BT stuff
	public HandlerEntry init()    
	{				
		HandlerEntry entry = new HandlerEntry();
		entry.name = new String("Heart Monitor");
		entry.description = new String("ECG, heart rate, accelerometer & event button from Alive Heart Monitor");
		entry.resid = R.drawable.heart_monitor;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefsheartmonitor;
	}

	public String About()
	{
	    String AboutText = new String(
	    		"Senses heart monitor equipment, based on the specification by Alive Technologies. \n\n"+
	    		"You can enable the heart monitor sensing here as well as select the device to connect to.\n" + 
	    		"The protocol is available from Alive Technologies.\n");
	    
		return AboutText;
	}
	
	public String AboutTitle()
	{
		return "Heart Monitor";
	}

	public void configurePreference(PreferenceActivity prefs)
	{
		int foundAlive = 0;
		
		// try to find the preference we want to configure
		ListPreference list = (ListPreference)prefs.findPreference("HeartMonitorHandler::BTStore");
		if (list==null)
			return;

		// get BT adapter
		BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		
		// is there a BT adapter?
		if (mBtAdapter == null)
		{
			list.setEntries(null);
			list.setEntryValues(null);
			return;
		}

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ListPreference
        if (pairedDevices.size() > 0) 
        {
        	// first see how many AliveTechs we have paired with
            for (BluetoothDevice device : pairedDevices) 
            {
                if (device.getName().contains("Alive") == true)
                	foundAlive++;
            }
            
            // paired with at least one?
            if (foundAlive>0)
            {
	        	CharSequence[] names = new CharSequence[foundAlive];
	        	CharSequence[] macs = new CharSequence[foundAlive];
	           
	        	// create entry and value lists!
	        	foundAlive = 0;
	        	for (BluetoothDevice device : pairedDevices) 
	            {
	                if (device.getName().contains("Alive") == true)
	                {
	                	names[foundAlive] = device.getName();
	                	macs[foundAlive] = device.getAddress();
	                	foundAlive++;
	                }
	            }
	        	// set names as entries in list
				list.setEntries(names);
				// set mac addresses as entries in preference
				list.setEntryValues(macs);
            }
        } 
	}
}
