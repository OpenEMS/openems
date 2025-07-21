package io.openems.edge.core.meta;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.CurrencyConfig;
import io.openems.edge.common.meta.types.SubdivisionCode;

@ObjectClassDefinition(//
		name = "Core Meta", //
		description = "The global manager for Metadata.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core Meta";

	@AttributeDefinition(name = "Currency", description = "Every monetary value is inherently expressed in this Currency. Values obtained in a different currency (e.g. energy prices from a web service) are internally converted to this Currency using the current exchange rate.")
	CurrencyConfig currency() default CurrencyConfig.EUR;

	@AttributeDefinition(name = "Is Ess Charge From Grid Allowed", description = "Charging the battery from grid is allowed.")
	boolean isEssChargeFromGridAllowed() default false;

	@AttributeDefinition(name = "Grid Connection Point Fuse Limit", description = "Maximum current allowed at the Grid Connection Point (GCP), i.e. the rating of the fuses [A].")
	int gridConnectionPointFuseLimit() default 32;

	@AttributeDefinition(name = "Subdivision code", description = "The ISO 3166-2 subdivision code (e.g. 'DE-BY' for Bavaria, Germany).")
	SubdivisionCode subdivisionCode() default SubdivisionCode.UNDEFINED;

	@AttributeDefinition(name = "Place name", description = "The best-matching local place name.")
	String placeName() default "";

	@AttributeDefinition(name = "Postcode", description = "The postal code of the location.")
	String postcode() default "";

	@AttributeDefinition(name = "Latitude", description = "The latitude coordinate of the system location in degrees, ranging from -90.0 to 90.0")
	double latitude() default -999.0;

	@AttributeDefinition(name = "Longitude", description = "The longitude coordinate of the system location in degrees, ranging from -180.0 to 180.0")
	double longitude() default -999.0;

	@AttributeDefinition(name = "Timezone", description = "The local time zone, e.g. 'Europe/Berlin'")
	String timezone() default "";
}
