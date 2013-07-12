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
package com.airs.handlers;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.airs.*;

/** Activity to self-annotate the blood pressure
 * @see android.app.Activity
 */
public class BloodPressureButton_selector extends Activity implements OnClickListener
{
	 // preferences
	 private SharedPreferences settings;

	 private TextView mTitle;
	 private boolean selected = false;
	 private EditText systolic, diastolic;
	 
	 /**
	  * Started when creating the {@link android.app.Activity}
	  * @see android.app.Activity#onCreate(android.os.Bundle)
	  */
	   @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {	    
	        // Set up the window layout
	        super.onCreate(savedInstanceState);
	           
	        // read preferences
	        settings = PreferenceManager.getDefaultSharedPreferences(this);
	        
			// set window title
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			setContentView(R.layout.bloodpressure);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	        
	        // get window title fields
	        mTitle = (TextView) findViewById(R.id.title_left_text);
	        mTitle.setText(getString(R.string.AIRS_BloodPressure));
        	
        	// hook listeners into buttons
    		Button ibt = (Button) findViewById(R.id.bp_ok);
    		ibt.setOnClickListener(this);
    		systolic = (EditText) findViewById(R.id.bp_systolic);
    		diastolic = (EditText) findViewById(R.id.bp_diastolic);
    		
    		// set text to previous values
    		systolic.setText(settings.getString("BloodPressureButton::systolic", "130"));
    		diastolic.setText(settings.getString("BloodPressureButton::diastolic", "80"));
	    }

	   /** Called when resuming the {@link android.app.Activty}
	    * @see android.app.Activity#onResume()
	    */
	   @Override
	   public void onResume() 
	   {
	        super.onResume();
	   }
	   
	   /**
	    * Called when Configuration of the system changed
	    * @param newConfig Reference to the changed {@link android.content.res.Configuration}
	    * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
	    */
	    @Override
	    public void onConfigurationChanged(Configuration newConfig) 
	    {
	    	super.onConfigurationChanged(newConfig);
	    }

	    /**
	     * Called when the {@link android.app.Activity} is paused
	     * @see android.app.Activity#onPause()
	     */
	    @Override
	    public void onPause() 
	    {
	        super.onPause();
	    }
	   
	    /**
	     * Called when {@link android.app.Activity} is restarted
	     * @see android.app.Activity#onRestart()
	     */
	    @Override
	    public synchronized void onRestart() 
	    {
	        super.onRestart();
	    }

	    /*
	     * Called when {@link android.app.Activity} is stopped
	     * @see android.app.Activity#onStop()
	     */
	    @Override
	    public void onStop() 
	    {
	        super.onStop();
	    }

	    /* Called when {@link android.app.Activity} is destroyed
	     * Here, we obtain the self-annotated text and send a broadcast to the {@link com.airs.handlers.BloodPressureButtonHandler}
	     * @see android.app.Activity#onDestroy()
	     */
	    @Override
	    public void onDestroy() 
	    {	    	
	    	String pressure = systolic.getText() + "/" + diastolic.getText();
	    	
	    	if (selected == true)
	    	{
				// send broadcast intent to signal end of selection to mood button handler
				Intent intent = new Intent("com.airs.bloodpressure");
				intent.putExtra("BloodPressure", pressure);
				
				sendBroadcast(intent);			
	    	}
			
			// now destroy activity
	        super.onDestroy();
	    }
	    
	    /**
	     * Called when the OK is pressed
	     * @param v Reference to the buttons {@link android.view.View}
	     * @see android.view.View.OnClickListener#onClick(android.view.View)
	     */
	    public void onClick(View v) 
		{
	    	// dispatch depending on button pressed
	    	switch (v.getId())
	    	{
	    	case R.id.bp_ok:
		    		selected = true;
	    			finish();
	    			break;
	    	}
		}
}
