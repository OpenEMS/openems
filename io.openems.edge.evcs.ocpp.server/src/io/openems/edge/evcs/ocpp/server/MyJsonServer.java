package io.openems.edge.evcs.ocpp.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.AuthenticationException;
import eu.chargetime.ocpp.JSONServer;
import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.ServerEvents;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ServerCoreProfile;
import eu.chargetime.ocpp.feature.profile.ServerFirmwareManagementProfile;
import eu.chargetime.ocpp.feature.profile.ServerLocalAuthListProfile;
import eu.chargetime.ocpp.feature.profile.ServerRemoteTriggerProfile;
import eu.chargetime.ocpp.feature.profile.ServerReservationProfile;
import eu.chargetime.ocpp.feature.profile.ServerSmartChargingProfile;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.SessionInformation;
import eu.chargetime.ocpp.model.core.AvailabilityType;
import eu.chargetime.ocpp.model.core.ChangeAvailabilityRequest;
import eu.chargetime.ocpp.model.core.GetConfigurationConfirmation;
import eu.chargetime.ocpp.model.core.GetConfigurationRequest;
import eu.chargetime.ocpp.model.core.KeyValueType;
import io.openems.edge.evcs.ocpp.common.AbstractManagedOcppEvcsComponent;

public class MyJsonServer {

	private final Logger log = LoggerFactory.getLogger(MyJsonServer.class);

	private final EvcsOcppServer parent;

	/**
	 * The JSON OCPP server.
	 *
	 * <p>
	 * Responsible for sending and receiving OCPP JSON commands.
	 */
	private final JSONServer server;

	// All implemented Profiles
	private final ServerCoreProfile coreProfile;
	private final ServerFirmwareManagementProfile firmwareProfile;
	private final ServerLocalAuthListProfile localAuthListProfile = new ServerLocalAuthListProfile();
	private final ServerRemoteTriggerProfile remoteTriggerProfile = new ServerRemoteTriggerProfile();
	private final ServerReservationProfile reservationProfile = new ServerReservationProfile();
	private final ServerSmartChargingProfile smartChargingProfile = new ServerSmartChargingProfile();

	public MyJsonServer(EvcsOcppServer parent) {
		this.parent = parent;

		this.coreProfile = new ServerCoreProfile(new CoreEventHandlerImpl(parent));
		this.firmwareProfile = new ServerFirmwareManagementProfile(new FirmwareManagementEventHandlerImpl(parent));

		var server = new JSONServer(this.coreProfile);
		server.addFeatureProfile(this.firmwareProfile);
		server.addFeatureProfile(this.localAuthListProfile);
		server.addFeatureProfile(this.remoteTriggerProfile);
		server.addFeatureProfile(this.reservationProfile);
		server.addFeatureProfile(this.smartChargingProfile);
		this.server = server;
	}

	/**
	 * Starting the OCPP Server. Responds to every connecting/disconnecting charging
	 * station.
	 * 
	 * @param ip   the IP address
	 * @param port the port
	 */
	protected void activate(String ip, int port) {

		this.server.open(ip, port, new ServerEvents() {

			@Override
			public void newSession(UUID sessionIndex, SessionInformation information) {
				MyJsonServer.this.logDebug("New session [" + sessionIndex + "] " //
						+ "Chargepoint [" + information.getIdentifier() + "] " //
						+ "IP: " + information.getAddress());

				var ocppIdentifier = information.getIdentifier().replace("/", "");

				MyJsonServer.this.parent.ocppSessions.put(ocppIdentifier, sessionIndex);

				var presentEvcss = MyJsonServer.this.parent.ocppEvcss.get(ocppIdentifier);

				if (presentEvcss == null) {
					return;
				}
				MyJsonServer.this.parent.activeEvcsSessions.put(sessionIndex, presentEvcss);

				for (AbstractManagedOcppEvcsComponent evcs : presentEvcss) {
					evcs.newSession(MyJsonServer.this.parent, sessionIndex);
					MyJsonServer.this.sendInitialRequests(sessionIndex, evcs);
				}
			}

			@Override
			public void lostSession(UUID sessionIndex) {
				MyJsonServer.this.logDebug("Session " + sessionIndex + " lost connection");

				var sessionEvcss = MyJsonServer.this.parent.activeEvcsSessions.getOrDefault(sessionIndex,
						new ArrayList<>());

				if (sessionEvcss != null) {
					for (AbstractManagedOcppEvcsComponent ocppEvcs : sessionEvcss) {
						ocppEvcs.lostSession();
					}
				}

				var ocppId = "";
				for (Entry<String, UUID> session : MyJsonServer.this.parent.ocppSessions.entrySet()) {
					if (session.getValue().equals(sessionIndex)) {
						ocppId = session.getKey();
					}
				}

				MyJsonServer.this.parent.ocppSessions.remove(ocppId);
				MyJsonServer.this.parent.activeEvcsSessions.remove(sessionIndex);
			}

			@Override
			public void authenticateSession(SessionInformation arg0, String arg1, byte[] arg2)
					throws AuthenticationException {
				MyJsonServer.this.logDebug("authenticateSession " + arg0 + "; " + arg1);
			}
		});
	}

	protected void deactivate() {
		this.server.close();
	}

	/**
	 * Send a request to an Evcs using the server.
	 *
	 * @param session unique session id referring to the corresponding Evcs
	 * @param request given request that needs to be sent
	 * @return CompletitionStage
	 * @throws OccurenceConstraintException OccurenceConstraintException
	 * @throws UnsupportedFeatureException  UnsupportedFeatureException
	 * @throws NotConnectedException        NotConnectedException
	 */
	public CompletionStage<Confirmation> send(UUID session, Request request)
			throws OccurenceConstraintException, UnsupportedFeatureException, NotConnectedException {
		return this.server.send(session, request);
	}

	/**
	 * Default implementation of the send method.
	 *
	 * @param session given session
	 * @param request given request
	 */
	public void sendDefault(UUID session, Request request) {
		try {
			this.send(session, request).whenComplete((confirmation, throwable) -> {
				this.logDebug(confirmation.toString());
			});
		} catch (OccurenceConstraintException e) {
			this.logWarn("This is not a valid OCPP request: " + request);
		} catch (UnsupportedFeatureException e) {
			this.logWarn("This feature is not implemented by the charging station: " + request);
		} catch (NotConnectedException e) {
			this.logWarn("The server is not connected: " + request);
		}
	}

	/**
	 * Sending initially all required requests to the EVCS.
	 *
	 * @param sessionIndex given session
	 * @param ocppEvcs     given evcs
	 */
	protected void sendInitialRequests(UUID sessionIndex, AbstractManagedOcppEvcsComponent ocppEvcs) {
		// Setting the Evcss of this session id to available
		var changeAvailabilityRequest = new ChangeAvailabilityRequest(ocppEvcs.getConfiguredConnectorId(),
				AvailabilityType.Operative);
		this.sendDefault(sessionIndex, changeAvailabilityRequest);

		// Sending all required requests defined for each EVCS
		var requiredRequests = ocppEvcs.getRequiredRequestsAfterConnection();
		for (Request request : requiredRequests) {
			this.sendDefault(sessionIndex, request);
		}

		var configuration = this.getConfiguration(sessionIndex);
		this.logDebug(configuration.toString());
	}

	/**
	 * Sending all permanently required requests to the EVCS.
	 *
	 * @param evcss given evcss
	 */
	protected void sendPermanentRequests(List<AbstractManagedOcppEvcsComponent> evcss) {
		if (evcss == null) {
			return;
		}
		for (AbstractManagedOcppEvcsComponent ocppEvcs : evcss) {
			var requiredRequests = ocppEvcs.getRequiredRequestsDuringConnection();
			for (Request request : requiredRequests) {
				this.sendDefault(ocppEvcs.getSessionId(), request);
			}
		}
	}

	private HashMap<String, String> getConfiguration(UUID sessionIndex) {
		var hash = new HashMap<String, String>();
		var request = new GetConfigurationRequest();
		try {
			var resp = this.send(sessionIndex, request);

			var get = (GetConfigurationConfirmation) resp.toCompletableFuture().get(2, TimeUnit.SECONDS);
			var das = get.getConfigurationKey();
			for (KeyValueType element : das) {
				hash.put(element.getKey(), element.getValue());
			}
		} catch (OccurenceConstraintException | UnsupportedFeatureException | NotConnectedException
				| InterruptedException | ExecutionException | java.util.concurrent.TimeoutException ex) {
			this.logDebug(ex.getMessage());
		}
		return hash;
	}

	private void logWarn(String message) {
		this.parent.logWarn(this.log, message);
	}

	private void logDebug(String message) {
		this.parent.logDebug(this.log, message);
	}
}
