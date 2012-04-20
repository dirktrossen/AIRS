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
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class MapViewerActivity extends MapActivity
{
    private TextView		mTitle;
    public  TextView 		mTitle2;
	private MapView mapView;
    private Bundle bundle;
    private int history_x[];			// different history arrays
    private int history_y[];			// different history arrays
	private long time[];
	private int  number_values;
	private List<Overlay> mapOverlays;
	private Drawable drawable;
	private MapViewerOverlay itemizedOverlay;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
   		String title;
		Intent intent = getIntent();
		int i, marker;
		int latitude, longitude;
		GeoPoint point;
		OverlayItem overlayitem;
		
        // Set up the window layout
        super.onCreate(savedInstanceState);

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
       
        title = bundle.getString("com.airs.Title");
        if (title != null)
        	mTitle2.setText(title);
        else
        	mTitle2.setText("Title");
        
        // add zoom
        mapView = (MapView) findViewById(R.id.mapview);
    	mapView.setBuiltInZoomControls(true);
    	
    	// create overlays
    	mapOverlays = mapView.getOverlays();
    	drawable = this.getResources().getDrawable(R.drawable.pin);
    	itemizedOverlay = new MapViewerOverlay(drawable);
    	
    	// add points
    	history_x = bundle.getIntArray("com.airs.CoordX");		// get sensor values
    	history_y = bundle.getIntArray("com.airs.CoordY");		// get sensor values
    	
    	// now draw markers
    	for (i=0;i<number_values;i++)
       	{
    		// create geo point
    		point = new GeoPoint(history_x[i], history_y[i]);
        	overlayitem = new OverlayItem(point, "", "");
        	itemizedOverlay.addOverlay(overlayitem);
	        	
	        	// dereference for garbage collector
	//        	point = null;
	//        	overlayitem = null;
    	}
    	
    	// now add overlay to picture
    	mapOverlays.add(itemizedOverlay);
    }

	@Override
	protected boolean isRouteDisplayed() 
	{
	    return false;
	}
	
}