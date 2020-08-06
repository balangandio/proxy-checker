package com.comxa.universo42.proxychecker.modelo;

public class ProxyBean {
	private String addr;
	private int port;
	
	public ProxyBean(String addr, int port) {
		this.addr = addr;
		this.port = port;
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
