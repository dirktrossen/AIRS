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

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * 
 * @author declanshanaghy
 * http://blog.350nice.com/wp/archives/240
 * MultiChoice Preference Widget for Android
 *
 */
public class ListPreferenceMultiSelect extends ListPreference 
{
	private String separator = "::";
	private boolean[] mClickedDialogEntryIndices;
	
	// Constructor
	public ListPreferenceMultiSelect(Context context, AttributeSet attrs) 
	{
        super(context, attrs);
        mClickedDialogEntryIndices = new boolean[getEntries().length];
    }
	
	@Override
    public void setEntries(CharSequence[] entries) 
	{
    	super.setEntries(entries);
    	if (entries.length != 0)
	        mClickedDialogEntryIndices = new boolean[entries.length];
    }
    
    public ListPreferenceMultiSelect(Context context) 
    {
        this(context, null);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) 
    {
    	CharSequence[] entries = getEntries();
    	CharSequence[] entryValues = getEntryValues();
    	
    	// are there any entries?
        if (entries == null || entryValues == null || entries.length != entryValues.length ) 
        {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array of equal length");
        }

        restoreCheckedEntries();
        builder.setMultiChoiceItems(entries, mClickedDialogEntryIndices, 
                new DialogInterface.OnMultiChoiceClickListener() 
        		{
					public void onClick(DialogInterface dialog, int which, boolean val) 
					{
						mClickedDialogEntryIndices[which] = val;
					}
        		});
    }
        
    private void restoreCheckedEntries() 
    {
    	int i, j;
    	// get stored key
    	String value = getValue();
    	if (value == null)
    		return;
    	
    	CharSequence[] entryValues = getEntryValues();
    	
    	// Explode the string read in sharedpreferences
    	String[] vals = value.split(separator);
    	
    	if ( vals.length != 0 ) 
        	for (i=0; i<entryValues.length; i++ ) 
        	{
        		String entry = entryValues[i].toString();
        		
        		for (j=0;j<vals.length;j++)
        			if ( vals[j].compareTo(entry) == 0 ) 
        				mClickedDialogEntryIndices[i] = true;
        	}
    }

	@Override
    protected void onDialogClosed(boolean positiveResult) 
	{
		StringBuffer values = new StringBuffer();
		String valueString;
		boolean first = false;
        
    	CharSequence[] entryValues = getEntryValues();
        if (positiveResult == true && entryValues != null) 
        {
        	for ( int i=0; i<entryValues.length; i++ ) 
        		if ( mClickedDialogEntryIndices[i] == true ) 
        		{
        			// append separator after first one has been written
        			if (first == true)
        				values.append(separator);
        			
        			values.append((String) entryValues[i]);
        			first = true;
        		}

        	valueString = values.toString();
        	
            if (callChangeListener(valueString) == true) 
        		setValue(valueString);
        }
    }
}
