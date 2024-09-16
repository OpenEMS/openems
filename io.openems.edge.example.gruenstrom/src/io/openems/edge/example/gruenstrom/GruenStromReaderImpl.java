package io.openems.edge.example.gruenstrom;

import java.time.Instant;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GruenStrom.Reader", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class GruenStromReaderImpl extends AbstractOpenemsComponent implements OpenemsComponent, GruenStromReader {

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public GruenStromReaderImpl() {
		super(OpenemsComponent.ChannelId.values(), GruenStromReader.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.httpBridge = this.httpBridgeFactory.get();
		// Hard code channel value so we can read it in UI
		this.getGreenLevelChannel().setNextValue(54);

		// TODO: Implement api

		this.httpBridge.request(new Endpoint("https://api.corrently.io/v2.0/gsi/prediction?zip=" + config.plz(), // Url
				HttpMethod.GET, // HttpMethod
				30, //
				120, //
				"", //
				Map.of() //
		)).thenAccept(t -> {
			final var responseBody = t.data();
			try {
				// Using OpenEMS own JsonUtils class
				JsonElement je = JsonUtils.parse(responseBody);
				JsonObject jo = JsonUtils.getAsJsonObject(je);
				JsonArray ja = JsonUtils.getAsJsonArray(jo.get("data"));
				final var currentTimestamp = Instant.now().toEpochMilli();
				// Iterate over the array to find the correct entry
				for (var dataValue : ja) {
					final var dataPoint = JsonUtils.getAsJsonObject(dataValue);
					final var time = dataPoint.get("time").getAsLong();
					if (time == currentTimestamp) {
						final var co2 = JsonUtils.getAsInt(dataPoint.get("co2"));
						this.getGreenLevelChannel().setNextValue(54);
					}
				}

			} catch (OpenemsNamedException e) {
				// log message or error message
			}

		});

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}

}
