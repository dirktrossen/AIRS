package com.airs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import com.airs.helper.SerialPortLogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class AIRS_templates extends Activity implements OnClickListener 
{
	private AIRS_templates context;
	private MyCustomBaseAdapter customAdapter;
	private List<String> annotations;
    private ListView annotation_list;
    private ImageButton save_button, load_button, delete_button, shortcut_button;
	private EditText text;
	private int selected_text;
    
    private SharedPreferences settings;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
            // Set up the window layout
            super.onCreate(savedInstanceState);

            // save context for later
            context = this;
            
    	    // set call log view
            setContentView(R.layout.manage_templates);
            
            // set up list view
            annotation_list = (ListView)findViewById(R.id.templates_list);
            annotation_list.setItemsCanFocus(true); 
            annotation_list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            annotation_list.setSelected(true);
            annotation_list.setEnabled(true);
	        annotations = new ArrayList<String>();

	        // hook buttons
            save_button 	= (ImageButton)findViewById(R.id.templates_save);
            save_button.setOnClickListener(this);
            load_button	= (ImageButton)findViewById(R.id.templates_load);
            load_button.setOnClickListener(this);
            delete_button = (ImageButton)findViewById(R.id.templates_delete);
            delete_button.setOnClickListener(this);
            shortcut_button = (ImageButton)findViewById(R.id.templates_shortcut);
            shortcut_button.setOnClickListener(this);	        
	        
	        // get call log entries
            gatherFiles();            
    }
 
    @Override
    public void onResume() 
    {
        super.onResume();
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
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    }
        
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) 
    {
 		// key de-pressed?
		if (event.getAction() == KeyEvent.ACTION_UP)
			// is it the BACK key?
			if (event.getKeyCode()==KeyEvent.KEYCODE_BACK)
                finish();

        return super.dispatchKeyEvent(event);
    }
    
    public void onClick(View v) 
    {        
    	AlertDialog.Builder builder;
    	AlertDialog alert;
    	LayoutInflater inflater;
    	View dialog_text;
	    String dirPath;
        File shortcutFile;
        
    	switch(v.getId())
    	{
    	case R.id.templates_load:
    		if (annotations.size() == 0)
              	Toast.makeText(getApplicationContext(), "You need at least one template being saved before restoring!", Toast.LENGTH_LONG).show();          
    		else
    		{
	    		// IMPORTANT: any change in restoring the settings in AIRS_shortcut needs to be copied over here, too!!!
	    		// build dialog box
	    		builder = new AlertDialog.Builder(this);
	    		builder.setIcon(android.R.drawable.ic_menu_recent_history)
	    		       .setTitle("Load Template")
	    		       .setMessage("Loading the template will override the current settings!\nAre You sure?")
	    		       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
		    		       {
		    		           public void onClick(DialogInterface dialog, int id) 
		    		           {
		    		                dialog.cancel();
		    		           }
		    		       })
	    		       .setPositiveButton("OK", new DialogInterface.OnClickListener() 
		    		       {
		    		           public void onClick(DialogInterface dialog, int id) 
		    		           {
			    		           	long synctime;
			    		    	    int version, i; 
			    		    	    boolean tables, tables2;
			    		    	    String dirPath;
			    		            File shortcutFile;
	
			    		       		// get current template file
			    		       		dirPath = getExternalFilesDir(null).getAbsolutePath() + "/" + "templates";
	  	    		                shortcutFile = new File(dirPath, annotations.get(selected_text));
		
			    		   	        // get default preferences
			    		   	        settings = PreferenceManager.getDefaultSharedPreferences(context);
			    		   	        
			    		   	        // get values that should not be overwritten!
			    		   	        synctime = settings.getLong("SyncTimestamp", 0);
			    		   	        version = settings.getInt("Version", 0);	
			    		   	        tables = settings.getBoolean("AIRS_local::TablesExists", false);	
			    		   	        tables2 = settings.getBoolean("AIRS_local::Tables2Exists", false);	
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
			    		   	        settings = PreferenceManager.getDefaultSharedPreferences(context);
			    		   			Editor editor = settings.edit();
			    		   			
			    		   			// write certain back in order for them to not be overwritten!
			    		   			editor.putLong("SyncTimestamp", synctime);
			    		   			editor.putInt("Version", version);
			    		   			editor.putBoolean("AIRS_local::TablesExists", tables);
			    		   			editor.putBoolean("AIRS_local::Tables2Exists", tables2);
			    		   			
			    		   			// put back all entries related to event annotations
			    		   			for (i=0;i<own_events;i++)
			    		   				editor.putString("EventButtonHandler::Event"+Integer.toString(i), event[i]);
			    		   			editor.putString("EventButtonHandler::EventSelected", event_selected_entry);
		
			    		   			editor.commit();
			    		   			
			    		   			// notify user
		    		              	Toast.makeText(getApplicationContext(), "Restored settings in template '" + annotations.get(selected_text) + "'", Toast.LENGTH_LONG).show();          
	
		    		                dialog.dismiss();
		    		           }
		    		       });
	    		alert = builder.create();
	    		alert.show();  	
    		}
    		break;
    	case R.id.templates_save:
	        // inflate edittext
    		inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		
    		dialog_text = inflater.inflate(R.layout.add_template_dialog, null);
            
    		text = (EditText)dialog_text.findViewById(R.id.add_template_dialogbox);
	        text.setMovementMethod(new ScrollingMovementMethod());
	        
    		// clear text field
    		text.setText("");
    		// build dialog box
    		builder = new AlertDialog.Builder(this);
    		builder.setIcon(android.R.drawable.ic_menu_save)
    		       .setTitle("Filename to Save Settings as Template")
    		       .setView(dialog_text)
    		       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
	    		       {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		                dialog.cancel();
	    		           }
	    		       })
    		       .setPositiveButton("OK", new DialogInterface.OnClickListener() 
	    		       {
	    		           public void onClick(DialogInterface dialog, int id) 
	    		           {
	    		            	File preferenceFile = new File(getFilesDir(), "../shared_prefs/com.airs_preferences.xml");
	    			        	// if file does not exist use a path that is usually used by GalaxyS in 2.3!!!
	    		            	if (preferenceFile.exists() == false)
	    		            		preferenceFile = new File("/dbdata/databases/com.airs/shared_prefs/com.airs_preferences.xml");
	    		            	
	    		            	String dirPath = getExternalFilesDir(null).getAbsolutePath() + "/" + "templates";
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
	    		    	            }
	    		    	            catch(Exception e)
	    		    	            {
	    	    		              	SerialPortLogger.debug("AIRS_templates: Exception in saving template!");          
	    		    	            }        
	    		            	}    
	    		            	else
    	    		              	SerialPortLogger.debug("AIRS_templates: Preference file does not exist: " + preferenceFile.getAbsolutePath());  

	    		            	// gather list of files again
	    		            	gatherFiles();

	    		                dialog.dismiss();
	    		           }
	    		       });
    		alert = builder.create();
    		alert.show();  		
    		break;
    	case R.id.templates_delete:
    		if (annotations.size() != 0)
    		{
	    		// build dialog box
	    		builder = new AlertDialog.Builder(this);
	    		builder.setIcon(android.R.drawable.ic_menu_delete)
	    		       .setTitle("Delete Template")
	    		       .setMessage(annotations.get(selected_text))
	    		       .setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
		    		       {
		    		           public void onClick(DialogInterface dialog, int id) 
		    		           {
		    		                dialog.cancel();
		    		           }
		    		       })
	    		       .setPositiveButton("OK", new DialogInterface.OnClickListener() 
		    		       {
		    		           public void onClick(DialogInterface dialog, int id) 
		    		           {
	
		    		        	   String dirPath = getExternalFilesDir(null).getAbsolutePath() + "/" + "templates";
		    		        	   File shortcutFile = new File(dirPath, annotations.get(selected_text));
		    		        	   shortcutFile.delete();
	
		    		   		       // gather list of files again
		    		        	   gatherFiles();
		    		        	   dialog.dismiss();
		    		           }
		    		       });
	    		alert = builder.create();
	    		alert.show(); 
    		}
    		break;
    	case R.id.templates_shortcut:
    		if (annotations.size() == 0)
              	Toast.makeText(getApplicationContext(), "You need at least one template being saved before you can create a shortcut!", Toast.LENGTH_LONG).show();          
    		else
    		{
	    		// get current template file
	        	dirPath = getExternalFilesDir(null).getAbsolutePath() + "/" + "templates";
	            shortcutFile = new File(dirPath, annotations.get(selected_text));
	
	            // intent for starting AIRS
	    	    Intent intent;
	        	Intent shortcutIntent = new Intent(Intent.ACTION_MAIN); 
	        	shortcutIntent.setClassName(context, AIRS_shortcut.class.getName()); 
	        	
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
	        	
	           	Toast.makeText(getApplicationContext(), "Created AIRS shortcut on launcher homescreen!", Toast.LENGTH_LONG).show();          
    		}
    	default:
    	   	selected_text = customAdapter.set(v);
    	   	break;
    	}
    }
        
    private void gatherFiles()
    {
		int i;

		// clear annotation list and timestamps
		annotations.clear();
		// get adapter and set it
        customAdapter = new MyCustomBaseAdapter(this, annotations);
        annotation_list.setAdapter(customAdapter);
		
    	// path for templates
		File shortcutPath = new File(getExternalFilesDir(null).getAbsolutePath() + "/templates/");

		// get files in directory
		String [] file_list = shortcutPath.list(null);
		
		// add to list to show
		if (file_list != null)
			for (i=0;i<file_list.length;i++)
				annotations.add(file_list[i]);
    }
    
  	// Custom adapter for two line text list view with imageview (icon), defined in handlerentry.xml
  	private class MyCustomBaseAdapter extends BaseAdapter 
  	{
  		 private List<String> ArrayList;
  		 private List<ViewHolder> viewHolder;
  		 private LayoutInflater mInflater;

  		 public MyCustomBaseAdapter(Context context, List<String> results) 
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
  		 
  		 public View getView(int position, View convertView, ViewGroup parent) 
  		 {
  			 ViewHolder holder;
  			 if (convertView == null) 
  			 {
  				 convertView = mInflater.inflate(R.layout.manage_template_entry, null);
  				 holder = new ViewHolder();
  				 holder.entry = (TextView) convertView.findViewById(R.id.manage_template_entry_string);
  				 holder.entry.setOnClickListener(context);
  				 holder.checked = (RadioButton) convertView.findViewById(R.id.manage_template_entry_check);
  				 holder.checked.setOnClickListener(context);

  				 convertView.setTag(holder);
  				 viewHolder.add(holder);
  			 } 
  			 else 
  			 {
  				 holder = (ViewHolder) convertView.getTag();
  			 }
  		  
  			 // put text in list
  			 holder.entry.setText(ArrayList.get(position));

  			 // enable when first one
  			 if (position == 0)
  				 holder.checked.setChecked(true);
  			 
  			 return convertView;
  		 }

  		 class ViewHolder 
  		 {
  		  TextView entry;
  		  RadioButton checked;
  		 }
  	}
}
