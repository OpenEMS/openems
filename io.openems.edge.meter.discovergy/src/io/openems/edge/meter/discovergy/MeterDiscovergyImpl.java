package io.openems.edge.meter.discovergy;

import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.user.User;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.discovergy.jsonrpc.Field;
import io.openems.edge.meter.discovergy.jsonrpc.GetFieldNamesRequest;
import io.openems.edge.meter.discovergy.jsonrpc.GetFieldNamesResponse;
import io.openems.edge.meter.discovergy.jsonrpc.GetMetersRequest;
import io.openems.edge.meter.discovergy.jsonrpc.GetMetersResponse;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Discovergy", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class MeterDiscovergyImpl extends AbstractOpenemsComponent
		implements MeterDiscovergy, ElectricityMeter, OpenemsComponent, EventHandler, ComponentJsonApi {

	private MeterType meterType = MeterType.PRODUCTION;
	private DiscovergyApiClient apiClient = null;
	private DiscovergyWorker worker = null;

	public MeterDiscovergyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterDiscovergy.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.meterType = config.type();

		super.activate(context, config.id(), config.alias(), config.enabled());

		if (config.enabled()) {
			this.apiClient = new DiscovergyApiClient(config.email(), config.password());

			this.worker = new DiscovergyWorker(this, this.apiClient, config);
			this.worker.activate(config.id());
			this.worker.triggerNextRun();
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();

		if (this.worker != null) {
			this.worker.deactivate();
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.worker.triggerNextRun();
			break;
		}
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(GetMetersRequest.METHOD, call -> {
			return this.handleGetMetersRequest(//
					call.get(EdgeKeys.USER_KEY), //
					GetMetersRequest.from(call.getRequest()) //
			);
		});

		builder.handleRequest(GetFieldNamesRequest.METHOD, call -> {
			return this.handleGetFieldNamesRequest(//
					call.get(EdgeKeys.USER_KEY), //
					GetFieldNamesRequest.from(call.getRequest()) //
			);
		});
	}

	/**
	 * Handles a GetMetersRequest.
	 *
	 * <p>
	 * See {@link DiscovergyApiClient#getMeters()}
	 *
	 * @param user    the User
	 * @param request the GetMetersRequest
	 * @return the Response
	 * @throws OpenemsNamedException on error
	 */
	private GetMetersResponse handleGetMetersRequest(User user, GetMetersRequest request) throws OpenemsNamedException {
		var meters = this.apiClient.getMeters();
		return new GetMetersResponse(request.getId(), meters);
	}

	/**
	 * Handles a GetFieldNamesRequest.
	 *
	 * <p>
	 * See {@link DiscovergyApiClient#getFieldNames(String)}
	 *
	 * @param user    the User
	 * @param request the GetFieldNamesRequest
	 * @return the Response
	 * @throws OpenemsNamedException on error
	 */
	private GetFieldNamesResponse handleGetFieldNamesRequest(User user, GetFieldNamesRequest request)
			throws OpenemsNamedException {
		var fieldNames = this.apiClient.getFieldNames(request.getMeterId());
		Set<Field> fields = new HashSet<>();
		for (JsonElement fieldNameElement : fieldNames) {
			fields.add(JsonUtils.getAsEnum(Field.class, fieldNameElement));
		}
		return new GetFieldNamesResponse(request.getId(), fields);
	}
}
