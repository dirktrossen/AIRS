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
package com.airs;

import android.content.Context;
import android.preference.PreferenceActivity;

import com.airs.HandlerEntry;
import com.airs.handlerUIs.HandlerUI;

public class AIRS_general_settings implements HandlerUI
{   
	public HandlerEntry init(Context context)
	{
		HandlerEntry entry = new HandlerEntry();
		
		entry.name = new String(context.getString(R.string.main_Configure));
		entry.description = context.getString(R.string.main_Configure);
		entry.resid = R.drawable.general1;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.generalsettings;
	}

	public String About()
	{
		return null;
	}
	
	public String AboutTitle()
	{
		return "General Setting";
	}
	
	public void configurePreference(PreferenceActivity prefs)
	{
	}
}

