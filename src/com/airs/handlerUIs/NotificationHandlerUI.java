package com.airs.handlerUIs;

import android.preference.PreferenceActivity;

import com.airs.HandlerEntry;
import com.airs.R;

public class NotificationHandlerUI implements HandlerUI
{   
		public HandlerEntry init()
		{
			HandlerEntry entry = new HandlerEntry();
			entry.name = new String("IM Notifications");
			entry.description = new String("Records notifications from IM programs (currently Skype and Google Talk)");
			entry.resid = R.drawable.social;
			return (entry);
		}

		public int setDisplay()
		{
			return R.xml.prefsnotifications;
		}

		public String About()
		{
		    String AboutText = new String(
		    		"Captures notifications from certain IM programs. \n\n"+
		    		"Currently, Skype and Google Talk are supported. Skype only provides the name of the person you are chatting with, while Google Talk provides name and message.\n" + 
		    		"The sensor is implemented as a so-called Accessibility service and you need to enable its usage in the system settings (which the settings entry will lead you to).");

			return AboutText;
		}
		
		public String AboutTitle()
		{
			return "IM Notifications";
		}
		
		public void configurePreference(PreferenceActivity prefs)
		{
		}
}
