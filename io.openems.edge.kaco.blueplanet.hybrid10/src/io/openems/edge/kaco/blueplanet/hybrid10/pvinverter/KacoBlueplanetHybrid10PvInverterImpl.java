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
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.kaco.blueplanet.hybrid10.ErrorChannelId;
import io.openems.edge.kaco.blueplanet.hybrid10.core.KacoBlueplanetHybrid10Core;
import io.openems.edge.meter.api.SymmetricMeter;
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
		implements KacoBlueplanetHybrid10PvInverter, ManagedSymmetricPvInverter, SymmetricMeter, OpenemsComponent,
		TimedataProvider, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(KacoBlueplanetHybrid10PvInverterImpl.class);
	private final SetPvLimitHandler setPvLimitHandler = new SetPvLimitHandler(this,
			ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);
	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected KacoBlueplanetHybrid10Core core;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Power power;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private Instant lastSuccessfulCommunication = null;

	public KacoBlueplanetHybrid10PvInverterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ErrorChannelId.values(), //
				KacoBlueplanetHybrid10PvInverter.ChannelId.values() //
		);
		this._setMaxApparentPower(KacoBlueplanetHybrid10PvInverter.MAX_APPARENT_POWER);
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
		Integer activePower = null;
		Integer reactivePower = null;
		Integer activePowerLimit = null;

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

			activePower = Math.round(bpData.inverter.getPvPower());
			reactivePower = 0;
		}

		this._setActivePower(activePower);
		this._setReactivePower(reactivePower);
		this._setActivePowerLimit(activePowerLimit);

		// Calculate Energy
		this.calculateEnergy.update(activePower);
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
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(KacoBlueplanetHybrid10PvInverter.class, accessMode, 100) //
						.build());
	}
}
