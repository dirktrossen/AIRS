/*
Copyright (C) 2004-2006 Nokia Corporation
Copyright (C) 2008-2009, Dirk Trossen, nors@dirk-trossen.de

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
package com.android.airs.helper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.os.Environment;
import android.widget.Toast;

public class SerialPortLogger 
{
	private static boolean isDebugging = true;
	public static Activity nors;
	private static File fconn = null;
	private static BufferedOutputStream os = null;

	/**
	 * Write message to the serial port.
	 * 
	 */
	public static synchronized void write(String msg) 
	{
		if (os==null)
		{
		    try 
		    {
		    	// open file and create, if necessary
				fconn = new File(Environment.getExternalStorageDirectory(), "AIRS_log.txt");
		    	os = new BufferedOutputStream(new FileOutputStream(fconn, true));
		    }
		    catch(Exception e)
		    {
		    }
		}

		try
		{
			if (os!=null)
			{
				os.write(msg.getBytes(), 0, msg.length());
				os.flush();
			}
		}
		catch(Exception e)
		{
		}
	}

	/**
	 * By setting is Debugging: output to the port;
	 * By setting isSMSDebugging: send debug info to a cell phone;
	 * 	  
	 */
	
	public static synchronized void debugForced(String msg) 
	{
		write(msg + "\n");
	}

	public static synchronized void debug(String msg) 
	{
		if (isDebugging)
			write(msg + "\n");
	}
	
	public static void debugUI(String msg, int wait)
	{
		Toast.makeText(nors.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
		try 
		{
			Thread.sleep(wait);
		} 
		catch (InterruptedException ignore) 
		{
		}

	}

	/**
	 * @return is Debugging
	 */
	public static boolean isDebugging() 
	{
		return isDebugging;
	}

	/**
	 * @param b
	 */
	public static void setDebugging(boolean b) 
	{
		isDebugging = b;
	}

}
