package io.openems.edge.simulator.meter.grid.acting;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.GridMeter.Acting", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class SimulatorGridMeterActingImpl extends AbstractOpenemsComponent
		implements SimulatorGridMeterActing, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final Logger log = LoggerFactory.getLogger(SimulatorGridMeterActingImpl.class);
	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private StepResponseHandler stepResponseHandler;
	private Config config = null;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SimulatorDatasource datasource;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private volatile List<ManagedSymmetricEss> symmetricEsss = new CopyOnWriteArrayList<>();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public SimulatorGridMeterActingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SimulatorGridMeterActing.ChannelId.values() //
		);

	}

	@Activate
	private void activate(ComponentContext context, Config config) throws IOException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "datasource", config.datasource_id())) {
			return;
		}

		if (this.config.needFrequencyStepResponse()) {
			Instant startTime = this.convertTime(this.config.startTime());
			this.stepResponseHandler = new StepResponseHandler(this, startTime);
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			if (this.config.needFrequencyStepResponse()) {
				this.stepResponseHandler.doStepResponse();
			}
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			break;
		}
	}

	private void updateChannels() {
		/*
		 * get and store Simulated Active Power
		 */
		Integer simulatedActivePower = this.datasource.getValue(OpenemsType.INTEGER,
				new ChannelAddress(this.id(), "ActivePower"));
		this.channel(SimulatorGridMeterActing.ChannelId.SIMULATED_ACTIVE_POWER).setNextValue(simulatedActivePower);

		/*
		 * Calculate Active Power
		 */
		var activePower = simulatedActivePower;
		for (ManagedSymmetricEss ess : this.symmetricEsss) {
			if (ess instanceof MetaEss) {
				// ignore this Ess
				continue;
			}
			activePower = TypeUtils.subtract(activePower, ess.getActivePower().get());
		}

		this._setActivePower(activePower);
		var activePowerByThree = TypeUtils.divide(activePower, 3);
		this._setActivePowerL1(activePowerByThree);
		this._setActivePowerL2(activePowerByThree);
		this._setActivePowerL3(activePowerByThree);
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString();
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Grid
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			// Sell-To-Grid
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower * -1);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	/**
	 * Converts a string representation of time to an Instant object.
	 * <p>
	 * If the input time is null or empty, the current time is returned. If the
	 * input time is successfully parsed, it is converted to an Instant object. If
	 * the parsed time is in the past, and the current time is returned. If there's
	 * an error parsing the input time, and the current time is returned.
	 * </p>
	 *
	 * @param inputTime the string representation of time to be converted
	 * @return an Instant converted time
	 */
	public Instant convertTime(String inputTime) {

		Instant currentTime = this.getCurrentTime();
		if (inputTime == null || inputTime.isEmpty()) {
			return currentTime;
		}
		try {
			LocalDateTime localDateTime = LocalDateTime.parse(inputTime,
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			Instant futureTime = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
			return currentTime.isAfter(futureTime) ? currentTime : futureTime;
		} catch (DateTimeParseException e) {
			this.log.error(
					"Error parsing input time: " + inputTime + " instead current time: " + currentTime + " is taken.");
			return currentTime;
		}
	}

	/**
	 * Retrieves the current time component manager.
	 *
	 * @return An Instant representing the current time.
	 */
	public Instant getCurrentTime() {
		var currentTime = this.componentManager.getClock().withZone(ZoneId.systemDefault());
		return Instant.now(currentTime);
	}

}
