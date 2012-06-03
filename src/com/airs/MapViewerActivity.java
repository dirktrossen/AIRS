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

import com.airs.helper.SerialPortLogger;
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
import android.widget.Toast;

public class MapViewerActivity extends MapActivity implements OnClickListener
{
    private TextView		mTitle;
    public  TextView 		mTitle2;
	private MapView mapView;
    private Bundle bundle;
    private int history_x[];			// different history arrays
    private int history_y[];			// different history arrays
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

	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
   		String title;
		Intent intent = getIntent();
		int i;
		GeoPoint point;
		OverlayItem overlayitem;
		Time timeStamp = new Time();

        // Set up the window layout
        super.onCreate(savedInstanceState);

        // get preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);
       	editor = settings.edit();

       	// get previous zoom level
       	zoomLevel = settings.getInt("ZoomLevel", 15);
        
        // get activity parameters
        bundle = intent.getExtras();

        number_values = bundle.getInt("com.airs.Length");	// get number of values
        if (number_values == 0)
        	finish();
        
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
    	
    	// get points
    	history_x = bundle.getIntArray("com.airs.CoordX");		// get sensor values
    	history_y = bundle.getIntArray("com.airs.CoordY");		// get sensor values
    	
    	// get timestamps
    	time = bundle.getLongArray("com.airs.Time");		// get time values

    	// now draw markers
    	for (i=0;i<number_values;i++)
       	{
    		// get timestamp for time measured
    		timeStamp.set(time[i]);

    		// create geo point
    		point = new GeoPoint(history_x[i], history_y[i]);
        	overlayitem = new OverlayItem(point, timeStamp.format("%H:%M:%S on %d.%m.%Y"), "");
        	
        	// is this the last element?
        	if (i==number_values-1)
        	{
        		last_recorded_location = point;				// save point for later in case button is pressed
            	mapController.animateTo(point);				// then centre map at it and use different marker pin!
        		itemizedOverlay.addOverlay(overlayitem, this.getResources().getDrawable(R.drawable.current_pin));
        	}
        	else // add overlay item to list with default marker        	        	
        		itemizedOverlay.addOverlay(overlayitem);
        	
	        // dereference for garbage collector
	       	point = null;
	       	overlayitem = null;
    	}
    	
    	// now add overlay to picture
    	mapOverlays.add(itemizedOverlay);    
    	
    	// now add own location overlay
    	ownLocation = new MyLocationOverlay(getApplicationContext(), mapView);
    	ownLocation.enableCompass();
    	ownLocation.enableMyLocation();
    	mapOverlays.add(ownLocation);
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
    		builder.setTitle("AIRS Map View")
    			   .setMessage(getString(R.string.MapAbout))
    			   .setIcon(R.drawable.about)
    		       .setNeutralButton("OK", new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
    		                dialog.cancel();
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
        }
        return false;
    }
}
