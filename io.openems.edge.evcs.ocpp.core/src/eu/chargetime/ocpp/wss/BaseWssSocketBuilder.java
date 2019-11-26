package eu.chargetime.ocpp.wss;
/*
 ubitricity.com - Java-OCA-OCPP

 MIT License

 Copyright (C) 2018 Evgeny Pakhomov <eugene.pakhomov@ubitricity.com>

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
*/

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;

import javax.net.ssl.SSLSocketFactory;

/** Base implementation of WssSocketBuilder. */
public class BaseWssSocketBuilder implements WssSocketBuilder {

	public static final int DEFAULT_WSS_PORT = 443;
	private Proxy proxy = Proxy.NO_PROXY;
	private SocketFactory socketFactory = Socket::new;
	private SSLSocketFactory sslSocketFactory;
	private boolean tcpNoDelay;
	private boolean reuseAddr;
	private boolean autoClose = true;
	private URI uri;
	private InetSocketAddressFactory inetSocketAddressFactory = (host, port) -> new InetSocketAddress(host, port);

	// 0 for infinite timeout
	private int connectionTimeout = 0;

	private BaseWssSocketBuilder() {
	}

	public static BaseWssSocketBuilder builder() {
		return new BaseWssSocketBuilder();
	}

	public BaseWssSocketBuilder proxy(Proxy proxy) {
		this.proxy = proxy;
		return this;
	}

	public BaseWssSocketBuilder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
		this.sslSocketFactory = sslSocketFactory;
		return this;
	}

	public BaseWssSocketBuilder socketFactory(SocketFactory socketFactory) {
		this.socketFactory = socketFactory;
		return this;
	}

	public BaseWssSocketBuilder inetSocketAddressFactory(InetSocketAddressFactory inetSocketAddressFactory) {
		this.inetSocketAddressFactory = inetSocketAddressFactory;
		return this;
	}

	public BaseWssSocketBuilder tcpNoDelay(boolean tcpNoDelay) {
		this.tcpNoDelay = tcpNoDelay;
		return this;
	}

	public BaseWssSocketBuilder reuseAddr(boolean reuseAddr) {
		this.reuseAddr = reuseAddr;
		return this;
	}

	public BaseWssSocketBuilder autoClose(boolean autoClose) {
		this.autoClose = autoClose;
		return this;
	}

	@Override
	public BaseWssSocketBuilder uri(URI uri) {
		this.uri = uri;
		return this;
	}

	public BaseWssSocketBuilder connectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
		return this;
	}

	@Override
	public Socket build() throws IOException {
		verify(true);

		Socket socket = socketFactory.getSocket(proxy);
		socket.setTcpNoDelay(tcpNoDelay);
		socket.setReuseAddress(reuseAddr);

		if (!socket.isBound()) {
			socket.connect(inetSocketAddressFactory.getInetSocketAddress(uri.getHost(), getPort(uri)),
					connectionTimeout);
		}

		return sslSocketFactory.createSocket(socket, uri.getHost(), getPort(uri), autoClose);
	}

	@Override
	public void verify() {
		verify(false);
	}

	public interface SocketFactory {
		Socket getSocket(Proxy proxy);
	}

	public interface InetSocketAddressFactory {
		InetSocketAddress getInetSocketAddress(String host, int port);
	}

	private void verify(boolean complete) {
		if (sslSocketFactory == null) {
			throw new IllegalStateException("sslSocketFactory must be set");
		}

		if (complete) {
			if (uri == null) {
				throw new IllegalStateException("uri must be set");
			}
		}
	}

	private int getPort(URI uri) {
		int port = uri.getPort();
		if (port == -1) {
			String scheme = uri.getScheme();
			if ("wss".equals(scheme)) {
				return DEFAULT_WSS_PORT;
			} else if ("ws".equals(scheme)) {
				throw new IllegalArgumentException("Not supported scheme: " + scheme);
			} else {
				throw new IllegalArgumentException("Unknown scheme: " + scheme);
			}
		}
		return port;
	}
}
