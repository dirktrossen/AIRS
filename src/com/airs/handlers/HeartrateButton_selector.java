/*
Copyright (C) 2011, Created by Justin Wetherell - phishman3579@gmail.com
original code licensed under Apache License 2.0
Integration into AIRS: Copyright (C) 2012, Dirk Trossen, airs@dirk-trossen.de

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

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.airs.*;

public class HeartrateButton_selector extends Activity implements OnClickListener, SurfaceHolder.Callback, PreviewCallback
{
	public static final int RED 			= 1;
	public static final int GREEN 			= 2;
	// states for camera
	public static final int NOT_READY		= 1;
	public static final int READY			= 2;
	public static final int RUNNING			= 3;

	 // preferences
	 private SharedPreferences settings;

	 private TextView mTitle;
	 private TextView mTitle2;
	 private int heartrate, adjustment;
	 private boolean selected = false;
	 private static SurfaceView preview = null;
	 private static SurfaceHolder previewHolder = null;
	 private static Camera camera = null;
	 private static WakeLock wakeLock = null;

	 private static final AtomicBoolean processing = new AtomicBoolean(false);
	 private static int averageIndex = 0;
	 private static final int averageArraySize = 4; // used to be 4
	 private static final int[] averageArray = new int[averageArraySize];

	 private static int currentType = GREEN;

	 private static int beatsIndex = 0;
	 private static final int beatsArraySize = 3;
	 private static final int[] beatsArray = new int[beatsArraySize];
	 private static double beats = 0;
	 private static long startTime = 0;
	 private static int previewReady = NOT_READY;
	 private static int maxMeasurements = 3, currentMeasurements;

	   @Override
	    public void onCreate(Bundle savedInstanceState) 
	    {	    
	        // Set up the window layout
	        super.onCreate(savedInstanceState);
	           
	        // read preferences
	        settings = PreferenceManager.getDefaultSharedPreferences(this);
	        adjustment = Integer.parseInt(settings.getString("HeartrateHandler::adjustment", "0"));
	        
			// set window title
	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
	        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			setContentView(R.layout.heartrate);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	        
	        // get window title fields
	        mTitle = (TextView) findViewById(R.id.title_left_text);
	        mTitle2 = (TextView) findViewById(R.id.heartrate_puls);
	        mTitle.setText(getString(R.string.AIRS_Heartrate));
        	
        	// hook listeners into buttons
    		ImageButton ibt = (ImageButton) findViewById(R.id.heartrate_ok);
    		ibt.setOnClickListener(this);
    		Button bt = (Button) findViewById(R.id.heartrate_cancel);
    		bt.setOnClickListener(this);
    		
    		// hook preview into view
            preview = (SurfaceView) findViewById(R.id.heartrate_preview);
            previewHolder = preview.getHolder();
            previewHolder.addCallback(this);
            previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            
            // now get wakelock to not dim screen while measuring
            PowerManager pm = (PowerManager) getSystemService(getApplicationContext().POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "AIRS heartrate");
            
            // set variables
            previewReady = NOT_READY;
            currentType = GREEN;
    		selected = false;
    		currentMeasurements = 0;
	    }

	   @Override
	   public void onResume() 
	   {
	        super.onResume();

	        // qcquire camera resources
	        try
	        {
	        	camera = Camera.open();
		        camera.setPreviewCallback(this);
		        if (previewReady == RUNNING)
		        	camera.startPreview();
	        }
	        catch(Exception e)
	        {
		  		Toast.makeText(getApplicationContext(), getString(R.string.Camera_error), Toast.LENGTH_LONG).show();
	        	finish();
	        }
	        
	        // acquire wake lock
	        wakeLock.acquire();
	    }
	   
	    @Override
	    public void onConfigurationChanged(Configuration newConfig) 
	    {
	    	super.onConfigurationChanged(newConfig);
	    }

	    @Override
	    public void onPause() 
	    {
	        super.onPause();

	        // release wake lock
	        wakeLock.release();

	        // release camera resources
	        camera.setPreviewCallback(null);
	        camera.stopPreview();
	        camera.release();
	        camera = null;
	    }
	   
	    @Override
	    public synchronized void onRestart() 
	    {
	        super.onRestart();
	    }

	    @Override
	    public void onStop() 
	    {
	        super.onStop();
	    }

	    @Override
	    public void onDestroy() 
	    {	    	
	        // release camera resources
	    	if (camera != null)
	    		camera.release();
	    	
	    	if (selected == true)
	    	{
				// send broadcast intent to signal end of selection to mood button handler
				Intent intent = new Intent("com.airs.heartrate");
				intent.putExtra("Heartrate", heartrate);
				
				sendBroadcast(intent);			
	    	}
			
			// now destroy activity
	        super.onDestroy();
	    }

	    public void onActivityResult(int requestCode, int resultCode, Intent data) 
	    {
	    	return;
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
		        		Toast.makeText(getApplicationContext(), R.string.HeartrateAbout, Toast.LENGTH_LONG).show();
		            return true;
		        }
		        return false;
		    }

	    public void onClick(View v) 
		{
	    	// dispatch depending on button pressed
	    	switch (v.getId())
	    	{
	    	case R.id.heartrate_ok:
	    		// depending on camera state
	    		switch(previewReady)
	    		{
	    		// surface not yet ready -> start running once callback comes back
	    		case NOT_READY:
	    			previewReady = RUNNING;
	    			break;
	    		// surface ready -> start previewing now!
	    		case READY:
	    			previewReady = RUNNING;
	    	        startTime = System.currentTimeMillis();
	            	camera.startPreview();
	            	break;
	            // camera running -> stop and confirm
	    		case RUNNING: 
		    		selected = true;
	    			finish();
	    			break;
	    		}
	    		break;
	    	case R.id.heartrate_cancel:
	    		selected = false;
	    		finish();
	    		break;
	    	}
		}
	    
        @Override
        public void surfaceCreated(SurfaceHolder holder) 
        {
            try 
            {
                camera.setPreviewDisplay(previewHolder);
                camera.setPreviewCallback(this);
            } 
            catch (Exception e) 
            {
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) 
        {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            Camera.Size size = getSmallestPreviewSize(width, height, parameters);
            if (size != null) 
            {
                parameters.setPreviewSize(size.width, size.height);
            }
            camera.setParameters(parameters);
            
            // set to RUNNING already -> start preview then and remember start time!
            if (previewReady == RUNNING)
            {
    	        startTime = System.currentTimeMillis();
            	camera.startPreview();
            }
            else 	// otherwise signal that it is ready!
            	previewReady = READY;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // Ignore
        }

	    private Camera.Size getSmallestPreviewSize(int width, int height, Camera.Parameters parameters) 
	    {
	        Camera.Size result = null;

	        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
	            if (size.width <= width && size.height <= height) {
	                if (result == null) {
	                    result = size;
	                } else {
	                    int resultArea = result.width * result.height;
	                    int newArea = size.width * size.height;

	                    if (newArea < resultArea) result = size;
	                }
	            }
	        }

	        return result;
	    }
	    
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) 
        {
        	int i;
        	int rollingAverage;
        	 
            if (data == null) 
            	return;
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null) 
            	return;

            // anything running?
            if (!processing.compareAndSet(false, true)) 
            	return;

            // determine red 'energy'
            int imgAvg = decodeYUV420SPtoRedAvg(data.clone(), size.height, size.width);
            if (imgAvg == 0 || imgAvg == 255) 
            {
                processing.set(false);
                return;
            }

            // determine average of rolling array
            int averageArrayAvg = 0;
            int averageArrayCnt = 0;
            for (i = 0; i < averageArray.length; i++) 
                if (averageArray[i] > 0) 
                {
                    averageArrayAvg += averageArray[i];
                    averageArrayCnt++;
                }

            if (averageArrayCnt > 0)
            	rollingAverage = averageArrayAvg / averageArrayCnt;
            else
            	rollingAverage = 0;
            
            int newType = currentType;
            if (imgAvg < rollingAverage) 
            {
                newType = RED;
                if (newType != currentType) 
                    beats++;
            } 
            else 
            	if (imgAvg > rollingAverage) 
            		newType = GREEN;

            // place in rolling array
            if (averageIndex == averageArraySize) 
            	averageIndex = 0;
            averageArray[averageIndex] = imgAvg;
            averageIndex++;

            // Transitioned from one state to another to the same
            if (newType != currentType) 
                currentType = newType;

            long endTime = System.currentTimeMillis();
            double totalTimeInSecs = (endTime - startTime) / 1000d;
            
            // wait at least 10 seconds
            if (totalTimeInSecs >= 10) 
            {
                double bps = (beats / totalTimeInSecs);
                int dpm = (int) (bps * 60d);
                if (dpm < 30 || dpm > 180) 
                {
                    startTime = System.currentTimeMillis();
                    beats = 0;
                    processing.set(false);
                    return;
                }

                // place in rolling array
                if (beatsIndex == beatsArraySize) 
                	beatsIndex = 0;
                beatsArray[beatsIndex] = dpm;
                beatsIndex++;

                int beatsArrayAvg = 0;
                int beatsArrayCnt = 0;
                for (i = 0; i < beatsArray.length; i++) 
                    if (beatsArray[i] > 0) 
                    {
                        beatsArrayAvg += beatsArray[i];
                        beatsArrayCnt++;
                    }

                int beatsAvg = (beatsArrayAvg / beatsArrayCnt);
                heartrate = beatsAvg + adjustment;
                mTitle2.setText(String.valueOf(heartrate)+ " beats");
                startTime = System.currentTimeMillis();
                beats = 0;
                
                // test if maximum time is reached
                currentMeasurements++;
                if (currentMeasurements>maxMeasurements)
                	finish();
            }
            processing.set(false);
        }
        
        private int decodeYUV420SPtoRedSum(byte[] yuv420sp, int width, int height) 
        {
            if (yuv420sp == null) 
            	return 0;

            final int frameSize = width * height;

            int sum = 0;
            for (int j = 0, yp = 0; j < height; j++) 
            {
                int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
                for (int i = 0; i < width; i++, yp++) 
                {
                    int y = (0xff & ((int) yuv420sp[yp])) - 16;
                    if (y < 0) y = 0;
                    if ((i & 1) == 0) 
                    {
                        v = (0xff & yuv420sp[uvp++]) - 128;
                        u = (0xff & yuv420sp[uvp++]) - 128;
                    }
                    int y1192 = 1192 * y;
                    int r = (y1192 + 1634 * v);
                    int g = (y1192 - 833 * v - 400 * u);
                    int b = (y1192 + 2066 * u);

                    if (r < 0) 
                    	r = 0;
                    else 
                    	if (r > 262143) 
                    		r = 262143;
                    if (g < 0) 
                    	g = 0;
                    else 
                    	if (g > 262143) 
                    		g = 262143;
                    if (b < 0) 
                    	b = 0;
                    else 
                    	if (b > 262143) 
                    		b = 262143;

                    int pixel = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
                    int red = (pixel >> 16) & 0xff;
                    sum += red;
                }
            }
            return sum;
        }

        public int decodeYUV420SPtoRedAvg(byte[] yuv420sp, int width, int height) 
        {
            if (yuv420sp == null) return 0;

            final int frameSize = width * height;

            int sum = decodeYUV420SPtoRedSum(yuv420sp, width, height);
            return (sum / frameSize);
        }

}


