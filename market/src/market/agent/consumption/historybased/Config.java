package market.agent.consumption.historybased;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "MarketAgent History-Based Consumption", //
		description = "Represents consumption at the market.")
@interface Config {
	String service_pid();

	String id() default "agent1";

	@AttributeDefinition(name = "Meter-ID", description = "ID of represented meter.")
	String meter_id();

	boolean enabled() default true;

	@AttributeDefinition(name = "Speed-Factor", description = "Each time-constant like 'day' is multiplied by this factor.")
	double speedFactor() default 1.0;

	String webconsole_configurationFactory_nameHint() default "MarketAgent History-Based Consumption [{id}]";
}