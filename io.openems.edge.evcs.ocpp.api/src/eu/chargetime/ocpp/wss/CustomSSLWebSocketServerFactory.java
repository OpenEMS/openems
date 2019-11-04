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
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.java_websocket.SSLSocketChannel2;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

/**
 * Overriding wrapping of channel from DefaultSSLWebSocketServerFactory to restrict enabled ciphers.
 */
public final class CustomSSLWebSocketServerFactory extends DefaultSSLWebSocketServerFactory {

  private List<String> ciphers;

  public CustomSSLWebSocketServerFactory(SSLContext sslContext, List<String> ciphers) {
    super(sslContext);
    this.ciphers = ciphers;
  }

  @Override
  public ByteChannel wrapChannel(SocketChannel channel, SelectionKey key) throws IOException {
    SSLEngine e = sslcontext.createSSLEngine();
    /*
     * See https://github.com/TooTallNate/Java-WebSocket/issues/466
     *
     * For TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 you must patch your java installation directly.
     */
    List<String> enabledCiphers = new ArrayList<String>(Arrays.asList(e.getEnabledCipherSuites()));
    enabledCiphers.retainAll(ciphers);

    e.setEnabledCipherSuites(enabledCiphers.toArray(new String[enabledCiphers.size()]));
    e.setUseClientMode(false);
    return new SSLSocketChannel2(channel, e, exec, key);
  }
}
