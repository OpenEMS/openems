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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import javax.net.ssl.SSLContext;

import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.protocols.IProtocol;
import org.java_websocket.protocols.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.feature.profile.Profile;
import eu.chargetime.ocpp.feature.profile.ServerCoreProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.wss.BaseWssFactoryBuilder;
import eu.chargetime.ocpp.wss.WssFactoryBuilder;

public class JSONServer implements IServerAPI {

  private static final Logger logger = LoggerFactory.getLogger(JSONServer.class);

  public final Draft draftOcppOnly;
  private final WebSocketListener listener;
  private final Server server;
  private final FeatureRepository featureRepository;

  /**
   * The core feature profile is required as a minimum. The constructor creates WS-ready server.
   *
   * @param coreProfile implementation of the core feature profile.
   * @param configuration network configuration for a json server.
   */
  public JSONServer(ServerCoreProfile coreProfile, JSONConfiguration configuration) {
    featureRepository = new FeatureRepository();
    SessionFactory sessionFactory = new SessionFactory(featureRepository);

    ArrayList<IProtocol> protocols = new ArrayList<>();
    protocols.add(new Protocol("ocpp1.6"));
    protocols.add(new Protocol(""));
    draftOcppOnly = new Draft_6455(Collections.emptyList(), protocols);

    this.listener = new WebSocketListener(sessionFactory, configuration, draftOcppOnly);
    server = new Server(this.listener, featureRepository, new PromiseRepository());
    featureRepository.addFeatureProfile(coreProfile);
  }

  /**
   * The core feature profile is required as a minimum. The constructor creates WS-ready server.
   *
   * @param coreProfile implementation of the core feature profile.
   */
  public JSONServer(ServerCoreProfile coreProfile) {
    this(coreProfile, JSONConfiguration.get());
  }

  /**
   * The core feature profile is required as a minimum. The constructor creates WSS-ready server.
   *
   * @param coreProfile implementation of the core feature profile.
   * @param wssFactoryBuilder to build {@link org.java_websocket.WebSocketServerFactory} to support
   *     wss://.
   * @param configuration network configuration for a json server.
   */
  public JSONServer(
      ServerCoreProfile coreProfile,
      WssFactoryBuilder wssFactoryBuilder,
      JSONConfiguration configuration) {
    this(coreProfile, configuration);
    enableWSS(wssFactoryBuilder);
  }

  /**
   * The core feature profile is required as a minimum. The constructor creates WSS-ready server.
   *
   * @param coreProfile implementation of the core feature profile.
   * @param wssFactoryBuilder to build {@link org.java_websocket.WebSocketServerFactory} to support
   *     wss://.
   */
  public JSONServer(ServerCoreProfile coreProfile, WssFactoryBuilder wssFactoryBuilder) {
    this(coreProfile, wssFactoryBuilder, JSONConfiguration.get());
  }

  // To ensure the exposed API is backward compatible
  public void enableWSS(SSLContext sslContext) throws IOException {
    WssFactoryBuilder builder = BaseWssFactoryBuilder.builder().sslContext(sslContext);
    enableWSS(builder);
  }

  /**
   * Enables server to accept WSS connections. The {@code wssFactoryBuilder} must be initialized at
   * that step (as required parameters set might vary depending on implementation the {@link
   * eu.chargetime.ocpp.wss.WssFactoryBuilder#verify()} is used to ensure initialization).
   *
   * @param wssFactoryBuilder builder to provide WebSocketServerFactory
   * @return instance of {@link JSONServer}
   * @throws IllegalStateException in case if the server is already connected
   * @throws IllegalStateException in case {@code wssFactoryBuilder} not initialized properly
   */
  public JSONServer enableWSS(WssFactoryBuilder wssFactoryBuilder) {
    wssFactoryBuilder.verify();
    listener.enableWSS(wssFactoryBuilder);
    return this;
  }

  @Override
  public void addFeatureProfile(Profile profile) {
    featureRepository.addFeatureProfile(profile);
  }

  @Override
  public void closeSession(UUID session) {
    server.closeSession(session);
  }

  @Override
  public void open(String host, int port, ServerEvents serverEvents) {
    logger.info("Feature repository: {}", featureRepository);
    server.open(host, port, serverEvents);
  }

  @Override
  public void close() {
    server.close();
  }

  @Override
  public boolean isClosed() {
    return listener.isClosed();
  }

  @Override
  public CompletionStage<Confirmation> send(UUID session, Request request)
      throws OccurenceConstraintException, UnsupportedFeatureException, NotConnectedException {
    return server.send(session, request);
  }
}
