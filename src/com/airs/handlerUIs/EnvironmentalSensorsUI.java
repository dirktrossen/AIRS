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

import android.preference.PreferenceActivity;

import com.airs.HandlerEntry;
import com.airs.*;

public class EnvironmentalSensorsUI implements HandlerUI
{   
	public HandlerEntry init()
	{
		HandlerEntry entry = new HandlerEntry();
		entry.name = new String("Environmental Sensors");
		entry.description = new String("Obtains various environmental information, such as light, barometer and weather conditions at current location");
		entry.resid = R.drawable.weather;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefsenvironmental;
	}

	public String About()
	{
	    String AboutText = new String(
	    		"Obtains environmental information such as light, barometer, and various weather conditions (temperature, humidity, ...) at the current location through an online weather API. ");

		return AboutText;
	}
	
	public String AboutTitle()
	{
		return "Environmental Sensors";
	}
	
	public void configurePreference(PreferenceActivity prefs)
	{
	}
}

