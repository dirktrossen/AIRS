/*
Copyright (C) 2012, Dirk Trossen, airs@dirk-trossen.de

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

public class HeartrateHandlerUI implements HandlerUI
{
    // BT stuff
	public HandlerEntry init()    
	{				
		HandlerEntry entry = new HandlerEntry();
		entry.name = new String("Heart Rate");
		entry.description = new String("Heart rate measured through your camera");
		entry.resid = R.drawable.heart_monitor;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefsheartrate;
	}

	public String About()
	{
	    String AboutText = new String(
	    		"Measures your heart rate by having you place your finger over the camera while the software determines changes in red colors.\n\n"+
	    		"You can adjust the measurements with an offset value, compensating for any misalignments.");
	    
		return AboutText;
	}
	
	public String AboutTitle()
	{
		return "Heart Rate";
	}

	public void configurePreference(PreferenceActivity prefs)
	{
	}
}
