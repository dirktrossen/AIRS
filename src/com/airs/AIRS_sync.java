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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class AIRS_sync extends Activity implements OnClickListener
{
	// states for handler 
	public static final int START_ACTIVITY	= 1;
	public static final int FINISH_ACTIVITY	= 2;
	public static final int UPDATE_VALUES	= 3;
	public static final int FINISH_NO_VALUES_ACTIVITY	= 4;

	// states for activity management
	public static final int SYNC_FINISHED	= 18;
    public static final int SYNC_BATCH		= 1000;
        
    // preferences
    private SharedPreferences settings;
  
    // other variables
	private TextView mTitle;
	private TextView mTitle2;
	private TextView ProgressText;
    private long synctime, end_synctime;
    private boolean check_end_synctime = false;
    private boolean remove_files = false;
	private int max_recording_size;
	private int read_data_entries = 0;
    private ArrayList<Uri> uris = new ArrayList<Uri>();
    private ArrayList<File> files = new ArrayList<File>();

    // database variables
    AIRS_database database_helper;
    SQLiteDatabase airs_storage;

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
        
        // get default preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);
		
        // setup resources
		// check if persistent flag is running, indicating the AIRS has been running
		if (settings.getBoolean("AIRS_local::running", false) == true)
		{
			// read current recording file timestamp
			end_synctime = Long.valueOf(settings.getString("AIRS_local::recording_file", "0"));
			check_end_synctime = true;
		}

        // now open database
        database_helper = new AIRS_database(this.getApplicationContext());
        airs_storage = database_helper.getReadableDatabase();

		// set custom title
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.sync_dialog);		
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
 
        // get window title fields
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle2 = (TextView) findViewById(R.id.title_right_text);
        mTitle.setText(R.string.app_name);
        mTitle2.setText("Synchronizing Recordings");

        // get progress text view
        ProgressText = (TextView) findViewById(R.id.sync_progresstext);
        ProgressText.setText("Start synchronizing");

        // get cancel button
        Button cancel 		= (Button)findViewById(R.id.sync_cancel);
        cancel.setOnClickListener(this);

        // get timestamp of last sync
        synctime = settings.getLong("SyncTimestamp", 0);
        
        // get maximum size of recording files
        max_recording_size = Integer.parseInt(settings.getString("RecordingSize", "512")) * 1000;

        // get remove temp files 
        remove_files = settings.getBoolean("SyncRemoveFiles", false);

        new SyncThread();
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
        
    public void onClick(View v) 
    {
		int i;
		File current;

    	if (v.getId() == R.id.sync_cancel)
    	{
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage("Do you want to interrupt synchronization?")
    			   .setTitle("Synchronizing Local Recordings")
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
    	}
    	
    	if (v.getId() == R.id.sync_finish)
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
                        
            // remove temp files?
            if (remove_files == true)
            	for (i=0;i<files.size();i++)
            	{
            		current = files.get(i);
            		current.delete();
            	}
    		
            // now finish activity
            finish();    		
    	}
    }   
	
	// The Handler that gets information back from the other threads, updating the values for the UI
	public final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {
	       Intent intent = new Intent(), chosen;
	       int i;

           switch (msg.what) 
           {
           case START_ACTIVITY:        	   
		     	// prepare intent for choosing sharing
		        intent = new Intent(Intent.ACTION_SEND);
		        intent.setType("text/plain");
		        chosen = Intent.createChooser(intent,"Send Local Recordings To:");
		        
		        // anything chosen?
		        if (chosen!=null)
		        {
		        	// now send files with chosen method
			        for (i=0;i<uris.size();i++)
			        {
					    intent.putExtra(Intent.EXTRA_STREAM, uris.get(i));
						startActivity(chosen);
			        }		        
		        }	        
           	break;
           case FINISH_NO_VALUES_ACTIVITY:
   	  			Toast.makeText(getApplicationContext(), "There are no values to synchronize!", Toast.LENGTH_LONG).show();
   	  			finish();
           case FINISH_ACTIVITY:
        	   finish();
        	   break;
           case UPDATE_VALUES:
	    		ProgressText.setText("Read DB Entries: " + String.valueOf(msg.getData().getLong("Value")));
	    		break;
           default:  
           	break;
           }
       }
    };

	private class SyncThread implements Runnable
	{
	 	private Thread thread;

		SyncThread()
		{
			(thread = new Thread(this)).start();
		}
	     public void run()
	     {
	    	String query;
			int t_column, s_column, v_column;
			Cursor values;
			String value, symbol;
			String line_to_write;
			byte[] writebyte;
			int number_values;
			boolean syncing = true;
			int i;
			File fconn, path;				// public for sharing file when exiting
			BufferedOutputStream os = null;
			long currentmilli;
			Calendar cal = Calendar.getInstance();
			boolean set_timestamp = true, at_least_once_written = false;
			long currenttime = synctime, currentstart = 0;
			// use handler to start activity
	        Message start_msg = mHandler.obtainMessage(START_ACTIVITY);
	        Message finish_msg = mHandler.obtainMessage(FINISH_ACTIVITY);
	        Message finish2_msg = mHandler.obtainMessage(FINISH_NO_VALUES_ACTIVITY);

			read_data_entries = 0;
			try
			{
				while(syncing == true)
				{
			    	// use current milliseconds for filename
			    	currentmilli = System.currentTimeMillis();
			    	// open file in public directory
			    	path = new File(Environment.getExternalStorageDirectory(), "AIRS_temp");
			    	// make sure that path exists
			    	path.mkdirs();
			    	// open file and create, if necessary
		    		fconn = new File(path, String.valueOf(currentmilli) + ".txt");
	    			os = new BufferedOutputStream(new FileOutputStream(fconn, true));
			    	
					// build and add URI   
					uris.add(Uri.fromFile(fconn));	
					// save file for later
					files.add(fconn);

			    	// set timestamp when we will have found the first timestamp
			    	set_timestamp = true;
			    				    	
					while (fconn.length()<max_recording_size && syncing == true)
					{
						query = new String("SELECT Timestamp, Symbol, Value from 'airs_values' WHERE Timestamp > " + String.valueOf(currenttime) + " ORDER BY Timestamp ASC LIMIT " + String.valueOf(SYNC_BATCH));
						values = airs_storage.rawQuery(query, null);
						
						// garbage collect
						query = null;
						
						if (values == null)
						{
					        mHandler.sendMessage(finish_msg);
					        syncing = false;
					        break;
						}

						// get number of rows
						number_values = values.getCount();

						// if nothing is read
						if (number_values == 0)
						{
							// signal end of synchronization loop
							syncing = false;
							break;
						}
						
						// if less than asked for is read -> syncing finished!
						if (number_values < SYNC_BATCH)
							syncing = false;
						
						// get column index for timestamp and value
						t_column = values.getColumnIndex("Timestamp");
						s_column = values.getColumnIndex("Symbol");
						v_column = values.getColumnIndex("Value");
						
						if (t_column == -1 || v_column == -1 || s_column == -1)
						{
					        mHandler.sendMessage(finish_msg);
					        syncing = false;
					        break;
						}
							
						// move to first row to start
			    		values.moveToFirst();
			    		// read DB values into arrays
			    		for (i=0;i<number_values;i++)
			    		{
			    			// get timestamp
			    			currenttime = values.getLong(t_column);
	
			    			if (set_timestamp == true)
			    			{
						    	// set 
						    	cal.setTimeInMillis(currenttime);
					    		// store timestamp
					    		String time = new String(cal.getTime().toString() + "\n");
				    			os.write(time.getBytes(), 0, time.length());				    			
				    			// save for later
				    			currentstart = currenttime;
					    		// don't set timestamp anymore later
					    		set_timestamp = false;
					    		at_least_once_written = true;
			    			}
	
			    			// get symbol
			    			symbol = values.getString(s_column);
			    			// get value
			    			value = values.getString(v_column);
	
			    			// create line to write to file
			    			line_to_write = new String("#" + String.valueOf(currenttime-currentstart) + ";" + symbol + ";" + value + "\n");

			    			// now write to file
				    		writebyte = line_to_write.getBytes();

			    			os.write(writebyte, 0, writebyte.length);
			    			os.flush();
			    			
			    			// garbage collect the output data
			    			writebyte = null;
			    			line_to_write = null;
			    			
			    			// now move to next row
			    			values.moveToNext();
			    		}	
			    		
			    		// increase counter
			    		read_data_entries += number_values;
				        Bundle bundle = new Bundle();
				        bundle.putLong("Value", read_data_entries);
				        Message update_msg = mHandler.obtainMessage(UPDATE_VALUES);
				        update_msg.setData(bundle);
				        mHandler.sendMessage(update_msg);
					}
					
					// close output file
					os.close();					
				}
				
	    		if (at_least_once_written == true)
	    			// use handler to start activity
			        mHandler.sendMessage(start_msg);
	    		else
			        mHandler.sendMessage(finish2_msg);

			}
    		catch(Exception e)
    		{
    			try
    			{
    				if (os != null)
    					os.close();
    			}
    			catch(Exception ex)
    			{
    			}
		        mHandler.sendMessage(finish_msg);
    		}	
	     }
	 }
}

