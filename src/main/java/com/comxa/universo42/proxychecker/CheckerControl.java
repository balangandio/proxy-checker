package com.comxa.universo42.proxychecker;

import com.comxa.universo42.proxychecker.modelo.Proxy;
import com.comxa.universo42.proxychecker.modelo.ProxyChecker;

import java.util.List;

public interface CheckerControl {
    public ProxyChecker getChecker();
    public void setChecker(List<Proxy> proxyList, int qtdThreads, String payload);
}
