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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.model.Confirmation;
import eu.chargetime.ocpp.model.Request;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.ocpp.common.AbstractOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.common.OcppServer;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Ocpp.Server", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class OcppServerImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, ConfigurationListener, OcppServer, EventHandler {

	public final static String DEFAULT_IP = "0.0.0.0";
	public final static int DEFAULT_PORT = 8887;

	private final Logger log = LoggerFactory.getLogger(OcppServerImpl.class);
	protected Config config;

	@Reference
	protected ComponentManager componentManager;

	private final MyJsonServer myJsonServer = new MyJsonServer(this);

	public OcppServerImpl() {
		super(OpenemsComponent.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException, OccurenceConstraintException,
			UnsupportedFeatureException, NotConnectedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.config = config;
		this.myJsonServer.activate(config.ip(), config.port());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.myJsonServer.deactivate();
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			for (EvcsSession evcsSession : this.myJsonServer.getActiveSessions()) {
				// TODO: Put that logic in ReadHandler
				this.sendPermanentRequests(evcsSession);
			}
			break;
		}
	}

	@Override
	public CompletionStage<Confirmation> send(UUID session, Request request)
			throws OccurenceConstraintException, UnsupportedFeatureException, NotConnectedException {
		return this.myJsonServer.send(session, request);
	}

	/**
	 * Sending all permanently required requests to the EVCS
	 * 
	 * @param sessionIndex
	 * @param evcss
	 */
	private void sendPermanentRequests(EvcsSession evcsSession) {
		for (AbstractOcppEvcsComponent evcs : evcsSession.getOcppEvcss()) {
			List<Request> requiredRequests = evcs.getRequiredRequestsDuringConnection();
			for (Request request : requiredRequests) {
				this.myJsonServer.sendDefault(evcsSession.getSessionId(), request);
			}
		}
	}

	/**
	 * Searching the OcppEvcs components for the given identifier.
	 * 
	 * @param identifier   given identifier
	 * @param sessionIndex given session
	 * @return List of AbstractOcppEvcsComponents
	 */
	protected List<AbstractOcppEvcsComponent> getComponentsWithIdentifier(String identifier, UUID sessionIndex) {
		List<AbstractOcppEvcsComponent> result = new ArrayList<AbstractOcppEvcsComponent>();
		List<OpenemsComponent> components = componentManager.getEnabledComponents();

		for (OpenemsComponent openemsComponent : components) {
			if (openemsComponent instanceof AbstractOcppEvcsComponent) {
				AbstractOcppEvcsComponent ocppEvcs = (AbstractOcppEvcsComponent) openemsComponent;
				String ocppId = ocppEvcs.getOcppId().value().orElse(""); // TODO: Channel unnötig

				if (identifier.equals("/" + ocppId)) {
					// TODO this does not seem to belong to a 'get' method:
					ocppEvcs.getChargingSessionId().setNextValue(sessionIndex.toString());
					ocppEvcs.getChargingstationCommunicationFailed().setNextValue(false);
					ocppEvcs.getStatus().setNextValue(Status.NOT_READY_FOR_CHARGING);
					result.add(ocppEvcs);
				}
			}
		}
		return result;
	}

	/**
	 * Searching again for all Sessions after the configurations changed.
	 */
	@Override
	public void configurationEvent(ConfigurationEvent event) {
		for (EvcsSession evcsSession : this.getActiveSessions()) {
			List<AbstractOcppEvcsComponent> evcss = getComponentsWithIdentifier(
					evcsSession.getSessionInformation().getIdentifier(), evcsSession.getSessionId());
			if (!evcss.isEmpty()) {
				evcsSession.setOcppEvcss(evcss);
			} else {
				this.logDebug(this.log, "No EVCS component found for " //
						+ "Session [" + evcsSession.getSessionId() + "] and " //
						+ "OCPP Identifier [" + evcsSession.getSessionInformation().getIdentifier() + "].");
			}
		}
	}

	/**
	 * Get all active sessions.
	 * 
	 * @return List of EvcsSessions
	 */
	public List<EvcsSession> getActiveSessions() {
		return this.myJsonServer.getActiveSessions();
	}

	@Override
	public void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}
}
