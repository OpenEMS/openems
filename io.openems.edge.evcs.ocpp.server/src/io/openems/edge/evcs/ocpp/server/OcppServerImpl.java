package io.openems.edge.evcs.ocpp.server;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
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
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.ocpp.common.AbstractManagedOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.common.OcppServer;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Ocpp.Server", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class OcppServerImpl extends AbstractOpenemsComponent implements OpenemsComponent, OcppServer, EventHandler {

	public static final String DEFAULT_IP = "0.0.0.0";
	public static final int DEFAULT_PORT = 8887;

	private final Logger log = LoggerFactory.getLogger(OcppServerImpl.class);

	/** The JSON server - responsible for the OCPP communication. */
	private final MyJsonServer myJsonServer = new MyJsonServer(this);
	/** Currently connected sessions with their related evcs components. */
	protected final Map<UUID, List<AbstractManagedOcppEvcsComponent>> activeEvcsSessions = new HashMap<>();

	@Reference
	protected ComponentManager componentManager;

	protected Config config;
	/** Currently configured ocpp evcss. */
	protected Map<String, List<AbstractManagedOcppEvcsComponent>> ocppEvcss = new HashMap<>();
	/** Current sessions (Existing connections between server and evcs hardware). */
	protected Map<String, UUID> ocppSessions = new HashMap<>();

	/**
	 * Adds each Evcs component to a list and checks whether there is a matching
	 * session.
	 *
	 * @param evcs new Evcs
	 */
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addEvcs(Evcs evcs) {
		if (!(evcs instanceof AbstractManagedOcppEvcsComponent) || evcs == null) {
			return;
		}
		var ocppEvcs = (AbstractManagedOcppEvcsComponent) evcs;
		var presentEvcss = this.ocppEvcss.get(ocppEvcs.getConfiguredOcppId());

		if (presentEvcss == null) {
			var initEvcssArr = new ArrayList<AbstractManagedOcppEvcsComponent>();
			initEvcssArr.add(ocppEvcs);
			this.ocppEvcss.put(ocppEvcs.getConfiguredOcppId(), initEvcssArr);
		} else {
			presentEvcss.add(ocppEvcs);
		}

		var sessionId = this.ocppSessions.get(ocppEvcs.getConfiguredOcppId());
		if (sessionId == null) {
			return;
		}
		this.activeEvcsSessions.put(sessionId, presentEvcss);
		ocppEvcs.newSession(this, sessionId);
		this.myJsonServer.sendInitialRequests(sessionId, ocppEvcs);
	}

	/**
	 * Removes the given Evcs component from the list and checks whether there is a
	 * present session that should be removed.
	 *
	 * @param evcs Evcs that should be removed
	 */
	protected void removeEvcs(Evcs evcs) {
		if (!(evcs instanceof AbstractManagedOcppEvcsComponent) || evcs == null) {
			return;
		}
		var ocppEvcs = (AbstractManagedOcppEvcsComponent) evcs;
		var evcss = this.activeEvcsSessions.get(ocppEvcs.getSessionId());
		if (evcss != null) {
			if (evcss.size() < 2) {
				this.activeEvcsSessions.remove(ocppEvcs.getSessionId());
			} else {
				this.activeEvcsSessions.get(ocppEvcs.getSessionId()).remove(ocppEvcs);
			}
		}
		this.ocppEvcss.remove(ocppEvcs.getConfiguredOcppId());
		ocppEvcs.lostSession();
	}

	public OcppServerImpl() {
		super(OpenemsComponent.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException,
			OccurenceConstraintException, UnsupportedFeatureException, NotConnectedException {
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
			for (Entry<UUID, List<AbstractManagedOcppEvcsComponent>> evcsSessions : this.activeEvcsSessions
					.entrySet()) {
				this.myJsonServer.sendPermanentRequests(evcsSessions.getValue());
			}
			break;
		}
	}

	@Override
	public CompletionStage<Confirmation> send(UUID session, Request request)
			throws OccurenceConstraintException, UnsupportedFeatureException, NotConnectedException {
		return this.myJsonServer.send(session, request);
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
