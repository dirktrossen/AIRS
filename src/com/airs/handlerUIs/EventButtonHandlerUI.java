/*
Copyright (C) 2010 Dirk Trossen, airs@dirk-trossen.de

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

public class EventButtonHandlerUI implements HandlerUI
{    
	public HandlerEntry init()
	{		
		HandlerEntry entry = new HandlerEntry();
		entry.name = new String("Event Button");
		entry.description = new String("A button widget to mark meaningful events");
		entry.resid = R.drawable.event_local;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefseventbutton;
	}
	
	public String About()
	{
	    String AboutText = new String(
	    		"Allows for marking meaningful events through a homescreen (button) widget.\n\n"+
	    		"The user can input their own meaningful description for the event to be marked.\n" + 
	    		"AIRS stores the most recently defined descriptions with the maximum number to be stored configurable.");
 
		return AboutText;
	}	
	
	public String AboutTitle()
	{
		return "Event Button";
	}

	public void configurePreference(PreferenceActivity prefs)
	{
	}
}
