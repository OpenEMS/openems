package io.openems.edge.simulator.application;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.test.TimeLeapClock;

@Component(//
		name = "Simulator", //
		immediate = true, //
		property = { //
				"id=_simulator", //
				"enabled=true", //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
		})
public class Simulator extends AbstractOpenemsComponent
		implements ClockProvider, OpenemsComponent, JsonApi, EventHandler {

	private final Logger log = LoggerFactory.getLogger(Simulator.class);

	@Reference
	private ComponentManager componentManager;

	private volatile ExecuteSimulationRequest currentSimulation = null;
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

	public Simulator() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext componentContext, BundleContext bundleContext) throws OpenemsException {
		super.activate(componentContext, "_simulator", "Simulator", true);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
			Clock clock = this.clock;
			ExecuteSimulationRequest currentSimulation = this.currentSimulation;
			if (currentSimulation != null && clock instanceof TimeLeapClock) {
				((TimeLeapClock) clock).leap(currentSimulation.clock.timeleap, ChronoUnit.SECONDS);
			}
			break;
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
		/*
		 * Delete all non-required Components
		 */
		for (OpenemsComponent component : this.componentManager.getEnabledComponents()) {
			if (component.serviceFactoryPid() == null || component.serviceFactoryPid().trim().isEmpty()) {
				continue;
			}
			System.out.println(component.serviceFactoryPid());
			switch (component.serviceFactoryPid()) {
			case "Controller.Api.Rest.ReadOnly":
			case "Controller.Api.ModbusTcp.ReadOnly":
				// ignore
				break;
			default:
				// delete
				this.deleteComponent(user, component.id());
			}
		}

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

		this.currentSimulation = request;

//		NetworkConfiguration config = this.operatingSystem.getNetworkConfiguration();
//		GetNetworkConfigResponse response = new GetNetworkConfigResponse(request.getId(), config);
//		return CompletableFuture.completedFuture(response);
		return CompletableFuture.completedFuture(null);
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
