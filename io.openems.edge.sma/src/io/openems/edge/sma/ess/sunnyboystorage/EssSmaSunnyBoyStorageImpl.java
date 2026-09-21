package io.openems.edge.sma.ess.sunnyboystorage;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.SMA.SunnyBoyStorage", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class EssSmaSunnyBoyStorageImpl extends AbstractOpenemsModbusComponent implements EssSmaSunnyBoyStorage,
		ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent, ModbusSlave {

	/**
	 * SBS 2.5 peak AC power in W.
	 */
	private static final int MAX_APPARENT_POWER = 2500;

	@Reference
	private Power power;

	private Config config;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EssSmaSunnyBoyStorageImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				EssSmaSunnyBoyStorage.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());

		setValue(this, SymmetricEss.ChannelId.MAX_APPARENT_POWER, MAX_APPARENT_POWER);
		setValue(this, SymmetricEss.ChannelId.CAPACITY, config.capacity());
		setValue(this, ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, -MAX_APPARENT_POWER);
		setValue(this, ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, MAX_APPARENT_POWER);
		setValue(this, SymmetricEss.ChannelId.GRID_MODE, GridMode.ON_GRID);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		if (this.config.readOnlyMode()) {
			return;
		}

		// 2289 = Charge battery
		// 2290 = Discharge battery
		// 2424 = Presetting (self-consumption)
		int bmsMode = activePower < 0 ? 2289 : (activePower > 0 ? 2290 : 2424);
		IntegerWriteChannel bmsModeChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.BMS_MODE);
		bmsModeChannel.setNextWriteValue(bmsMode);

		// For forced charge/discharge set min=max to lock in the exact power rate.
		// For self-consumption (activePower=0) set min=0, max=MAX so the internal BMS
		// can freely manage the battery.
		final int minCharge;
		final int maxCharge;
		final int minDischarge;
		final int maxDischarge;
		if (activePower < 0) {
			int absPower = Math.abs(activePower);
			minCharge = absPower;
			maxCharge = absPower;
			minDischarge = 0;
			maxDischarge = 0;
		} else if (activePower > 0) {
			minCharge = 0;
			maxCharge = 0;
			minDischarge = activePower;
			maxDischarge = activePower;
		} else {
			// Solver commanded 0 W: hold battery neutral.
			// BMS_MODE=2424 (Presetting) with all limits=0 locks the device at 0 W,
			// matching EVCC's "Hold" mode. The balancing controller sends explicit
			// charge/discharge commands every cycle when actual power is needed.
			minCharge = 0;
			maxCharge = 0;
			minDischarge = 0;
			maxDischarge = 0;
		}

		// All 6 CmpBMS registers must be refreshed within every 60 s window
		IntegerWriteChannel minChargeChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.MIN_CHARGE_POWER);
		minChargeChannel.setNextWriteValue(minCharge);

		IntegerWriteChannel maxChargeChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.MAX_CHARGE_POWER);
		maxChargeChannel.setNextWriteValue(maxCharge);

		IntegerWriteChannel minDischargeChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.MIN_DISCHARGE_POWER);
		minDischargeChannel.setNextWriteValue(minDischarge);

		IntegerWriteChannel maxDischargeChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.MAX_DISCHARGE_POWER);
		maxDischargeChannel.setNextWriteValue(maxDischarge);

		// GridWSpt=0: let the inverter manage the grid/PV split internally.
		// Min/max charge-power registers already lock in the required power rate.
		IntegerWriteChannel gridSetpointChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.GRID_POWER_SETPOINT);
		gridSetpointChannel.setNextWriteValue(0);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// The SMA Sunny Boy Storage uses the SMA register number directly as the
		// 0-based Modbus PDU address (non-standard but confirmed by live testing).
		//
		// Reads:
		// FC3 @ 30775 : Battery Power [W] int32 (2 words)
		// FC3 @ 30513 : Energy Total [Wh] uint64 (4 words)
		// FC3 @ 30845 : State of Charge [%] uint32 (2 words)
		//
		// Writes (all 6 must be refreshed within every 60 s window):
		// FC16 @ 40236 : BMS Mode uint32 (2289=Charge, 2290=Discharge, 2424=Presetting)
		// FC16 @ 40793 : Min Charge Power [W] uint32 (CmpBMS.BatChaMinW)
		// FC16 @ 40795 : Max Charge Power [W] uint32 (CmpBMS.BatChaMaxW)
		// FC16 @ 40797 : Min Discharge Power [W] uint32 (CmpBMS.BatDschMinW)
		// FC16 @ 40799 : Max Discharge Power [W] uint32 (CmpBMS.BatDschMaxW)
		// FC16 @ 40801 : Grid Power Setpoint [W] int32 (CmpBMS.GridWSpt,
		// positive=import/charge)
		//
		// Power limits (live from device, same register layout as SunnyIsland):
		// FC3 @ 40189 : Allowed Charge Power [W] uint32 (INVERT → negative)
		// FC3 @ 40191 : Allowed Discharge Power [W] uint32 Priority.LOW: initial safe
		// defaults (-2500/+2500) set in activate(); these refine over time.
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(30775, Priority.HIGH, //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, //
								new SignedDoublewordElement(30775))), //
				new FC3ReadRegistersTask(30513, Priority.LOW, //
						m(EssSmaSunnyBoyStorage.ChannelId.ENERGY_TOTAL, //
								new UnsignedQuadruplewordElement(30513))), //
				new FC3ReadRegistersTask(30845, Priority.LOW, //
						m(SymmetricEss.ChannelId.SOC, //
								new UnsignedDoublewordElement(30845))), //
				new FC3ReadRegistersTask(40189, Priority.LOW, //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, //
								new UnsignedDoublewordElement(40189), INVERT), //
						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, //
								new UnsignedDoublewordElement(40191))), //
				new FC16WriteRegistersTask(40236, //
						m(EssSmaSunnyBoyStorage.ChannelId.BMS_MODE, //
								new UnsignedDoublewordElement(40236))), //
				new FC16WriteRegistersTask(40793, //
						m(EssSmaSunnyBoyStorage.ChannelId.MIN_CHARGE_POWER, //
								new UnsignedDoublewordElement(40793)), //
						m(EssSmaSunnyBoyStorage.ChannelId.MAX_CHARGE_POWER, //
								new UnsignedDoublewordElement(40795))), //
				new FC16WriteRegistersTask(40797, //
						m(EssSmaSunnyBoyStorage.ChannelId.MIN_DISCHARGE_POWER, //
								new UnsignedDoublewordElement(40797)), //
						m(EssSmaSunnyBoyStorage.ChannelId.MAX_DISCHARGE_POWER, //
								new UnsignedDoublewordElement(40799)), //
						m(EssSmaSunnyBoyStorage.ChannelId.GRID_POWER_SETPOINT, //
								new SignedDoublewordElement(40801))) //
		);
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|P:" + this.getActivePower().asString() //
				+ "|AllowedCharge:" + this.getAllowedChargePower().asString() //
				+ "|AllowedDischarge:" + this.getAllowedDischargePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EssSmaSunnyBoyStorage.class, accessMode, 300) //
						.build());
	}
}
