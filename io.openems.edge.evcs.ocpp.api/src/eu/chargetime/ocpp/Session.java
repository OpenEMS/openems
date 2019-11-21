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
import eu.chargetime.ocpp.utilities.MoreObjects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unites outgoing {@link Request} with incoming {@link Confirmation}s or
 * errors. Catches errors and responds with error messages.
 */
public class Session implements ISession {

	private static final Logger logger = LoggerFactory.getLogger(Session.class);

	private final UUID sessionId = UUID.randomUUID();
	private final Communicator communicator;
	private final Queue queue;
	private final RequestDispatcher dispatcher;
	private final IFeatureRepository featureRepository;
	private SessionEvents events;

	/**
	 * Handles required injections.
	 *
	 * @param communicator send and receive messages.
	 * @param queue        store and restore requests based on unique ids.
	 */
	public Session(Communicator communicator, Queue queue, PromiseFulfiller fulfiller,
			IFeatureRepository featureRepository) {
		this.communicator = communicator;
		this.queue = queue;
		this.dispatcher = new RequestDispatcher(fulfiller);
		this.featureRepository = featureRepository;
	}

	/**
	 * Get a unique session {@link UUID} identifier.
	 *
	 * @return the unique session {@link UUID} identifier
	 */
	public UUID getSessionId() {
		return sessionId;
	}

	/**
	 * Send a {@link Request}.
	 *
	 * @param action  action name to identify the feature.
	 * @param payload the {@link Request} payload to send
	 * @param uuid    unique identification to identify the request
	 */
	public void sendRequest(String action, Request payload, String uuid) {
		communicator.sendCall(uuid, action, payload);
	}

	/**
	 * Store a {@link Request} and get the unique id.
	 *
	 * @param payload the {@link Request} payload to send
	 * @return unique identification to identify the request.
	 */
	public String storeRequest(Request payload) {
		return queue.store(payload);
	}

	/**
	 * Send a {@link Confirmation} to a {@link Request}
	 *
	 * @param uniqueId     the unique identification the receiver expects.
	 * @param confirmation the {@link Confirmation} payload to send.
	 */
	public void sendConfirmation(String uniqueId, String action, Confirmation confirmation) {
		communicator.sendCallResult(uniqueId, action, confirmation);
	}

	private Optional<Class<? extends Confirmation>> getConfirmationType(String uniqueId)
			throws UnsupportedFeatureException {
		Optional<Request> requestOptional = queue.restoreRequest(uniqueId);

		if (requestOptional.isPresent()) {
			Optional<Feature> featureOptional = featureRepository.findFeature(requestOptional.get());
			if (featureOptional.isPresent()) {
				return Optional.of(featureOptional.get().getConfirmationType());
			} else {
				logger.debug("Feature for request with id: {} not found in session: {}", uniqueId, this);
				throw new UnsupportedFeatureException(
						"Error with getting confirmation type by request id = " + uniqueId);
			}
		} else {
			logger.debug("Request with id: {} not found in session: {}", uniqueId, this);
		}

		return Optional.empty();
	}

	/**
	 * Connect to a specific uri, provided a call back handler for connection
	 * related events.
	 *
	 * @param uri          url and port of the remote system.
	 * @param eventHandler call back handler for connection related events.
	 */
	public void open(String uri, SessionEvents eventHandler) {
		this.events = eventHandler;
		dispatcher.setEventHandler(eventHandler);
		communicator.connect(uri, new CommunicatorEventHandler());
	}

	/** Close down the connection. */
	public void close() {
		communicator.disconnect();
	}

	public void accept(SessionEvents eventHandler) {
		this.events = eventHandler;
		dispatcher.setEventHandler(eventHandler);
		communicator.accept(new CommunicatorEventHandler());
	}

	private class CommunicatorEventHandler implements CommunicatorEvents {
		private static final String OCCURENCE_CONSTRAINT_VIOLATION = "Payload for Action is syntactically correct but at least one of the fields violates occurence constraints";
		private static final String INTERNAL_ERROR = "An internal error occurred and the receiver was not able to process the requested Action successfully";
		private static final String UNABLE_TO_PROCESS = "Unable to process action";

		@Override
		public void onCallResult(String id, String action, Object payload) {
			try {
				Optional<Class<? extends Confirmation>> confirmationTypeOptional = getConfirmationType(id);

				if (confirmationTypeOptional.isPresent()) {
					Confirmation confirmation = communicator.unpackPayload(payload, confirmationTypeOptional.get());
					if (confirmation.validate()) {
						events.handleConfirmation(id, confirmation);
					} else {
						communicator.sendCallError(id, action, "OccurenceConstraintViolation",
								OCCURENCE_CONSTRAINT_VIOLATION);
					}
				} else {
					logger.warn(INTERNAL_ERROR);
					communicator.sendCallError(id, action, "InternalError", INTERNAL_ERROR);
				}
			} catch (PropertyConstraintException ex) {
				logger.warn(ex.getMessage(), ex);
				communicator.sendCallError(id, action, "TypeConstraintViolation", ex.getMessage());
			} catch (UnsupportedFeatureException ex) {
				logger.warn(INTERNAL_ERROR, ex);
				communicator.sendCallError(id, action, "InternalError", INTERNAL_ERROR);
			} catch (Exception ex) {
				logger.warn(UNABLE_TO_PROCESS, ex);
				communicator.sendCallError(id, action, "FormationViolation", UNABLE_TO_PROCESS);
			}
		}

		@Override
		public synchronized void onCall(String id, String action, Object payload) {
			Optional<Feature> featureOptional = featureRepository.findFeature(action);
			if (!featureOptional.isPresent()) {
				communicator.sendCallError(id, action, "NotImplemented", "Requested Action is not known by receiver");
			} else {
				try {
					Request request = communicator.unpackPayload(payload, featureOptional.get().getRequestType());
					if (request.validate()) {
						CompletableFuture<Confirmation> promise = dispatcher.handleRequest(request);
						promise.whenComplete(new ConfirmationHandler(id, action, communicator));
					} else {
						communicator.sendCallError(id, action, "OccurenceConstraintViolation",
								OCCURENCE_CONSTRAINT_VIOLATION);
					}
				} catch (PropertyConstraintException ex) {
					logger.warn(ex.getMessage(), ex);
					communicator.sendCallError(id, action, "TypeConstraintViolation", ex.getMessage());
				} catch (Exception ex) {
					logger.warn(UNABLE_TO_PROCESS, ex);
					communicator.sendCallError(id, action, "FormationViolation", UNABLE_TO_PROCESS);
				}
			}
		}

		@Override
		public void onError(String id, String errorCode, String errorDescription, Object payload) {
			events.handleError(id, errorCode, errorDescription, payload);
		}

		@Override
		public void onDisconnected() {
			events.handleConnectionClosed();
		}

		@Override
		public void onConnected() {
			events.handleConnectionOpened();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Session session = (Session) o;
		return MoreObjects.equals(sessionId, session.sessionId);
	}

	@Override
	public int hashCode() {
		return MoreObjects.hash(sessionId);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("sessionId", sessionId).add("communicator", communicator)
				.add("queue", queue).add("dispatcher", dispatcher).add("featureRepository", featureRepository)
				.add("events", events).toString();
	}
}
