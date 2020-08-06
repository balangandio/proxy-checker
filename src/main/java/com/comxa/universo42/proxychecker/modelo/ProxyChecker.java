package com.comxa.universo42.proxychecker.modelo;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ProxyChecker implements Runnable {
	public static final int MILLISEGUNDOS_SLEEP = 1000;
	
	private List<Proxy> proxies;
	private String payload;
	private int qtdThreads;
	private int qtdThreadsDone;
	private int qtdChecked;
	
	private Iterator<Proxy> iterador;
	private ThreadChecker []threads;
	private boolean running;
	
	private List<Proxy> proxyOns = Collections.synchronizedList(new LinkedList<Proxy>());
	private List<Proxy> proxyOffs = Collections.synchronizedList(new LinkedList<Proxy>());

	public ProxyChecker(List<Proxy> proxies, int qtdThreads, String payload) {
		this.proxies = proxies;
		this.qtdThreads = qtdThreads;
		this.payload = payload;
	}
	
	public void check(boolean blocking) {
		running = true;
		iterador = this.proxies.iterator();
		threads = new ThreadChecker[this.qtdThreads];
		
		for (int i = 1; i <= threads.length; i++) {
			threads[i-1] = new ThreadChecker();
			threads[i-1].run(false);
		}
		
		if (blocking)
			run();
		else
			new Thread(this).start();
	}

	@Override
	public void run() {
		while (running && !isComplete()) {
			try {
				Thread.sleep(MILLISEGUNDOS_SLEEP);
			} catch (InterruptedException e) {
				onLog("Proxy checker: interrupted exception!");
			}
			onLog(String.format("Checked: %d/%d - Threads: %d/%d", qtdChecked, proxies.size(), qtdThreadsDone, qtdThreads));
		}

		running = false;
		if (isComplete())
			onComplete();
	}

	public void stop() {
		running = false;
		if (threads != null)
			for (ThreadChecker t : threads)
				if (t.proxy != null)
					t.proxy.stopCheck();
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isComplete() {
		return (qtdThreadsDone == qtdThreads);
	}
	
	public List<Proxy> getProxyOns() {
		return this.proxyOns;
	}
	
	public List<Proxy> getProxyOffs() {
		return this.proxyOffs;
	}
	
	public void onLog(String log) {}

	public void onComplete() {}
	
	private synchronized Proxy getProxy() {
		if (!running || !iterador.hasNext())
			return null;

		Proxy p = iterador.next();

		if (payload != null)
			p.setRequest(payload);

		return p;
	}
	
	
	private class ThreadChecker implements Runnable {
		private Proxy proxy;

		public void run(boolean blocking) {
			if (blocking)
				run();
			else 
				new Thread(this).start();
		}

		@Override
		public void run() {
			try {
				while ((proxy = getProxy()) != null) {
					proxy.check(true);

					if (!proxy.isStopped()) {
						if (proxy.isOn())
							proxyOns.add(proxy);
						else
							proxyOffs.add(proxy);

						qtdChecked++;
					}
				}
			} finally {
				qtdThreadsDone++;	
			}
		}
	}
}
