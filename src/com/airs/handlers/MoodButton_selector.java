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
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.airs.*;

public class MoodButton_selector extends Activity implements OnItemClickListener, OnClickListener
{
	 private TextView mTitle;
	 private TextView mTitle2;
	 private Editor editor;

	 // preferences
	 private SharedPreferences settings;
	 private String mood = null, mood_icon = null;
	 private boolean selected = false;
	 private boolean own_defined = false;
	 
	 // list of mood icons
	 private ListView mood_icons;
	 private ImageView mood_iconown;
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
				own_defined = settings.getBoolean("MoodHandler::Mood_own", false);
			}
			catch(Exception e)
			{
			}

			// set window title
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			setContentView(R.layout.mood_selection);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	 
	        // get window title fields
	        mTitle = (TextView) findViewById(R.id.title_left_text);
	        mTitle2 = (TextView) findViewById(R.id.title_right_text);
	        mTitle.setText(getString(R.string.AIRS_Mood_Selector));
	        mTitle2.setText(getString(R.string.AIRS_mood_selection2) + " " + mood);
		    
	        // initialize own defined event click listener
    		Button bt = (Button) findViewById(R.id.mooddefined);
    		bt.setOnClickListener(this);
    		bt = (Button) findViewById(R.id.mooddelete);
    		bt.setOnClickListener(this);
    		
       		// was the stored string own defined?
			if (own_defined == true)
			{
				EditText et = (EditText) findViewById(R.id.moodown);
				et.setText(mood);
				own_defined = false;
			}

	        // initialize list of mood icons
	        mood_icons 	= (ListView)findViewById(R.id.moodiconList);
	        
	        // Set up the ListView for mood icons selection
	        mMoodArrayList 	  = new ArrayList<HandlerEntry>();
	        mood_icons.setAdapter(new MyCustomBaseAdapter(this, mMoodArrayList));
	        mood_icons.setOnItemClickListener(this);
		    
	        // add mood icons to list
	        addMoodIcon(getString(R.string.Very_Happy), R.drawable.mood_very_happy);
	        addMoodIcon(getString(R.string.Happy), R.drawable.mood_happy);
	        addMoodIcon(getString(R.string.Feeling_Good), R.drawable.mood_feeling_good);	       
	        addMoodIcon(getString(R.string.Positively_Excited), R.drawable.mood_feeling_good);	       
	        addMoodIcon(getString(R.string.Confused), R.drawable.mood_confused);
	        addMoodIcon(getString(R.string.Anxious), R.drawable.mood_doubtful);
	        addMoodIcon(getString(R.string.Doubtful), R.drawable.mood_doubtful);
	        addMoodIcon(getString(R.string.Not_Sure), R.drawable.mood_not_sure);
	        addMoodIcon(getString(R.string.Upset), R.drawable.mood_upset);
	        addMoodIcon(getString(R.string.Not_Happy), R.drawable.mood_not_happy);
	        addMoodIcon(getString(R.string.Annoyed), R.drawable.mood_annoyed);
	        addMoodIcon(getString(R.string.Shocked), R.drawable.mood_shocked);
	        addMoodIcon(getString(R.string.Tired), R.drawable.mood_tired);
	        addMoodIcon(getString(R.string.Sad), R.drawable.mood_sad);
	        addMoodIcon(getString(R.string.Very_Sad), R.drawable.mood_very_sad);
	        
	        // now hook the button for own icon selection
	        mood_iconown 	= (ImageView)findViewById(R.id.moodown_icon);
	        mood_iconown.setOnClickListener(this);
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
		            
		            // put flag on own defined mood
		            if (own_defined == false)
			            editor.putBoolean("MoodHandler::Mood_own", false);
		            else
			            editor.putBoolean("MoodHandler::Mood_own", true);

		            // finally commit to storing values!!
		            editor.commit();
				}
				catch(Exception e)
				{
				}
	
				// send broadcast intent to signal end of selection to mood button handler
				Intent intent = new Intent("com.airs.moodselected");
				if (own_defined == true)
				{
					if (mood_icon != null)
						intent.putExtra("Mood", mood + "::" + mood_icon);
					else
						intent.putExtra("Mood", mood);
				}
				else
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
    	    super.onActivityResult(requestCode, resultCode, data); 

	    	// pick up selection
	    	if (resultCode == RESULT_OK)
	    	{
	    		mood_icon = data.getStringExtra("mood");
	    		int resid = data.getIntExtra("resid", R.drawable.mood_own);
	    		mood_iconown.setImageResource(resid);
	    	}
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
	    
	    public void onClick(View v) 
		{
	    	EditText et;
	    	// dispatch depending on button pressed
	    	switch(v.getId())
	    	{
	    	case R.id.mooddefined:
	    		et = (EditText) findViewById(R.id.moodown);
		    	// read input string from edit field
	    		mood = et.getText().toString().replaceAll("'","''");
	    		mood = et.getText().toString().replaceAll(":","");
	    		mood = et.getText().toString().replaceAll("::","");
	    		selected = true;
	    		own_defined = true;
	    		finish();
	    		break;
	    	case R.id.mooddelete:
	    		et = (EditText) findViewById(R.id.moodown);
	    		et.setText("");
	    		break;
	    	case R.id.moodown_icon:
	    		Intent intent = new Intent(getApplicationContext(), MoodButton_iconselector.class);
		    	startActivityForResult(intent, 101);	        	
		    	break;
	    	}
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
