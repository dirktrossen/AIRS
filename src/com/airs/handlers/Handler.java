/*
Copyright (C) 2004-2006 Nokia Corporation, Contact: Dirk Trossen, airs@dirk-trossen.de
Copyright (C) 2010-2011, Dirk Trossen, airs@dirk-trossen.de

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
/**
 * Interface for sensor handlers
 */
public interface Handler 
{
	/**
	 * Method to acquire sensor symbol
	 * @param sensor String of the sensor symbol to be acquired
	 * @param query String of the query to be executed
	 */
    public byte[] Acquire(String sensor, String query);
    /**
     * Method to discover sensor symbols supported by a handler 
     */
    public void   Discover();
    /**
     * Method to destroy resources of handler
     */
    public void   destroyHandler();
    /**
     * Method to share the current value of the given sensor
     * @param sensor String of the sensor to be shared
     * @return human-readable String representation of last value
     */
    public String Share(String sensor);
    /**
     * Method to provide a historical view of the given sensor
     * @param sensor String ot the sensor to be shown
     */
    public void History(String sensor);
}
