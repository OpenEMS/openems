package io.openems.edge.core.appmanager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core App-Manager", //
		description = "The global manager for OpenEMS Apps. (Changes are only applied to the components if the App gets updated via UI or JSONRPC-Request)")
@interface Config {

	@AttributeDefinition(name = "OpenEMS Key", description = "OpenEMS Key for installing the free apps.") //
	String keyForFreeApps() default "0000-0000-0000-0000";

	@AttributeDefinition(name = "OpenEMS Apps", description = "OpenEMS App properties as a JSON Array") //
	String apps() default "[]";

	String webconsole_configurationFactory_nameHint() default "Core App-Manager";

}