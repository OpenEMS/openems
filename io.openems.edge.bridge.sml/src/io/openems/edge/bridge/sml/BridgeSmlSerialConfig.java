package io.openems.edge.bridge.sml;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.FlowControl;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.StopBits;

@ObjectClassDefinition(//
		name = "Bridge SML Serial", //
		description = "Provides a service for connecting to, querying and writing to a SML (Smart Messaging Language) serial device.")
@interface BridgeSmlSerialConfig {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "sml0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Port-Name", description = "The name of the serial port - e.g. '/dev/ttyUSB0' or 'COM3'")
	String portName() default "/dev/ttyUSB0";

	@AttributeDefinition(name = "Baudrate", description = "The baudrate - e.g. 9600, 19200, 38400, 57600 or 115200")
	int baudRate() default 9600;

	@AttributeDefinition(name = "Databits", description = "The number of databits - e.g. 8")
	DataBits databits() default DataBits.DATABITS_8;

	@AttributeDefinition(name = "Stopbits", description = "The number of stopbits - '1', '1.5' or '2'")
	StopBits stopbits() default StopBits.STOPBITS_1;

	@AttributeDefinition(name = "Parity", description = "The parity - 'none', 'even', 'odd', 'mark' or 'space'")
	Parity parity() default Parity.EVEN;
	
	@AttributeDefinition(name = "Timeout", description = "Timout in seconds for Serial Port")
	int timeout() default 2;
	
	@AttributeDefinition(name = "Flow Control", description = "Flow Control for Serial Port")
	FlowControl flowControl() default FlowControl.NONE;

	@AttributeDefinition(name = "Invalidate elements after how many read Errors?", description = "Increase this value if modbus read errors happen frequently.")
	int invalidateElementsAfterReadErrors() default 1;

	String webconsole_configurationFactory_nameHint() default "Bridge SML Serial [{id}]";
}