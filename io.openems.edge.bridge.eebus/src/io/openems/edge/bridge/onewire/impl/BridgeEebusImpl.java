package io.openems.edge.bridge.onewire.impl;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import org.openmuc.jeebus.ship.api.Ship;
import org.openmuc.jeebus.ship.api.ShipNodeConfiguration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.eebus.BridgeEebus;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bridge.EEBUS", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
public class BridgeEebusImpl extends AbstractOpenemsComponent implements BridgeEebus, OpenemsComponent {

	private final EebusConnectionHandler connectionHandler = new EebusConnectionHandler();

	public BridgeEebusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeEebus.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		var conf = new ShipNodeConfiguration(//
				/* ipAddress */ "127.0.0.1", //
				/* port */ 1234, //
				/* wssPath */ "wss://127.0.0.1", //
				/* keepAlive */ false, //
				/* serviceId */ "OpenEMS-serviceId", //
				/* serviceDomain */ "OpenEMS-serviceDomain", //
				/* serviceInstance */ "OpenEMS-serviceInstance", //
				/* alias */ "OpenEMS-alias", //
				/* keyStorePassphrase */ new char[0], //
				/* keyPairPassphrase */ new char[0], //
				/* distinguishedName */ "OpenEMS-distinguishedName", //
				/* certificateValidityInDays */ 1 //
		);
		var ship = new Ship(conf, this.connectionHandler);
		System.out.println("activate() " + ship);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "This is the EEBUS Bridge";
	}
}