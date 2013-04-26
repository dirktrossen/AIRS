/*
Copyright (C) 2012-2013, Dirk Trossen, airs@dirk-trossen.de

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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.airs.helper.SerialPortLogger;

public class AIRS_record_tab extends Activity implements OnClickListener
{
    // handler for starting local AIRS
	public static final int START_REMOTELY = 3;
	public static final int UPDATE_DOWNLOAD = 1;

	// expose template for other tabs
	public static String current_template = "";
	
	// Layout Views
    private ImageButton main_record;
    private Spinner main_spinner;
    private ProgressDialog progressdialog;

    // preferences
    private SharedPreferences settings;
  
    // other variables
    private AIRS_local 	AIRS_locally;
    private AIRS_remote	AIRS_remotely;
    private AIRS_record_tab	airs;
    
	private MyCustomBaseAdapter customAdapter;
	private List<String> annotations;
    private ListView annotation_list;
    private Button delete_button, shortcut_button, download_button;
    private ProgressBar pb;
    private Dialog dialog;
	private int selected_text = -1;
	private String[] remote_templates = {"Scenario_1", "Scenario_2", "Scenario_3"};
	private int remote_file, downloaded_file;

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
        setContentView(R.layout.recording);

        // get buttons and set onclick listener
        main_record = (ImageButton)findViewById(R.id.button_record);
        main_record.setOnClickListener(this);
        // get spinner
        main_spinner = (Spinner)findViewById(R.id.spinner_record);
              
        // set up list view
        annotation_list = (ListView)findViewById(R.id.templates_list);
        annotation_list.setItemsCanFocus(true); 
        annotation_list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        annotation_list.setSelected(true);
        annotation_list.setEnabled(true);
        annotations = new ArrayList<String>();

        // hook buttons
        delete_button = (Button)findViewById(R.id.templates_delete);
        delete_button.setOnClickListener(this);
        shortcut_button = (Button)findViewById(R.id.templates_shortcut);
        shortcut_button.setOnClickListener(this);	        
        download_button = (Button)findViewById(R.id.templates_download);
        download_button.setOnClickListener(this);	
                
        // get template entries
        gatherFiles();            

	    // start service and connect to it -> then discover the sensors
        getApplicationContext().startService(new Intent(this, AIRS_local.class));
        getApplicationContext().bindService(new Intent(this, AIRS_local.class), mConnection, Service.BIND_AUTO_CREATE);   

	    // start service and connect to it -> then discover the sensors
        getApplicationContext().startService(new Intent(this, AIRS_remote.class));
        getApplicationContext().bindService(new Intent(this, AIRS_remote.class), mConnection2, Service.BIND_AUTO_CREATE);   

		// check if persistent flag is running, indicating the AIRS has been running (and would re-start if continuing)
		if (settings.getBoolean("AIRS_local::running", false) == true)
		{
	    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage(getString(R.string.AIRS_running_exit))
    			   .setTitle(getString(R.string.AIRS_Sensing))
    		       .setCancelable(false)
    		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
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
 		    			    stopService(new Intent(airs, AIRS_remote.class));
 		    			    finish();
    		           }
    		       })
    		       .setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
		    			    finish();
    		                dialog.cancel();
    		           }
    		       });
    		AlertDialog alert = builder.create();
    		alert.show();
		}
		
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
    			HandlerUIManager.AboutDialog(getString(R.string.WhatsNew2) , getString(R.string.WhatsNew));
	        }
        }
        catch(Exception e)
        {
        }            
    }

    @Override
    public synchronized void onResume() 
    {
        super.onResume();
        
        // refresh list of template files
        gatherFiles();        
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
       
       if (AIRS_remotely!=null)
       {
		   if (AIRS_remotely.running == false)
			   getApplicationContext().stopService(new Intent(this, AIRS_remote.class));
		   // unbind from service
		   getApplicationContext().unbindService(mConnection2);
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
		inflater.inflate(R.menu.options_main, menu);    			
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    	
    	Intent intent;
    	
        switch (item.getItemId()) 
        {
        case R.id.main_copyright:
    		try
    		{
    			HandlerUIManager.AboutDialog("AIRS V" + this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName , getString(R.string.Copyright) + getString(R.string.ReleaseNotes));
    		}
    		catch(Exception e)
    		{
    		}
    		break;
        case R.id.main_about:
			HandlerUIManager.AboutDialog(getString(R.string.Help) , getString(R.string.RecordAbout));
			break;
        case R.id.main_manual:
        	intent = new Intent(this,AIRS_manual.class);
        	startActivity(intent);
        	break;
        case R.id.main_dbadmin:
        	intent = new Intent(this,AIRS_DBAdmin.class);
        	startActivity(intent);
        	break;
        }
        return false;
    }
        
    public void onClick(View v) 
    {
    	AlertDialog.Builder builder;
    	AlertDialog alert;
	    String dirPath;
        File shortcutFile;

    	switch(v.getId())
    	{
    	case R.id.button_record:
    		if (main_spinner.getSelectedItemPosition() == 0)
    		{
	    		// check if persistent flag is running, indicating the AIRS has been running (and would re-start if continuing)
	    		if (settings.getBoolean("AIRS_local::running", false) == true)
	    		{
	        		builder = new AlertDialog.Builder(this);
	        		builder.setMessage(getString(R.string.AIRS_running_exit))
	        			   .setTitle(getString(R.string.AIRS_Sensing))
	        		       .setCancelable(false)
	        		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
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
	     		    			    stopService(new Intent(airs, AIRS_remote.class));
	     		    			    finish();
	        		           }
	        		       })
	        		       .setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() 
	        		       {
	        		           public void onClick(DialogInterface dialog, int id) 
	        		           {
	        		                dialog.cancel();
	        		           }
	        		       });
	        		alert = builder.create();
	        		alert.show();
	    		}
	    		else
	    		{		           
		     		// start measurements now 
		    		if (AIRS_locally != null)
		    		{
		    			// merely restart without GUI
		    			AIRS_locally.Restart(false);
		                // service running message
		               	Toast.makeText(getApplicationContext(), getString(R.string.AIRS_started_local), Toast.LENGTH_LONG).show();     
		               	// finish UI
		    			finish();
		    		}
	           	}
    		}
    		else
    		{
	    		// check if persistent flag is running, indicating the AIRS has been running (and would re-start if continuing)
	    		if (settings.getBoolean("AIRS_local::running", false) == true)
	    		{
	        		builder = new AlertDialog.Builder(this);
	        		builder.setMessage(getString(R.string.AIRS_running_exit))
	        			   .setTitle(getString(R.string.AIRS_Sensing))
	        		       .setCancelable(false)
	        		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
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
	     		    			    stopService(new Intent(airs, AIRS_remote.class));
	     		    			    finish();
	        		           }
	        		       })
	        		       .setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() 
	        		       {
	        		           public void onClick(DialogInterface dialog, int id) 
	        		           {
	        		                dialog.cancel();
	        		           }
	        		       });
	        		alert = builder.create();
	        		alert.show();
	    		}
	    		else
	    		{			
	    	        // now start sensing
	                start_sensing();
	           	}
    		}
    		break;
    	case R.id.templates_delete:
    		if (annotations.size() == 0 || selected_text == -1)
              	Toast.makeText(getApplicationContext(), getString(R.string.At_least_one_template2), Toast.LENGTH_LONG).show();          
    		else
    		{
	    		// build dialog box
	    		builder = new AlertDialog.Builder(this);
	    		builder.setIcon(android.R.drawable.ic_menu_delete)
	    		       .setTitle(getString(R.string.Delete_template))
	    		       .setMessage(annotations.get(selected_text))
	    		       .setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() 
		    		       {
		    		           public void onClick(DialogInterface dialog, int id) 
		    		           {
		    		                dialog.cancel();
		    		           }
		    		       })
	    		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
		    		       {
		    		           public void onClick(DialogInterface dialog, int id) 
		    		           {
		    		        	   File external_storage = getExternalFilesDir(null);
		    		        	   
		    		        	   if (external_storage != null)
		    		        	   {
			    		        	   String dirPath = external_storage.getAbsolutePath() + "/" + "templates";
			    		        	   File shortcutFile = new File(dirPath, annotations.get(selected_text));
			    		        	   shortcutFile.delete();
		
			    		   		       // gather list of files again
			    		        	   gatherFiles();
		    		        	   }
		    		        	   dialog.dismiss();
		    		           }
		    		       });
	    		alert = builder.create();
	    		alert.show(); 
    		}
    		break;
    	case R.id.templates_shortcut:
    		if (annotations.size() == 0 || selected_text == -1)
              	Toast.makeText(getApplicationContext(), getString(R.string.At_least_one_template), Toast.LENGTH_LONG).show();          
    		else
    		{
    			File external_storage = getExternalFilesDir(null);
        	   
    			if (external_storage != null)
    			{
		    		// get current template file
		        	dirPath = external_storage.getAbsolutePath() + "/" + "templates";
		            shortcutFile = new File(dirPath, annotations.get(selected_text));
		
		            // intent for starting AIRS
		    	    Intent intent;
		        	Intent shortcutIntent = new Intent(Intent.ACTION_MAIN); 
		        	shortcutIntent.setClassName(airs, AIRS_shortcut.class.getName()); 
		        	
		        	shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		        	shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		        	shortcutIntent.putExtra("preferences", shortcutFile.toString());
		
		        	// intent for creating the shortcut
		        	intent = new Intent();
		        	intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		        	intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, annotations.get(selected_text));
		        	intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.icon));
		
		        	intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		        	sendBroadcast(intent);    
		        	
		           	Toast.makeText(getApplicationContext(), getString(R.string.Created_shortcut), Toast.LENGTH_LONG).show();          
    			}
    		}
    		break;
    	case R.id.templates_download:
    		downloaded_file = 0;
    		showProgress(remote_templates.length);
    		for (remote_file=0;remote_file<remote_templates.length;remote_file++)
    			new DownloadThread(remote_templates[remote_file]);
			break;
    	case R.id.spinner_record:
    		break;
	   	default:
	   		// select by clicking and load right away
    	   	selected_text = customAdapter.set(v);
    	   	if (selected_text != -1)
    	   	{
	    		// IMPORTANT: any change in restoring the settings in AIRS_shortcut needs to be copied over here, too!!!
	    		// build dialog box
	    		builder = new AlertDialog.Builder(this);
	    		builder.setIcon(android.R.drawable.ic_menu_recent_history)
	    		       .setTitle(getString(R.string.Load_template))
	    		       .setMessage(getString(R.string.Load_template2))
	    		       .setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() 
		    		       {
		    		           public void onClick(DialogInterface dialog, int id) 
		    		           {
		    		                dialog.cancel();
		    		           }
		    		       })
	    		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
		    		       {
		    		           public void onClick(DialogInterface dialog, int id) 
		    		           {
			    		           	long synctime;
			    		    	    int version, i; 
			    		    	    boolean tables, tables2;
			    		    	    String music;
			    		    	    String dirPath;
			    		            File shortcutFile;
	
			    		       		// get current template file
			    		            File external_storage = getExternalFilesDir(null);
			    		            
			    		            if (external_storage != null)
			    		            {
				    		       		dirPath = external_storage.getAbsolutePath() + "/" + "templates";
		  	    		                shortcutFile = new File(dirPath, annotations.get(selected_text));
			
				    		   	        // get default preferences
				    		   	        settings = PreferenceManager.getDefaultSharedPreferences(airs);
				    		   	        
				    		   	        // get values that should not be overwritten!
				    		   	        synctime = settings.getLong("SyncTimestamp", 0);
				    		   	        version = settings.getInt("Version", 0);	
				    		   	        tables = settings.getBoolean("AIRS_local::TablesExists", false);	
				    		   	        tables2 = settings.getBoolean("AIRS_local::Tables2Exists", false);	
				    			        music = settings.getString("MusicPlayerHandler::Music", "");

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
				    		   	        
				    		           	File preferenceFile = new File(getFilesDir(), "../shared_prefs/com.airs_preferences.xml");
			
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
				    		   	        
				    		   	        // get default preferences
				    		   	        settings = PreferenceManager.getDefaultSharedPreferences(airs);
				    		   			Editor editor = settings.edit();
				    		   			
				    		   			// write certain back in order for them to not be overwritten!
				    		   			editor.putLong("SyncTimestamp", synctime);
				    		   			editor.putInt("Version", version);
				    		   			editor.putBoolean("AIRS_local::TablesExists", tables);
				    		   			editor.putBoolean("AIRS_local::Tables2Exists", tables2);
				    					editor.putString("MusicPlayerHandler::Music", music);

				    		   			// put back all entries related to event annotations
				    		   			for (i=0;i<own_events;i++)
				    		   				editor.putString("EventButtonHandler::Event"+Integer.toString(i), event[i]);
				    		   			editor.putString("EventButtonHandler::EventSelected", event_selected_entry);
			
				    		   			editor.commit();
				    		   			
				    		   			// notify user
			    		              	Toast.makeText(getApplicationContext(), getString(R.string.Restored_settings) + " '" + annotations.get(selected_text) + "'", Toast.LENGTH_LONG).show();          
			    		              	
			    		              	// save templates for other tabs to use
			    		              	current_template = new String(annotations.get(selected_text));
			    		            }	
		    		                dialog.dismiss();
		    		           }
		    		       });
	    		alert = builder.create();
	    		alert.show();  
    	   	}
	   		break;
    	}
    }
       
	// start RSA
	private void start_sensing()
	{
		if (AIRS_remotely != null)
		{
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage(getString(R.string.Start_remote_sensing))
    		       .setCancelable(false)
    		       .setIcon(R.drawable.icon)
    		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    						// forcefully print out midlet version given in manifest!
    						SerialPortLogger.debug("- AIRS Gateway");

    			        	progressdialog = ProgressDialog.show(AIRS_record_tab.this, "Start remote sensing", "Please wait...", true);

    			        	// start service from handler
    				        Message msg = mHandler.obtainMessage(START_REMOTELY);
    				        mHandler.sendMessage(msg);
    		           }
    		       })
    		       .setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() 
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
           case UPDATE_DOWNLOAD:
    			// count downloaded files
        		downloaded_file++;
            	pb.setProgress(downloaded_file);
            	// if we got all, dismiss dialog and gather the files from local directory
            	if (downloaded_file == remote_templates.length)
            	{
            		dialog.dismiss();
            		gatherFiles();
            	}
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
  	
    // remote service connection
    private ServiceConnection mConnection2 = new ServiceConnection() 
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
  	
    private class DownloadThread implements Runnable
    {
    	private String template_name;
    	
    	DownloadThread(String name)
    	{
    		template_name = new String(name);
			new Thread(this).start();
    	}
  
	    public void run()
	    {
	    	int total_size = 0;
			try 
			{
				URL url = new URL("http://www.tecvis.co.uk/wp-content/uploads/story_templates/" + template_name);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
				
				urlConnection.setRequestMethod("GET");
				urlConnection.setDoOutput(true);
	
				//connect
				urlConnection.connect();
	
				//set the path where we want to save the fileÊÊÊÊÊÊÊÊÊÊ 
	        	String dirPath = getExternalFilesDir(null).getAbsolutePath() + "/" + "templates";
	        	File shortcutPath = new File(dirPath);
	        	if (!shortcutPath.exists())
	        		shortcutPath.mkdirs();
	
	        	//create a new file, to save the downloaded file 
				File file = new File(shortcutPath, template_name);
				FileOutputStream fileOutput = new FileOutputStream(file);
	
    			SerialPortLogger.debugForced("download: created local file " + template_name);

				//Stream used for reading the data from the internet
				InputStream inputStream = urlConnection.getInputStream();
	
				//create a buffer...
				byte[] buffer = new byte[1024];
				int bufferLength = 0;
	
    			SerialPortLogger.debugForced("download: fetch file " + template_name);

				while ( (bufferLength = inputStream.read(buffer)) > 0 ) 
				{
					fileOutput.write(buffer, 0, bufferLength);
					total_size += bufferLength;
				} 
				//close the output stream when complete //
				fileOutput.close();				
				
    			SerialPortLogger.debugForced("download: file " + template_name + " complete with size=" + String.valueOf(total_size));
		         
				// now update progress dialog, possibly dismiss and gather files again
				mHandler.sendMessage(mHandler.obtainMessage(UPDATE_DOWNLOAD));
			}
			catch (final Exception e) 
			{    				
				// now update progress dialog, possibly dismiss and gather files again
				mHandler.sendMessage(mHandler.obtainMessage(UPDATE_DOWNLOAD));
			}
	    }
    }
    
    void showProgress(int max_files)
    {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.downloadprogress);
        dialog.setTitle("Download Progress");
         
        pb = (ProgressBar)dialog.findViewById(R.id.downloadprogress_progress_bar);
        pb.setProgress(0);
        pb.setMax(max_files);
        dialog.show();
    }
    
    // read list of template files from external storage
    private void gatherFiles()
    {
		int i;
		File external_storage;

		// clear annotation list and timestamps
		annotations.clear();
		selected_text = -1;
		// get adapter and set it
        customAdapter = new MyCustomBaseAdapter(this, annotations);
        annotation_list.setAdapter(customAdapter);
		
    	// path for templates
        external_storage = getExternalFilesDir(null);
        if (external_storage != null)
        {
			File shortcutPath = new File(external_storage.getAbsolutePath() + "/templates/");
	
			// get files in directory
			String [] file_list = shortcutPath.list(null);
			
			// add to list to show
			if (file_list != null)
				for (i=0;i<file_list.length;i++)
					annotations.add(file_list[i]);
        }
    }
    
  	// Custom adapter for radio + text list entry, defined in manage_template_entry.xml
  	private class MyCustomBaseAdapter extends BaseAdapter 
  	{
  		 private List<String> ArrayList;
  		 private List<ViewHolder> viewHolder;
  		 private LayoutInflater mInflater;

  		 public MyCustomBaseAdapter(Activity context, List<String> results) 
  		 {
  			 ArrayList = results;
  			 viewHolder = new ArrayList<ViewHolder>();
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

  		 public int set(View button)
  		 {
  			 int i, j;
  			 
  			 for (i=0;i<ArrayList.size();i++)
  				 // found button?
  				 if (viewHolder.get(i).entry == button || viewHolder.get(i).checked == button)
  				 {
  					 // set this button and reset all others
  					 viewHolder.get(i).checked.setChecked(true);
  					 for (j=0;j<ArrayList.size();j++)
  						 if (j!=i)
  		  					 viewHolder.get(j).checked.setChecked(false);
  					
  					 return i;
  				 }
  			 
  			 return -1;
  		 }

  		 public void set(int selected)
  		 {
  			 int i;
  			 
			 // set this button and reset all others
  			 viewHolder.get(selected).checked.setChecked(true);

			 for (i=0;i<ArrayList.size();i++)
				 if (i!=selected)
  					 viewHolder.get(i).checked.setChecked(false);  					
  		 }

  		 public View getView(int position, View convertView, ViewGroup parent) 
  		 {
  			 ViewHolder holder;
  			 if (convertView == null) 
  			 {
  				 convertView = mInflater.inflate(R.layout.manage_template_entry, null);
  				 holder = new ViewHolder();
  				 holder.entry = (TextView) convertView.findViewById(R.id.manage_template_entry_string);
  				 holder.entry.setOnClickListener(airs);
  				 holder.checked = (RadioButton) convertView.findViewById(R.id.manage_template_entry_check);
  				 holder.checked.setOnClickListener(airs);

  				 convertView.setTag(holder);
  				 viewHolder.add(holder);
  			 } 
  			 else 
  			 {
  				 holder = (ViewHolder) convertView.getTag();
  			 }
  		  
  			 // put text in list
  			 holder.entry.setText(ArrayList.get(position));
  			 
  			 // if selected, set to checked
  			 if (position == selected_text)
  				 holder.checked.setChecked(true);
  			 
  			 // set id for check
  			 convertView.setId(R.id.templates_list);
  			 return convertView;
  		 }

  		 class ViewHolder 
  		 {
  		  TextView entry;
  		  RadioButton checked;
  		 }
  	}
}

