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
package com.airs.handlers;

import java.util.concurrent.Semaphore;

import android.content.Context;
import android.os.Environment;
import android.os.FileObserver;

import com.airs.helper.Waker;
import com.airs.platform.HandlerManager;
import com.airs.platform.SensorRepository;

/**
 * @author trossen
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MediaWatcherHandler implements Handler
{
	private boolean camera, music, pictures, videos;
	private Semaphore watcher_semaphore 	= new Semaphore(1);
	private boolean installed = false;
	private OwnObserver camera_observer = null, music_observer = null, pictures_observer = null, videos_observer = null;
	private String watched_file;
	private String watched_type;
	private boolean reading_sensor = false;
	private String camera_directory, music_directory, pictures_directory, videos_directory;
	
	/**
	 * Sleep function 
	 * @param millis
	 */
	protected void sleep(long millis) 
	{
		Waker.sleep(millis);
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
	public synchronized byte[] Acquire(String sensor, String query)
	{
		StringBuffer readings = null;

		// have observers been installed?
		if (installed == false)
		{
			if (camera == true)
			{
				camera_observer = new OwnObserver(camera_directory, "camera");
				camera_observer.startWatching();

			}

			if (music == true)
			{
				music_observer = new OwnObserver(music_directory, "music");
				music_observer.startWatching();
			}
				
			if (pictures == true)
			{
				pictures_observer = new OwnObserver(pictures_directory, "pictures");
				pictures_observer.startWatching();
			}
			
			if (videos == true)
			{
				videos_observer = new OwnObserver(videos_directory, "videos");
				videos_observer.startWatching();
			}

			installed = true;
		}
		
		// now wait for observer to call!
		wait(watcher_semaphore); 
		// lock sensor
		reading_sensor = true;
		
		if (watched_type != null && watched_file != null)
		{
			readings = new StringBuffer(sensor);
			readings.append(watched_type + ":" + watched_file);
			// lock sensor
			reading_sensor = false;

			return readings.toString().getBytes();		
		}
		else
		{
			reading_sensor = false;

			return null;					
		}
		
	}
	
	/***********************************************************************
	 Function    : Share()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public String Share(String sensor)
	{		
		return null;		
	}
	
	/***********************************************************************
	 Function    : History()
	 Input       : sensor input for specific history views
	 Output      :
	 Return      :
	 Description : calls historical views
	***********************************************************************/
	public void History(String sensor)
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
		// is at least one watch type selected?
		if (camera == true || music == true || pictures == true || videos == true)
			SensorRepository.insertSensor(new String("MW"), new String("file"), new String("Watched media folder"), new String("txt"), 0, 0, 1, false, 0, this);	    
	}
	
	public MediaWatcherHandler(Context airs)
	{
		camera   = HandlerManager.readRMS_b("MediaWatcherHandler::Camera", false);
		music    = HandlerManager.readRMS_b("MediaWatcherHandler::Music", false);
		pictures = HandlerManager.readRMS_b("MediaWatcherHandler::Pictures", false);
		videos   = HandlerManager.readRMS_b("MediaWatcherHandler::Videos", false);

		camera_directory = HandlerManager.readRMS("MediaWatcherHandler::CameraDirectory", Environment.getExternalStorageDirectory()+"/DCIM/Camera");
		music_directory = HandlerManager.readRMS("MediaWatcherHandler::MusicDirectory", Environment.getExternalStorageDirectory()+"/Music");
		pictures_directory = HandlerManager.readRMS("MediaWatcherHandler::PicturesDirectory", Environment.getExternalStorageDirectory()+"/Pictures");
		videos_directory = HandlerManager.readRMS("MediaWatcherHandler::VideosDirectory", Environment.getExternalStorageDirectory()+"/Videos");
		
		// arm the semaphores now
		wait(watcher_semaphore); 
	}
	
	public void destroyHandler()
	{
		// release all semaphores for unlocking the Acquire() threads
		watcher_semaphore.release();

		reading_sensor = false;
		
		// destroy observers
		if (camera_observer != null)
		{
			camera_observer.stopWatching();
			camera_observer = null;
		}
		if (music_observer != null)
		{
			music_observer.stopWatching();
			music_observer = null;
		}
		if (pictures_observer != null)
		{
			pictures_observer.stopWatching();
			pictures_observer = null;
		}
		if (pictures_observer != null)
		{
			pictures_observer.stopWatching();
			pictures_observer = null;
		}
	}
	
	// own Observer class
	private class OwnObserver extends FileObserver
	{
		private String media_type;
		
		public OwnObserver(String path, String type)
		{
			super(path, FileObserver.CREATE);
			// store media type for later
			media_type = new String(type);
		}
		
		// called when event occurs
		public void onEvent (int event, String path)
		{
			while (reading_sensor == true)
				sleep(200);
			
			if (path != null)
				// discount tmp files!
				if (path.endsWith(".tmp") == false)
				{
					// copy watched file
					watched_file = new String(path);
					watched_type = new String(media_type);
					
					// release semaphore
					watcher_semaphore.release(); 
				}
		}
	}


}
