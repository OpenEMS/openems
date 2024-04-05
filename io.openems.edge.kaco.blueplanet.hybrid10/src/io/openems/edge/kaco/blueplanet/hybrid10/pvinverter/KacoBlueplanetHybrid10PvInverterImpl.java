package io.openems.edge.kaco.blueplanet.hybrid10.pvinverter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.kaco.blueplanet.hybrid10.ErrorChannelId;
import io.openems.edge.kaco.blueplanet.hybrid10.core.KacoBlueplanetHybrid10Core;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Kaco.BlueplanetHybrid10.PvInverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class KacoBlueplanetHybrid10PvInverterImpl extends AbstractOpenemsComponent
		implements KacoBlueplanetHybrid10PvInverter, ManagedSymmetricPvInverter, ElectricityMeter, OpenemsComponent,
		TimedataProvider, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(KacoBlueplanetHybrid10PvInverterImpl.class);
	private final SetPvLimitHandler setPvLimitHandler = new SetPvLimitHandler(this,
			ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);
	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected KacoBlueplanetHybrid10Core core;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private Instant lastSuccessfulCommunication = null;

	public KacoBlueplanetHybrid10PvInverterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ErrorChannelId.values(), //
				KacoBlueplanetHybrid10PvInverter.ChannelId.values() //
		);
		this._setMaxApparentPower(KacoBlueplanetHybrid10PvInverter.MAX_APPARENT_POWER);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumReactivePowerFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "core", config.core_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		try {
			// reset limit
			this.setActivePowerLimit(null);
			this.setPvLimitHandler.run();
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			try {
				this.setPvLimitHandler.run();

				this.channel(KacoBlueplanetHybrid10PvInverter.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.channel(KacoBlueplanetHybrid10PvInverter.ChannelId.PV_LIMIT_FAILED).setNextValue(true);
			}
			break;
		}
	}

	private void updateChannels() {
		Integer activePowerL1 = null, activePowerL2 = null, activePowerL3 = null, //
				voltageL1 = null, voltageL2 = null, voltageL3 = null, //
				currentL1 = null, currentL2 = null, currentL3 = null, //
				reactivePowerL1 = null, reactivePowerL2 = null, reactivePowerL3 = null, //
				activePowerLimit = null, frequency = null;

		var bpData = this.core.getBpData();

		/*
		 * Handle Communication Failed State: Pure PV-Inverter is unreachable during the
		 * night because it has no battery supply. Ignore error for 24 hours.
		 */
		if (bpData != null) {
			this.lastSuccessfulCommunication = Instant.now();
			this._setCommunicationFailed(false);
		} else {
			this._setCommunicationFailed(//
					/* if has never been successful */ this.lastSuccessfulCommunication == null //
							/* or if more than 24 hours ago */ || this.lastSuccessfulCommunication
									.isBefore(Instant.now().minus(24, ChronoUnit.HOURS)));
		}

		if (bpData != null) {
			float eplimitPerc = bpData.settings.getEPLimit() / 100;
			activePowerLimit = (int) (KacoBlueplanetHybrid10PvInverter.MAX_APPARENT_POWER * eplimitPerc);

			// Set error channels
			List<String> errors = bpData.status.getErrors();
			Stream.of(ErrorChannelId.values()) //
					.forEach(c -> this.channel(c).setNextValue(errors.contains(c.getErrorCode())));

			activePowerL1 = Math.round(bpData.inverter.getAcPower(0));
			activePowerL2 = Math.round(bpData.inverter.getAcPower(1));
			activePowerL3 = Math.round(bpData.inverter.getAcPower(2));

			voltageL1 = Math.round(bpData.inverter.getAcVoltage(0) * 1000);
			voltageL2 = Math.round(bpData.inverter.getAcVoltage(1) * 1000);
			voltageL3 = Math.round(bpData.inverter.getAcVoltage(2) * 1000);

			currentL1 = Math.round(bpData.inverter.getAcPower(0) / bpData.inverter.getAcVoltage(0) * 1000);
			currentL2 = Math.round(bpData.inverter.getAcPower(1) / bpData.inverter.getAcVoltage(1) * 1000);
			currentL3 = Math.round(bpData.inverter.getAcPower(2) / bpData.inverter.getAcVoltage(2) * 1000);

			reactivePowerL1 = Math.round(bpData.inverter.getReactivPower(0));
			reactivePowerL2 = Math.round(bpData.inverter.getReactivPower(1));
			reactivePowerL3 = Math.round(bpData.inverter.getReactivPower(2));

			frequency = Math.round(bpData.inverter.getGridFrequency() * 1000);
		}

		this._setActivePowerL1(activePowerL1);
		this._setActivePowerL2(activePowerL2);
		this._setActivePowerL3(activePowerL3);

		this._setVoltageL1(voltageL1);
		this._setVoltageL2(voltageL2);
		this._setVoltageL3(voltageL3);

		this._setCurrentL1(currentL1);
		this._setCurrentL2(currentL2);
		this._setCurrentL3(currentL3);

		this._setReactivePowerL1(reactivePowerL1);
		this._setReactivePowerL2(reactivePowerL2);
		this._setReactivePowerL3(reactivePowerL3);

		this._setActivePowerLimit(activePowerLimit);
		this._setFrequency(frequency);

		// Calculate Energy
		this.calculateEnergy.update(this.getActivePower().get());
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
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode));
	}
}
