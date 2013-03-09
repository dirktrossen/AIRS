/*
Copyright (C) 2012, Dirk Trossen, airs@dirk-trossen.de

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

import java.util.List;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

public class MapViewerActivity extends MapActivity implements OnClickListener
{
    private TextView		mTitle;
    public  TextView 		mTitle2;
	private MapView mapView;
    private Bundle bundle;
    private String history[];			// different history arrays
	private long time[];
	private int  number_values;
	private int zoomLevel;
	private List<Overlay> mapOverlays;
	private Drawable drawable;
	private MapViewerOverlay itemizedOverlay;
	private MyLocationOverlay ownLocation;
	private MapController mapController;
    private SharedPreferences settings;
    private Editor editor;
    private GeoPoint last_recorded_location;
    private boolean showTrack = true;
    private String Symbol;
    private SQLiteDatabase airs_storage;
    private int history_length;
    private long startedTime;
	private Cursor values = null;
    private boolean FirstDrawn = true;

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
   		String title;
		Intent intent = getIntent();

        // Set up the window layout
        super.onCreate(savedInstanceState);

        // get preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);
       	editor = settings.edit();

        // now open database
        airs_storage = AIRS_local.airs_storage;

       	// get previous zoom level
       	zoomLevel = settings.getInt("ZoomLevel", 15);
        history_length = Integer.parseInt(settings.getString("SensorHistory", "20"));

        // start time of measurement
		startedTime = settings.getLong("AIRS_local::time_started", 0);

        // history fields for faster access
		history = new String[history_length];
		time = new long[history_length];

        // get activity parameters
        bundle = intent.getExtras();

        Symbol = bundle.getString("com.airs.Symbol");	// get symbol

        // window title as feature
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.mapview);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        
        // get window title fields
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle2 = (TextView) findViewById(R.id.title_right_text);
        mTitle.setText(R.string.app_name);
       
        // set listener for buttons
        ImageButton button 		= (ImageButton)findViewById(R.id.mapview_my_location);
        button.setOnClickListener(this);
        button 		= (ImageButton)findViewById(R.id.mapview_last_location);
        button.setOnClickListener(this);

        // get and set title
        title = bundle.getString("com.airs.Title");
        if (title != null)
        	mTitle2.setText(title);
        else
        	mTitle2.setText("Title");
        
        // add zoom
        mapView = (MapView) findViewById(R.id.mapview);
    	mapView.setBuiltInZoomControls(true);
    	
    	// get controller for map
    	mapController = mapView.getController();
    	// set zoom level
    	mapController.setZoom(zoomLevel);
    	
    	// create overlays
    	mapOverlays = mapView.getOverlays();
    	drawable = this.getResources().getDrawable(R.drawable.pin);
    	itemizedOverlay = new MapViewerOverlay(drawable, getApplicationContext());
    	
    	// initiate own location overlay
    	ownLocation = new MyLocationOverlay(getApplicationContext(), mapView);
    	ownLocation.enableCompass();
    	ownLocation.enableMyLocation();

    	// now draw markers
		addOverlay();    	
    }
    
    @Override
    public void onDestroy() 
    {
       super.onDestroy();
       
   	   // unregister location updates
       ownLocation.disableMyLocation();

   	// store current zoom level for later
       editor.putInt("ZoomLevel", mapView.getZoomLevel());
       editor.commit();       
    }
    
	@Override
	protected boolean isRouteDisplayed() 
	{
	    return false;
	}
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    }
    
    public void onClick(View v) 
    {
    	GeoPoint ownPoint;
    	// dispatch depending on button pressed
    	switch(v.getId())
    	{
    	case R.id.mapview_my_location:
    		ownPoint = ownLocation.getMyLocation();
    		if (ownPoint != null)
    			mapController.animateTo(ownPoint);
            break;
    	case R.id.mapview_last_location:
        	mapController.animateTo(last_recorded_location);				// then centre map at it and use different marker pin!
    		break;
    	}	
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	MenuInflater inflater;
        menu.clear();    		
        inflater = getMenuInflater();
       	inflater.inflate(R.menu.options_map, menu);
      
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    	
        switch (item.getItemId()) 
        {
        case R.id.main_about:
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setTitle(getString(R.string.AIRS_Map))
    			   .setMessage(getString(R.string.MapAbout))
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
        case R.id.map_mapview:
        	mapView.setSatellite(false);
       		return true;
        case R.id.map_satview:
        	mapView.setSatellite(true);
       		return true;
    	case R.id.map_path:
    		if (showTrack == true)
    			showTrack = false;
    		else
    			showTrack = true;
    		// remove overlay
    		mapOverlays.clear();
    		
    		// not first draw anymore
    	    FirstDrawn = false;
    		// add overlay back
   			addOverlay();    	
    		
    		mapView.postInvalidate();
    		break;
        }
        return false;
    }
    
    private void addOverlay()
    {
    	int i, j=0;
		GeoPoint point, last = new GeoPoint(0, 0);
		OverlayItem overlayitem = null;
		Time timeStamp = new Time();
		String [] columns = {"Timestamp", "Value"};
		int t_column, v_column;
		double longitude, latitude;
		double altitude;
		double temp_c, temp_f, humidity;
		String condition, wind;

		String selection = "Timestamp > " + startedTime + " AND Symbol='"+ Symbol +"'";
		
		// issue query to the database
		if (values ==null)
		{
//			synchronized(airs_storage)
			{
				values = airs_storage.query("airs_values", columns, selection, null, null, null, "Timestamp DESC", String.valueOf(history_length));
			}
		}   	
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
    		for (i=number_values-1;i>-1;i--)
    		{
    			// get timestamp
    			time[i] = values.getLong(t_column);
    			// get value
    			history[i] = values.getString(v_column);
    			if (history[i] == null)
    				finish();
    			
    			// now move to next row
    			values.moveToNext();
    		}
    		
    		values.close();
    		values = null;
		
	    	// now draw markers
	    	for (i=0;i<number_values;i++)
	       	{
    			if (time[i]>startedTime)
    			{
		    		// get timestamp for time measured
		    		timeStamp.set(time[i]);
		
		    		// create geo point
	    			String [] tokens = history[i].split(":");
	    			
	        		if (Symbol.compareTo("GI") == 0)
	        		{
		    			// read tokens in values
		    			longitude = Double.parseDouble(tokens[0].trim());
		    			latitude = Double.parseDouble(tokens[1].trim());
		    			altitude = Double.parseDouble(tokens[2].trim());
		    			point = new GeoPoint((int)(latitude * 1e6), (int)(longitude *1e6));
			    		if (FirstDrawn == true)
				        	overlayitem = new OverlayItem(point, timeStamp.format("%H:%M:%S on %d.%m.%Y"), "Altitude: " + String.valueOf((int)altitude) + " m");
	        		}
	        		else
	        		{
		    			latitude = Double.parseDouble(tokens[0].trim());
		    			longitude = Double.parseDouble(tokens[1].trim());
		    			temp_c = Double.parseDouble(tokens[2].trim());
		    			temp_f = Double.parseDouble(tokens[3].trim());
		    			humidity = Double.parseDouble(tokens[4].trim());
		    			condition = tokens[5];
		    			wind = tokens[6];
			    		point = new GeoPoint((int)(latitude * 1e6), (int)(longitude *1e6));
			    		if (FirstDrawn == true)
				        	overlayitem = new OverlayItem(point, timeStamp.format("%H:%M:%S on %d.%m.%Y"), "Conditions:\nTemperature: " + String.valueOf(temp_c) + " C (" + String.valueOf(temp_f) + " F)\nHumidity: " + String.valueOf(humidity) + "%\nConditions: " + condition + "\nWind: " + wind);
	        		}
		    		
		    		if (j == 0)
		    			last = point;
		    		
		    		// count the actually shown points
		    		j++;
		        	
		        	// is this the last element?
		        	if (i==number_values-1)
		        	{
		        		last_recorded_location = point;				// save point for later in case button is pressed
			    		if (FirstDrawn == true)
			    			mapController.animateTo(point);				// then centre map at it and use different marker pin!
			    		if (FirstDrawn == true)
			    			itemizedOverlay.addOverlay(overlayitem, this.getResources().getDrawable(R.drawable.current_pin));
		        	}
		        	else // add overlay item to list with default marker   
			    		if (FirstDrawn == true)
			    			itemizedOverlay.addOverlay(overlayitem);
		        	
		        	// shall I add the track?
		        	if (showTrack == true)
		        	{
		        		mapOverlays.add(new MapViewerOverlayTrack(last, point));
		        		last = point;
		        	}
		        	
			        // dereference for garbage collector
			       	point = null;
			       	overlayitem = null;
    			}
	    	} 
	    	
	    	// now add overlay to picture
	    	mapOverlays.add(itemizedOverlay);   
	    	
	    	// add own location
	    	mapOverlays.add(ownLocation);
    	} 
    	
    	// finish if there's no value to be shown
    	if (j==0)
    		finish();
    }
}
