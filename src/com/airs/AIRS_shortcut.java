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

import com.airs.database.AIRS_restore;
import com.airs.database.AIRS_upload;
import com.airs.helper.SafeCopyPreferences;

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

/**
 * Activity to start a local recording via a launcher screen shortcut
 *
 * @see         AIRS_restore
 */
public class AIRS_shortcut extends Activity
{
     // preferences
     private SharedPreferences settings;
	 private AIRS_local AIRS_locally;
	 private Activity act;
	 private String preferences, template;
	 private AlertDialog alert;

	 /** Called when the activity is first created. 
	     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
	     */
	 @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
		    Intent intent= getIntent();
		   
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
		        // get intent extras
		        if ((preferences = intent.getStringExtra("preferences")) != null)
		        {	
		            File shortcutFile = new File(preferences);
		            
		            // get just the name of the preference template
		            template = shortcutFile.getName();
	
		        	// copy preference file if original preferences exist
		        	if (shortcutFile.exists() == true)
		        		SafeCopyPreferences.copyPreferences(this, shortcutFile);
		        	else
		        	{
			     		Toast.makeText(getApplicationContext(), getString(R.string.Shortcut_not_found), Toast.LENGTH_LONG).show();
			     		finish();
		        	}
		        }
		        
		        // reset the timer to the right setting
		        AIRS_upload.setTimer(this);
		        
		        // start service and connect to it -> then discover the sensors
		        startService(new Intent(this, AIRS_local.class));
		        // bind to service
		        if (bindService(new Intent(this, AIRS_local.class), mConnection, 0)==false)
		     		Toast.makeText(getApplicationContext(), "Measurement Activity::Binding to service unsuccessful!", Toast.LENGTH_LONG).show();
			}
	    }

	 /** Called when the activity is restarted. 
	     */
	 @Override
	    public synchronized void onRestart() 
	    {
	        super.onRestart();
	    }

	 /** Called when the activity is stopped. 
	     */
	 @Override
	    public void onStop() 
	    {
	        super.onStop();
	    }

	 /** Called when the activity is destroyed. 
	     */
	 @Override
	    public void onDestroy() 
	    {
	    	// stop service from updating value adapter
	    	if (AIRS_locally!=null)
	    		unbindService(mConnection);
	    		    	
	        super.onDestroy();
	    }

	 /**
	     * Called when called {@link android.app.Activity} has finished. See {@link android.app.Activity} how it works
	     * @param requestCode ID being used when calling the Activity
	     * @param resultCode result code being set by the called Activity
	     * @param data Reference to the {@link android.content.Intent} with result data from the called Activity
	     */
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

