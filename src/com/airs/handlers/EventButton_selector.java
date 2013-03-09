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
*/
package com.airs.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.airs.*;

public class EventButton_selector extends Activity implements OnItemClickListener, OnClickListener, OnItemLongClickListener
{
	 private final static int MAX_STRINGS 	= 50;

	 private TextView mTitle;
	 private TextView mTitle2;
	 private Editor editor;

	 // preferences
	 private SharedPreferences settings;
	 private String[] event = null;
	 private boolean selected = false;
	 private String selected_entry;
	 private int own_events = 5;
	 private ArrayList<String> event_list = new ArrayList<String>();
	 
	 // list of mood icons
	 private ListView mood_icons;
	 private ArrayList<HandlerEntry> mMoodArrayList;
	 private MyCustomBaseAdapter myCustomAdapter;

	   @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
		    int i;
		    String last_selected;
		    
	        // Set up the window layout
	        super.onCreate(savedInstanceState);
	        
	        // read preferences
	        settings = PreferenceManager.getDefaultSharedPreferences(this);
	        editor = settings.edit();
	        
	        // read last selected mood value and maximum descriptions
			try
			{				
				// read maximum number of descriptions
				own_events = Integer.parseInt(settings.getString("EventButtonHandler::MaxEventDescriptions", "5"));
				if (own_events<1)
					own_events = 5;
				if (own_events>MAX_STRINGS)
					own_events = MAX_STRINGS;
			}
			catch(Exception e)
			{
				own_events = 5;
			}

			// create appropriate number of strings
			event = new String[own_events];
			
			// read possibly stored descriptions
			// shall we sort them?
			if (settings.getBoolean("EventButtonHandler::SortAnnotations", false) == true)
			{
				for (i=0;i<own_events;i++)
					event_list.add(settings.getString("EventButtonHandler::Event"+Integer.toString(i), ""));
			    Collections.sort(event_list, new Comparator<String>() {
			        @Override
			        public int compare(String s1, String s2) {
			            return s1.compareToIgnoreCase(s2);
			        }
			    });
				for (i=0;i<own_events;i++)
	        		event[i] = event_list.get(i);
			}
			else
				for (i=0;i<own_events;i++)
					event[i]	= settings.getString("EventButtonHandler::Event"+Integer.toString(i), "");

			// set window title
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
//	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			setContentView(R.layout.mood_selection);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	        
	        // get window title fields
	        mTitle = (TextView) findViewById(R.id.title_left_text);
	        mTitle2 = (TextView) findViewById(R.id.title_right_text);
	        mTitle.setText("AIRS Annotations");
	        // show last selected event string
	        last_selected = settings.getString("EventButtonHandler::EventSelected", "-");
        	mTitle2.setText("Last: " + last_selected);
		    
	        // initialize own defined event
    		Button bt = (Button) findViewById(R.id.mooddefined);
    		bt.setOnClickListener(this);
    		bt = (Button) findViewById(R.id.mooddelete);
    		bt.setOnClickListener(this);
    		
    		// was there at least one stored string?
			if (event[0].compareTo("") != 0)
			{
				EditText et = (EditText) findViewById(R.id.moodown);
				et.setText(event[0]);
			}

	        // initialize list of mood icons
	        mood_icons 	= (ListView)findViewById(R.id.moodiconList);
	        
	        // Set up the ListView for mood icons selection
	        mMoodArrayList 	  = new ArrayList<HandlerEntry>();
	        myCustomAdapter = new MyCustomBaseAdapter(this, mMoodArrayList);
	        mood_icons.setAdapter(myCustomAdapter);
	        mood_icons.setOnItemClickListener(this);
	        mood_icons.setOnItemLongClickListener(this);
		    
	        // add mood icons to list
	        for (i=0;i<own_events;i++)
	        	if (event[i].compareTo("") != 0)
	        		addEventIcon(event[i], R.drawable.event_marker);
	    }

	    @Override
	    public synchronized void onRestart() 
	    {
	        super.onRestart();
	    }

	    @Override
	    public void onStop() 
	    {
	        super.onStop();
	    }

	    @Override
	    public void onDestroy() 
	    {
	    	int i;
	    	
	    	if (selected == true)
	    	{
				try
				{
					// put mood value into store if it has some content
					for (i=0;i<own_events;i++)
						if (event[i].compareTo("") != 0)
							editor.putString("EventButtonHandler::Event"+Integer.toString(i), event[i]);
						else
							editor.putString("EventButtonHandler::Event"+Integer.toString(i), "");
		            
					// also store selected one
					editor.putString("EventButtonHandler::EventSelected", selected_entry);
		            // finally commit to storing values!!
		            editor.commit();
				}
				catch(Exception e)
				{
				}
	
				// send broadcast intent to signal end of selection to mood button handler
				Intent intent = new Intent("com.airs.eventselected");
				intent.putExtra("Event", selected_entry);
				
				sendBroadcast(intent);
				
				// clear flag
				selected = false;
	    	}
			
			// now destroy activity
	        super.onDestroy();
	    }

	    public void onActivityResult(int requestCode, int resultCode, Intent data) 
	    {
	    	return;
	    }
	    
		   @Override
		    public boolean onPrepareOptionsMenu(Menu menu) 
		    {
		    	MenuInflater inflater;

		    	menu.clear();    		
		    	inflater = getMenuInflater();
		    	inflater.inflate(R.menu.options_about, menu);    		
		    	return true;
		    }

		    @Override
		    public boolean onOptionsItemSelected(MenuItem item) 
		    {
		        switch (item.getItemId()) 
		        {
		        case R.id.main_about:
		        		Toast.makeText(getApplicationContext(), R.string.EventAbout, Toast.LENGTH_LONG).show();
		            return true;
		        }
		        return false;
		    }

	    public void onClick(View v) 
		{
	    	int i, size;
	    	boolean added = false;
	    	
	    	EditText et;
	    	// dispatch depending on button pressed
	    	switch (v.getId())
	    	{
	    	case R.id.mooddefined:
	    		et = (EditText) findViewById(R.id.moodown);
	    		
	    		// is there a free field?
	    		for (i=0;i<own_events;i++)
	    			if (event[i].compareTo("") == 0)
	    			{
	    				added = true;
	    				// add to list
	    				event[i] = et.getText().toString();
	    				// add also to visible list
		        		addEventIcon(event[i], R.drawable.event_marker);
		   	        	myCustomAdapter.notifyDataSetChanged();
			    		selected_entry = event[i];
	    				break;
	    			}
	    		
	    		if (added == false)
	    		{
				    // read input string from edit field
		    		event[0] = et.getText().toString();
	    			// select this entry
		    		selected_entry = event[0];

		    		// create new entry for position 0
			    	HandlerEntry entry = new HandlerEntry();			    	
			    	entry.name = event[0];
			    	entry.resid = R.drawable.event_marker;
		    		mMoodArrayList.set((int)0, entry);
	   	        	myCustomAdapter.notifyDataSetChanged();
	   	        	// now read all entries again
	   	        	
	   	        	size = mMoodArrayList.size();
	   	        	
	   	        	// now re-build name array
	   		        for (i=1;i<own_events;i++)
	   		        	if (i<size)
	   		        		event[i] = mMoodArrayList.get(i).name;
	   		        	else
	   		        		event[i] ="";

	    		}

	    		// indicated that we selected the first entry
	    		selected = true;
	    		finish();
	    		break;
	    	case R.id.mooddelete:
	    		et = (EditText) findViewById(R.id.moodown);
	    		et.setText("");
	    		break;
	    	}
		}


	    public boolean onItemLongClick(AdapterView<?> av, View v, int arg2, long arg3)
	    {
	    	final int selected = (int)arg3;
	    	
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage(getString(R.string.Delete_event))
    			   .setTitle(getString(R.string.AIRS_Events))
    		       .setCancelable(false)
    		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		        	    int i, size;
    		   	        	mMoodArrayList.remove((int)selected);
    		   	        	myCustomAdapter.notifyDataSetChanged();
    		   	        	size = mMoodArrayList.size();
    		   	        	
    		   	        	// now re-build name array
    		   		        for (i=0;i<own_events;i++)
    		   		        	if (i<size)
    		   		        		event[i] = mMoodArrayList.get(i).name;
    		   		        	else
    		   		        		event[i] ="";
    		   		        
   		                	dialog.cancel();
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

	    public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
	    {
	    	// get list view entry
	    	// HandlerEntry entry = (HandlerEntry)av.getItemAtPosition((int)arg3);
	    	
	    	// read entries name for the selected mood
    		// indicated that we selected the first entry
	    	selected = true;
    		selected_entry = mMoodArrayList.get((int)arg3).name;
	    	finish();
	    }
	    
	    private void addEventIcon(String name, int resId)
	    {
	    	HandlerEntry entry = new HandlerEntry();
	    	
	    	entry.name = name;
	    	entry.resid = resId;
	    	
	        mMoodArrayList.add(entry);
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
	  				 convertView = mInflater.inflate(R.layout.moodentry, null);
	  				 holder = new ViewHolder();
	  				 holder.name = (TextView) convertView.findViewById(R.id.moodname);
	  				 holder.image = (ImageView)convertView.findViewById(R.id.moodimage);

	  				 convertView.setTag(holder);
	  			 } 
	  			 else 
	  			 {
	  				 holder = (ViewHolder) convertView.getTag();
	  			 }
	  		  
	  			 holder.name.setText(ArrayList.get(position).name);
	  			 holder.image.setImageResource(ArrayList.get(position).resid);

	  		  return convertView;
	  		 }

	  		 class ViewHolder 
	  		 {
	  		  TextView name;
	  		  ImageView image;
	  		 }
	  	}
}

