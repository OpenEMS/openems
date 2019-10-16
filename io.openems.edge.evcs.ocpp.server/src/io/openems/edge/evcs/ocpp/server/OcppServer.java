package io.openems.edge.evcs.ocpp.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import eu.chargetime.ocpp.JSONServer;
import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.ServerEvents;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ServerCoreProfile;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.SessionInformation;
import eu.chargetime.ocpp.model.core.ChargingProfile;
import eu.chargetime.ocpp.model.core.ChargingProfileKindType;
import eu.chargetime.ocpp.model.core.ChargingProfilePurposeType;
import eu.chargetime.ocpp.model.core.ChargingRateUnitType;
import eu.chargetime.ocpp.model.core.ChargingSchedule;
import eu.chargetime.ocpp.model.core.ChargingSchedulePeriod;
import eu.chargetime.ocpp.model.smartcharging.SetChargingProfileRequest;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.OcppEvcs;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Evcs.Ocpp.Server", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class OcppServer extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

	public ServerCoreProfile core;
	public JSONServer server;
	private String femsIP;
	private OcppEventHandler ocppEventHandler;
	protected HashMap<UUID, OcppEvcs> sessionMap = new HashMap<UUID, OcppEvcs>();

	@Reference
	protected ComponentManager componentManager;

	public OcppServer() {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
		this.ocppEventHandler = new OcppEventHandler(this);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException, OccurenceConstraintException,
			UnsupportedFeatureException, NotConnectedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		core = new ServerCoreProfile(this.ocppEventHandler);
		server = new JSONServer(core);

		InetAddress femsIpAddress = InetAddress.getLocalHost();
		this.femsIP = femsIpAddress.getHostAddress().toString();

		server.open(this.femsIP, 8887, new ServerEvents() {

			@Override
			public void lostSession(UUID sessionIndex) {
				System.out.println("Session " + sessionIndex + " lost connection");

				for (HashMap.Entry<UUID, OcppEvcs> entry : sessionMap.entrySet()) {
					UUID session = entry.getKey();
					Evcs evcs = entry.getValue();
					if (session.equals(sessionIndex)) {
						evcs.channel(OcppEvcs.ChannelId.CHARGING_SESSION_ID).setNextValue(null);
						sessionMap.remove(sessionIndex);
					}
				}
			}

			@Override
			public void newSession(UUID sessionIndex, SessionInformation information) {
				System.out.println("New session " + sessionIndex + ": Chargepoint: " + information.getIdentifier()
						+ ", IP: " + information.getAddress());

				List<OpenemsComponent> components = componentManager.getEnabledComponents();
				for (OpenemsComponent openemsComponent : components) {
					if (openemsComponent instanceof OcppEvcs) { // TODO: All OcppEvcss
						StringReadChannel channelOcppId = openemsComponent.channel(OcppEvcs.ChannelId.OCPP_ID);
						String ocppId = channelOcppId.value().orElse("");
						if (information.getIdentifier().equals("/" + ocppId)) {
							openemsComponent.channel(OcppEvcs.ChannelId.CHARGING_SESSION_ID)
									.setNextValue(sessionIndex.toString());
							sessionMap.put(sessionIndex, (OcppEvcs) openemsComponent);
							break;
						}
					}
				}
			}
		});
		/*
		 * //
		 */
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		server.close();
	}

	/**
	 * Send message to EVCS. Returns true if sent successfully
	 *
	 * @param sessionIndex Current session
	 * @param request      Request that will be sent
	 * @return
	 * @throws NotConnectedException
	 * @throws UnsupportedFeatureException
	 * @throws OccurenceConstraintException
	 */
	protected boolean send(UUID sessionIndex, Request request) {

		// Use the feature profile to help create event
		// e.g. ClearCacheRequest request = core.createClearCacheRequest();

		try {
			server.send(sessionIndex, request).whenComplete((confirmation, throwable) -> {
				System.out.println(confirmation);
			});
		} catch (OccurenceConstraintException e) {
			System.out.println("The request is not a valid OCPP request.");
		} catch (UnsupportedFeatureException e) {
			System.out.println("This feature is not implemented by the charging station.");
		} catch (NotConnectedException e) {
			System.out.println("The server is not connected.");
		}
		return true;
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:

			for (HashMap.Entry<UUID, OcppEvcs> entry : sessionMap.entrySet()) {
				Evcs evcs = entry.getValue();
				if (evcs != null) {
					/* Change availability example
					ChangeAvailabilityRequest changeAvailabilityRequest = new ChangeAvailabilityRequest();
					changeAvailabilityRequest.setConnectorId(0);
					changeAvailabilityRequest.setType(AvailabilityType.Operative);
					this.send(entry.getKey(), changeAvailabilityRequest);
					*/
					SetChargingProfileRequest chargingProfile = new SetChargingProfileRequest();
					chargingProfile.setConnectorId(0);
					ChargingProfile csChargingProfile = new ChargingProfile();
					csChargingProfile.setChargingProfileId(100);
					csChargingProfile.setStackLevel(0);
					csChargingProfile.setChargingProfilePurpose(ChargingProfilePurposeType.ChargePointMaxProfile);
					csChargingProfile.setChargingProfileKind(ChargingProfileKindType.Absolute);
					
					ChargingSchedule cs = new ChargingSchedule();
					cs.setChargingRateUnit(ChargingRateUnitType.W);
					ChargingSchedulePeriod[] cspArr = new ChargingSchedulePeriod[1];
					ChargingSchedulePeriod csp = new ChargingSchedulePeriod();
					csp.setLimit(10.0);
					csp.setNumberPhases(3);
					cspArr[0] = csp;
					//cs.setChargingSchedulePeriod();
					//csChargingProfile.setChargingSchedule();
					chargingProfile.setCsChargingProfiles(csChargingProfile);
					
					//this.send(entry.getKey(), chargingProfile);
				}
			}
			// handle writes
			// this.writeHandler.run();
			break;
		}
	}
}
