/*
Copyright (C) 2004-2006 Nokia Corporation
Copyright (C) 2008-2011, Dirk Trossen, airs@dirk-trossen.de

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.view.View.OnClickListener;
import android.view.KeyEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Toast;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;

import com.airs.handlerUIs.HandlerUI;
import com.airs.helper.SerialPortLogger;
import android.widget.AdapterView.OnItemClickListener;

public class AIRS extends Activity implements OnClickListener, OnItemClickListener
{
	// Menu being in what mode?
    public static final int MENU_MAIN = 1;
    public static final int MENU_SETTINGS 	= 2;
    public static final int MENU_HANDLERS 	= 3;
    public static final int MENU_HANDLER 	= 4;
    public static final int MENU_LOCAL 		= 5;
    public static final int MENU_VALUES		= 6;
    public static final int MENU_SYNC		= 7;

    public static final int SYNC_FINISHED	= 18;
    
    public int currentMenu = MENU_MAIN;

    // handler for starting local AIRS
	public static final int START_LOCALLY = 1;
	public static final int DISCOVER_LOCALLY = 2;
	public static final int START_REMOTELY = 3;

	// Layout Views
    private ListView handlers;
    private TextView mTitle;
    public  TextView mTitle2;
    private ImageButton main_remote; 
    private ImageButton main_local;
    private ArrayList<HandlerEntry> mHandlerArrayList;
    private ProgressDialog progressdialog;
    private ListView syncFiles;
    private ArrayAdapter<String> mSyncArrayAdapter;
    
    // preferences
    private SharedPreferences settings;
  
    // other variables
    private boolean locally = true;
    private boolean locally_running = false;
    private boolean remote_running = false;
    private AIRS_local 	AIRS_locally;
    private AIRS_remote	AIRS_remotely;
    private AIRS		airs;
    public  static HandlerUI	current_handler;
    private int sync_files;
    private long synctime;
	File[] files;

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
        
        // read preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);
		
        // save activity in debug class
        SerialPortLogger.nors = this;
		// is debugging on?
   		SerialPortLogger.setDebugging(settings.getBoolean("Debug", false));
		SerialPortLogger.debug("AIRS debug output at " + Calendar.getInstance().getTime().toString());
		
		// initialize HandlerUI Manager
		HandlerUIManager.createHandlerUIs(this);

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
		
		// set window features
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
 
        // get window title fields
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle2 = (TextView) findViewById(R.id.title_right_text);
        
        setupMain();
        
        // start service and connect to it -> then discover the sensors
        startService(new Intent(this, AIRS_local.class));
        bindService(new Intent(this, AIRS_local.class), mConnection, 0);   
        // start service and connect to it
        startService(new Intent(this, AIRS_remote.class));
        bindService(new Intent(this, AIRS_remote.class), mConnection2, 0);  
        
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
			   stopService(new Intent(this, AIRS_local.class));
		   // unbind from service
		   unbindService(mConnection);
       }
       
       if (AIRS_remotely!=null)
       {
    	   if (AIRS_remotely.running == false)
    		   stopService(new Intent(this, AIRS_remote.class));
		   // unbind from service
		   unbindService(mConnection2);
       }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
    	super.onConfigurationChanged(newConfig);
    }
 
    private void setupMain() 
    {
        setContentView(R.layout.main);

        // Set up the custom title
        mTitle.setText(R.string.app_name);
        mTitle2.setText(R.string.general_menu);

        // get buttons and set onclick listener
        main_remote 	= (ImageButton)findViewById(R.id.button_remote);
        main_local 		= (ImageButton)findViewById(R.id.button_local);
        main_remote.setOnClickListener(this); 
        main_local.setOnClickListener(this);

        currentMenu = MENU_MAIN;
    }

    private void setupHandlerUIs() 
    {
        setContentView(R.layout.handlers);
        handlers 	= (ListView)findViewById(R.id.handlerList);
        // Find and set up the ListView for handler UIs
        mHandlerArrayList 	  = new ArrayList<HandlerEntry>();
        handlers.setAdapter(new MyCustomBaseAdapter(this, mHandlerArrayList));
	    handlers.setOnItemClickListener(this);
	    	    
        // initialize handler UIs
	    int i;
	    for(i=0;i<HandlerUIManager.max_handlers;i++)
	    	if (HandlerUIManager.handlers[i]!=null)
	    		mHandlerArrayList.add(HandlerUIManager.handlers[i].init());
 
        // Set up the custom title
        mTitle.setText(R.string.app_name);
        mTitle2.setText(R.string.handlers_menu);

        currentMenu = MENU_HANDLERS;
    }
    
    private void setupSync() 
    {
    	File path;
    	String filename;
    	String timestamp_s;
    	long timestamp;
    	long length;
    	int i;
    	
    	//set view
		setContentView(R.layout.sensors);
        // Set up the custom title
        mTitle.setText(R.string.app_name);
		mTitle2.setText("Choose files to be synchronized");
 
		// build up list of files
		syncFiles 	= (ListView)airs.findViewById(R.id.sensorList);
        syncFiles.setItemsCanFocus(false); 
        syncFiles.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mSyncArrayAdapter = new ArrayAdapter<String>(airs, android.R.layout.simple_list_item_multiple_choice);
        // Find and set up the ListView for paired devices
        syncFiles.setAdapter(mSyncArrayAdapter);

        // get timestamp of last sync
        synctime = settings.getLong("SyncTimestamp", 0);
        
        sync_files = 0;
    	// open file in public directory
    	path = new File(Environment.getExternalStorageDirectory(), "AIRS_values");
    	if (path.exists() == true)
    	{
    		// get files in directory
    		files = path.listFiles();
    		
    		if (files != null)
	    		// look through all files
	    		for (i=0; i<files.length;i++)
	    		{
	    			filename = files[i].getName();
	    			
	    			// consider all text files that are still writeable
	    			if ((filename.endsWith(".txt") == true))
	    			{
	    				try
	    				{
	    					// file name is timestamp
	    					timestamp_s = filename.substring(0, filename.lastIndexOf(".txt"));
	    					timestamp = (long)(Long.parseLong(timestamp_s));
	    					
	    					// if timestamp of filename more recent than synctime
	    					if (timestamp > synctime)
	    					{
		    					Date date = new Date(timestamp);
		    					length = files[i].length();
		    					
		    					// add timestamp with size to list adapter
		    					if (length >1000)
		    						mSyncArrayAdapter.add(date.toLocaleString() + " (" + String.valueOf(length/1000) + " kB)");
		    					else
		    						mSyncArrayAdapter.add(date.toLocaleString() + " (" + String.valueOf(length) + " B)");
		    					
		    					// count files to be shown
		        				sync_files++;
	    					}
	
	    				}
	    				catch(Exception e)
	    				{
	    				}
	    			}
	    		}
    	}
    	
    	if (sync_files == 0)
           	Toast.makeText(getApplicationContext(), "There are no files to be synchronized!", Toast.LENGTH_LONG).show();          
    	
        currentMenu = MENU_SYNC;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	MenuInflater inflater;
    	switch(currentMenu)
    	{
    	case MENU_MAIN:
	        menu.clear();    		
	        inflater = getMenuInflater();
	        inflater.inflate(R.menu.options_main, menu);
	        return true;
    	case MENU_LOCAL:
	        menu.clear();    		
	        inflater = getMenuInflater();
	        inflater.inflate(R.menu.options_local, menu);    		
    		return true;
    	case MENU_VALUES:
	        menu.clear();    		
	        inflater = getMenuInflater();
	        inflater.inflate(R.menu.options_local_sensing, menu);    		
    		return true;
    	case MENU_SYNC:
	        menu.clear();    		
	        inflater = getMenuInflater();
	        inflater.inflate(R.menu.options_sync, menu);    		
    		return true;    		
    	default:
	        menu.clear();    		
	        inflater = getMenuInflater();
	        inflater.inflate(R.menu.options_about, menu);
    		return true;
    	}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	int i;
    	
        switch (item.getItemId()) 
        {
        case R.id.main_about:
        	// call about dialogue
        	switch (currentMenu)
        	{
        	case MENU_MAIN:	
        		try
        		{
        			HandlerUIManager.AboutDialog("AIRS V" + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName , getString(R.string.Copyright) + getString(R.string.ReleaseNotes));
        		}
        		catch(Exception e)
        		{
        		}
        		break;
        	case MENU_HANDLERS:
        		HandlerUIManager.AboutDialog("List of handlers with settings", getString(R.string.HandlersList));
        		break;
        	case MENU_LOCAL:
        		HandlerUIManager.AboutDialog("AIRS Local", getString(R.string.LocalAbout));
        		break;
        	case MENU_SYNC:
        		HandlerUIManager.AboutDialog("Synchronize Local Recordings", getString(R.string.SyncAbout));
        		break;
        	default:
        		break;
        	}
            return true;
        case R.id.main_configure:
        	// call main settings menu
    		currentMenu = MENU_MAIN;
            Intent settingsActivity = new Intent(getBaseContext(), Prefs.class);
            settingsActivity.putExtra("Resource", R.xml.generalsettings);
            settingsActivity.putExtra("About", getString(R.string.GeneralSettings));
            settingsActivity.putExtra("AboutTitle", "General Settings");            
            startActivity(settingsActivity);
            return true;
        case R.id.main_configure_handlers:
        	// call handler settings list menu
       		setupHandlerUIs();
        	return true;
        case R.id.main_shortcut:
        case R.id.local_shortcut:
        	// intent for starting AIRS
        	Intent shortcutIntent = new Intent(this, AIRS.class);
        	shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	
        	// intent for creating the shortcut
        	Intent intent = new Intent();
        	intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        	intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "AIRS shortcut");
        	intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.icon));

        	intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        	sendBroadcast(intent);
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
        case R.id.sync_selectall:
    		if (sync_files>0)
    			for (i=0;i<sync_files;i++)
    				syncFiles.setItemChecked(i, true);
        	return true;
        case R.id.main_sync:
        	if (locally_running == true)
        		Toast.makeText(getApplicationContext(), "AIRS local sensing service is still running!\nFinish your recording before synchronizing!", Toast.LENGTH_LONG).show();  
        	else
        		setupSync();
        	return true;
        case R.id.local_unselectall:
       		AIRS_locally.unselectall();
       		return true;
        case R.id.sync_unselectall:
    		if (sync_files>0)
    			for (i=0;i<sync_files;i++)
    				syncFiles.setItemChecked(i, false);
        	return true;
        case R.id.sync_start:
        	sync_recordings();
        	return true;
        case R.id.local_sensorinfo:
        	AIRS_locally.sensor_info();
        	return true;        	
        }
        return false;
    }
    
    public void onClick(View v) 
    {
    	// dispatch depending on button pressed
    	switch(v.getId())
    	{
    	case R.id.button_remote:
        	if (locally_running == true)
        		Toast.makeText(getApplicationContext(), "AIRS local sensing service is still running!\nClick on notification message and exit the current sensing from UI that will pop up!", Toast.LENGTH_LONG).show();          
        	else
        		if (remote_running == true)
            		Toast.makeText(getApplicationContext(), "AIRS remote sensing service is still running!\nClick on notification message and exit the current sensing from UI that will pop up!", Toast.LENGTH_LONG).show();          
        		else
        		{
					locally = false;
		            start_sensing();
	        	}
            break;
    	case R.id.button_local:
        	if (locally_running == true)
        		Toast.makeText(getApplicationContext(), "AIRS local sensing service is still running!\nClick on notification message and exit the current sensing from UI that will pop up!", Toast.LENGTH_LONG).show();          
        	else
        		if (remote_running == true)
            		Toast.makeText(getApplicationContext(), "AIRS remote sensing service is still running!\nClick on notification message and exit the current sensing from UI that will pop up!", Toast.LENGTH_LONG).show();          
        		else
        		{
					locally = true;
		            start_sensing();
	        	}
            break;
     	default:
    		break;
    	}
    }
    
    public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) 
    {
	    int i, j = 0;
	    

	    // try to find the handlerUI entry
	    for(i=0;i<HandlerUIManager.max_handlers;i++)
	    	if (HandlerUIManager.handlers[i]!=null)
	    	{
	    		if (j == (int)arg3)
	    		{
	    	        currentMenu = MENU_HANDLERS;
	    			current_handler = HandlerUIManager.handlers[i];
	    			// start preferences activity with intent being set to resource ID for preferences
	                Intent settingsActivity = new Intent(getBaseContext(), Prefs.class);
	                settingsActivity.putExtra("Resource", current_handler.setDisplay());
	                settingsActivity.putExtra("About", current_handler.About());
	                settingsActivity.putExtra("AboutTitle", current_handler.AboutTitle());            
	                startActivity(settingsActivity);
	    			return;
	    		}
	    		j++;
	    	}
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) 
    {
    	if (event.getAction() == KeyEvent.ACTION_DOWN)
    		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
    			return true;
    	
 		// key de-pressed?
		if (event.getAction() == KeyEvent.ACTION_UP)
		{
			// is it the BACK key?
			if (event.getKeyCode()==KeyEvent.KEYCODE_BACK)
			{
            	switch(currentMenu)
            	{
            	case MENU_MAIN:
            	case MENU_LOCAL:
            		AlertDialog.Builder builder = new AlertDialog.Builder(this);
            		builder.setMessage("Are you sure you want to exit?")
            		       .setCancelable(false)
            		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() 
            		       {
            		           public void onClick(DialogInterface dialog, int id) 
            		           {
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
            		break;
            	case MENU_HANDLERS:
	            	setupMain();
            		break;
            	case MENU_HANDLER:
            		setupHandlerUIs();
            		break;
            	case MENU_SYNC:
	            	setupMain();
	            	break;
            	case MENU_SETTINGS:
            		currentMenu = MENU_MAIN;
                    break;
            	default:
            		break;
            	}
    			return true;
			}
		}

        return super.dispatchKeyEvent(event);
    }
    
    // sync recordings
    private void sync_recordings()
    {
    	int i, j=0;
    	String filename, timestamp_s;
    	long timestamp;
        ArrayList<Uri> uris = new ArrayList<Uri>();
    	Intent intent = new Intent();
    	boolean checked_one = false;
    	
    	// prepare intent
        intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("text/plain");
    	
        // anything there?
        if (files != null)
			// look through all files
			for (i=0; i<files.length;i++)
			{
				filename = files[i].getName();
				
				// any file there?
				if (filename != null)
					// consider all text files
					if ((filename.endsWith(".txt") == true))
					{
						try
						{
							// file name is timestamp
							timestamp_s = filename.substring(0, filename.lastIndexOf(".txt"));
							if (timestamp_s != null)
							{
								timestamp = (long)(Long.parseLong(timestamp_s));
								
								// if timestamp of filename more recent than synctime
								if (timestamp > synctime)
								{
									// is item checked in list?
									if (syncFiles.isItemChecked(j) == true)
									{
										// build and add URI   
										uris.add(Uri.fromFile(files[i]));							
										
										// checked at least one
										checked_one = true;
									}
									// count all txt files to sync in directory
									j++;
								}
							}
						}
						catch(Exception e)
						{
						}	
					}
			}
		
        // now build and start chooser intent
		if (checked_one == true)
		{
		    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			startActivityForResult(Intent.createChooser(intent,"Send Local Recordings To:"), SYNC_FINISHED);
			setupMain();
		}
		else
    		Toast.makeText(getApplicationContext(), "Select at least one file to share!", Toast.LENGTH_LONG).show();          
    }
    
	// start RSA
	private void start_sensing()
	{
		// set debugging mode
   		SerialPortLogger.setDebugging(settings.getBoolean("Debug", false));

    	// remote mode?
		if (locally == false)
		{
			if (AIRS_remotely != null)
			{
				// forcefully print out midlet version given in manifest!
				SerialPortLogger.debug("- AIRS Gateway");

	        	progressdialog = ProgressDialog.show(AIRS.this, "Start remote sensing", "Please wait...", true);

		        Message msg = mHandler.obtainMessage(START_REMOTELY);
		        mHandler.sendMessage(msg);
			}
		}
		else
		{
			// connected to service?
			if (AIRS_locally != null)
			{
	        	progressdialog = ProgressDialog.show(AIRS.this, "Start local sensing", "Please wait...", true);
		        // now place two messages in Handler queue - this is to allow for threaded start of local sensing - otherwise the UI thread will block!
		        // send message into handler queue to start locally!
		        Message msg = mHandler.obtainMessage(START_LOCALLY);
		        mHandler.sendMessage(msg);

		        // send message into handler queue to discover locally!
		        msg = mHandler.obtainMessage(DISCOVER_LOCALLY);
		        mHandler.sendMessage(msg);	        
			}
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
		if (requestCode == SYNC_FINISHED)
		{
    		// write current timestamp for later syncs
        	Editor editor = settings.edit();
			// put version code into store
            editor.putLong("SyncTimestamp", System.currentTimeMillis());
            
            // finally commit to storing values!!
            editor.commit();
		}
    	return;
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
        		   AIRS_locally.Discover(airs);
        		   progressdialog.cancel();
        	   }
        	   break;
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
  	    	AIRS_locally = ((AIRS_local.LocalBinder)service).getService();
  	        locally_running = AIRS_locally.running;
  	    }

  	    public void onServiceDisconnected(ComponentName className) {
  	        // This is called when the connection with the service has been
  	        // unexpectedly disconnected -- that is, its process crashed.
  	        // Because it is running in our same process, we should never
  	        // see this happen.
  	    	AIRS_locally = null;
  	    }
  	};  

    // local service connection
    private ServiceConnection mConnection2 = new ServiceConnection() 
    {
  	    public void onServiceConnected(ComponentName className, IBinder service) 
  	    {
  	        // This is called when the connection with the service has been
  	        // established, giving us the service object we can use to
  	        // interact with the service.  Because we have bound to a explicit
  	        // service that we know is running in our own process, we can
  	        // cast its IBinder to a concrete class and directly access it.
  	    	AIRS_remotely= ((AIRS_remote.LocalBinder)service).getService();
  	    	
  	    	// is service running after failed connection -> then stop it
  	    	if (AIRS_remotely.failed == true)
  			   stopService(new Intent(airs, AIRS_remote.class));

  	        remote_running = AIRS_remotely.running;
  	    }

  	    public void onServiceDisconnected(ComponentName className) {
  	        // This is called when the connection with the service has been
  	        // unexpectedly disconnected -- that is, its process crashed.
  	        // Because it is running in our same process, we should never
  	        // see this happen.
  	    	AIRS_remotely = null;
  	    }
  	};  

  	// Custom adapter for two line text list view with imageview (icon), defined in handlerentry.xml
  	private class MyCustomBaseAdapter extends BaseAdapter 
  	{
  		 private ArrayList<HandlerEntry> ArrayList;
  		 private LayoutInflater mInflater;

  		 public MyCustomBaseAdapter(Context context, ArrayList<HandlerEntry> results) 
  		 {
  			 ArrayList = results;
  			 mInflater = LayoutInflater.from(context);
  		 }

  		 public int getCount() 
  		 {
  			 return ArrayList.size();
  		 }

  		 public Object getItem(int position) 
  		 {
  			 return ArrayList.get(position);
  		 }

  		 public long getItemId(int position) 
  		 {
  			 return position;
  		 }

  		 public View getView(int position, View convertView, ViewGroup parent) 
  		 {
  			 ViewHolder holder;
  			 if (convertView == null) 
  			 {
  				 convertView = mInflater.inflate(R.layout.handlerentry, null);
  				 holder = new ViewHolder();
  				 holder.name = (TextView) convertView.findViewById(R.id.handlername);
  				 holder.description = (TextView) convertView.findViewById(R.id.handlerdescription);
  				 holder.image = (ImageView)convertView.findViewById(R.id.handlerimage);

  				 convertView.setTag(holder);
  			 } 
  			 else 
  			 {
  				 holder = (ViewHolder) convertView.getTag();
  			 }
  		  
  			 holder.name.setText(ArrayList.get(position).name);
  			 holder.description.setText(ArrayList.get(position).description);
  			 holder.image.setImageResource(ArrayList.get(position).resid);

  		  return convertView;
  		 }

  		 class ViewHolder 
  		 {
  		  TextView name;
  		  TextView description;
  		  ImageView image;
  		 }
  	}
}