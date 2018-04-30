package io.openems.edge.bridge.modbus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Modbus/RTU Serial Bridge", //
		description = "Provides a service for connecting to, querying and writing to a Modbus/RTU device.")
@interface ConfigSerial {
	String service_pid();

	String id() default "modbus0";

	@AttributeDefinition(name = "Port-Name", description = "The name of the serial port - e.g. '/dev/ttyUSB0' or 'COM3'")
	String portName();

	@AttributeDefinition(name = "Baudrate", description = "The baudrate - e.g. 9600, 19200, 38400, 57600 or 115200")
	int baudRate() default 9600;

	@AttributeDefinition(name = "Databits", description = "The number of databits - e.g. 8")
	int databits() default 8;

	@AttributeDefinition(name = "Stopbits", description = "The number of stopbits - '1', '1.5' or '2'")
	String stopbits() default "1";

	@AttributeDefinition(name = "Parity", description = "The parity - 'none', 'even', 'odd', 'mark' or 'space'")
	String parity() default "none";

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Modbus/RTU Serial Bridge [{id}]";
}