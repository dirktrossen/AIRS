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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

public class AIRS_tabs extends TabActivity implements OnTabChangeListener
{
	private int currentTab = 0;
	private int no_tabs = 0;
	private int no_settings_tab, no_sync_tab, no_manual_tab, no_local_tab;
	
    // preferences
    private SharedPreferences settings;
	private boolean showRemote;
	
	public void onCreate(Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	    
        // get default preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        showRemote = settings.getBoolean("showRemote", true);

        // set layout
        setContentView(R.layout.tabs);

	    Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, AIRS_local_tab.class);

	    // first tab: local sensing
	    spec = tabHost.newTabSpec("local").setIndicator("Local",
	                      res.getDrawable(R.drawable.file))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    // store tab number
	    no_local_tab = no_tabs;
	    no_tabs++;

	    // second tab: remote sensing, if it needs to be shown
	    if (showRemote == true)
	    {
		    intent = new Intent().setClass(this, AIRS_remote_tab.class);
		    spec = tabHost.newTabSpec("remote").setIndicator("Remote",
		                      res.getDrawable(R.drawable.server))
		                  .setContent(intent);
		    tabHost.addTab(spec);
		    no_tabs++;
	    }
	    
	    // third tab: settings
	    intent = new Intent().setClass(this, AIRS_settings_tab.class);
	    spec = tabHost.newTabSpec("handlers").setIndicator("Settings",
	                      res.getDrawable(R.drawable.general2))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    // store tab number
	    no_settings_tab = no_tabs;
	    no_tabs++;

	    // fourth tab: sync
	    intent = new Intent().setClass(this, AIRS_sync_tab.class);
	    spec = tabHost.newTabSpec("sync").setIndicator("Sync",
	                      res.getDrawable(R.drawable.sync))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    // store tab number
	    no_sync_tab = no_tabs;
	    no_tabs++;

	    // fifth tab: web view
	    intent = new Intent().setClass(this, AIRS_web_tab.class);
	    spec = tabHost.newTabSpec("manual").setIndicator("Online Manual",
	                      res.getDrawable(R.drawable.manual))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    // store tab number
	    no_manual_tab = no_tabs;
	    no_tabs++;
	    
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
            		builder.setMessage("Are you sure you want to exit?")
            		       .setCancelable(false)
            		       .setIcon(R.drawable.icon)
            		       .setTitle("Exit AIRS")
            		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() 
            		       {
            		           public void onClick(DialogInterface dialog, int id) 
            		           {
            		                finish();
            		           }
            		       })
            		       .setNegativeButton("No", new DialogInterface.OnClickListener() 
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
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	int i;
    	
        switch (item.getItemId()) 
        {
        case R.id.main_about:
        	// call about dialogue
        	if (getTabHost().getCurrentTab() == no_settings_tab)
        		HandlerUIManager.AboutDialog("Settings", getString(R.string.HandlersList));
        	if (getTabHost().getCurrentTab() == no_manual_tab)
        		HandlerUIManager.AboutDialog("Online Manual", getString(R.string.ManualAbout));
            return true; 
        default:
        	return false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	if (getTabHost().getCurrentTab() == no_local_tab || getTabHost().getCurrentTab() == no_sync_tab)
    		return false;
    	{
        	MenuInflater inflater;
            menu.clear();    		
            inflater = getMenuInflater();
            inflater.inflate(R.menu.options_about, menu);
            return true;
    	}

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
