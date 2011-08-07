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

import android.preference.PreferenceActivity;

import com.airs.HandlerEntry;
import com.airs.*;

public class LocationHandlerUI implements HandlerUI
{   
	public HandlerEntry init()
	{
		HandlerEntry entry = new HandlerEntry();
		entry.name = new String("Location");
		entry.description = new String("Various location sources like GPS, Wifi and cell");
		entry.resid = R.drawable.location;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefslocation;
	}

	public String About()
	{
	    String AboutText = new String(
	    		"Senses location-related information.\n\n"+
	    		"The sensor values are location values, utilizing cell identification info, Wifi beacon information as well as GPS.\n" + 
	    		"cellID is a simple integer, GPS will give long/lat, and WLAN the MAC address, SSID as well as signal strength of nearby access points. \n" +
	    		"Here you can select the poll intervalls for Wifi and GPS.\n");

		return AboutText;
	}
	
	public String AboutTitle()
	{
		return "Location";
	}

	public void configurePreference(PreferenceActivity prefs)
	{
	}
}
