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

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

@SuppressWarnings("deprecation")
public class AIRS_tabs extends TabActivity implements OnTabChangeListener
{
	static public boolean sensors_shown = false;
	private int currentTab = 0;
	private int no_tabs = 0;
	private TabHost tabHost;
	
	public void onCreate(Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	    
        // set layout
        setContentView(R.layout.tabs);

	    Resources res = getResources(); // Resource object to get Drawables
	    tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    // first tab: local sensing
	    intent = new Intent().setClass(this, AIRS_record_tab.class);
	    spec = tabHost.newTabSpec("local").setIndicator(getString(R.string.tab_Record),
	                      res.getDrawable(R.drawable.record))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    no_tabs++;

	    // second tab: sync
	    intent = new Intent().setClass(this, AIRS_sync.class);
	    spec = tabHost.newTabSpec("remote").setIndicator(getString(R.string.tab_Sync),
	                      res.getDrawable(R.drawable.server))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    no_tabs++;
	    
	    // third tab: settings
	    intent = new Intent().setClass(this, AIRS_settings_tab.class);
	    spec = tabHost.newTabSpec("handlers").setIndicator(getString(R.string.tab_Config),
	                      res.getDrawable(R.drawable.general2))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    // store tab number
	    no_tabs++;
	    
	    // fourth tab: storica
		if (Build.VERSION.SDK_INT>=14)
		{
		    intent = new Intent().setClass(this, AIRS_visualisation.class);
		    spec = tabHost.newTabSpec("storica").setIndicator(getString(R.string.tab_Visualise),
		                      res.getDrawable(R.drawable.visualise))
		                  .setContent(intent);
		    tabHost.addTab(spec);
		    // store tab number
		    no_tabs++;
		}

	    // current tab
	    tabHost.setCurrentTab(currentTab);
	    tabHost.setOnTabChangedListener(this);
	}
	
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) 
    {
    	if (event.getAction() == KeyEvent.ACTION_DOWN)
    		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
    			return true;
    	
 		// key de-pressed?
		if (event.getAction() == KeyEvent.ACTION_UP)
		{
			// is it the BACK key?
			if (event.getKeyCode()==KeyEvent.KEYCODE_BACK)
			{
            		AlertDialog.Builder builder = new AlertDialog.Builder(this);
            		builder.setCancelable(false)
            		       .setIcon(R.drawable.icon)
            		       .setTitle(getString(R.string.Exit_AIRS2))
            		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
            		       {
            		           public void onClick(DialogInterface dialog, int id) 
            		           {
            		                finish();
            		           }
            		       })
            		       .setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() 
            		       {
            		           public void onClick(DialogInterface dialog, int id) 
            		           {
            		                dialog.cancel();
            		           }
            		       });
            		AlertDialog alert = builder.create();
            		alert.show();
			}
		}

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    }
    
    public void onTabChanged(String tabId)
    {
           View currentView = getTabHost().getCurrentView();
           if (getTabHost().getCurrentTab() > currentTab)
           {
               currentView.setAnimation( inFromRightAnimation() );
           }
           else
           {
               currentView.setAnimation( outToRightAnimation() );
           }

           currentTab = getTabHost().getCurrentTab();
    }
    
    public Animation inFromRightAnimation()
    {
        Animation inFromRight = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, +1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        inFromRight.setDuration(240);
        inFromRight.setInterpolator(new AccelerateInterpolator());
        return inFromRight;
    }

    public Animation outToRightAnimation()
    {
        Animation outtoLeft = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, -1.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f);
        outtoLeft.setDuration(240);
        outtoLeft.setInterpolator(new AccelerateInterpolator());
        return outtoLeft;
    }
}
