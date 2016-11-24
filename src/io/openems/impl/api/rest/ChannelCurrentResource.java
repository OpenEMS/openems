package io.openems.impl.api.rest;

import java.util.Optional;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.exception.NotImplementedException;
import io.openems.core.Databus;
import io.openems.core.utilities.JsonUtils;

public class ChannelCurrentResource extends ServerResource {

	private static Logger log = LoggerFactory.getLogger(ChannelCurrentResource.class);

	private final Databus databus;

	public ChannelCurrentResource() {
		this.databus = Databus.getInstance();
	}

	@Get("json") public Representation getCurrentValue() {
		String thing = (String) this.getRequestAttributes().get("thing");
		String channel = (String) this.getRequestAttributes().get("channel");
		Optional<?> value = this.databus.getValue(thing, channel);
		if (value.isPresent()) {
			try {
				return new StringRepresentation(JsonUtils.getAsJsonElement(value.get()).toString(),
						MediaType.APPLICATION_JSON);
			} catch (NotImplementedException e) {
				log.warn(e.getMessage());
				throw new ResourceException(404);
			}
		} else {
			// Channel not found
			throw new ResourceException(404);
		}
	}

}
