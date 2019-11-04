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

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServiceReceiver extends SOAPSyncHelper implements Receiver {
  private static final Logger logger = LoggerFactory.getLogger(WebServiceReceiver.class);

  private RadioEvents events;
  private SOAPConnection soapConnection;
  private String url;
  private WebServiceReceiverEvents receiverEvents;
  private boolean connected;

  public WebServiceReceiver(String url, WebServiceReceiverEvents receiverEvents) {
    this.url = url;
    this.receiverEvents = receiverEvents;
    connected = false;
  }

  @Override
  public void disconnect() {
    if (connected) {
      try {
        soapConnection.close();
        connected = false;
      } catch (SOAPException e) {
        logger.info("disconnect() failed", e);
      }
    }
    events.disconnected();
    receiverEvents.disconnect();
  }

  @Override
  public boolean isClosed() {
    return !connected;
  }

  @Override
  public void accept(RadioEvents events) {
    this.events = events;
    try {
      SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
      soapConnection = soapConnectionFactory.createConnection();
      connected = true;
      events.connected();
    } catch (SOAPException e) {
      logger.warn("accept() failed", e);
    }
  }

  @Override
  void forwardMessage(SOAPMessage message) {
    events.receivedMessage(message);
  }

  @Override
  void sendRequest(SOAPMessage message) throws NotConnectedException {
    if (!connected) throw new NotConnectedException();

    new Thread(
            () -> {
              try {
                events.receivedMessage(soapConnection.call(message, url));
              } catch (SOAPException e) {
                disconnect();
              }
            })
        .start();
  }
}
