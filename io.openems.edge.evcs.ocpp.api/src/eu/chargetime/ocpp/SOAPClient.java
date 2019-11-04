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
import eu.chargetime.ocpp.feature.profile.ClientCoreProfile;
import eu.chargetime.ocpp.feature.profile.Profile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.SOAPHostInfo;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.xml.soap.SOAPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SOAPClient implements IClientAPI {
  private static final Logger logger = LoggerFactory.getLogger(SOAPClient.class);
  private static final String WSDL_CHARGE_POINT =
      "eu/chargetime/ocpp/OCPP_ChargePointService_1.6.wsdl";

  private Client client;
  private SOAPCommunicator communicator;
  private WebServiceTransmitter transmitter;
  private URL callback;
  private HttpServer server;
  private ExecutorService threadPool;
  private FeatureRepository featureRepository;

  /**
   * The core feature profile is required. The client will use the information taken from the
   * callback parameter to open a HTTP based Web Service.
   *
   * @param chargeBoxIdentity required identity used in message header.
   * @param callback call back info that the server can send requests to.
   * @param coreProfile implementation of the core feature profile.
   */
  public SOAPClient(String chargeBoxIdentity, URL callback, ClientCoreProfile coreProfile) {

    SOAPHostInfo hostInfo =
        new SOAPHostInfo.Builder()
            .isClient(true)
            .chargeBoxIdentity(chargeBoxIdentity)
            .fromUrl(callback.toString())
            .namespace(SOAPHostInfo.NAMESPACE_CHARGEBOX)
            .build();

    this.callback = callback;
    this.transmitter = new WebServiceTransmitter();
    this.communicator = new SOAPCommunicator(hostInfo, transmitter);
    featureRepository = new FeatureRepository();
    ISession session = new SessionFactory(featureRepository).createSession(communicator);
    this.client = new Client(session, featureRepository, new PromiseRepository());
    featureRepository.addFeatureProfile(coreProfile);
  }

  @Override
  public void addFeatureProfile(Profile profile) {
    featureRepository.addFeatureProfile(profile);
  }

  /**
   * Connect to server and set To header. Client opens a WebService for incoming requests.
   *
   * @param uri url and port of the server
   */
  public void connect(String uri, ClientEvents events) {
    communicator.setToUrl(uri);
    this.client.connect(uri, events);
    openWS();
  }

  @Override
  public CompletionStage<Confirmation> send(Request request)
      throws OccurenceConstraintException, UnsupportedFeatureException {
    return client.send(request);
  }

  /** Disconnect from server Closes down local callback service. */
  public void disconnect() {
    this.client.disconnect();
    if (server != null) {
      server.stop(1);
      threadPool.shutdownNow();
    }
  }

  /**
   * Flag if connection is closed.
   *
   * @return true if connection was closed or not opened
   */
  @Override
  public boolean isClosed() {
    return transmitter.isClosed();
  }

  private int getPort() {
    return callback.getPort() == -1 ? 8000 : callback.getPort();
  }

  private void openWS() {
    try {
      server = HttpServer.create(new InetSocketAddress(callback.getHost(), getPort()), 0);
      server.createContext(
          "/",
          new WSHttpHandler(
              WSDL_CHARGE_POINT,
              message -> {
                SOAPMessage soapMessage = null;
                try {
                  soapMessage = transmitter.relay(message.getMessage()).get();
                } catch (InterruptedException e) {
                  logger.warn("openWS() transmitter.relay failed", e);
                } catch (ExecutionException e) {
                  logger.warn("openWS() transmitter.relay failed", e);
                }
                return soapMessage;
              }));
      threadPool = Executors.newCachedThreadPool();
      server.setExecutor(threadPool);
      server.start();
    } catch (IOException e) {
      logger.warn("openWS() failed", e);
    }
  }
}
