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
package com.airs;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

public class AIRS_mood_selector extends Activity implements OnClickListener
{
	 private TextView mTitle;
	 private TextView mTitle2;
	 private Editor editor;

	 // preferences
	 private SharedPreferences settings;
	 private String mood = null;
	 private boolean selected = false;
	 
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
		    
	        // get emoticons buttons and set onclick listener
	        ((ImageButton)findViewById(R.id.mood_happy)).setOnClickListener(this); 
	        ((ImageButton)findViewById(R.id.mood_content)).setOnClickListener(this); 
	        ((ImageButton)findViewById(R.id.mood_surprised)).setOnClickListener(this); 
	        ((ImageButton)findViewById(R.id.mood_sad)).setOnClickListener(this); 
	        ((ImageButton)findViewById(R.id.mood_angry)).setOnClickListener(this); 
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

	    public void onClick(View v) 
	    {
	    	// dispatch depending on button pressed
	    	switch(v.getId())
	    	{
	    	case R.id.mood_happy:
	    		mood = new String("Happy");
	    		selected = true;
	    		finish();
        		break;
	    	case R.id.mood_content:
	    		mood = new String("Content");
	    		selected = true;
	    		finish();
        		break;
	    	case R.id.mood_surprised:
	    		mood = new String("Surprised");
	    		selected = true;
	    		finish();
        		break;
	    	case R.id.mood_sad:
	    		mood = new String("Sad");
	    		selected = true;
	    		finish();
        		break;
	    	case R.id.mood_angry:
	    		mood = new String("Angry");
	    		selected = true;
	    		finish();
        		break;
	     	default:
	    		break;
	    	}
	    }}
