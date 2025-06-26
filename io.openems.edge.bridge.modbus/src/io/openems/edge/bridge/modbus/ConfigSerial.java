package io.openems.edge.bridge.modbus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;

@ObjectClassDefinition(//
		name = "Bridge Modbus/RTU Serial", //
		description = "Provides a service for connecting to, querying and writing to a Modbus/RTU device.")
@interface ConfigSerial {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "modbus0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Port-Name", description = "The name of the serial port - e.g. '/dev/ttyUSB0' or 'COM3'")
	String portName() default "/dev/ttyUSB0";

	@AttributeDefinition(name = "Baudrate", description = "The baudrate - e.g. 9600, 19200, 38400, 57600 or 115200")
	int baudRate() default 9600;

	@AttributeDefinition(name = "Databits", description = "The number of databits - e.g. 8")
	int databits() default 8;

	@AttributeDefinition(name = "Stopbits", description = "The number of stopbits - '1', '1.5' or '2'")
	Stopbit stopbits() default Stopbit.ONE;

	@AttributeDefinition(name = "Parity", description = "The parity - 'none', 'even', 'odd', 'mark' or 'space'")
	Parity parity() default Parity.NONE;

	@AttributeDefinition(name = "Enable termination", description = "Sets whether the interface shall enable internal bus termination")
	boolean enableTermination() default true;

	@AttributeDefinition(name = "Delay before TX [μs]", description = "Sets the delay between activating the transmitter and actually sending data. There are devices in the field requiring such a delay for start bit detection.", min = "0")
	int delayBeforeTx() default 1000;

	@AttributeDefinition(name = "Delay after TX [μs]", description = "Sets the delay between the end of transmitting data and deactivating the transmitter.", min = "0")
	int delayAfterTx() default 0;

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	@AttributeDefinition(name = "Invalidate elements after how many read Errors?", description = "Increase this value if modbus read errors happen frequently.")
	int invalidateElementsAfterReadErrors() default 1;

	String webconsole_configurationFactory_nameHint() default "Bridge Modbus/RTU Serial [{id}]";
}