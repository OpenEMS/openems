package io.openems.backend.application;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.openems.common.OpenemsConstants;

@Component(immediate = true)
public class BackendApp {

	private final Logger log = LoggerFactory.getLogger(BackendApp.class);

	@Activate
	private void activate() {
		final var message = "OpenEMS Backend version [" + OpenemsConstants.VERSION + "] started";
		final var line = Strings.repeat("=", message.length());
		this.log.info(line);
		this.log.info(message);
		this.log.info(line);

		this.log.debug("Activate BackendApp");
	}

	@Deactivate
	private void deactivate() {
		this.log.debug("Deactivate BackendApp");
	}

}
