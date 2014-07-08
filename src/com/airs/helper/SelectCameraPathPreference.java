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
*/
package com.airs.helper;

import java.io.File;

import com.airs.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.widget.Toast;

public class SelectCameraPathPreference extends Activity
{
	// activity result code
	private static final int SELECT_PHOTO  = 100;
	
	private SharedPreferences settings;


    /** Called when the activity is first created. 
     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
     */
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        // get settings
        settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        
        // start picture selector
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, SELECT_PHOTO);      
		
		// show toast for user
  		Toast.makeText(getApplicationContext(), getString(R.string.Camera_path_select), Toast.LENGTH_LONG).show();
    }
    
    /** Called when the configuration of the activity has changed.
     * @param newConfig new configuration after change 
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) 
    {
    	    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

    	    switch(requestCode) 
    	    { 
    	    case SELECT_PHOTO:
    	        if(resultCode == RESULT_OK)
    	        {  
    	            Uri selectedImage = imageReturnedIntent.getData();
    	            String[] filePathColumn = {MediaStore.Images.Media.DATA};

    	            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
    	            cursor.moveToFirst();

    	            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
    	            String filePath = cursor.getString(columnIndex);
    	            cursor.close();

    	            if (filePath != null)
    	            {
	    	            // get path of image now
	    	            File file = new File(filePath);
	    	            String path = file.getParent();
	    	            
	    	      		Editor edit = settings.edit();
	    	      		edit.putString("MediaWatcherHandler::CameraDirectory", path);
    	            }
    	            else
    	            	Toast.makeText(getApplicationContext(), getString(R.string.Camera_path_select), Toast.LENGTH_LONG).show();

    	      		// now finish the activity
    	      		finish();
    	        }
    	        break;
    	    }
    }
}