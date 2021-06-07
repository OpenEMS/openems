package io.openems.edge.goodwe.batteryinverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ApplyPowerContext;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.goodwe.common.AbstractGoodWe;
import io.openems.edge.goodwe.common.ApplyPowerHandler;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
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

	private final Logger log = LoggerFactory.getLogger(GoodWeBatteryInverterImpl.class);

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

	private Config config;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
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
		switch (this.config.batteryRegisters()) {
		case FROM_45350:
			// this.writeToChannel(GoodWe.ChannelId.BMS_LEAD_CAPACITY, //
			// LEAD_BATTERY_CAPACITY);
			// this.writeToChannel(GoodWe.ChannelId.BMS_STRINGS, //
			// TypeUtils.divide(battery.getDischargeMinVoltage().get(),
			// MODULE_MIN_VOLTAGE));
			// this.writeToChannel(GoodWe.ChannelId.BMS_DISCHARGE_MIN_VOLTAGE, //
			// battery.getDischargeMinVoltage().get());
			writeBmsChannels(battery);
			break;

		case FROM_47900:
//			this.writeToChannel(GoodWe.ChannelId.WBMS_SOC, //
//					battery.getSoc().get());
//			this.writeToChannel(GoodWe.ChannelId.WBMS_SOH, //
//					battery.getSoh().get());
//			this.writeToChannel(GoodWe.ChannelId.WBMS_STRINGS, //
//					TypeUtils.divide(battery.getDischargeMinVoltage().get(), MODULE_MIN_VOLTAGE));
//			this.writeToChannel(GoodWe.ChannelId.WBMS_DISCHARGE_MIN_VOLTAGE, //
//					battery.getDischargeMinVoltage().get());
//			this.writeToChannel(GoodWe.ChannelId.WBMS_DISCHARGE_MAX_CURRENT, //
//					TypeUtils.fitWithin(0, MAX_DC_CURRENT, battery.getDischargeMaxCurrent().get()));
//			this.writeToChannel(GoodWe.ChannelId.WBMS_CHARGE_MAX_VOLTAGE, //
//					battery.getChargeMaxVoltage().get());
//			this.writeToChannel(GoodWe.ChannelId.WBMS_CHARGE_MAX_CURRENT, //
//					TypeUtils.fitWithin(0, MAX_DC_CURRENT, battery.getChargeMaxCurrent().get()));
			break;
		case NONE:
		}
	}

	private final Map<GoodWe.ChannelId, Integer> lastWrittenValues = new HashMap<>();

	private static Integer preprocessAmpereValue(Value<Integer> v) {
		Integer value = v.get();
		value = TypeUtils.fitWithin(0, MAX_DC_CURRENT, value);

		if (value != null && value > 0 && value < MAX_DC_CURRENT) {
			// To avoid very frequent updates, round newValue to nearest multiple of 5, but
			// at least 1. This is because each update of BMS values currently causes a
			// downtime of the inverter.
			value = (int) (5 * Math.floor(value / 5.));
			if (value == 0) {
				value = 1;
			}
		}
		return value;
	}

//	private void writeToChannel(GoodWe.ChannelId channelId, Object value)
//			throws IllegalArgumentException, OpenemsNamedException {
//		WriteChannel<?> channel = this.channel(channelId);
//		Object currentValue = channel.value().get();
//		if (value != null && !Objects.equals(currentValue, value)) {
//			this.logInfo(this.log, "Update  " + channelId.id() + " from [" + currentValue + "] to [" + value + "]");
//		} else {
//			this.logInfo(this.log, "Refresh " + channelId.id() + " [" + currentValue + "]");
//		}
//		channel.setNextWriteValueFromObject(value);
//	}

	/**
	 * BMS-Registers need to be written all at once.
	 */
	private void writeBmsChannels(Battery battery) throws OpenemsNamedException {
		// Read battery values
		Integer chargeMaxVoltage = this.getBmsChargeMaxVoltage().orElse(0);
		Integer chargeMaxCurrent = preprocessAmpereValue(battery.getChargeMaxCurrent());
		Integer dischargeMinVoltage = this.getBmsDischargeMinVoltage().orElse(0);
		Integer dischargeMaxCurrent = preprocessAmpereValue(battery.getDischargeMaxCurrent());

		// Replace null values
		TypeUtils.orElse(chargeMaxVoltage, 0);
		TypeUtils.orElse(chargeMaxCurrent, 0);
		TypeUtils.orElse(dischargeMinVoltage, 0);
		TypeUtils.orElse(dischargeMaxCurrent, 0);

		// Is Update required?
		if (Objects.equals(this.lastWrittenValues.get(GoodWe.ChannelId.BMS_CHARGE_MAX_VOLTAGE), chargeMaxVoltage)
				&& Objects.equals(this.lastWrittenValues.get(GoodWe.ChannelId.BMS_CHARGE_MAX_CURRENT), chargeMaxCurrent)
				&& Objects.equals(this.lastWrittenValues.get(GoodWe.ChannelId.BMS_DISCHARGE_MIN_VOLTAGE),
						dischargeMinVoltage)
				&& Objects.equals(this.lastWrittenValues.get(GoodWe.ChannelId.BMS_DISCHARGE_MAX_CURRENT),
						dischargeMaxCurrent)) {
			// No Update required
			this.logInfo(this.log, "No Update Required for Voltages [" + dischargeMinVoltage + ";" + chargeMaxVoltage
					+ "] and Currents [" + chargeMaxCurrent + ";" + dischargeMaxCurrent + "]");

			return;
		}

		// Print log
		this.logInfo(this.log, "Update Voltages [" + dischargeMinVoltage + ";" + chargeMaxVoltage + "] and Currents ["
				+ chargeMaxCurrent + ";" + dischargeMaxCurrent + "]");

		// Write to Channels
		this.setBmsChargeMaxVoltage(chargeMaxVoltage);
		this.setBmsChargeMaxCurrent(chargeMaxCurrent);
		this.setBmsDischargeMinVoltage(dischargeMinVoltage);
		this.setBmsDischargeMaxCurrent(dischargeMaxCurrent);

		// Store lastWrittenValues
		this.lastWrittenValues.put(GoodWe.ChannelId.BMS_CHARGE_MAX_VOLTAGE, chargeMaxVoltage);
		this.lastWrittenValues.put(GoodWe.ChannelId.BMS_CHARGE_MAX_CURRENT, chargeMaxCurrent);
		this.lastWrittenValues.put(GoodWe.ChannelId.BMS_DISCHARGE_MIN_VOLTAGE, dischargeMinVoltage);
		this.lastWrittenValues.put(GoodWe.ChannelId.BMS_DISCHARGE_MAX_CURRENT, dischargeMaxCurrent);
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
		// TODO logic is insufficient
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
	public void run(Battery battery, int setActivePower, int setReactivePower, ApplyPowerContext context)
			throws OpenemsNamedException {
		// Set Battery Limits
		this.setBatteryLimits(battery);

		// Calculate ActivePower and Energy values.
		this.updatePowerAndEnergyChannels();
		this.lastSoc = battery.getSoc();

		// Calculate and store Max-AC-Export and -Import for use in
		// getStaticConstraints()
		Integer maxDcChargePower = /* can be negative for force-discharge */
				TypeUtils.multiply(//
						/* Charge-Max-Current */ this.getBmsChargeMaxCurrent().get(), //
						/* Battery Voltage */ battery.getVoltage().get());
		int pvProduction = TypeUtils.min(0, this.calculatePvProduction());
		this._setMaxAcImport(TypeUtils.multiply(/* negate */ -1, //
				TypeUtils.subtract(maxDcChargePower,
						TypeUtils.min(maxDcChargePower /* avoid negative number for `subtract` */, pvProduction))));
		this._setMaxAcExport(TypeUtils.sum(//
				/* Max DC-Discharge-Power */ TypeUtils.multiply(//
						/* Discharge-Max-Current */ this.getBmsDischargeMaxCurrent().get(), //
						/* Battery Voltage */ battery.getVoltage().get()),
				/* PV Production */ pvProduction));

		if (this.config.emsPowerMode() != EmsPowerMode.UNDEFINED && this.config.emsPowerSet() >= 0) {
			System.out.println("Static " + this.config.emsPowerMode() + "[" + this.config.emsPowerSet() + "]");
			IntegerWriteChannel emsPowerSetChannel = this.channel(GoodWe.ChannelId.EMS_POWER_SET);
			emsPowerSetChannel.setNextWriteValue(this.config.emsPowerSet());
			EnumWriteChannel emsPowerModeChannel = this.channel(GoodWe.ChannelId.EMS_POWER_MODE);
			emsPowerModeChannel.setNextWriteValue(this.config.emsPowerMode());
		} else {
			// Apply Power Set-Point
			ApplyPowerHandler.apply(this, false /* read-only mode is never true */, setActivePower, context);
		}
	}

	@Override
	public BatteryInverterConstraint[] getStaticConstraints() throws OpenemsNamedException {
		if (this.config.emsPowerMode() != EmsPowerMode.UNDEFINED && this.config.emsPowerSet() >= 0) {
			// Manual EMS Settings active
			return new BatteryInverterConstraint[] { //
					new BatteryInverterConstraint("Manual Override", Phase.ALL, Pwr.ACTIVE, //
							Relationship.EQUALS, 0), //
					new BatteryInverterConstraint("Manual Override", Phase.ALL, Pwr.REACTIVE, //
							Relationship.EQUALS, 0), //
			};
		}

		return new BatteryInverterConstraint[] { //
				new BatteryInverterConstraint("Max AC Import", Phase.ALL, Pwr.ACTIVE, //
						Relationship.GREATER_OR_EQUALS, this.getMaxAcImport().orElse(0)), //
				new BatteryInverterConstraint("Max AC Export", Phase.ALL, Pwr.ACTIVE, //
						Relationship.LESS_OR_EQUALS, this.getMaxAcExport().orElse(0)) //
		};
	}

	@Override
	public String debugLog() {
		return "AllowedAC:" + this.getMaxAcImport().asStringWithoutUnit() + ";" + this.getMaxAcExport().asString();
	}
}
