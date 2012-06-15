/*
Copyright (C) 2008-2011, Dirk Trossen, airs@dirk-trossen.de

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
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.telephony.PhoneStateListener;

import com.airs.helper.SerialPortLogger;
import com.airs.platform.History;
import com.airs.platform.SensorRepository;

public class CellHandler extends PhoneStateListener implements com.airs.handlers.Handler
{
	public static final int INIT_SIGNALSTRENGTH = 1;
	public static final int INIT_DATACONNECTED = 2;
	public static final int INIT_CELLLOCATION = 3;
	
	Context nors;
	// phone state classes
	private TelephonyManager tm;
	private CellHandler cellhandler;

	// are these there?
	private boolean enableProperties = false;
	// sensor data   
	private int cellID, cellLac, mcc;
	private int oldCellID = -1, oldcellLac = -1, oldNCC = -1;
	private int oldcellStrength = -1, oldcellStrength_bar = -1;
	private int roaming = 0, oldroaming = -1;
	private int data_state, olddata_state = -1;
	private boolean signal_read = false;
	private boolean bar_read 	= false;
	private boolean data_read 	= false;
	private boolean cell_read 	= false;
	private boolean lac_read 	= false;
	private boolean ncc_read 	= false;
	
	private boolean startedData = false, startedSignal = false, startedLocation = false;
	
	private Semaphore signal_semaphore	 	= new Semaphore(1);
	private Semaphore bar_semaphore	 		= new Semaphore(1);
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
	 Function    : Share()
	 Input       : sensor input is ignored here!
	 Output      :
	 Return      :
	 Description : acquires current sensors values and sends to
	 		 	   QueryResolver component
	***********************************************************************/
	public synchronized String Share(String sensor)
	{		
		switch(sensor.charAt(1))
		{
		case 'I':
		    return "The current cell identifier I am connected to is " + String.valueOf(oldCellID);
		case 'L':
		    return "The current cell area identifier I am connected to is " + String.valueOf(oldcellLac);
		case 'R':
	        if (roaming == 1)
			    return "I'm currently roaming!";
	        else
			    return "I'm currently NOT roaming!";
		case 'S':
		    return "The current cell signal strength is " + String.valueOf(oldcellStrength) + " dB";
		case 'B':
		    return "The current cell signal strength is " + String.valueOf(oldcellStrength_bar) + " bars";
		case 'D':
	        if (olddata_state == 1)
			    return "I'm currently using cellular data!";
	        else
			    return "I'm currently NOT using cellular data!";
		case 'C':
		    return "My current cell network country code is " + String.valueOf(oldNCC);
		}		
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
		switch(sensor.charAt(1))
		{
		case 'S':
			History.timelineView(nors, "Signal strength [dBm]", "CS");
			break;
		case 'B':
			History.timelineView(nors, "Signal strength [bar]", "CB");
			break;
		}		
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
		    SensorRepository.insertSensor(new String("CR"), new String("boolean"), new String("Roaming"), new String("int"), 0, 0, 1, false, 60000, this);	    
		    SensorRepository.insertSensor(new String("CD"), new String("boolean"), new String("Data connected"), new String("int"), 0, 0, 1, false, 0, this);	    
		    SensorRepository.insertSensor(new String("CS"), new String("dBm"), new String("Signal strength"), new String("int"), 0, -120, 0, true, 0, this);	    
		    SensorRepository.insertSensor(new String("CB"), new String("bars"), new String("Signal strength"), new String("int"), 0, 0, 7, true, 0, this);	    
		    SensorRepository.insertSensor(new String("CI"), new String("ID"), new String("Cell identifier"), new String("int"), 0, 0, 65535, false, 0, this);	    
		    SensorRepository.insertSensor(new String("CL"), new String("ID"), new String("Location Area Code"), new String("int"), 0, 0, 65535, false, 0, this);	    
		    SensorRepository.insertSensor(new String("CC"), new String("MCC"), new String("Mobile Country Code"), new String("int"), 0, 0, 65535, false, 0, this);
		}		
	}
	
	public CellHandler(Context nors)
	{
		this.nors = nors;
		this.cellhandler = this;
		
		try
		{
			// try getting phone manager
		    tm  = (TelephonyManager) nors.getSystemService(Context.TELEPHONY_SERVICE); 			// if something returned, enter sensor value
		    if (tm != null)
			{
		    	// if it is not a GSM phone, return right away
				if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_GSM)
					return;

				// arm semaphores
				wait(data_semaphore); 
				wait(signal_semaphore); 
				wait(bar_semaphore); 
				wait(cellid_semaphore); 
				wait(lac_semaphore); 
				wait(mcc_semaphore); 
	
				// register my listener for getting signal strength, location changes and data connection state events
				// but only if airplane mode is not enabled!
				if ((Settings.System.getInt(nors.getContentResolver(),Settings.System.AIRPLANE_MODE_ON, 0) == 0))
					enableProperties = true;
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
					// has location been started?
					if (startedLocation == false)
					{
						// send message to handler thread to start location listener
				        Message msg = mHandler.obtainMessage(INIT_CELLLOCATION);
				        mHandler.sendMessage(msg);	
					}

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
					// has location been started?
					if (startedLocation == false)
					{
						// send message to handler thread to start location listener
				        Message msg = mHandler.obtainMessage(INIT_CELLLOCATION);
				        mHandler.sendMessage(msg);	
					}

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
					// has signal strength been started?
					if (startedSignal == false)
					{
						// send message to handler thread to start signal strength listener
				        Message msg = mHandler.obtainMessage(INIT_SIGNALSTRENGTH);
				        mHandler.sendMessage(msg);	
					}

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
				case 'B':
					// has signal strength been started?
					if (startedSignal == false)
					{
						// send message to handler thread to start signal strength listener
				        Message msg = mHandler.obtainMessage(INIT_SIGNALSTRENGTH);
				        mHandler.sendMessage(msg);	
					}

					wait(bar_semaphore); // block until semaphore available

					// if listener was triggered, create reading
					if (bar_read == true)
					{						
						reading = new byte[6];
						reading[0] = (byte)sensor.charAt(0);
						reading[1] = (byte)sensor.charAt(1);
						reading[2] = (byte)((oldcellStrength_bar>>24) & 0xff);
						reading[3] = (byte)((oldcellStrength_bar>>16) & 0xff);
						reading[4] = (byte)((oldcellStrength_bar>>8) & 0xff);
						reading[5] = (byte)(oldcellStrength_bar & 0xff);
						
						// clear flag
						bar_read = false;
					}
					
					break;
				case 'D':
					// has signal strength been started?
					if (startedData == false)
					{
						// send message to handler thread to start signal strength listener
				        Message msg = mHandler.obtainMessage(INIT_DATACONNECTED);
				        mHandler.sendMessage(msg);	
					}

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
					// has location been started?
					if (startedLocation == false)
					{
						// send message to handler thread to start location listener
				        Message msg = mHandler.obtainMessage(INIT_CELLLOCATION);
				        mHandler.sendMessage(msg);	
					}

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

	// The Handler that gets information back from the other threads, initializing phone sensors
	// We use a handler here to allow for the Acquire() function, which runs in a different thread, to issue an initialization of the invidiaul sensors
	// since registerListener() can only be called from the main Looper thread!!
	public final Handler mHandler = new Handler() 
    {
       @Override
       public void handleMessage(Message msg) 
       {      
    	   int events = 0;
    	   
           switch (msg.what) 
           {
           case INIT_DATACONNECTED:
        	   startedData = true;
	           break;  
           case INIT_SIGNALSTRENGTH:
        	   startedSignal = true;
	           break;  
           case INIT_CELLLOCATION:
        	   startedLocation = true;
	           break;  
           default:  
           	break;
           }
           
           // build mask of events to listen to
           if (startedData == true)
        	   events |= PhoneStateListener.LISTEN_DATA_CONNECTION_STATE;
           if (startedSignal == true)
        	   events |= PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;
           if (startedLocation == true)
        	   events |= PhoneStateListener.LISTEN_CELL_LOCATION;
        	   
           // register listener now
           tm.listen(cellhandler, events);
       }
    };

	
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
		int strength, signal;
	
		// signal strength in ASU
		strength = signalStrength.getGsmSignalStrength();
		
		// convert ASU in dBm
		signal = -113 + 2*strength;
		
		// has anything changed?
		if (signal != oldcellStrength)
		{
			// convert ASU into bars
			if (strength <= 2 || strength == 99) 
				oldcellStrength_bar = 0;
			else 
				if (strength >= 12) 
					oldcellStrength_bar = 4;
				else 
					if (strength >= 8) 
						oldcellStrength_bar = 3;
					else 
						if (strength >= 5) 
							oldcellStrength_bar = 2;
						else oldcellStrength_bar = 1;
			
			oldcellStrength = signal;
			
			// stuff can be read now
			signal_read = true;
			bar_read = true;

			// release semaphores
			signal_semaphore.release();
			bar_semaphore.release();
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
	    
	    try
	    {
			String networkOperator = tm.getNetworkOperator();
		    if (networkOperator != null) 
		        mcc = Integer.parseInt(networkOperator.substring(0, 3));
		    else
		    	mcc = 0;
	    }catch(Exception e)
	    {
	    	mcc = 0;
	    }

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

