package io.openems.edge.ess.sungrow;

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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
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
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.sungrow.enums.ChargeDischargeCommand;
import io.openems.edge.ess.sungrow.enums.EmsMode;

@Designate(ocd = Config.class, factory = true)
@Component(//
	name = "Ess.Sungrow", //
	immediate = true, //
	configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SungrowEssImpl extends AbstractOpenemsModbusComponent implements SungrowEss, SymmetricEss,
	ManagedSymmetricEss, HybridEss, StartStoppable, ModbusComponent, OpenemsComponent {

    private static final ElementToChannelConverter GRID_MODE_CONVERTER = new ElementToChannelConverter(value -> {
	if (value.equals(0x55)) {
	    return GridMode.ON_GRID;
	}
	if (value.equals(0xAA)) {
	    return GridMode.OFF_GRID;
	}
	return GridMode.UNDEFINED;
    });

    private static final ElementToChannelConverter START_STOP_CONVERTER = new ElementToChannelConverter(//
	    value -> {
		if (value.equals(0xCF)) {
		    return StartStop.START;
		}
		if (value.equals(0xCE)) {
		    return StartStop.STOP;
		}
		return StartStop.UNDEFINED;
	    }, //
	    value -> {
		if (value.equals(StartStop.START)) {
		    return 0xCF;
		}
		if (value.equals(StartStop.STOP)) {
		    return 0xCE;
		}
		return null;
	    });

    @Reference
    private Power power;

    private Config config = null;

	private int heartbeat = 500;

    public SungrowEssImpl() {
	super(//
		OpenemsComponent.ChannelId.values(), //
		ModbusComponent.ChannelId.values(), //
		SymmetricEss.ChannelId.values(), //
		ManagedSymmetricEss.ChannelId.values(), //
		HybridEss.ChannelId.values(), //
		StartStoppable.ChannelId.values(), //
		SungrowEss.ChannelId.values() //
	);
    }

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
	super.setModbus(modbus);
    }

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		//TODO These values shouldn't be hard coded, maybe derived from nominal output power
		this._setAllowedChargePower(-8000);
		this._setAllowedDischargePower(8000);
		this._setMaxApparentPower(8000);
		//NOTE: This should normally be read from the device
		this._setGridMode(GridMode.ON_GRID);
		
		this.installPowerListener();
		this.installEnergyListener();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	private void installPowerListener() {
		this.getBatteryPowerChannel().onUpdate(value -> {
			if (!value.isDefined()) {
				return;
			}
			var power = value.get();
			if (this.isCharging()) {
				this._setDcDischargePower(-power);
			} else {
				this._setDcDischargePower(power);
			}
		});
	}
	
	private void installEnergyListener() {
		final Consumer<Value<Long>> acChargeEnergy = (value) -> {
			var dcChrg = this.getDcChargeEnergy();
			var pvChrg = this.getTotalBatteryChargeEnergyFromPv();
			if (dcChrg.isDefined() && pvChrg.isDefined()) {
				var dcChargeEnergy = dcChrg.get();
				var pvChargeEnergy = pvChrg.get();
				this._setActiveChargeEnergy(dcChargeEnergy - pvChargeEnergy);
			}
		};
		this.getDcChargeEnergyChannel().onUpdate(acChargeEnergy);
		this.getTotalBatteryChargeEnergyFromPvChannel().onUpdate(acChargeEnergy);
	}
	


    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

	return new ModbusProtocol(this, //
		new FC4ReadInputRegistersTask(4989, Priority.HIGH, //
			m(SungrowEss.ChannelId.SERIAL_NUMBER, new StringWordElement(4989, 10)), //
			new DummyRegisterElement(4999), // Device type code
			m(SungrowEss.ChannelId.NOMINAL_OUTPUT_POWER, new UnsignedWordElement(5000), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			new DummyRegisterElement(5001), // Output type
			m(SungrowEss.ChannelId.DAILY_OUTPUT_ENERGY, new UnsignedWordElement(5002), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
				new UnsignedDoublewordElement(5003).wordOrder(WordOrder.LSWMSW), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			new DummyRegisterElement(5005, 5006), //
			m(SungrowEss.ChannelId.INSIDE_TEMPERATURE, new SignedWordElement(5007)), //
			new DummyRegisterElement(5008, 5009), //
			m(SungrowEss.ChannelId.MPPT1_VOLTAGE, new UnsignedWordElement(5010), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			m(SungrowEss.ChannelId.MPPT1_CURRENT, new UnsignedWordElement(5011), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			m(SungrowEss.ChannelId.MPPT2_VOLTAGE, new UnsignedWordElement(5012), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			m(SungrowEss.ChannelId.MPPT2_CURRENT, new UnsignedWordElement(5013), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			new DummyRegisterElement(5014, 5015), //
			m(SungrowEss.ChannelId.TOTAL_DC_POWER, //
				new UnsignedDoublewordElement(5016).wordOrder(WordOrder.LSWMSW)), //
			m(SungrowEss.ChannelId.VOLTAGE_L1, new UnsignedWordElement(5018), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.VOLTAGE_L2, new UnsignedWordElement(5019), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.VOLTAGE_L3, new UnsignedWordElement(5020), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			new DummyRegisterElement(5021, 5031), //
			m(SymmetricEss.ChannelId.REACTIVE_POWER, //
				new SignedDoublewordElement(5032).wordOrder(WordOrder.LSWMSW)), //
			m(SungrowEss.ChannelId.POWER_FACTOR, new SignedWordElement(5034)), //
			m(SungrowEss.ChannelId.GRID_FREQUENCY, new UnsignedWordElement(5035), //
				ElementToChannelConverter.SCALE_FACTOR_2)), //

		new FC4ReadInputRegistersTask(5621, Priority.LOW, //
			m(SungrowEss.ChannelId.EXPORT_LIMIT_MIN, new UnsignedWordElement(5621), //
				ElementToChannelConverter.SCALE_FACTOR_1), //
			m(SungrowEss.ChannelId.EXPORT_LIMIT_MAX, new UnsignedWordElement(5622), //
				ElementToChannelConverter.SCALE_FACTOR_1), //
			new DummyRegisterElement(5623, 5626), //
			m(SungrowEss.ChannelId.BDC_RATED_POWER, new UnsignedWordElement(5627), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			new DummyRegisterElement(5628, 5633), //
			m(SungrowEss.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(5634)), //
			m(SungrowEss.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(5635)) //
		), //

		new FC4ReadInputRegistersTask(12999, Priority.HIGH, //
			m(SungrowEss.ChannelId.SYSTEM_STATE, new UnsignedWordElement(12999)), //
			m(new BitsWordElement(13000, this) //
				.bit(0, SungrowEss.ChannelId.POWER_GENERATED_FROM_PV) //
				.bit(1, SungrowEss.ChannelId.BATTERY_CHARGING) //
				.bit(2, SungrowEss.ChannelId.BATTERY_DISCHARGING) //
				.bit(3, SungrowEss.ChannelId.POSITIVE_LOAD_POWER) //
				.bit(4, SungrowEss.ChannelId.FEED_IN_POWER) //
				.bit(5, SungrowEss.ChannelId.IMPORT_POWER_FROM_GRID) //
				.bit(7, SungrowEss.ChannelId.NEGATIVE_LOAD_POWER) //
			), //
			m(SungrowEss.ChannelId.DAILY_PV_GENERATION, new UnsignedWordElement(13001), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.TOTAL_PV_GENERATION, //
				new UnsignedDoublewordElement(13002).wordOrder(WordOrder.LSWMSW), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.DAILY_EXPORT_POWER_FROM_PV, new UnsignedWordElement(13004), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.TOTAL_EXPORT_ENERGY_FROM_PV, //
				new UnsignedDoublewordElement(13005).wordOrder(WordOrder.LSWMSW), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.LOAD_POWER,
				new SignedDoublewordElement(13007).wordOrder(WordOrder.LSWMSW)), //
			m(SungrowEss.ChannelId.EXPORT_POWER, //
				new SignedDoublewordElement(13009).wordOrder(WordOrder.LSWMSW)), //
			m(SungrowEss.ChannelId.DAILY_BATTERY_CHARGE_ENERGY_FROM_PV, new UnsignedWordElement(13011), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.TOTAL_BATTERY_CHARGE_ENERGY_FROM_PV, //
				new UnsignedDoublewordElement(13012).wordOrder(WordOrder.LSWMSW), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.CO2_REDUCTION, //
				new UnsignedDoublewordElement(13014).wordOrder(WordOrder.LSWMSW), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			m(SungrowEss.ChannelId.DAILY_DIRECT_ENERGY_CONSUMPTION, new UnsignedWordElement(13016), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.TOTAL_DIRECT_ENERGY_CONSUMPTION, //
				new UnsignedDoublewordElement(13017).wordOrder(WordOrder.LSWMSW), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(13019), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			m(SungrowEss.ChannelId.BATTERY_CURRENT, new UnsignedWordElement(13020), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SungrowEss.ChannelId.BATTERY_POWER, new UnsignedWordElement(13021)), //
			m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(13022), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			m(SungrowEss.ChannelId.SOH, new UnsignedWordElement(13023), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			m(SungrowEss.ChannelId.BATTERY_TEMPERATURE, new SignedWordElement(13024)), //
			m(SungrowEss.ChannelId.DAILY_BATTERY_DISCHARGE_ENERGY, new UnsignedWordElement(13025), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(HybridEss.ChannelId.DC_DISCHARGE_ENERGY, //
				new UnsignedDoublewordElement(13026).wordOrder(WordOrder.LSWMSW), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.SELF_CONSUMPTION_OF_TODAY, new UnsignedWordElement(13028), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(13029), //
				GRID_MODE_CONVERTER), //
			m(SungrowEss.ChannelId.CURRENT_L1, new SignedWordElement(13030), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.CURRENT_L2, new SignedWordElement(13031), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.CURRENT_L3, new SignedWordElement(13032), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(13033))
				.wordOrder(WordOrder.LSWMSW), //
			m(SungrowEss.ChannelId.DAILY_IMPORT_ENERGY, new UnsignedWordElement(13035), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.TOTAL_IMPORT_ENERGY,
				new UnsignedDoublewordElement(13036).wordOrder(WordOrder.LSWMSW), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SymmetricEss.ChannelId.CAPACITY, new UnsignedWordElement(13038), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.DAILY_CHARGE_ENERGY, new UnsignedWordElement(13039), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, //
				new UnsignedDoublewordElement(13040).wordOrder(WordOrder.LSWMSW), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			new DummyRegisterElement(13042, 13043), //
			m(SungrowEss.ChannelId.DAILY_EXPORT_ENERGY, new UnsignedWordElement(13044), //
				ElementToChannelConverter.SCALE_FACTOR_2), //
			m(SungrowEss.ChannelId.TOTAL_EXPORT_ENERGY,
				new UnsignedDoublewordElement(13045).wordOrder(WordOrder.LSWMSW), //
				ElementToChannelConverter.SCALE_FACTOR_2) //
		), //
		new FC3ReadRegistersTask(12999, Priority.LOW, //
			m(StartStoppable.ChannelId.START_STOP, new UnsignedWordElement(12999), //
				START_STOP_CONVERTER) //
		), //
		new FC3ReadRegistersTask(13049, Priority.HIGH, //
			m(SungrowEss.ChannelId.EMS_MODE, new UnsignedWordElement(13049)), //
			m(SungrowEss.ChannelId.CHARGE_DISCHARGE_COMMAND, new UnsignedWordElement(13050)), //
			m(SungrowEss.ChannelId.CHARGE_DISCHARGE_POWER, new UnsignedWordElement(13051)), //
			new DummyRegisterElement(13052, 13056), //
			m(SungrowEss.ChannelId.MAX_SOC, new UnsignedWordElement(13057), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			m(SungrowEss.ChannelId.MIN_SOC, new UnsignedWordElement(13058), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			new DummyRegisterElement(13059, 13078), //
			m(SungrowEss.ChannelId.HEARTBEAT, new UnsignedWordElement(13079)), //
			new DummyRegisterElement(13080, 13084), //
			m(SungrowEss.ChannelId.METER_COMM_DETECTION, new UnsignedWordElement(13085)), //
			m(SungrowEss.ChannelId.EXPORT_POWER_LIMITATION, new UnsignedWordElement(13086)), //
			new DummyRegisterElement(13087, 13098), //
			m(SungrowEss.ChannelId.RESERVED_SOC_FOR_BACKUP, new UnsignedWordElement(13099)) //
		), //

		new FC16WriteRegistersTask(13049, //
			m(SungrowEss.ChannelId.EMS_MODE, new UnsignedWordElement(13049)), //
			m(SungrowEss.ChannelId.CHARGE_DISCHARGE_COMMAND, new UnsignedWordElement(13050)), //
			m(SungrowEss.ChannelId.CHARGE_DISCHARGE_POWER, new UnsignedWordElement(13051)) //
		), //
		new FC6WriteRegisterTask(13079, //
			m(SungrowEss.ChannelId.HEARTBEAT, new UnsignedWordElement(13079)) //
		) //

	);
    }

    private boolean isCharging() {
	return (this.getBatteryChargingChannel().value().orElse(//
		!this.getBatteryDischargingChannel().value().orElse(true)));
    }

    @Override
    public Integer getSurplusPower() {
	return null;
    }

    @Override
    public Power getPower() {
	return this.power;
    }

    @Override
    public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
	if (this.config.readOnly()) {
	    this.getEmsModeChannel().setNextWriteValue(EmsMode.SELF_CONSUMPTION);
			this.getHeartbeatChannel().setNextWriteValue(0);
			return;
		}
		if (this.heartbeat == 500) {
			this.heartbeat = 600;
		} else {
			this.heartbeat = 500;
		}
		this.getHeartbeatChannel().setNextWriteValue(this.heartbeat);
		this.channel(SungrowEss.ChannelId.DEBUG_HEARTBEAT).setNextValue(this.heartbeat);
	
		this.getEmsModeChannel().setNextWriteValue(EmsMode.EXTERNAL_EMS_MODE);
	
		if (activePower > 0) {
			this.getChargeDischargeCommandChannel().setNextWriteValue(ChargeDischargeCommand.DISCHARGE);
			this.getChargeDischargePowerChannel().setNextWriteValue(activePower);
		} else {
			this.getChargeDischargeCommandChannel().setNextWriteValue(ChargeDischargeCommand.CHARGE);
			this.getChargeDischargePowerChannel().setNextWriteValue(-activePower);
		}
	}

    @Override
    public int getPowerPrecision() {
	return 1;
    }

    @Override
    public void setStartStop(StartStop value) throws OpenemsNamedException {
    // TODO not yet implemented
    }

    @Override
    public String debugLog() {
	return new StringBuilder() //
		.append("SoC:").append(this.getSoc()) //
		.append("|Active Power:").append(this.getActivePower().toString()).append("|DC Discharge Power:")
		.append(this.getDcDischargePower().toString()).toString();
    }

}
