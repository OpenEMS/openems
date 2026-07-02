package io.openems.edge.bridge.eebus.peer;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import io.openems.edge.bridge.eebus.api.EebusPeer;
import org.openmuc.jeebus.ship.api.ShipConnectionInfoSnapshot;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import io.openems.edge.bridge.eebus.api.BridgeEebus;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = PeerConfig.class, factory = true)
@Component(name = "Bridge.EEBUS.Device", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
public class EebusPeerImpl extends AbstractOpenemsComponent implements EebusPeer, OpenemsComponent, InternalEebusPeer {
	private final Logger log = LoggerFactory.getLogger(EebusPeerImpl.class);

	private String ski;

	public EebusPeerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeEebus.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, PeerConfig config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.ski = config.ski();

		if (!this.isValid()) {
			this.logError(this.log, "Configuration for eebus peer is invalid. Check if the SKI is correct.");
		}
	}

	@Override
	public String getSki() {
		return this.ski;
	}

	@Override
	public boolean isValid() {
		//Original function, but not usable because of missing OSGI export: return KeyManagement.isValidSki(this.ski);
		return this.ski.matches("[0-9a-fA-F]+") && this.ski.replaceAll("\\s+", "").length() == 40;
	}

	public void updateConnectionInfo(ShipConnectionInfoSnapshot connectionInfo) {
		if (connectionInfo == null) {
			setValue(this, EebusPeer.ChannelId.CONNECTION_FAILURE, true);
			return;
		}

		setValue(this, EebusPeer.ChannelId.CONNECTION_FAILURE, false);
		setValue(this, EebusPeer.ChannelId.CONNECTION_TYPE, this.mapConnectionType(connectionInfo.getConnectionType()));
		setValue(this, EebusPeer.ChannelId.TRUST_LEVEL, connectionInfo.getTrustLevel());
		setValue(this, EebusPeer.ChannelId.MISSING_TRUST, connectionInfo.getTrustLevel() < 16);
		setValue(this, EebusPeer.ChannelId.REMOTE_IP, connectionInfo.getRemoteIP());
	}

	private EebusPeerConnectionType mapConnectionType(ShipConnectionInfoSnapshot.ConnectionTypeEnum type) {
		return switch (type) {
			case CLIENT_CONNECTION_TO_PEER -> EebusPeerConnectionType.CLIENT_CONNECTION_TO_PEER;
			case PEER_CONNECTED_TO_SERVER -> EebusPeerConnectionType.PEER_CONNECTED_TO_SERVER;
		};
	}
}
