package io.openems.edge.simulator.app;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;

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

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.test.TimeLeapClock;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Simulator.App", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"id=" + OpenemsConstants.SIMULATOR_ID, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
		})
public class SimulatorApp extends AbstractOpenemsComponent
		implements ClockProvider, OpenemsComponent, JsonApi, EventHandler {

	private final Logger log = LoggerFactory.getLogger(SimulatorApp.class);

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference
	private ComponentManager componentManager;

	private volatile User currentSimulationUser = null;
	private volatile ExecuteSimulationRequest currentSimulationRequest = null;
	private volatile Clock clock = Clock.systemDefaultZone();

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
			this.stopSimulation(currentSimulationUser);
		}
	}

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

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || this.currentSimulationRequest == null || this.currentSimulationUser == null) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
			Clock clock = this.clock;
			ExecuteSimulationRequest currentSimulationRequest = this.currentSimulationRequest;
			User currentSimulationUser = this.currentSimulationUser;

			if (currentSimulationRequest != null && clock instanceof TimeLeapClock) {
				// Apply simulated Time-Leap per Cycle
				this.applyTimeLeap((TimeLeapClock) clock, currentSimulationRequest);

				LocalDateTime now = LocalDateTime.now(clock);
				if (now.isAfter(currentSimulationRequest.clock.end)) {
					// Stop simulation
					this.stopSimulation(currentSimulationUser);
				}
			}
			break;
		}
	}

	/**
	 * Apply simulated Time-Leap per Cycle.
	 * 
	 * @param clock             the {@link TimeLeapClock}
	 * @param currentSimulation the current {@link ExecuteSimulationRequest}
	 */
	private void applyTimeLeap(TimeLeapClock clock, ExecuteSimulationRequest currentSimulationRequest) {
		clock.leap(currentSimulationRequest.clock.timeleapPerCycle, ChronoUnit.MILLIS);
	}

	/**
	 * Delete all non-required Components.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void deleteAllConfigurations(User user) throws OpenemsNamedException {
		for (OpenemsComponent component : this.componentManager.getEnabledComponents()) {
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
	}

	/**
	 * Stop the Simulation.
	 * 
	 * @param user the current simulation {@link User}
	 */
	private void stopSimulation(User user) {
		this.logInfo(this.log, "#");
		this.logInfo(this.log, "# Stopping Simulation");
		this.logInfo(this.log, "#");

		this.clock = Clock.systemDefaultZone();
		this.currentSimulationRequest = null;
		this.currentSimulationUser = null;
		this.setCycleTime(Cycle.DEFAULT_CYCLE_TIME);
		try {
			this.deleteAllConfigurations(user);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Unable to stop Simulation: " + e.getMessage());
			e.printStackTrace();
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
	private synchronized CompletableFuture<JsonrpcResponseSuccess> handleExecuteSimulationRequest(User user,
			ExecuteSimulationRequest request) throws OpenemsNamedException {
		this.logInfo(this.log, "#");
		this.logInfo(this.log, "# Starting Simulation");
		this.logInfo(this.log, "#");

		this.deleteAllConfigurations(user);

		/*
		 * Configure Clock
		 */
		TimeLeapClock timeLeapClock = new TimeLeapClock(//
				request.clock.start.toInstant(ZoneOffset.UTC), ZoneId.systemDefault());
		this.clock = timeLeapClock;

		/*
		 * Create Components
		 */
		for (CreateComponentConfigRequest createRequest : request.components) {
			this.logInfo(this.log, "Create Component [" + createRequest.getComponentId() + "] from ["
					+ createRequest.getFactoryPid() + "]");
			this.componentManager.handleJsonrpcRequest(user, createRequest);
		}

		this.currentSimulationRequest = request;
		this.currentSimulationUser = user;

		/*
		 * Start Simulation Cycles
		 */
		this.setCycleTime(AbstractWorker.DO_NOT_WAIT);

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	private void deleteComponent(User user, String componentId) throws OpenemsNamedException {
		this.logInfo(this.log, "Delete Component [" + componentId + "]");
		DeleteComponentConfigRequest deleteComponentConfigRequest = new DeleteComponentConfigRequest(componentId);
		this.componentManager.handleJsonrpcRequest(user, deleteComponentConfigRequest);
	}

	@Override
	public Clock getClock() {
		return this.clock;
	}
}
