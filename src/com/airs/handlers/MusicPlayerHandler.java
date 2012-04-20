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
package com.airs.handlers;

import java.util.concurrent.Semaphore;

import com.airs.helper.SerialPortLogger;
import com.airs.platform.HandlerManager;
import com.airs.platform.SensorRepository;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

/**
 * @author trossen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MusicPlayerHandler implements com.airs.handlers.Handler
{
	public static final int INIT_MUSIC 			= 1;

	private Context airs;
	private String Music, Artist, Album, Track;
	private String Music_old;
	private boolean startedMusicPlayer = false;
	private Semaphore music_semaphore 		= new Semaphore(1);
	private Semaphore artist_semaphore 		= new Semaphore(1);
	private Semaphore album_semaphore 		= new Semaphore(1);
	private Semaphore track_semaphore 		= new Semaphore(1);

	/**
	 * Sleep function 
	 * @param millis
	 */
	protected void sleep(long millis) 
	{
		try 
		{
			Thread.sleep(millis);
		} 
		catch (InterruptedException ignore) 
		{
		}
	}
	
	private void wait(Semaphore sema)
	{
		try
		{
			sema.acquire();
		}
		catch(Exception e)
		{
		}
	}
	
	/***********************************************************************
	 Function    : Acquire()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public byte[] Acquire(String sensor, String query)
	{	
		// has music player intent been started?
		if (startedMusicPlayer == false)
		{
			// send message to handler thread to start Outgoing Call
	        Message msg = mHandler.obtainMessage(INIT_MUSIC);
	        mHandler.sendMessage(msg);	
		}

		// All playing information
		if(sensor.compareTo("MP") == 0)
		{
			wait(music_semaphore); // block until semaphore available

			if (Music == null)
				return null;
			
			// create reading buffer with callee number
		    StringBuffer buffer = new StringBuffer("MP");
		    buffer.append(Music);
    		return buffer.toString().getBytes();
		}

		// Artist playing information
		if(sensor.compareTo("MA") == 0)
		{
			wait(artist_semaphore); // block until semaphore available

			if (Artist == null)
				return null;
			
			// create reading buffer with callee number
		    StringBuffer buffer = new StringBuffer("MA");
		    buffer.append(Artist);
    		return buffer.toString().getBytes();
		}

		// Album playing information
		if(sensor.compareTo("ML") == 0)
		{
			wait(album_semaphore); // block until semaphore available

			if (Album == null)
				return null;
			
			// create reading buffer with callee number
		    StringBuffer buffer = new StringBuffer("ML");
		    buffer.append(Album);
    		return buffer.toString().getBytes();
		}

		// All playing information
		if(sensor.compareTo("MT") == 0)
		{
			wait(track_semaphore); // block until semaphore available

			if (Track == null)
				return null;
			
			// create reading buffer with callee number
		    StringBuffer buffer = new StringBuffer("MT");
		    buffer.append(Track);
    		return buffer.toString().getBytes();
		}
		
		return null;
	}
	
	/***********************************************************************
	 Function    : Share()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public synchronized String Share(String sensor)
	{		
		// battery level?
		if(sensor.compareTo("MP") == 0 && Artist != null && Album != null && Track != null)
			return "The current music playing is " + Artist + " from album '" + Album + "' with song '" + Track + "'";
		
		if(sensor.compareTo("MA") == 0 && Artist != null)
			return "The current artist playing is " + "'" + Artist + "'";

		if(sensor.compareTo("ML") == 0 && Album != null)
			return "The current album playing is " + "'" + Album + "'";

		if(sensor.compareTo("MT") == 0 && Track != null)
			return "The current track playing is " + "'" + Track + "'";

		return null;		
	}
	
	/***********************************************************************
	 Function    : History()
	 Input       : sensor input for specific history views
	 Output      :
	 Return      :
	 Description : calls historical views
	***********************************************************************/
	public synchronized void History(String sensor)
	{
	}
	
	/***********************************************************************
	 Function    : Discover()
	 Input       : 
	 Output      : string with discovery information
	 Return      : 
	 Description : provides discovery information of this particular acquisition 
	 			   module, hardcoded 
	***********************************************************************/
	public void Discover()
	{
    	SensorRepository.insertSensor(new String("MP"), new String("Music"), new String("Currently playing"), new String("txt"), 0, 0, 1, false, 0, this);	    	    	
    	SensorRepository.insertSensor(new String("MA"), new String("Artist"), new String("Current Artist"), new String("txt"), 0, 0, 1, false, 0, this);	    	    	
    	SensorRepository.insertSensor(new String("ML"), new String("Album"), new String("Current Album"), new String("txt"), 0, 0, 1, false, 0, this);	    	    	
    	SensorRepository.insertSensor(new String("MT"), new String("Track"), new String("Current Track"), new String("txt"), 0, 0, 1, false, 0, this);	    	    	
	}
	
	public MusicPlayerHandler(Context airs)
	{
		this.airs = airs;
		try
		{
			// charge the semaphores to block at next call!
			music_semaphore.acquire(); 
			album_semaphore.acquire(); 
			artist_semaphore.acquire(); 
			track_semaphore.acquire(); 
			
			// get persistently stored info of last readings
			Music_old  = HandlerManager.readRMS("MusicPlayerHandler::Music", "");

		}
		catch(Exception e)
		{
			SerialPortLogger.debugForced("Semaphore!!!!");
		}
	}
	
	public void destroyHandler()
	{
		if (startedMusicPlayer == true)
			airs.unregisterReceiver(SystemReceiver);
	}
		
	// The Handler that gets information back from the other threads, initializing phone sensors
	// We use a handler here to allow for the Acquire() function, which runs in a different thread, to issue an initialization of the invidiual sensors
	// since registerListener() can only be called from the main Looper thread!!
	private final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {     
    	   IntentFilter intentFilter;
    	   
           switch (msg.what) 
           {
           case INIT_MUSIC:
      	        intentFilter = new IntentFilter();
      	   	    intentFilter.addAction("com.android.music.metachanged");
      	   	    intentFilter.addAction("com.android.music.playstatechanged");
      	   	    intentFilter.addAction("com.android.music.playbackcomplete");
      	   	    intentFilter.addAction("com.android.music.queuechanged");
	   		   	airs.registerReceiver(SystemReceiver, intentFilter);
	   		   	startedMusicPlayer = true;
	   		   	break;  
           default:  
           		break;
           }
           

       }
    };

		
	private final BroadcastReceiver SystemReceiver = new BroadcastReceiver() 
	{
        @Override
        public void onReceive(Context context, Intent intent) 
        {
            String action = intent.getAction();
            
        	if (action.compareTo("com.android.music.playbackcomplete") == 0)
        		Artist = Album = Track = Music = null;
        	else
        	{
        		// get player information
            	Artist = intent.getStringExtra("artist");            	
            	Album = intent.getStringExtra("album");
            	Track = intent.getStringExtra("track");
            	
            	// combine to overall information, if there is anything to combine!
            	if (Artist != null && Album != null && Track != null)
            	{
            		Music = Artist + ":" + Album + ":" + Track;
            	
	            	// test if overall music information has changed
	            	if (Music_old.equals(Music) == false)
	            	{
	            		// store changed information
	            		HandlerManager.writeRMS("MusicPlayerHandler::Music", Music);
	            		// and remember
	            		Music_old = new String(Music);
	            	}
	            	else
	            		Artist = Album = Track = Music = null;   // if info is the same as before, don't use it!!
            	}
            	else
            		Artist = Album = Track = Music = null;
        	}
        	
        	// only signal if there's a changed music info
        	if (Music != null)
        	{
	        	music_semaphore.release();			// release semaphore
	        	artist_semaphore.release();			// release semaphore
	        	album_semaphore.release();			// release semaphore
	        	track_semaphore.release();			// release semaphore
        	}
        }
    };
}

