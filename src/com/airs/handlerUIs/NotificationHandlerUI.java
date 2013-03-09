package com.airs.handlerUIs;

import android.content.Context;
import android.preference.PreferenceActivity;

import com.airs.HandlerEntry;
import com.airs.R;

public class NotificationHandlerUI implements HandlerUI
{   
	Context context; 
	
	public HandlerEntry init(Context context)
		{
			this.context = context;
			
			HandlerEntry entry = new HandlerEntry();
			entry.name = context.getString(R.string.NotificationHandlerUI_name);
			entry.description = context.getString(R.string.NotificationHandlerUI_description);
			entry.resid = R.drawable.social;
			return (entry);
		}

		public int setDisplay()
		{
			return R.xml.prefsnotifications;
		}

		public String About()
		{
			return context.getString(R.string.NotificationHandlerUI_about);
		}
		
		public String AboutTitle()
		{
			return context.getString(R.string.NotificationHandlerUI_name);
		}
		
		public void configurePreference(PreferenceActivity prefs)
		{
		}
}
