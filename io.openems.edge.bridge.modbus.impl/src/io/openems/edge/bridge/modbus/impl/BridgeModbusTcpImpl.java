package io.openems.edge.bridge.modbus.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Modbus.Tcp", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class BridgeModbusTcpImpl extends AbstractOpenemsComponent implements BridgeModbusTcp, OpenemsComponent {

	@Activate
	void activate(Config config) {
		super.activate(config.id(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

}
