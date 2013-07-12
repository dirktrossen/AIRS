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
package com.airs.helper;

import android.content.Context;

/**
 * Class to implement the sleep function used throughout the platform, right now a simple {@link Thread} sleep()
 *
 */
public class Waker
{
	/**
	 * Initialise the sleep function
	 * @param context Reference to the calling {@link android.content.Context}
	 */
	static public void init(Context context)
	{
	}
	
	/**
	 * Sleep function
	 * @param milli time to sleep in milliseconds
	 */
	static public void sleep(long milli)
	{
		try
		{
			Thread.sleep(milli);
		}
		catch(Exception e)
		{
		}
	}
}
