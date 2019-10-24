package io.openems.edge.evcs.ocpp.server;

import java.net.UnknownHostException;
import java.util.ArrayList;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.JSONServer;
import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.ServerEvents;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ServerCoreProfile;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.SessionInformation;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.OcppEvcs;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Evcs.Ocpp.Server", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class OcppServer extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(OcppServer.class);
	public ServerCoreProfile core;
	public JSONServer server;
	private OcppEventHandler ocppEventHandler;
	protected HashMap<UUID, List<OcppEvcs>> sessionMap = new HashMap<UUID, List<OcppEvcs>>();

	@Reference
	protected ComponentManager componentManager;

	public OcppServer() {
		super(
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
		
		server.open("0.0.0.0", 8887, new ServerEvents() {

			@Override
			public void lostSession(UUID sessionIndex) {
				logInfo(log, "Session " + sessionIndex + " lost connection");

				for (HashMap.Entry<UUID, List<OcppEvcs>> entry : sessionMap.entrySet()) {
					UUID session = entry.getKey();
					List<OcppEvcs> evcss = entry.getValue();
					for (OcppEvcs ocppEvcs : evcss) {
						if (session.equals(sessionIndex)) {
							ocppEvcs.channel(OcppEvcs.ChannelId.CHARGING_SESSION_ID).setNextValue(null);
							ocppEvcs.getChargingstationCommunicationFailed().setNextValue(true);
							sessionMap.remove(sessionIndex);
						}
					}
				}
			}

			@Override
			public void newSession(UUID sessionIndex, SessionInformation information) {
				logInfo(log, "New session " + sessionIndex + ": Chargepoint: " + information.getIdentifier()+ ", IP: " + information.getAddress());

				List<OpenemsComponent> components = componentManager.getEnabledComponents();
				List<OcppEvcs> evcssWithThisId = new ArrayList<OcppEvcs>();
				for (OpenemsComponent openemsComponent : components) {
					if (openemsComponent instanceof OcppEvcs) {
						StringReadChannel channelOcppId = openemsComponent.channel(OcppEvcs.ChannelId.OCPP_ID);
						String ocppId = channelOcppId.value().orElse("");
						if (information.getIdentifier().equals("/" + ocppId)) {
							openemsComponent.channel(OcppEvcs.ChannelId.CHARGING_SESSION_ID)
									.setNextValue(sessionIndex.toString());
							evcssWithThisId.add((OcppEvcs) openemsComponent);
						}
					}
				}
				sessionMap.put(sessionIndex, evcssWithThisId);
			}
		});
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
	 * @param request Request that will be sent
	 * @return When the request has been sent and a confirmation is received
	 * @throws NotConnectedException
	 * @throws UnsupportedFeatureException
	 * @throws OccurenceConstraintException
	 */
	protected boolean send(UUID sessionIndex, Request request) {
		try {
			server.send(sessionIndex, request).whenComplete((confirmation, throwable) -> {
				this.logInfo(log, confirmation.toString());
			});
			return true;
		} catch (OccurenceConstraintException e) {
			this.logWarn(log, "The request is not a valid OCPP request.");
		} catch (UnsupportedFeatureException e) {
			this.logWarn(log, "This feature is not implemented by the charging station.");
		} catch (NotConnectedException e) {
			this.logWarn(log, "The server is not connected.");
		}
		return false;
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			if(sessionMap.isEmpty()) {
				this.logInfo(this.log, "No charging stations connected. Please insert the OCPP server address 'ws://femsIp:8887' into the charging stations.");
			}
			for (HashMap.Entry<UUID, List<OcppEvcs>> entry : sessionMap.entrySet()) {
				List<OcppEvcs> evcs = entry.getValue();
				if (evcs != null) {
					// handle writes
					// this.writeHandler.run();
				}
			}
			break;
		}
	}
	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}
}
