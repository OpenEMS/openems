package io.openems.edge.evcs.ocpp.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.osgi.service.cm.ConfigurationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.ocpp.common.AbstractOcppEvcsComponent;

public class MyJsonServer {

	private final Logger log = LoggerFactory.getLogger(MyJsonServer.class);

	private final OcppServerImpl parent;

	/**
	 * Currently connected sessions (Communications with each charging station).
	 */
	private final List<EvcsSession> activeSessions = new ArrayList<EvcsSession>();

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

	public MyJsonServer(OcppServerImpl parent) {
		this.parent = parent;

		this.coreProfile = new ServerCoreProfile(new CoreEventHandlerImpl(parent, this));
		this.firmwareProfile = new ServerFirmwareManagementProfile(new FirmwareManagementEventHandlerImpl(parent));

		JSONServer server = new JSONServer(coreProfile);
		server.addFeatureProfile(firmwareProfile);
		server.addFeatureProfile(localAuthListProfile);
		server.addFeatureProfile(remoteTriggerProfile);
		server.addFeatureProfile(reservationProfile);
		server.addFeatureProfile(smartChargingProfile);
		this.server = server;
	}

	/**
	 * Defining the protocols and starting the OCPP Server. Responds to every
	 * connected/disconnected charging station.
	 */
	public void activate(String ip, int port) {
		server.open(ip, port, new ServerEvents() {

			@Override
			public void lostSession(UUID sessionIndex) {
				MyJsonServer.this.logDebug("Session " + sessionIndex + " lost connection");

				for (EvcsSession evcsSession : MyJsonServer.this.activeSessions) {
					for (MeasuringEvcs measuringEvcs : evcsSession.getOcppEvcss()) {
						if (evcsSession.getSessionId().equals(sessionIndex)) {
							measuringEvcs.channel(AbstractOcppEvcsComponent.ChannelId.CHARGING_SESSION_ID)
									.setNextValue(null);
							measuringEvcs.getChargingstationCommunicationFailed().setNextValue(true);
							// TODO: never remove during a for-loop! Use iterator instead
							activeSessions.remove(evcsSession);
						}
					}
				}
			}

			@Override
			public void newSession(UUID sessionIndex, SessionInformation information) {
				MyJsonServer.this.logDebug("New session [" + sessionIndex + "] " //
						+ "Chargepoint [" + information.getIdentifier() + "] " //
						+ "IP: " + information.getAddress());

				List<AbstractOcppEvcsComponent> evcssWithThisId = MyJsonServer.this.parent
						.getComponentsWithIdentifier(information.getIdentifier(), sessionIndex);
				activeSessions.add(new EvcsSession(sessionIndex, information, evcssWithThisId));

				MyJsonServer.this.sendInitialRequests(sessionIndex, evcssWithThisId);
			}
		});
	}

	/**
	 * Sending initially all required requests to the EVCS
	 * 
	 * @param sessionIndex
	 * @param evcss
	 */
	private void sendInitialRequests(UUID sessionIndex, List<AbstractOcppEvcsComponent> evcss) {
		for (AbstractOcppEvcsComponent ocppEvcs : evcss) {
			// Setting the Evcss of this session id to available
			ChangeAvailabilityRequest changeAvailabilityRequest = new ChangeAvailabilityRequest(
					ocppEvcs.getConnectorId().value().orElse(0), // TODO "0" seems to be a bad idea here 
					AvailabilityType.Operative);
			this.sendDefault(sessionIndex, changeAvailabilityRequest);

			// Sending all required requests defined for each EVCS
			List<Request> requiredRequests = ocppEvcs.getRequiredRequestsAfterConnection();
			for (Request request : requiredRequests) {
				this.sendDefault(sessionIndex, request);
			}

			HashMap<String, String> configuration = getConfiguration(sessionIndex);
			this.logDebug(configuration.toString());
		}
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
	 * Get all active Sessions
	 * 
	 * @return List of EvcsSessions
	 */
	public List<EvcsSession> getActiveSessions() {
		return this.activeSessions;
	}

	/**
	 * Searching again for all Sessions after the configurations changed.
	 */
	public void configurationEvent(ConfigurationEvent event) {
		for (EvcsSession evcsSession : this.activeSessions) {
			List<AbstractOcppEvcsComponent> evcss = this.parent.getComponentsWithIdentifier(
					evcsSession.getSessionInformation().getIdentifier(), evcsSession.getSessionId());
			if (!evcss.isEmpty()) {
				evcsSession.setOcppEvcss(evcss);
			} else {
				this.logDebug("No EVCS component found for " //
						+ "Session [" + evcsSession.getSessionId() + "] and " //
						+ "OCPP Identifier [" + evcsSession.getSessionInformation().getIdentifier() + "].");
			}
		}
	}

	private HashMap<String, String> getConfiguration(UUID sessionIndex) {
		HashMap<String, String> hash = new HashMap<>();
		GetConfigurationRequest request = new GetConfigurationRequest();
		try {
			CompletionStage<Confirmation> resp = this.send(sessionIndex, request);

			GetConfigurationConfirmation get = (GetConfigurationConfirmation) resp.toCompletableFuture().get(2,
					TimeUnit.SECONDS);
			KeyValueType[] das = get.getConfigurationKey();
			for (int i = 0; i < das.length; i++) {
				hash.put(das[i].getKey(), das[i].getValue());
			}
		} catch (OccurenceConstraintException | UnsupportedFeatureException | NotConnectedException
				| InterruptedException | ExecutionException ex) {

		} catch (java.util.concurrent.TimeoutException ex) {
		}
		return hash;
	}

	public void deactivate() {
		this.server.close();
	}

	public CompletionStage<Confirmation> send(UUID session, Request request)
			throws OccurenceConstraintException, UnsupportedFeatureException, NotConnectedException {
		return this.server.send(session, request);
	}

	private void logWarn(String message) {
		this.parent.logWarn(this.log, message);
	}

	private void logDebug(String message) {
		this.parent.logDebug(this.log, message);
	}

}
