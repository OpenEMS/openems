package io.openems.edge.core.meta;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.CurrencyConfig;

@ObjectClassDefinition(//
		name = "Core Meta", //
		description = "The global manager for Metadata.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core Meta";

	@AttributeDefinition(name = "Currency", description = "Every monetary value is inherently expressed in this Currency. Values obtained in a different currency (e.g. energy prices from a web service) are internally converted to this Currency using the current exchange rate.")
	CurrencyConfig currency() default CurrencyConfig.EUR;

	@AttributeDefinition(name = "Is Ess Charge From Grid Allowed", description = "Charging the battery from grid is allowed.")
	boolean isEssChargeFromGridAllowed() default false;

	@AttributeDefinition(name = "Grid Connection Point Fuse Limit", description = "Maximum current allowed at the Grid Connection Point (GCP), i.e. the rating of the fuses [A]")
	int gridConnectionPointFuseLimit() default 32;

	@AttributeDefinition(name = "Latitude", description = "Specifies the latitude coordinate of the system in degrees, ranging from -90.0 to 90.0")
	double latitude() default -999.0;

	@AttributeDefinition(name = "Longitude", description = "Specifies the longitude coordinate of the system in degrees, ranging from -180.0 to 180.0")
	double longitude() default -999.0;
}
