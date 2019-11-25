package io.openems.edge.evcs.ocpp.server;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import eu.chargetime.ocpp.model.SessionInformation;
import io.openems.edge.evcs.ocpp.core.AbstractOcppEvcsComponent;

/**
 * Represents an OCPP session with additional information and the matching EVCS.
 */
public class EvcsSession {

	private UUID sessionId;
	private SessionInformation sessionInformation = new SessionInformation();
	private List<AbstractOcppEvcsComponent> ocppEvcss = new ArrayList<AbstractOcppEvcsComponent>();

	public EvcsSession(UUID sessionId, SessionInformation sessionInformation,
			List<AbstractOcppEvcsComponent> ocppEvcss) {
		this.setSessionId(sessionId);
		this.setSessionInformation(sessionInformation);
		this.setOcppEvcss(ocppEvcss);
	}

	public EvcsSession(UUID sessionId) {
		this.setSessionId(sessionId);
	}

	public UUID getSessionId() {
		return sessionId;
	}

	public void setSessionId(UUID sessionId) {
		this.sessionId = sessionId;
	}

	public SessionInformation getSessionInformation() {
		return sessionInformation;
	}

	public void setSessionInformation(SessionInformation sessionInformation) {
		if (sessionInformation != null) {
			this.sessionInformation = sessionInformation;
		}
	}

	public List<AbstractOcppEvcsComponent> getOcppEvcss() {
		return ocppEvcss;
	}

	public void setOcppEvcss(List<AbstractOcppEvcsComponent> ocppEvcss) {
		if (ocppEvcss != null) {
			this.ocppEvcss = ocppEvcss;
		}
	}

	public boolean hasEvcss() {
		return !ocppEvcss.isEmpty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EvcsSession other = (EvcsSession) obj;
		if (sessionId == null) {
			if (other.sessionId != null)
				return false;
		} else if (!sessionId.equals(other.sessionId))
			return false;
		return true;
	}
}
