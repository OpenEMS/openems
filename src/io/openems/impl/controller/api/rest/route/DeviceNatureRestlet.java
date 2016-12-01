package io.openems.impl.controller.api.rest.route;

import java.util.Set;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.thing.Thing;
import io.openems.core.ThingRepository;
import io.openems.impl.controller.api.rest.OpenemsRestlet;

public class DeviceNatureRestlet extends OpenemsRestlet {

	private final ThingRepository thingRepository;

	public DeviceNatureRestlet() {
		super();
		thingRepository = ThingRepository.getInstance();
	}

	@Override public void handle(Request request, Response response) {
		super.handle(request, response);

		// call handler methods
		if (request.getMethod().equals(Method.GET)) {
			Representation entity = get();
			response.setEntity(entity);
		}
	}

	/**
	 * handle HTTP GET request
	 *
	 * @param thingId
	 * @param channelId
	 * @return
	 */
	private Representation get() {
		JsonObject j = new JsonObject();
		for (Class<? extends Thing> clazz : thingRepository.getThingClasses()) {
			if (DeviceNature.class.isAssignableFrom(clazz)) {
				// clazz is a DeviceNature
				Set<Thing> things = thingRepository.getThingsByClass(clazz);
				JsonArray jThings = new JsonArray();
				for (Thing thing : things) {
					jThings.add(thing.id());
				}
				j.add(clazz.getCanonicalName(), jThings);
			}
		}
		return new StringRepresentation(j.toString(), MediaType.APPLICATION_JSON);
	}
}
