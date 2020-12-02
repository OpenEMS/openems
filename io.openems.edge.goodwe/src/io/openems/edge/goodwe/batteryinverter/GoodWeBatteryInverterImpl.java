package io.openems.edge.goodwe.batteryinverter;

import java.util.Optional;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.goodwe.common.AbstractGoodWe;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.applypower.ApplyPowerStateMachine;
import io.openems.edge.goodwe.common.applypower.Context;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.BatteryInverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
public class GoodWeBatteryInverterImpl extends AbstractGoodWe
		implements GoodWeBatteryInverter, GoodWe, HybridManagedSymmetricBatteryInverter,
		ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, OpenemsComponent, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(GoodWeBatteryInverterImpl.class);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateAcDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	private final ApplyPowerStateMachine applyPowerStateMachine = new ApplyPowerStateMachine(
			ApplyPowerStateMachine.State.UNDEFINED);

	/**
	 * Holds the latest known SoC. Updated in {@link #run(Battery, int, int)}.
	 */
	private Value<Integer> lastSoc = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public GoodWeBatteryInverterImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				HybridManagedSymmetricBatteryInverter.ChannelId.values(), //
				GoodWe.ChannelId.values(), //
				GoodWeBatteryInverter.ChannelId.values() //
		);
		// GoodWe is always started
		this._setStartStop(StartStop.START);
	}

	private void updatechannels() {
		/*
		 * Update ActivePower from P_BATTERY1 and chargers ACTUAL_POWER
		 */
		Integer productionPower = this.calculatePvProduction();
		final Channel<Integer> batteryPower = this.channel(GoodWe.ChannelId.P_BATTERY1);
		Integer activePower = TypeUtils.sum(productionPower, batteryPower.value().get());
		this._setActivePower(activePower);

		/*
		 * Calculate AC Energy
		 */
		if (activePower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			this.calculateAcDischargeEnergy.update(null);
		} else if (activePower > 0) {
			// Discharge
			this.calculateAcChargeEnergy.update(0);
			this.calculateAcDischargeEnergy.update(activePower);
		} else {
			// Charge
			this.calculateAcChargeEnergy.update(activePower * -1);
			this.calculateAcDischargeEnergy.update(0);
		}
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public Integer getSurplusPower() {
		if (this.lastSoc.orElse(0) < 99) {
			return null;
		}
		Integer productionPower = this.calculatePvProduction();
		if (productionPower == null || productionPower < 100) {
			return null;
		}
		return productionPower;
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// GoodWe is always started. This has no effect.
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {
		// Calculate ActivePower and Energy values.
		this.updatechannels();
		this.lastSoc = battery.getSoc();

		// Update ApplyPowerStateMachine
		Integer pvProduction = this.calculatePvProduction();
		int soc = battery.getSoc().orElse(0);
		ApplyPowerStateMachine.State state = ApplyPowerStateMachine.evaluateState(this.getGoodweType(),
				false /* read-only mode is never true */, pvProduction, soc, setActivePower);

		// Store the current State
		this.channel(GoodWe.ChannelId.APPLY_POWER_STATE_MACHINE).setNextValue(state);

		// Prepare Context
		Context context = new Context(this, Optional.ofNullable(pvProduction).orElse(0), setActivePower);

		// Call the StateMachine
		try {

			this.applyPowerStateMachine.forceNextState(state);
			this.applyPowerStateMachine.run(context); // apply the force next state
			this.applyPowerStateMachine.run(context); // execute correct handler
			IntegerWriteChannel emsPowerSetChannel = this.channel(GoodWe.ChannelId.EMS_POWER_SET);
			emsPowerSetChannel.setNextWriteValue(context.getEssPowerSet());

			EnumWriteChannel emsPowerModeChannel = this.channel(GoodWe.ChannelId.EMS_POWER_MODE);
			emsPowerModeChannel.setNextWriteValue(context.getNextPowerMode());

			this.channel(GoodWeBatteryInverter.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(GoodWeBatteryInverter.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public String debugLog() {
		return this.applyPowerStateMachine.getCurrentState().asCamelCase();
	}

}
