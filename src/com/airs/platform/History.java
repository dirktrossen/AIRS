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
package com.airs.platform;

import com.airs.MapViewerActivity;
import com.airs.TimelineActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class History 
{
	// types for values
	public static final int TYPE_INT 		= 1;
	public static final int TYPE_COORD 		= 4;

    private int history_length;			// length for history
    private int history_type;			// type of values
    private int history_int[];			// different history arrays
    private int history_x[];			// different history arrays
    private int history_y[];			// different history arrays
    private int copy_x[];
    private int copy_y[];
    private long history_time[];
    private boolean rolled_over = false;
    
    private int history_current = 0;	// index of current entry

    public History(int type)
    {
    	// how many to store?
		history_length = HandlerManager.readRMS_i("SensorHistory", 20);

		// store type for history
		history_type = type;
		
		// values for timestamps
		history_time = new long[history_length];
		
		// establish history array, depending on type
		switch(history_type)
		{
		case TYPE_INT: 
			history_int = new int[history_length];
			break;
		case TYPE_COORD: 
			history_x = new int[history_length];
			history_y = new int[history_length];
			break;
		}
    }
    
    // push int values into array
    public void push(int value)
    {
    	history_time[history_current] = System.currentTimeMillis();
    	history_int[history_current] = value;
    	
    	history_current++;
    	
    	// if current index exceeds capacity, start from beginning
    	if (history_current == history_length)
    	{
    		rolled_over = true;		// rolled over at least once
    		history_current = 0;
    	}
    }
    
    // push int values into array
    public void push(int x, int y)
    {
    	history_time[history_current] = System.currentTimeMillis();
    	history_x[history_current] = x;
    	history_y[history_current] = y;
    	
    	history_current++;
    	
    	// if current index exceeds capacity, start from beginning
    	if (history_current == history_length)
    	{
    		rolled_over = true;		// rolled over at least once
    		history_current = 0;
    	}
    }

    // create copy of timestamps for display
    public long[] copy_time()
    {
    	long copy[] = new long[history_length];
    	int i = 0, current;
    	
    	// start with the entry one after the current one, if we have rolled over at least once
    	if (rolled_over == true)
    	{
	    	current = history_current + 1;
	    	if (current >= history_length)
	    		current = 0;
    	}
    	else
    		current = 0;		// start with first one
    	
    	while (i<history_length)
    	{
    		copy[i] = history_time[current];
    		
    		current++;
        	if (current >= history_length)
        		current = 0;    
        	i++;
    	}
    	
    	return copy;
    }

    // create copy of history for display
    public int[] copy_history_i()
    {
    	int copy[] = new int[history_length];
    	int i = 0, current;
    	
    	// start with the entry one after the current one, if we have rolled over at least once
    	if (rolled_over == true)
    	{
	    	current = history_current + 1;
	    	if (current >= history_length)
	    		current = 0;
    	}
    	else
    		current = 0;		// start with first one
    	
    	while (i<history_length)
    	{
    		copy[i] = history_int[current];
    		
    		current++;
        	if (current >= history_length)
        		current = 0;    
        	i++;
    	} 
    	
    	return copy;
    }
    
    // create copy of history for display
    public void copy_history_coord()
    {
    	int i = 0, current;
    	
    	copy_x = new int[history_length];
    	copy_y = new int[history_length];

    	// start with the entry one after the current one, if we have rolled over at least once
    	if (rolled_over == true)
    	{
	    	current = history_current + 1;
	    	if (current >= history_length)
	    		current = 0;
    	}
    	else
    		current = 0;		// start with first one
    	
    	while (i<history_length)
    	{
    		copy_x[i] = history_x[current];
    		copy_y[i] = history_y[current];
    		
    		current++;
        	if (current >= history_length)
        		current = 0;    
        	i++;
    	}    	
    }


    // start timeline activity
    public void timelineView(Context airs, String title, int scaler)
    {
    	if (rolled_over == false && history_current == 0)
    		return;
    	
    	Intent intent = new Intent(airs.getApplicationContext(), TimelineActivity.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    	// create bundle
    	Bundle bundle = new Bundle();

    	bundle.putString("com.airs.Title", title);
    	
    	// scaler for values
    	bundle.putInt("com.airs.Scaler", scaler);

    	bundle.putInt("com.airs.Type", history_type);

    	bundle.putIntArray("com.airs.Int", copy_history_i());  
    	
    	// provide the timestamps
    	bundle.putLongArray("com.airs.Time", copy_time());
    	
    	// provide the current length of the history
    	if (rolled_over)
    		bundle.putInt("com.airs.Length", history_length);
    	else
    		bundle.putInt("com.airs.Length", history_current);

        intent.putExtras(bundle);

 	   	airs.startActivity(intent);
    }
    
    // start mapview activity
    public void mapView(Context airs, String title)
    {
    	if (rolled_over == false && history_current == 0)
    		return;

    	Intent intent = new Intent(airs.getApplicationContext(), MapViewerActivity.class);
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    	// create bundle
    	Bundle bundle = new Bundle();

    	// place title
    	bundle.putString("com.airs.Title", title);
    	
    	// provide the strings
    	copy_history_coord();
    	bundle.putIntArray("com.airs.CoordX", copy_x);  
    	bundle.putIntArray("com.airs.CoordY", copy_y);  

    	// provide the timestamps
        bundle.putLongArray("com.airs.Time", copy_time());
    	
    	// provide the current length of the history
    	if (rolled_over)
    		bundle.putInt("com.airs.Length", history_length);
    	else
    		bundle.putInt("com.airs.Length", history_current);

        intent.putExtras(bundle);

    	airs.startActivity(intent);    	
    }
}
