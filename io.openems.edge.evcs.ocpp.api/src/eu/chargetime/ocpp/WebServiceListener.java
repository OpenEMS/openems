package eu.chargetime.ocpp;
/*
   ChargeTime.eu - Java-OCA-OCPP

   MIT License

   Copyright (C) 2016-2018 Thomas Volden <tv@chargetime.eu>

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

import com.sun.net.httpserver.HttpServer;
import eu.chargetime.ocpp.model.SOAPHostInfo;
import eu.chargetime.ocpp.model.SessionInformation;
import eu.chargetime.ocpp.utilities.TimeoutTimer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import javax.xml.soap.SOAPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServiceListener implements Listener {
  private static final Logger logger = LoggerFactory.getLogger(WebServiceListener.class);
  private static final String WSDL_CENTRAL_SYSTEM =
      "eu/chargetime/ocpp/OCPP_CentralSystemService_1.6.wsdl";
  private final ISessionFactory sessionFactory;

  private ListenerEvents events;
  private String fromUrl = null;
  private HttpServer server;
  private boolean handleRequestAsync;
  private volatile boolean closed = true;

  public WebServiceListener(ISessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void open(String hostname, int port, ListenerEvents listenerEvents) {
    events = listenerEvents;
    fromUrl = String.format("http://%s:%d", hostname, port);
    try {
      server = HttpServer.create(new InetSocketAddress(hostname, port), 0);
      server.createContext("/", new WSHttpHandler(WSDL_CENTRAL_SYSTEM, new WSHttpEventHandler()));

      server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
      server.start();

      closed = false;
    } catch (IOException e) {
      logger.warn("open() failed", e);
    }
  }

  @Override
  public void close() {
    if (server != null) server.stop(1);
    closed = true;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void setAsyncRequestHandler(boolean async) {
    handleRequestAsync = async;
  }

  private class WSHttpEventHandler implements WSHttpHandlerEvents {
    private static final long INITIAL_TIMEOUT = 1000 * 60 * 5;
    HashMap<String, WebServiceReceiver> chargeBoxes;

    public WSHttpEventHandler() {
      chargeBoxes = new HashMap<>();
    }

    private void removeChargebox(String identity) {
      if (chargeBoxes.containsKey(identity)) chargeBoxes.remove(identity);
    }

    @Override
    public SOAPMessage incomingRequest(SOAPMessageInfo messageInfo) {
      SOAPMessage message = messageInfo.getMessage();
      String identity = SOAPSyncHelper.getHeaderValue(message, "chargeBoxIdentity");
      if (!chargeBoxes.containsKey(identity)) {
        String toUrl = SOAPSyncHelper.getHeaderValue(message, "From");
        WebServiceReceiver webServiceReceiver =
            new WebServiceReceiver(toUrl, () -> removeChargebox(identity));

        SOAPHostInfo hostInfo =
            new SOAPHostInfo.Builder()
                .isClient(false)
                .chargeBoxIdentity(identity)
                .fromUrl(fromUrl)
                .namespace(SOAPHostInfo.NAMESPACE_CENTRALSYSTEM)
                .build();
        SOAPCommunicator communicator = new SOAPCommunicator(hostInfo, webServiceReceiver);
        communicator.setToUrl(toUrl);

        ISession session = sessionFactory.createSession(communicator);
        TimeoutTimer timeoutTimer =
            new TimeoutTimer(
                INITIAL_TIMEOUT,
                () -> {
                  session.close();
                  chargeBoxes.remove(identity);
                });

        // TODO: Decorator created but not used
        ISession sessionDecorator = new TimeoutSessionDecorator(timeoutTimer, session);

        SessionInformation information =
            new SessionInformation.Builder()
                .Identifier(identity)
                .InternetAddress(messageInfo.getAddress())
                .SOAPtoURL(toUrl)
                .build();
        events.newSession(session, information);
        chargeBoxes.put(identity, webServiceReceiver);
      }

      SOAPMessage confirmation = null;
      try {
        confirmation = chargeBoxes.get(identity).relay(message).get();
      } catch (InterruptedException | ExecutionException e) {
        logger.warn("incomingRequest() chargeBoxes.relay failed", e);
      }

      return confirmation;
    }
  }
}
