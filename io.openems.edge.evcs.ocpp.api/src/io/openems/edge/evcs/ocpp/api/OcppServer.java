package io.openems.edge.evcs.ocpp.api;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;

public interface OcppServer {

	/**
	 * Send message to EVCS.
	 * <p>
	 * Example: <blockquote>
	 * 
	 * <pre>
	 * send(session, request).whenComplete((confirmation, throwable) {
	 *    this.logInfo(log, confirmation.toString());
	 * });
	 * </pre>
	 * 
	 * </blockquote>
	 *
	 * @param session Current session
	 * @param request Request that will be sent
	 * @return When the request has been sent and a confirmation is received
	 * @throws NotConnectedException
	 * @throws UnsupportedFeatureException
	 * @throws OccurenceConstraintException
	 */
	public CompletionStage<Confirmation> send(UUID session, Request request)
			throws OccurenceConstraintException, UnsupportedFeatureException, NotConnectedException;
}
