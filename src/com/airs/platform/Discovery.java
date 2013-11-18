/*
Copyright (C) 2005-2006 Nokia Corporation
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
package com.airs.platform;

import com.airs.helper.SerialPortLogger;

/**
 * Class to handle discovery requests sent by remote application server
 * Implements a {@link Callback} for the PUBLISH methods being created
 */
public class Discovery implements Callback, Runnable
{
	private EventComponent 	current_EC;
	private DIALOG_INFO		dialog=null;
	private int 			polltime=15000;
	private String			TO = new String("REMONT AS");
	private String  		event_name = new String("available");
	
	private void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}

	/**
	 * Sleep function 
	 * @param millis
	 */
	private void sleep(long millis) 
	{
		try 
		{
			Thread.sleep(millis);
		} 
		catch (InterruptedException ignore) 
		{
		}
	}

	/**
	 * Constructor, starting the internal discovery thread
	 * @param current_EC Reference to the {@link EventComponent} that instantiates this component
	 */
	public Discovery(EventComponent current_EC)
	{
		this.current_EC = current_EC;
		
		// start receiving thread in EventComponent
		new Thread(this).start();
	}
	
	/** 
	 * Runnable thread of this class for local discovery of sensors and generating PUBLISH methods
	 */
	public void run() 
	{
		Sensor discovery_old, current, found;
		boolean discover_changed;
		int  i;
		char EOL;
		StringBuffer discovery_data;

		// form new line characters
		EOL=13;
		// sleep until connected to application server -> event component will signal this!
		while(current_EC.connected == false)
			sleep(1000);
		
		while(true)
		{
			debug("Discovery::run: start Discovery");
			// copy old list
			discovery_old = SensorRepository.root_sensor;
			// start new list
			SensorRepository.deleteSensor();
		    // run through all handlers and discover locally first
		    for (i=0; i<HandlerManager.max_handlers;i++)
		    {
		        // is there any handler entry?
		        if (HandlerManager.handlers[i] != null)
		            // call discovery function of handler 
		            HandlerManager.handlers[i].Discover();
		    }
		    
		    discover_changed = false;
		    
		    // now determine if new sensors are old sensors
		    current = discovery_old;
		    while(current!=null)
		    {
		    	// can old sensor still be found in new ones?
		    	if ((found = SensorRepository.findSensor(current.Symbol))!=null)
		    		current.discovered = found.discovered = true;
		    	else
		    		discover_changed = true;
		    	current = current.next;
		    }
		   
		    // if all old ones are found, let's see if there are any new ones?
		    if (discover_changed == false)
		    {
		    	current = SensorRepository.root_sensor;
		    	while(current != null)
		    	{
		    		// something discovered that was not in old discovery?
		    		if (current.discovered == false)
		    			discover_changed = true;
		    		current=current.next;
		    	}
		    }

		    // now run through list for garbage collection
		    current = discovery_old;
		    while(current!=null)
		    {
		    	found = current.next;
		    	// dereference for garbage collection
		    	current = null;
		    	current = found;
		    }
		    
		    // did anything change -> publish!
		    if (discover_changed == true)
		    {
			    debug("Discovery::run: ran through all handlers -> read Repository now!");
			    discovery_data = new StringBuffer();
			    i= 0 ;
			    current = SensorRepository.root_sensor;
			    while(current != null)
			    {
			        discovery_data.append(current.Symbol + "::" + current.Description + "::" + current.Unit + "::" + current.type + "::" + current.scaler + "::" + current.min + "::" + current.max);
			        discovery_data.append(EOL);
			        current = current.next;
			        i++;
			    }
	
			    // if there's no dialog -> create one
				debug("Discovery::run: send out PUBLISH with " + i + " sensors");
				if (dialog == null)
					dialog=current_EC.Publish(TO.getBytes(), event_name.getBytes(), discovery_data.toString().getBytes(), (int)polltime/1000 + 5, this);
				else
					current_EC.Publish(dialog, TO.getBytes(), event_name.getBytes(), discovery_data.toString().getBytes(), (int)polltime/1000 + 5);					
		    }
		    else
		    	debug("Discovery::there is no change in the sensors -> No PUBLISH!");

			sleep(polltime);
		}
	}
	 
	/**
	 * Callback function for CONFIRMs of the (discovery) publications
	 * @param dialog Reference to the {@link DIALOG_INFO} for the CONFIRM dialog
	 */
	public void callback(DIALOG_INFO dialog)
	{
		debug("Discovery::callback:received method");
		debug("...FROM  : " + new String(dialog.current_method.FROM.string));
		debug("...TO	 : " + new String(dialog.current_method.TO.string));

		// what method type??
		switch(dialog.current_method.method_type)
		{
		case method_type.method_CONFIRM:
			// set state rightin order to send further NOTIFYs
			dialog.dialog_state = dialog_state.PUBLICATION_VALID;
			debug("...it's a CONFIRM - doing nothing");
			// unlock dialog -> do not forget otherwise the dialog becomes unusuable!
			dialog.locked = false;								
			break;
		default:
			// unlock dialog -> do not forget otherwise the dialog becomes unusuable!
			dialog.locked = false;
			debug("...there is another method - shouldn't happen");
		}
		
	}	
}
