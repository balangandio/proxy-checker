package com.comxa.universo42.proxychecker.modelo;

import java.io.IOException;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.HTTPProxyData;

public class Proxy extends ProxyBean implements Runnable {
	public static final int TIME_OUT_TCP = 15000;
	public static final int TIME_OUT_KEX = 20000;

	private String request;
	
	private boolean isComplete;
	private boolean isOn;
	private boolean stopped;

	private Exception e;
	private Connection conn;

	public Proxy(String addr, int port) {
		super(addr, port);
	}
	
	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public boolean isOn() throws IllegalStateException {
		if (!this.isComplete)
			throw new IllegalStateException("Proxy not checked yet!");
		
		return this.isOn;
	}

	public Exception getException() {
		return this.e;
	}
	
	public boolean isStopped() {
		return this.stopped;
	}
	
	public void stopCheck() {
		if (conn != null) {
			this.stopped = true;
			conn.closeHard();
		}
	}
	
	public void check(boolean blocking) {
		if (blocking)
			run();
		else
			new Thread(this).start();
	}
    
    @Override
    public void run() {
    	conn = new Connection("42.42.42.42");
    	
    	conn.setProxyData(new HTTPProxyData(getAddr(), getPort(), getRequest()));
    	
    	try {
			conn.connect(null, TIME_OUT_TCP, TIME_OUT_KEX);

			this.isOn = true;
		} catch (IOException e) {
			this.e = e;
		} finally {
			conn.close();
		}
    	
    	isComplete = true;
    	onComplete();
    }
    
    public void onComplete() {}
	
	
	public String toString() {
		return String.format("%s:%d", getAddr(), getPort());
	}
}
