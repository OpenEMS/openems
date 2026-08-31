package io.openems.edge.sungrow.ess;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;

import java.util.function.Consumer;

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
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Sungrow", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EssSungrowImpl extends AbstractOpenemsModbusComponent implements EssSungrow, SymmetricEss,
		ManagedSymmetricEss, HybridEss, ModbusComponent, TimedataProvider, OpenemsComponent {

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateActiveChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateActiveDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_DISCHARGE_ENERGY);

	private final ApplyPowerHandler applyPowerHandler = new ApplyPowerHandler();

	@Reference
	protected Power power;

	@Reference
	private Sum sum;

	private Config config = null;

	private int heartbeat = 500;

	public EssSungrowImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				EssSungrow.ChannelId.values() //
		);
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		// NOTE: This should normally be read from the device
		this._setGridMode(GridMode.ON_GRID);

		this.installPowerListeners();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Installs listeners to calculate power values from existing values.
	 */
	private void installPowerListeners() {
		this.installDcDischargePowerListener();
		this.installAllowedChargePowerListener();
		this.installAllowedDischargePowerListener();
	}

	/**
	 * Installs a listener calculating the DC discharge power from the active power
	 * and pv power.
	 */
	private void installDcDischargePowerListener() {
		final Consumer<Value<Integer>> dcDischarge = ignore -> {
			this._setDcDischargePower(TypeUtils.subtract(//
					this.getActivePower().get(), this.getTotalDcPower().get()));
		};
		this.getActivePowerChannel().onSetNextValue(dcDischarge);
		this.getTotalDcPowerChannel().onSetNextValue(dcDischarge);
	}

	/**
	 * Installs a listener calculating the allowed charge power from the battery
	 * voltage and the charge max current.
	 */
	private void installAllowedChargePowerListener() {
		final Consumer<Value<Integer>> allowedCharge = ignore -> {
			this._setAllowedChargePower(//
					// set to 0 if either value is undefined
					-this.getBatteryVoltage().orElse(0) * this.getChargeMaxCurrent().orElse(0));
		};
		this.getBatteryVoltageChannel().onSetNextValue(allowedCharge);
		this.getChargeMaxCurrentChannel().onSetNextValue(allowedCharge);
	}

	/**
	 * Installs a listener calculating the allowed discharge power from the battery
	 * voltage and the discharge max current.
	 */
	private void installAllowedDischargePowerListener() {
		final Consumer<Value<Integer>> allowedDischarge = ignore -> {
			this._setAllowedDischargePower(//
					// set to 0 if either value is undefined
					this.getBatteryVoltage().orElse(0) * this.getDischargeMaxCurrent().orElse(0));
		};
		this.getBatteryVoltageChannel().onSetNextValue(allowedDischarge);
		this.getDischargeMaxCurrentChannel().onSetNextValue(allowedDischarge);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(4989, Priority.HIGH, //
						m(EssSungrow.ChannelId.SERIAL_NUMBER, new StringWordElement(4989, 10)), //
						new DummyRegisterElement(4999), // Device type code
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER, new UnsignedWordElement(5000), //
								SCALE_FACTOR_2), //
						new DummyRegisterElement(5001), // Output type
						m(EssSungrow.ChannelId.DAILY_OUTPUT_ENERGY, new UnsignedWordElement(5002), //
								SCALE_FACTOR_2), //
						new DummyRegisterElement(5003, 5006), //
						m(EssSungrow.ChannelId.INSIDE_TEMPERATURE, new SignedWordElement(5007)), //
						new DummyRegisterElement(5008, 5009), //
						m(EssSungrow.ChannelId.MPPT1_VOLTAGE, new UnsignedWordElement(5010), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.MPPT1_CURRENT, new UnsignedWordElement(5011), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.MPPT2_VOLTAGE, new UnsignedWordElement(5012), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.MPPT2_CURRENT, new UnsignedWordElement(5013), //
								SCALE_FACTOR_2), //
						new DummyRegisterElement(5014, 5015), //
						m(EssSungrow.ChannelId.TOTAL_DC_POWER, //
								new UnsignedDoublewordElement(5016).wordOrder(WordOrder.LSWMSW)), //
						m(EssSungrow.ChannelId.VOLTAGE_L1, new UnsignedWordElement(5018), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.VOLTAGE_L2, new UnsignedWordElement(5019), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.VOLTAGE_L3, new UnsignedWordElement(5020), //
								SCALE_FACTOR_2), //
						new DummyRegisterElement(5021, 5031), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, //
								new SignedDoublewordElement(5032).wordOrder(WordOrder.LSWMSW)), //
						m(EssSungrow.ChannelId.POWER_FACTOR, new SignedWordElement(5034)), //
						m(EssSungrow.ChannelId.GRID_FREQUENCY, new UnsignedWordElement(5035), //
								SCALE_FACTOR_1)), //

				new FC4ReadInputRegistersTask(5621, Priority.LOW, //
						m(EssSungrow.ChannelId.EXPORT_LIMIT_MIN, new UnsignedWordElement(5621), //
								SCALE_FACTOR_1), //
						m(EssSungrow.ChannelId.EXPORT_LIMIT_MAX, new UnsignedWordElement(5622), //
								SCALE_FACTOR_1), //
						new DummyRegisterElement(5623, 5626), //
						m(EssSungrow.ChannelId.BDC_RATED_POWER, new UnsignedWordElement(5627), //
								SCALE_FACTOR_2), //
						new DummyRegisterElement(5628, 5633), //
						m(EssSungrow.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(5634)), //
						m(EssSungrow.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(5635)) //
				), //

				new FC4ReadInputRegistersTask(12999, Priority.HIGH, //
						m(EssSungrow.ChannelId.SYSTEM_STATE, new UnsignedWordElement(12999)), //
						m(new BitsWordElement(13000, this) //
								.bit(0, EssSungrow.ChannelId.POWER_GENERATED_FROM_PV) //
								.bit(1, EssSungrow.ChannelId.BATTERY_CHARGING) //
								.bit(2, EssSungrow.ChannelId.BATTERY_DISCHARGING) //
								.bit(3, EssSungrow.ChannelId.POSITIVE_LOAD_POWER) //
								.bit(4, EssSungrow.ChannelId.FEED_IN_POWER) //
								.bit(5, EssSungrow.ChannelId.IMPORT_POWER_FROM_GRID) //
								.bit(7, EssSungrow.ChannelId.NEGATIVE_LOAD_POWER) //
						), //
						m(EssSungrow.ChannelId.DAILY_PV_GENERATION, new UnsignedWordElement(13001), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.TOTAL_PV_GENERATION, //
								new UnsignedDoublewordElement(13002).wordOrder(WordOrder.LSWMSW), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.DAILY_EXPORT_POWER_FROM_PV, new UnsignedWordElement(13004), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.TOTAL_EXPORT_ENERGY_FROM_PV, //
								new UnsignedDoublewordElement(13005).wordOrder(WordOrder.LSWMSW), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.LOAD_POWER,
								new SignedDoublewordElement(13007).wordOrder(WordOrder.LSWMSW)), //
						m(EssSungrow.ChannelId.EXPORT_POWER, //
								new SignedDoublewordElement(13009).wordOrder(WordOrder.LSWMSW)), //
						m(EssSungrow.ChannelId.DAILY_BATTERY_CHARGE_ENERGY_FROM_PV, new UnsignedWordElement(13011), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.TOTAL_BATTERY_CHARGE_ENERGY_FROM_PV, //
								new UnsignedDoublewordElement(13012).wordOrder(WordOrder.LSWMSW), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.CO2_REDUCTION, //
								new UnsignedDoublewordElement(13014).wordOrder(WordOrder.LSWMSW), //
								SCALE_FACTOR_MINUS_1), //
						m(EssSungrow.ChannelId.DAILY_DIRECT_ENERGY_CONSUMPTION, new UnsignedWordElement(13016), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.TOTAL_DIRECT_ENERGY_CONSUMPTION, //
								new UnsignedDoublewordElement(13017).wordOrder(WordOrder.LSWMSW), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(13019), //
								SCALE_FACTOR_MINUS_1), //
						m(EssSungrow.ChannelId.BATTERY_CURRENT, new UnsignedWordElement(13020), //
								SCALE_FACTOR_MINUS_1), //
						m(EssSungrow.ChannelId.BATTERY_POWER, new UnsignedWordElement(13021)), //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(13022), //
								SCALE_FACTOR_MINUS_1), //
						m(EssSungrow.ChannelId.SOH, new UnsignedWordElement(13023), //
								SCALE_FACTOR_MINUS_1), //
						m(EssSungrow.ChannelId.BATTERY_TEMPERATURE, new SignedWordElement(13024)), //
						m(EssSungrow.ChannelId.DAILY_BATTERY_DISCHARGE_ENERGY, new UnsignedWordElement(13025), //
								SCALE_FACTOR_2), //
						new DummyRegisterElement(13026, 13027), //
						m(EssSungrow.ChannelId.SELF_CONSUMPTION_OF_TODAY, new UnsignedWordElement(13028), //
								SCALE_FACTOR_MINUS_1), //
						// NOTE: Grid mode is not read properly
						new DummyRegisterElement(13029),
						m(EssSungrow.ChannelId.CURRENT_L1, new SignedWordElement(13030), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.CURRENT_L2, new SignedWordElement(13031), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.CURRENT_L3, new SignedWordElement(13032), //
								SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(13033))
								.wordOrder(WordOrder.LSWMSW), //
						m(EssSungrow.ChannelId.DAILY_IMPORT_ENERGY, new UnsignedWordElement(13035), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.TOTAL_IMPORT_ENERGY,
								new UnsignedDoublewordElement(13036).wordOrder(WordOrder.LSWMSW), //
								SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.CAPACITY, new UnsignedWordElement(13038), //
								SCALE_FACTOR_1), //
						m(EssSungrow.ChannelId.DAILY_CHARGE_ENERGY, new UnsignedWordElement(13039), //
								SCALE_FACTOR_2), //
						new DummyRegisterElement(13040, 13043), //
						m(EssSungrow.ChannelId.DAILY_EXPORT_ENERGY, new UnsignedWordElement(13044), //
								SCALE_FACTOR_2), //
						m(EssSungrow.ChannelId.TOTAL_EXPORT_ENERGY,
								new UnsignedDoublewordElement(13045).wordOrder(WordOrder.LSWMSW), //
								SCALE_FACTOR_2) //
				), //

				new FC3ReadRegistersTask(13049, Priority.HIGH, //
						m(EssSungrow.ChannelId.EMS_MODE, new UnsignedWordElement(13049)), //
						m(EssSungrow.ChannelId.CHARGE_DISCHARGE_COMMAND, new UnsignedWordElement(13050)), //
						m(EssSungrow.ChannelId.CHARGE_DISCHARGE_POWER, new UnsignedWordElement(13051)), //
						new DummyRegisterElement(13052, 13056), //
						m(EssSungrow.ChannelId.MAX_SOC, new UnsignedWordElement(13057), //
								SCALE_FACTOR_MINUS_1), //
						m(EssSungrow.ChannelId.MIN_SOC, new UnsignedWordElement(13058), //
								SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(13059, 13078), //
						m(EssSungrow.ChannelId.HEARTBEAT, new UnsignedWordElement(13079)), //
						new DummyRegisterElement(13080, 13084), //
						m(EssSungrow.ChannelId.METER_COMM_DETECTION, new UnsignedWordElement(13085)), //
						m(EssSungrow.ChannelId.EXPORT_POWER_LIMITATION, new UnsignedWordElement(13086)), //
						new DummyRegisterElement(13087, 13098), //
						m(EssSungrow.ChannelId.RESERVED_SOC_FOR_BACKUP, new UnsignedWordElement(13099)) //
				), //

				new FC16WriteRegistersTask(13049, //
						m(EssSungrow.ChannelId.EMS_MODE, new UnsignedWordElement(13049)), //
						m(EssSungrow.ChannelId.CHARGE_DISCHARGE_COMMAND, new UnsignedWordElement(13050)), //
						m(EssSungrow.ChannelId.CHARGE_DISCHARGE_POWER, new UnsignedWordElement(13051)) //
				), //
				new FC6WriteRegisterTask(13079, //
						m(EssSungrow.ChannelId.HEARTBEAT, new UnsignedWordElement(13079)) //
				) //

		);
	}

	@Override
	public Integer getSurplusPower() {
		var productionPower = this.getTotalDcPower().orElse(0);
		if (productionPower < 100) {
			return null;
		}
		// "+" because the allowed charge power is negative
		var surplusPower = productionPower + this.getAllowedChargePower().orElse(0);
		if (surplusPower < 0) {
			return null;
		}
		return surplusPower;
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {

		this.calculateEnergy();
		this.updateHeartbeat();
		this.applyPowerHandler.apply(this, activePower, this.config.controlMode(), this.sum.getGridActivePower());
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// AC
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateActiveChargeEnergy.update(null);
			this.calculateActiveDischargeEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Inverter
			this.calculateActiveChargeEnergy.update(0);
			this.calculateActiveDischargeEnergy.update(activePower);
		} else {
			// Sell-To-Inverter
			this.calculateActiveChargeEnergy.update(activePower * -1);
			this.calculateActiveDischargeEnergy.update(0);
		}
		// DC
		var dcPower = this.getDcDischargePower().get();
		if (dcPower == null) {
			// Not available
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		} else if (dcPower > 0) {
			// Discharging battery
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(dcPower);
		} else {
			// Charging battery
			this.calculateDcChargeEnergy.update(dcPower * -1);
			this.calculateDcDischargeEnergy.update(0);
		}
	}

	/**
	 * Toggles the heartbeat value between 500 and 600 and sets a debug channel.
	 * 
	 * @throws OpenemsNamedException on write error
	 */
	private void updateHeartbeat() throws OpenemsNamedException {
		if (this.heartbeat == 500) {
			this.heartbeat = 600;
		} else {
			this.heartbeat = 500;
		}
		this.getHeartbeatChannel().setNextWriteValue(this.heartbeat);
		this.channel(EssSungrow.ChannelId.DEBUG_HEARTBEAT).setNextValue(this.heartbeat);
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append("SoC:").append(this.getSoc()) //
				.append("|Active Power:").append(this.getActivePower().toString()) //
				.append("|DC Discharge Power:").append(this.getDcDischargePower().toString()).toString();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}
