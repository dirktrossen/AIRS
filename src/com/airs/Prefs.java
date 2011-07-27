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
package com.airs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Handles preferences (general and handler-specific ones) for NORS
 */
public class Prefs extends PreferenceActivity 
{
	String AboutTitle, About;
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        // get intent that started this event
        Intent intent = getIntent();
        
        int rid 	= intent.getIntExtra("Resource", 0);
        AboutTitle 	= intent.getStringExtra("AboutTitle");
        About		= intent.getStringExtra("About");
        // add preference resource ID that is included in the intent
        addPreferencesFromResource(rid);
        
        // call current handlerUI's configuration function to adjust preferences
        if (AIRS.current_handler != null)
        	AIRS.current_handler.configurePreference(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	MenuInflater inflater;
        menu.clear();    		
        inflater = getMenuInflater();
        inflater.inflate(R.menu.options_about, menu);
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(AboutTitle)
			   .setMessage(About)
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
		return true;
    }   
}
