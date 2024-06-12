package io.openems.edge.bridge.can.linux;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.bridge.can.CanHardwareType;
import io.openems.edge.bridge.can.LogVerbosity;

@ObjectClassDefinition(//
		name = "Bridge CAN Linux SocketCAN", //
		description = "Provides a service for reading and writing single CAN frames to/from a CAN device.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "can0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "CAN Baudrate", description = "CAN Baudrate in kBaud (not implemented yet).")
	BaudRate can_baudrate() default BaudRate.BAUD_500_KBPS;

	@AttributeDefinition(name = "CAN Driver", description = "Uses the integrated Simulator or the CAN Hardware (Kunbus RevPi CAN Connector).")
	CanHardwareType selected_hardware() default CanHardwareType.SOCKETCAN;

	@AttributeDefinition(name = "Invalidate elements after how many CAN read errors", description = "Typically each CAN Identfier has a defined cycle time. If a CAN frame is not received "
			+ "after (X * cycle time) ms, the appropriate channel elements are invalidated ")
	int invalidateElementsAfterReadErrors() default 10;

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity. Useful for developing new devices that use this module.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;
	
	@AttributeDefinition(name = "Interface Name", description = "Name of the interface to bind on, e. g. \"vcan0\" or \"can0\"")
	String interface_name() default "can0";

	String webconsole_configurationFactory_nameHint() default "Bridge CAN Linux SocketCAN[{id}]";

}
