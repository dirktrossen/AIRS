/*
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
package com.airs.handlerUIs;

import java.util.List;
import java.util.Set;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

import com.airs.*;

public class LocationHandlerUI implements HandlerUI
{   
	Context context; 
	
	public HandlerEntry init(Context context)
	{
		this.context = context;
		
		HandlerEntry entry = new HandlerEntry();
		entry.name = context.getString(R.string.LocationHandlerUI_name);
		entry.description = context.getString(R.string.LocationHandlerUI_description);
		entry.resid = R.drawable.location;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefslocation;
	}

	public String About()
	{
		return context.getString(R.string.LocationHandlerUI_about);
	}
	
	public String AboutTitle()
	{
		return context.getString(R.string.LocationHandlerUI_name);
	}

	public void configurePreference(PreferenceActivity prefs)
	{
		int i;
		
		// try to find the preference we want to configure
		ListPreference list = (ListPreference)prefs.findPreference("LocationHandler::AdaptiveGPS_WiFis");
		if (list==null)
			return;

    	// put all the WiFi APs in the list
		WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		if (wm != null)
		{
			List<WifiConfiguration> networks = wm.getConfiguredNetworks();
			
			if (networks.size() >0)
			{
	        	String [] names = new String[networks.size()];
	
				for (i=0;i<networks.size();i++)
					names[i] = new String(networks.get(i).SSID.replace("\"", ""));
				
	        	// set names as entries in list
				list.setEntries(names);
				// set mac addresses as entries in preference
				list.setEntryValues(names);
			}
			else
			{
				list.setEntries(null);
				list.setEntryValues(null);
			}
		}
	}
}
