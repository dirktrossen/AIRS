package com.airs.database;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.airs.R;
import com.airs.helper.Waker;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ParentReference;

public class AIRS_upload_service extends Service implements MediaHttpUploaderProgressListener
{
	// current batch of recordings for sync
	private static final int SYNC_BATCH		= 5000;
	
    private final IBinder mBinder = new LocalBinder();
    private SharedPreferences settings;
    private Editor editor;
    private NotificationManager mNotificationManager;
    private ConnectivityManager cm; 
    private Notification notification;
    private long synctime, currenttime, new_synctime, currentstart = 0;
    private File sync_file;
    private Uri share_file;
	private File fconn;				// public for sharing file when exiting
	private BufferedOutputStream os = null;
	private boolean at_least_once_written = false;
    private SQLiteDatabase airs_storage;
    private AIRS_database database_helper;
	private GoogleAccountCredential credential;
    private Drive service;
    private Context context;
    private AIRS_upload_service this_service;
    private boolean wifi_only;
    private String currentFilename;
    // GDrive folder
    private String GDrive_Folder;

    public class LocalBinder extends Binder 
    {
    	AIRS_upload_service getService() 
        {
            return AIRS_upload_service.this;
        }
    }
    
    /**
     * Returns current instance of AIRS_upload_service Service to anybody binding to it
     * @param intent Reference to calling {@link android.content.Intent}
     * @return current instance to service
     */
	@Override
	public IBinder onBind(Intent intent) 
	{
		return mBinder;
	}
		
	/**
	 * Called when system is running low on memory
	 * @see android.app.Service
	 */
	@Override
	public void onLowMemory()
	{
	}
	
	/**
	 * Called when starting the service the first time around
	 * @see android.app.Service
	 */
	@Override
	public void onCreate() 
	{
		Log.v("AIRS", "Started upload service");
		
		// save for later
		context = this.getApplicationContext();
		this_service = this;
		
		// get notification manager for later
		mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE); 

        // now get connectivity manager for net type check
    	cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // get default preferences and editor
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        editor = settings.edit();
        
        // get settings for upload preference
        wifi_only = settings.getBoolean("UploadWifi", true);

        // get handle to Google Drive
        service = getDriveService(context);
  
        // get Google drive folder
        GDrive_Folder = settings.getString("GDriveFolder", "AIRS");

        if (service != null)
        {
		    try 
		    {	            
	            // get database
	            database_helper = new AIRS_database(context);
	            airs_storage = database_helper.getWritableDatabase();
	              
		        // start sync thread
		        new SyncThread();
		    } 
		    catch(Exception e)
		    {
		    	Log.e("AIRS", "Cannot open AIRS database for upload!");
	            AIRS_upload.setTimer(context);		// timer will fire again soon!
		    }
        }
	}
	
	/**
	 * Called when service is destroyed, e.g., by stopService()
	 * Here, we tear down all recording threads, close all handlers, unregister receivers for battery signal and close the thread for indicating the recording
	 */
	@Override
	public void onDestroy() 
	{
		Log.v("AIRS", "...destroyed upload service");
	}
	
	private class SyncThread implements Runnable
	{
		SyncThread()
		{
			new Thread(this).start();
		}
		
	     public void run()
	     {
    	 	com.google.api.services.drive.model.File AIRS_dir = null;
    	 	com.google.api.services.drive.model.File body;
    	 	com.google.api.services.drive.model.File file;
    	 	java.io.File fileContent;
    	 	FileContent mediaContent;
    	 	Drive.Files.Insert insert;
    	 	MediaHttpUploader uploader;
    	 	boolean right_network = true, try_upload = true;
    	 	
            // now create the syncfile
            if (createSyncFile(context) == true)
            {	            
                // try to upload until right network is available
                while(try_upload == true)
                {
	                try 
	                {		
                    	right_network = checkRightNetwork();
	        		    
	        		    // only if right network is available, try to upload
	        		    if (right_network == true)
	        		    {		        		    
		                	Log.v("AIRS", "trying to find AIRS recordings directory");

		                	List<com.google.api.services.drive.model.File> files = service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder' AND trashed=false AND 'root' in parents").execute().getItems();
		                    for (com.google.api.services.drive.model.File f : files) 
		                    {
		                    	if (f.getTitle().compareTo(GDrive_Folder) == 0)
		                    		AIRS_dir = f;
		                    }
		                    
		                    if (AIRS_dir == null)
		                    {
			                	Log.v("AIRS", "...need to create AIRS recordings directory");

			                    // create AIRS recordings directory
			                    body = new com.google.api.services.drive.model.File();
			                    body.setTitle(GDrive_Folder);
			                    body.setMimeType("application/vnd.google-apps.folder");
			                    AIRS_dir = service.files().insert(body).execute();
		                    }
		                    
		                    // File's binary content
		                    fileContent = new java.io.File(share_file.getPath());
		                    mediaContent = new FileContent("text/plain", fileContent);
		                    
		                    // File's metadata
		                    body = new com.google.api.services.drive.model.File();
		                    body.setTitle(fileContent.getName());
		                    body.setMimeType("text/plain");
		                    body.setParents(Arrays.asList(new ParentReference().setId(AIRS_dir.getId())));
		                    
		                    Log.v("AIRS", "...trying to upload AIRS recordings");
	
		                    // now get the uploader handle and set resumable upload
		                    insert = service.files().insert(body, mediaContent);
		                    uploader = insert.getMediaHttpUploader();
		                    uploader.setDirectUploadEnabled(false);
		                    uploader.setChunkSize(MediaHttpUploader.DEFAULT_CHUNK_SIZE);
		                    uploader.setProgressListener(this_service);
		                	Log.v("AIRS", "...executing upload AIRS recordings");
	
		                	do
		                	{
		                    	right_network = checkRightNetwork();
			        		    
			        		    // only if right network is available, try to upload
			        		    if (right_network == true)
			        		    {		        		    
			                    // now execute the upload
				                    file = insert.execute();
				                    if (file != null) 
				                    {
					                	Log.v("AIRS", "...writing new sync timestamp");
							    		// write the time until read for later syncs
										// put sync timestamp into store
							       		editor.putLong("SyncTimestamp", new_synctime);
							            
							            // finally commit to storing values!!
							            editor.commit();
							                        
							            // remove temp files
							            sync_file.delete();	
							            
							            // now finish this loop since we are done!
							            try_upload = false;
				                    }  
			        		    }
			        		    else
			        		    {
			        		    	Log.v("AIRS", "...sleeping until right network becomes available");
			        		    	Waker.sleep(15000);			        		    	
			        		    }
		                	}while(right_network == false);
	        		    }
	        		    else
	        		    {
	        		    	Log.v("AIRS", "...sleeping until right network becomes available");
	        		    	Waker.sleep(15000);
	        		    }
                    }
	                catch (Exception e) 		                  
	                {
	                    Log.e("AIRS", "something went wrong in uploading the sync data: " + e.toString());	                    
	                }
                } 
            }
            else
            {
            	Log.v("AIRS", "...nothing to sync, it seems");
            	Log.v("AIRS", "...writing new sync timestamp");
	    		// write the time until read for later syncs
				// put sync timestamp into store
	       		editor.putLong("SyncTimestamp", new_synctime);
	            
	            // finally commit to storing values!!
	            editor.commit();
            }
            
            // set timer again
            AIRS_upload.setTimer(context);
            
            // now stop the overall service all together!
            stopSelf();
	     }
	}
    
	private boolean checkRightNetwork()
	{
		boolean right_network = true;
	 	NetworkInfo netInfo;

		// check network connectivity
	    netInfo = cm.getActiveNetworkInfo();
	    // any network available?
	    if (netInfo != null)
	    {
	    	// is it the right network (in case wifi only is enabled)?
	    	if (netInfo.getType() != ConnectivityManager.TYPE_WIFI && wifi_only == true) 
	    		right_network = false;
	    }
	    else
	    	right_network = false;	// no network available anyways
	    
	    return right_network;
	}
	
    private Drive getDriveService(Context context) 
    {
    	String accountName = settings.getString("AIRS_local::accountname", "");
    	Log.v("AIRS", "account: " + accountName);
    	
	    credential = GoogleAccountCredential.usingOAuth2(context, Arrays.asList(DriveScopes.DRIVE));
	    credential.setSelectedAccountName(accountName);
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
            .build();
    }

    public void progressChanged(MediaHttpUploader uploader)
    {
    	long vibration[] = {0,200,0};
    	int progress;
    	
        switch (uploader.getUploadState()) 
        {
          case MEDIA_IN_PROGRESS:
       		 notification = new Notification(R.drawable.notification_icon, context.getString(R.string.Sync_uploading), System.currentTimeMillis());
      		
      		 try
      		 {
      			 progress = (int)(uploader.getProgress() * 100.0f);
      		 }
      		 catch(Exception e)
      		 {
      			 progress = 0;
      		 }
      		 notification.setLatestEventInfo(context, context.getString(R.string.Sync_uploading), context.getString(R.string.Sync_progress) + String.valueOf(progress) + "%", null);
      		 // set the time again for ICS
      		 notification.when = System.currentTimeMillis();
      		 // don't allow clearing the notification
      		 notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
        	 notification.vibrate			 = vibration;

      		 mNotificationManager.notify(9999, notification);
             break;
          case MEDIA_COMPLETE:
    		 notification = new Notification(R.drawable.notification_icon, context.getString(R.string.Sync_upload), System.currentTimeMillis());
   		
      		 // create pending intent for starting the activity
      		 notification.setLatestEventInfo(context, context.getString(R.string.Sync_upload), "", null);
      		 // set the time again for ICS
      		 notification.when = System.currentTimeMillis();
      		 // don't allow clearing the notification
      		 notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
        	 notification.vibrate			 = vibration;

      		 mNotificationManager.notify(9999, notification);
      		 break;
  		  default:
  			 break;
        }
      }
    
    private boolean createSyncFile(Context context)
    {
    	if (createValueFile(context) == true)
    		return createNoteFile(context);
    	
    	return false;
    }
    
    private boolean createValueFile(Context context)
    {
    	String query;
		int t_column, s_column, v_column;
		Cursor values;
		String value, symbol;
		String line_to_write;
		byte[] writebyte;
		int number_values;
		int i;
		long currentmilli;
		Calendar cal = Calendar.getInstance();
		boolean set_timestamp = true;
		boolean syncing = true;

        // get timestamp of last sync
        synctime = settings.getLong("SyncTimestamp", 0);
        currenttime = synctime;
        
        // sync until just about now!
        new_synctime = System.currentTimeMillis();
        
        Log.v("AIRS", "start creating values sync file!");
        
        // path for templates
        File external_storage = context.getExternalFilesDir(null);
       
        if (external_storage == null)
  			return false;

   		sync_file = new File(external_storage, "AIRS_temp");

		// get files in directory
		String [] file_list = sync_file.list(null);
		
		// remove files in AIRS_temp directory
		if (file_list != null)
			for (i=0;i<file_list.length;i++)
			{
				File remove = new File(sync_file, file_list[i]);
				remove.delete();
			}

		try
		{
	    	// use current milliseconds for filename
	    	currentmilli = System.currentTimeMillis();
	    	// open file in public directory
	    	sync_file = new File(external_storage, "AIRS_temp");
	    	// make sure that path exists
	    	sync_file.mkdirs();
	    	// open file and create, if necessary
	    	currentFilename = new String(String.valueOf(currentmilli) + ".txt");
	    	fconn = new File(sync_file, currentFilename);
			os = new BufferedOutputStream(new FileOutputStream(fconn, true));
	    	
			// build URI for sharing
			share_file = Uri.fromFile(fconn);	

	    	// set timestamp when we will have found the first timestamp
	    	set_timestamp = true;
	    				    	
			while (syncing == true)
			{
				query = new String("SELECT Timestamp, Symbol, Value from 'airs_values' WHERE Timestamp > " + String.valueOf(currenttime) + " AND TimeStamp < " + String.valueOf(new_synctime) + " LIMIT " + String.valueOf(SYNC_BATCH));
				values = airs_storage.rawQuery(query, null);
				
				// garbage collect
				query = null;
				
				if (values == null)
				{
		    		if (at_least_once_written == true)
		    			return true;
		    		else
		    		{
		    			os.close();
		    			return false;
		    		}
				}

				// get number of rows
				number_values = values.getCount();

				// if nothing is read (anymore)
				if (number_values == 0)
				{
		    		if (at_least_once_written == true)
		    			return true;
		    		else
		    		{
		    			os.close();
		    			return false;
		    		}
				}

				// get column index for timestamp and value
				t_column = values.getColumnIndex("Timestamp");
				s_column = values.getColumnIndex("Symbol");
				v_column = values.getColumnIndex("Value");
				
				if (t_column == -1 || v_column == -1 || s_column == -1)
				{
		    		if (at_least_once_written == true)
		    			return true;
		    		else
		    		{
		    			os.close();
		    			return false;
		    		}
				}
					
		        Log.v("AIRS", "...reading next batch!");

				// move to first row to start
	    		values.moveToFirst();
	    		// read DB values into arrays
	    		for (i=0;i<number_values;i++)
	    		{
	    			// get timestamp
	    			currenttime = values.getLong(t_column);

	    			if (set_timestamp == true)
	    			{
				    	// set 
				    	cal.setTimeInMillis(currenttime);
			    		// store timestamp
				    	// force a date format to address Android 4.3 changes that changed zzz to 'BST' and similar
			    		DateFormat sdf = new SimpleDateFormat ("EEE MMM dd HH:mm:ss ZZZZ yyyy", Locale.getDefault());
			    		String time = new String(sdf.format(cal.getTime()) + "\n");
		    			os.write(time.getBytes(), 0, time.length());				    			
		    			// save for later
		    			currentstart = currenttime;
			    		// don't set timestamp anymore later
			    		set_timestamp = false;
			    		at_least_once_written = true;
	    			}

	    			// get symbol
	    			symbol = values.getString(s_column);
	    			// get value
	    			value = values.getString(v_column);

	    			// add empty string as space
	    			if (value.compareTo("") == 0)
	    				value = " ";
	    			
	    			// create line to write to file
	    			line_to_write = new String("#" + String.valueOf(currenttime-currentstart) + ";" + symbol + ";" + value + "\n");

	    			// now write to file
		    		writebyte = line_to_write.getBytes();

	    			os.write(writebyte, 0, writebyte.length);
	    			
	    			// garbage collect the output data
	    			writebyte = null;
	    			line_to_write = null;
	    			
	    			// now move to next row
	    			values.moveToNext();
	    		}	
	    				        
		        // close values to free up memory
		        values.close();
			}
		}
		catch(Exception e)
		{
			try
			{
				if (os != null)
					os.close();
			}
			catch(Exception ex)
			{
			}
			// signal end of synchronization
		}	
		
		// now return
		if (at_least_once_written == true)
			return true;
		else
			return false;
    }
    
    private boolean createNoteFile(Context context)
    {
    	String query;
		int y_column, m_column, d_column, a_column, c_column, mo_column;
		Cursor values;
		String value, symbol;
		String line_to_write;
		byte[] writebyte;
		int number_values;
		int i;
		boolean syncing = true;

        // get timestamp of last sync
        synctime = settings.getLong("SyncTimestamp", 0);
        currenttime = synctime;
               
        Log.v("AIRS", "start creating sync notes file!");
        
		try
		{
			// now sync the notes, if any
			syncing = true;
			while (syncing == true)
			{
				query = new String("SELECT Year, Month, Day, Annotation, created, modified from 'airs_annotations' WHERE created > " + String.valueOf(currenttime) + " AND created < " + String.valueOf(new_synctime) + " LIMIT " + String.valueOf(SYNC_BATCH));
				values = airs_storage.rawQuery(query, null);
				
				// garbage collect
				query = null;
				
				if (values == null)
				{
			        // purge file
			        os.close();

		    		if (at_least_once_written == true)
		    			return true;
		    		else
		    			return false;
				}

				// get number of rows
				number_values = values.getCount();

				// if nothing is read (anymore)
				if (number_values == 0)
				{
			        // purge file
			        os.close();

		    		if (at_least_once_written == true)
		    			return true;
		    		else
		    			return false;
				}

				// get column index for timestamp and value
				y_column = values.getColumnIndex("Year");
				m_column = values.getColumnIndex("Month");
				d_column = values.getColumnIndex("Day");
				a_column = values.getColumnIndex("Annotation");
				c_column = values.getColumnIndex("created");
				mo_column = values.getColumnIndex("modified");
				
				if (y_column == -1 || m_column == -1 || d_column == -1 || a_column == -1 || c_column == -1 || mo_column == -1)
				{
			        // purge file
			        os.close();

		    		if (at_least_once_written == true)
		    			return true;
		    		else
		    			return false;
				}
					
		        Log.v("AIRS", "...reading next batch!");

				// move to first row to start
	    		values.moveToFirst();
	    		// read DB values into arrays
	    		for (i=0;i<number_values;i++)
	    		{
	    			// get timestamp
	    			currenttime = values.getLong(c_column);

	    			// set symbol
	    			symbol = "UN";
	    			// create value as concatenation of year:month:day:modified:annotation
	    			value = String.valueOf(values.getInt(y_column)) + ":" + String.valueOf(values.getInt(m_column)) + ":" + String.valueOf(values.getInt(d_column)) + ":" + String.valueOf(values.getLong(mo_column)) + ":" + values.getString(a_column);

	    			// create line to write to file
	    			line_to_write = new String("#" + String.valueOf(currenttime-currentstart) + ";" + symbol + ";" + value + "\n");

	    			// now write to file
		    		writebyte = line_to_write.getBytes();

	    			os.write(writebyte, 0, writebyte.length);
	    			
	    			// garbage collect the output data
	    			writebyte = null;
	    			line_to_write = null;
	    			
	    			// now move to next row
	    			values.moveToNext();
	    		}	
	    				        
		        // close values to free up memory
		        values.close();
			}
			
			// close output file
			os.close();					
		}
		catch(Exception e)
		{
			try
			{
				if (os != null)
					os.close();
			}
			catch(Exception ex)
			{
			}
			// signal end of synchronization
		}	
		
		// now return
		if (at_least_once_written == true)
			return true;
		else
			return false;
    }
}
