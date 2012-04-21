package com.airs;

import java.util.Calendar;

import com.airs.platform.History;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
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
    private int type;  
    private Bundle bundle;
	private int history_i[];
	private long time[];
	private int  number_values;
	private long minTime = Long.MAX_VALUE;
	private long maxTime = Long.MIN_VALUE;
	private int min = Integer.MAX_VALUE;
	private int max = Integer.MIN_VALUE;
	private int scaler = 0;

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
    		type = bundle.getInt("com.airs.Type", History.TYPE_INT);
	
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	        setContentView(R.layout.timelineview);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	 
	        // get Deskptop dimensions
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
					
	        		if (scaler == 0)
	        			Toast.makeText(getApplicationContext(), "First sensing at " + timeStamp.format("%H:%M:%S") + " with value : " + Integer.toString(history_i[0]), Toast.LENGTH_LONG).show();
	        		else
	        			Toast.makeText(getApplicationContext(), "First sensing at " + timeStamp.format("%H:%M:%S") + " with value : " + Integer.toString(history_i[0]) + "* 10^" + Integer.toString(scaler), Toast.LENGTH_LONG).show();
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
		float fscaler, minx, maxx;
		String minS, maxS;

		number_values = bundle.getInt("com.airs.Length");	// get number of values
    	time = bundle.getLongArray("com.airs.Time");		// get time values
    	scaler = bundle.getInt("com.airs.Scaler");			// scaler for values
    	
    	// determine 10^scaler
   	 	fscaler = 1;
		if (scaler>0)
			 for (i=0;i<scaler;i++)
				 fscaler *=10;
		else
			 for (i=scaler;i<0;i++)
				 fscaler /=10;
    	
        if (type ==History.TYPE_INT)
        {
        	history_i = bundle.getIntArray("com.airs.Int");		// get sensor values
        	if (history_i != null)
        	{
        		// determine min/max of view 
        		for (i=0;i<number_values;i++)
        		{
        			if (history_i[i]<min)
        				min = history_i[i];
        			if (history_i[i]>max)
        				max = history_i[i];
        			if (time[i]<minTime)
        				minTime = time[i];
        			if (time[i]>maxTime)
        				maxTime = time[i];
        		}       		
        		// set axis descriptions
        		minx = (float)min * fscaler;
        		maxx = (float)max * fscaler;
        		
        		// if values before left of decimal are the same -> need to show float decimals
        		if (Math.floor(minx) == Math.floor(maxx))
        		{      
        			minS = Float.toString((float)min*fscaler);
        			maxS = Float.toString((float)max*fscaler);
        			
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
            		minY.setText(Integer.toString((int)((float)min*fscaler)));
            		maxY.setText(Integer.toString((int)((float)max*fscaler)));        			
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
    	        switch(type)
    	        {
    	        case History.TYPE_INT:
            		// set scaling in view properly
            		DisplayView.setMinMax(min, max, minTime, maxTime);

	        		// push values into path 
	        		for (i=0;i<number_values;i++)
	        			DisplayView.pushPath(time[i], history_i[i]);
	        			        		
	        		DisplayView.postInvalidate();
    	        	break;
    	        case History.TYPE_COORD:
    	        	finish();
    	        	break;
    	        }
        	}
        }
    };
}