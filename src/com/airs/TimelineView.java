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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class TimelineView extends View
{
    private Paint paint = new Paint();
    private Path path = new Path();
    private int width = -1, height = -1;
    private float scalingY, scalingX;
    private int  max;
    private long minTime;
    private boolean sizes = false;
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

    public TimelineView(Context context) 
    {
        super(context);
        paint.setColor(Color.GREEN);
        paint.setStyle(Style.STROKE);   
        paint.setStrokeWidth(2);
    }

    public TimelineView(Context context,  AttributeSet attrs) 
    {
        super(context, attrs);
        paint.setColor(Color.GREEN);
        paint.setStyle(Style.STROKE);   
        paint.setStrokeWidth(2);
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld)
    {
            super.onSizeChanged(xNew, yNew, xOld, yOld);

            width = xNew;
            height = yNew;
    }
    
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
    }
    
    public void pushPath(long time, int voltage)
    {    
    	float x = (float)(time - minTime) * scalingX;
    	    	
    	if (time==minTime)
    		path.moveTo((float)x, (float)(max-voltage)*scalingY);
    	else
    		path.lineTo((float)x, (float)(max-voltage)*scalingY);
    }
    
    public void setMinMax(int min, int max, long minX, long maxX)
    {
    	this.max = max;
    	minTime = minX;
    	
    	scalingX = ((float)width)/(float)(maxX - minX);
    	scalingY = ((float)height)/(float)(max - min);
    }
}

