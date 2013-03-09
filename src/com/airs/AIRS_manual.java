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

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AIRS_manual extends Activity
{
    WebView mWebView;
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        mWebView = new WebView(this);
        mWebView.setWebViewClient(new HelloWebViewClient());
        mWebView.loadUrl("http://tecvis.co.uk/software/software/airs/");

        setContentView(mWebView);

    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) 
    {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) 
    {
    	MenuInflater inflater;
        menu.clear();    		
        inflater = getMenuInflater();
        inflater.inflate(R.menu.options_web, menu);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {    	    	
        switch (item.getItemId()) 
        {
        case R.id.main_about:
       		HandlerUIManager.AboutDialog(getString(R.string.Online_Manual), getString(R.string.ManualAbout));
       		return true;
        case R.id.web_blog:
            mWebView.loadUrl("http://dalore.me.uk/DOT/category/airs/");        	
        	return true;
        case R.id.web_back:
            mWebView.goBack();
            return true;
        case R.id.web_forward:
            mWebView.goForward();
            return true;
        }
        return false;
    }
    private class HelloWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}

