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

import java.util.ArrayList;
import java.util.Calendar;

import com.airs.handlerUIs.HandlerUI;
import com.airs.helper.SerialPortLogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class AIRS_settings_tab extends Activity implements OnItemClickListener
{
	// Layout Views
    private ListView handlers;
    private ArrayList<HandlerEntry> mHandlerArrayList;
    
    // preferences
    private SharedPreferences settings;
    private AIRS_general_settings generalsettings;
  
    // other variables
    private Context		airs;
    public  static HandlerUI	current_handler;
	
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

        Bundle extras = getIntent().getExtras();
        if (extras != null) 
        {
            String prefs_file = extras.getString("Preference");
           	Toast.makeText(getApplicationContext(), "Received intent extra =" + prefs_file, Toast.LENGTH_LONG).show();          
        }
        
        // get default preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);
		
        // save activity in debug class
        SerialPortLogger.nors = this;
		// is debugging on?
   		SerialPortLogger.setDebugging(settings.getBoolean("Debug", false));
		SerialPortLogger.debug("AIRS debug output at " + Calendar.getInstance().getTime().toString());
		
		// initialize HandlerUI Manager
		HandlerUIManager.createHandlerUIs(this);
        
        setContentView(R.layout.handlers);
        handlers 	= (ListView)findViewById(R.id.handlerList);
        // Find and set up the ListView for handler UIs
        mHandlerArrayList 	  = new ArrayList<HandlerEntry>();
        handlers.setAdapter(new MyCustomBaseAdapter(this, mHandlerArrayList));
	    handlers.setOnItemClickListener(this);
	    	    
	    // add general settings
	    generalsettings = new AIRS_general_settings();
	    mHandlerArrayList.add(generalsettings.init());
	    
        // initialize handler UIs
	    int i;
	    for(i=0;i<HandlerUIManager.max_handlers;i++)
	    	if (HandlerUIManager.handlers[i]!=null)
	    		mHandlerArrayList.add(HandlerUIManager.handlers[i].init()); 
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
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
    	super.onConfigurationChanged(newConfig);
    }
 
    public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) 
    {
	    int i, j = 0;
	    
	    if (arg3 == 0)
	    {
            Intent settingsActivity = new Intent(getBaseContext(), Prefs.class);
            settingsActivity.putExtra("Resource", R.xml.generalsettings);
            settingsActivity.putExtra("About", getString(R.string.GeneralSettings));
            settingsActivity.putExtra("AboutTitle", "General Settings");            
            startActivity(settingsActivity);	    	
	    }
	    else
	    {
		    // try to find the handlerUI entry
		    for(i=0;i<HandlerUIManager.max_handlers;i++)
		    	if (HandlerUIManager.handlers[i]!=null)
		    	{
		    		if (j+1 == (int)arg3)
		    		{
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
    }

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

