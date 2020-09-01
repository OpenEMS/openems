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
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest.Property;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.simulator.app.ExecuteSimulationRequest.Profile;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Simulator.App", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"id=" + OpenemsConstants.SIMULATOR_ID, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
		})
public class SimulatorApp extends AbstractOpenemsComponent
		implements SimulatorDatasource, ClockProvider, OpenemsComponent, JsonApi, EventHandler, Timedata {

	private static final long MILLISECONDS_BETWEEN_LOGS = 5_000;

	private final Logger log = LoggerFactory.getLogger(SimulatorApp.class);

	@Reference
	private ConfigurationAdmin configurationAdmin;

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
			super();
			this.user = user;
			this.request = request;
			this.clock = clock;
			this.response = response;
		}

		public void addData(ZonedDateTime timestamp, List<Channel<?>> channels) {
			SortedMap<ChannelAddress, JsonElement> values = new TreeMap<ChannelAddress, JsonElement>();
			for (Channel<?> channel : channels) {
				values.put(channel.address(), channel.value().asJson());
			}
			this.collectedData.put(timestamp, values);
		}
	}

	private volatile CurrentSimulation currentSimulation = null;
	private volatile CurrentSimulation lastSimulation = null;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public SimulatorApp() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext componentContext, Config config) throws OpenemsException {
		super.activate(componentContext, OpenemsConstants.SIMULATOR_ID, "Simulator", config.enabled());
	}

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
	 * Handles a ExecuteSimulationRequest.
	 * 
	 * @param user    the User
	 * @param request the ExecuteSimulationRequest
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
		Set<String> simulatorComponentIds = new HashSet<String>();
		for (CreateComponentConfigRequest createRequest : request.components) {
			this.logInfo(this.log, "Create Component [" + createRequest.getComponentId() + "] from ["
					+ createRequest.getFactoryPid() + "]");
			simulatorComponentIds.add(createRequest.getComponentId());
			this.componentManager.handleJsonrpcRequest(user, createRequest);
		}
		this.waitForComponentsToActivate(simulatorComponentIds);

		this.logInfo(this.log, "All Simulator-Components are activated!");

		// prepare response
		CompletableFuture<ExecuteSimulationResponse> response = new CompletableFuture<ExecuteSimulationResponse>();

		// Configure Clock
		TimeLeapClock timeLeapClock = new TimeLeapClock(//
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
		CurrentSimulation currentSimulation = this.currentSimulation;
		if (currentSimulation != null) {
			return currentSimulation.clock;
		} else {
			return Clock.systemDefaultZone();
		}
	}

	private long lastLogMessage = 0;

	/**
	 * Is executed on every Cycle After Write Event.
	 */
	private void simulateNextCycle() {
		CurrentSimulation currentSimulation = this.currentSimulation;
		if (currentSimulation == null) {
			return;
		}

		// Apply simulated Time-Leap per Cycle
		this.applyTimeLeap(currentSimulation.clock, currentSimulation.request);

		ZonedDateTime now = ZonedDateTime.now(currentSimulation.clock);

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
		CurrentSimulation currentSimulation = this.currentSimulation;
		if (currentSimulation == null) {
			return;
		}

		ZonedDateTime now = ZonedDateTime.now(currentSimulation.clock);
		List<Channel<?>> channels = new ArrayList<Channel<?>>();
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
	 * @param clock             the {@link TimeLeapClock}
	 * @param currentSimulation the current {@link ExecuteSimulationRequest}
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
	 * @throws OpenemsNamedException on error
	 */
	private void deleteAllConfigurations(User user) throws OpenemsNamedException {
		Set<String> deletedComponents = new HashSet<>();
		for (OpenemsComponent component : this.componentManager.getAllComponents()) {
			deletedComponents.add(component.id());
			String factoryPid = component.serviceFactoryPid();
			if (factoryPid == null || factoryPid.trim().isEmpty()) {
				continue;
			}
			if (factoryPid.startsWith("Core.") || factoryPid.startsWith("Controller.Api.")) {
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
	 * 
	 * @param currentSimulation the current simulation
	 */
	private void stopSimulation() {
		this.logInfo(this.log, "Stopping Simulation");

		CurrentSimulation currentSimulation = this.currentSimulation;
		User user;
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
	 * @param user        the User
	 * @param componentId the Component-ID
	 * @throws OpenemsNamedException on error
	 */
	private void deleteComponent(User user, String componentId) throws OpenemsNamedException {
		this.logInfo(this.log, "Delete Component [" + componentId + "]");
		DeleteComponentConfigRequest deleteComponentConfigRequest = new DeleteComponentConfigRequest(componentId);
		this.componentManager.handleJsonrpcRequest(user, deleteComponentConfigRequest);
	}

	/**
	 * Sets the global OpenEMS Edge Cycle-Time.
	 * 
	 * @param cycleTime the cycleTime in [ms]
	 */
	private void setCycleTime(int cycleTime) {
		try {
			Configuration config = this.configurationAdmin.getConfiguration("Core.Cycle", null);
			Dictionary<String, Object> properties = new Hashtable<>();
			properties.put("cycleTime", cycleTime);
			config.update(properties);
		} catch (IOException e) {
			this.logError(this.log, "Unable to configure Core Cycle-Time. " + e.getClass() + ": " + e.getMessage());
		}
	}

	private void waitForComponentsToActivate(Set<String> simulatorComponentIds) throws OpenemsException {
		// Wait for Components to appear
		for (int i = 0; i < 100; i++) {
			Set<String> allComponentIds = this.componentManager.getAllComponents().stream().map(c -> c.id())
					.collect(Collectors.toSet());
			simulatorComponentIds.removeAll(allComponentIds);

			if (simulatorComponentIds.isEmpty()) {
				// finished
				return;
			} else {
				this.logInfo(this.log, "Still waiting for [" + simulatorComponentIds + "] to activate");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					this.log.warn(e.getClass().getSimpleName() + ": " + e.getMessage());
				}
			}
		}
		throw new OpenemsException("Timeout while waiting for [" + simulatorComponentIds + "] to activate");
	}

	private void waitForComponentsToDeactivate(Set<String> deletedComponents) throws OpenemsException {
		Set<String> stillExistingComponents = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			List<OpenemsComponent> allComponents = this.componentManager.getAllComponents();
			stillExistingComponents = allComponents.stream().map(c -> c.id()).collect(Collectors.toSet());
			stillExistingComponents.removeAll(deletedComponents);

			if (stillExistingComponents.isEmpty()) {
				// finished
				return;
			} else {
				this.log.info("Still waiting for [" + stillExistingComponents + "] to disappear");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					this.log.warn(e.getClass().getSimpleName() + ": " + e.getMessage());
				}
			}
		}
		throw new OpenemsException("Timeout while waiting for [" + stillExistingComponents + "] to disappear");
	}

	@Override
	public Set<String> getKeys() {
		if (this.currentSimulation == null) {
			return new HashSet<String>();
		}
		return this.currentSimulation.request.profiles.keySet();
	}

	@Override
	public int getTimeDelta() {
		return -1;
	}

	@Override
	public <T> T getValue(OpenemsType type, String channelAddress) {
		if (this.currentSimulation == null) {
			return null;
		}
		return TypeUtils.getAsType(type, this.currentSimulation.request.profiles.get(channelAddress).getCurrentValue());
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
			throws OpenemsNamedException {
		if (this.lastSimulation == null || this.lastSimulation.collectedData.isEmpty()) {
			return new TreeMap<>();
		}
		Period fakePeriod = this.convertToSimulatedFromToDates(fromDate, toDate);
		return this.lastSimulation.collectedData.subMap(fakePeriod.fromDate, fakePeriod.toDate);
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		if (this.lastSimulation == null || this.lastSimulation.collectedData.isEmpty()) {
			return new TreeMap<>();
		}
		Period fakePeriod = this.convertToSimulatedFromToDates(fromDate, toDate);
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> partOfCollectedData = this.lastSimulation.collectedData
				.subMap(fakePeriod.fromDate, fakePeriod.toDate);
		SortedMap<ChannelAddress, JsonElement> result = new TreeMap<ChannelAddress, JsonElement>();
		SortedMap<ChannelAddress, JsonElement> firstValues = partOfCollectedData.get(partOfCollectedData.firstKey());
		SortedMap<ChannelAddress, JsonElement> lastValues = partOfCollectedData.get(partOfCollectedData.lastKey());
		for (ChannelAddress channel : channels) {
			Long firstValue = (Long) JsonUtils.getAsType(Long.class, firstValues.get(channel));
			Long lastValue = (Long) JsonUtils.getAsType(Long.class, lastValues.get(channel));
			if (firstValue != null && lastValue != null) {
				result.put(channel, new JsonPrimitive(lastValue - firstValue));
			} else {
				result.put(channel, JsonNull.INSTANCE);
			}
		}
		return result;
	}

	@Override
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress) {
		final JsonElement value;
		if (this.lastSimulation == null || this.lastSimulation.collectedData.isEmpty()) {
			value = JsonNull.INSTANCE;
		} else {
			SortedMap<ChannelAddress, JsonElement> lastValues = this.lastSimulation.collectedData
					.get(this.lastSimulation.collectedData.lastKey());
			value = lastValues.get(channelAddress);
		}
		return CompletableFuture.completedFuture(Optional.ofNullable(value));
	}

	/**
	 * Helper class for
	 * {@link SimulatorApp#convertToSimulatedFromToDates(ZonedDateTime, ZonedDateTime)}.
	 */
	private static class Period {
		private final ZonedDateTime fromDate;
		private final ZonedDateTime toDate;

		public Period(ZonedDateTime fromDate, ZonedDateTime toDate) {
			super();
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
		long durationDays = Duration.between(fromDate, toDate).toDays();
		long toDateOffset = Duration.between(toDate, ZonedDateTime.now()).toDays();
		ZonedDateTime lastCollected = this.lastSimulation.collectedData.lastKey();
		ZonedDateTime newToDate = lastCollected.minusDays(toDateOffset);
		ZonedDateTime newFromDate = newToDate.minusDays(durationDays);
		return new Period(newFromDate, newToDate);
	}

}
