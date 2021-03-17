package io.openems.edge.bridge.mbus;

import org.openmuc.jmbus.wireless.WMBusConnection;
import org.openmuc.jmbus.wireless.WMBusMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Bridge Wireless M-Bus", //
		description = "Provides a service for reading a WM-Bus device.")
@interface ConfigWMBus {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "wmbus0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Serial-Device", description = "Serial Device Name")
	String portName() default "/dev/ttyUSB0";

	@AttributeDefinition(name = "Manufacturer", description = "Manufacturer of the Serial Device")
	WMBusConnection.WMBusManufacturer manufacturer() default WMBusConnection.WMBusManufacturer.AMBER;

	@AttributeDefinition(name = "Mode", description = "WM-Bus mode")
	WMBusMode mode() default WMBusMode.T;

	@AttributeDefinition(name = "Scan for devices", description = "Print info of any received signals to the log.")
	boolean scan() default false;

	@AttributeDefinition(name = "Debug mode", description = "Print additional info to the log.")
	boolean debug() default false;

	String webconsole_configurationFactory_nameHint() default "Bridge Wireless M-Bus [{id}]";
}