package com.comxa.universo42.proxychecker.view;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;

import com.comxa.universo42.proxychecker.R;

import java.util.ArrayList;

public class ProxyListActivity extends FragmentActivity {
    public static final String INTENT_PROXIES = "proxies";
    public static final String INTENT_PROXY_ONS = "proxiesOns";
    public static final String INTENT_PROXY_OFFS = "proxiesOffs";

    private FragmentTabHost tabHost;
    private ArrayList<String> proxies;
    private ArrayList<String> proxiesOns;
    private ArrayList<String> proxiesOffs;
    private boolean hasTab;

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_proxies);

        tabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        tabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        proxies = getIntent().getStringArrayListExtra(INTENT_PROXIES);

        if (proxies != null) {
            makeTab("Tab0", getString(R.string.titleTabProxies) + " ("+proxies.size()+")", TabFragment.TabProxies.class);
        } else {
            proxiesOns = getIntent().getStringArrayListExtra(INTENT_PROXY_ONS);
            if (proxiesOns != null)
                makeTab("Tab1", getString(R.string.titleTabOns) + " ("+proxiesOns.size()+")", TabFragment.TabProxyOns.class);

            proxiesOffs = getIntent().getStringArrayListExtra(INTENT_PROXY_OFFS);
            if (proxiesOffs != null)
                makeTab("Tab2", getString(R.string.titleTabOffs) + " ("+proxiesOffs.size()+")", TabFragment.TabProxyOffs.class);
        }

        if (!hasTab)
            makeTab("Tab4", getString(R.string.titleTabProxies), TabFragment.TabProxyEmpty.class);
    }

    private void makeTab(String specName, String title, Class classe) {
        tabHost.addTab(tabHost.newTabSpec(specName).setIndicator(title), classe, null);
        hasTab = true;
    }

    public ArrayList<String> getProxies() {
        return this.proxies;
    }

    public ArrayList<String> getProxyOns() {
        return this.proxiesOns;
    }

    public ArrayList<String> getProxyOffs() {
        return this.proxiesOffs;

    }
}
