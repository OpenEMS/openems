package io.openems.edge.simulator.app;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.session.Role;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.common.user.User;
import io.openems.edge.simulator.app.ExecuteSimulationRequest.Profile;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.Timeranges;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = SimulatorAppImpl.SINGLETON_SERVICE_PID, //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
})
public class SimulatorAppImpl extends AbstractOpenemsComponent
		implements SimulatorApp, SimulatorDatasource, ClockProvider, OpenemsComponent, JsonApi, EventHandler, Timedata {

	public static final String SINGLETON_SERVICE_PID = "Simulator.App";
	public static final String SINGLETON_COMPONENT_ID = "_simulator";

	private static final long MILLISECONDS_BETWEEN_LOGS = 5_000;

	private final Logger log = LoggerFactory.getLogger(SimulatorAppImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	private static class CurrentSimulation {
		private final User user;
		private final ExecuteSimulationRequest request;
		private final TimeLeapClock clock;
		private final CompletableFuture<ExecuteSimulationResponse> response;
		private final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> collectedData = new TreeMap<>();

		public CurrentSimulation(User user, ExecuteSimulationRequest request, TimeLeapClock clock,
				CompletableFuture<ExecuteSimulationResponse> response) {
			this.user = user;
			this.request = request;
			this.clock = clock;
			this.response = response;
		}

		public void addData(ZonedDateTime timestamp, List<Channel<?>> channels) {
			SortedMap<ChannelAddress, JsonElement> values = new TreeMap<>();
			for (Channel<?> channel : channels) {
				values.put(channel.address(), channel.value().asJson());
			}
			this.collectedData.put(timestamp, values);
		}
	}

	private volatile CurrentSimulation currentSimulation = null;
	private volatile CurrentSimulation lastSimulation = null;

	public SimulatorAppImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SimulatorApp.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext componentContext, Config config) throws OpenemsException {
		super.activate(componentContext, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, config.enabled());

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Activate
	private void modified(ComponentContext componentContext, Config config) throws OpenemsException {
		super.modified(componentContext, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, config.enabled());

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.isEnabled()) {
			this.stopSimulation();
		}
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.ADMIN);

		switch (request.getMethod()) {

		case ExecuteSimulationRequest.METHOD:
			return this.handleExecuteSimulationRequest(user, ExecuteSimulationRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a {@link ExecuteSimulationRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link ExecuteSimulationRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private synchronized CompletableFuture<ExecuteSimulationResponse> handleExecuteSimulationRequest(User user,
			ExecuteSimulationRequest request) throws OpenemsNamedException {
		this.logInfo(this.log, "Starting Simulation");

		this.deleteAllConfigurations(user);

		// Stop Cycle
		this.setCycleTime(AbstractWorker.ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN);

		// Create Ess.Power with disabled PID filter
		this.componentManager.handleJsonrpcRequest(user,
				new CreateComponentConfigRequest("Ess.Power", Arrays.asList(new Property("enablePid", false))));

		// Create Components
		Set<String> simulatorComponentIds = new HashSet<>();
		for (CreateComponentConfigRequest createRequest : request.components) {
			this.logInfo(this.log, "Create Component [" + createRequest.getComponentId() + "] from ["
					+ createRequest.getFactoryPid() + "]");
			simulatorComponentIds.add(createRequest.getComponentId());
			this.componentManager.handleJsonrpcRequest(user, createRequest);
		}
		this.waitForComponentsToActivate(simulatorComponentIds);

		this.logInfo(this.log, "All Simulator-Components are activated!");

		// prepare response
		var response = new CompletableFuture<ExecuteSimulationResponse>();

		// Configure Clock
		var timeLeapClock = new TimeLeapClock(//
				request.clock.start.toInstant(), ZoneId.systemDefault());

		// keep simulation data for later use
		this.lastSimulation = null;
		this.currentSimulation = new CurrentSimulation(user, request, timeLeapClock, response);

		// Start Simulation Cycles
		this.setCycleTime(AbstractWorker.DO_NOT_WAIT);

		return response;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || this.currentSimulation == null) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.collectData();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
			this.simulateNextCycle();
			break;
		}
	}

	@Override
	public Clock getClock() {
		var currentSimulation = this.currentSimulation;
		if (currentSimulation != null) {
			return currentSimulation.clock;
		}
		return Clock.systemDefaultZone();
	}

	private long lastLogMessage = 0;

	/**
	 * Is executed on every Cycle After Write Event.
	 */
	private void simulateNextCycle() {
		var currentSimulation = this.currentSimulation;
		if (currentSimulation == null) {
			return;
		}

		// Apply simulated Time-Leap per Cycle
		this.applyTimeLeap(currentSimulation.clock, currentSimulation.request);

		var now = ZonedDateTime.now(currentSimulation.clock);

		if (System.currentTimeMillis() - MILLISECONDS_BETWEEN_LOGS > this.lastLogMessage) {
			this.logInfo(this.log, "Simulating " + now.withZoneSameInstant(ZoneId.of("UTC")));
			this.lastLogMessage = System.currentTimeMillis();
		}

		if (now.isAfter(currentSimulation.request.clock.end)) {
			// Stop simulation
			this.stopSimulation();
		}
	}

	private void collectData() {
		var currentSimulation = this.currentSimulation;
		if (currentSimulation == null) {
			return;
		}

		var now = ZonedDateTime.now(currentSimulation.clock);
		List<Channel<?>> channels = new ArrayList<>();
		for (ChannelAddress channelAddress : currentSimulation.request.collects) {
			try {
				channels.add(this.componentManager.getChannel(channelAddress));
			} catch (IllegalArgumentException | OpenemsNamedException e) {
				e.printStackTrace();
			}
		}
		currentSimulation.addData(now, channels);
	}

	/**
	 * Apply simulated Time-Leap per Cycle.
	 *
	 * @param clock                    the {@link TimeLeapClock}
	 * @param currentSimulationRequest the current {@link ExecuteSimulationRequest}
	 */
	private void applyTimeLeap(TimeLeapClock clock, ExecuteSimulationRequest currentSimulationRequest) {
		if (currentSimulationRequest.clock.executeCycleTwice) {
			if (++this.repeatCounter == 2) {
				this.repeatCounter = 0;
			}
		}
		if (this.repeatCounter == 0) {
			// Apply time leap
			clock.leap(currentSimulationRequest.clock.timeleapPerCycle, ChronoUnit.MILLIS);

			// Select next profile values
			for (Profile profile : this.currentSimulation.request.profiles.values()) {
				profile.selectNextValue();
			}
		}
	}

	/**
	 * This "flip-flop" boolean is used to implement the 'executeCycleTwice' in the
	 * {@link ExecuteSimulationRequest}.
	 */
	private int repeatCounter = 0;

	/**
	 * Delete all non-required Components.
	 *
	 * @param user the {@link User}
	 * @throws OpenemsNamedException on error
	 */
	private void deleteAllConfigurations(User user) throws OpenemsNamedException {
		Set<String> deletedComponents = new HashSet<>();
		for (OpenemsComponent component : this.componentManager.getAllComponents()) {
			deletedComponents.add(component.id());
			var factoryPid = component.serviceFactoryPid();
			if (factoryPid == null || factoryPid.trim().isEmpty()) {
				continue;
			}
			if (factoryPid.startsWith("Core.") //
					|| factoryPid.startsWith("Controller.Api.") //
					|| factoryPid.startsWith("Predictor.")) {
				continue;
			}
			switch (factoryPid) {
			case "Simulator.App":
				// ignore
				break;
			default:
				// delete
				this.deleteComponent(user, component.id());
			}
		}
		this.waitForComponentsToDeactivate(deletedComponents);
		this.logInfo(this.log, "All Components are deactivated!");
	}

	/**
	 * Stop the Simulation.
	 */
	private void stopSimulation() {
		this.logInfo(this.log, "Stopping Simulation");

		var currentSimulation = this.currentSimulation;
		final User user;
		if (currentSimulation != null) {
			user = currentSimulation.user;
			currentSimulation.response.complete(
					new ExecuteSimulationResponse(currentSimulation.request.getId(), currentSimulation.collectedData));
		} else {
			user = null;
		}

		this.lastSimulation = this.currentSimulation;
		this.currentSimulation = null;
		this.setCycleTime(Cycle.DEFAULT_CYCLE_TIME);

		try {
			this.deleteAllConfigurations(user);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Unable to stop Simulation: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Deletes a Component configuration.
	 *
	 * @param user        the {@link User}
	 * @param componentId the Component-ID
	 * @throws OpenemsNamedException on error
	 */
	private void deleteComponent(User user, String componentId) throws OpenemsNamedException {
		this.logInfo(this.log, "Delete Component [" + componentId + "]");
		var deleteComponentConfigRequest = new DeleteComponentConfigRequest(componentId);
		this.componentManager.handleJsonrpcRequest(user, deleteComponentConfigRequest);
	}

	/**
	 * Sets the global OpenEMS Edge Cycle-Time.
	 *
	 * @param cycleTime the cycleTime in [ms]
	 */
	private void setCycleTime(int cycleTime) {
		try {
			var config = this.cm.getConfiguration("Core.Cycle", null);
			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put("cycleTime", cycleTime);
			config.update(properties);
		} catch (IOException e) {
			this.logError(this.log, "Unable to configure Core Cycle-Time. " + e.getClass() + ": " + e.getMessage());
		}
	}

	private void waitForComponentsToActivate(Set<String> simulatorComponentIds) throws OpenemsException {
		// Wait for Components to appear
		for (var i = 0; i < 100; i++) {
			Set<String> allComponentIds = this.componentManager.getAllComponents().stream().map(OpenemsComponent::id)
					.collect(Collectors.toSet());
			simulatorComponentIds.removeAll(allComponentIds);

			if (simulatorComponentIds.isEmpty()) {
				// finished
				return;
			}
			this.logInfo(this.log, "Still waiting for [" + simulatorComponentIds + "] to activate");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				this.log.warn(e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}
		throw new OpenemsException("Timeout while waiting for [" + simulatorComponentIds + "] to activate");
	}

	private void waitForComponentsToDeactivate(Set<String> deletedComponents) throws OpenemsException {
		Set<String> stillExistingComponents = new HashSet<>();
		for (var i = 0; i < 100; i++) {
			var allComponents = this.componentManager.getAllComponents();
			stillExistingComponents = allComponents.stream().map(OpenemsComponent::id).collect(Collectors.toSet());
			stillExistingComponents.removeAll(deletedComponents);

			if (stillExistingComponents.isEmpty()) {
				// finished
				return;
			}
			this.log.info("Still waiting for [" + stillExistingComponents + "] to disappear");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				this.log.warn(e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}
		throw new OpenemsException("Timeout while waiting for [" + stillExistingComponents + "] to disappear");
	}

	@Override
	public Set<String> getKeys() {
		if (this.currentSimulation == null) {
			return new HashSet<>();
		}
		return this.currentSimulation.request.profiles.keySet();
	}

	@Override
	public int getTimeDelta() {
		return -1;
	}

	@Override
	public <T> T getValue(OpenemsType type, ChannelAddress channelAddress) {
		if (this.currentSimulation == null) {
			return null;
		}
		// First: try full ChannelAddress
		var profile = this.currentSimulation.request.profiles.get(channelAddress.toString());
		if (profile == null) {
			// Not found: try Channel-ID only (without Component-ID)
			profile = this.currentSimulation.request.profiles.get(channelAddress.getChannelId());
		}
		return TypeUtils.getAsType(type, profile.getCurrentValue());
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		if (this.lastSimulation == null || this.lastSimulation.collectedData.isEmpty()) {
			// return empty result
			return new TreeMap<>();
		}

		var fakePeriod = this.convertToSimulatedFromToDates(fromDate, toDate);
		var data = this.lastSimulation.collectedData.subMap(fakePeriod.fromDate, fakePeriod.toDate);

		if (channels.isEmpty()) {
			// No Channels given -> return all data
			return data;
		}

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> result = new TreeMap<>();
		for (Entry<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> entry : this.lastSimulation.collectedData
				.subMap(fakePeriod.fromDate, fakePeriod.toDate).entrySet()) {
			var values = entry.getValue();
			var resultPerTimestamp = new TreeMap<ChannelAddress, JsonElement>();
			for (ChannelAddress channel : channels) {
				var value = values.get(channel);
				resultPerTimestamp.put(channel, value == null ? JsonNull.INSTANCE : value);
			}
			result.put(entry.getKey(), resultPerTimestamp);
		}
		return result;
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		if (this.lastSimulation == null || this.lastSimulation.collectedData.isEmpty()) {
			return new TreeMap<>();
		}
		var fakePeriod = this.convertToSimulatedFromToDates(fromDate, toDate);
		var partOfCollectedData = this.lastSimulation.collectedData.subMap(fakePeriod.fromDate, fakePeriod.toDate);
		SortedMap<ChannelAddress, JsonElement> result = new TreeMap<>();
		var firstValues = partOfCollectedData.get(partOfCollectedData.firstKey());
		var lastValues = partOfCollectedData.get(partOfCollectedData.lastKey());
		for (ChannelAddress channel : channels) {
			var firstValue = (Long) JsonUtils.getAsType(Long.class, firstValues.get(channel));
			var lastValue = (Long) JsonUtils.getAsType(Long.class, lastValues.get(channel));
			if (firstValue != null && lastValue != null) {
				result.put(channel, new JsonPrimitive(lastValue - firstValue));
			} else {
				result.put(channel, JsonNull.INSTANCE);
			}
		}
		return result;
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		throw new NotImplementedException("QueryHistoryEnergyPerPeriod is not implemented for Simulator-App");
	}

	@Override
	public SortedMap<Long, SortedMap<ChannelAddress, JsonElement>> queryResendData(ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		throw new NotImplementedException("QueryResendData is not implemented for Simulator-App");
	}

	@Override
	public Timeranges getResendTimeranges(ChannelAddress notSendChannel, long lastResendTimestamp)
			throws OpenemsNamedException {
		throw new NotImplementedException("GetResendTimeranges is not implemented for Simulator-App");
	}

	@Override
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress) {
		final JsonElement value;
		if (this.lastSimulation == null || this.lastSimulation.collectedData.isEmpty()) {
			value = JsonNull.INSTANCE;
		} else {
			var lastValues = this.lastSimulation.collectedData.get(this.lastSimulation.collectedData.lastKey());
			value = lastValues.get(channelAddress);
		}
		return CompletableFuture.completedFuture(Optional.ofNullable(value));
	}

	/**
	 * Helper class for
	 * {@link SimulatorAppImpl#convertToSimulatedFromToDates(ZonedDateTime, ZonedDateTime)}.
	 */
	private static class Period {
		private final ZonedDateTime fromDate;
		private final ZonedDateTime toDate;

		public Period(ZonedDateTime fromDate, ZonedDateTime toDate) {
			this.fromDate = fromDate;
			this.toDate = toDate;
		}
	}

	/**
	 * Adjusts the FromDate and ToDate as if they would be current.
	 *
	 * <p>
	 * For the simulation the fromDate and toDate do not actually matter, so very
	 * often something like 1st January 2000 will be used. That would be
	 * inconvenient to visualize in OpenEMS UI, so we fake the dates here.
	 *
	 * @param fromDate the original Request fromDate
	 * @param toDate   the original Request toDate
	 * @return a {@link Period} with faked fromDate and toDate
	 */
	private Period convertToSimulatedFromToDates(ZonedDateTime fromDate, ZonedDateTime toDate) {
		if (this.lastSimulation == null) {
			return null;
		}
		var durationDays = Duration.between(fromDate, toDate).toDays();
		var toDateOffset = Duration.between(toDate, ZonedDateTime.now()).toDays();
		var lastCollected = this.lastSimulation.collectedData.lastKey();
		var newToDate = lastCollected.minusDays(toDateOffset);
		var newFromDate = newToDate.minusDays(durationDays);
		return new Period(newFromDate, newToDate);
	}

}
