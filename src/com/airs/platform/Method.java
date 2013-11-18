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


/**
 * Return codes for CONFIRM methods
 */
class Ret_Codes
{
	static final String RC_200_OK			= new String("200 OK");
	static final String RC_400_BAD_REQUEST	= new String("400 Bad Request");
	static final String RC_404_NOT_FOUND	= new String("404 Not Found");
	static final String RC_489_BAD_EVENT	= new String("489 Bad Event");
	static final String RC_4xx_BAD_STATE	= new String("4xx Bad State");
}

/**
 * Dialog_state
 */
class dialog_state
{
	static final short	SUBSCRIPTION_PENDING			= 0;
	static final short	SUBSCRIPTION_TERMINATED_CLIENT	= 1;
	static final short	SUBSCRIPTION_TERMINATED_SERVER	= 2;
	static final short	SUBSCRIPTION_VALID				= 3;
	static final short	NOTIFICATION_SENT				= 4;
	static final short	PUBLICATION_PENDING				= 5;
	static final short	PUBLICATION_VALID				= 6;
}

/**
 *  definition of an octet string to be transferred over the wire 
 */
class OctetString
{
	public short		length=0;
	public byte			string[]=null;
}

/**
 * definition of an octet string with long int of length to be transferred over the wire 
 */
class lOctetString
{
	public int			length=0;
	public byte			string[]=null;
}

/**
 * Definition of method_type 
 */
class method_type
{
	static final short	method_SUBSCRIBE		= 0;
	static final short	method_NOTIFY			= 1;
	static final short	method_PUBLISH			= 2;
	static final short	method_CONFIRM			= 3;
	static final short	method_BYE				= 4;
}

/**
 * Definition of confirm_type 
 */
class confirm_type
{
	static final short	confirm_PUBLISH			= 0;
	static final short	confirm_OTHERS			= 1;
}

/**
 * Structure of a SUBSCRIBE
 */
class SUBSCRIBE
{
	public short 		dialog_id;
	public short 		CSeq;
	public int			Expires;
}

/**
 * Structure of a NOTIFY
 */
class NOTIFY
{
	public short 		dialog_id;
	public short		CSeq;
}

/**
 * Structure of a PUBLISH
 */
class PUBLISH
{
	public int		e_tag;
	public int		Expires;
}

/**
 * Structure of a CONFIRM
 */
class CONFIRM
{
	public short		confirm_type;
	class dialog
	{
		public short		dialog_id;		// to be used in dialogs
		public short		CSeq;			// to be used in dialogs
	}
	public dialog		dialog;
	public int			e_tag;			// to be used in PUBLISH only!
	public int			Expires;
	public OctetString	ret_code;
	
	CONFIRM()
	{
		dialog = new dialog();
		ret_code = new OctetString();
	}
}

/**
 * Structure of a BYE
 */
class BYE
{
	public short		dialog_id;
	public short		CSeq;
	public OctetString	reason;
	BYE()
	{
		reason = new OctetString();
	}
}

/**
 * Structure of a general Method (which can be either SUBSCRIBE, PUBLISH, CONFIRM or BYE)
 */
class Method 
{
	public short		method_type;
	public OctetString	FROM;
	public OctetString	TO;
	public OctetString	event_name;
	// now the method-specific entries
	public SUBSCRIBE	sub;
	public NOTIFY		not;
	public PUBLISH		pub;
	public CONFIRM		conf;
	public BYE			BYE;
	// eventually the body
	public lOctetString	event_body;
	
	Method()
	{
		FROM	= new OctetString();
		TO		= new OctetString();
		event_name 	= new OctetString();
		sub = new SUBSCRIBE();
		not = new NOTIFY();
		pub = new PUBLISH();
		conf = new CONFIRM();
		BYE = new BYE();
		event_body  = new lOctetString();
	}
}

/**
 * Structure that holds a specific dialog information
 *
 */
class DIALOG_INFO
{
	public Method			current_method;		// current method of dialog
	public Callback			callback;			// callback used
	public QueryResolver    query;				// associated query if it is an acquisition, otherwise NULL
	public OctetString		peer;				// peer of the connection
	public short			dialog_state;		// state of the dialog
	public short			CSeq;				// CSeg has been tranferred out to make programming easier
	public short 			dialog_id;			// dialog_id hsa been transferred out to make programming easier
	public boolean 			locked;				// if true, other operations will be blocked
	public DIALOG_INFO		next;
	DIALOG_INFO()
	{
		query = null;
		current_method=null;
		peer = new OctetString();
		next = null;
	}
}

/**
 * Structure that holds information about the user agent, specifically the event it serves and the callback it provides
 *
 */
class EVENT_UA
{
	EVENT_UA() 
	{
			this.event_name = new byte[20];
	}
	public byte		event_name[];
	public Callback	callback;
	public EVENT_UA	next;
}
