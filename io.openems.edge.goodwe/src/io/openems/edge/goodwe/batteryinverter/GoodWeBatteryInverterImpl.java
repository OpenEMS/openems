package io.openems.edge.goodwe.batteryinverter;

import java.util.Objects;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.goodwe.common.AbstractGoodWe;
import io.openems.edge.goodwe.common.ApplyPowerHandler;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.BatteryInverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
public class GoodWeBatteryInverterImpl extends AbstractGoodWe
		implements GoodWeBatteryInverter, GoodWe, HybridManagedSymmetricBatteryInverter,
		ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, OpenemsComponent {

	private static final int MAX_DC_CURRENT = 25; // [A]

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	// For Fenecon Home Battery, Lead Battery Capacity has to be set as a battery
	// parameter
	// TODO get from Battery
	private static final int LEAD_BATTERY_CAPACITY = 200;
	// Fenecon Home Battery Static module min voltage, used to calculate battery
	// module number per tower
	// TODO get from Battery
	private static final int MODULE_MIN_VOLTAGE = 42;

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
				SymmetricBatteryInverter.ChannelId.ACTIVE_POWER, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_POWER, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_CHARGE_ENERGY, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_ENERGY, //
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

	/**
	 * Sets the Battery Limits.
	 * 
	 * @param battery the linked {@link Battery}
	 * @throws OpenemsNamedException on error
	 */
	private void setBatteryLimits(Battery battery) throws OpenemsNamedException {
		// Battery String
		IntegerWriteChannel bmsBatteryString = this.channel(GoodWe.ChannelId.BATT_STRINGS);
		if (!Objects.equals(bmsBatteryString.value().orElse(0),
				(battery.getDischargeMinVoltage().orElse(0) / MODULE_MIN_VOLTAGE))) {
			bmsBatteryString.setNextWriteValue(battery.getDischargeMinVoltage().orElse(0) / MODULE_MIN_VOLTAGE);
		}

		IntegerWriteChannel bmsLeadBatCapacity = this.channel(GoodWe.ChannelId.LEAD_BAT_CAPACITY);
		if (!Objects.equals(bmsLeadBatCapacity.value().orElse(0), LEAD_BATTERY_CAPACITY)) {
			bmsLeadBatCapacity.setNextWriteValue(LEAD_BATTERY_CAPACITY);
		}

		IntegerWriteChannel bmsVoltUnderMin = this.channel(GoodWe.ChannelId.BATT_VOLT_UNDER_MIN);
		if (!Objects.equals(bmsVoltUnderMin.value().get(), battery.getDischargeMinVoltage().get())) {
			bmsVoltUnderMin.setNextWriteValueFromObject(battery.getDischargeMinVoltage());
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
	public Integer getDcPvPower() {
		return this.calculatePvProduction();
	}

	@Override
	public Integer getSurplusPower() {
		if (this.lastSoc == null || this.lastSoc.orElse(0) < 99) {
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
		// Set Battery Limits
		this.setBatteryLimits(battery);

		// Calculate ActivePower and Energy values.
		this.updatePowerAndEnergyChannels();
		this.lastSoc = battery.getSoc();

		// Calculate and store Max-AC-Export and -Import for use in
		// getStaticConstraints()
		Integer maxDcChargePower = /* can be negative for force-discharge */
				TypeUtils.multiply(//
						/* Charge-Max-Current; max 25 A */ TypeUtils.min(battery.getChargeMaxCurrent().get(),
								MAX_DC_CURRENT), //
						/* Battery Voltage */ battery.getVoltage().get());
		Integer pvProduction = TypeUtils.min(0, this.calculatePvProduction());
		this._setMaxAcImport(TypeUtils.multiply(/* negate */ -1, //
				TypeUtils.subtract(maxDcChargePower,
						TypeUtils.min(maxDcChargePower /* avoid negative number for `subtract` */, pvProduction))));
		this._setMaxAcExport(TypeUtils.sum(//
				/* Max DC-Discharge-Power */ TypeUtils.multiply(//
						/* Charge-Max-Current; max 25 A */ TypeUtils.min(battery.getDischargeMaxCurrent().get(),
								MAX_DC_CURRENT), //
						/* Battery Voltage */ battery.getVoltage().get()),
				/* PV Production */ pvProduction));

		// Apply Power Set-Point
		ApplyPowerHandler.apply(this, false /* read-only mode is never true */, setActivePower);
	}

	@Override
	public BatteryInverterConstraint[] getStaticConstraints() throws OpenemsNamedException {
		return new BatteryInverterConstraint[] { //
				new BatteryInverterConstraint("Max AC Import", Phase.ALL, Pwr.ACTIVE, //
						Relationship.GREATER_OR_EQUALS, this.getMaxAcImport().orElse(0)), //
				new BatteryInverterConstraint("Max AC Export", Phase.ALL, Pwr.ACTIVE, //
						Relationship.LESS_OR_EQUALS, this.getMaxAcExport().orElse(0)) //
		};
	}
}
