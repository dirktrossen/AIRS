package com.airs;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class TimelineActivity extends Activity
{    
	public static final int PUSH_VALUES = 1;

	// Layout Views
    private TextView		mTitle;
    public  TextView 		mTitle2;
    private TextView		minX, maxX, minY, maxY;
    private TimelineView 	DisplayView;
    private Bundle bundle;
	private float history_f[];
	private long time[];
	private int  number_values;
	private long minTime = Long.MAX_VALUE;
	private long maxTime = Long.MIN_VALUE;
	private long startedTime;
	private float min = Float.MAX_VALUE;
	private float max = Float.MIN_VALUE;
	private int history_length;
    // database variables
    AIRS_database database_helper;
    SQLiteDatabase airs_storage;
    private SharedPreferences settings;

   	/**
	 * Sleep function 
	 * @param millis
	 */
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
    		String title;
    		Intent intent = getIntent();
    		Time timeStamp = new Time();

            // Set up the window layout
            super.onCreate(savedInstanceState);

            // get activity parameters
            bundle = intent.getExtras();
            
        	// how many to store?
            settings = PreferenceManager.getDefaultSharedPreferences(this);
            history_length = Integer.parseInt(settings.getString("SensorHistory", "20"));

    		history_f = new float[history_length];
    		time = new long[history_length];
    		
    		// start time of measurement
    		startedTime = settings.getLong("AIRS_local::time_started", 0);
    		
            // now open database
            database_helper = new AIRS_database(this.getApplicationContext());
            airs_storage = database_helper.getReadableDatabase();

            // set window title
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	        setContentView(R.layout.timelineview);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	 
	        // get Desktop dimensions
	        Display display = getWindowManager().getDefaultDisplay();       
	        int width = display.getWidth();

	        // set dialog dimensions
        	getWindow().setLayout(width, width*9/16); // 
	        
	        // get window title fields
	        mTitle = (TextView) findViewById(R.id.title_left_text);
	        mTitle2 = (TextView) findViewById(R.id.title_right_text);
	        mTitle.setText(R.string.app_name);
	       
	        // get axis text fields
	        minX = (TextView) findViewById(R.id.timeline_minx);
	        maxX = (TextView) findViewById(R.id.timeline_maxx);
	        minY = (TextView) findViewById(R.id.timeline_miny);
	        maxY = (TextView) findViewById(R.id.timeline_maxy);

	        title = bundle.getString("com.airs.Title");
	        if (title != null)
	        	mTitle2.setText(title);
	        else
	        	mTitle2.setText("Title");
	        
	        // set timeline view
	        DisplayView = (TimelineView) findViewById(R.id.surfaceMeasure);
	        DisplayView.invalidate();
	        
	        // get data entries and determine min/max as well as set edit fields
	        getDataAndMax();
	        
	        switch (number_values)
	        {
	        	case 0:	// if there's no value, then return
		        	finish();
		        	break;
	        	case 1:	// if there's only one value then show Toast message only
	        		timeStamp.set(time[0]);
	        		minX.setText(timeStamp.format("%H:%M:%S"));
					
        			Toast.makeText(getApplicationContext(), "First sensing at " + timeStamp.format("%H:%M:%S") + " with value : " + Float.toString(history_f[0]), Toast.LENGTH_LONG).show();
	        		finish();
	        		break;    
	        }
    }

    @Override
    public void onResume() 
    {
        super.onResume();

		// now indicate finished writing by sending new values
        Message msg = mHandler.obtainMessage(PUSH_VALUES);
		msg.setData(bundle);
        mHandler.sendMessageDelayed(msg, 500);
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
        	// call about dialogue
    		Toast.makeText(getApplicationContext(), R.string.TimelineAbout, Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
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
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {  
    	return;
    }
    
    private void getDataAndMax()
    {
		int i;
		Time timeStamp = new Time();
		String minS, maxS;
		String symbol;
		String [] columns = {"Timestamp", "Value"};
		int t_column, v_column;
		Cursor values;
		String value;
		
		symbol = bundle.getString("com.airs.Symbol");

		String selection = "Symbol='"+ symbol +"'";
		
		// issue query to the database
		values = airs_storage.query(database_helper.DATABASE_TABLE_NAME, columns, selection, null, null, null, "Timestamp DESC", String.valueOf(history_length));
		    	
		if (values == null)
			finish();
		
		// get column index for timestamp and value
		t_column = values.getColumnIndex("Timestamp");
		v_column = values.getColumnIndex("Value");
		
		if (t_column == -1 || v_column == -1)
			finish();
		
		number_values = values.getCount();
				
		// are there any values?
    	if (number_values != 0)
    	{
    		// move to first row to start
    		values.moveToFirst();
    		// read DB values into arrays
    		for (i=number_values-1;i>=0;i--)
    		{
    			// get timestamp
    			time[i] = values.getLong(t_column);
    			// get value
    			value = values.getString(v_column);
    			if (value != null)
    				history_f[i] = Float.parseFloat(value);
    			
    			// now move to next row
    			values.moveToNext();
    		}
    		// determine min/max of view 
    		for (i=0;i<number_values;i++)
    		{
    			if (time[i]>startedTime)
    			{
	    			if (history_f[i]<min)
	    				min = history_f[i];
	    			if (history_f[i]>max)
	    				max = history_f[i];
	    			if (time[i]<minTime)
	    				minTime = time[i];
	    			if (time[i]>maxTime)
	    				maxTime = time[i];
    			}
    		}       		

    		// if values before left of decimal are the same -> need to show float decimals
    		if (Math.floor(min) == Math.floor(max))
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
    		timeStamp.set(minTime);
    		minX.setText(timeStamp.format("%H:%M:%S"));
    		timeStamp.set(maxTime);
    		maxX.setText(timeStamp.format("%H:%M:%S"));
    		
    		DisplayView.postInvalidate();       		
    	}
    	else
    		finish();
    }
    
    // The Handler that gets information back from the other services
    public final Handler mHandler = new Handler() 
    {
        @Override
        public void handleMessage(Message msg) 
        {        	
        	int i;
        	
    		if (msg.what == PUSH_VALUES)
        	{
        		// set scaling in view properly
        		DisplayView.setMinMax(min, max, minTime, maxTime);

        		// push values into path 
        		for (i=0;i<number_values;i++)
        		{
        			if (time[i]>startedTime)
        				DisplayView.pushPath(time[i], history_f[i]);
        		}
        			        		
        		DisplayView.postInvalidate();
        	}
        }
    };
}