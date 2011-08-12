/*
Copyright (C) 2008-2011, Dirk Trossen, airs@dirk-trossen.de

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

import android.app.Activity;
import android.content.Context;
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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.airs.*;

public class MoodButton_selector extends Activity implements OnItemClickListener
{
	 private TextView mTitle;
	 private TextView mTitle2;
	 private Editor editor;

	 // preferences
	 private SharedPreferences settings;
	 private String mood = null;
	 private boolean selected = false;
	 
	 // list of mood icons
	 private ListView mood_icons;
	 private ArrayList<HandlerEntry> mMoodArrayList;

	   @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        // Set up the window layout
	        super.onCreate(savedInstanceState);
	        
	        // read preferences
	        settings = PreferenceManager.getDefaultSharedPreferences(this);
	        editor = settings.edit();
	        
	        // read last selected mood value
			try
			{
				mood	= settings.getString("MoodHandler::Mood", "Happy");
			}
			catch(Exception e)
			{
			}

			// set window title
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
			setContentView(R.layout.mood_selection);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	 
	        // get window title fields
	        mTitle = (TextView) findViewById(R.id.title_left_text);
	        mTitle2 = (TextView) findViewById(R.id.title_right_text);
	        mTitle.setText(R.string.app_name);
	        mTitle2.setText("Last Selected: " + mood);
		    
	        // initialize list of mood icons
	        setContentView(R.layout.handlers);
	        mood_icons 	= (ListView)findViewById(R.id.handlerList);
	        
	        // Set up the ListView for mood icons selection
	        mMoodArrayList 	  = new ArrayList<HandlerEntry>();
	        mood_icons.setAdapter(new MyCustomBaseAdapter(this, mMoodArrayList));
	        mood_icons.setOnItemClickListener(this);
		    
	        // add mood icons to list
	        addMoodIcon("Very Happy", R.drawable.mood_very_happy);
	        addMoodIcon("Happy", R.drawable.mood_happy);
	        addMoodIcon("Feeling Good", R.drawable.mood_feeling_good);	       
	        addMoodIcon("Positively Excited", R.drawable.mood_feeling_good);	       
	        addMoodIcon("Confused", R.drawable.mood_confused);
	        addMoodIcon("Anxious", R.drawable.mood_doubtful);
	        addMoodIcon("Doubtful", R.drawable.mood_doubtful);
	        addMoodIcon("Not Sure", R.drawable.mood_not_sure);
	        addMoodIcon("Upset", R.drawable.mood_upset);
	        addMoodIcon("Not Happy", R.drawable.mood_not_happy);
	        addMoodIcon("Annoyed", R.drawable.mood_annoyed);
	        addMoodIcon("Shocked", R.drawable.mood_shocked);
	        addMoodIcon("Tired", R.drawable.mood_tired);
	        addMoodIcon("Sad", R.drawable.mood_sad);
	        addMoodIcon("Very Sad", R.drawable.mood_very_sad);
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
	    	if (selected == true)
	    	{
				try
				{
					// put mood value into store
		            editor.putString("MoodHandler::Mood", mood);
		            
		            // finally commit to storing values!!
		            editor.commit();
				}
				catch(Exception e)
				{
				}
	
				// send broadcast intent to signal end of selection to mood button handler
				Intent intent = new Intent("com.airs.moodselected");
				intent.putExtra("Mood", mood);
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
		        		Toast.makeText(getApplicationContext(), R.string.MoodAbout, Toast.LENGTH_LONG).show();
		            return true;
		        }
		        return false;
		    }


	    public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
	    {
	    	// get list view entry
	    	HandlerEntry entry = (HandlerEntry)av.getItemAtPosition((int)arg3);
	    	
	    	// read entries name for the selected mood
	    	mood = new String(entry.name);
	    	selected = true;
	    	finish();
	    }
	    
	    private void addMoodIcon(String name, int resId)
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
