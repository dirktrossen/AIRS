/*
Copyright (C) 2014, TecVis LP, support@tecvis.co.uk

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
package com.airs.handlerUIs;

import java.util.ArrayList;

import com.airs.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity to implement the HeartMonitorHandler configuration UI for BT Smart type of devices. This is called from the preference window
 */
@SuppressLint("NewApi")
public class HeartMonitorSmartHandlerUI extends Activity implements OnClickListener, OnItemClickListener
{
    private ArrayAdapter<String> mLeDeviceListAdapter;
    private ArrayList<String> mLeDeviceListName = new ArrayList<String>();
    private ArrayList<BluetoothDevice> mLeDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothAdapter mBluetoothAdapter;
    private ListView device_list;
    private int selectedDevice = AdapterView.INVALID_POSITION;
    // preferences
    private SharedPreferences settings;
    private Editor editor;

    /** Called when the activity is first created. 
     * @param savedInstanceState a Bundle of the saved state, according to Android lifecycle model
     */
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        // get default preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        editor = settings.edit();
        
        // is BT Smart supported on this device?
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) 
        {
        	BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();

            // Checks if Bluetooth is supported on the device.
            if (mBluetoothAdapter != null) 
            {
                // is BT enabled?
                if (mBluetoothAdapter.isEnabled() == true) 
                {
        	        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
                	setContentView(R.layout.devicelist);
        	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        	 
        	        // get window title fields
        	        TextView mTitle = (TextView) findViewById(R.id.title_left_text);
        	        mTitle.setText(R.string.app_name);
        	        mTitle = (TextView) findViewById(R.id.title_right_text);
        	        mTitle.setText(R.string.Device_Selection_title2);
        	        
	                // Initializes list view adapter.
	                mLeDeviceListAdapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, mLeDeviceListName);
	                device_list = (ListView)findViewById(R.id.BTdevice_list);
	                device_list.setItemsCanFocus(false); 
	                device_list.setDividerHeight(2);
	                device_list.setAdapter(mLeDeviceListAdapter);
	                device_list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	                device_list.setOnItemClickListener(this);

	                // set button callback
	                Button select = (Button)findViewById(R.id.BTdevice_select);
	                select.setOnClickListener(this);

	                mBluetoothAdapter.startLeScan(mLeScanCallback);
                }
                else
                {
                    Toast.makeText(this, R.string.Device_Selection_Error2, Toast.LENGTH_SHORT).show();
                    finish();
                }                	
            }
            else
            {
                Toast.makeText(this, R.string.Device_Selection_Error, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        else
        {
            Toast.makeText(this, R.string.Device_Selection_Error, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /** Called when the activity is destroyed
     */
    @Override
    protected void onDestroy() 
    {
        super.onDestroy();

        // stop scanning
        mBluetoothAdapter.stopLeScan(mLeScanCallback);        
    }
    
	/** Called when a list item has been clicked on by the user
     * @param v Reference to the {android.view.View} of the button
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) 
    {
 	   selectedDevice = position;
    }

	/** Called when a button has been clicked on by the user
     * @param v Reference to the {android.view.View} of the button
     */
	public void onClick(View v) 
    {
    	switch(v.getId())
    	{
    	case R.id.BTdevice_select:
            // stop scanning
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            
    		if (selectedDevice != AdapterView.INVALID_POSITION)
    		{
	            BluetoothDevice device = mLeDeviceList.get(selectedDevice);
	            
	            if (device == null) 
	            	return;
	            
	            // now write selected BT address into settings store
	            editor.putString("HeartMonitorHandler::BTSmartStore", device.getAddress());
	            editor.commit();
	            
	            Toast.makeText(this, device.getName() + " " + device.getAddress(), Toast.LENGTH_SHORT).show();
	            
	            // finish activity
	            finish();
    		}
    		else
	            Toast.makeText(this, getString(R.string.Device_Selection_Error2), Toast.LENGTH_SHORT).show();
    			
            break;
    	}
    }
    
    // Device scan callback.
    @SuppressLint("NewApi")
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() 
    {

        /** Called when device has been found in LE scan 
         * @param device reference to BluetoothDevice
         * @param rssi, signal strength in dB
         * @param scanRecord, additional info as byte array
         */
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) 
        {
            runOnUiThread(new Runnable() 
            {
                @Override
                public void run() 
                {
                	boolean found = false;
                	int i;
                	BluetoothDevice current = null;
                	String currentAddress = device.getAddress();
                	
                	// is found device already been discovered before?
                	if (mLeDeviceList.size()>0)
	                	for (i=0;i<mLeDeviceList.size();i++)
	                	{
	                		current = mLeDeviceList.get(i);
	                		if (current.getAddress().compareTo(currentAddress) == 0)
	                			found = true;
	                	}
                	
                	// add only truly new devices to the list!
                	if (found == false)
                	{
	                	// add device name to listview
	                	String name = device.getName();
	                	if (name != null)
	                	{
		                	mLeDeviceListName.add(name);
		                    mLeDeviceListAdapter.notifyDataSetChanged();
	                	}
	                	// add device to device list
	                	mLeDeviceList.add(device);
                	}
                }
            });
        }
    };
}
