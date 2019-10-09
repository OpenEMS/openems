package eu.chargetime.ocpp;

import java.net.InetSocketAddress;

import javax.xml.soap.SOAPMessage;

/** Created by emil on 21.05.2017. */
public class SOAPMessageInfo {
  private final InetSocketAddress address;
  private final SOAPMessage message;

  public SOAPMessageInfo(InetSocketAddress address, SOAPMessage message) {
    this.address = address;
    this.message = message;
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  public SOAPMessage getMessage() {
    return message;
  }
}
