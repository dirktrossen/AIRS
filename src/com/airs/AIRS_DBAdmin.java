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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

public class AIRS_DBAdmin extends Activity implements OnClickListener
{
	// Layout Views
    private ImageButton db_backup;
    private ImageButton db_restore;
    private ImageButton db_index;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        // Set up the window layout
        super.onCreate(savedInstanceState);
        
        // set content of View
        setContentView(R.layout.db_admin);

        // get buttons and set onclick listener
        db_backup 		= (ImageButton)findViewById(R.id.db_backup);
        db_backup.setOnClickListener(this);
        db_restore 		= (ImageButton)findViewById(R.id.db_restore);
        db_restore.setOnClickListener(this);
        db_index 		= (ImageButton)findViewById(R.id.db_index);
        db_index.setOnClickListener(this);             
    }
    
    @Override
    public synchronized void onPause() 
    {
        super.onPause();
    }

    @Override
    public void onStop() 
    {
        super.onStop();
    }

    @Override
    public void onDestroy() 
    {
       super.onDestroy();
       
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
    	super.onConfigurationChanged(newConfig);
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
        switch (item.getItemId()) 
        {
        case R.id.main_about:
        	// call about dialogue
    		Toast.makeText(getApplicationContext(), R.string.DBadminAbout, Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }


    @Override
    public void onClick(View v) 
    {
    	final Intent intent;
    	
        switch (v.getId()) 
        {
        case R.id.db_backup:
        	intent = new Intent(this,AIRS_backup.class);
        	startActivity(intent);
        	break;
        case R.id.db_restore:
        	intent = new Intent(this,AIRS_restore.class);
        	startActivity(intent);
        	break;
        case R.id.db_index:
           	intent = new Intent(this ,AIRS_index.class);
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    		builder.setMessage(getString(R.string.Index_DB_message))
    			   .setTitle(getString(R.string.Index_DB))
    		       .setCancelable(false)
    		       .setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() 
    		       {
    		           public void onClick(DialogInterface dialog, int id) 
    		           {
	    		        	startActivity(intent);
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
        	break;
        }
    } 
}

