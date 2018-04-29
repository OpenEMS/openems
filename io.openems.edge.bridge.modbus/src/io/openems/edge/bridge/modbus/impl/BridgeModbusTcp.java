package io.openems.edge.bridge.modbus.impl;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.facade.MyModbusMaster;
import io.openems.edge.bridge.modbus.api.facade.MyModbusTCPMaster;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.controllerexecutor.EdgeEventConstants;

/**
 * Provides a service for connecting to, querying and writing to a Modbus/TCP
 * device
 */
@Designate(ocd = ConfigTcp.class, factory = true)
@Component(name = "Bridge.Modbus.Tcp", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class BridgeModbusTcp extends AbstractModbusBridge implements BridgeModbus, OpenemsComponent, EventHandler {

	// private final Logger log =
	// LoggerFactory.getLogger(BridgeModbusTcpImpl.class);

	/**
	 * The configured IP address
	 */
	private String ipAddress = "";

	@Activate
	void activate(ComponentContext context, ConfigTcp config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.ipAddress = config.ip();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected MyModbusMaster createModbusMaster() {
		return new MyModbusTCPMaster(this.ipAddress);
	}
}