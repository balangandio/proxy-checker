package com.trilead.ssh2;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;

import com.trilead.ssh2.crypto.CryptoWishList;
import com.trilead.ssh2.transport.TransportManager;
import com.trilead.ssh2.util.TimeoutService;
import com.trilead.ssh2.util.TimeoutService.TimeoutToken;

/**
 * A <code>Connection</code> is used to establish an encrypted TCP/IP connection
 * to a SSH-2 server.
 * <p>
 * Typically, one
 * <ol>
 * <li>creates a {@link #Connection(String) Connection} object.</li>
 * <li>calls the {@link #connect() connect()} method.</li>
 * <li>finally, one must close the connection and release resources with the
 * {@link #close() close()} method.</li>
 * </ol>
 * 
 * @author Christian Plattner, plattner@trilead.com
 * @version $Id: Connection.java,v 1.3 2008/04/01 12:38:09 cplattne Exp $
 */

public class Connection {
	/**
	 * The identifier presented to the SSH-2 server.
	 */
	public final static String identification = "TrileadSSH2Java_213";


	/**
	 * Will be used to generate all random data needed for the current
	 * connection. Note: SecureRandom.nextBytes() is thread safe.
	 */
	private SecureRandom generator;
	
	private boolean compression = false;

	private CryptoWishList cryptoWishList = new CryptoWishList();

	private DHGexParameters dhgexpara = new DHGexParameters();

	private final String hostname;

	private final int port;

	private TransportManager tm;

	private boolean tcpNoDelay = false;

	private ProxyData proxyData = null;

	/**
	 * Prepares a fresh <code>Connection</code> object which can then be used to
	 * establish a connection to the specified SSH-2 server.
	 * <p>
	 * Same as {@link #Connection(String, int) Connection(hostname, 22)}.
	 * 
	 * @param hostname
	 *            the hostname of the SSH-2 server.
	 */
	public Connection(String hostname) {
		this(hostname, 22);
	}

	/**
	 * Prepares a fresh <code>Connection</code> object which can then be used to
	 * establish a connection to the specified SSH-2 server.
	 * 
	 * @param hostname
	 *            the host where we later want to connect to.
	 * @param port
	 *            port on the server, normally 22.
	 */
	public Connection(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	
	/**
	 * Close the connection to the SSH-2 server. All assigned sessions will be
	 * closed, too. Can be called at any time. Don't forget to call this once
	 * you don't need a connection anymore - otherwise the receiver thread may
	 * run forever.
	 */
	public synchronized void close() {
		Throwable t = new Throwable("Closed due to user request.");
		close(t, false);
	}

	private void close(Throwable t, boolean hard) {
		if (tm != null) {
			tm.close(t, hard == false);
			tm = null;
		}
	}

	public void closeHard() {
		if (tm != null) {
			Throwable t = new Throwable("Closed due to user request.");
			tm.close(t, false);
			tm = null;
		}
	}

	/**
	 * Same as {@link #connect(ServerHostKeyVerifier, int, int) connect(null, 0,
	 * 0)}.
	 * 
	 * @return see comments for the
	 *         {@link #connect(ServerHostKeyVerifier, int, int)
	 *         connect(ServerHostKeyVerifier, int, int)} method.
	 * @throws IOException
	 */
	public synchronized ConnectionInfo connect() throws IOException {
		return connect(null, 0, 0);
	}

	/**
	 * Same as {@link #connect(ServerHostKeyVerifier, int, int)
	 * connect(verifier, 0, 0)}.
	 * 
	 * @return see comments for the
	 *         {@link #connect(ServerHostKeyVerifier, int, int)
	 *         connect(ServerHostKeyVerifier, int, int)} method.
	 * @throws IOException
	 */
	public synchronized ConnectionInfo connect(ServerHostKeyVerifier verifier)
			throws IOException {
		return connect(verifier, 0, 0);
	}

	/**
	 * Connect to the SSH-2 server and, as soon as the server has presented its
	 * host key, use the
	 * {@link ServerHostKeyVerifier#verifyServerHostKey(String, int, String, byte[])
	 * ServerHostKeyVerifier.verifyServerHostKey()} method of the
	 * <code>verifier</code> to ask for permission to proceed. If
	 * <code>verifier</code> is <code>null</code>, then any host key will be
	 * accepted - this is NOT recommended, since it makes man-in-the-middle
	 * attackes VERY easy (somebody could put a proxy SSH server between you and
	 * the real server).
	 * <p>
	 * Note: The verifier will be called before doing any crypto calculations
	 * (i.e., diffie-hellman). Therefore, if you don't like the presented host
	 * key then no CPU cycles are wasted (and the evil server has less
	 * information about us).
	 * <p>
	 * However, it is still possible that the server presented a fake host key:
	 * the server cheated (typically a sign for a man-in-the-middle attack) and
	 * is not able to generate a signature that matches its host key. Don't
	 * worry, the library will detect such a scenario later when checking the
	 * signature (the signature cannot be checked before having completed the
	 * diffie-hellman exchange).
	 * <p>
	 * Note 2: The
	 * {@link ServerHostKeyVerifier#verifyServerHostKey(String, int, String, byte[])
	 * ServerHostKeyVerifier.verifyServerHostKey()} method will *NOT* be called
	 * from the current thread, the call is being made from a background thread
	 * (there is a background dispatcher thread for every established
	 * connection).
	 * <p>
	 * Note 3: This method will block as long as the key exchange of the
	 * underlying connection has not been completed (and you have not specified
	 * any timeouts).
	 * <p>
	 * Note 4: If you want to re-use a connection object that was successfully
	 * connected, then you must call the {@link #close()} method before invoking
	 * <code>connect()</code> again.
	 * 
	 * @param verifier
	 *            An object that implements the {@link ServerHostKeyVerifier}
	 *            interface. Pass <code>null</code> to accept any server host
	 *            key - NOT recommended.
	 * 
	 * @param connectTimeout
	 *            Connect the underlying TCP socket to the server with the given
	 *            timeout value (non-negative, in milliseconds). Zero means no
	 *            timeout. If a proxy is being used (see
	 *            {@link #setProxyData(ProxyData)}), then this timeout is used
	 *            for the connection establishment to the proxy.
	 * 
	 * @param kexTimeout
	 *            Timeout for complete connection establishment (non-negative,
	 *            in milliseconds). Zero means no timeout. The timeout counts
	 *            from the moment you invoke the connect() method and is
	 *            cancelled as soon as the first key-exchange round has
	 *            finished. It is possible that the timeout event will be fired
	 *            during the invocation of the <code>verifier</code> callback,
	 *            but it will only have an effect after the
	 *            <code>verifier</code> returns.
	 * 
	 * @return A {@link ConnectionInfo} object containing the details of the
	 *         established connection.
	 * 
	 * @throws IOException
	 *             If any problem occurs, e.g., the server's host key is not
	 *             accepted by the <code>verifier</code> or there is problem
	 *             during the initial crypto setup (e.g., the signature sent by
	 *             the server is wrong).
	 *             <p>
	 *             In case of a timeout (either connectTimeout or kexTimeout) a
	 *             SocketTimeoutException is thrown.
	 *             <p>
	 *             An exception may also be thrown if the connection was already
	 *             successfully connected (no matter if the connection broke in
	 *             the mean time) and you invoke <code>connect()</code> again
	 *             without having called {@link #close()} first.
	 *             <p>
	 *             If a HTTP proxy is being used and the proxy refuses the
	 *             connection, then a {@link HTTPProxyException} may be thrown,
	 *             which contains the details returned by the proxy. If the
	 *             proxy is buggy and does not return a proper HTTP response,
	 *             then a normal IOException is thrown instead.
	 */
	public synchronized ConnectionInfo connect(ServerHostKeyVerifier verifier, int connectTimeout, int kexTimeout) throws IOException {
		final class TimeoutState {
			boolean isCancelled = false;
			boolean timeoutSocketClosed = false;
		}

		if (tm != null) 
			throw new IOException("Connection to " + hostname + " is already in connected state!");

		if (connectTimeout < 0)
			throw new IllegalArgumentException("connectTimeout must be non-negative!");

		if (kexTimeout < 0)
			throw new IllegalArgumentException("kexTimeout must be non-negative!");

		final TimeoutState state = new TimeoutState();

		tm = new TransportManager(hostname, port);

		// Don't offer compression if not requested
		if (!compression) {
			cryptoWishList.c2s_comp_algos = new String[] { "none" };
			cryptoWishList.s2c_comp_algos = new String[] { "none" };
		}

		synchronized (tm) {
			/* We could actually synchronize on anything. */
		}

		try {
			TimeoutToken token = null;

			if (kexTimeout > 0) {
				final Runnable timeoutHandler = new Runnable() {
					@Override
					public void run() {
						synchronized (state) {
							if (state.isCancelled)
								return;
							state.timeoutSocketClosed = true;
							tm.close(new SocketTimeoutException("The connect timeout expired"), false);
						}
					}
				};

				long timeoutHorizont = System.currentTimeMillis() + kexTimeout;

				token = TimeoutService.addTimeoutHandler(timeoutHorizont, timeoutHandler);
			}

			try {
				tm.initialize(cryptoWishList, verifier, dhgexpara, connectTimeout, getOrCreateSecureRND(), proxyData);
			} catch (SocketTimeoutException se) {
				throw (SocketTimeoutException) new SocketTimeoutException("The connect() operation on the socket timed out.").initCause(se);
			}

			tm.setTcpNoDelay(tcpNoDelay);

			/* Wait until first KEX has finished */

			ConnectionInfo ci = tm.getConnectionInfo(1);

			/* Now try to cancel the timeout, if needed */

			if (token != null) {
				TimeoutService.cancelTimeoutHandler(token);

				/* Were we too late? */

				synchronized (state) {
					if (state.timeoutSocketClosed)
						throw new IOException("This exception will be replaced by the one below =)");

					state.isCancelled = true;
				}
			}

			return ci;
		} catch (SocketTimeoutException ste) {
			throw ste;
		} catch (IOException e1) {
			/* This will also invoke any registered connection monitors */
			close(new Throwable("There was a problem during connect."), false);

			synchronized (state) {
				/*
				 * Show a clean exception, not something like "the socket is
				 * closed!?!"
				 */
				if (state.timeoutSocketClosed)
					throw new SocketTimeoutException("The kexTimeout (" + kexTimeout + " ms) expired.");
			}

			/* Do not wrap a HTTPProxyException */
			if (e1 instanceof HTTPProxyException)
				throw e1;

			throw (IOException) new IOException("There was a problem while connecting to " + hostname + ":" + port).initCause(e1);
		}
	}

	/**
	 * Used to tell the library that the connection shall be established through
	 * a proxy server. It only makes sense to call this method before calling
	 * the {@link #connect() connect()} method.
	 * <p>
	 * At the moment, only HTTP proxies are supported.
	 * <p>
	 * Note: This method can be called any number of times. The
	 * {@link #connect() connect()} method will use the value set in the last
	 * preceding invocation of this method.
	 * 
	 * @see HTTPProxyData
	 * 
	 * @param proxyData
	 *            Connection information about the proxy. If <code>null</code>,
	 *            then no proxy will be used (non surprisingly, this is also the
	 *            default).
	 */
	public synchronized void setProxyData(ProxyData proxyData) {
		this.proxyData = proxyData;
	}

	/**
	 * Enable/disable TCP_NODELAY (disable/enable Nagle's algorithm) on the
	 * underlying socket.
	 * <p>
	 * Can be called at any time. If the connection has not yet been established
	 * then the passed value will be stored and set after the socket has been
	 * set up. The default value that will be used is <code>false</code>.
	 * 
	 * @param enable
	 *            the argument passed to the <code>Socket.setTCPNoDelay()</code>
	 *            method.
	 * @throws IOException
	 */
	public synchronized void setTCPNoDelay(boolean enable) throws IOException {
		tcpNoDelay = enable;

		if (tm != null)
			tm.setTcpNoDelay(enable);
	}
	
	private final SecureRandom getOrCreateSecureRND() {
		if (generator == null)
			generator = new SecureRandom();

		return generator;
	}
}
