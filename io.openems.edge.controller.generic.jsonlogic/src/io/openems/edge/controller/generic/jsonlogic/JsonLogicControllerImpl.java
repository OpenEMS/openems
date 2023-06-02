package io.openems.edge.controller.generic.jsonlogic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonElement;

import io.github.meiskalt7.jsonlogic.JsonLogic;
import io.github.meiskalt7.jsonlogic.JsonLogicException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Generic.JsonLogic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class JsonLogicControllerImpl extends AbstractOpenemsComponent
		implements JsonLogicController, Controller, OpenemsComponent {

	private final JsonLogic jsonLogic = new JsonLogic();
	private final List<ChannelAddress> channelAddresses = new ArrayList<>();

	@Reference
	private ComponentManager componentManager;

	private Config config = null;

	public JsonLogicControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				JsonLogicController.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		this.recursivelyParseVars(JsonUtils.parse(config.rule()));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Parse the JsonLogic rule and try to find "var" entries.
	 *
	 * @param json the JsonLogic rule
	 * @throws OpenemsNamedException on error
	 */
	private void recursivelyParseVars(JsonElement json) throws OpenemsNamedException {
		if (json.isJsonObject()) {
			// Found a JsonObject
			for (Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
				// Is there any key "var"
				if (entry.getKey().equals("var") && entry.getValue().isJsonPrimitive()) {
					var var = entry.getValue().getAsJsonPrimitive();
					if (var.isString()) {
						// Parse as ChannelAddress and add to list
						this.channelAddresses.add(ChannelAddress.fromString(var.getAsString()));
					}
				}
				// Recursive call
				this.recursivelyParseVars(entry.getValue());
			}
		} else if (json.isJsonArray()) {
			// Found a JsonArray
			for (JsonElement entry : json.getAsJsonArray()) {
				// Recursive call
				this.recursivelyParseVars(entry);
			}
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Read JsonLogic data from Channels
		Map<String, Object> data = new HashMap<>();
		for (ChannelAddress channelAddress : this.channelAddresses) {
			Object value = this.componentManager.getChannel(channelAddress).value().get();
			data.put(channelAddress.toString(), value);
		}

		// Apply JsonLogic rule
		List<?> result;
		try {
			result = (List<?>) this.jsonLogic.apply(this.config.rule(), data);
		} catch (JsonLogicException e) {
			throw new OpenemsException("JsonLogicException: " + e.getMessage());
		} catch (ClassCastException e) {
			throw new OpenemsException("Result is not a JsonArray: " + e.getMessage());
		}

		// Get Set-Channel requests
		for (Object entry : result) {
			List<?> request = (List<?>) entry;
			var channelAddress = ChannelAddress.fromString((String) request.get(0));
			WriteChannel<?> channel = this.componentManager.getChannel(channelAddress);
			Object value = request.get(1);
			channel.setNextWriteValueFromObject(value);
		}
	}
}

// TODO: once gson version 2.8.6 or higher is compatible with OSGi on Java 8: use json-logic library
// from maven instead of local file. Json-logic library on maven requires Gson 2.8.6; but Gson 2.8.6
// is not compatible with OSGi on Java 8 as it has the wrong manifest headers.
// See -> https://github.com/google/gson/issues/1601
// This is why we are using a manually compiled jar here, based on the fork at
// https://github.com/sfeilmeier/json-logic-java
//
// To revert back to official version, add to pom.xml:
//<dependency>
//	<groupId>io.github.meiskalt7</groupId>
//	<artifactId>json-logic-java</artifactId>
//	<version>1.0.0</version>
//</dependency>
