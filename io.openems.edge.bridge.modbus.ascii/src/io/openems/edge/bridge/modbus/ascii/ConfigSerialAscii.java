package io.openems.edge.bridge.modbus.ascii;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;

@ObjectClassDefinition(//
		name = "Bridge Modbus/ASCII Serial", //
		description = "Provides a service for connecting to, querying and writing to a Modbus/ASCII device over serial port.")
@interface ConfigSerialAscii {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "modbusAscii0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Port-Name", description = "The name of the serial port - e.g. '/dev/ttyUSB0' or 'COM3'")
	String portName() default "/dev/ttyUSB0";

	@AttributeDefinition(name = "Baudrate", description = "The baudrate - e.g. 9600, 19200, 38400, 57600 or 115200")
	int baudRate() default 9600;

	@AttributeDefinition(name = "Databits", description = "The number of databits - typically 7 for ASCII, 8 for some implementations")
	int databits() default 8;

	@AttributeDefinition(name = "Stopbits", description = "The number of stopbits - '1', '1.5' or '2'")
	Stopbit stopbits() default Stopbit.ONE;

	@AttributeDefinition(name = "Parity", description = "The parity - 'none', 'even', 'odd', 'mark' or 'space'. Typically 'even' for Modbus/ASCII")
	Parity parity() default Parity.EVEN;

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity. Use READS_AND_WRITES_DURATION_TRACE_EVENTS for raw frame logging.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	@AttributeDefinition(name = "Invalidate elements after how many read Errors?", description = "Increase this value if modbus read errors happen frequently.")
	int invalidateElementsAfterReadErrors() default 1;

	@AttributeDefinition(name = "ABL compatible mode", description = "Enable compatibility with the ABL eMH1 EVCC wallbox, which sends '>' (0x3E) as the response frame-start character instead of the standard ':' (0x3A). When enabled, the bridge transparently replaces '>' with ':' in the received byte stream.")
	boolean ablCompatible() default false;

	String webconsole_configurationFactory_nameHint() default "Bridge Modbus/ASCII Serial [{id}]";
}
