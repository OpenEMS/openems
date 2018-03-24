package io.openems.edge.controller.debuglog;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.DebugLog", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class DebugLog extends AbstractOpenemsComponent implements Controller {

	private final Logger log = LoggerFactory.getLogger(DebugLog.class);

	@Activate
	void activate(Config config) {
		super.activate(config.id(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		log.info("[" + this.id() + "] runs");
	}
}
