package eu.chargetime.ocpp;
/*
ChargeTime.eu - Java-OCA-OCPP
Copyright (C) 2015-2016 Thomas Volden <tv@chargetime.eu>

MIT License

Copyright (C) 2016-2018 Thomas Volden

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

import eu.chargetime.ocpp.feature.Feature;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles basic client logic: Holds a list of supported features. Keeps track of outgoing request.
 * Calls back when a confirmation is received.
 *
 * <p>Must be overloaded in order to support specific protocols and formats.
 */
public class Client {
  private static final Logger logger = LoggerFactory.getLogger(Client.class);

  private ISession session;
  private final IFeatureRepository featureRepository;
  private final IPromiseRepository promiseRepository;

  /**
   * Handle required injections.
   *
   * @param session Inject session object
   * @see Session
   */
  public Client(
      ISession session,
      IFeatureRepository featureRepository,
      IPromiseRepository promiseRepository) {
    this.session = session;
    this.featureRepository = featureRepository;
    this.promiseRepository = promiseRepository;
  }

  /**
   * Connect to server
   *
   * @param uri url and port of the server
   * @param events client events for connect/disconnect
   */
  public void connect(String uri, ClientEvents events) {
    session.open(
        uri,
        new SessionEvents() {

          @Override
          public void handleConfirmation(String uniqueId, Confirmation confirmation) {
            Optional<CompletableFuture<Confirmation>> promiseOptional =
                promiseRepository.getPromise(uniqueId);
            if (promiseOptional.isPresent()) {
              promiseOptional.get().complete(confirmation);
              promiseRepository.removePromise(uniqueId);
            } else {
              logger.debug("Promise not found for confirmation {}", confirmation);
            }
          }

          @Override
          public Confirmation handleRequest(Request request) throws UnsupportedFeatureException {
            Optional<Feature> featureOptional = featureRepository.findFeature(request);
            if (featureOptional.isPresent()) {
              return featureOptional.get().handleRequest(null, request);
            } else {
              throw new UnsupportedFeatureException();
            }
          }

          @Override
          public void handleError(
              String uniqueId, String errorCode, String errorDescription, Object payload) {
            Optional<CompletableFuture<Confirmation>> promiseOptional =
                promiseRepository.getPromise(uniqueId);
            if (promiseOptional.isPresent()) {
              promiseOptional
                  .get()
                  .completeExceptionally(new CallErrorException(errorCode, errorCode, payload));
              promiseRepository.removePromise(uniqueId);
            } else {
              logger.debug("Promise not found for error {}", errorDescription);
            }
          }

          @Override
          public void handleConnectionClosed() {
            if (events != null) events.connectionClosed();
          }

          @Override
          public void handleConnectionOpened() {
            if (events != null) events.connectionOpened();
          }
        });
  }

  /** Disconnect from server */
  public void disconnect() {
    try {
      session.close();
    } catch (Exception ex) {
      logger.info("session.close() failed", ex);
    }
  }

  /**
   * Send a {@link Request} to the server. Can only send {@link Request} that the client supports.
   *
   * @param request outgoing request
   * @return call back object, will be fulfilled with confirmation when received
   * @throws UnsupportedFeatureException trying to send a request from an unsupported feature
   * @throws OccurenceConstraintException Thrown if the request isn't valid.
   * @see CompletableFuture
   */
  public CompletableFuture<Confirmation> send(Request request)
      throws UnsupportedFeatureException, OccurenceConstraintException {
    Optional<Feature> featureOptional = featureRepository.findFeature(request);
    if (!featureOptional.isPresent()) {
      logger.error("Can't send request: unsupported feature. Payload: {}", request);
      throw new UnsupportedFeatureException();
    }

    if (!request.validate()) {
      logger.error("Can't send request: not validated. Payload {}: ", request);
      throw new OccurenceConstraintException();
    }

    String id = session.storeRequest(request);
    CompletableFuture<Confirmation> promise = promiseRepository.createPromise(id);

    session.sendRequest(featureOptional.get().getAction(), request, id);
    return promise;
  }
}
