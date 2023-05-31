package io.openems.edge.io.revpi.bsp.digitalio;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
	name = "IO RevolutionPi BSP DigitalIO Board", //
	description = "Implements the access to the Kunbus RevolutionPi DigitalIO, DigitalInput and DigitalOutput enhancement hardware")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "io0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "RevPi Type", description = "Type of enhancement board (DIO, DI, DO)")
    ExpansionModule revpiType() default ExpansionModule.REVPI_DIO;

    @AttributeDefinition(name = "IN", description = "Name of the Inputs 1 to X (RevPI DIO (14) and Revpi DI (16) module)")
    String[] in() default { //
	    "I_1", "I_2", "I_3", "I_4", //
	    "I_5", "I_6", "I_7", "I_8", //
	    "I_9", "I_10", "I_11", "I_12", //
	    "I_13", "I_14" //
    };

    @AttributeDefinition(name = "OUT", description = "Name of the Outputs 1 to X (RevPI DIO (14) and Revpi DO (16) module)")
    String[] out() default { //
	    "O_1", "O_2", "O_3", "O_4", //
	    "O_5", "O_6", "O_7", "O_8", //
	    "O_9", "O_10", "O_11", "O_12", //
	    "O_13", "O_14" //
    };

    @AttributeDefinition(name = "Read Output", description = "Cyclically read hardware state and put it in output channels (must be false on REVPI DI)")
    boolean updateOutputFromHardware() default true;

    @AttributeDefinition(name = "Is Simulation", description = "Runs component in simulation mode (withou any real hardware)")
    boolean isSimulationMode() default false;

    @AttributeDefinition(name = "Simulation DataIn", description = "Provide 16 InputData (0, 1)")
    String simulationDataIn() default "1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0";

    String webconsole_configurationFactory_nameHint() default "IO RevolutionPi BSP DigitalIO Board [{id}]";

}