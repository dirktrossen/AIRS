/*
Copyright (C) 2011-2013, Dirk Trossen, airs@dirk-trossen.de

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
import android.content.Context;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

import com.airs.*;

public class HeartMonitorHandlerUI implements HandlerUI
{
	Context context; 
	
	public HandlerEntry init(Context context)
	{				
		this.context = context;
		
		HandlerEntry entry = new HandlerEntry();
		entry.name = context.getString(R.string.HeartMonitorHandlerUI_name);
		entry.description = context.getString(R.string.HeartMonitorHandlerUI_description);
		entry.resid = R.drawable.heart_monitor;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefsheartmonitor;
	}

	public String About()
	{  
		return context.getString(R.string.HeartMonitorHandlerUI_about);
	}
	
	public String AboutTitle()
	{
		return context.getString(R.string.HeartMonitorHandlerUI_name);
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
        	String [] names = new String[pairedDevices.size()];
        	String [] macs = new String[pairedDevices.size()];

        	// create entry and value lists!
        	foundAlive = 0;
        	for (BluetoothDevice device : pairedDevices) 
            {
            	names[foundAlive] = device.getName();
            	macs[foundAlive] = device.getAddress();
            	foundAlive++;
            }
        	// set names as entries in list
			list.setEntries(names);
			// set mac addresses as entries in preference
			list.setEntryValues(macs);
        } 
	}
}
