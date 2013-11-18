/*
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
package com.airs.handlerUIs;

import java.util.List;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.airs.*;
import com.airs.platform.HandlerEntry;

/**
 * Class to implement the GPSHandler and WifiHandler configuration UI, based on the HandlerUI interface class
 *
 * @see com.airs.handlerUIs.HandlerUI HandlerUI 
 * @see com.airs.handlers.GPSHandler GPSHandler
 * @see com.airs.handlers.WifiHandler WifiHandler
 */
public class LocationHandlerUI implements HandlerUI, OnSharedPreferenceChangeListener
{   
	private Context context;
	private PreferenceActivity prefActivity;
	
	/**
	 * Initialises the settings entry with the name, description and icon resource ID
	 * @param context Reference to the {@link android.content.Context} realising this entry
	 */
	public HandlerEntry init(Context context)
	{
		this.context = context;
		
		HandlerEntry entry = new HandlerEntry();
		entry.name = context.getString(R.string.LocationHandlerUI_name);
		entry.description = context.getString(R.string.LocationHandlerUI_description);
		entry.resid = R.drawable.location;
		return (entry);
	}

	/**
	 * Returns the resource ID to the preference XML file containing the layout of the preference
	 * @return resource ID
	 */
	public int setDisplay()
	{
		return R.xml.prefslocation;
	}

	/**
	 * Returns the About String shown when selecting the About menu item in the Options menu
	 * @return About String of the About text
	 */
	public String About()
	{
		return context.getString(R.string.LocationHandlerUI_about);
	}
	
	/**
	 * Returns the Title for the About Dialog shown when selecting the About menu item in the Options menu
	 * @return String of the title
	 */
	public String AboutTitle()
	{
		return context.getString(R.string.LocationHandlerUI_name);
	}

	
	/**
	 * Callback for when a preference entry has changed - this is needed here to recreate the right checkmarks on the adaptive GPS WiFi list for the ListPreferenceMultiSelect
	 * Unfortunately, the method here is rather crude by closing and restarting the activity - no better way found yet
	 * @param sharedPreferences Reference to the current {@link android.content.SharedPreferences}
	 * @param key String of the key that has changed
	 */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
    {
    	// only do something when the list of adaptive GPS Wifi APs has changed
    	if (key.compareTo("LocationHandler::AdaptiveGPS_WiFis") == 0) 
    	{
    		prefActivity.finish();
    		context.startActivity(prefActivity.getIntent());
    	}
    }
    
    /**
     * Destroys any resources for this {@link HandlerUI}
     */
    public void destroy()
    {
        // unregister listener to preference changes
        PreferenceManager.getDefaultSharedPreferences(prefActivity.getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);	     	
    }
    
	/**
	 * Function to configure the Preference activity with any preset value necessary - here gathering the known WiFi access points and populating the appropriate ListPreference
	 * @param prefs Reference to {@link android.preference.PreferenceActivity}
	 */
	public void configurePreference(PreferenceActivity prefs)
	{
		int i;
		boolean set_list = false;
		
        // register listener to preference changes in order to recreate entries
        PreferenceManager.getDefaultSharedPreferences(prefs.getApplicationContext()).registerOnSharedPreferenceChangeListener(this);	 

        // save for later
        prefActivity = prefs;
		
		// try to find the preference we want to configure
		ListPreference list = (ListPreference)prefs.findPreference("LocationHandler::AdaptiveGPS_WiFis");
		if (list==null)
			return;

    	// put all the WiFi APs in the list
		WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		if (wm != null)
		{
			List<WifiConfiguration> networks = wm.getConfiguredNetworks();
			
			if (networks != null)
				if (networks.size() >0)
				{
		        	String [] names = new String[networks.size()];
		
					for (i=0;i<networks.size();i++)
						names[i] = new String(networks.get(i).SSID.replace("\"", ""));
					
		        	// set names as entries in list
					list.setEntries(names);
					// set mac addresses as entries in preference
					list.setEntryValues(names);
					// list is defined
					set_list = true;
				}
			
			// no list?
			if (set_list == false)
			{
	        	String [] names = new String[1];
	        	String [] names2 = new String[1];
	        	
	        	// create message entry
	        	names[0] = new String(context.getString(R.string.AdaptiveGPS_NoWifi));
	        	names2[0] = new String ("");
	        	
				list.setEntries(names);
				list.setEntryValues(names2);
			}
		}
	}
}
