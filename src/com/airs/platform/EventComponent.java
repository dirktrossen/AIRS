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

import android.widget.Toast;

import com.airs.AIRS_remote;
import com.airs.helper.SerialPortLogger;

/**
 * @author trossen
 *
 */
/**
 * @author trossen
 * @date Nov 2, 2004
 * 
 * Purpose: 
 */
public class EventComponent implements Runnable 
{
	private DIALOG_INFO incoming=null, outgoing=null;
	private EVENT_UA	event_ua=null;
	private TCPClient	current_TCPClient=null;
	private short		dialog_id= 0;
	private Thread 		thread = null;
	public  AIRS_remote  airs;
	public  boolean		connected = false;
	
	protected static void debug(String msg) 
	{
		SerialPortLogger.debug(msg);
	}

	/**
	 * Sleep function 
	 * @param millis
	 */
	protected static void sleep(long millis) 
	{
		try 
		{
			Thread.sleep(millis);
		} 
		catch (InterruptedException ignore) 
		{
		}
	}

	/***********************************************************************
	 Function    : EventComponent()
	 Input       : 
	 Output      :
	 Return      :
	 Description : constructor of class, creates TCPClient and starts own thread
	***********************************************************************/
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

	/***********************************************************************
	 Function    : run()
	 Input       : 
	 Output      :
	 Return      :
	 Description : Runnable thread of this class for receiving methods
	***********************************************************************/
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
		        	Toast.makeText(airs, "It seems that the TCP connection broke. It is recommended to restart the gateway since the server might be temporarily down!", Toast.LENGTH_LONG).show();
		        SerialPortLogger.debugForced("CONNECTION FAILURE: Restart gateway!");
		        return;
			}
		}
	}
	
	/***********************************************************************
	 Function    : stop()
	 Input       : 
	 Output      :
	 Return      :
	 Description : stop EventComponent
	***********************************************************************/
	public void stop() 
	{
		// signal thread that being disconnected -> will disconnect TCP Client and show alerter
		if (thread!=null)
			thread.interrupt();
		current_TCPClient.disconnect();
	}
	
	/***********************************************************************
	 Function    : registerEventServer()
	 Input       : pointer to callback for this event server, pointer to 
	               event name
	 Output      :
	 Return      : 'true' if successful
	 Description : registers event server UA with event delivery component
	***********************************************************************/
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


	/***********************************************************************
	 Function    : Subscribe()
	 Input       : pointer to server, pointer to event name, pointer to 
				   event body, body length, pointer to callback function
	 Output      :
	 Return      : pointer to dialog info
	 Description : Establishes subscription dialog
	***********************************************************************/
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

	/***********************************************************************
	 Function    : Notify()
	 Input       : pointer to dialog, event body and length, and callback to 
	               handle confirmations
	 Output      :
	 Return      : 'true' if successful
	 Description : sends notification in subscription dialog
	***********************************************************************/
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

	/***********************************************************************
	 Function    : Publish()
	 Input       : pointer to method struct and callback to handle confirmations
	 Output      :
	 Return      : 'true' if successful
	 Description : establishes publication dialog
	***********************************************************************/
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


	/***********************************************************************
	 Function    : Publish()
	 Input       : pointer to method struct and callback to handle confirmations
	 Output      :
	 Return      : 'true' if successful
	 Description : establishes publication dialog
	***********************************************************************/
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

	/***********************************************************************
	 Function    : Confirm()
	 Input       : pointer to method struct, pointer to return code
	 Output      :
	 Return      : 'true' if successful
	 Description : sends confirmation outside of any existing dialog, using 
	               the information from the old method
	***********************************************************************/
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

	/***********************************************************************
	 Function    : Confirm()
	 Input       : pointer to dialog struct, pointer to return code
	 Output      :
	 Return      : 'true' if successful
	 Description : sends confirmation within existing dialog
	***********************************************************************/
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

	/***********************************************************************
	 Function    : Bye()
	 Input       : pointer to method struct and callback to handle confirmations
	 Output      :
	 Return      : 'true' if successful
	 Description : terminates existing dialog
	***********************************************************************/
	synchronized public boolean Bye(DIALOG_INFO dialog, Callback callback)
	{
		return true;
	}

	/***********************************************************************
	 Function    : Dispatch()
	 Input       : 
	 Output      :
	 Return      : 'true' if successful
	 Description : dispatches incoming method
	***********************************************************************/
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

	/***********************************************************************
	 Function    : create_Dialog()
	 Input       : pointer to current method, pointer to current callback,
				   state, boolean for direction
	 Output      :
	 Return      : pointer to new dialog
	 Description : creates new dialog
				   for incoming dialogs, we'll take the dialog id that came 
				   with the method
				   for outgoing dialogs, we create a dialog id bound to the
				   TCP client
				   Hence, the dialog id is always set by the client (subscriber
				   or publisher)!
	***********************************************************************/
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

	/***********************************************************************
	 Function    : delete_Dialog()
	 Input       : pointer to dialog, boolean for direction
	 Output      :
	 Return      : 
	 Description : deletes dialog information
	***********************************************************************/
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

	/***********************************************************************
	 Function    : find_Dialog()
	 Input       : dialog identifier, boolean for direction (true=incoming)
	 Output      :
	 Return      : DIALOG_INFO that matches dialog_id
	 Description : finds dialog information that matches dialog_id
	***********************************************************************/
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
	
	/***********************************************************************
	 Function    : find_EventUA()
	 Input       : event name 
	 Output      :
	 Return      : pointer to callback
	 Description : finds callback for event name -> used for incoming subscription
	***********************************************************************/
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
