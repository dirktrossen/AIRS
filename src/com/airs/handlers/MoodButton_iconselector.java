/*
Copyright (C) 2013, TecVis LP, support@tecvis.co.uk

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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import com.airs.*;

/** Activity to select the mood icon being chosen for own mood definitions
 * @see android.app.Activity
 */
public class MoodButton_iconselector extends Activity implements OnItemClickListener
{
	 private TextView mTitle;

	 // preferences
	 private String mood = null;
	 private int mood_id;
	 
	 // list of mood icons
	 private GridView mood_icons;
	 private ArrayList<MoodEntry> mMoodArrayList;

	 /**
	  * Started when creating the {@link android.app.Activity}
	  * @see android.app.Activity#onCreate(android.os.Bundle)
	  */
	 @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
	        // Set up the window layout
	        super.onCreate(savedInstanceState);
	        
			// set window title
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			setContentView(R.layout.mood_iconselection);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	 
	        // get window title fields
	        mTitle = (TextView) findViewById(R.id.title_left_text);
	        mTitle.setText(getString(R.string.AIRS_Mood_Selector));	    
    		
	        // initialize list of mood icons
	        mood_icons 	= (GridView)findViewById(R.id.mood_icongrid);
	        
	        // Set up the ListView for mood icons selection
	        mMoodArrayList 	  = new ArrayList<MoodEntry>();
	        mood_icons.setAdapter(new MyCustomBaseAdapter(this, mMoodArrayList));
	        mood_icons.setOnItemClickListener(this);
		    
	        // add mood icons to list
	        addMoodIcon(getString(R.string.Very_Happy), R.drawable.mood_very_happy);
	        addMoodIcon(getString(R.string.Happy), R.drawable.mood_happy);
	        addMoodIcon(getString(R.string.Giggly), R.drawable.mood_giggly);
	        addMoodIcon(getString(R.string.Feeling_Good), R.drawable.mood_feeling_good);	       
	        addMoodIcon(getString(R.string.Positively_Excited), R.drawable.mood_feeling_good);	       
	        addMoodIcon(getString(R.string.Confused), R.drawable.mood_confused);
	        addMoodIcon(getString(R.string.Anxious), R.drawable.mood_doubtful);
	        addMoodIcon(getString(R.string.Doubtful), R.drawable.mood_doubtful);
	        addMoodIcon(getString(R.string.Not_Sure), R.drawable.mood_not_sure);
	        addMoodIcon(getString(R.string.Upset), R.drawable.mood_upset);
	        addMoodIcon(getString(R.string.Not_Happy), R.drawable.mood_not_happy);
	        addMoodIcon(getString(R.string.Angry), R.drawable.mood_angry);
	        addMoodIcon(getString(R.string.Annoyed), R.drawable.mood_annoyed);
	        addMoodIcon(getString(R.string.Ashamed), R.drawable.mood_ashamed);
	        addMoodIcon(getString(R.string.Embarrassed), R.drawable.mood_embarassed);
	        addMoodIcon(getString(R.string.Surprised), R.drawable.mood_surprised);
	        addMoodIcon(getString(R.string.Shocked), R.drawable.mood_shocked);
	        addMoodIcon(getString(R.string.Worried), R.drawable.mood_worried);
	        addMoodIcon(getString(R.string.Scared), R.drawable.mood_scared);
	        addMoodIcon(getString(R.string.Bored), R.drawable.mood_bored);
	        addMoodIcon(getString(R.string.Tired), R.drawable.mood_tired);
	        addMoodIcon(getString(R.string.Sleepy), R.drawable.mood_sleepy);
	        addMoodIcon(getString(R.string.Staring), R.drawable.mood_staring);
	        addMoodIcon(getString(R.string.Sick), R.drawable.mood_sick);
	        addMoodIcon(getString(R.string.Sad), R.drawable.mood_sad);
	        addMoodIcon(getString(R.string.Very_Sad), R.drawable.mood_very_sad);
	        addMoodIcon(getString(R.string.Disappointed), R.drawable.mood_dissappointed);	    
	    }

	 /** Called when restarting the {@link android.app.Activty}
	    * @see android.app.Activity#onRestart()
	    */
	 @Override
	    public synchronized void onRestart() 
	    {
	        super.onRestart();
	    }

	 /** Called when stopping the {@link android.app.Activty}
	    * @see android.app.Activity#onStop()
	    */
	 @Override
	    public void onStop() 
	    {
	        super.onStop();
	    }

	 /** Called when destroying the {@link android.app.Activty}
	    * @see android.app.Activity#onDestroy()
	    */
	 @Override
	    public void onDestroy() 
	    {			
			// now destroy activity
	        super.onDestroy();
	    }
    
	 /**
     * Called if a list item has been clicked on, here any of the mood annotations
     * @param av Reference to the parent view
     * @param v Reference to the {@link android.view.View} being clicked on
     * @param arg2 don't care
     * @param arg3 index of the list items being clicked on
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
	 public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3)
	    {
	    	// get list view entry
	    	MoodEntry entry = (MoodEntry)av.getItemAtPosition((int)arg3);
	    	
	    	// read entries name for the selected mood
	    	mood = new String(entry.name);
	    	mood_id = entry.resid;
	    	
        	Intent returnIntent = new Intent();
        	returnIntent.putExtra("mood", mood);
        	returnIntent.putExtra("resid", mood_id);
    		setResult(RESULT_OK,returnIntent);

	    	finish();
	    }
	    
	    private void addMoodIcon(String name, int resId)
	    {
	    	MoodEntry entry = new MoodEntry();
	    	
	    	entry.name = name;
	    	entry.resid = resId;
	    	
	        mMoodArrayList.add(entry);
	    }
	    
 		 class MoodEntry 
  		 {
 			 String name;
 			 int resid;
  		 }

	  	// Custom adapter for two line text list view with imageview (icon), defined in handlerentry.xml
	  	private class MyCustomBaseAdapter extends BaseAdapter 
	  	{
	  		 private ArrayList<MoodEntry> ArrayList;
	  		 private LayoutInflater mInflater;

	  		 public MyCustomBaseAdapter(Context context, ArrayList<MoodEntry> results) 
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
	  				 convertView = mInflater.inflate(R.layout.moodiconentry, null);
	  				 holder = new ViewHolder();
	  				 holder.image = (ImageView)convertView.findViewById(R.id.moodimageselection);

	  				 convertView.setTag(holder);
	  			 } 
	  			 else 
	  			 {
	  				 holder = (ViewHolder) convertView.getTag();
	  			 }
	  		  
	  			 holder.image.setImageResource(ArrayList.get(position).resid);

	  		     return convertView;
	  		 }

	  		 class ViewHolder 
	  		 {
	  		  ImageView image;
	  		 }
	  	}
}

