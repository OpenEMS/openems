package market.agent.grid;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "MarketAgent Grid", //
		description = "Represents production at the market.")
@interface Config {
	String service_pid();

	String id() default "agent0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Speed-Factor", description = "Each time-constant like 'day' is multiplied by this factor.")
	double speedFactor() default 1.0;

	@AttributeDefinition(name = "SellPrice", description = "The price you get payed, when selling to grid [1€].")
	double sellPrice();

	@AttributeDefinition(name = "BuyPrice", description = "The price you have to pay, when buying from grid [1€].")
	double buyPrice();

	@AttributeDefinition(name = "MaxPower", description = "The maximum power the mains connection can handle [1W].")
	int maxPower();

	String webconsole_configurationFactory_nameHint() default "MarketAgent Grid [{id}]";
}