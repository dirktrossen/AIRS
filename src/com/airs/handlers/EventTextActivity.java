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
package com.airs.handlers;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

/** Activity to self-annotate any text being pasted to AIRS
 * @see android.app.Activity
 */
public class EventTextActivity extends Activity 
{
	private ActivityManager am;
	
	/**
	  * Started when creating the {@link android.app.Activity}
	  * @see android.app.Activity#onCreate(android.os.Bundle)
	  */
	@Override
	public void onCreate (Bundle savedInstanceState) 
	{
    	String applicationName = "unknown";
    	String caller;
        PackageManager pm = this.getPackageManager();

        // Set up the window layout
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();

        am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasks(10000,ActivityManager.RECENT_WITH_EXCLUDED);

    	Log.v("AIRS", "Started ACTION_SEND activity!");

        // more than one task?
        if (recentTasks.size() > 1)
        {
        	RecentTaskInfo sender = recentTasks.get(0);
        	
        	if (sender.baseIntent != null)
        	{
        		caller = sender.baseIntent.resolveActivity(getPackageManager()).getPackageName();

        		Log.v("AIRS", "Last recent component: " + caller);

	            try 
	            { 
	              	  applicationName = pm.getApplicationLabel(pm.getApplicationInfo(caller, PackageManager.GET_META_DATA)).toString();
	            }
	            catch(Exception e) 
	            {
	            }
	            
        		Log.v("AIRS", "Last recent program: " + applicationName);
        	}
        }
        else
        	Log.v("AIRS", "Just one recent task?");

        
        // if text has been sent to AIRS, record it now!
        if (Intent.ACTION_SEND.equals(action)) 
        {
			// send broadcast intent to signal end of selection to mood button handler
			Intent intentAIRS = new Intent("com.airs.eventtext");
			intentAIRS.putExtra("Text", applicationName + "::" + intent.getStringExtra(Intent.EXTRA_TEXT));			
			sendBroadcast(intentAIRS);			
        }
        finish();
    }
}
