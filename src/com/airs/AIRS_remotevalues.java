/*
Copyright (C) 2011, Dirk Trossen, airs@dirk-trossen.de

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

import com.airs.helper.SerialPortLogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity to view the current measurements of the ongoing remote recording
 *
 * @see         AIRS_remote
 */
public class AIRS_remotevalues extends Activity
{
	 private static final int REFRESH_VALUES = 1;
	 private TextView mTitle;
	 private TextView mTitle2;
	 private TextView mValue1, mValue2;
	 private AIRS_remote AIRS_remote = null;;
	 private Activity act;
	 private ValuesThread Values;

	 /** Called when the activity is first created. 
	     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
	     */
	 @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        // Set up the window layout
	        super.onCreate(savedInstanceState);
	        
	        act = this;
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
			setContentView(R.layout.remotesensing);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	 
	        // get window title fields
	        mTitle = (TextView) findViewById(R.id.title_left_text);
	        mTitle2 = (TextView) findViewById(R.id.title_right_text);
	        mTitle.setText(R.string.app_name);
	        mTitle2.setText("Remote Sensing");

	        // get value fields
	        mValue1 = (TextView) findViewById(R.id.remote_values1);
	        mValue2 = (TextView) findViewById(R.id.remote_values2);
	        
	        // bind to service
	        if (bindService(new Intent(this, AIRS_remote.class), mConnection, 0)==false)
	     		Toast.makeText(getApplicationContext(), "Remote Activity::Binding to service unsuccessful!", Toast.LENGTH_LONG).show();
	        
			 Values = new ValuesThread();
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

	 /** Called when the activity is stopped. 
	     */
	 @Override
	    public void onDestroy() 
	    {
	   		try
	   		{
		   		 // if ValueThread is running, stop it!
		   		 if (Values!=null)
		   			Values.thread.interrupt(); 
	   		}
	   		catch(Exception e)
	   		{
	   		}
	   		
	   		// unbind from service
	   		if (AIRS_remote != null)
	 		   unbindService(mConnection);

	   		// remove handler messages from queue
   		 	mHandler.removeMessages(REFRESH_VALUES);

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

	 /** Called when the Options menu is opened
	     * @param menu Reference to the {@link android.view.Menu}
	     */
	 @Override
	    public boolean onPrepareOptionsMenu(Menu menu) 
	    {
	    	MenuInflater inflater;

	    	menu.clear();    		
	    	inflater = getMenuInflater();
	    	inflater.inflate(R.menu.options_remote_sensing, menu);    		
	    	return true;
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
	    
	 /** Called when an option menu item has been selected by the user
	     * @param item Reference to the {@link android.view.MenuItem} clicked on
	     */
	 @Override
	    public boolean onOptionsItemSelected(MenuItem item) 
	    {
	        switch (item.getItemId()) 
	        {
	        case R.id.main_about:
	        		Toast.makeText(getApplicationContext(), R.string.RemoteAbout, Toast.LENGTH_LONG).show();
	            return true;
	        case R.id.remote_exit:
	    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    		builder.setMessage(getString(R.string.Exit_AIRS))
	    		       .setTitle(getString(R.string.AIRS_Remote_Sensing))
	    		       .setCancelable(false)
	    		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
	    		       {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		       		   SerialPortLogger.debugForced("AIRS_remotevalues::Exiting");
	    		    		   if (AIRS_remote != null)
	    		    			 stopService(new Intent(act, AIRS_remote.class));
	    					   SerialPortLogger.debugForced("AIRS_remotevalues::...stopped service");

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
	    		AlertDialog alert = builder.create();
	    		alert.show();

		        return true;
	        }
	        return false;
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
	     		AIRS_remote = ((AIRS_remote.LocalBinder)service).getService();
	     		// if start has failed, stop service
	     		if (AIRS_remote.failed == true)
	     		{
	    			stopService(new Intent(act, AIRS_remote.class));
	     			finish();
	     		}
	     		// if not running, show text
	  	        if (AIRS_remote.running == false)
		     		Toast.makeText(getApplicationContext(), getString(R.string.Sensing_not_running), Toast.LENGTH_SHORT).show();
	  	    }

	  	    public void onServiceDisconnected(ComponentName className) {
	  	        // This is called when the connection with the service has been
	  	        // unexpectedly disconnected -- that is, its process crashed.
	  	        // Because it is running in our same process, we should never
	  	        // see this happen.
	     		AIRS_remote = null;
	  	    }
	  	};
	  	
		 // The Handler that gets information back from the other threads, updating the values for the UI
		 private final Handler mHandler = new Handler() 
	     {
	        @Override
	        public void handleMessage(Message msg) 
	        {        	
	            switch (msg.what) 
	            {
	            case REFRESH_VALUES:
	            	if (AIRS_remote!=null)
	            	{
						mValue1.setText(String.valueOf(AIRS_remote.values_sent));
						mValue2.setText(String.valueOf((float)AIRS_remote.bytes_sent/1000));
	            	}
		            break;  
	            default:  
	            	break;
	            }
	        }
	     };

		 // Vibrate watchdog
		 private class ValuesThread implements Runnable
		 {			 
			 public Thread thread;
			 
			 ValuesThread()
			 {
				// save thread for later to stop 
				(thread = new Thread(this)).start();			 
			 }
			 
			 public void run()
			 {
				 
				 while (true)
				 {		

					 if (AIRS_remote!=null)
					 {
						// send message to handler 
				        Message msg = mHandler.obtainMessage(REFRESH_VALUES);
				        mHandler.sendMessage(msg);
					 }

					 // sleep for 1s
					 try
					 {
						 Thread.sleep(1000);
					 }
					 catch(InterruptedException e)
					 {
						 return;
					 }
				 }
			 }
		 }
}
