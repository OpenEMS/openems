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
	 * Send message to EVCS. Returns true if sent successfully
	 * 
	 * @param session Session Index of the Evcs
	 * @param request OCPP request 
	 * @return boolean
	 */
	//boolean send(UUID session, Request request);

	public CompletionStage<Confirmation> send(UUID session, Request request) throws OccurenceConstraintException, UnsupportedFeatureException, NotConnectedException ;
	
}
