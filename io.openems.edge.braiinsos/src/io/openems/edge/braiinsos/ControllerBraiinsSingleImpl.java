package io.openems.edge.braiinsos;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.MeterType;
import io.openems.edge.braiinsos.api.BraiinsApi;
import io.openems.edge.braiinsos.api.MinerStats;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.BraiinsOS.Single", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class ControllerBraiinsSingleImpl extends AbstractOpenemsComponent implements Controller, SinglePhaseMeter,
		ElectricityMeter, ControllerBraiinsSingle, OpenemsComponent, EventHandler, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(ControllerBraiinsSingleImpl.class);
	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference(cardinality = MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;

	private BraiinsApi braiinApi;
	private Config config;

	public ControllerBraiinsSingleImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerBraiinsSingle.ChannelId.values() //
		);

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	protected void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	private void applyConfig(Config config) {
		this.config = config;

		// Cleanup
		if (this.braiinApi != null) {
			this.braiinApi.deactivate();
			this.braiinApi = null;
		}

		if (!config.enabled()) {
			return;
		}

		// Restart
		this.braiinApi = new BraiinsApi(this.httpBridgeFactory, //
				config.ip(), config.username(), config.password(), this::applyMinerStats);
		this.braiinApi.activate();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.braiinApi != null) {
			this.braiinApi.deactivate();
			this.braiinApi = null;
		}
		super.deactivate();
	}

	private Mode.Actual lastSentMode = null;
	private CompletableFuture<Void> runFuture = null;

	@Override
	public void run() throws OpenemsNamedException {
		var targetMode = this.config.mode().actual;
		if (targetMode == null || targetMode == this.lastSentMode) {
			return; // no changes -> stop
		}

		if (this.runFuture == null) {
			this.runFuture = switch (targetMode) {
			case ON -> this.braiinApi.callActionResume();
			case OFF -> this.braiinApi.callActionPause();
			};
			this.lastSentMode = targetMode;
		}

		if (this.runFuture.isDone()) {
			try {
				this.runFuture.get();

			} catch (InterruptedException | ExecutionException e) {
				// Mode change did not work -> retry
				this.logError(this.log, "Unable to set mode [" + targetMode + "]: " + e.getMessage());
				e.printStackTrace();
				this.lastSentMode = null;
			}
			this.runFuture = null;
		}
	}

	/**
	 * Regularly called by {@link BraiinsApi}.
	 * 
	 * @param ms the {@link MinerStats}
	 */
	private void applyMinerStats(MinerStats ms) {
		setValue(this, ControllerBraiinsSingle.ChannelId.COMMUNICATION_FAILED, ms == null);
		setValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER, ms == null ? null : ms.approximatedConsumption());
		setValue(this, ControllerBraiinsSingle.ChannelId.EFFICIENCY, ms == null ? null : ms.efficiency());
		setValue(this, ControllerBraiinsSingle.ChannelId.REAL_HASHRATE_LAST_15S,
				ms == null ? null : ms.realHashRateLast15s());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
			-> this.calculateEnergy();
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateEnergy.update(activePower);
		} else {
			this.calculateEnergy.update(0);
		}
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append("L:").append(this.getActivePower()) //
				.append("|Efficiency:").append(this.channel(ControllerBraiinsSingle.ChannelId.EFFICIENCY).value()) //
				.append("|Hashrate:")
				.append(this.channel(ControllerBraiinsSingle.ChannelId.REAL_HASHRATE_LAST_15S).value()) //
				.toString();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public SinglePhase getPhase() {
		return this.config.phase();
	}
}
