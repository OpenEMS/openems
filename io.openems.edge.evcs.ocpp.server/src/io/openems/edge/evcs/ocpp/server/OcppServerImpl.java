package io.openems.edge.evcs.ocpp.server;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;
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
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.ocpp.api.AbstractOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.api.OcppServer;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Evcs.Ocpp.Server", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class OcppServerImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, ConfigurationListener, OcppServer {

	private final Logger log = LoggerFactory.getLogger(OcppServerImpl.class);

	/**
	 * The JSON OCPP server.
	 * <p>
	 * Responsible for sending and receiving OCPP JSON commands
	 */
	private JSONServer server;

	// All implemented Profiles
	private ServerCoreProfile coreProfile;
	private ServerFirmwareManagementProfile firmwareProfile;
	private ServerLocalAuthListProfile localAuthListProfile = new ServerLocalAuthListProfile();
	private ServerRemoteTriggerProfile remoteTriggerProfile = new ServerRemoteTriggerProfile();
	private ServerReservationProfile reservationProfile = new ServerReservationProfile();
	private ServerSmartChargingProfile smartChargingProfile = new ServerSmartChargingProfile();

	// Currently connected sessions (Communications with each charging station)
	protected List<EvcsSession> activeSessions = new ArrayList<EvcsSession>();

	@Reference
	protected ComponentManager componentManager;

	public OcppServerImpl() {
		super(OpenemsComponent.ChannelId.values() //
		);
		this.coreProfile = new ServerCoreProfile(new CoreEventHandlerImpl(this));
		this.firmwareProfile = new ServerFirmwareManagementProfile(new FirmwareManagementEventHandlerImpl(this));
	}

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException, OccurenceConstraintException,
			UnsupportedFeatureException, NotConnectedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		startServer();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		server.close();
	}

	private void startServer() {
		server = new JSONServer(coreProfile);
		server.addFeatureProfile(firmwareProfile);
		server.addFeatureProfile(localAuthListProfile);
		server.addFeatureProfile(remoteTriggerProfile);
		server.addFeatureProfile(reservationProfile);
		server.addFeatureProfile(smartChargingProfile);

		server.open("0.0.0.0", 8887, new ServerEvents() {

			@Override
			public void lostSession(UUID sessionIndex) {
				logInfo(log, "Session " + sessionIndex + " lost connection");

				for (EvcsSession evcsSession : activeSessions) {
					for (MeasuringEvcs measuringEvcs : evcsSession.getOcppEvcss()) {
						if (evcsSession.getSessionId().equals(sessionIndex)) {
							measuringEvcs.channel(AbstractOcppEvcsComponent.ChannelId.CHARGING_SESSION_ID)
									.setNextValue(null);
							measuringEvcs.getChargingstationCommunicationFailed().setNextValue(true);
							activeSessions.remove(evcsSession);
						}
					}
				}
			}

			@Override
			public void newSession(UUID sessionIndex, SessionInformation information) {
				logInfo(log, "New session " + sessionIndex + ": Chargepoint: " + information.getIdentifier() + ", IP: "
						+ information.getAddress());

				List<AbstractOcppEvcsComponent> evcssWithThisId = searchForComponentWithThatIdentifier(
						information.getIdentifier(), sessionIndex);
				activeSessions.add(new EvcsSession(sessionIndex, information, evcssWithThisId));

				ChangeAvailabilityRequest changeAvailabilityRequest = new ChangeAvailabilityRequest();
				for (AbstractOcppEvcsComponent ocppEvcs : evcssWithThisId) {
					logInfo(log, "Setting EVCS " + ocppEvcs.alias() + " availability to operative");
					changeAvailabilityRequest.setConnectorId(ocppEvcs.getConnectorId().value().orElse(0));
					changeAvailabilityRequest.setType(AvailabilityType.Operative);
					sendDefault(sessionIndex, changeAvailabilityRequest);
				}
			}
		});
	}

	@Override
	public CompletionStage<Confirmation> send(UUID session, Request request)
			throws OccurenceConstraintException, UnsupportedFeatureException, NotConnectedException {
		return server.send(session, request);
	}

	/**
	 * Default implementation of the send method.
	 * 
	 * @param session
	 * @param request
	 */
	public void sendDefault(UUID session, Request request) {
		try {
			this.send(session, request).whenComplete((confirmation, throwable) -> {
				this.logInfo(log, confirmation.toString());
			});
		} catch (OccurenceConstraintException e) {
			this.logWarn(log, "The request is not a valid OCPP request.");
		} catch (UnsupportedFeatureException e) {
			this.logWarn(log, "This feature is not implemented by the charging station.");
		} catch (NotConnectedException e) {
			this.logWarn(log, "The server is not connected.");
		}
	}

	/**
	 * Searching the OcppEvcs Components for the given identifier.
	 * 
	 * @param identifier
	 * @param sessionIndex
	 * @return List<AbstractOcppEvcsComponent>
	 */
	private List<AbstractOcppEvcsComponent> searchForComponentWithThatIdentifier(String identifier, UUID sessionIndex) {
		List<AbstractOcppEvcsComponent> evcssWithThisId = new ArrayList<AbstractOcppEvcsComponent>();
		List<OpenemsComponent> components = componentManager.getEnabledComponents();

		for (OpenemsComponent openemsComponent : components) {
			if (openemsComponent instanceof AbstractOcppEvcsComponent) {
				AbstractOcppEvcsComponent ocppEvcs = (AbstractOcppEvcsComponent) openemsComponent;
				String ocppId = ocppEvcs.getOcppId().value().orElse("");

				if (identifier.equals("/" + ocppId)) {
					ocppEvcs.getChargingSessionId().setNextValue(sessionIndex.toString());
					ocppEvcs.getChargingstationCommunicationFailed().setNextValue(false);
					ocppEvcs.status().setNextValue(Status.NOT_READY_FOR_CHARGING);
					evcssWithThisId.add(ocppEvcs);
				}
			}
		}
		return evcssWithThisId;
	}

	@Override
	public void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	/**
	 * Searching again for all Sessions after the configurations changed
	 */
	@Override
	public void configurationEvent(ConfigurationEvent event) {
		for (EvcsSession evcsSession : activeSessions) {
			List<AbstractOcppEvcsComponent> evcss = searchForComponentWithThatIdentifier(
					evcsSession.getSessionInformation().getIdentifier(), evcsSession.getSessionId());
			if (!evcss.isEmpty()) {
				evcsSession.setOcppEvcss(evcss);
			} else {
				this.logInfo(this.log, "No EVCS component found for Session " + evcsSession.getSessionId()
						+ " and OCPP Identifier " + evcsSession.getSessionInformation().getIdentifier() + ".");
			}
		}
	}

	public List<EvcsSession> getActiveSessions() {
		return activeSessions;
	}
}
