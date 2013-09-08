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
package com.airs.visualisations;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

/**
 * Class for the overlay track being shown for the GI/VI sensors
 */
public class MapViewerOverlayTrack extends Overlay 
{
    private GeoPoint gp1;
    private GeoPoint gp2;

    /**
     * Constructor
     * @param gp1 first point to draw from
     * @param gp2 second point to draw to
     */
    public MapViewerOverlayTrack(GeoPoint gp1, GeoPoint gp2) 
    {
        this.gp1 = gp1;
        this.gp2 = gp2;
    }

    /**
     * Draws the overlay track on the canvas
     * @param canvas Reference to the {@link android.graphics.Canvas} to drawn on
     * @param mapView Reference to the {@link com.google.android.maps.MapView} that the track appears on
     * @param shadow draws a shadow (true) or not (false) under the track
     * @param when not used here but required, simply forwarded to super method
     */
    @Override
    public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
            long when) 
    {
        // TODO Auto-generated method stub
        Projection projection = mapView.getProjection();
        
        
        if (shadow == false) 
        {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            Point point = new Point();
            projection.toPixels(gp1, point);
            paint.setColor(Color.BLUE);
            Point point2 = new Point();
            projection.toPixels(gp2, point2);
            // stroke width depends on zoom level
            int zoomLevel = mapView.getZoomLevel();
            if (zoomLevel > 18)
            	paint.setStrokeWidth(6);
            else
            	paint.setStrokeWidth(4);
            // now draw line
            canvas.drawLine((float) point.x, (float) point.y, (float) point2.x,(float) point2.y, paint);
        }
        return super.draw(canvas, mapView, shadow, when);
    }

    /**
     * Draws the overlay track on the canvas
     * @param canvas Reference to the {@link android.graphics.Canvas} to drawn on
     * @param mapView Reference to the {@link com.google.android.maps.MapView} that the track appears on
     * @param shadow draws a shadow (true) or not (false) under the track
     */
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) 
    {
        // TODO Auto-generated method stub

        super.draw(canvas, mapView, shadow);
    }

}