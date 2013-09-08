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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;

/**
 * Class to implement a timeline with two colors and a grid
 * @see TimelineActivity
 */
public class TimelineView extends View
{
    private Paint paint  = new Paint();
    private Paint paint_average = new Paint();
    private Paint paint_grid = new Paint();
    private Path path  = new Path();
    private Path path_average = new Path();
    private Path path_grid = new Path();
    private int width = -1, height = -1;
    private float scalingY, scalingX;
    private float  max;
    private long minTime;
    private boolean sizes = false;
    private ProgressBar progress = null;
    
    /**
     * Constructor, setting the various color and stroke characteristics for the timeline
     * @param context Reference to the calling {@link android.content.Context}
     */
    public TimelineView(Context context) 
    {
        super(context);
        paint.setColor(Color.GREEN);
        paint.setStyle(Style.STROKE);   
        paint.setStrokeWidth(2);
        // now set for average line
        paint_average.setColor(Color.RED);
        paint_average.setStyle(Style.STROKE);
        paint_average.setStrokeWidth(2);
        paint_average.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));
        // now set for grid
        paint_grid.setColor(Color.WHITE);
        paint_grid.setStyle(Style.STROKE);
        paint_grid.setStrokeWidth(1);
        paint_grid.setPathEffect(new DashPathEffect(new float[] {5,10}, 0));
    }

    /**
     * Constructor, setting the various color and stroke characteristics for the timeline
     * @param context Reference to the calling {@link android.content.Context}
     * @param attrs Reference to the {@link android.util.AttributeSet} of the layout drawn for
     */
    public TimelineView(Context context,  AttributeSet attrs) 
    {
        super(context, attrs);
        paint.setColor(Color.GREEN);
        paint.setStyle(Style.STROKE);   
        paint.setStrokeWidth(2);
        // now set for average line
        paint_average.setColor(Color.RED);
        paint_average.setStyle(Style.STROKE);
        paint_average.setStrokeWidth(2);
        paint_average.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));
        // now set for grid
        paint_grid.setColor(Color.WHITE);
        paint_grid.setStyle(Style.STROKE);
        paint_grid.setStrokeWidth(1);
        paint_grid.setPathEffect(new DashPathEffect(new float[] {5,10}, 0));
    }

    /**
     * called when size of the view changed
     * @param xNew new width
     * @param yNew new height
     * @param xOld old width
     * @param yOld old height
     */
    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld)
    {
            super.onSizeChanged(xNew, yNew, xOld, yOld);

            width = xNew;
            height = yNew;
    }
    
    /**
     * Called by the UI thread to draw the timeline view
     * @param canvas Reference to the {@link android.graphics.Canvas} drawn on
     */
    @Override
    public void onDraw(Canvas canvas) 
    {
    	if (sizes == false)
    	{
    		width = getWidth();
    		height = getHeight();
    	}

    	if (path.isEmpty()==false)
    		canvas.drawPath(path, paint);

    	if (path_average.isEmpty()==false)
    		canvas.drawPath(path_average, paint_average);
    	
    	if (path_grid.isEmpty()==false)
    		canvas.drawPath(path_grid, paint_grid);

    	if (progress != null)
    		progress.setVisibility(View.INVISIBLE);
    }
    
    /**
     * Push a timeline point (x=time, y=voltage) to the path, which is then later drawn by the onDraw() function
     * @param time current time of the point
     * @param voltage current y value of the point
     */
    public void pushPath(long time, float voltage)
    {    
    	float x = (float)(time - minTime) * scalingX;
    	    	
    	if (time==minTime)
    	{
    		// more than jsut one value?
    		if (scalingX != 0)
    			path.moveTo((float)x, (float)(max-voltage)*scalingY);
    		else
    		{	// otherwise draw single line
    			path.moveTo(0.0f, (float)(max-voltage)*scalingY);
    			path.lineTo(width, (float)(max-voltage)*scalingY);
    		}
    	}
    	else
    		path.lineTo((float)x, (float)(max-voltage)*scalingY);
    }
    
    /**
     * Pushes a point (y=voltage) of the average line from far left to far right, which is then later drawn by the onDraw() function
     * @param voltage current y value of the point
     */
    public void pushAverage(float voltage)
    {        	    	
		path_average.moveTo((float)0, (float)(max-voltage)*scalingY);
		path_average.lineTo((float)width, (float)(max-voltage)*scalingY);
    }

    /**
     * Pushes the path information for the grid to be drawn
     */
    public void pushGrid()
    {   
    	// first the vertical grid lines
    	path_grid.moveTo((float)0, height/4);
    	path_grid.lineTo((float)width, height/4);
    	path_grid.moveTo((float)0, height/2);
    	path_grid.lineTo((float)width, height/2);
    	path_grid.moveTo((float)0, height/4 * 3);
    	path_grid.lineTo((float)width, height/4 * 3);
		
    	// then the horizontal grid lines
    	path_grid.moveTo((float)width/4, 0);
    	path_grid.lineTo((float)width/4, height);
    	path_grid.moveTo((float)width/2, 0);
    	path_grid.lineTo((float)width/2, height);
    	path_grid.moveTo((float)width/4 * 3, 0);
    	path_grid.lineTo((float)width/4 * 3, height);
    }

    /**
     * Sets the min/max information for the timeline view so that the values are properly scaled when pushPath() is called
     * @param min minimal y value
     * @param max maximum y value
     * @param minX minimal x value
     * @param maxX maximum x value
     */
    public void setMinMax(float min, float max, long minX, long maxX)
    {
    	this.max = max;
    	minTime = minX;
    	
   		scalingX = ((float)width)/(float)(maxX - minX);   			
    	scalingY = ((float)height)/(float)(max - min);
    	
    	// reset paths
    	path.reset();
    	path_average.reset();
    	path_grid.reset();
    }

    /**
     * Store a reference to the {@link android.widget.ProgressBar} that is shown during drawing
     * @param progress
     */
    public void setProgressBar(ProgressBar progress)
    {
    	this.progress = progress;
    }
    
    /**
     * sets the main color of the path
     * @param color color code
     */
    public void setMainColor(int color)
    {
    	paint.setColor(color);
    }
}

