package io.openems.edge.application;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import info.faljse.SDNotify.SDNotify;
import io.openems.common.OpenemsConstants;

@Component(immediate = true)
public class EdgeApp {

	private final Logger log = LoggerFactory.getLogger(EdgeApp.class);

	@Activate
	private void activate() {
		var message = "OpenEMS version [" + OpenemsConstants.VERSION + "] started";
		var line = Strings.repeat("=", message.length());
		this.log.info(line);
		this.log.info(message);
		this.log.info(line);

		// Announce Operating System that OpenEMS Edge started
		var socketName = System.getenv().get("NOTIFY_SOCKET");
		if (socketName != null && socketName.length() != 0) {
			if (SDNotify.isAvailable()) {
				SDNotify.sendNotify();
			}
		}
	}

	@Deactivate
	private void deactivate() {
		this.log.debug("Deactivate EdgeApp");
	}

}
