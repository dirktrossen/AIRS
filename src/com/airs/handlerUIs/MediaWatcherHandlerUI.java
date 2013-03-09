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

import android.content.Context;
import android.preference.PreferenceActivity;

import com.airs.*;

public class MediaWatcherHandlerUI implements HandlerUI
{   
	Context context; 
	
	public HandlerEntry init(Context context)
	{
		this.context = context;
		
		HandlerEntry entry = new HandlerEntry();
		entry.name = context.getString(R.string.MediaWatcherHandlerUI_name);
		entry.description = context.getString(R.string.MediaWatcherHandlerUI_description);
		entry.resid = R.drawable.mediawatcher;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefsmedia;
	}

	public String About()
	{
		return context.getString(R.string.MediaWatcherHandlerUI_about);
	}
	
	public String AboutTitle()
	{
		return context.getString(R.string.MediaWatcherHandlerUI_name);
	}

	public void configurePreference(PreferenceActivity prefs)
	{
	}
}

