/*
Copyright (C) 2006 Nokia Corporation
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

public class PhoneSensorsHandlerUI implements HandlerUI
{   
	public HandlerEntry init()
	{
		HandlerEntry entry = new HandlerEntry();
		entry.name = new String("Phone & System");
		entry.description = new String("Various phone-based information like orientation, light, RAM, battery etc");
		entry.resid = R.drawable.phone;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefsphonesensors;
	}

	public String About()
	{
	    String AboutText = new String(
	    		"Senses various phone-based sensors & system information.\n\n"+
	    		"The orientation is only supported for models with internal compass. Other information includes light, proximity as well as system information such as battery, RAM and tasks.");

		return AboutText;
	}
	
	public String AboutTitle()
	{
		return "Phone Sensors";
	}

	public void configurePreference(PreferenceActivity prefs)
	{
	}
}
