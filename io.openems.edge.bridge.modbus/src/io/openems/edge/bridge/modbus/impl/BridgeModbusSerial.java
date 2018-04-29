package io.openems.edge.bridge.modbus.impl;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import com.ghgande.j2mod.modbus.util.SerialParameters;

import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.facade.MyModbusMaster;
import io.openems.edge.bridge.modbus.api.facade.MyModbusSerialMaster;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.controllerexecutor.EdgeEventConstants;

/**
 * Provides a service for connecting to, querying and writing to a Modbus/RTU
 * device
 */
@Designate(ocd = ConfigSerial.class, factory = true)
@Component(name = "Bridge.Modbus.Serial", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class BridgeModbusSerial extends AbstractModbusBridge implements BridgeModbus, OpenemsComponent, EventHandler {

	// private final Logger log =
	// LoggerFactory.getLogger(BridgeModbusTcpImpl.class);

	/**
	 * The configured Port-Name (e.g. '/dev/ttyUSB0' or 'COM3')
	 */
	private String portName = "";

	/**
	 * The configured Baudrate (e.g. 9600)
	 */
	private int baudrate;

	/**
	 * The configured Databits (e.g. 8)
	 */
	private int databits;

	/**
	 * The configured Stopbits
	 */
	private String stopbits;

	/**
	 * The configured parity
	 */
	private String parity;

	@Activate
	void activate(ComponentContext context, ConfigSerial config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.portName = config.portName();
		this.baudrate = config.baudRate();
		this.databits = config.databits();
		this.stopbits = config.stopbits();
		this.parity = config.parity();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected MyModbusMaster createModbusMaster() {
		SerialParameters params = new SerialParameters();
		params.setPortName(this.portName);
		params.setBaudRate(this.baudrate);
		params.setDatabits(this.databits);
		params.setStopbits(this.stopbits);
		params.setParity(this.parity);
		return new MyModbusSerialMaster(params);
	}
}