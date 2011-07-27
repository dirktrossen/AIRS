/*
Copyright (C) 2008-2011, Dirk Trossen, nors@dirk-trossen.de

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
package com.android.airs.handlers;

import java.util.concurrent.Semaphore;

import android.content.Context;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.PhoneStateListener;

import com.android.airs.helper.*;
import com.android.airs.platform.SensorRepository;

public class CellHandler extends PhoneStateListener implements Handler
{
	Context nors;
	// phone state classes
	private TelephonyManager tm;

	// are these there?
	private boolean enableProperties = false;
	// sensor data   
	private int cellID, cellLac, mcc;
	private int oldCellID = -1, oldcellLac = -1, oldNCC = -1;
	private int oldcellStrength = -1;
	private int roaming = 0, oldroaming = -1;
	private int data_state, olddata_state = -1;
	private boolean signal_read = false;
	private boolean data_read = false;
	private boolean cell_read = false;
	private boolean lac_read = false;
	private boolean ncc_read = false;
	
	private Semaphore signal_semaphore	 	= new Semaphore(1);
	private Semaphore data_semaphore	 	= new Semaphore(1);
	private Semaphore cellid_semaphore	 	= new Semaphore(1);
	private Semaphore lac_semaphore	 		= new Semaphore(1);
	private Semaphore mcc_semaphore	 		= new Semaphore(1);
	
	protected void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}
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
		// acquire data and send out
		try
		{
			if (enableProperties == true)
				return cellReading(sensor);
		}
		catch (Exception e) 
		{
			debug("CellHandler:Acquire: Exception: " + e.toString());
		}
		
		return null;		
	}
	
	/***********************************************************************
	 Function    : Discover()
	 Input       : 
	 Output      : string with discovery information
	 Return      : 
	 Description : provides discovery information of this particular acquisition 
	 			   module 
	***********************************************************************/
	public void Discover()
	{
	    if (enableProperties == true)
	    {
		    SensorRepository.insertSensor(new String("CR"), new String("boolean"), new String("Roaming"), new String("int"), 0, 0, 1, 60000, this);	    
		    SensorRepository.insertSensor(new String("CD"), new String("boolean"), new String("Data connected"), new String("int"), 0, 0, 1, 0, this);	    
		    SensorRepository.insertSensor(new String("CS"), new String("dBm"), new String("Signal strength"), new String("int"), 0, -120, 0, 0, this);	    
		    SensorRepository.insertSensor(new String("CI"), new String("ID"), new String("Cell identifier"), new String("int"), 0, 0, 65535, 0, this);	    
		    SensorRepository.insertSensor(new String("CL"), new String("ID"), new String("Location Area Code"), new String("int"), 0, 0, 65535, 0, this);	    
		    SensorRepository.insertSensor(new String("CC"), new String("NCC"), new String("Net Country Code"), new String("int"), 0, 0, 65535, 0, this);
		}		
	}
	
	public CellHandler(Context nors)
	{
		this.nors = nors;
		
		try
		{
			// try getting phone manager
		    tm  = (TelephonyManager) nors.getSystemService(Context.TELEPHONY_SERVICE); 			// if something returned, enter sensor value
			if (tm != null)
			{
				// arm semaphores
				wait(data_semaphore); 
				wait(signal_semaphore); 
				wait(cellid_semaphore); 
				wait(lac_semaphore); 
				wait(mcc_semaphore); 
	
				// register my listener for getting signal strength, location changes and data connection state events
				// but only if airplane mode is not enabled!
				if ((Settings.System.getInt(nors.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0) == 0))
				{
					tm.listen(this, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_CELL_LOCATION);
					enableProperties = true;
				}
			}
		}
		catch(Exception e)
		{
		}
	}
	
	public void destroyHandler()
	{
		// unregister listeners
		if (enableProperties == true)
			tm.listen(this, PhoneStateListener.LISTEN_NONE);
	}

	private byte[] cellReading(String sensor)
	{
		byte[] reading = null;
		
		try 
		{		
				switch(sensor.charAt(1))
				{
				case 'I':
					wait(cellid_semaphore); // block until semaphore available

				    // cellID changed?
				    if (cell_read == true)
					{		
				    	cell_read = false;
						// create reading
						reading = new byte[6];
						reading[0] = (byte)sensor.charAt(0);
						reading[1] = (byte)sensor.charAt(1);
						reading[2] = (byte)((oldCellID>>24) & 0xff);
						reading[3] = (byte)((oldCellID>>16) & 0xff);
						reading[4] = (byte)((oldCellID>>8) & 0xff);
						reading[5] = (byte)(oldCellID & 0xff);			
					}
					break;
				case 'L':
					wait(lac_semaphore); // block until semaphore available

				    if (lac_read == true)
					{		
				    	lac_read = false;
						// create reading
						reading = new byte[6];
						reading[0] = (byte)sensor.charAt(0);
						reading[1] = (byte)sensor.charAt(1);
						reading[2] = (byte)((oldcellLac>>24) & 0xff);
						reading[3] = (byte)((oldcellLac>>16) & 0xff);
						reading[4] = (byte)((oldcellLac>>8) & 0xff);
						reading[5] = (byte)(oldcellLac & 0xff);			
					}
					break;
				case 'R':
					// try reading GSM cell info
			        if (tm.isNetworkRoaming()==true)
			        	roaming = 1;
			        else
			        	roaming = 0;
					// any change?
					if (roaming != oldroaming)
					{
						oldroaming = roaming;
						reading = new byte[6];
						reading[0] = (byte)sensor.charAt(0);
						reading[1] = (byte)sensor.charAt(1);
						reading[2] = (byte)((roaming>>24) & 0xff);
						reading[3] = (byte)((roaming>>16) & 0xff);
						reading[4] = (byte)((roaming>>8) & 0xff);
						reading[5] = (byte)(roaming & 0xff);			
					}
					break;
				case 'S':
					wait(signal_semaphore); // block until semaphore available

					// if listener was triggered, create reading
					if (signal_read == true)
					{						
						reading = new byte[6];
						reading[0] = (byte)sensor.charAt(0);
						reading[1] = (byte)sensor.charAt(1);
						reading[2] = (byte)((oldcellStrength>>24) & 0xff);
						reading[3] = (byte)((oldcellStrength>>16) & 0xff);
						reading[4] = (byte)((oldcellStrength>>8) & 0xff);
						reading[5] = (byte)(oldcellStrength & 0xff);
						
						// clear flag
						signal_read = false;
					}
					
					break;
				case 'D':
					wait(data_semaphore); // block until semaphore available

					// if listener was triggered, create reading
					if (data_read == true)
					{
						reading = new byte[6];
						reading[0] = (byte)sensor.charAt(0);
						reading[1] = (byte)sensor.charAt(1);
						reading[2] = (byte)((olddata_state>>24) & 0xff);
						reading[3] = (byte)((olddata_state>>16) & 0xff);
						reading[4] = (byte)((olddata_state>>8) & 0xff);
						reading[5] = (byte)(olddata_state & 0xff);
						
						data_read = false;
					}
					break;
				case 'C':
					wait(mcc_semaphore); // block until semaphore available

				    if (ncc_read == true)
					{		
				    	ncc_read = false;
						// create reading
						reading = new byte[6];
						reading[0] = (byte)sensor.charAt(0);
						reading[1] = (byte)sensor.charAt(1);
						reading[2] = (byte)((oldNCC>>24) & 0xff);
						reading[3] = (byte)((oldNCC>>16) & 0xff);
						reading[4] = (byte)((oldNCC>>8) & 0xff);
						reading[5] = (byte)(oldNCC & 0xff);			
					}
					break;
				}
		}
		catch (Exception e) 
		{
			debug("LocationHandler::Exception in cellReading()");
		}
		
		return reading;
	}

	@Override
	public void onDataConnectionStateChanged (int state)
	{
		if (state == TelephonyManager.DATA_CONNECTED)
			data_state = 1;
		if (state == TelephonyManager.DATA_DISCONNECTED)
			data_state = 0;
		
		if (data_state != olddata_state)
		{
			olddata_state = data_state;
			data_read = true;

			// release semaphore
			data_semaphore.release();
		}		
	}

	@Override
	public void onSignalStrengthsChanged(SignalStrength signalStrength)
    {
		int strength;
	
		strength = signalStrength.getGsmSignalStrength();
		if (strength != oldcellStrength)
		{
			oldcellStrength = -113 + 2*strength;
			signal_read = true;

			// release semaphore
			signal_semaphore.release();
		}
    }
	
	@Override
	public void onCellLocationChanged (CellLocation location)
	{
		cellID = ((GsmCellLocation)location).getCid();
		// cellID changed?
	    if (cellID != oldCellID)
		{		
	    	oldCellID = cellID;
	    	cell_read = true;

			// release semaphore for picking up the values
			cellid_semaphore.release();
		}

		cellLac = ((GsmCellLocation)location).getLac();
	    // LAC changed?
	    if (cellLac != oldcellLac)
		{		
	    	oldcellLac = cellLac;
	    	lac_read = true;

			// release semaphore for picking up the values
	    	lac_semaphore.release();
		}
	    
		String networkOperator = tm.getNetworkOperator();
	    if (networkOperator != null) 
	        mcc = Integer.parseInt(networkOperator.substring(0, 3));
	    else
	    	mcc = 0;

	    // NCC changed?
	    if (mcc != oldNCC)
	    {
	    	oldNCC = mcc;
	    	ncc_read = true;
	    
			// release semaphore for picking up the values
	    	mcc_semaphore.release();
	    }
	}
}

