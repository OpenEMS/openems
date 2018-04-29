package io.openems.edge.bridge.modbus.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Modbus/RTU Serial Bridge", //
		description = "Provides a service for connecting to, querying and writing to a Modbus/RTU device.")
@interface ConfigSerial {
	String service_pid();

	String id();

	@AttributeDefinition(name = "Port-Name", description = "The name of the serial port - e.g. '/dev/ttyUSB0' or 'COM3'")
	String portName();

	@AttributeDefinition(name = "Baudrate", description = "The baudrate - e.g. 9600, 19200, 38400, 57600 or 115200")
	int baudRate();

	@AttributeDefinition(name = "Databits", description = "The number of databits - e.g. 8")
	int databits() default 8;

	@AttributeDefinition(name = "Stopbits", description = "The number of stopbits - e.g. '1', '1.5' or '2'")
	String stopbits() default "1";

	@AttributeDefinition(name = "Parity", description = "The parity - e.g. 'none', 'even', 'odd', 'mark', 'space'")
	String parity() default "none";

	boolean enabled();

	String webconsole_configurationFactory_nameHint() default "Modbus/RTU Serial Bridge [{id}]";
}