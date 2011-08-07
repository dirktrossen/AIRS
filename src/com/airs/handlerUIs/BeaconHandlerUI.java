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

public class BeaconHandlerUI implements HandlerUI
{    
	public HandlerEntry init()    
	{
		HandlerEntry entry = new HandlerEntry();
		entry.name = new String("Bluetooth Beacon");
		entry.description = new String("Surrounding BT devices");
		entry.resid = R.drawable.bt2;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefsbtbeacon;
	}

	public String About()
	{
	    String AboutText = new String(
	    		"Senses surrounding Bluetooth devices.\n\n"+
	    		"The sensor values are of the format MAC::name for each discovered device. \n" + 
	    		"All service devices are attempted to be discovered so that all discoverable BT devices should  be sensed.\n" +
	    		"The user can be asked to switch on BT if it is switched off before starting the sensing.");

		return AboutText;
	}
	
	public String AboutTitle()
	{
		return "BT Beacon";
	}
	
	public void configurePreference(PreferenceActivity prefs)
	{
	}
}
