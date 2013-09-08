/*
Copyright (C) 2013, TecVis LP, support@tecvis.co.uk

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

import com.airs.R;
import com.airs.platform.HandlerEntry;

/**
 * Class to implement the NotificationHandler configuration UI, based on the HandlerUI interface class
 *
 * @see com.airs.handlerUIs.HandlerUI HandlerUI 
 * @see com.airs.handlers.NotificationHandler NotificationHandler
 */
public class NotificationHandlerUI implements HandlerUI
{   
	private Context context; 
	
	/**
	 * Initialises the settings entry with the name, description and icon resource ID
	 * @param context Reference to the {@link android.content.Context} realising this entry
	 */
	public HandlerEntry init(Context context)
	{
		this.context = context;
		
		HandlerEntry entry = new HandlerEntry();
		entry.name = context.getString(R.string.NotificationHandlerUI_name);
		entry.description = context.getString(R.string.NotificationHandlerUI_description);
		entry.resid = R.drawable.social;
		return (entry);
	}

	/**
	 * Returns the resource ID to the preference XML file containing the layout of the preference
	 * @return resource ID
	 */
	public int setDisplay()
	{
		return R.xml.prefsnotifications;
	}

	/**
	 * Returns the About String shown when selecting the About menu item in the Options menu
	 * @return About String of the About text
	 */
	public String About()
	{
		return context.getString(R.string.NotificationHandlerUI_about);
	}
	
	/**
	 * Returns the Title for the About Dialog shown when selecting the About menu item in the Options menu
	 * @return String of the title
	 */
	public String AboutTitle()
	{
		return context.getString(R.string.NotificationHandlerUI_name);
	}
	
    /**
     * Destroys any resources for this {@link HandlerUI}
     */
    public void destroy()
    {
    }
    
	/**
	 * Function to configure the Preference activity with any preset value necessary - empty here
	 * @param prefs Reference to {@link android.preference.PreferenceActivity}
	 */
	public void configurePreference(PreferenceActivity prefs)
	{
	}
}
