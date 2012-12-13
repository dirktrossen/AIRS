/*
Copyright (C) 2011, Dirk Trossen, airs@dirk-trossen.de

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
package com.airs.handlers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.airs.*;

public class HeartrateButton_widget extends AppWidgetProvider
{
	Context context;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) 
    {
        // For each widget that needs an update, get the text that we should display:
        //   - Create a RemoteViews object for it
        //   - Set the text in the RemoteViews object
        //   - Tell the AppWidgetManager to show that views object for the widget.
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widgetlayout_heart);
        
        // create our own broadcast event for event button pressing -> EventButtonHandler subscribes to this!
        Intent defineIntent = new Intent("com.airs.heartratebutton");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, defineIntent, PendingIntent.FLAG_UPDATE_CURRENT); 
        views.setOnClickPendingIntent(R.id.heart_button_local, pendingIntent);

        // Tell the widget manager
        appWidgetManager.updateAppWidget(appWidgetIds, views);
        
        // save context for later!
        this.context = context;
    }
    
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) 
    {
    }

    @Override
    public void onEnabled(Context context) 
    {
    	// create intent to broadcast?
    }

    @Override
    public void onDisabled(Context context) 
    {
    	// remove intent to broadcast?
    }
}
