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

import android.os.Bundle;
import android.os.Message;

import com.airs.AIRS_remote;
import com.airs.helper.SerialPortLogger;


/**
 * Class that handles all incoming {@link Method} types 
 */
public class EventComponent implements Runnable 
{
	private DIALOG_INFO incoming=null, outgoing=null;
	private EVENT_UA	event_ua=null;
	private TCPClient	current_TCPClient=null;
	private short		dialog_id= 0;
	private Thread 		thread = null;
	/**
	 * Reference to {@link AIRS_remote} service
	 */
	public  AIRS_remote  airs;
	/**
	 * Flag if connected (true) or not (false)
	 */
	public  boolean		connected = false;
	
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
	 * Start the EventComponent after being instantiated
	 * @param airs Reference to the {@link AIRS_remote} service which started this component
	 * @param IPAddress String with the IP address to connect to 
	 * @param IPPort Port to connect to at the given IP address
	 */
	public boolean startEC(AIRS_remote airs, String IPAddress, String IPPort)
	{
		current_TCPClient	= new TCPClient();
		// was connection successful?
		if (current_TCPClient.startTCP(airs, IPAddress, IPPort) == false)
			return false;
		
		connected = true;
		
		this.airs = airs;
		
		// start receiving thread in EventComponent
		thread = new Thread(this);
		thread.start();
		
		return true;
	}

	/**
	 * Runnable thread of this class for receiving methods from remote application server
	 */
	public void run() 
	{
		Method current_method;
		// keep on running
		while (connected == true) 
		{
			while(current_TCPClient.connected == false)
				sleep(250);
			try 
			{
				// signal connection to Acquisition and Discovery
				connected = true;
				// read method from TCP connection
				current_method = current_TCPClient.read();
				// if one came in -> dispatch
				if (Dispatch(current_method)==false)
					debug("Error in dispatching incoming method");
			} 
			catch (Exception e) 
			{
		        current_TCPClient.disconnect();
		        if (connected == true)
		        {
			        Message msg = airs.mHandler.obtainMessage(AIRS_remote.SHOW_NOTIFICATION);
			        Bundle bundle = new Bundle();
			        bundle.putString(AIRS_remote.TEXT, "It seems that the TCP connection broke. It is recommended to restart the gateway since the server might be temporarily down!");
					msg.setData(bundle);
			        airs.mHandler.sendMessage(msg);

		        }
		        SerialPortLogger.debugForced("CONNECTION FAILURE: Restart gateway!");
		        return;
			}
		}
	}
	
	/**
	 * Stop the EventComponent
	 */
	public void stop() 
	{
		// signal thread that being disconnected -> will disconnect TCP Client and show alerter
		if (thread!=null)
			thread.interrupt();
		current_TCPClient.disconnect();
	}
	
	/**
	 * Register an event server with a given Callback for a given event name
	 * @param callback Reference to the {@link Callback} for this event
	 * @param event_name_string String for the event name being registered
	 * @return true, if successful
	 */
	synchronized public boolean registerEventServer(Callback callback, String event_name_string)
	{
		EVENT_UA	new_UA = null;
		byte		event_name[];
		
		event_name = event_name_string.getBytes();
		
		if (find_EventUA(event_name)!=null)
		{
			debug("EventComponent::registerEventServer(): event name already registered!");
			return false;
		}
		
		// create new event UA structure
		new_UA = new EVENT_UA();
		
		// fill in the fields
		new_UA.event_name	=	event_name;
		new_UA.callback		= 	callback;
		new_UA.next			= 	null;
		
		// now insert in list
		// first one?
		if (event_ua == null)
			event_ua = new_UA;
		else
		{
			new_UA.next		= event_ua;
			event_ua		= new_UA;
		}
		
		return true;
	}


	/**
	 * Establishes a SUBSCRIBE dialog
	 * @param TO byte array with the name to send this to (usually 'application server')
	 * @param event_name byte array with the event name being subscribed to
	 * @param event_body byte array with the event body
	 * @param body_length length of the event body array
	 * @param Expires time to expire in milliseconds
	 * @param callback Reference to the {@link Callback} for this dialog 
	 * @return DIALOG_INFO of the established dialog
	 */
	synchronized public DIALOG_INFO Subscribe(byte TO[], byte event_name[], byte event_body[], int body_length, int Expires, Callback callback)
	{
		Method	  method = new Method();
		DIALOG_INFO	new_dialog=null;

		// not connected?
		if (current_TCPClient.connected == false)
			return null;
		// fill fields of methods
		method.method_type	= method_type.method_SUBSCRIBE;
		method.FROM.string 	= current_TCPClient.IMEI.getBytes();
		method.FROM.length 	= (short)current_TCPClient.IMEI.length();
		method.TO.string	= TO;
		method.TO.length	= (short)TO.length;
		method.event_name.string	= event_name;
		method.event_name.length	= (short)event_name.length;
		method.event_body.string	= event_body;
		method.event_body.length	= event_body.length;
		method.sub.Expires 			= Expires;
		// create new dialog for this subscription
		if ((new_dialog = create_Dialog(method, callback, dialog_state.SUBSCRIPTION_PENDING, false)) != null)
		{
			// try to send the method
			if (current_TCPClient.write(method) == true)
				return new_dialog;
			else
				delete_Dialog(new_dialog, false);
		}
		else
			debug("EventComponent::Subscribe(): cannot allocate memory for Method");

		return null;
	}

	/**
	 * Sends notification in subscription dialog
	 * @param dialog_id identifier for the dialog being used
	 * @param event_body byte array with the event body being sent
	 * @param callback Reference to the {@link Callback} being notified upon completion
	 * @return true, if successful
	 */
	synchronized public boolean Notify(short dialog_id, byte event_body[], Callback callback)
	{
		DIALOG_INFO	current_dialog;

		// not connected?
		if (current_TCPClient.connected == false)
			return false;

		debug("EventComponent::Notify:invoked NOTIFY");
		
		if ((current_dialog = find_Dialog(dialog_id, true))!=null)
		{
			// only allow to send if subscription is valid or another notification has been sent (which means that subscription is valid) 
			if ((current_dialog.dialog_state == dialog_state.SUBSCRIPTION_VALID)
			 ||(current_dialog.dialog_state == dialog_state.NOTIFICATION_SENT))
			{
				// wait until the dialog is unlocked!!
				while (current_dialog.locked == true)
					sleep(100);
	
				// if dialog is terminated now, return (might be incoming BYE!)
				if (current_dialog.dialog_state == dialog_state.SUBSCRIPTION_TERMINATED_CLIENT)
					return false;
				
				// lock dialog!
				current_dialog.locked = true;
				
				// set method type to NOTIFY
				debug("...set method to NOTIFY");
				current_dialog.current_method.method_type	= method_type.method_NOTIFY;
		
				// fill in FROM and TO
				debug("...fill in FROM and TO");
				current_dialog.current_method.FROM.string 	= current_TCPClient.IMEI.getBytes();
				current_dialog.current_method.FROM.length 	= (short)current_TCPClient.IMEI.length();
				current_dialog.current_method.TO.string   	= current_dialog.peer.string;
				current_dialog.current_method.TO.length	= current_dialog.peer.length;
		
				// fill in event body
				debug("...fill in event body");
				current_dialog.current_method.event_body.string	= event_body;
				current_dialog.current_method.event_body.length	= event_body.length;
				
				// copy dialog_id and increase CSeq
				current_dialog.current_method.not.dialog_id	 	= dialog_id;
				// always increase CSeq in subcription part of the method structure!
				current_dialog.CSeq++;
				// and now copy over to notification part of the method structure!
				current_dialog.current_method.not.CSeq			= current_dialog.CSeq; 
		
				debug("...sending NOTIFY");
				
				// track values being sent
				airs.values_sent++;
				
				// try to send the method
				if (current_TCPClient.write(current_dialog.current_method) == true)
				{
					// now set the dialog state correctly
					current_dialog.dialog_state					= dialog_state.NOTIFICATION_SENT;
					// free dialog
					current_dialog.locked = false;
					debug("...done");
					return true;
				}
				else
				{
					// free dialog
					current_dialog.locked = false;
					debug("EventComponent::Notify(): Error in sending method");
				}
			}
			else
				debug("EventComponent::Notify(): wrong dialog state");
		}
		else
			debug("EventComponent::Notify(): cannot find dialog for this dialog_id");

		return false;
	}

	/**
	 * Establishes a new PUBLISH dialog
	 * @param TO byte array with the name to send this to (usually 'application server')
	 * @param event_name byte array with the event name being subscribed to
	 * @param event_body byte array with the event body
	 * @param Expires time to expire in milliseconds
	 * @param callback Reference to the {@link Callback} for this dialog 
	 * @return DIALOG_INFO of the established dialog
	 */
	synchronized public DIALOG_INFO Publish(byte TO[], byte event_name[], byte event_body[], int Expires, Callback callback)
	{
		Method	  method = new Method();
		DIALOG_INFO	new_dialog = null;

		// not connected?
		if (current_TCPClient.connected == false)
			return null;

		// fill fields of methods 
		method.method_type	= method_type.method_PUBLISH;
		method.FROM.string 	= current_TCPClient.IMEI.getBytes();
		method.FROM.length 	= (short)current_TCPClient.IMEI.length();
		method.TO.string	= TO;
		method.TO.length	= (short)TO.length;
		method.event_name.string	= event_name;
		method.event_name.length	= (short)event_name.length;
		method.event_body.string	= event_body;
		method.event_body.length	= event_body.length;
		method.pub.Expires 			= Expires;
		// create new dialog for this subscription
		if ((new_dialog = create_Dialog(method, callback, dialog_state.PUBLICATION_PENDING, false)) != null)
		{
			// try to send the method
			if (current_TCPClient.write(method) == true)
				return new_dialog;
			else
				delete_Dialog(new_dialog, false);
		}
		else
			debug("EventComponent::Publish(): cannot allocate memory for Method");

		return null;
	}

	/**
	 * Sends a PUBLISH method in an existing dialog
	 * @param dialog Reference to the {@link DIALOG_INFO} that holds the dialog information to be used
	 * @param TO byte array with the name to send this to (usually 'application server')
	 * @param event_name byte array with the event name being subscribed to
	 * @param event_body byte array with the event body
	 * @param Expires time to expire in milliseconds
	 * @return true, if successful
	 */
	synchronized public boolean Publish(DIALOG_INFO dialog, byte TO[], byte event_name[], byte event_body[], int Expires)
	{
		// not connected?
		if (current_TCPClient.connected == false)
			return false;

		// fill fields of methods 
		dialog.current_method.method_type	= method_type.method_PUBLISH;
		dialog.current_method.FROM.string 	= current_TCPClient.IMEI.getBytes();
		dialog.current_method.FROM.length 	= (short)current_TCPClient.IMEI.length();
		dialog.current_method.TO.string		= TO;
		dialog.current_method.TO.length		= (short)TO.length;
		dialog.current_method.event_name.string	= event_name;
		dialog.current_method.event_name.length	= (short)event_name.length;
		dialog.current_method.event_body.string	= event_body;
		dialog.current_method.event_body.length	= event_body.length;
		dialog.current_method.pub.Expires 		= Expires;
		dialog.current_method.pub.e_tag			= (int)dialog.dialog_id;
		dialog.dialog_state						= dialog_state.PUBLICATION_PENDING;
		
		// try to send the method
		return current_TCPClient.write(dialog.current_method);
	}

	/**
	 * sends confirmation outside of any existing dialog, using the information from the old method
	 * @param method Reference to the {@link Method} being re-used
	 * @param ret_code Return code being inserted into the CONFIRM method
	 * @return true, if successful
	 */
	synchronized public boolean Confirm(Method method, String ret_code)
	{
		byte swap[];
		short swap_length;
		boolean return_code;

		// swap FROM and TO
		swap 				= method.FROM.string;
		method.FROM.string 	= method.TO.string;
		method.TO.string   	= swap;
		swap_length 		= method.FROM.length;
		method.FROM.length  = method.TO.length;
		method.TO.length	= swap_length;

		switch(method.method_type)
		{
			case method_type.method_SUBSCRIBE:
				// copy dialog identifier from original request and use sequence number plus one
				method.conf.dialog.dialog_id  = method.sub.dialog_id;
				method.conf.dialog.CSeq		  = (short)(method.sub.CSeq + 1);
				break;
			case method_type.method_NOTIFY:
				// copy dialog identifier from original request and use sequence number plus one
				method.conf.dialog.dialog_id  = method.not.dialog_id;
				method.conf.dialog.CSeq		  = (short)(method.not.CSeq + 1);
				break;
			case method_type.method_BYE:
				// copy dialog identifier from original request and use sequence number plus one
				method.conf.dialog.dialog_id  = method.BYE.dialog_id;
				method.conf.dialog.CSeq		  = (short)(method.BYE.CSeq + 1);
				break;
			case method_type.method_PUBLISH:
				// copy dialog identifier from original request and use sequence number plus one
				method.conf.e_tag  			  = method.pub.e_tag;
				break;
		}
		if (method.method_type == method_type.method_PUBLISH)
			method.conf.confirm_type	 = confirm_type.confirm_PUBLISH;
		else
			method.conf.confirm_type	 = confirm_type.confirm_OTHERS;
		method.conf.Expires		     = 0;
		method.conf.ret_code.string	 = ret_code.getBytes();
		method.conf.ret_code.length	 = (short)ret_code.length();

		// there is an error -> form method for negative confirmation
		method.method_type = method_type.method_CONFIRM;
		
		return_code = current_TCPClient.write(method);

		return return_code;
	}

	/**
	 * sends confirmation within existing dialog
	 * @param dialog_info Reference to the {@link DIALOG_INFO} of the dialog being used
	 * @param ret_code Return code being inserted into the CONFIRM method
	 * @return true, if successful
	 */
	synchronized public boolean Confirm(DIALOG_INFO dialog_info, String ret_code)
	{
		boolean return_code;

		// wait until the dialog is unlocked!!
		while (dialog_info.locked == true)
			sleep(100);
		
		debug("EventComponent::Confirm:send out confirmation");
		// fill in FROM and TO
		dialog_info.current_method.FROM.string 	= current_TCPClient.IMEI.getBytes();
		dialog_info.current_method.FROM.length 	= (short)current_TCPClient.IMEI.length();
		dialog_info.current_method.TO.string   	= dialog_info.peer.string;
		dialog_info.current_method.TO.length	= dialog_info.peer.length;

		dialog_info.CSeq++;
		switch(dialog_info.current_method.method_type)
		{
			case method_type.method_SUBSCRIBE:
				debug("EventComponent::Confirm:method is SUBSRIBE");
				// copy dialog identifier from original request and use sequence number plus one
				dialog_info.current_method.conf.dialog.dialog_id  = dialog_info.current_method.sub.dialog_id;
				dialog_info.current_method.conf.dialog.CSeq		  = dialog_info.CSeq;
				break;
			case method_type.method_NOTIFY:
				debug("EventComponent::Confirm:method is NOTIFY");
				// copy dialog identifier from original request and use sequence number plus one
				dialog_info.current_method.conf.dialog.dialog_id  = dialog_info.current_method.not.dialog_id;
				dialog_info.current_method.conf.dialog.CSeq		  = dialog_info.CSeq;
				break;
			case method_type.method_BYE:
				// copy dialog identifier from original request and use sequence number plus one
				dialog_info.current_method.conf.dialog.dialog_id  = dialog_info.current_method.BYE.dialog_id;
				dialog_info.current_method.conf.dialog.CSeq	  	  = dialog_info.CSeq;
				break;
			case method_type.method_PUBLISH:
				// copy dialog identifier from original request and use sequence number plus one
				dialog_info.current_method.conf.e_tag  = dialog_info.current_method.pub.e_tag;
				break;
		}
		if (dialog_info.current_method.method_type == method_type.method_PUBLISH)
			dialog_info.current_method.conf.confirm_type	 = confirm_type.confirm_PUBLISH;
		else
			dialog_info.current_method.conf.confirm_type	 = confirm_type.confirm_OTHERS;
			
		dialog_info.current_method.conf.Expires			 = (int)0;
		dialog_info.current_method.conf.ret_code.string	 = ret_code.getBytes();
		dialog_info.current_method.conf.ret_code.length	 = (short)ret_code.length();

		dialog_info.current_method.method_type = method_type.method_CONFIRM;

		dialog_info.current_method.event_body.string = null;
		dialog_info.current_method.event_body.length = 0;
		
		return_code = current_TCPClient.write(dialog_info.current_method);

		return return_code;
	}

	/**
	 * Terminates existing dialog
	 * @param dialog Reference to the {@link DIALOG_INFO} of the dialog to be terminated
	 * @param callback Reference to the {@link Callback} of this dialog
	 * @return true, if successful
	 */
	synchronized public boolean Bye(DIALOG_INFO dialog, Callback callback)
	{
		return true;
	}

	/**
	 * Dispatches incoming method
	 * @param Reference to the {@link Method} being dispatched
	 * @return true, if successful
	 */
	boolean Dispatch(Method current_method)
	{
		short dialog_id;
		boolean ret_value = true, direction;
		DIALOG_INFO current_dialog;
		Callback	current_callback;

		debug("EventComponent::Dispatch:dispatch newly arrived method");
		// handle different methods
		switch(current_method.method_type)
		{
		case method_type.method_SUBSCRIBE:
			debug("...it's a SUBSCRIBE");
			// is there an existing dialog?
			if ((current_dialog = find_Dialog(current_method.sub.dialog_id, true)) != null)
			{
				// is the existing dialog a subscription that is valid -> re-subscription otherwise it is an error!!!
				if ((current_dialog.dialog_state == dialog_state.SUBSCRIPTION_VALID) || (current_dialog.dialog_state == dialog_state.NOTIFICATION_SENT))
				{
					current_dialog.current_method = current_method;    			// store last received method
					current_dialog.locked = true;								// lock dialog
					current_dialog.callback.callback(current_dialog);			// and call dialog callback
				}
				else
					Confirm(current_method, Ret_Codes.RC_400_BAD_REQUEST);
			}
			else
			{
				debug("...it's outside an existing dialog");
				// try to find the event UA for the incoming method
				if ((current_callback = find_EventUA(current_method.event_name.string)) != null)
				{
					// create a new dialog for this incoming one
					if ((current_dialog= create_Dialog(current_method, current_callback, dialog_state.SUBSCRIPTION_PENDING, true))!= null)
					{
						current_dialog.locked = true;								// lock dialog
						current_callback.callback(current_dialog);
					}
				}
				else
				{
					Confirm(current_method, Ret_Codes.RC_489_BAD_EVENT);
				}
			}
			break;
		case method_type.method_NOTIFY:
			debug("...it's a NOTIFY");
			// is there an existing incoming dialog?
			if ((current_dialog = find_Dialog(current_method.not.dialog_id, false)) != null)
			{
				// has anything been sent before (SUBSCRIBE, NOTIFY, BYE, or PUBLISH)?
				if ((current_dialog.dialog_state == dialog_state.SUBSCRIPTION_VALID))
				{
					current_dialog.current_method = current_method;    // store last received method
					current_dialog.locked = true;								// lock dialog
					current_dialog.callback.callback(current_dialog);	// and call dialog callback
				}
				else
				{
					Confirm(current_method, Ret_Codes.RC_4xx_BAD_STATE);
					debug("EventComponent::Dispatch(): received NOTIFY in wrong state -> discard");
				}
			}
			else
			{
				Confirm(current_method, Ret_Codes.RC_404_NOT_FOUND);
				debug("EventComponent::Dispatch(): received NOTIFY without dislog -> discard");
			}
			break;
		case method_type.method_PUBLISH:
			debug("...it's a PUBLISH");
			break;
		case method_type.method_CONFIRM:
			debug("...it's a CONFIRM");
			if (current_method.conf.confirm_type == confirm_type.confirm_OTHERS)
			{
				dialog_id = current_method.conf.dialog.dialog_id;
				direction = true;
			}
			else
			{
				dialog_id = (short)current_method.conf.e_tag;
				direction = false;		// publication is only outgoing dialog!!
			}

			// is CONFIRM for outgoing dialog?
			if (direction==false)
			{
				// is there an existing outgoing dialog (PUBLISH)?
				if ((current_dialog = find_Dialog(dialog_id, false)) != null)
				{
					// has anything been sent before (SUBSCRIBE, NOTIFY, BYE, or PUBLISH)?
					if ((current_dialog.dialog_state == dialog_state.SUBSCRIPTION_PENDING)
					  ||(current_dialog.dialog_state == dialog_state.PUBLICATION_PENDING)
					  ||(current_dialog.dialog_state == dialog_state.SUBSCRIPTION_TERMINATED_CLIENT))
					{
						current_dialog.current_method = current_method;    // store last received method
						current_dialog.locked = true;								// lock dialog
						current_dialog.callback.callback(current_dialog);	// and call dialog callback
					}
					else
						debug("EventComponent::Dispatch(): received CONFIRM out-of-band -> discard");
				}
			}
			else  // or is it for incoming dialog
			{
				// is there an existing incoming dialog?
				if ((current_dialog = find_Dialog(dialog_id, true)) != null)
				{
					// has anything been sent before (SUBSCRIBE, NOTIFY, BYE, or PUBLISH)?
					if ((current_dialog.dialog_state == dialog_state.NOTIFICATION_SENT)
					  ||(current_dialog.dialog_state == dialog_state.SUBSCRIPTION_TERMINATED_SERVER))
					{
						current_dialog.current_method = current_method;    // store last received method
						current_dialog.locked = true;								// lock dialog
						current_dialog.callback.callback(current_dialog);	// and call dialog callback
					}
					else
						debug("EventComponent::Dispatch(): received CONFIRM out-of-band -> discard");
				}
				else
					debug("EventComponent::Dispatch(): received CONFIRM out-of-band -> discard");
			}

			break;
		case method_type.method_BYE:
			debug("...it's a BYE");
			// is there an existing incoming dialog?
			if ((current_dialog = find_Dialog(current_method.BYE.dialog_id, true)) != null)
			{
				debug("...found dialog for it");
				while (current_dialog.locked == true)
					sleep(100);
				// lock dialog
				current_dialog.locked = true;
					
				// has anything been sent before (SUBSCRIBE, NOTIFY, BYE, or PUBLISH)?
				if ((current_dialog.dialog_state == dialog_state.SUBSCRIPTION_VALID)
				  ||(current_dialog.dialog_state == dialog_state.PUBLICATION_VALID)
				  ||(current_dialog.dialog_state == dialog_state.NOTIFICATION_SENT))
				{
					debug("...call callback for BYE method");
					current_dialog.current_method = current_method;    // store last received method
					current_dialog.callback.callback(current_dialog);	// and call dialog callback
					delete_Dialog(current_dialog, true);				// delete dialog from list
				}
				else
					debug("EventComponent::Dispatch(): received BYE out-of-band -> discard");
			}
			else
				debug("EventComponent::Dispatch(): received BYE out-of-band -> discard");
			break;
		default:	// shouldn't come though
			ret_value = false;
			break;
		}
		return ret_value;
	}

	/**
	 * creates new dialog: 
	 * for incoming dialogs, we'll take the dialog id that came with the method
	 * for outgoing dialogs, we create a dialog id bound to the TCP client
	 * Hence, the dialog id is always set by the client (subscriber or publisher)!
	 * @param method Reference to the {@link Method} that creates the dialog
	 * @param callback Reference to the {@link Callback} for this dialog
	 * @param state initial state of the dialog
	 * @param direction true, if outgoing, false if incoming
	 * @return Reference to a {@link DIALOG_INFO} that has been created for this new dialog
	 */
	DIALOG_INFO create_Dialog(Method method, Callback callback, short state, boolean direction)
	{
		DIALOG_INFO new_insert = new DIALOG_INFO();

		// copy fields
		new_insert.current_method 	= method;
		// if incoming dialog -> FROM is peer
		if (direction==true)
			new_insert.peer.string		= method.FROM.string;
		else
			new_insert.peer.string		= method.TO.string;
		new_insert.peer.length			= (short)new_insert.peer.string.length;
		
		new_insert.callback	   		= callback;
		new_insert.next		   		= null;
		new_insert.dialog_state	   	= state;
		new_insert.CSeq				= (short)0;

		// insert in different queue, depending on direction
		if (direction == true)
		{
			// copy dialog id
			if (method.method_type == method_type.method_SUBSCRIBE)
				new_insert.dialog_id = method.sub.dialog_id;
			else
				new_insert.dialog_id = (short)method.pub.e_tag;
			
			if (incoming != null)
			{
				new_insert.next	= incoming;
				incoming 	   	= new_insert;
			}
			else
				incoming = new_insert;
		}
		else
		{
			// create new dialog id for this client, note that only SUBSCRIBE and PUBLISH can create dialogs!!
			dialog_id++;
			new_insert.current_method.sub.dialog_id = dialog_id;
			new_insert.current_method.sub.CSeq		= 0;
			new_insert.current_method.pub.e_tag	    = (int)dialog_id;
			new_insert.dialog_id					= dialog_id;
			
			if (outgoing != null)
			{
				new_insert.next 	= outgoing;
				outgoing  	   		= new_insert;
			}
			else
				outgoing = new_insert;
		}

		return new_insert;
	}

	/**
	 * Deletes dialog information
	 * @param dialog Reference to the {@link DIALOG_INFO} being deleted
	 * @param direction Direction of the dialog with true for outgoing, false for incoming
	 */
	void delete_Dialog(DIALOG_INFO dialog, boolean direction)
	{
		DIALOG_INFO	last, search;

		// for incoming dialogs?
		if (direction == true)
			last=search = incoming;
		else
			last=search = outgoing;

		while(search != null)
		{
			// found dialog?
			if (search==dialog)
			{
				// is dialog the first one?
				if (last == search)
				{
					if (direction == true)
						incoming = incoming.next;
					else
						outgoing = outgoing.next;
				}
				else
					last.next = search.next;
				
				return;
			}
			else
			{
				last = search;
				search = search.next;
			}
		}
	}

	/**
	 * Finds dialog information that matches dialog_id
	 * @param dialog_id identifier of the dialog to be searched for
	 * @param direction Direction of the dialog with true for outgoing, false for incoming
	 * @return Reference to the {@link DIALOG_INFO} being found
	 */
	DIALOG_INFO	find_Dialog(short dialog_id, boolean direction)
	{
		DIALOG_INFO	search;

		// for incoming dialogs?
		if (direction == true)
			search = incoming;
		else
			search = outgoing;

		while(search!=null)
		{
			// dialog identifier is stored in either subscription, notification or confirmation method
			// but look for matching TCP clients since dialog_ids are only unique for particular TCP client
			if (search.dialog_id == dialog_id)
				return search;
			search = search.next;
		}
		return null;		
	}
	
	/**
	 * Finds callback for event name -> used for incoming subscription
	 * @param event_name byte array of the event name whose UA is to be found
	 * @return Reference to {@link Callback}
	 */
	Callback find_EventUA(byte event_name[])
	{
		EVENT_UA	search = event_ua;
		int i, length = event_name.length;
		boolean found;

		while(search != null)
		{
			// string comparison is done here!
			// is length of names already the same??
			if (length == search.event_name.length)
			{
				// mark as found
				found = true;
				for (i = 0; i<length ; i++)
					if (event_name[i]!=search.event_name[i])
						found=false;  // did not match -> mark as not found
				// if found -> return callback
				if (found == true)	
					return search.callback;
			}
			search = search.next;
		}

		return null;
	}

}
