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

import com.airs.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import android.content.*;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;

/**
 * Class to index the AIRS database
 *
 * @see         AIRS_database
 */
public class AIRS_index extends Activity
{
	// states for handler 
	private static final int FINISH_ACTIVITY			= 1;
	private static final int FINISH2_ACTIVITY		= 2;

	// database variables
    private AIRS_database database_helper;
    private  SQLiteDatabase airs_storage;

    // preferences
    private Context airs;
  
    // other variables
	private TextView mTitle;
	private TextView ProgressText;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        // Set up the window layout
        super.onCreate(savedInstanceState);
        
        // get context for thread
        airs = getApplicationContext();
        
		// set custom title
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.backuprestore_dialog);		
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
 
        // get window title fields
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        mTitle.setText("Index Recordings");

        // get progress text view
        ProgressText = (TextView) findViewById(R.id.backuprestore_progresstext);
        ProgressText.setText("Starting indexing");

        new IndexThread();
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
        	   Toast.makeText(getApplicationContext(), getString(R.string.Cannot_index), Toast.LENGTH_LONG).show();
        	   finish();
        	   break;
           case FINISH2_ACTIVITY:
        	   Toast.makeText(getApplicationContext(), getString(R.string.Finished_indexing), Toast.LENGTH_LONG).show();
        	   finish();
        	   break;
           default:  
           	break;
           }
       }
    };

	private class IndexThread implements Runnable
	{
		IndexThread()
		{
			new Thread(this).start();
		}
		
		/** Thread for indexing.
	     */
		public void run()
	     {
	    	 Message finish_msg = mHandler.obtainMessage(FINISH_ACTIVITY);
	    	 Message finish2_msg = mHandler.obtainMessage(FINISH2_ACTIVITY);

	    	 try 
	    	 {
    	        // get database
    		    try 
    		    {	            
    	            // get database
    	            database_helper = new AIRS_database(airs);
    	            do
    	            {
    	            	airs_storage = database_helper.getWritableDatabase();
    	            	if (airs_storage == null)
    	            	{
    	            		try
    	            		{
    	            			Thread.sleep(500);
    	            		}
    	            		catch(Exception e)
    	            		{
    	            		}
    	            	}
    	            }while(airs_storage == null);
    		    } 
    		    catch(Exception e)
    		    {
    		        mHandler.sendMessage(finish_msg);
    		    }	    

     	    	String command = "CREATE INDEX 'time_index' ON 'airs_values' ('timestamp' ASC)";	        	
    	    	airs_storage.execSQL(command);
    	    	
		        mHandler.sendMessage(finish2_msg);
	    	 } 
	    	 catch (Exception e) 
     	     {
			        mHandler.sendMessage(finish_msg);
	    	 }
	     }
	 }
}


