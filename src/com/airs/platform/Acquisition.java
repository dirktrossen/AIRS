/*
Copyright (C) 2004-2006 Nokia Corporation
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
 * 
 * Class to handle acquisition requests that arrive from remote application server
 * Implements a {@link Callback} for the CONFIRM methods being created
 */
public class Acquisition implements Callback
{
	private EventComponent current_EC;
	private String Auth = "no init";
    private int confirm;
	
	protected static void debug(String msg) 
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
	 * Constructor
	 * @param current_EC Reference to {@link EventComponent} that is instantiating the class
	 */
	public Acquisition(EventComponent current_EC)
	{
		this.current_EC = current_EC;
		// get display for confirmation
		
		// register acquisition event server
		if (this.current_EC.registerEventServer(this, "acquire")==false)
			debug("Acquisition::Acquisition(): failure in registering 'acquire' event");

		// initialize confirmation procedure
		initConfirmation();
	}
	
	
	/**
	 * Callback for notifications that arrive from remote application server
	 * @param dialog Reference to {@link DIALOG_INFO} that holds the notification info
	 */
	public void callback(DIALOG_INFO dialog)
	{
		QueryResolver query = null;
		int positive = 0;
		
		debug("Acquisition::callback:received method");
		debug("...FROM  : " + new String(dialog.current_method.FROM.string));
		debug("...TO	 : " + new String(dialog.current_method.TO.string));

		// what method type??
		switch(dialog.current_method.method_type)
		{
		case method_type.method_SUBSCRIBE:
			debug("...event : " + new String(dialog.current_method.event_name.string));
			debug("...body  : " + new String(dialog.current_method.event_body.string));

			if (QueryResolver.s_parse(dialog.current_method.event_body.string) != null)
			{
				// need to prepare confirmation requester?
				if (Auth.compareTo("On") == 0)
				{
					askConfirmation(new String(dialog.current_method.event_body.string, 0, dialog.current_method.event_body.length));
				}
				
		        if (confirm == 0)
		        {
					// create query for this subscription (thread is not yet started with this!!)
					query = new QueryResolver(current_EC, this, dialog.dialog_id, dialog.current_method.event_body.string);
					
					// accept subscription and set state right!
					dialog.dialog_state = dialog_state.SUBSCRIPTION_VALID;
					debug("...and send back CONFIRM with '200 OK'");
					// save query in doalog information for later
					dialog.query = query;
					// unlock dialog -> do not forget otherwise the dialog becomes unusuable!
					dialog.locked = false;								
					// send CONFIRM
					current_EC.Confirm(dialog, Ret_Codes.RC_200_OK);
					// now start query thread after positive confirmation has been sent for this query! -> shoots off NOTIFYs
					new Thread(query).start();
					
					positive = 0;
		        }
		        else
		        	positive = 3;
			}
			else
				positive = 2;
			
			// negative confirmation?
			if (positive != 0)
			{
				// reject subscription and set state right!
				dialog.dialog_state = dialog_state.SUBSCRIPTION_TERMINATED_SERVER;
				debug("...and send back CONFIRM with negative result");
				// unlock dialog -> do not forget otherwise the dialog becomes unusuable!
				dialog.locked = false;
				// send CONFIRM
				switch (positive)
				{
				case 1:
					current_EC.Confirm(dialog, Ret_Codes.RC_400_BAD_REQUEST);
					break;
				case 2: 
					current_EC.Confirm(dialog, Ret_Codes.RC_404_NOT_FOUND);
					break;
				case 3 : 
					current_EC.Confirm(dialog, Ret_Codes.RC_400_BAD_REQUEST);
					break;
				}
			}
			break;
		case method_type.method_CONFIRM:
			// set state right in order to send further NOTIFYs
			// get return code as string
			String ret_code = new String(dialog.current_method.conf.ret_code.string);
			// positive CONFIRM?
			if (ret_code.equals("200 OK"))
			{
				dialog.dialog_state = dialog_state.SUBSCRIPTION_VALID;
				debug("...it's a positive CONFIRM - doing nothing");
			}
			else
			{
				debug("...it's a CONFIRM with code '" + ret_code + "' -> tearing down later");
			}
			// unlock dialog -> do not forget otherwise the dialog becomes unusuable!
			dialog.locked = false;								
			break;
		case method_type.method_BYE:
			// set state right in order to send further NOTIFYs
			debug("...it's a BYE -> terminating dialog " + dialog.dialog_id);
			// set state to terminated to avoid future NOTIFYs
			dialog.dialog_state = dialog_state.SUBSCRIPTION_TERMINATED_CLIENT;
			// terminate query
			debug("...terminate query");
			dialog.query.terminate = true;
			// unlock dialog -> do not forget otherwise the dialog becomes unusuable!
			dialog.locked = false;								
			// send positive CONFIRM
			debug("...send confirm back");
			current_EC.Confirm(dialog, Ret_Codes.RC_200_OK);
			break;
		default:
			// unlock dialog -> do not forget otherwise the dialog becomes unusuable!
			dialog.locked = false;
			debug("...there is another method - shouldn't happen");
		}
	}

	synchronized private void askConfirmation(String query)
	{    
        confirm = -1;
        // wait for user input
        while(confirm == -1)
        	sleep(200);		       
 	}

	synchronized private void initConfirmation()
	{
	}
}
