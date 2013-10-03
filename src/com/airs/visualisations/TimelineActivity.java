/*
Copyright (C) 2013, TecVis, support@tecvis.co.uk

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
package com.airs.visualisations;

import java.util.Calendar;
import java.util.Locale;

import com.airs.AIRS_local;
import com.airs.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ZoomButton;

/**
 * Class to implement a timeline view for all sensors that support it. This activity is started from the {@link com.airs.platform.History} class after an item supporting timeline has been clicked on by the user
 *
 */
public class TimelineActivity extends Activity implements OnTouchListener, OnClickListener, TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener
{   
	// handler variables
	private static final int FINISH_ACTIVITY	= 1;
	private static final int PUSH_VALUES 	= 2;
	
	// offset for full day timestamp
	private static final long FULL_DAY 		= 1000*60*60*24;	// milliseconds per day
	
	// Layout Views
    private TextView		mTitle;
    private TextView		minX, maxX, minY, maxY;
    private TimelineView 	DisplayView;
    private ProgressBar		progressbar;
    private ZoomButton		zoomIn, zoomOut;
    private Bundle bundle;
	private float history_f[];
	private long time[];
	private int  first_values;
	private int  repeatInterval, repeatJump;
	private long minTime = Long.MAX_VALUE;
	private long maxTime = Long.MIN_VALUE;
	private long startedTime, endTime, windowTime, minReadingTime, maxWindowTime;
	private int currentIndex = 0, valuesShown;
	private int currentZoom = 0, maxZoom = 6;
	private float min = Float.MAX_VALUE;
	private float max = Float.MIN_VALUE;
	private boolean showGrid;
	private boolean showAverage;
	private boolean setMax;
	private float averageValue;
	private String Symbol;
    // database variables
    private SQLiteDatabase airs_storage;
	private Cursor values = null;
	private AsyncTask<String, Long, Long> task;

	/** Called when the activity is first created. 
     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
     */
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
    		String title;
    		Intent intent = getIntent();

            // Set up the window layout
            super.onCreate(savedInstanceState);
           
            // get activity parameters
            bundle = intent.getExtras();
            Symbol = bundle.getString("com.airs.Symbol");		// get symbol
            // get time of midnight
			Calendar cal = Calendar.getInstance(Locale.getDefault());
			cal.set(Calendar.HOUR, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.AM_PM, Calendar.AM);
            startedTime = cal.getTimeInMillis();					// timestamp of start of visualisation
            endTime 	= startedTime + FULL_DAY;					// end time
            windowTime = FULL_DAY;									// start with full timewindow
            currentZoom = 0;										// show full zoom
            currentIndex = 0;
                       
            // now open database
            airs_storage = AIRS_local.airs_storage;

            if (airs_storage != null)
            {
	            // get preferences
	            repeatInterval = 500;
	            repeatJump     = 25;
	            showGrid       = true;
	            showAverage       = true;
	
	            // set window title
		        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		        setContentView(R.layout.timelineview);
		        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.timeline_title);
		        // get window title fields
		        mTitle = (TextView) findViewById(R.id.timeline_title_text);
	
		        // set title of window to string with sensor description
		        title = bundle.getString("com.airs.Title");
		        if (title != null)
		        	mTitle.setText(title);
		        else
		        	mTitle.setText("-");
		 
		        // get Desktop dimensions
		        Display display = getWindowManager().getDefaultDisplay();       
		        int width = display.getWidth();
		        int height = display.getHeight();
		        
		        // set dialog dimensions
		        if (width*9/16 > height)
		        	getWindow().setLayout(height*16/9, height);  
		        else
		        	getWindow().setLayout(width, width*9/16);  
		        
		        // get axis text fields
		        minX  = (TextView) findViewById(R.id.timeline_minx);
		        maxX  = (TextView) findViewById(R.id.timeline_maxx);
		        minY  = (TextView) findViewById(R.id.timeline_miny);
		        maxY  = (TextView) findViewById(R.id.timeline_maxy);
	
		        // get zoom buttons
		        zoomIn  = (ZoomButton) findViewById(R.id.timeline_zoomIn);
		        zoomIn.setOnClickListener(this);
				zoomIn.setEnabled(true);
		        zoomOut = (ZoomButton) findViewById(R.id.timeline_zoomOut);
		        zoomOut.setOnClickListener(this);
				zoomOut.setEnabled(false);
	
		        // set listener for button clicks
		        Button button = (Button) findViewById(R.id.timeline_backward);
		        button.setOnTouchListener(this);
		        button = (Button) findViewById(R.id.timeline_forward);
		        button.setOnTouchListener(this);
		        ImageView select = (ImageView) findViewById(R.id.timeline_select_maxx);
		        select.setOnClickListener(this);
		        select = (ImageView) findViewById(R.id.timeline_select_minx);
		        select.setOnClickListener(this);
		        
		        // set timeline view
		        DisplayView = (TimelineView) findViewById(R.id.surfaceMeasure);
		        DisplayView.invalidate();
		        
		        // get progress bar
		        progressbar = (ProgressBar) findViewById(R.id.timeline_progress);
	
		    	// now draw markers
		        task = new GatherTask();
		        task.execute(Symbol);
            }
            else
            	finish();
    }
 
	/** Called when the activity is resumed. 
     */
	@Override
    public void onResume() 
    {
        super.onResume();
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
        if (task != null)
        	task.cancel(true);
        
        // free DB resources
        if (values != null)
     	   values.close();
    }      
    
	/** Called when the configuration of the activity has changed.
     * @param newConfig new configuration after change 
     */
	@Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
      
      // get Desktop dimensions
      Display display = getWindowManager().getDefaultDisplay();       
      int width = display.getWidth();
      int height = display.getHeight();
      
      // set dialog dimensions
      if (width*9/16 > height)
      	getWindow().setLayout(height*16/9, height);  
      else
      	getWindow().setLayout(width, width*9/16);  
      
		// now push values into display
      Message push_msg = mHandler.obtainMessage(PUSH_VALUES);
      mHandler.sendMessage(push_msg);	
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

	/** Called when a button has been clicked on by the user
     * @param v Reference to the {@link android.view.View} of the button
     */
	@Override
	public void onClick(View v)
	{	
		TimePickerDialog timepicker;
		Time timestamp = new Time();
		
		switch (v.getId())
		{
		case R.id.timeline_select_minx:
			// setting minimum time?
			// indicate that min time is set
			timestamp.set(minTime);
			setMax = false;
			
			// create time picker dialog and show
			timepicker = new TimePickerDialog(this, this, timestamp.hour, timestamp.minute, true);
			timepicker.setTitle(R.string.Timeline_Viewer3);
			timepicker.show();
			return;
		case R.id.timeline_select_maxx:
			// setting maximum time?
			// indicate that max time is set
			timestamp.set(maxTime);
			setMax = true;

			// create time picker dialog and show
			timepicker = new TimePickerDialog(this, this, timestamp.hour, timestamp.minute, true);
			timepicker.setTitle(R.string.Timeline_Viewer4);
			timepicker.show();
			return;
		case R.id.timeline_zoomIn:
			currentZoom++;
			if (currentZoom>maxZoom)
			{
				zoomIn.setEnabled(false);
				currentZoom--;
			}
			zoomOut.setEnabled(true);
			break;
		case R.id.timeline_zoomOut:
			currentZoom--;
			if (currentZoom<0)
			{
				zoomOut.setEnabled(false);
				currentZoom++;
			}
			zoomIn.setEnabled(true);
			break;
		}
		
    	// select zoom data based on level now
    	switch(currentZoom)
          {
          case 0:
          	windowTime = maxWindowTime;
          	currentIndex = 0;
          	break;
          case 1:
       		windowTime = 6 * 3600 * 1000;
          	break;
          case 2:
       		windowTime = 3 * 3600 * 1000;
          	break;
          case 3:
       		windowTime = 1 * 3600 * 1000;
          	break;
          case 4:
       		windowTime = 30 * 60 * 1000;
           	break;
          case 5:
       		windowTime = 10 * 60 * 1000;
            break;
          case 6:
       		windowTime = 5 * 60 * 1000;
          	break;          
          default:
        	 break; 
          }
    	
    	progressTimeline(0);
	}

	/**
	 * Called when time is set in {@link android.widget.TimePicker}
	 * @param view Reference to {@link android.widget.TimePicker} view
	 * @param hourOfDay hour of day being chosen
	 * @param minute minute of hour being chosen
	 */
	public void onTimeSet (TimePicker view, int hourOfDay, int minute)
	{
		Calendar cal = Calendar.getInstance();
		int i;
		long chosen_mills;
		boolean redraw = false;
		
		// set to today's min time
		cal.setTimeInMillis(minTime);
		// now set hour and minutes
		cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
		cal.set(Calendar.MINUTE, minute);
		
		// get milliseconds
		chosen_mills = cal.getTimeInMillis();
		
		// set minimum time
		if (setMax == false)
		{
			// valid time set?
			if (chosen_mills>=time[0] && chosen_mills<time[first_values-1])
			{
				for (i=0;i<first_values && time[i] <= chosen_mills;i++) ;

				currentIndex = i;
				windowTime = maxTime - time[currentIndex];
				redraw = true;
			}
		}
		else
		{
			// valid time set?
			if (chosen_mills>time[0] && chosen_mills<=time[first_values-1])
			{
				for (i=first_values-1;i>=0 && time[i] >= chosen_mills;i--) ;

				windowTime = time[i] - time[currentIndex];		
				redraw = true;
			}
		}
			
		// force redraw?
		if (redraw == true)
			mHandler.sendMessage(mHandler.obtainMessage(PUSH_VALUES));	
	}
	
	/**
	 * Called when date is set in {@link android.widget.DatePicker}
	 * @param view Reference to {@link android.widget.DatePicker} view
	 * @param year year being chosen
	 * @param monthOfYear month of the year being chosen
	 * @param dayOfMonth day of the month being chosen
	 */
	public void onDateSet (DatePicker view, int year, int monthOfYear, int dayOfMonth)
	{
		Calendar cal = Calendar.getInstance();
		int i;
		long chosen_mills;
		boolean redraw = false;
		
		// set to today's min time
		cal.setTimeInMillis(minTime);
		// now set hour and minutes
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, monthOfYear);
		cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		
		// get milliseconds
		chosen_mills = cal.getTimeInMillis();
		
		// set minimum time
		if (setMax == false)
		{
			// valid time set?
			if (chosen_mills>=time[0])
			{
				for (i=0;i<first_values && time[i] <= chosen_mills;i++) ;

				currentIndex = i;
				windowTime = maxTime - time[currentIndex];
				redraw = true;
			}
		}
		else
		{
			// valid time set?
			if (chosen_mills<=time[first_values-1])
			{
				for (i=first_values-1;i>=0 && time[i] >= chosen_mills;i--) ;

				windowTime = time[i] - time[currentIndex];		
				redraw = true;
			}
		}
			
		// force redraw?
		if (redraw == true)
			mHandler.sendMessage(mHandler.obtainMessage(PUSH_VALUES));	
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
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(getString(R.string.AIRS_Timeline))
    			   .setMessage(getString(R.string.TimelineAbout))
    			   .setIcon(R.drawable.about)
    		       .setNeutralButton(getString(R.string.OK), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		                dialog.dismiss();
    		           }
    		       });
    		AlertDialog alert = builder.create();
    		alert.show();
    		
    		// Make the textview clickable. Must be called after show()
    	    ((TextView)alert.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            return true;
        }

        return true;
    }
        
    /**
     * Called for dispatching key events sent to the Activity
     * @param event Reference to the {@link android.view.KeyEvent} being pressed
     * @return true, if consumed, false otherwise
     */
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
    
	/**
	 * Called when touch event occurred
	 * @param v Reference to the {@link android.view.View} that has focus
	 * @param me Reference to the {@link android.view.MotionEvent} of the touch event
	 */
	public boolean onTouch(View v, MotionEvent me) 
	{
    	long progress = (long)(valuesShown*repeatJump)/100;
    	
    	// ensure at least one value increment!
    	if (progress == 0) 
    		progress = 1;
    	
        if (me.getAction() == MotionEvent.ACTION_DOWN) 
        {
        	// set state to pressed
        	v.setPressed(true);

	    	switch(v.getId())
	    	{
	    	case R.id.timeline_forward:
	        	progressTimeline(progress);
	
		        new PressedThread((Button)v);
	    		return true;
	    	case R.id.timeline_backward:
	        	progressTimeline(-progress);
	
		        new PressedThread((Button)v);
		        
		        return true;
	    	}
        }
        
        if (me.getAction() == MotionEvent.ACTION_UP)
        {
        	v.setPressed(false);
        	return true;
        }
        
        return false;
    }

    
    // repeat progress in timeline window after arrow buttons have been pressed
	// run until button is depressed
	private class PressedThread implements Runnable
	{
		Button button;
		
	 	PressedThread(Button pressed)
		{
	 		button = pressed;
			(new Thread(this)).start();
		}
	     public void run()
	     {
	    	 // wait first before setting on repeat
	    	 try
	    	 {
	    		Thread.sleep(repeatInterval);
	     	 }
	    	 catch(Exception e)
	    	 {
	    	 }

	    	 // repeat while being pressed
	    	 while(button.isPressed() == true)
	    	 {   	    	    	
	    		 	long progress = (long)(valuesShown*repeatJump)/100;
		 	    	// ensure at least one value increment!
		 	    	if (progress == 0) 
		 	    		progress = 1;
		 	    	
	    	    	switch(button.getId())
	    	    	{
	    	    	case R.id.timeline_forward:
	    	        	progressTimeline(progress);
	    	    		break;
	    	    	case R.id.timeline_backward:
	    	        	progressTimeline(-progress);
	    	    		break;
	    	    	}    
	    	    	
	    	    	try
	    	    	{
	    	    		Thread.sleep(repeatInterval);
	    	    	}
	    	    	catch(Exception e)
	    	    	{
	    	    	}
	    	 }
	    		 
	     }
	}
    
	private void progressTimeline(long progress)
	{
		int new_index = currentIndex, i;
        Message push_msg = mHandler.obtainMessage(PUSH_VALUES);

		new_index += progress;
    	// new index out of day? -> determine boundaries
    	if (new_index < 0)
    		new_index = 0;
    	if (new_index>=first_values)
    		new_index = first_values-1;
    	
    	if (windowTime + time[new_index] > time[first_values-1])
    	{
    		// move from rightmost value (end of day) down until window size is filled 
    		for (i=first_values-1;i>=0;i--)
    		{
    			if (time[i] + windowTime>=time[first_values-1])
    				new_index = i;
    			else
    				continue;
    		} 
    	}
    	
   		currentIndex = new_index;     	// current index is new index

   		// does current window go beyond end of day?
    	if (windowTime + time[currentIndex] > minReadingTime + maxWindowTime)
    		windowTime = minReadingTime + maxWindowTime - time[currentIndex];

    	// now push values into display
        mHandler.sendMessage(push_msg);	
	}
	
	
	private class GatherTask extends AsyncTask<String, Long, Long> 
	{
	     protected Long doInBackground(String... params) 
	     {
				int i, number_values;
				int t_column, v_column;
				String query;		

				// issue query to the database
				if (values == null)
				{
					// single or double value query?
					query = new String("SELECT Timestamp, Value from 'airs_values' WHERE Timestamp BETWEEN " + String.valueOf(startedTime) + " AND " + String.valueOf(endTime) + " AND Symbol='" + Symbol + "'");

					values = airs_storage.rawQuery(query, null);
				}
				    	
				if (values == null)
			        return Long.valueOf(-1);
				
				// get column index for timestamp and value
				t_column = values.getColumnIndex("Timestamp");
				v_column = values.getColumnIndex("Value");
				
				if (t_column == -1 || v_column == -1)
			        return Long.valueOf(-1);
				
				number_values = values.getCount();
						
				// are there any values?
		    	if (number_values != 0)
		    	{
		    		// allocate history fields
		    		time = new long[number_values];
		    		history_f = new float[number_values];
		    		
		    		// prepare averaging
		    		averageValue = 0.0f;
		    		
		    		first_values = 0;
		    		
		    		// move to first row to start
		    		values.moveToFirst();
		    		// read DB values into arrays
		    		for (i=0;i<number_values;i++)
		    		{
		    			// get timestamp
		    			time[first_values] = values.getLong(t_column);

		    			// store minimal time in the DB readings
		    			if (first_values==0)
		    				minReadingTime = time[0];
		    			
		    			// maximum window time
	    				windowTime = maxWindowTime = time[first_values] - minReadingTime;
		    			
		    			// get value
	    				history_f[first_values]  = values.getFloat(v_column);
	    				// count for averaging
	    				averageValue += history_f[first_values];
	    				
	    				// count first values
	    				first_values++;
		    			
		    			// now move to next row
		    			values.moveToNext();
		    		}
		    		
		    		// now average
		    		if (first_values != 0)
		    			averageValue /= first_values;
		    		else	// if there's no first symbol value, return error
				        return Long.valueOf(-1);		    	

		    		// return task and show values
			        return Long.valueOf(0);
		    	}
		    	else
			        return Long.valueOf(-1);		    	
	     }

	     protected void onPreExecute() 
	     {
         	progressbar.setVisibility(View.VISIBLE);
	     }

	     protected void onPostExecute(Long result) 
	     {
	    	 int i;
	    	 
	    	 // everything ok -> show values
	    	 if (result.longValue() == 0)
	    	 {     		
	     			// handle the case of a single value
	     		switch(first_values)
	     		{
	     		case 0:
		        	finish();
		        	break;
	     		case 1:
     	    		Time timeStamp = new Time();
	        		timeStamp.set(time[0]);
					
        			Toast.makeText(getApplicationContext(), getString(R.string.First_sensing) + " " + timeStamp.format("%H:%M:%S") + " " + getString(R.string.First_sensing2) + " " + Float.toString(history_f[0]), Toast.LENGTH_LONG).show();
	        		finish();

	     		}
	     		
        		// determine min/max values
        		if (getMaxMin() == true)
        		{  
	        		// set scaling in view properly
	        		DisplayView.setMinMax(min, max, minTime, maxTime);
	
	        		// push values into path 
	        		for (i=currentIndex;i<first_values;i++)
	        		{
	        			if (time[i]<=time[currentIndex] + windowTime)
	        				DisplayView.pushPath(time[i], history_f[i]);
	        			else
	        			{
        					DisplayView.pushPath(time[currentIndex] + windowTime, history_f[i]);
	        				break;
	        			}
	        		}

	        		// showing average?
	        		if (showAverage == true)
	        			DisplayView.pushAverage(averageValue);

	        		// showing grid?
	        		if (showGrid == true)
	        			DisplayView.pushGrid();

	        		// set progress bar with display view to make it invisible after drawing
	        		DisplayView.setProgressBar(progressbar);
	        		DisplayView.postInvalidate();
        		}
	    	 }
	    	 else
	    	 {
	             Log.e("AIRS", "...terminated GatherThread()");

	    		 finish();
	    	 }
	     }
	}
	
	@SuppressLint("FloatMath")
	private boolean getMaxMin()
	{
		int i;
		Time timeStamp = new Time();
		String minS, maxS;

		// reset min/max values
		minTime = Long.MAX_VALUE;
		min = Float.MAX_VALUE;
		max = -Float.MAX_VALUE;

		valuesShown = 0;
		
		// determine min/max of first value set 
		for (i=currentIndex;i<first_values;i++)
		{
			if (time[i]<=time[currentIndex] + windowTime)
			{
    			if (history_f[i]<min)
    				min = history_f[i];
    			if (history_f[i]>max)
    				max = history_f[i];
    			if (time[i]<minTime)
    				minTime = time[i];
    			
    			// at least one value fits
    			valuesShown++;
			}
			else
				break;
		}

		if (valuesShown == 0)
			return false;   

		// if all values are the same, draw epsilon around them!
		if (min == max)
		{
			min -=  min/10;
			max +=  min/10;
		}
		
		maxTime = minTime + windowTime;

		// if values before left of decimal are the same -> need to show float decimals
		if (android.util.FloatMath.floor(min) == android.util.FloatMath.floor(max))
		{      
			minS = Float.toString(min);
			maxS = Float.toString(max);
			
			// show same length decimals!
			if (minS.length() > maxS.length())
			{	
        		minY.setText(minS.substring(0, maxS.length()));
        		maxY.setText(maxS);
			}
			else
			{
        		minY.setText(minS);
        		maxY.setText(maxS.substring(0, minS.length()));        				
			}
		}
		else	// otherwise only show integers
		{
    		minY.setText(Integer.toString((int)(min)));
    		maxY.setText(Integer.toString((int)(max)));        			
		}

		// get current maxY text
		String oldMax = maxY.getText().toString();

		// y-axis padding: is max text smaller than min text?
		if (oldMax.length()<minY.getText().length())
		{
			int difference = minY.getText().length() - oldMax.length();
			StringBuffer maxPadding = new StringBuffer();
			
			// now create string with spaces to pad
			for (i=0;i<difference * 3;i++)
				maxPadding.append(" ");
			
			maxY.setText(maxPadding.toString() + oldMax);
		}

		// set time axis now
		timeStamp.set(minTime);
		minX.setText(timeStamp.format(getString(R.string.TimeFormat2)));
		timeStamp.set(maxTime);
		maxX.setText(timeStamp.format(getString(R.string.TimeFormat2)));	
		
		return true;
	}
    
    // The Handler that gets information back from the other services
    private final Handler mHandler = new Handler() 
    {
        @Override
        public void handleMessage(Message msg) 
        {        	
        	int i;
        	
        	switch(msg.what)
        	{
        	case PUSH_VALUES:
        		// determine min/max values
        		if (getMaxMin() == true)
        		{       		
	        		// set scaling in view properly
	        		DisplayView.setMinMax(min, max, minTime, maxTime);

	        		// push values into path 
	        		for (i=currentIndex;i<first_values;i++)
	        		{
	        			if (time[i]<=time[currentIndex] + windowTime)
	        				DisplayView.pushPath(time[i], history_f[i]);
	        			else
	        			{
        					DisplayView.pushPath(time[currentIndex] + windowTime, history_f[i]);
	        				break;
	        			}
	        		}
	        		        		
	        		// showing average?
	        		if (showAverage == true)
	        			DisplayView.pushAverage(averageValue);
	        		
	        		// showing grid?
	        		if (showGrid == true)
	        			DisplayView.pushGrid();

	    	        // set progress bar with display view to make it invisible after drawing
	        		DisplayView.setProgressBar(progressbar);
	        		DisplayView.postInvalidate();
        		}
        		break;
            case FINISH_ACTIVITY:
         	   break;
        	}
        }
    };
}