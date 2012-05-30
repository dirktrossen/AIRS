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
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.content.*;
import android.content.SharedPreferences.Editor;

import com.airs.handlerUIs.HandlerUI;

public class AIRS_sync_tab extends Activity
{
    public static final int SYNC_FINISHED	= 18;
    
	// Layout Views
    private ListView syncFiles;
    private ArrayAdapter<String> mSyncArrayAdapter;
    
    // preferences
    private SharedPreferences settings;
  
    // other variables
    private AIRS_sync_tab		airs;
    public  static HandlerUI	current_handler;
    private int sync_files;
    private long synctime, end_synctime;
    private boolean check_end_synctime = false;
	File[] files;
	public String prefs_file;

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
		
        // setup resources
        setupSync();
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
    }
    
    private void setupSync() 
    {
    	File path;
    	String filename;
    	String timestamp_s;
    	long timestamp;
    	long length;
    	int i;
    	boolean to_sync;
    	
		// check if persistent flag is running, indicating the AIRS has been running
		if (settings.getBoolean("AIRS_local::running", false) == true)
		{
			// read current recording file timestamp
			end_synctime = Long.valueOf(settings.getString("AIRS_local::recording_file", "0"));
			check_end_synctime = true;
		}

    	//set view
		setContentView(R.layout.sensors);

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
	    					
	    					// assume no sync'ing
	    					to_sync = false;
	    					// is timestamp of filename more recent than synctime? -> sync
	    					if (timestamp > synctime)
	    						to_sync = true;
	    					// is timestamp larger than end_synctime (which should only be the current recording file? -> don't sync
	    					if (check_end_synctime == true && timestamp >= end_synctime)
	    						to_sync = false;
	    						
	    					if (to_sync == true)
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
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	MenuInflater inflater;
        menu.clear();    		
        inflater = getMenuInflater();
        inflater.inflate(R.menu.options_sync, menu);    		
		return true;    		
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	int i;
    	
        switch (item.getItemId()) 
        {
        case R.id.main_about:
        	// call about dialogue
       		HandlerUIManager.AboutDialog("Synchronize Local Recordings", getString(R.string.SyncAbout));
            return true;
        case R.id.sync_selectall:
    		if (sync_files>0)
    			for (i=0;i<sync_files;i++)
    				syncFiles.setItemChecked(i, true);
        	return true;
        case R.id.sync_unselectall:
    		if (sync_files>0)
    			for (i=0;i<sync_files;i++)
    				syncFiles.setItemChecked(i, false);
        	return true;
        case R.id.sync_start:
        	sync_recordings();
        	return true;
        }
        return false;
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
    	boolean to_sync;

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

								// assume no sync'ing
		    					to_sync = false;
		    					// is timestamp of filename more recent than synctime? -> sync
		    					if (timestamp > synctime)
		    						to_sync = true;
		    					// is timestamp larger than end_synctime (which should only be the current recording file? -> don't sync
		    					if (check_end_synctime == true && timestamp >= end_synctime)
		    						to_sync = false;
		    						
		    					if (to_sync == true)
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
		}
		else
    		Toast.makeText(getApplicationContext(), "Select at least one file to share!", Toast.LENGTH_LONG).show();          
    }
    
	public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
		if (requestCode == SYNC_FINISHED)
		{
    		// write current timestamp for later syncs
        	Editor editor = settings.edit();
			// put sync timestamp into store
        	if (check_end_synctime == true)		// if there is a recording ongoing, sync timestamp is just before the current recording!
                editor.putLong("SyncTimestamp", end_synctime - 1);
        	else        		
        		editor.putLong("SyncTimestamp", System.currentTimeMillis());
            
            // finally commit to storing values!!
            editor.commit();
		}
    	return;
    }
}
