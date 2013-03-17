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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
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
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
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
    public  static HandlerUI	current_handler;
	private EditText text;

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
	    	    
	    // add select sensors here
		HandlerEntry entry = new HandlerEntry();
		
		entry.name = new String(getString(R.string.Select_Sensors));
		entry.description = getString(R.string.Select_Sensors2);
		entry.resid = R.drawable.select;
	    mHandlerArrayList.add(entry);

	    // add general settings
	    generalsettings = new AIRS_general_settings();
	    mHandlerArrayList.add(generalsettings.init(this));
	    
        // initialize handler UIs
	    int i;
	    for(i=0;i<HandlerUIManager.max_handlers;i++)
	    	if (HandlerUIManager.handlers[i]!=null)
	    		mHandlerArrayList.add(HandlerUIManager.handlers[i].init(this)); 
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
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	MenuInflater inflater;
        menu.clear();    		
        inflater = getMenuInflater();
		inflater.inflate(R.menu.options_config, menu);    			
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    	
    	AlertDialog.Builder builder;
    	AlertDialog alert;
    	LayoutInflater inflater;
    	View dialog_text;
    	
        switch (item.getItemId()) 
        {
        case R.id.main_about:
    		HandlerUIManager.AboutDialog("Settings", getString(R.string.HandlersList));
    		break;
        case R.id.templates_save:
	        // inflate edittext
    		inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		
    		dialog_text = inflater.inflate(R.layout.add_template_dialog, null);
            
    		text = (EditText)dialog_text.findViewById(R.id.add_template_dialogbox);
	        text.setMovementMethod(new ScrollingMovementMethod());
	        
    		// set text field to current template
    		text.setText(AIRS_record_tab.current_template);
    		
    		// build dialog box
    		builder = new AlertDialog.Builder(this);
    		builder.setIcon(android.R.drawable.ic_menu_save)
    		       .setTitle(getString(R.string.Save_template))
    		       .setView(dialog_text)
    		       .setNegativeButton(getString(R.string.Cancel), new DialogInterface.OnClickListener() 
	    		       {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		                dialog.cancel();
	    		           }
	    		       })
    		       .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() 
	    		       {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		            	File preferenceFile = new File(getFilesDir(), "../shared_prefs/com.airs_preferences.xml");
	    			        	// if file does not exist use a path that is usually used by GalaxyS in 2.3!!!
	    		            	if (preferenceFile.exists() == false)
	    		            		preferenceFile = new File("/dbdata/databases/com.airs/shared_prefs/com.airs_preferences.xml");
	    		            	
	    		            	File external_storage = getExternalFilesDir(null);
	    		            	if (external_storage != null)
	    		            	{
		    		            	String dirPath = external_storage.getAbsolutePath() + "/" + "templates";
		    		            	File shortcutPath = new File(dirPath);
		    		            	if (!shortcutPath.exists())
		    		            		shortcutPath.mkdirs();
		    		            	
		    		                File shortcutFile = new File(shortcutPath, text.getText().toString());
		    		            	
		    		            	// copy preference file if original preferences exist
		    		            	if (preferenceFile.exists() == true)
		    		            	{
		    		    	            try
		    		    	            {
		    		    	                FileChannel src = new FileInputStream(preferenceFile).getChannel();
		    		    	                FileChannel dst = new FileOutputStream(shortcutFile).getChannel();
		    		    	                dst.transferFrom(src, 0, src.size());
		    		    	                src.close();
		    		    	                dst.close();
		    		    	                
					    		   			// notify user
				    		              	Toast.makeText(getApplicationContext(), getString(R.string.Saved_settings) + " '" + text.getText() + "'", Toast.LENGTH_LONG).show();          
		    		    	            }
		    		    	            catch(Exception e)
		    		    	            {
		    	    		              	SerialPortLogger.debug("AIRS_templates: Exception in saving template!");          
		    		    	            }        
		    		            	}    
		    		            	else
	    	    		              	SerialPortLogger.debug("AIRS_templates: Preference file does not exist: " + preferenceFile.getAbsolutePath());  
	    		            	}
	    		            	
	    		                dialog.dismiss();
	    		           }
	    		       });
    		alert = builder.create();
    		alert.show();  		
    		break;
        }
        return false;
    }
 
    public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) 
    {
	    int i, j = 0;
	    Intent settingsActivity;
	    
	    switch((int)arg3)
	    {
	    case 0:
            settingsActivity = new Intent(this, AIRS_sensorselection.class);
            startActivity(settingsActivity);	    	
	    	break;
	    case 1:
            settingsActivity = new Intent(this, Prefs.class);
            settingsActivity.putExtra("Resource", R.xml.generalsettings);
            settingsActivity.putExtra("About", getString(R.string.GeneralSettings));
            settingsActivity.putExtra("AboutTitle", "General Settings");            
            startActivity(settingsActivity);	    	
            break;
        default:
		    // try to find the handlerUI entry
		    for(i=0;i<HandlerUIManager.max_handlers;i++)
		    	if (HandlerUIManager.handlers[i]!=null)
		    	{
		    		if (j+2 == (int)arg3)
		    		{
		    			current_handler = HandlerUIManager.handlers[i];
		    			// start preferences activity with intent being set to resource ID for preferences
		                settingsActivity = new Intent(this, Prefs.class);
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

