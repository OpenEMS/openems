package market.square.linear;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "MarketSquare Linear", //
		description = "Market implementation working with constant 15 minute blocks.")
@interface Config {
	String service_pid();

	String id() default "market0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Speed-Factor", description = "Each time-constant like 'day' is multiplied by this factor.")
	double speedFactor() default 1.0;

	String webconsole_configurationFactory_nameHint() default "MarketSquare Linear [{id}]";
}