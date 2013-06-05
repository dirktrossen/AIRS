/*
Copyright (C) 2012, Dirk Trossen, airs@dirk-trossen.de
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
*/package com.airs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import com.airs.platform.HandlerManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class AIRS_shortcut extends Activity
{
     // preferences
     private SharedPreferences settings;
	 private AIRS_local AIRS_locally;
	 private Activity act;
	 private String preferences, template;
	 private AlertDialog alert;

	   @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
		   Intent intent= getIntent();
		   long synctime;
		   int version, i;
		   boolean tables, tables2, first_start, copy_template;
		   String music, storedWifis;
		   
	        // Set up the window layout
	        super.onCreate(savedInstanceState);
	        
	        // store for later usage
	        act = this;
		    
	        // get default preferences
	        settings = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
	        
			// check if persistent flag is running, indicating the AIRS has been running (and would re-start if continuing)
			if (settings.getBoolean("AIRS_local::running", false) == true)
			{
	    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    		builder.setMessage(R.string.AIRS_running_exit2)
	    			   .setTitle(R.string.AIRS_Local_Sensing)
	    		       .setCancelable(false)
	    		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() 
	    		       {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		        	    // clear persistent flag
	    			           	Editor editor = settings.edit();
	    			           	editor.putBoolean("AIRS_local::running", false);
	    		                // finally commit to storing values!!
	    		                editor.commit();
	    		                // stop service
	 		    			    stopService(new Intent(act, AIRS_local.class));
	 		    			    finish();
	    		           }
	    		       })
	    		       .setNegativeButton("No", new DialogInterface.OnClickListener() 
	    		       {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		                dialog.cancel();
	    		                finish();
	    		           }
	    		       });
	    		alert = builder.create();
	    		alert.show();
			}
			else
			{	        
		        // get values that should not be overwritten!
		        synctime = settings.getLong("SyncTimestamp", 0);
		        version = settings.getInt("Version", 0);	
		        tables = settings.getBoolean("AIRS_local::TablesExists", false);	
		        tables2 = settings.getBoolean("AIRS_local::Tables2Exists", false);
		        first_start = settings.getBoolean("AIRS_local::first_start", false);
		        copy_template = settings.getBoolean("AIRS_local::copy_template", false);
		        music = settings.getString("MusicPlayerHandler::Music", "");
				storedWifis = settings.getString("LocationHandler::AdaptiveGPS_WiFis", "");

		        // read all entries related to event annotations
				int own_events = Integer.parseInt(settings.getString("EventButtonHandler::MaxEventDescriptions", "5"));
				if (own_events<1)
					own_events = 5;
				if (own_events>50)
					own_events = 50;
	
				String event_selected_entry = settings.getString("EventButtonHandler::EventSelected", "");
				String[] event = new String[own_events];			
				for (i=0;i<own_events;i++)
					event[i]	= settings.getString("EventButtonHandler::Event"+Integer.toString(i), "");
		        
		        // get intent extras
		        if ((preferences = intent.getStringExtra("preferences")) != null)
		        {
		        	File preferenceFile = new File(getFilesDir(), "../shared_prefs/com.airs_preferences.xml");
		        	// if file does not exist use a path that is usually used by GalaxyS in 2.3!!!
	            	if (preferenceFile.exists() == false)
	            		preferenceFile = new File("/dbdata/databases/com.airs/shared_prefs/com.airs_preferences.xml");
	
		            File shortcutFile = new File(preferences);
		            
		            // get just the name of the preference template
		            template = shortcutFile.getName();
	
		        	// copy preference file if original preferences exist
		        	if (shortcutFile.exists() == true)
		        	{
			            try
			            {
			                FileChannel src = new FileInputStream(shortcutFile).getChannel();
			                FileChannel dst = new FileOutputStream(preferenceFile).getChannel();
			                dst.transferFrom(src, 0, src.size());
			                src.close();
			                dst.close();		                
			            }
			            catch(Exception e)
			            {
			            }
		        	}
		        	else
		        	{
			     		Toast.makeText(getApplicationContext(), getString(R.string.Shortcut_not_found), Toast.LENGTH_LONG).show();
			     		finish();
		        	}
		        }
		        
		        settings = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
	
		        // get default preferences
				Editor editor = settings.edit();
				
				// write certain back in order for them to not be overwritten!
				editor.putLong("SyncTimestamp", synctime);
				editor.putInt("Version", version);
				editor.putBoolean("AIRS_local::TablesExists", tables);
				editor.putBoolean("AIRS_local::Tables2Exists", tables2);
				editor.putBoolean("AIRS_local::first_start", first_start);
				editor.putBoolean("AIRS_local::copy_template", copy_template);
				editor.putString("MusicPlayerHandler::Music", music);
				editor.putString("LocationHandler::AdaptiveGPS_WiFis", storedWifis);
				
				// put back all entries related to event annotations
				for (i=0;i<own_events;i++)
					editor.putString("EventButtonHandler::Event"+Integer.toString(i), event[i]);
				editor.putString("EventButtonHandler::EventSelected", event_selected_entry);
	
				editor.commit();
				
		        // start service and connect to it -> then discover the sensors
		        startService(new Intent(this, AIRS_local.class));
		        // bind to service
		        if (bindService(new Intent(this, AIRS_local.class), mConnection, 0)==false)
		     		Toast.makeText(getApplicationContext(), "Measurement Activity::Binding to service unsuccessful!", Toast.LENGTH_LONG).show();
			}
	    }

	    @Override
	    public synchronized void onRestart() 
	    {
	        super.onRestart();
	    }

	    @Override
	    public void onStop() 
	    {
	        super.onStop();
	    }

	    @Override
	    public void onDestroy() 
	    {
	    	// stop service from updating value adapter
	    	if (AIRS_locally!=null)
	    		unbindService(mConnection);
	    		    	
	        super.onDestroy();
	    }

	    public void onActivityResult(int requestCode, int resultCode, Intent data) 
	    {
	    	return;
	    }

	    private ServiceConnection mConnection = new ServiceConnection() 
	    {
	  	    public void onServiceConnected(ComponentName className, IBinder service) 
	  	    {
	  	        // This is called when the connection with the service has been
	  	        // established, giving us the service object we can use to
	  	        // interact with the service.  Because we have bound to a explicit
	  	        // service that we know is running in our own process, we can
	  	        // cast its IBinder to a concrete class and directly access it.
	     		AIRS_locally = ((AIRS_local.LocalBinder)service).getService();

	     		// use Restart() function in order to start without GUIs
        		AIRS_locally.Restart(false);
    		   
        		// store the file that started it
        		AIRS_locally.template = new String(template);
        		
    		    // announce and finish
    		    if (AIRS_locally.running == true)
    		    	Toast.makeText(getApplicationContext(), getString(R.string.AIRS_started_local), Toast.LENGTH_LONG).show();          
                finish();
	  	    }

	  	    public void onServiceDisconnected(ComponentName className) {
	  	        // This is called when the connection with the service has been
	  	        // unexpectedly disconnected -- that is, its process crashed.
	  	        // Because it is running in our same process, we should never
	  	        // see this happen.
	     		AIRS_locally = null;
	  	    }
	  	};
}

