package io.openems.edge.evcs.ocpp.api;

import java.util.UUID;

import eu.chargetime.ocpp.model.Request;

public interface OcppServer {

	boolean send(UUID sessionIndex, Request request);
	
}
