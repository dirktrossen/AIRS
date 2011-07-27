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

public class ProximityHandlerUI implements HandlerUI
{   
	public HandlerEntry init()
	{
		HandlerEntry entry = new HandlerEntry();
		entry.name = new String("Proximity Sampling");
		entry.description = new String("Records samples of audio when defined BT devices are around");
		entry.resid = R.drawable.proximity;
		return (entry);
	}

	public int setDisplay()
	{
		return R.xml.prefsproximity;
	}

	public String About()
	{
	    String AboutText = new String(
	    		"Samples surrounding sound. \n\n"+
	    		"This sensor records surrounding audio when it discovers certain BT devices around. " + 
	    		"The recording will continue until the previously discovered device is no longer discovered.\n\n" +
	    		"The settings allows for enabling the discoverability on this device for being discovered by others. However, this feature is often time-limited in most Android devices!\n\n" +
	    		"When recording this sensor, you should disable the BT Beacon sensor since both will compete for the BT discovery, which might lead to misfunction.\n" +
	    		"The sampling rate and the polling interval for the sampling can be set in order to optimize the reading and processing requirements.\n\n" +
	    		"WARNING: Recording audio is considered illegal in many countries when taking place without the other party's consent!\n\n" +
	    		"NORS takes no responsibility as to the legality of the recordings undertaken by the user of this software!");

		return AboutText;
	}
	
	public String AboutTitle()
	{
		return "Audio Sampling";
	}
	
	public void configurePreference(PreferenceActivity prefs)
	{
	}
}
