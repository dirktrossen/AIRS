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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.airs.helper.SerialPortLogger;

public class AIRS_remote_tab extends Activity implements OnClickListener
{
    // handler for starting remote AIRS
	public static final int START_REMOTELY = 3;

	// Layout Views
    private ImageButton main_remote;
    private ProgressDialog progressdialog;

    // preferences
    private SharedPreferences settings;
  
    // other variables
    private AIRS_remote	AIRS_remotely;
    private Context		airs;

	protected void sleep(long millis) 
	{
		try 
		{
			Thread.sleep(millis);
		} 
		catch (InterruptedException ignore) 
		{
		}
	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        // Set up the window layout
        super.onCreate(savedInstanceState);
        
        // save current instance for inner classes
        this.airs = this;
        
        // get default preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);
		
        // set content of View
        setContentView(R.layout.remote);

        TextView mText = (TextView) findViewById(R.id.remote_text);
        mText.setMovementMethod(new ScrollingMovementMethod());

        // get buttons and set onclick listener
        main_remote 		= (ImageButton)findViewById(R.id.button_remote);
        main_remote.setOnClickListener(this);
              
	    // start service and connect to it -> then discover the sensors
        getApplicationContext().startService(new Intent(this, AIRS_remote.class));
        getApplicationContext().bindService(new Intent(this, AIRS_remote.class), mConnection, Service.BIND_AUTO_CREATE);   
    }
    
    @Override
    public synchronized void onPause() 
    {
        super.onPause();
    }

    @Override
    public void onStop() 
    {
        super.onStop();
    }

    @Override
    public void onDestroy() 
    {
       super.onDestroy();
       
       if (AIRS_remotely!=null)
       {
		   if (AIRS_remotely.running == false)
			   getApplicationContext().stopService(new Intent(this, AIRS_remote.class));
		   // unbind from service
		   getApplicationContext().unbindService(mConnection);
       }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
    	super.onConfigurationChanged(newConfig);
    }
 

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	MenuInflater inflater;
        menu.clear();    		
        inflater = getMenuInflater();
        inflater.inflate(R.menu.options_about, menu);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
      		try
    		{
    			HandlerUIManager.AboutDialog("AIRS V" + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName , getString(R.string.Copyright) + getString(R.string.ReleaseNotes));
    		}
    		catch(Exception e)
    		{
    		}
    		
    		return true;
    }
    
    public void onClick(View v) 
    {
		// check if persistent flag is running, indicating the AIRS has been running (and would re-start if continuing)
		if (settings.getBoolean("AIRS_local::running", false) == true)
		{
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage("AIRS has been running.\nDo you want to interrupt current running and start over? You will need to start AIRS again.")
    			   .setTitle("AIRS Local Sensing")
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
 		    			    stopService(new Intent(airs, AIRS_local.class));
 		    			    finish();
    		           }
    		       })
    		       .setNegativeButton("No", new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		                dialog.cancel();
    		           }
    		       });
    		AlertDialog alert = builder.create();
    		alert.show();
		}
		else
		{			
	        // now start sensing
            start_sensing();
       	}
    }
       
	// start RSA
	private void start_sensing()
	{
		if (AIRS_remotely != null)
		{
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage("Are you sure you want to start remote sensing?\n\nAIRS will attempt to connect to the remote application server, blocking the ability to exit the AIRS service until the connection request times out.")
    		       .setCancelable(false)
    		       .setIcon(R.drawable.icon)
    		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    						// forcefully print out midlet version given in manifest!
    						SerialPortLogger.debug("- AIRS Gateway");

    			        	progressdialog = ProgressDialog.show(AIRS_remote_tab.this, "Start remote sensing", "Please wait...", true);

    				        Message msg = mHandler.obtainMessage(START_REMOTELY);
    				        mHandler.sendMessage(msg);
    		           }
    		       })
    		       .setNegativeButton("No", new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		        	   return;
    		           }
    		       });
    		AlertDialog alert = builder.create();
    		alert.show();
		}
	}

	 // The Handler that gets information back from the other threads, starting the various services from the main thread
	public final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {
           switch (msg.what) 
           {
           case START_REMOTELY:
		        // signal to service to start sensing
		        AIRS_remotely.started = true;
		        // start service 
		        startService(new Intent(airs, AIRS_remote.class));
		        
		        // stop progress dialog
	     		progressdialog.cancel();

		        // stop activity
		        finish();
		        break;
		    default:  
           	break;
           }
       }
    };
    
    // local service connection
    private ServiceConnection mConnection = new ServiceConnection() 
    {
  	    public void onServiceConnected(ComponentName className, IBinder service) 
  	    {
  	        // This is called when the connection with the service has been
  	        // established, giving us the service object we can use to
  	        // interact with the service.  Because we have bound to a explicit
  	        // service that we know is running in our own process, we can
  	        // cast its IBinder to a concrete class and directly access it.
  	    	AIRS_remotely = ((AIRS_remote.LocalBinder)service).getService();
  	    }

  	    public void onServiceDisconnected(ComponentName className) {
  	        // This is called when the connection with the service has been
  	        // unexpectedly disconnected -- that is, its process crashed.
  	        // Because it is running in our same process, we should never
  	        // see this happen.
  	    	AIRS_remotely = null;
  	    }
  	};  
}
