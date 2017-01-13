package io.openems.impl.controller.api.rest;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;

@ThingInfo("REST-API (z. B. für externe Datenabfrage)")
public class RestApiController extends Controller {

	public RestApiController() {
		super();
	}

	public RestApiController(String thingId) {
		super(thingId);
	}

	@ConfigInfo(title = "Sets the port", type = Integer.class)
	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this).defaultValue(8084);

	@Override
	public void run() {
		// Start REST-Api server
		try {
			ComponentSingleton.getComponent(port);
		} catch (OpenemsException e) {
			log.error(e.getMessage() + ": " + e.getCause());
		}
	}
}
