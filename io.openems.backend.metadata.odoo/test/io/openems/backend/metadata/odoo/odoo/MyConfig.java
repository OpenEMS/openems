package io.openems.backend.metadata.odoo.odoo;

import java.lang.annotation.Annotation;

import io.openems.backend.metadata.odoo.Config;
import io.openems.common.types.DebugMode;

@SuppressWarnings("all")
public record MyConfig(//
		Protocol odooProtocol, //
		String odooHost, //
		int odooPort, //
		int odooUid, //
		String odooLogin, //
		String odooPassword, //
		String pgHost, //
		int pgPort, //
		String pgUser, //
		String pgPassword, //
		String database, //
		int eventPoolSize, //
		int requestPoolSize, //
		int pgConnectionPoolSize, //
		DebugMode debugMode, //
		boolean enablePasswordAuthentication, //
		String authOAuthProviderName //
) implements Config {

	@Override
	public String webconsole_configurationFactory_nameHint() {
		return "";
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Config.class;
	}

}
