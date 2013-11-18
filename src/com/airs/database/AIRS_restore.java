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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import com.airs.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import android.content.*;
import android.content.res.Configuration;

/**
 * Activity to restore the AIRS database
 *
 * @see AIRS_backup
 * @see AIRS_DBAdmin
 */
public class AIRS_restore extends Activity
{
	// states for handler 
	private static final int FINISH_ACTIVITY			= 1;
	private static final int FINISH2_ACTIVITY		= 2;
	private static final int UPDATE_VALUES			= 3;
        
    // preferences
    private SharedPreferences settings;
  
    // other variables
	private TextView mTitle;
	private TextView ProgressText;

	/** Called when the activity is first created. 
     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
     */
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
	  		Toast.makeText(getApplicationContext(), getString(R.string.Cannot_restore), Toast.LENGTH_LONG).show();
	  		finish();
		}

		// set custom title
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.backuprestore_dialog);		
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
 
        // get window title fields
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        mTitle.setText(getString(R.string.Backup_recordings));

        // get progress text view
        ProgressText = (TextView) findViewById(R.id.backuprestore_progresstext);
        ProgressText.setText(getString(R.string.Starting_restore));

        new RestoreThread();
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
	
	// The Handler that gets information back from the other threads, updating the values for the UI
	private final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {
           switch (msg.what) 
           {
           case FINISH_ACTIVITY:
        	   Toast.makeText(getApplicationContext(), getString(R.string.Cannot_restore2), Toast.LENGTH_LONG).show();
        	   finish();
        	   break;
           case FINISH2_ACTIVITY:
        	   Toast.makeText(getApplicationContext(), getString(R.string.Finished_restore), Toast.LENGTH_LONG).show();
        	   finish();
        	   break;
           case UPDATE_VALUES:
	    		ProgressText.setText(getString(R.string.Restore_status) + " " + String.valueOf(msg.getData().getLong("Value")/1024) + " kB");
	    		break;
           default:  
           	break;
           }
       }
    };

	private class RestoreThread implements Runnable
	{
		RestoreThread()
		{
			new Thread(this).start();
		}
		
	     public void run()
	     {
	    	 Message finish_msg = mHandler.obtainMessage(FINISH_ACTIVITY);
	    	 Message finish2_msg = mHandler.obtainMessage(FINISH2_ACTIVITY);

	    	 try 
	    	 {
	    	        File sd = new File(getExternalFilesDir(null).getAbsolutePath());

	    	        if (sd.canWrite()) 
	    	        {
	    	            String backupDBPath = "AIRS_backup.db";
	    	            File currentDB = getDatabasePath(AIRS_database.DATABASE_NAME);
	    	            File backupDB = new File(sd, backupDBPath);

	    	    		// get backup size information
	    		        Bundle bundle = new Bundle();
	    		        bundle.putLong("Value", backupDB.length());
	    		        Message update_msg = mHandler.obtainMessage(UPDATE_VALUES);
	    		        update_msg.setData(bundle);
	    		        mHandler.sendMessage(update_msg);

	    	            if (backupDB.exists()) 
	    	            {
	    	            	FileOutputStream output = new FileOutputStream(currentDB);
	    	            	FileInputStream input = new FileInputStream(backupDB);
	    	                FileChannel dst = output.getChannel();
	    	                FileChannel src = input.getChannel();
	    	                dst.transferFrom(src, 0, src.size());
	    	                src.close();
	    	                dst.close();
	    	                input.close();
	    	                output.close();
	    	            }
	    	            else
    				        mHandler.sendMessage(finish_msg);
	    	            	
	    	            
				        mHandler.sendMessage(finish2_msg);
	    	        }
	    	        else
				        mHandler.sendMessage(finish_msg);
	    	 } 
	    	 catch (Exception e) 
     	     {
			        mHandler.sendMessage(finish_msg);
	    	 }
	     }
	 }
}

