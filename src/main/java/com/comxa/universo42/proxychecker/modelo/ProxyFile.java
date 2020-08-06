package com.comxa.universo42.proxychecker.modelo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProxyFile {
	private String request;
	private String data;
	private File path;
	private List<Proxy> proxies;

	public ProxyFile(String data) {
		this.data = data;
	}

	public ProxyFile(File filePath) {
		this.path = filePath;
	}

	public List<Proxy> getProxies() {
		return proxies;
	}

	public void setProxies(List<Proxy> proxies) {
		this.proxies = proxies;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public void save() throws IOException {
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(this.path);

			for (Proxy proxy : this.proxies)
				pw.println(proxy.toString());
		} finally {
			if (pw != null)
				pw.close();
		}
	}

	public void load() throws IOException {
		this.proxies = new ArrayList<Proxy>();
		String strData = (path != null) ? getFileStr(path) : data;

		Pattern ipPattern = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
		Matcher matcher = ipPattern.matcher(strData);

		while (matcher.find()) {
			String ip = strData.substring(matcher.start(), matcher.end());
			int port = findPort(ip, strData, 100);

			if (port != -1)
				this.proxies.add(new Proxy(ip, port));
		}
	}


	private int findPort(String ip, String data, int maxRange) {
		int aux = data.indexOf(ip);

		if (aux == -1)
			return -1;

		aux += ip.length();

		if (aux >= data.length()-1)
			return -1;

		int i;
		for (i = aux; i < data.length() && i-aux < maxRange && !Character.isDigit(data.charAt(i)); i++);

		if (i == data.length() || !Character.isDigit(data.charAt(i)))
			return -1;

		for (aux = i; i < data.length() && i-aux < 5 && Character.isDigit(data.charAt(i)); i++);

		return Integer.parseInt(data.substring(aux, i));
	}

	private String getFileStr(File file) throws IOException {
		StringBuilder builder = new StringBuilder();
		FileInputStream fileIn = null;
		try {
			fileIn = new FileInputStream(file);

			byte[] buffer = new byte[1024 * 32];
			int len;
			while ((len = fileIn.read(buffer)) != -1)
				builder.append(new String(buffer, 0, len));

		} finally {
			if (fileIn != null)
				fileIn.close();
		}

		return builder.toString();
	}
}
