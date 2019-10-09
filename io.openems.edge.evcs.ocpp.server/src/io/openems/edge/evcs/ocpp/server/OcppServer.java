package io.openems.edge.evcs.ocpp.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.UUID;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import eu.chargetime.ocpp.JSONServer;
import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.ServerEvents;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.feature.profile.ServerCoreEventHandler;
import eu.chargetime.ocpp.feature.profile.ServerCoreProfile;
import eu.chargetime.ocpp.model.SessionInformation;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.ocpp.unmanaged.EvcsOcppUnmanaged;
import io.openems.edge.evcs.ocpp.unmanaged.EvcsOcppUnmanagedChannelId;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Evcs.Ocpp.Server", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class OcppServer extends AbstractOpenemsComponent implements OpenemsComponent {

	public ServerCoreProfile core;
	public JSONServer server;
	private String femsIP;
	private final ServerCoreEventHandler eventHandler;
	private HashMap<UUID, Evcs> sessionMap = new HashMap<UUID, Evcs>();

	@Reference
	protected ComponentManager componentManager;

	public OcppServer() {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
		this.eventHandler = new EventHandler(this);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException, OccurenceConstraintException,
			UnsupportedFeatureException, NotConnectedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		core = new ServerCoreProfile(this.eventHandler);
		server = new JSONServer(core);

		InetAddress femsIpAddress = InetAddress.getLocalHost();
		this.femsIP = femsIpAddress.getHostAddress().toString();

		server.open(this.femsIP, 8887, new ServerEvents() {

			@Override
			public void lostSession(UUID sessionIndex) {
				System.out.println("Session " + sessionIndex + " lost connection");

				for (HashMap.Entry<UUID, Evcs> entry : sessionMap.entrySet()) {
					UUID session = entry.getKey();
					Evcs evcs = entry.getValue();
					/*
					if (session.equals(sessionIndex)) {
						EvcsOcppUnmanaged ev = new EvcsOcppUnmanaged();
						//evcs.channel(EvcsOcppUnmanagedChannelId.CHARGING_SESSION_ID).setNextValue(null);
						sessionMap.remove(sessionIndex);
					}
					*/
				}
			}

			@Override
			public void newSession(UUID sessionIndex, SessionInformation information) {
				System.out.println("New session " + sessionIndex + ": Chargepoint: " + information.getIdentifier()
						+ ", IP: " + information.getAddress());
				/*
				List<OpenemsComponent> components = componentManager.getEnabledComponents();			//TODO: Find a solution for this discouraged access warning
				for (OpenemsComponent openemsComponent : components) {
					if (openemsComponent instanceof EvcsOcppUnmanaged) { // TODO: All OcppEvcss

						openemsComponent.channel(EvcsOcppUnmanagedChannelId.CHARGING_SESSION_ID).setNextValue(sessionIndex.toString());
						sessionMap.put(sessionIndex, (Evcs) openemsComponent);
						break;
					}
				}
				*/
			}

		});
		/*
		 * // Use the feature profile to help create event ClearCacheRequest request =
		 * core.createClearCacheRequest();
		 * 
		 * UUID sessionIndex = null; // Server returns a promise which will be filled
		 * once it receives a // confirmation. // Select the distination client with the
		 * sessionIndex integer. server.send(sessionIndex,
		 * request).whenComplete((confirmation, throwable) ->
		 * System.out.println(confirmation));
		 */
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

}
