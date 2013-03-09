package com.airs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
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

public class AIRS_sensorselection extends Activity
{
    // handler for starting local AIRS
	public static final int DISCOVER_LOCALLY = 2;
	
	private Activity airs;

    private AIRS_local 	AIRS_locally;
    private ProgressDialog progressdialog;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        // Set up the window layout
        super.onCreate(savedInstanceState);
        
        // save for later
        airs = this;
        			    
	    // start service and connect to it -> then discover the sensors
        getApplicationContext().startService(new Intent(this, AIRS_local.class));
        getApplicationContext().bindService(new Intent(this, AIRS_local.class), mConnection, Service.BIND_AUTO_CREATE);   
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
       
	   // unbind from service
       if (AIRS_locally!=null)
       {
    	   AIRS_locally.saveSelections();
		   getApplicationContext().unbindService(mConnection);
       }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	MenuInflater inflater;
        menu.clear();    		
        inflater = getMenuInflater();
       	inflater.inflate(R.menu.options_local, menu);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    	
   	    switch (item.getItemId()) 
        {
        case R.id.local_about:
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(getString(R.string.Sensor_Selection))
    			   .setMessage(getString(R.string.LocalAbout))
    			   .setIcon(R.drawable.about)
    		       .setNeutralButton(getString(R.string.OK), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		                dialog.cancel();
    		           }
    		       });
    		AlertDialog alert = builder.create();
    		alert.show();
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
        return true;
    }    
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
    	super.onConfigurationChanged(newConfig);
    }

	 // The Handler that gets information back from the other threads, starting the various services from the main thread
	public final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {
           switch (msg.what) 
           {
           case DISCOVER_LOCALLY:
       		   AIRS_locally.Discover(airs);
       		   progressdialog.cancel();
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
			if (AIRS_locally != null)
			{
		    	progressdialog = ProgressDialog.show(airs, getString(R.string.Gathering_sensors), getString(R.string.Please_wait), true);
		        // send message into handler queue to discover locally!
		        mHandler.sendMessage(mHandler.obtainMessage(DISCOVER_LOCALLY));	 
			}
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
