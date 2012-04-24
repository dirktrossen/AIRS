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
package com.airs;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class MapViewerOverlay extends ItemizedOverlay 
{
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context airs;
	
	public MapViewerOverlay(Drawable arg0, Context airs) 
	{
		super(boundCenterBottom(arg0));
		// TODO Auto-generated constructor stub
		this.airs = airs;
	}

	public void addOverlay(OverlayItem overlay, Drawable marker)
	{
		// set marker for this item
		overlay.setMarker(boundCenterBottom(marker));
	    mOverlays.add(overlay);
	    populate();
	}

	public void addOverlay(OverlayItem overlay) 
	{
	    mOverlays.add(overlay);
	    populate();
	}
	
	@Override
	protected  boolean	onTap(int index) 
	{
		OverlayItem item = mOverlays.get(index);
		
		Toast.makeText(airs, "Measured at " + item.getTitle(), Toast.LENGTH_LONG).show();

		return false;
	}
	
	@Override
	protected OverlayItem createItem(int i) 
	{
	  return mOverlays.get(i);
	}

	@Override
	public int size() 
	{
		// TODO Auto-generated method stub
		return mOverlays.size();
	}
}
