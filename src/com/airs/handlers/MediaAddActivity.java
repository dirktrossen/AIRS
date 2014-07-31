/*
Copyright (C) 2014, TecVis LP, support@tecvis.co.uk

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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.airs.R;
import com.airs.database.AIRS_database;
import com.airs.platform.HandlerManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

/** Activity to add images as MW sensor when being shared to AIRS
 * @see android.app.Activity
 */
public class MediaAddActivity extends Activity 
{
    private AIRS_database database_helper;
    private SQLiteDatabase airs_storage;
    private SharedPreferences settings;
    
	/**
	  * Started when creating the {@link android.app.Activity}
	  * @see android.app.Activity#onCreate(android.os.Bundle)
	  */
	@Override
	public void onCreate (Bundle savedInstanceState) 
	{
		int i;
		boolean inserted = false;
		
        // Set up the window layout
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();

    	Log.v("AIRS", "Started ACTION_SEND/ACTION_SEND_MULTIPLE activity!");

    	// get settings
    	settings = PreferenceManager.getDefaultSharedPreferences(this);

        // get database
        database_helper = new AIRS_database(this.getApplicationContext());
        airs_storage = database_helper.getWritableDatabase();

        if (airs_storage == null)
        	Log.e("AIRS", "Can't open database!");
        else
        {
	        // if image(s) has been sent to AIRS, record it now!
	        if (Intent.ACTION_SEND.equals(action)) 
	        {
	        	Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
	            if (imageUri != null) 
	            	inserted = addImage(imageUri);
	        }
	        if (Intent.ACTION_SEND_MULTIPLE.equals(action)) 
	        {
	            ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
	            if (imageUris != null) 
	            	for (i=0;i<imageUris.size();i++)
	            		inserted = inserted | addImage(imageUris.get(i));
	        }	
	        
	        // has anything been inserted?
	        if (inserted == true)
	        	Toast.makeText(getApplicationContext(), R.string.Camera_added_image, Toast.LENGTH_LONG).show();
        }
        finish();
    }
	
	private boolean addImage(Uri media)
	{
		long timestamp;
		File file;
		String insert;
	
		Cursor returnCursor = getContentResolver().query(media, null, null, null, null);
		
		int dataIndex = returnCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		
	    returnCursor.moveToFirst();
	    file = new File(returnCursor.getString(dataIndex));
	    
		Log.v("AIRS", "trying to insert " + file.getAbsolutePath());
		
		if (file.exists() == true)
		{	
			// try to first get timestamp from EXIF, if not then when last modified
			try
			{
				ExifInterface exifInterface = new ExifInterface(file.toString());
				String datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
				
				SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
				
				if (datetime == null)
					timestamp = file.lastModified();
				else
				{
					try 
					{
			            Date date = sFormatter.parse(datetime);
			            if (date == null) 
							timestamp = file.lastModified();
			            else
			            	timestamp = date.getTime();
					} 
					catch (IllegalArgumentException ex) 
					{
						timestamp = file.lastModified();
					}
				}
			}
			catch(Exception e)
			{
				timestamp = file.lastModified();
			}

			// read camera directory setting
			String camera_directory;
			
			// use system path?
	        if (settings.getBoolean("MediaWatcherHandler::CameraDefault", true) == true)
	        	camera_directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
	        else
	        	camera_directory = settings.getString("MediaWatcherHandler::CameraDirectory", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera");

	        Log.e ("AIRS", "current camera directoy is: " + camera_directory);
	        Log.e ("AIRS", "path of picture to add is: " + file.getParent());
	        // is the path of the picture the camera path?
			if (file.getParent().compareTo(camera_directory) == 0)
			{
				// now check and set maintenance table entries
				check_and_set_Time(timestamp);
	
				// now add sensor to values
				insert = new String("INSERT into airs_values (Timestamp, Symbol, Value) VALUES ('"+ String.valueOf(timestamp) + "','MW','camera:" + file.getName() + "')");
				airs_storage.execSQL(insert);
				
				Log.v("AIRS", "Added image to database");
				
				return true;
			}
			else
	        	Toast.makeText(getApplicationContext(), R.string.Camera_added_image2, Toast.LENGTH_LONG).show();

		}
		else
			Log.e("AIRS", "File " + file.getAbsolutePath() + " does not exist");
		
		return false;
	}
	
	private void check_and_set_Time(long timestamp)
	{
		long beginning_of_day, end_of_day;
		
		 // get current day and set to timestamp
        Calendar cal = Calendar.getInstance();       
        cal.setTimeInMillis(timestamp);
        
        // get date information for later
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        
        // set to end of day
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.MILLISECOND, 999);
        end_of_day = cal.getTimeInMillis();
        
        // set to beginning of day
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 1);
        beginning_of_day = cal.getTimeInMillis();
        
		// is there any MW sensor during the day already?
		String query = new String("SELECT Symbol from 'airs_sensors_used' WHERE Timestamp > " + String.valueOf(beginning_of_day) + " AND Timestamp < " + String.valueOf(end_of_day) + " AND Symbol='MW'");
		Cursor values = airs_storage.rawQuery(query, null);

		// if there's no other MW sensor -> make entry for the day
		if (values.getCount() == 0)
		{
    		try
    		{
    			airs_storage.execSQL("INSERT into airs_sensors_used (Timestamp, Symbol) VALUES ('" + String.valueOf(timestamp) + "','MW')");
    		}
    		catch(Exception e)
    		{
    		}
		}
		
		// and return memory
		values.close();

		// is there any recording on the day at all?
		query = new String("SELECT Types from 'airs_dates' WHERE Year=" + String.valueOf(year) + " AND Month=" + String.valueOf(month) +" AND Day=" + String.valueOf(day));
		values = airs_storage.rawQuery(query, null);

		// if there's no other MW sensor -> make entry for the day
		if (values.getCount() == 0)
		{
	         // mark date now as having values
	         try
	         {
	        	 airs_storage.execSQL("INSERT into airs_dates (Year, Month, Day, Types) VALUES ('"+ String.valueOf(year) +  "','" + String.valueOf(month) + "','" + String.valueOf(day) + "','1')");	
	         }
	         catch(Exception e)
	         {
	        	 airs_storage.execSQL("CREATE TABLE IF NOT EXISTS airs_dates (Year INT, Month INT, Day INT, Types INT);");
	        	 airs_storage.execSQL("INSERT into airs_dates (Year, Month, Day, Types) VALUES ('"+ String.valueOf(year) +  "','" + String.valueOf(month) + "','" + String.valueOf(day) + "','1')");	
	         }
		}
		
		// and return memory
		values.close();
	}
}

