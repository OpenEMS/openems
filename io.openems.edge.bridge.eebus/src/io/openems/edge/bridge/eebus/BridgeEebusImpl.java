package io.openems.edge.bridge.eebus;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static org.openmuc.jeebus.shipspine.ShipCommunication.ConnectClientsTo.TRUSTED;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.bridge.http.time.periodic.PeriodicExecutor;
import io.openems.common.bridge.http.time.periodic.PeriodicExecutorFactory;
import org.openmuc.jeebus.ship.api.ShipConnectionInfoSnapshot;
import org.openmuc.jeebus.ship.api.ShipNodeConfiguration;
import org.openmuc.jeebus.ship.node.KeyManagement;
import org.openmuc.jeebus.shipspine.ShipCommunication;
import org.openmuc.jeebus.spine.api.Device;
import org.openmuc.jeebus.spine.xsd.v1.DeviceTypeEnumType;
import org.openmuc.jeebus.spine.xsd.v1.EntityTypeEnumType;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
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

import com.google.common.collect.ImmutableList;

import io.openems.common.bridge.eebus.api.EebusDeviceDiscovery;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.eebus.api.BridgeEebus;
import io.openems.edge.bridge.eebus.api.EebusPeer;
import io.openems.edge.bridge.eebus.api.EebusUseCaseManager;
import io.openems.edge.bridge.eebus.peer.InternalEebusPeer;
import io.openems.edge.bridge.eebus.usecase.EebusUseCaseManagerImpl;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.meta.Meta;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.EEBUS", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
@GenerateTargetsFromReferences("Peer")
public class BridgeEebusImpl extends AbstractOpenemsComponent implements BridgeEebus, OpenemsComponent, EventHandler {
	@Reference
	protected EebusDeviceDiscovery discovery;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Meta meta;

	@Reference
	private PeriodicExecutorFactory periodicExecutorFactory;

	private final Logger log = LoggerFactory.getLogger(BridgeEebusImpl.class);
	private final EebusUseCaseManagerImpl useCaseManager;
	private final List<InternalEebusPeer> peers = new CopyOnWriteArrayList<>();

	private ConfigCertificateStorage certificateStorage;
	private ShipNodeConfiguration shipNodeConfig;
	private ShipCommunication shipCommunication;
	private Device eebusDevice;
	private int connectionsAmount = 0;

	private PeriodicExecutor reInitScheduler;
	private boolean reInitRequired = true;

	public BridgeEebusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeEebus.ChannelId.values() //
		);

		this.useCaseManager = new EebusUseCaseManagerImpl(this);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.certificateStorage = new ConfigCertificateStorage(this.cm, this.servicePid());
		this.shipNodeConfig = this.createNodeConfig(config);

		this.reInitScheduler = this.periodicExecutorFactory.execute("BridgeEebus-DeviceReInit", () -> {
			if (this.reInitRequired || this.useCaseManager.isDirty()) {
				this.reinitializeDevice();
			}
			return DelayTimeProviderChain.fixedDelay(Duration.ofMinutes(1)).getDelay();
		}, DelayTimeProvider.Delay.of(Duration.ofSeconds(5)));
	}

	@Modified
	protected void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());

		this.certificateStorage = new ConfigCertificateStorage(this.cm, this.servicePid());
		this.shipNodeConfig = this.createNodeConfig(config);
		this.reInitRequired = true;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.unloadDevice();

		if (this.reInitScheduler != null) {
			this.reInitScheduler.dispose();
			this.reInitScheduler = null;
		}
	}

	@Reference(//
			target = "(&(eebus.id=${config.id})(enabled=true))", //
			bind = "bindPeer", //
			unbind = "unbindPeer", //
			policyOption = ReferencePolicyOption.GREEDY, //
			policy = ReferencePolicy.DYNAMIC, //
			cardinality = ReferenceCardinality.MULTIPLE //
	)
	protected synchronized void bindPeer(InternalEebusPeer peer) {
		this.peers.add(peer);
		this.reInitRequired = true;
	}

	protected synchronized void unbindPeer(InternalEebusPeer peer) {
		this.peers.remove(peer);
		this.reInitRequired = true;
	}

	protected void reinitializeDevice() {
		this.reInitRequired = false;

		this.log.info("Initializing eebus device ...");
		this.unloadDevice();
		this.initDevice();
		this.log.info("Initialized eebus device.");
	}

	protected void unloadDevice() {
		if (this.eebusDevice != null) {
			this.eebusDevice.close();
			this.eebusDevice = null;
		}
	}

	protected void initDevice() {
		if (!this.isEnabled()) {
			return;
		}

		try {
			var shipCommunication = new ShipCommunication(this.shipNodeConfig) //
					.withConnectClientsTo(TRUSTED) //
					.withTrustedSkis(this.getTrustedSkis()); //

			this.eebusDevice = this.buildDevice(shipCommunication);
			this.shipCommunication = shipCommunication;
			setValue(this, BridgeEebus.ChannelId.OWN_SKI, shipCommunication.getOwnSki());
			setValue(this, BridgeEebus.ChannelId.INITIALIZE_FAILURE, false);

		} catch (Exception ex) {
			this.log.error("Failed to initialize eebus bridge " + this.id(), ex);
			setValue(this, BridgeEebus.ChannelId.INITIALIZE_FAILURE, true);
		}
	}

	protected Device buildDevice(ShipCommunication shipCommunication) {
		var useCases = this.useCaseManager.createUseCases();

		return Device.getBuilder() //
				.withDeviceType(DeviceTypeEnumType.GENERIC) //
				.withCommunication(shipCommunication) //
				.withId("SPINE-DEVICE-ID") //
				.withDiscoverDevices(true) //
				.addEntity() //
				.setType(EntityTypeEnumType.CEM) //
				.withUseCases(useCases) //
				.applyToDevice() //
				.build();
	}

	private ShipNodeConfiguration createNodeConfig(Config config) {
		return new ShipNodeConfiguration(//
				Set.of(config.bindHost()), //
				config.bindPort(), //
				"/ship/", //
				true, //
				config.serviceID(), "local.", //
				config.serviceInstance(), //
				this.certificateStorage, //
				"CN=" + config.serviceID() + ", O=OpenEMS", //
				40 * 365 //
		);
	}

	@Override
	public String[] getTrustedSkis() {
		return this.peers.stream() //
				.map(InternalEebusPeer::getSki) //
				.toArray(String[]::new);
	}

	@Override
	public String debugLog() {
		return String.format("Connections:%d|Peers:%d|%s", this.connectionsAmount, this.peers.size(),
				this.useCaseManager.debugLog());
	}

	@Override
	public ImmutableList<EebusPeer> getPeers() {
		return ImmutableList.copyOf(this.peers);
	}

	@Override
	public EebusUseCaseManager getUseCaseManager() {
		return this.useCaseManager;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
			-> this.updateConnectionInfos();
		}
	}

	private void updateConnectionInfos() {
		if (this.shipCommunication == null) {
			return;
		}

		Comparator<ShipConnectionInfoSnapshot> sorting = Comparator.comparing(ShipConnectionInfoSnapshot::getTrustLevel)
				.reversed()
				.thenComparing(Comparator.comparing(ShipConnectionInfoSnapshot::getConnectionDate).reversed());

		var allConnections = this.shipCommunication.getConnectionInfos();
		this.connectionsAmount = allConnections.size();

		var connectionInfos = allConnections.stream() //
				.filter(ShipConnectionInfoSnapshot::isDataExchangeEstablished) //
				.sorted(sorting) //
				.toList();

		for (var peer : this.peers) {
			connectionInfos.stream() //
					.filter(x -> x.getSki().equals(peer.getSki())) //
					.findFirst() //
					.ifPresent(peer::updateConnectionInfo);
		}
	}
}
