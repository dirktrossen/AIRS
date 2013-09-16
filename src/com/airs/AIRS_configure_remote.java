/*
Copyright (C) 2013, TecVis LP, support@tecvis.co.uk

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
*/package com.airs;

import java.io.File;
import java.io.FileOutputStream;

import com.airs.helper.SafeCopyPreferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/**
 * Activity to remotely configure AIRS. The parameters for the configuration are sent through
 * an Android Intent with the paramters
 * @param com.airs.template.name name of the template shortcut to be created
 * @param com.airs.template.text text of the template file to be created and used in the shortcut
 */
public class AIRS_configure_remote extends Activity
{
     // template to be set
	 private String template, template_text;

	    /** Called when the activity is first created. 
	     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
	     */
	   @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {
		    String dirPath;
	        File shortcutFile;

	        // Set up the window layout
	        super.onCreate(savedInstanceState);
	        		    
	        // get activity parameters
	        Bundle bundle = getIntent().getExtras();
	        template = bundle.getString("com.airs.template.name");
	        template_text = bundle.getString("com.airs.template.text");
	               
	        // now create shortcut to new template
			File external_storage = getExternalFilesDir(null);
     	   
			if (external_storage != null)
			{
	    		// get current template file
	        	dirPath = external_storage.getAbsolutePath() + "/" + "templates";
            	File shortcutPath = new File(dirPath);
            	if (!shortcutPath.exists())
            		shortcutPath.mkdirs();
	            shortcutFile = new File(dirPath, template);
	            
	            // now write text into file
	            try
	            {
					FileOutputStream dst = new FileOutputStream(shortcutFile);
	                dst.write(template_text.getBytes());
	                dst.close();	                
	            }
                catch (Exception e)
                {
                	Log.e("AIRS", "Cannot write template text into file!");
                	finish();
                }

                // now copy template safely to preferences
                SafeCopyPreferences.copyPreferences(this, shortcutFile);
                
	            // intent for starting AIRS
	    	    Intent intent;
	        	Intent shortcutIntent = new Intent(Intent.ACTION_MAIN); 
	        	shortcutIntent.setClassName(getApplicationContext(), AIRS_shortcut.class.getName()); 
	        	
	        	shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        	shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	
	        	shortcutIntent.putExtra("preferences", shortcutFile.toString());
	
	        	// intent for creating the shortcut
	        	intent = new Intent();
	        	intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
	        	intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, template);
	        	intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.shortcut));
	
	        	intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
	        	sendBroadcast(intent);    
	        	
	           	Toast.makeText(getApplicationContext(), getString(R.string.Configured_airs), Toast.LENGTH_LONG).show();          
			}
			
			// close activity again!
			finish();
	    }

	    /** Called when the activity is restarted. 
	     */
	    @Override
	    public synchronized void onRestart() 
	    {
	        super.onRestart();
	    }

	    /** Called when the activity is stopped. 
	     */
	    @Override
	    public void onStop() 
	    {
	        super.onStop();
	    }

	    /** Called when the activity is destroyed. 
	     */
	    @Override
	    public void onDestroy() 
	    {
	        super.onDestroy();
	    }
}
