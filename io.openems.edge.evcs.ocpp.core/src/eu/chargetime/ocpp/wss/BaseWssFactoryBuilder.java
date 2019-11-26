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

import java.util.List;

import javax.net.ssl.SSLContext;

import org.java_websocket.WebSocketServerFactory;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

/** Base implementation of WssFactoryBuilder. */
public class BaseWssFactoryBuilder implements WssFactoryBuilder {

	private SSLContext sslContext;
	private List<String> ciphers;

	private BaseWssFactoryBuilder() {
	}

	public static BaseWssFactoryBuilder builder() {
		return new BaseWssFactoryBuilder();
	}

	public BaseWssFactoryBuilder ciphers(List<String> ciphers) {
		this.ciphers = ciphers;
		return this;
	}

	public BaseWssFactoryBuilder sslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
		return this;
	}

	@Override
	public WebSocketServerFactory build() {
		verify();

		return ciphers == null ? new DefaultSSLWebSocketServerFactory(sslContext)
				: new CustomSSLWebSocketServerFactory(sslContext, ciphers);
	}

	@Override
	public void verify() {
		if (sslContext == null) {
			throw new IllegalStateException("sslContext must be set");
		}
	}
}
