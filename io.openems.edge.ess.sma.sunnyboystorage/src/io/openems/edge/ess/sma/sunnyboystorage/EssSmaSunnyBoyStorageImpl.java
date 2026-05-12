package io.openems.edge.ess.sma.sunnyboystorage;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Sma.SunnyBoyStorage", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EssSmaSunnyBoyStorageImpl extends AbstractOpenemsModbusComponent
		implements EssSmaSunnyBoyStorage, ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent,
		ModbusSlave {

	/**
	 * SBS 2.5 peak AC power in W.
	 */
	private static final int MAX_APPARENT_POWER = 2500;

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	private Config config;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
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
		this._setMaxApparentPower(MAX_APPARENT_POWER);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		this._setCapacity(config.capacity());
		this.getAllowedChargePowerChannel().setNextValue(-MAX_APPARENT_POWER);
		this._setAllowedDischargePower(MAX_APPARENT_POWER);
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

		IntegerWriteChannel bmsModeChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.BMS_MODE);
		bmsModeChannel.setNextWriteValue(2424);

		int chargePower = activePower < 0 ? Math.abs(activePower) : 0;
		int dischargePower = activePower > 0 ? activePower : 0;

		// All 6 CmpBMS registers must be refreshed within every 60 s window
		IntegerWriteChannel minChargeChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.MIN_CHARGE_POWER);
		minChargeChannel.setNextWriteValue(0);

		IntegerWriteChannel maxChargeChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.MAX_CHARGE_POWER);
		maxChargeChannel.setNextWriteValue(chargePower);

		IntegerWriteChannel minDischargeChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.MIN_DISCHARGE_POWER);
		minDischargeChannel.setNextWriteValue(0);

		IntegerWriteChannel maxDischargeChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.MAX_DISCHARGE_POWER);
		maxDischargeChannel.setNextWriteValue(dischargePower);

		IntegerWriteChannel gridSetpointChannel = this.channel(EssSmaSunnyBoyStorage.ChannelId.GRID_POWER_SETPOINT);
		gridSetpointChannel.setNextWriteValue(activePower);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		/*
		 * The SMA Sunny Boy Storage uses the SMA register number directly as the
		 * 0-based Modbus PDU address (non-standard but confirmed by live testing).
		 *
		 * Reads:
		 *   FC4 @ 30775 : Battery Power [W]    int32  (2 words)
		 *   FC3 @ 30513 : Energy Total [Wh]    uint64 (4 words)
		 *   FC3 @ 30845 : State of Charge [%]  uint32 (2 words)
		 *
		 * Writes (all 6 must be refreshed within every 60 s window):
		 *   FC16 @ 40236 : BMS Mode              uint32 (2424=Normal, 2289=ForceCharge)
		 *   FC16 @ 40793 : Min Charge Power [W]  uint32 (CmpBMS.BatChaMinW)
		 *   FC16 @ 40795 : Max Charge Power [W]  uint32 (CmpBMS.BatChaMaxW)
		 *   FC16 @ 40797 : Min Discharge Power [W] uint32 (CmpBMS.BatDschMinW)
		 *   FC16 @ 40799 : Max Discharge Power [W] uint32 (CmpBMS.BatDschMaxW)
		 *   FC16 @ 40801 : Grid Power Setpoint [W] int32  (CmpBMS.GridWSpt)
		 */
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(30775, Priority.HIGH, //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, //
								new SignedDoublewordElement(30775))), //
				new FC3ReadRegistersTask(30513, Priority.LOW, //
						m(EssSmaSunnyBoyStorage.ChannelId.ENERGY_TOTAL, //
								new UnsignedQuadruplewordElement(30513))), //
				new FC3ReadRegistersTask(30845, Priority.HIGH, //
						m(SymmetricEss.ChannelId.SOC, //
								new UnsignedDoublewordElement(30845))), //
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
				+ "|AllowedDischarge:" + this.getAllowedDischargePower().asString();
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
