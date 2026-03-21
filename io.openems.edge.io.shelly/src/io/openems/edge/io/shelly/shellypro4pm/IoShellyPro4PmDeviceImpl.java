package io.openems.edge.io.shelly.shellypro4pm;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.types.Result;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.mdns.MDnsDiscovery;
import io.openems.edge.common.type.Phase;
import io.openems.edge.io.shelly.common.HttpBridgeShellyService;
import io.openems.edge.io.shelly.common.Utils;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBase;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBaseImpl;

@Designate(ocd = DeviceConfig.class, factory = true)
@Component(//
		name = "IO.Shelly.Pro4PM", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
public class IoShellyPro4PmDeviceImpl extends IoGen2ShellyBaseImpl
		implements IoShellyPro4PmDevice, IoGen2ShellyBase, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(IoShellyPro4PmDeviceImpl.class);

	private Phase.SinglePhase phase;
	private final CopyOnWriteArrayList<Consumer<Result<JsonObject>>> statusCallbacks = new CopyOnWriteArrayList<>();

	@Reference
	private MDnsDiscovery mDnsDiscovery;
	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;
	@Reference
	private HttpBridgeShellyService.HttpBridgeShellyServiceDefinition httpBridgeShellyServiceDefinition;

	public IoShellyPro4PmDeviceImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				IoGen2ShellyBase.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, DeviceConfig config) {
		this.phase = config.phase();

		super.activate(context, config.id(), config.alias(), config.enabled(), config.ip(), config.mdnsName(),
				config.debugMode(), true);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void subscribeDataCalls() {
		this.cycleService.subscribeJsonEveryCycle(this.baseUrl + "/rpc/Shelly.GetStatus", this::processHttpResult);
	}

	private void processHttpResult(HttpResponse<JsonElement> result, Throwable error) {
		setValue(this, IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, error != null);

		if (error != null) {
			this.logWarn(this.log, "Failed to fetch status from shelly: " + error.getMessage());

			var exception = error instanceof Exception ? (Exception) error
					: new Exception("Failed to fetch status from shelly", error);
			this.statusCallbacks.forEach(x -> x.accept(Result.error(exception)));
			return;
		}

		try {
			var response = JsonUtils.getAsJsonObject(result.data());

			this.channel(IoGen2ShellyBase.ChannelId.HAS_UPDATE)
					.setNextValue(Utils.readUpdatesAvailableStatusFromStatusResponse(response));

			this.statusCallbacks.forEach(x -> x.accept(Result.ok(response)));
			setValue(this, IoGen2ShellyBase.ChannelId.SLAVE_COMMUNICATION_FAILED, false);

		} catch (Exception e) {
			this.logWarn(this.log, "Error while parsing response: " + e.getMessage());
			this.statusCallbacks.forEach(x -> x.accept(Result.error(e)));
		}
	}

	@Override
	public void addStatusCallback(Consumer<Result<JsonObject>> callback) {
		this.statusCallbacks.add(callback);
	}

	@Override
	public void removeStatusCallback(Consumer<Result<JsonObject>> callback) {
		this.statusCallbacks.remove(callback);
	}

	@Override
	public HttpBridgeShellyService getShellyService() {
		return this.shellyService;
	}

	@Override
	public Phase.SinglePhase getPhase() {
		return this.phase;
	}

	@Override
	public String[] getSupportedShellyDeviceTypes() {
		return new String[] { "Pro4PM" };
	}

	@Override
	protected BridgeHttpFactory getHttpBridgeFactory() {
		return this.httpBridgeFactory;
	}

	@Override
	protected HttpBridgeCycleServiceDefinition getHttpBridgeCycleServiceDefinition() {
		return this.httpBridgeCycleServiceDefinition;
	}

	@Override
	protected HttpBridgeShellyService.HttpBridgeShellyServiceDefinition getHttpBridgeShellyServiceDefinition() {
		return this.httpBridgeShellyServiceDefinition;
	}

	@Override
	protected MDnsDiscovery getMDnsDiscovery() {
		return this.mDnsDiscovery;
	}
}
