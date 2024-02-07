package io.openems.edge.system.fenecon.industrial.s;

import static io.openems.common.types.ChannelAddress.fromString;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.user.User;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine.CoolingUnitContext;
import io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine.CoolingUnitStateMachine;
import io.openems.edge.system.fenecon.industrial.s.coolingunit.statemachine.CoolingUnitStateMachine.CoolingUnitState;
import io.openems.edge.system.fenecon.industrial.s.jsonrpc.EmergencyStopAcknowledgeRequest;
import io.openems.edge.system.fenecon.industrial.s.statemachine.Context;
import io.openems.edge.system.fenecon.industrial.s.statemachine.StateMachine;
import io.openems.edge.system.fenecon.industrial.s.statemachine.StateMachine.State;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "System.Fenecon.Industrial.S", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class SystemFeneconIndustrialSImpl extends AbstractOpenemsComponent
		implements SystemFeneconIndustrialS, OpenemsComponent, StartStoppable, EventHandler, JsonApi {

	private final Logger log = LoggerFactory.getLogger(SystemFeneconIndustrialSImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);
	private final CoolingUnitStateMachine coolingUnitStateMachine = new CoolingUnitStateMachine(
			CoolingUnitState.UNDEFINED);
	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);
	private final List<BatteryFeneconF2b> batteries = new ArrayList<>();

	private ParsedConfig parsedConfig;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MANDATORY) //
	private volatile ManagedSymmetricEss ess;

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	protected synchronized void addBattery(BatteryFeneconF2b battery) {
		this.batteries.add(battery);
	}

	protected synchronized void removeBattery(BatteryFeneconF2b battery) {
		this.batteries.remove(battery);
	}

	public SystemFeneconIndustrialSImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SystemFeneconIndustrialS.ChannelId.values()//
		);
	}

	public record ParsedConfig(//
			Config config, //
			ChannelAddress emergencyStopStateChannel, //
			ChannelAddress spdStateChannel, //
			ChannelAddress fuseStateChannel, //
			ChannelAddress coolingUnitErrorChannel, //
			ChannelAddress coolingUnitEnableChannel, //
			ChannelAddress acknowledgeEmergencyStopChannel//
	) {

		/**
		 * Sets the config input-output channels as {@link ChannelAddress}.
		 * 
		 * @param config the {@link Config}
		 * @return a record for ConfigChannels
		 * @throws OpenemsNamedException on error
		 */
		public static ParsedConfig from(Config config) throws OpenemsNamedException {
			return new ParsedConfig(//
					config, //
					// Input Channels
					fromString(config.emergencyStopState()), //
					fromString(config.spdTripped()), //
					fromString(config.fuseTripped()), //
					fromString(config.coolingUnitError()), //
					// Output Channels
					fromString(config.coolingUnitEnable()), //
					fromString(config.acknowledgeEmergencyStop()) //
			);
		}
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (!this.applyConfig(context, config)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled());
		if (!this.applyConfig(context, config)) {
			return;
		}
	}

	private boolean applyConfig(ComponentContext context, Config config) throws OpenemsNamedException {
		this.removeAcknowledgeCallback();
		this.parsedConfig = ParsedConfig.from(config);
		this.addAcknowledgeCallBack();

		return OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())//
				&& OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Battery", config.battery_ids());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.removeAcknowledgeCallback();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			this.handleStateMachine();
			this.handleInputChannel(this.parsedConfig.coolingUnitErrorChannel(),
					SystemFeneconIndustrialS.ChannelId.COOLING_UNIT_ERROR_STATE);
			this.handleInputChannel(this.parsedConfig.emergencyStopStateChannel(),
					SystemFeneconIndustrialS.ChannelId.EMERGENCY_STOP_STATE);
			this.handleInputChannel(this.parsedConfig.spdStateChannel(),
					SystemFeneconIndustrialS.ChannelId.SPD_TRIPPED);
			this.handleInputChannel(this.parsedConfig.fuseStateChannel(),
					SystemFeneconIndustrialS.ChannelId.FUSE_TRIPPED);
			this.handleAcknowledge();
		}
		}
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		// Store the current State
		this._setStateMachine(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		try {
			// Prepare Context
			var context = new Context(this, this.ess);
			var coolingUnitContext = new CoolingUnitContext(this, //
					this.componentManager.getClock(), //
					this.batteries, //
					this.componentManager.getChannel(this.parsedConfig.coolingUnitErrorChannel()), //
					this.componentManager.getChannel(this.parsedConfig.coolingUnitEnableChannel()));

			// Call the StateMachine
			WriteChannel<Boolean> coolingUnitEnableChannel = this.componentManager
					.getChannel(this.parsedConfig.coolingUnitEnableChannel());
			switch (this.parsedConfig.config.coolingUnitMode()) {
			case AUTO -> {
				this._setCoolingUnitStateMachine(this.coolingUnitStateMachine.getCurrentState());
				this.coolingUnitStateMachine.run(coolingUnitContext);
			}
			case MANUAL_OFF -> {
				coolingUnitEnableChannel.setNextWriteValue(false);
				this.coolingUnitStateMachine.forceNextState(CoolingUnitState.STOP_COOLING);
			}
			case MANUAL_ON -> {
				coolingUnitEnableChannel.setNextWriteValue(true);
				this.coolingUnitStateMachine.forceNextState(CoolingUnitState.START_COOLING);
			}
			}

			this.stateMachine.run(context);
			this.coolingUnitStateMachine.run(coolingUnitContext);
			this._setRunFailed(false);
		} catch (OpenemsNamedException e) {
			this._setRunFailed(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public void setStartStop(StartStop value) {
		this.startStopTarget.set(value);
	}

	/**
	 * Gets the channel value from given {@link ChannelAddress} and sets on the
	 * given method.
	 * 
	 * @param channelAddress the channel value to be read from.
	 * @param channelId      the {@link SystemFeneconIndustrialS.ChannelId} to set
	 *                       the current value.
	 */
	private void handleInputChannel(ChannelAddress channelAddress, SystemFeneconIndustrialS.ChannelId channelId) {
		try {
			Channel<Boolean> channel = this.componentManager.getChannel(channelAddress);
			var value = channel.value();
			// TODO Could cause the channel value to be not set if IO disappears
			if (!value.isDefined()) {
				return;
			}
			this.channel(channelId).setNextValue(value.get());
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget() {
		return switch (this.parsedConfig.config.startStop()) {
		case AUTO -> this.startStopTarget.get();
		case START -> StartStop.START;
		case STOP -> StartStop.STOP;
		};
	}

	/**
	 * Gets the channel value from given channel address.
	 * 
	 * @return true if its not null, or its defined and true.
	 */
	public boolean isInEmergencyStopState() {
		try {
			Channel<Boolean> channel = this.componentManager.getChannel(this.parsedConfig.emergencyStopStateChannel());
			return channel.value().orElse(false);
		} catch (Exception e) {
			this.logError(this.log, e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		return false;
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append(this.stateMachine.getCurrentState()) //
				.append("|")//
				.append("CoolingUnit: ")//
				.append(this.coolingUnitStateMachine.getCurrentState()) //
				.toString();
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.INSTALLER);
		switch (request.getMethod()) {
		case EmergencyStopAcknowledgeRequest.METHOD -> {
			return this.acknowledgeEmergencyStop(user, request);
		}
		}
		return null;
	}

	private final AtomicBoolean allowAcknowledge = new AtomicBoolean(false);

	private CompletableFuture<? extends JsonrpcResponseSuccess> acknowledgeEmergencyStop(User user,
			JsonrpcRequest request) throws OpenemsNamedException {
		try {
			WriteChannel<Boolean> outputChannel = this.componentManager
					.getChannel(this.parsedConfig.acknowledgeEmergencyStopChannel());
			outputChannel.setNextWriteValue(true);
			return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));

		} catch (Exception e) {
			this.logError(this.log, e.getClass().getSimpleName() + ": " + e.getMessage());
			throw e;
		}
	}

	private void handleAcknowledge() {
		try {
			WriteChannel<Boolean> outputChannel = this.componentManager
					.getChannel(this.parsedConfig.acknowledgeEmergencyStopChannel);
			if (!this.isInEmergencyStopState() && this.allowAcknowledge.get()) {
				outputChannel.setNextWriteValue(false);
			}
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private void addAcknowledgeCallBack() {
		// TODO Would be better to keep IO as a @Reference.
		// See https://git.intranet.fenecon.de/FENECON/fems/pulls/799#issuecomment-23990
		try {
			WriteChannel<Boolean> channel = this.componentManager
					.getChannel(this.parsedConfig.acknowledgeEmergencyStopChannel());
			channel.onSetNextWrite(this.acknowledgeCallback);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private final ThrowingConsumer<Boolean, OpenemsNamedException> acknowledgeCallback = value -> {
		// TODO possible bug because of core.cycle, update accordingly
		if (value == null || !value) {
			this.allowAcknowledge.set(false);
		}
		this.allowAcknowledge.set(true);
	};

	private void removeAcknowledgeCallback() {
		if (this.parsedConfig == null) {
			return;
		}

		try {
			WriteChannel<Boolean> outputChannel = this.componentManager
					.getChannel(this.parsedConfig.acknowledgeEmergencyStopChannel());
			outputChannel.getOnSetNextWrites().remove(this.acknowledgeCallback);

		} catch (Exception e) {
			this.logError(this.log, e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
