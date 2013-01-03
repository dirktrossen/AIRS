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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Calendar;

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
import android.widget.Toast;

import com.airs.helper.SerialPortLogger;

public class AIRS_local_tab extends Activity implements OnClickListener
{
    // handler for starting local AIRS
	public static final int START_LOCALLY = 1;
	public static final int DISCOVER_LOCALLY = 2;
	public static final int START_REMOTELY = 3;

	// Layout Views
    private ImageButton main_local;
    private ImageButton main_local_select;
    private ProgressDialog progressdialog;

    // preferences
    private SharedPreferences settings;
  
    // other variables
    private boolean 	discovery = false;
    private AIRS_local 	AIRS_locally;
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
		
        // save activity in debug class
        SerialPortLogger.nors = this;
		// is debugging on?
   		SerialPortLogger.setDebugging(settings.getBoolean("Debug", false));
		SerialPortLogger.debug("AIRS debug output at " + Calendar.getInstance().getTime().toString());
		
		// initialize HandlerUI Manager
		HandlerUIManager.createHandlerUIs(this);
		
        // set content of View
        setContentView(R.layout.local);

        TextView mText = (TextView) findViewById(R.id.local_text);
        mText.setMovementMethod(new ScrollingMovementMethod());

        // get buttons and set onclick listener
        main_local 		= (ImageButton)findViewById(R.id.button_local);
        main_local.setOnClickListener(this);
        main_local_select = (ImageButton)findViewById(R.id.button_local_select);
        main_local_select.setOnClickListener(this);
              
	    // start service and connect to it -> then discover the sensors
        getApplicationContext().startService(new Intent(this, AIRS_local.class));
        getApplicationContext().bindService(new Intent(this, AIRS_local.class), mConnection, Service.BIND_AUTO_CREATE);   

        // check if app has been updated
        try
        {
        	// is stored version code different from the package's?
	        if (this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode != settings.getInt("Version", 0))
	        {
	        	// get editor for settings
	        	Editor editor = settings.edit();
    			// put version code into store
                editor.putInt("Version", this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode);
                
                // finally commit to storing values!!
                editor.commit();
                
                // and now show what's new
    			HandlerUIManager.AboutDialog("What's new in AIRS?" , getString(R.string.WhatsNew));
	        }
        }
        catch(Exception e)
        {
        }       
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
       
       if (AIRS_locally!=null)
       {
		   if (AIRS_locally.running == false)
			   getApplicationContext().stopService(new Intent(this, AIRS_local.class));
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
        if (discovery == false)
        	inflater.inflate(R.menu.options_main, menu);
        else
        	inflater.inflate(R.menu.options_local, menu);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    	
    	final Intent intent;
    	
        switch (item.getItemId()) 
        {
        case R.id.main_about:
        	if (discovery == false)
        	{
	    		try
	    		{
	    			HandlerUIManager.AboutDialog("AIRS V" + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName , getString(R.string.Copyright) + getString(R.string.ReleaseNotes));
	    		}
	    		catch(Exception e)
	    		{
	    		}
        	}
        	else
        		HandlerUIManager.AboutDialog("AIRS Local", getString(R.string.LocalAbout));
    		break;
        case R.id.main_shortcut:
    		// check if persistent flag is running, indicating the AIRS has been running (and would re-start if continuing)
    		if (settings.getBoolean("AIRS_local::running", false) == true)
    		{
        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
        		builder.setMessage("AIRS has been running.\nYou need to stop AIRS before you can create a shortcut on your homescreen! Do you want to stop AIRS now?")
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
	        	File preferenceFile = new File(getFilesDir(), "../shared_prefs/com.airs_preferences.xml");
	    		File shortcutPath = new File(getExternalFilesDir(null).getAbsolutePath());
	            File shortcutFile = new File(shortcutPath, "shortcutPrefs_" + String.valueOf(System.currentTimeMillis()) + ".xml");
	        	
	        	// intent for starting AIRS
	        	Intent shortcutIntent = new Intent(Intent.ACTION_MAIN); 
	        	shortcutIntent.setClassName(this, AIRS_shortcut.class.getName()); 
	        	
	        	shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        	shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	// copy preference file if original preferences exist
	        	if (preferenceFile.exists() == true)
	        	{
		            try
		            {
		                FileChannel src = new FileInputStream(preferenceFile).getChannel();
		                FileChannel dst = new FileOutputStream(shortcutFile).getChannel();
		                dst.transferFrom(src, 0, src.size());
		                src.close();
		                dst.close();
		            	shortcutIntent.putExtra("preferences", shortcutFile.toString());
		            }
		            catch(Exception e)
		            {
		            }
	        	}        		
	        	
	        	// intent for creating the shortcut
	        	intent = new Intent();
	        	intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
	        	intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Quick AIRS");
	        	intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.icon));
	
	        	intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
	        	sendBroadcast(intent);	 
    		}
        	break;
        case R.id.main_sync:
        	intent = new Intent(this,AIRS_sync.class);
        	startActivity(intent);
        	break;
        case R.id.main_dbadmin:
        	intent = new Intent(this,AIRS_DBAdmin.class);
        	startActivity(intent);
        	break;
        case R.id.local_start:
        	// debugging on during measurements?
       		SerialPortLogger.setDebugging(settings.getBoolean("Debug", false));

        	// start service again - it should be all discovered now!
            startService(new Intent(this, AIRS_local.class));
            // service running now?
           	Toast.makeText(getApplicationContext(), "Started AIRS local service!\nYou can see its current view by clicking on the notification bar update.", Toast.LENGTH_LONG).show();          
            finish();
        	return true;
        case R.id.local_selectall:
       		AIRS_locally.selectall();
        	return true;
        case R.id.local_unselectall:
       		AIRS_locally.unselectall();
       		return true;
        case R.id.local_sensorinfo:
        	AIRS_locally.sensor_info();
        	return true;        	
        }
        return false;
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
	    	// dispatch depending on button pressed
	    	switch(v.getId())
	    	{
	    	case R.id.button_local_select:
		        // now start sensing with selecting sensors first
	            start_sensing();
	            break;
	    	case R.id.button_local:
	     		// use Restart() function in order to start without GUIs
	    		if (AIRS_locally != null)
	    		{
	    			AIRS_locally.Restart(false);
	                // service running message
	               	Toast.makeText(getApplicationContext(), "Started AIRS local service!\nYou can see its current view by clicking on the notification bar update.", Toast.LENGTH_LONG).show();     
	               	// finish UI
	    			finish();
	    		}
	    		break;
	    	}	
       	}
    }
       
	// start RSA
	private void start_sensing()
	{
		if (AIRS_locally != null)
		{
			// set debugging mode
	   		SerialPortLogger.setDebugging(settings.getBoolean("Debug", false));
	
	    	progressdialog = ProgressDialog.show(AIRS_local_tab.this, "Gathering local sensors", "Please wait...", true);
	        // now place two messages in Handler queue - this is to allow for threaded start of local sensing - otherwise the UI thread will block!
	        // send message into handler queue to start locally!
	        Message msg = mHandler.obtainMessage(START_LOCALLY);
	        mHandler.sendMessage(msg);
	
	        // send message into handler queue to discover locally!
	        msg = mHandler.obtainMessage(DISCOVER_LOCALLY);
	        mHandler.sendMessage(msg);	 
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
           case START_LOCALLY:
        	   // start locally -> initialize handlers
        	   AIRS_locally.start = true;
		       startService(new Intent(airs, AIRS_local.class));
        	   break;
           case DISCOVER_LOCALLY:
        	   // if not yet finished starting, send message again for 100ms later
        	   if (AIRS_locally.started==false)
        	   {
        		   // now discover locally
	   		       msg = mHandler.obtainMessage(DISCOVER_LOCALLY);
			       mHandler.sendMessageDelayed(msg, 100);	        
        	   }
        	   else
        	   {
        		   AIRS_locally.Discover(AIRS_local_tab.this);
        		   progressdialog.cancel();
        		   discovery = true;
        	   }
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
  	    	AIRS_locally = ((AIRS_local.LocalBinder)service).getService();
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
