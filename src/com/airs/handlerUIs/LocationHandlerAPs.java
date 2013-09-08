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
package com.airs.handlerUIs;

import java.util.ArrayList;
import java.util.List;

import com.airs.R;
import com.airs.database.AIRS_database;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity to show a hitlist of most visited APs, called from the {@link LocationHandlerUI} preference
 */
public class LocationHandlerAPs extends Activity implements OnClickListener, OnItemSelectedListener, OnItemLongClickListener
{
	private static final long FULL_DAY 		= 1000*60*60*24;	// milliseconds per day
	private static final long FULL_WEEK 	= FULL_DAY * 7;		// milliseconds per week
	private static final long FULL_MONTH 	= FULL_DAY * 31;	// milliseconds per month

	// Layout Views
    private Button ok_button;
    private Spinner duration_spinner;
    private ListView hitlist;
    private ProgressBar progress;
    // adapter for the AP list
    private ArrayAdapter<String> mAPsArrayAdapter;
    // list of visited APs and their counters
    private List<Integer> WiFiCounter;
    private List<String> WiFiNames;
    private StringBuffer storedWifis;
	private String[] adaptiveWifis;
	private AsyncTask<String, Long, Long> task;
    private long duration = FULL_WEEK;
    private long startedTime;
    // database stuff
    private Cursor values = null;
    private AIRS_database database_helper;
    private SQLiteDatabase airs_storage;
    // preferences
    private SharedPreferences settings;

    
    /** Called when the activity is first created. 
     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
     */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {    	
        // Set up the window layout
        super.onCreate(savedInstanceState);
        
        // current time in milliseconds
        startedTime = System.currentTimeMillis();
        
        try
        {
	        // get database
	        database_helper = new AIRS_database(getApplicationContext());
	        airs_storage = database_helper.getWritableDatabase();
	
			// store pointer to preferences
	        settings = PreferenceManager.getDefaultSharedPreferences(this);

			// retrieve WiFis marked for adaptive GPS
			storedWifis = new StringBuffer(settings.getString("LocationHandler::AdaptiveGPS_WiFis", ""));
			adaptiveWifis = storedWifis.toString().split("::");
	 
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	        // set content of View
	        setContentView(R.layout.adaptivehitlist);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

	        // get window title fields
	        TextView mTitle = (TextView) findViewById(R.id.title_left_text);
	        mTitle.setText(R.string.AdaptiveGPS);

	        // get buttons and set onclick listener
	        ok_button 		 = (Button)findViewById(R.id.hitlist_OK);
	        ok_button.setOnClickListener(this);
	        duration_spinner = (Spinner)findViewById(R.id.hitlist_duration);
	        duration_spinner.setOnItemSelectedListener(this);
	        progress		 = (ProgressBar)findViewById(R.id.hitlist_progress);
	        
	        // populate list of APs now
	        hitlist 	= (ListView)findViewById(R.id.hitlist_list);
	        hitlist.setOnItemLongClickListener(this);
	        hitlist.setItemsCanFocus(false); 
	        hitlist.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	        mAPsArrayAdapter = new ArrayAdapter<String>(this.getApplicationContext(), android.R.layout.simple_list_item_multiple_choice);
	        // Find and set up the ListView for paired devices
	        hitlist.setAdapter(mAPsArrayAdapter);
	        
	        // reserve memory for the counting lists
	        WiFiCounter = new ArrayList<Integer>();
	        WiFiNames   = new ArrayList<String>();			
        }
        catch(Exception e)
        {
        	finish();
        }
    }
    
    /** Called when the activity is paused. 
     */
    @Override
    public synchronized void onPause() 
    {
        super.onPause();
    }

    /** Called when the activity is stopped. 
     */
    @Override
    public void onStop() 
    {
        super.onStop();
    }

    /** Called when the activity is destroyed. 
     */
    @Override
    public void onDestroy() 
    {
       super.onDestroy();
       
    }
    
    /** Called when the configuration of the activity has changed.
     * @param newConfig new configuration after change 
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
    	super.onConfigurationChanged(newConfig);
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
       	inflater.inflate(R.menu.options_about, menu);
        
        return true;
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
        	// call about dialogue
    		Toast.makeText(getApplicationContext(), R.string.AdaptiveGPSAbout, Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    /** Called when a button has been clicked on by the user
     * @param v Reference to the {@link android.view.View} of the button
     */
    @Override
    public void onClick(View v) 
    {    	
        switch (v.getId()) 
        {
        case R.id.hitlist_OK:
        	// write the selection to the persistent storage
        	writeNewAPs();
        	// ...then finish the activity
        	finish();
        	break;
        }
    } 
    
    /**
     * Called when nothing is selected in spinner
     * @param parent Reference to the parent {@link android.view.View}
     */
    public void onNothingSelected(AdapterView<?> parent)
    {	
    }

    /**
     * Called when item in spinner is clicked
     * @param parent Reference to the parent {@link android.view.View}
     * @param view Reference to the current {@link android.view.View}
     * @param pos position of the spinner
     * @param id not needed
     * @return true if click is consumed
     */
    public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id)
    {
 		Toast.makeText(getApplicationContext(), "Number of visits at this WiFi AP: " + String.valueOf(WiFiCounter.get(pos)), Toast.LENGTH_LONG).show();

    	return true;
    }
    
    /**
     * Called when item in spinner is clicked
     * @param parent Reference to the parent {@link android.view.View}
     * @param view Reference to the current {@link android.view.View}
     * @param pos position of the spinner
     * @param id not needed
     */
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
    {
		// change duration
		switch(pos)
		{
		case 0:
			duration = FULL_WEEK;
	    	// now gather hit list (again)
	        task = new GatherTask();
	        task.execute("");			
			break;
		case 1:
			duration = FULL_MONTH;
	    	// now gather hit list (again)
	        task = new GatherTask();
	        task.execute("");
			break;
		}		
    } 
    
	private class GatherTask extends AsyncTask<String, Long, Long> 
	{
	     protected Long doInBackground(String... params) 
	     {
				int i, j, number_values, reading_index, current_counter;
				int v_column;
				String query;
				String reading;

				// issue query to the database
				query = new String("SELECT Value from 'airs_values' WHERE Timestamp BETWEEN " + String.valueOf(startedTime - duration) + " AND " + String.valueOf(startedTime) + " AND Symbol='WI'");

				values = airs_storage.rawQuery(query, null);
				    	
				if (values == null)
			        return Long.valueOf(-1);
				
				// get column index for timestamp and value
				v_column = values.getColumnIndex("Value");
				
				if (v_column == -1)
			        return Long.valueOf(-1);
				
				number_values = values.getCount();
										
				// are there any values?
		    	if (number_values != 0)
		    	{
		    		// move to first row to start
		    		values.moveToFirst();
		    		
		    		// go through all values
		    		for (i=0;i<number_values;i++)
		    		{
		    			// get current reading
		    			reading = values.getString(v_column);
		    			
		    			// split the value into the different SSIDs
						String[] devices = reading.split("\n");

						// go through all SSIDs of that reading
		    			for(j=0;j<devices.length;j++)
		    			{
		    				// try to find the reading in the list of already counted APs
		    				reading_index = WiFiNames.indexOf(devices[j]);
		    				
		    				// nothing found?
		    				if (reading_index == -1)
		    				{
		    					// skip the " " reading which means no WiFi AP around!!
		    					if (devices[j].compareTo("") != 0)
		    					{
			    					// add SSID to our list of read APs and add a counter with 1 visit
			    					WiFiNames.add(devices[j]);
			    					WiFiCounter.add(1);
		    					}
		    				}
		    				else
		    				{
		    					// get counter of the AP that we found
		    					current_counter = WiFiCounter.get(reading_index);
		    					// one more visit detected
		    					current_counter++;
		    					// write it back to list of counters
		    					WiFiCounter.set(reading_index, current_counter);
		    				}
		    			}
		    			// now move to next row
		    			values.moveToNext();
		    		}
		    		
		    		// sort AP list based on visit
					sortAPList();
					
		    		// return task and show values
			        return Long.valueOf(0);
		    	}
		    	else
			        return Long.valueOf(-1);		    	
	     }

	     protected void onPreExecute() 
	     {
         	progress.setVisibility(View.VISIBLE);
	     }

	     protected void onPostExecute(Long result) 
	     {
	    	 int i, j;
	    	 String currentWiFi;
	    	 
	    	 // free database memory
	    	 values.close();
	    	 values = null;
	    	 
	    	 if (result.longValue() == 0)
	    	 {
		     	Toast.makeText(getApplicationContext(), R.string.AdaptiveGPS_Hitlist_Click, Toast.LENGTH_LONG).show();
	    		
	    		mAPsArrayAdapter.clear();
	    		// now write the names of the visited WiFis into the listview adapter
	    		for (i=0;i<WiFiNames.size();i++)
	    			mAPsArrayAdapter.add(WiFiNames.get(i));
	    		
	    		// notify UI thread to redraw
	    		mAPsArrayAdapter.notifyDataSetChanged();
	    		
	    		// now set checkmarks in the listview depending on whether the entry is already marked for adaptive GPS
	    		for (i=0;i<WiFiNames.size();i++)
	    		{
	    			// get current WiFi name
	    			currentWiFi = WiFiNames.get(i);
	    			// now see if the WiFi name is already in the adaptive GPS list
	    			for (j=0;j<adaptiveWifis.length;j++)
	    				if (adaptiveWifis[j].compareTo(currentWiFi) == 0)
	    					hitlist.setItemChecked(i, true);
	    		}
	    		// let the progress bar disappear
	 	        progress.setVisibility(View.INVISIBLE);
	    	 }
	    	 else
	    	 {
	     		Toast.makeText(getApplicationContext(), R.string.AdaptiveGPS_NothingFound, Toast.LENGTH_LONG).show();
	     		finish();
	    	 }
	     }
	}
	
	/**
	 * sorts the AP list that was crawled by the GatherTask
	 */
	private void sortAPList()
	{
		int max, i, j;
		int max_counter = 0, total_counters, current_counter;
		String copyString;
		
		// get total number of counted APs
		total_counters = WiFiCounter.size();
		
		for (i=0;i<total_counters;i++)
		{
			max = Integer.MIN_VALUE;
			
			// find maximum value of the remaining array
			for (j=i;j<total_counters;j++)
			{
				if (WiFiCounter.get(j) > max)
				{
					max = WiFiCounter.get(j);
					max_counter = j;
				}
			}
			// get the value of the current array position
			current_counter = WiFiCounter.get(i);
			// copy the current array position to where the maximum value was stored
			WiFiCounter.set(max_counter, current_counter);
			// now store the maximum value at the current position
			WiFiCounter.set(i, max);
			
			// now swap the AP names, too
			copyString = WiFiNames.get(max_counter);
			WiFiNames.set(max_counter, WiFiNames.get(i));
			WiFiNames.set(i, copyString);					
		}
	}
	
	/**
	 * Checks the list of APs as to which ones need to be deleted from the stored list and which ones to be added
	 */
	private void writeNewAPs()
	{
		int i, j;
		String currentWiFi;
		boolean found;
		
		// go through the entire list to remove anything to be removed
		for (i=0;i<hitlist.getCount();i++)
		{
			// is the current item in the list not checked?
			if (hitlist.isItemChecked(i) == false)
			{
				// get current WiFi name
				currentWiFi = WiFiNames.get(i);
				found = false;

				storedWifis = new StringBuffer("");
				// now create a new list without the one to be removed
				for (j=0;j<adaptiveWifis.length;j++)
					if (adaptiveWifis[j].compareTo(currentWiFi) != 0)
					{
						if (found == true)
							storedWifis.append("::");
						
						storedWifis.append(adaptiveWifis[j]);
						
						found = true;
					}
				
				// now re-create the individual marked WiFis again
				adaptiveWifis = storedWifis.toString().split("::");
			}
		}
		
		// now go through the entire list again in order to add what needs to be added
		for (i=0;i<hitlist.getCount();i++)
		{
			// is the current item in the list checked?
			if (hitlist.isItemChecked(i) == true)
			{
				// get current WiFi name
				currentWiFi = WiFiNames.get(i);
				found = false;
				
				// now see if the WiFi name is already in the adaptive GPS list
				for (j=0;j<adaptiveWifis.length;j++)
					if (adaptiveWifis[j].compareTo(currentWiFi) == 0)
						found = true;
				
				// not found marked already in the stored list -> add to the persistent list
				if (found == false)
					storedWifis.append("::" + currentWiFi);
			}
		}
		
		
		// now write changes to persistent settings
		Editor editor = settings.edit();
		editor.putString("LocationHandler::AdaptiveGPS_WiFis", storedWifis.toString());
		editor.commit();
	}
}
