package io.openems.edge.ess.stabl;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;

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
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
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
		name = "ESS.Stabl", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EssStablImpl extends AbstractOpenemsModbusComponent
		implements EssStabl, ModbusComponent, OpenemsComponent, ModbusSlave {

	public static final int MAX_APPARENT_POWER = 66_000;

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EssStablImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				EssStabl.ChannelId.values() //
		);
		this._setMaxApparentPower(EssStablImpl.MAX_APPARENT_POWER);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1016, Priority.LOW, //
						m(EssStabl.ChannelId.COM_TIME_OUT_EMS, new UnsignedWordElement(1016)),
						m(EssStabl.ChannelId.SOFTWARE_RESET, new UnsignedWordElement(1017))),
				new FC3ReadRegistersTask(4000, Priority.LOW,
						m(EssStabl.ChannelId.ACTIVATE_POWER_STAGE, new UnsignedWordElement(4000))),
				new FC3ReadRegistersTask(4009, Priority.LOW,
						m(EssStabl.ChannelId.GRID_TYPE, new UnsignedWordElement(4009))),
				new FC3ReadRegistersTask(4195, Priority.LOW,
						m(EssStabl.ChannelId.SYSTEM_SIGNED_POWER_SET_POINT_AC, new SignedWordElement(4195))),
				new FC3ReadRegistersTask(6576, Priority.LOW,
						m(EssStabl.ChannelId.SOC_MIN_SYSTEM, new UnsignedWordElement(6576), SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6640, Priority.LOW,
						m(EssStabl.ChannelId.SOC_MAX_SYSTEM, new UnsignedWordElement(6640), SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6704, Priority.LOW,
						m(EssStabl.ChannelId.SOC_AVG_SYSTEM, new UnsignedWordElement(6704), SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6960, Priority.LOW,
						m(EssStabl.ChannelId.CURRENT_LIMIT_DISCHARGE_SYSTEM, new UnsignedWordElement(6960))),
				new FC3ReadRegistersTask(6964, Priority.LOW,
						m(EssStabl.ChannelId.CURRENT_LIMIT_CHARGE_SYSTEM, new UnsignedWordElement(6964))),
				new FC3ReadRegistersTask(5000, Priority.LOW,
						m(EssStabl.ChannelId.ACTUAL_MAIN_STATE, new SignedWordElement(5000))),
				new FC3ReadRegistersTask(5023, Priority.LOW,
						m(EssStabl.ChannelId.AC_DC_ACTIVE_GRID_TYPE, new UnsignedWordElement(5023))),
				new FC3ReadRegistersTask(5140, Priority.LOW,
						m(EssStabl.ChannelId.ACTUAL_POWER_AC_L1, new SignedWordElement(5140), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.ACTUAL_POWER_AC_L2, new SignedWordElement(5141), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.ACTUAL_POWER_AC_L3, new SignedWordElement(5142), SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(5160, Priority.LOW,
						m(EssStabl.ChannelId.GRID_VOLTAGE_L1, new SignedWordElement(5160), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.GRID_VOLTAGE_L2, new SignedWordElement(5161), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.GRID_VOLTAGE_L3, new SignedWordElement(5162), SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(5500, Priority.LOW,
						m(EssStabl.ChannelId.INLET_AIR_TEMPERATURE, new SignedWordElement(5500)),
						m(EssStabl.ChannelId.MCU_CORE_TEMPERATURE, new SignedWordElement(5501))),
				new FC3ReadRegistersTask(2808, Priority.LOW,
						m(EssStabl.ChannelId.TOTAL_ALARMS_CNT, new SignedWordElement(2808))),
				new FC3ReadRegistersTask(2810, Priority.LOW, //
						m(EssStabl.ChannelId.ALARM1, new SignedWordElement(2810)),
						m(EssStabl.ChannelId.ALARM2, new SignedWordElement(2811)),
						m(EssStabl.ChannelId.ALARM3, new SignedWordElement(2812)),
						m(EssStabl.ChannelId.ALARM4, new SignedWordElement(2813)),
						m(EssStabl.ChannelId.ALARM5, new SignedWordElement(2814)),
						m(EssStabl.ChannelId.ALARM6, new SignedWordElement(2815)),
						m(EssStabl.ChannelId.ALARM7, new SignedWordElement(2816)),
						m(EssStabl.ChannelId.ALARM8, new SignedWordElement(2817)),
						m(EssStabl.ChannelId.ALARM9, new SignedWordElement(2818)),
						m(EssStabl.ChannelId.ALARM10, new SignedWordElement(2819)),
						m(EssStabl.ChannelId.ALARM11, new SignedWordElement(2820)),
						m(EssStabl.ChannelId.ALARM12, new SignedWordElement(2821)),
						m(EssStabl.ChannelId.ALARM13, new SignedWordElement(2822)),
						m(EssStabl.ChannelId.ALARM14, new SignedWordElement(2823)),
						m(EssStabl.ChannelId.ALARM15, new SignedWordElement(2824)),
						m(EssStabl.ChannelId.ALARM16, new SignedWordElement(2825)),
						m(EssStabl.ChannelId.ALARM17, new SignedWordElement(2826)),
						m(EssStabl.ChannelId.ALARM18, new SignedWordElement(2827)),
						m(EssStabl.ChannelId.ALARM19, new SignedWordElement(2828)),
						m(EssStabl.ChannelId.ALARM20, new SignedWordElement(2829))),

				new FC6WriteRegisterTask(7000, //
						m(EssStabl.ChannelId.ACTIVE_POWER_M0D_SETPOINT_STRING_1_MODULEX,
								new UnsignedWordElement(7000))),

				new FC6WriteRegisterTask(7020, //
						m(EssStabl.ChannelId.ACTIVE_POWER_M0D_SETPOINT_STRING_2_MODULEX,
								new UnsignedWordElement(7020))),

				new FC6WriteRegisterTask(7040, //
						m(EssStabl.ChannelId.ACTIVE_POWER_M0D_SETPOINT_STRING_3_MODULEX, new UnsignedWordElement(7040)))

		);
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";" //
				+ this.getAllowedDischargePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 100;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EssStablImpl.class, accessMode, 100) //
						.build());
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		IntegerWriteChannel setActivePower = this.channel(EssStabl.ChannelId.SYSTEM_SIGNED_POWER_SET_POINT_AC);
		setActivePower.setNextWriteValue(activePower);

	}
}
