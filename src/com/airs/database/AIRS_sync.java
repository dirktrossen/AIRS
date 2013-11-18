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
package com.airs.database;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.airs.R;
import com.airs.platform.HandlerUIManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Activity to sync the AIRS database
 *
 */
public class AIRS_sync extends Activity implements OnClickListener
{
	// states for handler 
	private static final int START_ACTIVITY	= 1;
	private static final int UPDATE_VALUES	= 2;
	private static final int FINISH_NO_VALUES_ACTIVITY	= 3;
	private static final int NO_STORAGE		= 4;

	private static final int NO_SYNC			= 0;
	private static final int SYNC_STARTED	= 1;
	private static final int SYNC_CANCELLED	= 2;
	private static final int SYNC_FINISHED	= 2;
	
	// current batch of recordings for sync
	private static final int SYNC_BATCH		= 5000;
        
    // preferences
    private SharedPreferences settings;
    private Editor editor;
    
    // other variables
	private TextView ProgressText;
	private TextView syncText;
	private ImageButton startSync;
	private Button cancelButton;
	private ProgressBar progressbar;
    private long synctime;
	private int read_data_entries = 0;
	private File external_storage;
    private Uri share_file;
    private File sync_file;
	private File fconn;				// public for sharing file when exiting
	private BufferedOutputStream os = null;
	private boolean at_least_once_written = false;
	private long currenttime, currentstart = 0;
    private WakeLock wl;
    private int syncing = NO_SYNC;
    private Context context;
    
    // database variables
    private AIRS_database database_helper;
    private SQLiteDatabase airs_storage;

    /** Called when the activity is first created. 
     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        // Set up the window layout
        super.onCreate(savedInstanceState);
        
        // save for later
        this.context = this.getApplicationContext();
        
        // get default preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);
    	editor = settings.edit();

        // now open database
        database_helper = new AIRS_database(this.getApplicationContext());
        airs_storage = database_helper.getReadableDatabase();

		setContentView(R.layout.sync_dialog);		

        // get progress text view
        ProgressText = (TextView) findViewById(R.id.sync_progresstext);
        ProgressText.setVisibility(View.INVISIBLE);

        // get cancel button
        cancelButton = (Button)findViewById(R.id.sync_cancel);
        cancelButton.setOnClickListener(this);
        cancelButton.setVisibility(View.INVISIBLE);

        // get start button
        startSync	= (ImageButton)findViewById(R.id.sync_start);
        startSync.setOnClickListener(this);

        // hide progress bar first
        progressbar	= (ProgressBar)findViewById(R.id.sync_progress);
        progressbar.setVisibility(View.INVISIBLE);

        // get sync timestamp text view
        syncText = (TextView) findViewById(R.id.sync_text);

        syncing = NO_SYNC;
        
        // create new wakelock
        PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
		 
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AIRS Sync Lock"); 
        wl.acquire();        
    }

    /** Called when the activity is resumed. 
     */
    @Override
    public synchronized void onResume() 
    {
        super.onPause();
        
        // get timestamp of last sync
        synctime = settings.getLong("SyncTimestamp", 0);

        // set sync text view
		Time timeStamp = new Time();
		timeStamp.set(synctime);
        syncText.setText(getString(R.string.Last_sync) + " " + timeStamp.format("%H:%M:%S on %d.%m.%Y"));
    }

    /** Called when the activity is paused. 
     */
    @Override
    public synchronized void onPause() 
    {
        super.onPause();
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
       super.onDestroy();     
      
       // release wake lock if held
	   if (wl != null)
	   	 if (wl.isHeld() == true)
	   		 wl.release();
    }
     
    /** Called when the configuration of the activity has changed.
     * @param newConfig new configuration after change 
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    }
    
    /** Called when the Options menu is opened
     * @param menu Reference to the {@link android.view.Menu}
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	MenuInflater inflater;
        menu.clear();    		
        inflater = getMenuInflater();
		inflater.inflate(R.menu.options_sync, menu);    			
        
        return true;
    }

    /** Called when an option menu item has been selected by the user
     * @param item Reference to the {@link android.view.MenuItem} clicked on
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    	
        switch (item.getItemId()) 
        {
        case R.id.sync_about:
    		HandlerUIManager.AboutDialog(getString(R.string.main_Sync), getString(R.string.SyncAbout));
    		return true;
        case R.id.sync_setdate:
        	// if regular uploads are selected, do not allow for changing sync date!
        	if (Integer.valueOf(settings.getString("UploadFrequency", "0")) != 0)
    	  		Toast.makeText(getApplicationContext(), getString(R.string.Regular_sync), Toast.LENGTH_LONG).show();
        	else
        	{
		        // now get calendar data
				Calendar cal = Calendar.getInstance(Locale.getDefault());
				cal.setTimeInMillis(synctime);
				int month = cal.get(Calendar.MONTH);
				int year = cal.get(Calendar.YEAR);
				int day = cal.get(Calendar.DAY_OF_MONTH);
	
				DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() 
	        	{
	            @Override
	            public void onDateSet(DatePicker datePicker, int year, int month, int day) 
	            {
	            	// now form synctime from day/month/year selection
	            	Calendar cal = Calendar.getInstance(Locale.getDefault());
	            	cal.set(Calendar.YEAR, year);
	            	cal.set(Calendar.MONTH, month);
	            	cal.set(Calendar.DAY_OF_MONTH, day);
	            	cal.set(Calendar.HOUR, 0);
	            	cal.set(Calendar.MINUTE, 0);
	            	cal.set(Calendar.SECOND, 0);
	            	cal.set(Calendar.MILLISECOND, 1);
	            	cal.set(Calendar.AM_PM, Calendar.AM);
	            	synctime = cal.getTimeInMillis();
	            	
		            // set sync text view
		    		Time timeStamp = new Time();
		    		timeStamp.set(synctime);
		            syncText.setText(getString(R.string.Last_sync) + " " + timeStamp.format("%H:%M:%S on %d.%m.%Y"));
		            
		            // also place in preferences!
		       		editor.putLong("SyncTimestamp", synctime);
		            // finally commit to storing values!!
		            editor.commit();
		            
		            // set timer again
		            AIRS_upload.setTimer(context);
	
	            }
	        	}, year, month, day);
				dialog.setTitle(getString(R.string.Set_sync_date));
				dialog.setMessage(getString(R.string.Set_sync_date2));
				dialog.show();
        	}
	        return true;
        }
        
        return true;
    }
    
    /** Called when a button has been clicked on by the user
     * @param v Reference to the {@link android.view.View} of the button
     */
    public void onClick(View v) 
    {
    	// if regular uploads are selected, do not allow for manual sync
    	if (Integer.valueOf(settings.getString("UploadFrequency", "0")) != 0)
    	{
	  		Toast.makeText(getApplicationContext(), getString(R.string.Regular_sync), Toast.LENGTH_LONG).show();
	  		return;
    	}
    	
    	if (v.getId() == R.id.sync_start && syncing == NO_SYNC)
		{
            ProgressText.setText(getString(R.string.Start_synchronising));
            ProgressText.setVisibility(View.VISIBLE);
            progressbar.setVisibility(View.VISIBLE);
            cancelButton.setVisibility(View.VISIBLE);
	        // signal that syncing has started
	        syncing = SYNC_STARTED;
	        // start sync thread
	        new SyncThread();
		}
		
    	if (v.getId() == R.id.sync_cancel && syncing == SYNC_STARTED)
    	{
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage(getString(R.string.Interrupt_synchronising))
    			   .setTitle(getString(R.string.main_Sync))
    		       .setCancelable(false)
    		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		        	   syncing = SYNC_CANCELLED;
    		               ProgressText.setVisibility(View.INVISIBLE);
    		               progressbar.setVisibility(View.INVISIBLE);
    		               cancelButton.setVisibility(View.INVISIBLE);
    		               dialog.dismiss();
    		           }
    		       })
    		       .setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		                dialog.cancel();
    		           }
    		       });
    		AlertDialog alert = builder.create();
    		alert.show();
    	}    	
    }   
	
	// The Handler that gets information back from the other threads, updating the values for the UI
	private final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {
	       Intent chosen;

           switch (msg.what) 
           {
           case START_ACTIVITY:   
        	    if (syncing == SYNC_FINISHED)
        	    {
			     	// prepare intent for choosing sharing
			        Intent intent = new Intent(Intent.ACTION_SEND);
			        intent.setType("text/plain");
			        chosen = Intent.createChooser(intent,getString(R.string.Send_local_recordings));
			        
			        // anything chosen?
			        if (chosen!=null)
			        {
					    intent.putExtra(Intent.EXTRA_STREAM, share_file);
						startActivity(chosen);
						
			    		// write current timestamp for later syncs
						// put sync timestamp into store
			        	synctime = System.currentTimeMillis();
			       		editor.putLong("SyncTimestamp", synctime);
			            
			            // finally commit to storing values!!
			            editor.commit();
			                
			            // set timer again
			            AIRS_upload.setTimer(context);

			            // remove temp files
			            sync_file.delete();
			    		
			            // now finish activity
			            ProgressText.setVisibility(View.INVISIBLE);
			            progressbar.setVisibility(View.INVISIBLE);
			            cancelButton.setVisibility(View.INVISIBLE);
			            
			            // set sync text view
			    		Time timeStamp = new Time();
			    		timeStamp.set(synctime);
			            syncText.setText(getString(R.string.Last_sync) + " " + timeStamp.format("%H:%M:%S on %d.%m.%Y"));
			            
			            // reset sync state
			            syncing = NO_SYNC;
			        }
			        else
			        {
			            // now finish activity
			            ProgressText.setVisibility(View.INVISIBLE);
			            progressbar.setVisibility(View.INVISIBLE);
			            cancelButton.setVisibility(View.INVISIBLE);
			            
			            // reset sync state
			            syncing = NO_SYNC;
			        }
        	    }
		        break;
           case FINISH_NO_VALUES_ACTIVITY:
   	  			Toast.makeText(getApplicationContext(), getString(R.string.No_values_to_synchronise), Toast.LENGTH_LONG).show();
	            // now finish activity
	            ProgressText.setVisibility(View.INVISIBLE);
	            progressbar.setVisibility(View.INVISIBLE);
	            cancelButton.setVisibility(View.INVISIBLE);
	            
	            // reset sync state
	            syncing = NO_SYNC;
   	  			break;
           case UPDATE_VALUES:
	    		ProgressText.setText(getString(R.string.Temp_sync_file) + " " + String.valueOf(msg.getData().getLong("Value")/1000));
	    		break;
           case NO_STORAGE:
  	  			Toast.makeText(getApplicationContext(), getString(R.string.Cannot_find_storage), Toast.LENGTH_LONG).show();
	            // now finish activity
	            ProgressText.setVisibility(View.INVISIBLE);
	            progressbar.setVisibility(View.INVISIBLE);
	            cancelButton.setVisibility(View.INVISIBLE);
	            
	            // reset sync state
	            syncing = NO_SYNC;
  	  			break;
           default:  
           	break;
           }
       }
    };

	private class SyncThread implements Runnable
	{
		SyncThread()
		{
			new Thread(this).start();
		}
	     public void run()
	     {
	    	  	int i;
	    	 
		    	// path for templates
		        external_storage = getExternalFilesDir(null);
		        
		        if (external_storage == null)
		        {
			        syncing = SYNC_FINISHED;
			        mHandler.sendMessage(mHandler.obtainMessage(NO_STORAGE));
	   	  			return;
		        }
		        
		    	sync_file = new File(external_storage, "AIRS_temp");
	
				// get files in directory
				String [] file_list = sync_file.list(null);
				
				// remove files in AIRS_temp directory
				if (file_list != null)
					for (i=0;i<file_list.length;i++)
					{
						File remove = new File(sync_file, file_list[i]);
						remove.delete();
					}
				
		        SyncValues();
		        SyncNotes();
	     }
	     
	     private void SyncValues()
	     {
	    	String query;
			int t_column, s_column, v_column;
			Cursor values;
			String value, symbol;
			String line_to_write;
			byte[] writebyte;
			int number_values;
			int i;
			long currentmilli;
			Calendar cal = Calendar.getInstance();
			boolean set_timestamp = true;
			// use handler to start activity
	        Message finish2_msg = mHandler.obtainMessage(FINISH_NO_VALUES_ACTIVITY);
	
			read_data_entries = 0;
			try
			{
		    	// use current milliseconds for filename
		    	currentmilli = System.currentTimeMillis();
		    	// open file in public directory
		    	sync_file = new File(external_storage, "AIRS_temp");
		    	// make sure that path exists
		    	sync_file.mkdirs();
		    	// open file and create, if necessary
	    		fconn = new File(sync_file, String.valueOf(currentmilli) + ".txt");
    			os = new BufferedOutputStream(new FileOutputStream(fconn, true));
		    	
				// build URI for sharing
				share_file = Uri.fromFile(fconn);	

		    	// set timestamp when we will have found the first timestamp
		    	set_timestamp = true;
				currenttime = synctime;
		    	
				while (syncing == SYNC_STARTED)
				{
//						query = new String("SELECT Timestamp, Symbol, Value from 'airs_values' WHERE Timestamp > " + String.valueOf(currenttime) + " ORDER BY Timestamp ASC LIMIT " + String.valueOf(SYNC_BATCH));
					query = new String("SELECT Timestamp, Symbol, Value from 'airs_values' WHERE Timestamp > " + String.valueOf(currenttime) + " LIMIT " + String.valueOf(SYNC_BATCH));
					values = airs_storage.rawQuery(query, null);
					
					// garbage collect
					query = null;
					
					if (values == null)
					{
						// signal end of synchronization
				        syncing = SYNC_FINISHED;

				        // nothing to write?
			    		if (at_least_once_written == false)
			    		{
					        // purge file
			    			os.close();
					        mHandler.sendMessage(finish2_msg);
			    		}
			    		
			    		return;
					}

					// get number of rows
					number_values = values.getCount();

					// if nothing is read (anymore)
					if (number_values == 0)
					{
						// signal end of synchronization
						syncing = SYNC_FINISHED;
						
				        // nothing to write?
			    		if (at_least_once_written == false)
			    		{
					        // purge file
			    			os.close();
					        mHandler.sendMessage(finish2_msg);
			    		}
			    		
			    		return;
					}

					// get column index for timestamp and value
					t_column = values.getColumnIndex("Timestamp");
					s_column = values.getColumnIndex("Symbol");
					v_column = values.getColumnIndex("Value");
					
					if (t_column == -1 || v_column == -1 || s_column == -1)
					{
				        // signal end of synchronization
						syncing = SYNC_FINISHED;
						
				        // nothing to write?
			    		if (at_least_once_written == false)
			    		{
					        // purge file
			    			os.close();
					        mHandler.sendMessage(finish2_msg);
			    		}
			    		
			    		return;
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
//				    		String time = new String(cal.getTime().toString() + "\n");
					    	// force a date format to address Android 4.3 changes that changed zzz to 'BST' and similar
				    		DateFormat sdf = new SimpleDateFormat ("EEE MMM dd HH:mm:ss ZZZZ yyyy", Locale.getDefault());
				    		String time = new String(sdf.format(cal.getTime()) + "\n");
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

		    			// add empty string as space
		    			if (value.compareTo("") == 0)
		    				value = " ";
		    			
		    			// create line to write to file
		    			line_to_write = new String("#" + String.valueOf(currenttime-currentstart) + ";" + symbol + ";" + value + "\n");

		    			// now write to file
			    		writebyte = line_to_write.getBytes();

		    			os.write(writebyte, 0, writebyte.length);
		    			
		    			// garbage collect the output data
		    			writebyte = null;
		    			line_to_write = null;
		    			
		    			// now move to next row
		    			values.moveToNext();
		    		}	
		    		
		    		// increase counter
		    		read_data_entries = (int)fconn.length();
			        Bundle bundle = new Bundle();
			        bundle.putLong("Value", read_data_entries);
			        Message update_msg = mHandler.obtainMessage(UPDATE_VALUES);
			        update_msg.setData(bundle);
			        mHandler.sendMessage(update_msg);
			        
			        // close values to free up memory
			        values.close();
				}
				
				// close output file
				os.close();					
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
				// signal end of synchronization
    		}	
			
	        // nothing to write?
    		if (at_least_once_written == false)
		        mHandler.sendMessage(finish2_msg);
	     }
	     
	     private void SyncNotes()
	     {
	    	String query;
			int y_column, m_column, d_column, a_column, c_column, mo_column;
			Cursor values;
			String value, symbol;
			String line_to_write;
			byte[] writebyte;
			int number_values;
			int i;
			// use handler to start activity
	        Message start_msg = mHandler.obtainMessage(START_ACTIVITY);
	        Message finish2_msg = mHandler.obtainMessage(FINISH_NO_VALUES_ACTIVITY);

	    	// path for templates
	        File external_storage = getExternalFilesDir(null);
	        
	        if (external_storage == null)
	        {
		        syncing = SYNC_FINISHED;
		        mHandler.sendMessage(mHandler.obtainMessage(NO_STORAGE));
   	  			return;
	        }

			try
			{
				currenttime = synctime;
				// now sync the notes, if any
				syncing = SYNC_STARTED;
				while (syncing == SYNC_STARTED)
				{
					query = new String("SELECT Year, Month, Day, Annotation, created, modified from 'airs_annotations' WHERE created > " + String.valueOf(currenttime) + " LIMIT " + String.valueOf(SYNC_BATCH));
					values = airs_storage.rawQuery(query, null);
					
					// garbage collect
					query = null;
					
					if (values == null)
					{
				        // purge file
				        os.close();

						// signal end of synchronization
						syncing = SYNC_FINISHED;

						if (at_least_once_written == true)
			    			// use handler to start activity
					        mHandler.sendMessage(start_msg);
			    		else
					        mHandler.sendMessage(finish2_msg);
			    		
			    		return;
					}

					// get number of rows
					number_values = values.getCount();

					// if nothing is read (anymore)
					if (number_values == 0)
					{
				        // purge file
		    			os.close();

						// signal end of synchronization
						syncing = SYNC_FINISHED;
						
			    		if (at_least_once_written == true)
			    			// use handler to start activity
					        mHandler.sendMessage(start_msg);
			    		else
					        mHandler.sendMessage(finish2_msg);
			    		
			    		return;
					}

					// get column index for timestamp and value
					y_column = values.getColumnIndex("Year");
					m_column = values.getColumnIndex("Month");
					d_column = values.getColumnIndex("Day");
					a_column = values.getColumnIndex("Annotation");
					c_column = values.getColumnIndex("created");
					mo_column = values.getColumnIndex("modified");
					
					if (y_column == -1 || m_column == -1 || d_column == -1 || a_column == -1 || c_column == -1 || mo_column == -1)
					{
				        // purge file
		    			os.close();

						// signal end of synchronization
						syncing = SYNC_FINISHED;
						
			    		if (at_least_once_written == true)
			    			// use handler to start activity
					        mHandler.sendMessage(start_msg);
			    		else
					        mHandler.sendMessage(finish2_msg);
			    		
			    		return;
					}
						
			        Log.e("AIRS", "...reading next batch!");

					// move to first row to start
		    		values.moveToFirst();
		    		// read DB values into arrays
		    		for (i=0;i<number_values;i++)
		    		{
		    			// get timestamp
		    			currenttime = values.getLong(c_column);

		    			// set symbol
		    			symbol = "UN";
		    			// create value as concatenation of year:month:day:modified:annotation
		    			value = String.valueOf(values.getInt(y_column)) + ":" + String.valueOf(values.getInt(m_column)) + ":" + String.valueOf(values.getInt(d_column)) + ":" + String.valueOf(values.getLong(mo_column)) + ":" + values.getString(a_column);

		    			// create line to write to file
		    			line_to_write = new String("#" + String.valueOf(currenttime-currentstart) + ";" + symbol + ";" + value + "\n");

		    			// now write to file
			    		writebyte = line_to_write.getBytes();

		    			os.write(writebyte, 0, writebyte.length);
		    			
		    			// garbage collect the output data
		    			writebyte = null;
		    			line_to_write = null;
		    			
		    			// now move to next row
		    			values.moveToNext();
		    		}	
		    				        
			        // close values to free up memory
			        values.close();
				}
				
				// close output file
				os.close();					
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
				// signal end of synchronization
    		}	
			
    		if (at_least_once_written == true)
    			// use handler to start activity
		        mHandler.sendMessage(start_msg);
    		else
		        mHandler.sendMessage(finish2_msg);

	     }
	 }
}

