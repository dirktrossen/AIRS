package com.airs.helper;

import java.util.Arrays;
import java.util.List;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.airs.R;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

public class ConnectGoogleAccount extends Activity
{
	static private final int REQUEST_ACCOUNT_PICKER = 1;
	static private final int REQUEST_AUTHORIZATION = 2;
	
	// the actual credential token
	private GoogleAccountCredential credential;  
    // preferences
    private SharedPreferences settings;

	/** Called when the activity is first created. 
     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
     */
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);        

        // set content of View
        setContentView(R.layout.googleaccounts);

        try
        {
		    credential = GoogleAccountCredential.usingOAuth2(this.getApplicationContext(), Arrays.asList(DriveScopes.DRIVE));
		    startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);	
        }
        catch(Exception e)
        {
	  		Toast.makeText(getApplicationContext(), getString(R.string.ConnectGoogle4), Toast.LENGTH_LONG).show();
        	finish();
        }
    }
	
	/** Called when the activity is resumed. 
     */
	@Override
    public synchronized void onResume() 
    {
        super.onResume();        
    }

	/** Called when the activity is paused. 
     */
	@Override
    public synchronized void onPause() 
    {
        super.onPause();
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
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
//		super.onActivityResult(requestCode, resultCode, data); 

		switch (requestCode) 
	    {
	    case REQUEST_ACCOUNT_PICKER:
	      if (resultCode == RESULT_OK && data != null && data.getExtras() != null)
	      {
	        String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
	        if (accountName != null) 
	        {
	    	    credential.setSelectedAccountName(accountName);

	    	    // create upload directory here for continuing with the authentication
	        	createDirectory();

	            // get default preferences
	            settings = PreferenceManager.getDefaultSharedPreferences(this);
        	    // clear persistent flag
	           	Editor editor = settings.edit();
	           	editor.putString("AIRS_local::accountname", accountName);
                // finally commit to storing values!!
                editor.commit();
	        }
	      }
	      break;
	    case REQUEST_AUTHORIZATION:
	        if (resultCode == Activity.RESULT_OK) 
	        	createDirectory();
	        else 
	        	startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
	        break;
	    }  
	} 
	
	private void createDirectory()
	{
	    new Thread(new Runnable() 
	    {
	        public void run() 
	        {
	        	boolean running = true;
	    	 	com.google.api.services.drive.model.File AIRS_dir = null;
	    	 	com.google.api.services.drive.model.File body;
	        	
	        	while(running == true)
	        	{
		    	    Drive service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
	                .build();
	                try
	                {
	                	Log.e("AIRS", "trying to find AIRS recordings directory");
	                	
	                	List<com.google.api.services.drive.model.File> files = service.files().list().setQ("mimeType = 'application/vnd.google-apps.folder'").execute().getItems();
	                    for (com.google.api.services.drive.model.File f : files) 
	                    {
	                    	if (f.getTitle().compareTo("AIRS recordings") == 0)
	                    		AIRS_dir = f;
	                    }
	                    
	                    if (AIRS_dir == null)
	                    {
		                	Log.e("AIRS", "...need to create AIRS recordings directory");

		                    // create AIRS recordings directory
		                    body = new com.google.api.services.drive.model.File();
		                    body.setTitle("AIRS recordings");
		                    body.setMimeType("application/vnd.google-apps.folder");
		                    AIRS_dir = service.files().insert(body).execute();
			                com.google.api.services.drive.model.File folder = service.files().insert(body).execute();
			                if (AIRS_dir != null)
			                	Log.e("AIRS", "Created folder with id = " + folder.getId());	
	                    }

	                    running = false;
	                }
	                catch (UserRecoverableAuthIOException e) 
	                {
		                Log.e("AIRS", "Require authorization - starting activity!");
	                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
	                }
	                catch(Exception e)
	                {
	                	Log.e("AIRS", "something went wrong with folder creation: " + e.toString());
	                }
	        	}
	        	
                finish();
	        }
	    }).start();  
	}
}
