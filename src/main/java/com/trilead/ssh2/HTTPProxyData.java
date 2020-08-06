package com.trilead.ssh2;

/**
 * A <code>HTTPProxyData</code> object is used to specify the needed connection
 * data to connect through a HTTP proxy.
 * 
 * @see Connection#setProxyData(ProxyData)
 * 
 * @author Christian Plattner, plattner@trilead.com
 * @version $Id: HTTPProxyData.java,v 1.1 2007/10/15 12:49:56 cplattne Exp $
 */

public class HTTPProxyData implements ProxyData {
	public final String proxyHost;
	public final int proxyPort;
	public final String payload;

	public HTTPProxyData(String proxyHost, int proxyPort, String payload) {
		if (proxyHost == null)
			throw new IllegalArgumentException("proxyHost must be non-null");

		if (proxyPort < 0)
			throw new IllegalArgumentException("proxyPort must be non-negative");

		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.payload = payload;
	}
}
