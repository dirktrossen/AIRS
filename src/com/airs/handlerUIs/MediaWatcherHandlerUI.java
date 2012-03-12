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

public class MediaWatcherHandlerUI implements HandlerUI
{   
	public HandlerEntry init()
	{
		HandlerEntry entry = new HandlerEntry();
		entry.name = new String("Media Folders");
		entry.description = new String("Watch various media folder such as camera, music, ...");
		entry.resid = R.drawable.mediawatcher;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefsmedia;
	}

	public String About()
	{
	    String AboutText = new String(
	    		"Watches various media-related folders.\n\n"+
	    		"Camera, music and other folders can be watched with respect to creating new files, such as when taking a picture.");

		return AboutText;
	}
	
	public String AboutTitle()
	{
		return "Media Folders";
	}

	public void configurePreference(PreferenceActivity prefs)
	{
	}
}

