package io.openems.edge.core.user;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configures the User Service.
 *
 * <p>
 * By default the following passwords are set:
 * <ul>
 * <li>User admin: admin
 * <li>User installer: installer
 * <li>User owner: owner
 * <li>User guest: user
 * </ul>
 */
@ObjectClassDefinition(//
		name = "Core User", //
		description = "This component handles User authentication.")
@interface Config {
	@AttributeDefinition(name = "Admin: password", description = "hashed password for User 'admin'", type = AttributeType.PASSWORD)
	String adminPassword() default "txASlUVQkEI9Bxa/IZOJe8l3+R4lMzFTShz27vK44go=";

	@AttributeDefinition(name = "Admin: salt", description = "salt for User 'admin'", type = AttributeType.PASSWORD)
	String adminSalt() default "YWRtaW4=";

	@AttributeDefinition(name = "Installer: password", description = "hashed password for User 'installer'", type = AttributeType.PASSWORD)
	String installerPassword() default "2O1dMlsFdwafy58ehrT+X+0CEWaAmBRad5JFbTLx/Wo=";

	@AttributeDefinition(name = "Installer: salt", description = "salt for User 'installer'", type = AttributeType.PASSWORD)
	String installerSalt() default "aW5zdGFsbGVy";

	@AttributeDefinition(name = "Owner: password", description = "hashed password for User 'owner'", type = AttributeType.PASSWORD)
	String ownerPassword() default "eJgLBfHTmehv4S1whsfjeE3q3AJmJCBabV59Y65eoYI=";

	@AttributeDefinition(name = "Owner: salt", description = "salt for User 'owner'", type = AttributeType.PASSWORD)
	String ownerSalt() default "b3duZXI=";

	@AttributeDefinition(name = "Guest: password", description = "hashed password for User 'guest'", type = AttributeType.PASSWORD)
	String guestPassword() default "IcIzJSOvNM1PvQ8v5ypFvPoTZyHw3Knob+zi7d+WspU=";

	@AttributeDefinition(name = "Guest: salt", description = "salt for User 'guest'", type = AttributeType.PASSWORD)
	String guestSalt() default "dXNlcg==";

	String webconsole_configurationFactory_nameHint() default "Core User";
}