package io.openems.edge.ess.fenecon.commercial40;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.fenecon.commercial40.charger.EssDcChargerFeneconCommercial40;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Fenecon.Commercial40", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
)
public class EssFeneconCommercial40Impl extends AbstractOpenemsModbusComponent implements EssFeneconCommercial40,
		ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(EssFeneconCommercial40Impl.class);

	protected final static int MAX_APPARENT_POWER = 40000;
	protected final static int NET_CAPACITY = 40000;

	private final static int UNIT_ID = 100;
	private final static int MIN_REACTIVE_POWER = -10000;
	private final static int MAX_REACTIVE_POWER = 10000;

	private Config config;
	private EssDcChargerFeneconCommercial40 charger = null;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	public EssFeneconCommercial40Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ChannelId.values() //
		);
		this.getCapacity().setNextValue(NET_CAPACITY);
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		if (this.config.readOnlyMode()) {
			return;
		}

		IntegerWriteChannel setActivePowerChannel = this.channel(ChannelId.SET_ACTIVE_POWER);
		setActivePowerChannel.setNextWriteValue(activePower);
		IntegerWriteChannel setReactivePowerChannel = this.channel(ChannelId.SET_REACTIVE_POWER);
		setReactivePowerChannel.setNextWriteValue(reactivePower);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// EnumReadChannels
		SYSTEM_STATE(Doc.of(SystemState.values())), //
		CONTROL_MODE(Doc.of(ControlMode.values())), //
		BATTERY_MAINTENANCE_STATE(Doc.of(BatteryMaintenanceState.values())), //
		INVERTER_STATE(Doc.of(InverterState.values())), //
		SYSTEM_MANUFACTURER(Doc.of(SystemManufacturer.values())), //
		SYSTEM_TYPE(Doc.of(SystemType.values())), //
		BATTERY_STRING_SWITCH_STATE(Doc.of(BatteryStringSwitchState.values())), //
		BMS_DCDC_WORK_STATE(Doc.of(BmsDcdcWorkState.values())), //
		BMS_DCDC_WORK_MODE(Doc.of(BmsDcdcWorkMode.values())), //

		// EnumWriteChannels
		SET_WORK_STATE(Doc.of(SetWorkState.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		// IntegerWriteChannel
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_PV_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		// IntegerReadChannels
		ORIGINAL_ALLOWED_CHARGE_POWER(new IntegerDoc() //
				.onInit(channel -> { //
					// on each Update to the channel -> set the ALLOWED_CHARGE_POWER value with a
					// delta of max 500
					channel.onChange(originalValueChannel -> {
						IntegerReadChannel currentValueChannel = channel.getComponent()
								.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER);
						Optional<Integer> originalValue = originalValueChannel.asOptional();
						Optional<Integer> currentValue = currentValueChannel.value().asOptional();
						int value;
						if (!originalValue.isPresent() && !currentValue.isPresent()) {
							value = 0;
						} else if (originalValue.isPresent() && !currentValue.isPresent()) {
							value = originalValue.get();
						} else if (!originalValue.isPresent() && currentValue.isPresent()) {
							value = currentValue.get();
						} else {
							value = Math.max(originalValue.get(), currentValue.get() - 500);
						}
						currentValueChannel.setNextValue(value);
					});
				})), //
		ORIGINAL_ALLOWED_DISCHARGE_POWER(new IntegerDoc() //
				.onInit(channel -> { //
					// on each Update to the channel -> set the ALLOWED_DISCHARGE_POWER value with a
					// delta of max 500
					channel.onChange(originalValueChannel -> {
						IntegerReadChannel currentValueChannel = channel.getComponent()
								.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER);
						Optional<Integer> originalValue = originalValueChannel.asOptional();
						Optional<Integer> currentValue = currentValueChannel.value().asOptional();
						int value;
						if (!originalValue.isPresent() && !currentValue.isPresent()) {
							value = 0;
						} else if (originalValue.isPresent() && !currentValue.isPresent()) {
							value = originalValue.get();
						} else if (!originalValue.isPresent() && currentValue.isPresent()) {
							value = currentValue.get();
						} else {
							value = Math.min(originalValue.get(), currentValue.get() + 500);
						}
						currentValueChannel.setNextValue(value);
					});
				})), //
		PROTOCOL_VERSION(Doc.of(OpenemsType.INTEGER)), //
		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BATTERY_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		AC_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)), //
		AC_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)), //
		GRID_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ)), //
		INVERTER_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		INVERTER_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		INVERTER_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		INVERTER_CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		INVERTER_CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		INVERTER_CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		IPM_TEMPERATURE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		IPM_TEMPERATURE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		IPM_TEMPERATURE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		TRANSFORMER_TEMPERATURE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		CELL_1_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_2_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_3_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_4_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_5_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_6_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_7_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_8_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_9_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_10_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_11_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_12_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_13_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_14_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_15_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_16_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_17_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_18_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_19_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_20_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_21_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_22_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_23_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_24_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_25_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_26_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_27_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_28_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_29_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_30_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_31_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_32_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_33_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_34_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_35_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_36_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_37_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_38_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_39_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_40_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_41_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_42_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_43_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_44_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_45_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_46_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_47_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_48_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_49_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_50_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_51_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_52_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_53_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_54_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_55_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_56_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_57_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_58_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_59_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_60_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_61_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_62_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_63_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_64_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_65_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_66_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_67_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_68_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_69_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_70_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_71_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_72_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_73_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_74_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_75_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_76_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_77_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_78_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_79_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_80_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_81_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_82_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_83_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_84_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_85_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_86_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_87_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_88_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_89_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_90_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_91_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_92_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_93_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_94_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_95_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_96_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_97_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_98_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_99_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_100_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_101_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_102_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_103_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_104_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_105_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_106_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_107_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_108_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_109_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_110_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_111_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_112_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_113_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_114_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_115_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_116_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_117_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_118_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_119_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_120_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_121_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_122_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_123_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_124_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_125_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_126_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_127_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_128_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_129_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_130_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_131_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_132_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_133_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_134_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_135_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_136_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_137_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_138_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_139_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_140_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_141_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_142_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_143_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_144_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_145_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_146_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_147_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_148_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_149_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_150_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_151_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_152_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_153_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_154_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_155_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_156_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_157_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_158_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_159_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_160_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_161_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_162_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_163_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_164_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_165_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_166_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_167_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_168_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_169_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_170_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_171_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_172_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_173_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_174_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_175_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_176_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_177_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_178_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_179_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_180_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_181_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_182_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_183_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_184_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_185_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_186_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_187_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_188_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_189_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_190_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_191_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_192_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_193_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_194_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_195_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_196_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_197_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_198_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_199_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_200_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_201_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_202_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_203_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_204_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_205_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_206_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_207_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_208_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_209_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_210_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_211_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_212_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_213_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_214_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_215_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_216_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_217_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_218_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_219_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_220_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_221_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_222_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_223_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_224_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		SURPLUS_FEED_IN_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		// StateChannels
		STATE_0(Doc.of(Level.WARNING) //
				.text("Emergency Stop")), //
		STATE_1(Doc.of(Level.WARNING) //
				.text("Key Manual Stop")), //
		STATE_2(Doc.of(Level.WARNING) //
				.text("Transformer Phase B Temperature Sensor Invalidation")), //
		STATE_3(Doc.of(Level.WARNING) //
				.text("SD Memory Card Invalidation")), //
		STATE_4(Doc.of(Level.WARNING) //
				.text("Inverter Communication Abnormity")), //
		STATE_5(Doc.of(Level.WARNING) //
				.text("Battery Stack Communication Abnormity")), //
		STATE_6(Doc.of(Level.WARNING) //
				.text("Multifunctional Ammeter Communication Abnormity")), //
		STATE_7(Doc.of(Level.WARNING) //
				.text("Remote Communication Abnormity")), //
		STATE_8(Doc.of(Level.WARNING) //
				.text("PVDC1 Communication Abnormity")), //
		STATE_9(Doc.of(Level.WARNING) //
				.text("PVDC2 Communication Abnormity")), //
		STATE_10(Doc.of(Level.WARNING) //
				.text("Transformer Severe Overtemperature")), //
		STATE_11(Doc.of(Level.FAULT) //
				.text("DC Precharge Contactor Close Unsuccessfully")), //
		STATE_12(Doc.of(Level.FAULT) //
				.text("AC Precharge Contactor Close Unsuccessfully")), //
		STATE_13(Doc.of(Level.FAULT) //
				.text("AC Main Contactor Close Unsuccessfully")), //
		STATE_14(Doc.of(Level.FAULT) //
				.text("DC Electrical Breaker1 Close Unsuccessfully")), //
		STATE_15(Doc.of(Level.FAULT) //
				.text("DC Main Contactor Close Unsuccessfully")), //
		STATE_16(Doc.of(Level.FAULT) //
				.text("AC Breaker Trip")), //
		STATE_17(Doc.of(Level.FAULT) //
				.text("AC Main Contactor Open When Running")), //
		STATE_18(Doc.of(Level.FAULT) //
				.text("DC Main Contactor Open When Running")), //
		STATE_19(Doc.of(Level.FAULT) //
				.text("AC Main Contactor Open Unsuccessfully")), //
		STATE_20(Doc.of(Level.FAULT) //
				.text("DC Electrical Breaker1 Open Unsuccessfully")), //
		STATE_21(Doc.of(Level.FAULT) //
				.text("DC Main Contactor Open Unsuccessfully")), //
		STATE_22(Doc.of(Level.FAULT) //
				.text("Hardware PDP Fault")), //
		STATE_23(Doc.of(Level.FAULT) //
				.text("Master Stop Suddenly")), //
		STATE_24(Doc.of(Level.FAULT) //
				.text("DCShortCircuitProtection")), //
		STATE_25(Doc.of(Level.FAULT) //
				.text("DCOvervoltageProtection")), //
		STATE_26(Doc.of(Level.FAULT) //
				.text("DCUndervoltageProtection")), //
		STATE_27(Doc.of(Level.FAULT) //
				.text("DCInverseNoConnectionProtection")), //
		STATE_28(Doc.of(Level.FAULT) //
				.text("DCDisconnectionProtection")), //
		STATE_29(Doc.of(Level.FAULT) //
				.text("CommutingVoltageAbnormityProtection")), //
		STATE_30(Doc.of(Level.FAULT) //
				.text("DCOvercurrentProtection")), //
		STATE_31(Doc.of(Level.FAULT) //
				.text("Phase1PeakCurrentOverLimitProtection")), //
		STATE_32(Doc.of(Level.FAULT) //
				.text("Phase2PeakCurrentOverLimitProtection")), //
		STATE_33(Doc.of(Level.FAULT) //
				.text("Phase3PeakCurrentOverLimitProtection")), //
		STATE_34(Doc.of(Level.FAULT) //
				.text("Phase1GridVoltageSamplingInvalidation")), //
		STATE_35(Doc.of(Level.FAULT) //
				.text("Phase2VirtualCurrentOverLimitProtection")), //
		STATE_36(Doc.of(Level.FAULT) //
				.text("Phase3VirtualCurrentOverLimitProtection")), //
		STATE_37(Doc.of(Level.FAULT) //
				.text("Phase1GridVoltageSamplingInvalidation2")), //
		STATE_38(Doc.of(Level.FAULT) //
				.text("Phase2ridVoltageSamplingInvalidation")), //
		STATE_39(Doc.of(Level.FAULT) //
				.text("Phase3GridVoltageSamplingInvalidation")), //
		STATE_40(Doc.of(Level.FAULT) //
				.text("Phase1InvertVoltageSamplingInvalidation")), //
		STATE_41(Doc.of(Level.FAULT) //
				.text("Phase2InvertVoltageSamplingInvalidation")), //
		STATE_42(Doc.of(Level.FAULT) //
				.text("Phase3InvertVoltageSamplingInvalidation")), //
		STATE_43(Doc.of(Level.FAULT) //
				.text("ACCurrentSamplingInvalidation")), //
		STATE_44(Doc.of(Level.FAULT) //
				.text("DCCurrentSamplingInvalidation")), //
		STATE_45(Doc.of(Level.FAULT) //
				.text("Phase1OvertemperatureProtection")), //
		STATE_46(Doc.of(Level.FAULT) //
				.text("Phase2OvertemperatureProtection")), //
		STATE_47(Doc.of(Level.FAULT) //
				.text("Phase3OvertemperatureProtection")), //
		STATE_48(Doc.of(Level.FAULT) //
				.text("Phase1TemperatureSamplingInvalidation")), //
		STATE_49(Doc.of(Level.FAULT) //
				.text("Phase2TemperatureSamplingInvalidation")), //
		STATE_50(Doc.of(Level.FAULT) //
				.text("Phase3TemperatureSamplingInvalidation")), //
		STATE_51(Doc.of(Level.FAULT) //
				.text("Phase1PrechargeUnmetProtection")), //
		STATE_52(Doc.of(Level.FAULT) //
				.text("Phase2PrechargeUnmetProtection")), //
		STATE_53(Doc.of(Level.FAULT) //
				.text("Phase3PrechargeUnmetProtection")), //
		STATE_54(Doc.of(Level.FAULT) //
				.text("UnadaptablePhaseSequenceErrorProtection")), //
		STATE_55(Doc.of(Level.FAULT) //
				.text("DSPProtection")), //
		STATE_56(Doc.of(Level.FAULT) //
				.text("Phase1GridVoltageSevereOvervoltageProtection")), //
		STATE_57(Doc.of(Level.FAULT) //
				.text("Phase1GridVoltageGeneralOvervoltageProtection")), //
		STATE_58(Doc.of(Level.FAULT) //
				.text("Phase2GridVoltageSevereOvervoltageProtection")), //
		STATE_59(Doc.of(Level.FAULT) //
				.text("Phase2GridVoltageGeneralOvervoltageProtection")), //
		STATE_60(Doc.of(Level.FAULT) //
				.text("Phase3GridVoltageSevereOvervoltageProtection")), //
		STATE_61(Doc.of(Level.FAULT) //
				.text("Phase3GridVoltageGeneralOvervoltageProtection")), //
		STATE_62(Doc.of(Level.FAULT) //
				.text("Phase1GridVoltageSevereUndervoltageProtection")), //
		STATE_63(Doc.of(Level.FAULT) //
				.text("Phase1GridVoltageGeneralUndervoltageProtection")), //
		STATE_64(Doc.of(Level.FAULT) //
				.text("Phase2GridVoltageSevereUndervoltageProtection")), //
		STATE_65(Doc.of(Level.FAULT) //
				.text("Phase2GridVoltageGeneralUndervoltageProtection")), //
		STATE_66(Doc.of(Level.FAULT) //
				.text("Phase3GridVoltageSevereUndervoltageProtection")), //
		STATE_67(Doc.of(Level.FAULT) //
				.text("Phase3GridVoltageGeneralUndervoltageProtection")), //
		STATE_68(Doc.of(Level.FAULT) //
				.text("SevereOverfrequncyProtection")), //
		STATE_69(Doc.of(Level.FAULT) //
				.text("GeneralOverfrequncyProtection")), //
		STATE_70(Doc.of(Level.FAULT) //
				.text("SevereUnderfrequncyProtection")), //
		STATE_71(Doc.of(Level.FAULT) //
				.text("GeneralsUnderfrequncyProtection")), //
		STATE_72(Doc.of(Level.FAULT) //
				.text("Phase1Gridloss")), //
		STATE_73(Doc.of(Level.FAULT) //
				.text("Phase2Gridloss")), //
		STATE_74(Doc.of(Level.FAULT) //
				.text("Phase3Gridloss")), //
		STATE_75(Doc.of(Level.FAULT) //
				.text("IslandingProtection")), //
		STATE_76(Doc.of(Level.FAULT) //
				.text("Phase1UnderVoltageRideThrough")), //
		STATE_77(Doc.of(Level.FAULT) //
				.text("Phase2UnderVoltageRideThrough")), //
		STATE_78(Doc.of(Level.FAULT) //
				.text("Phase3UnderVoltageRideThrough")), //
		STATE_79(Doc.of(Level.FAULT) //
				.text("Phase1InverterVoltageSevereOvervoltageProtection")), //
		STATE_80(Doc.of(Level.FAULT) //
				.text("Phase1InverterVoltageGeneralOvervoltageProtection")), //
		STATE_81(Doc.of(Level.FAULT) //
				.text("Phase2InverterVoltageSevereOvervoltageProtection")), //
		STATE_82(Doc.of(Level.FAULT) //
				.text("Phase2InverterVoltageGeneralOvervoltageProtection")), //
		STATE_83(Doc.of(Level.FAULT) //
				.text("Phase3InverterVoltageSevereOvervoltageProtection")), //
		STATE_84(Doc.of(Level.FAULT) //
				.text("Phase3InverterVoltageGeneralOvervoltageProtection")), //
		STATE_85(Doc.of(Level.FAULT) //
				.text("InverterPeakVoltageHighProtectionCauseByACDisconnect")), //
		STATE_86(Doc.of(Level.WARNING) //
				.text("DCPrechargeContactorInspectionAbnormity")), //
		STATE_87(Doc.of(Level.WARNING) //
				.text("DCBreaker1InspectionAbnormity")), //
		STATE_88(Doc.of(Level.WARNING) //
				.text("DCBreaker2InspectionAbnormity")), //
		STATE_89(Doc.of(Level.WARNING) //
				.text("ACPrechargeContactorInspectionAbnormity")), //
		STATE_90(Doc.of(Level.WARNING) //
				.text("ACMainontactorInspectionAbnormity")), //
		STATE_91(Doc.of(Level.WARNING) //
				.text("ACBreakerInspectionAbnormity")), //
		STATE_92(Doc.of(Level.WARNING) //
				.text("DCBreaker1CloseUnsuccessfully")), //
		STATE_93(Doc.of(Level.WARNING) //
				.text("DCBreaker2CloseUnsuccessfully")), //
		STATE_94(Doc.of(Level.WARNING) //
				.text("ControlSignalCloseAbnormallyInspectedBySystem")), //
		STATE_95(Doc.of(Level.WARNING) //
				.text("ControlSignalOpenAbnormallyInspectedBySystem")), //
		STATE_96(Doc.of(Level.WARNING) //
				.text("NeutralWireContactorCloseUnsuccessfully")), //
		STATE_97(Doc.of(Level.WARNING) //
				.text("NeutralWireContactorOpenUnsuccessfully")), //
		STATE_98(Doc.of(Level.WARNING) //
				.text("WorkDoorOpen")), //
		STATE_99(Doc.of(Level.WARNING) //
				.text("Emergency1Stop")), //
		STATE_100(Doc.of(Level.WARNING) //
				.text("ACBreakerCloseUnsuccessfully")), //
		STATE_101(Doc.of(Level.WARNING) //
				.text("ControlSwitchStop")), //
		STATE_102(Doc.of(Level.WARNING) //
				.text("GeneralOverload")), //
		STATE_103(Doc.of(Level.WARNING) //
				.text("SevereOverload")), //
		STATE_104(Doc.of(Level.WARNING) //
				.text("BatteryCurrentOverLimit")), //
		STATE_105(Doc.of(Level.WARNING) //
				.text("PowerDecreaseCausedByOvertemperature")), //
		STATE_106(Doc.of(Level.WARNING) //
				.text("InverterGeneralOvertemperature")), //
		STATE_107(Doc.of(Level.WARNING) //
				.text("ACThreePhaseCurrentUnbalance")), //
		STATE_108(Doc.of(Level.WARNING) //
				.text("RestoreFactorySettingUnsuccessfully")), //
		STATE_109(Doc.of(Level.WARNING) //
				.text("PoleBoardInvalidation")), //
		STATE_110(Doc.of(Level.WARNING) //
				.text("SelfInspectionFailed")), //
		STATE_111(Doc.of(Level.WARNING) //
				.text("ReceiveBMSFaultAndStop")), //
		STATE_112(Doc.of(Level.WARNING) //
				.text("RefrigerationEquipmentinvalidation")), //
		STATE_113(Doc.of(Level.WARNING) //
				.text("LargeTemperatureDifferenceAmongIGBTThreePhases")), //
		STATE_114(Doc.of(Level.WARNING) //
				.text("EEPROMParametersOverRange")), //
		STATE_115(Doc.of(Level.WARNING) //
				.text("EEPROMParametersBackupFailed")), //
		STATE_116(Doc.of(Level.WARNING) //
				.text("DCBreakerCloseunsuccessfully")), //
		STATE_117(Doc.of(Level.WARNING) //
				.text("CommunicationBetweenInverterAndBSMUDisconnected")), //
		STATE_118(Doc.of(Level.WARNING) //
				.text("CommunicationBetweenInverterAndMasterDisconnected")), //
		STATE_119(Doc.of(Level.WARNING) //
				.text("CommunicationBetweenInverterAndUCDisconnected")), //
		STATE_120(Doc.of(Level.WARNING) //
				.text("BMSStartOvertimeControlledByPCS")), //
		STATE_121(Doc.of(Level.WARNING) //
				.text("BMSStopOvertimeControlledByPCS")), //
		STATE_122(Doc.of(Level.WARNING) //
				.text("SyncSignalInvalidation")), //
		STATE_123(Doc.of(Level.WARNING) //
				.text("SyncSignalContinuousCaputureFault")), //
		STATE_124(Doc.of(Level.WARNING) //
				.text("SyncSignalSeveralTimesCaputureFault")), //
		STATE_125(Doc.of(Level.WARNING) //
				.text("CurrentSamplingChannelAbnormityOnHighVoltageSide")), //
		STATE_126(Doc.of(Level.WARNING) //
				.text("CurrentSamplingChannelAbnormityOnLowVoltageSide")), //
		STATE_127(Doc.of(Level.WARNING) //
				.text("EEPROMParametersOverRange")), //
		STATE_128(Doc.of(Level.WARNING) //
				.text("UpdateEEPROMFailed")), //
		STATE_129(Doc.of(Level.WARNING) //
				.text("ReadEEPROMFailed")), //
		STATE_130(Doc.of(Level.WARNING) //
				.text("CurrentSamplingChannelAbnormityBeforeInductance")), //
		STATE_131(Doc.of(Level.WARNING) //
				.text("ReactorPowerDecreaseCausedByOvertemperature")), //
		STATE_132(Doc.of(Level.WARNING) //
				.text("IGBTPowerDecreaseCausedByOvertemperature")), //
		STATE_133(Doc.of(Level.WARNING) //
				.text("TemperatureChanel3PowerDecreaseCausedByOvertemperature")), //
		STATE_134(Doc.of(Level.WARNING) //
				.text("TemperatureChanel4PowerDecreaseCausedByOvertemperature")), //
		STATE_135(Doc.of(Level.WARNING) //
				.text("TemperatureChanel5PowerDecreaseCausedByOvertemperature")), //
		STATE_136(Doc.of(Level.WARNING) //
				.text("TemperatureChanel6PowerDecreaseCausedByOvertemperature")), //
		STATE_137(Doc.of(Level.WARNING) //
				.text("TemperatureChanel7PowerDecreaseCausedByOvertemperature")), //
		STATE_138(Doc.of(Level.WARNING) //
				.text("TemperatureChanel8PowerDecreaseCausedByOvertemperature")), //
		STATE_139(Doc.of(Level.WARNING) //
				.text("Fan1StopFailed")), //
		STATE_140(Doc.of(Level.WARNING) //
				.text("Fan2StopFailed")), //
		STATE_141(Doc.of(Level.WARNING) //
				.text("Fan3StopFailed")), //
		STATE_142(Doc.of(Level.WARNING) //
				.text("Fan4StopFailed")), //
		STATE_143(Doc.of(Level.WARNING) //
				.text("Fan1StartupFailed")), //
		STATE_144(Doc.of(Level.WARNING) //
				.text("Fan2StartupFailed")), //
		STATE_145(Doc.of(Level.WARNING) //
				.text("Fan3StartupFailed")), //
		STATE_146(Doc.of(Level.WARNING) //
				.text("Fan4StartupFailed")), //
		STATE_147(Doc.of(Level.WARNING) //
				.text("HighVoltageSideOvervoltage")), //
		STATE_148(Doc.of(Level.WARNING) //
				.text("HighVoltageSideUndervoltage")), //
		STATE_149(Doc.of(Level.WARNING) //
				.text("HighVoltageSideVoltageChangeUnconventionally")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0x0101, Priority.LOW, //
						m(EssFeneconCommercial40Impl.ChannelId.SYSTEM_STATE, new UnsignedWordElement(0x0101)),
						m(EssFeneconCommercial40Impl.ChannelId.CONTROL_MODE, new UnsignedWordElement(0x0102)),
						new DummyRegisterElement(0x0103), // WorkMode: RemoteDispatch
						m(EssFeneconCommercial40Impl.ChannelId.BATTERY_MAINTENANCE_STATE,
								new UnsignedWordElement(0x0104)),
						m(EssFeneconCommercial40Impl.ChannelId.INVERTER_STATE, new UnsignedWordElement(0x0105)),
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(0x0106), //
								new ElementToChannelConverter((value) -> {
									Integer intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
									if (intValue != null) {
										switch (intValue) {
										case 1:
											return GridMode.OFF_GRID;
										case 2:
											return GridMode.ON_GRID;
										}
									}
									return GridMode.UNDEFINED;
								})),
						new DummyRegisterElement(0x0107), //
						m(EssFeneconCommercial40Impl.ChannelId.PROTOCOL_VERSION, new UnsignedWordElement(0x0108)),
						m(EssFeneconCommercial40Impl.ChannelId.SYSTEM_MANUFACTURER, new UnsignedWordElement(0x0109)),
						m(EssFeneconCommercial40Impl.ChannelId.SYSTEM_TYPE, new UnsignedWordElement(0x010A)),
						new DummyRegisterElement(0x010B, 0x010F), //
						m(new BitsWordElement(0x0110, this) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_0) //
								.bit(6, EssFeneconCommercial40Impl.ChannelId.STATE_1)),
						m(new BitsWordElement(0x0111, this) //
								.bit(3, EssFeneconCommercial40Impl.ChannelId.STATE_2) //
								.bit(12, EssFeneconCommercial40Impl.ChannelId.STATE_3)),
						new DummyRegisterElement(0x0112, 0x0124), //
						m(new BitsWordElement(0x0125, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_4) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_5) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_6) //
								.bit(4, EssFeneconCommercial40Impl.ChannelId.STATE_7) //
								.bit(8, EssFeneconCommercial40Impl.ChannelId.STATE_8) //
								.bit(9, EssFeneconCommercial40Impl.ChannelId.STATE_9)),
						m(new BitsWordElement(0x0126, this) //
								.bit(3, EssFeneconCommercial40Impl.ChannelId.STATE_10)), //
						new DummyRegisterElement(0x0127, 0x014F), //
						m(EssFeneconCommercial40Impl.ChannelId.BATTERY_STRING_SWITCH_STATE,
								new UnsignedWordElement(0x0150))), //
				new FC3ReadRegistersTask(0x0180, Priority.LOW, //
						m(new BitsWordElement(0x0180, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_11) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_12) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_13) //
								.bit(3, EssFeneconCommercial40Impl.ChannelId.STATE_14) //
								.bit(4, EssFeneconCommercial40Impl.ChannelId.STATE_15) //
								.bit(5, EssFeneconCommercial40Impl.ChannelId.STATE_16) //
								.bit(6, EssFeneconCommercial40Impl.ChannelId.STATE_17) //
								.bit(7, EssFeneconCommercial40Impl.ChannelId.STATE_18) //
								.bit(8, EssFeneconCommercial40Impl.ChannelId.STATE_19) //
								.bit(9, EssFeneconCommercial40Impl.ChannelId.STATE_20) //
								.bit(10, EssFeneconCommercial40Impl.ChannelId.STATE_21) //
								.bit(11, EssFeneconCommercial40Impl.ChannelId.STATE_22) //
								.bit(12, EssFeneconCommercial40Impl.ChannelId.STATE_23)),
						new DummyRegisterElement(0x0181), //
						m(new BitsWordElement(0x0182, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_24) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_25) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_26) //
								.bit(3, EssFeneconCommercial40Impl.ChannelId.STATE_27) //
								.bit(4, EssFeneconCommercial40Impl.ChannelId.STATE_28) //
								.bit(5, EssFeneconCommercial40Impl.ChannelId.STATE_29) //
								.bit(6, EssFeneconCommercial40Impl.ChannelId.STATE_30) //
								.bit(7, EssFeneconCommercial40Impl.ChannelId.STATE_31) //
								.bit(8, EssFeneconCommercial40Impl.ChannelId.STATE_32) //
								.bit(9, EssFeneconCommercial40Impl.ChannelId.STATE_33) //
								.bit(10, EssFeneconCommercial40Impl.ChannelId.STATE_34) //
								.bit(11, EssFeneconCommercial40Impl.ChannelId.STATE_35) //
								.bit(12, EssFeneconCommercial40Impl.ChannelId.STATE_36) //
								.bit(13, EssFeneconCommercial40Impl.ChannelId.STATE_37) //
								.bit(14, EssFeneconCommercial40Impl.ChannelId.STATE_38) //
								.bit(15, EssFeneconCommercial40Impl.ChannelId.STATE_39)),
						m(new BitsWordElement(0x0183, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_40) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_41) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_42) //
								.bit(3, EssFeneconCommercial40Impl.ChannelId.STATE_43) //
								.bit(4, EssFeneconCommercial40Impl.ChannelId.STATE_44) //
								.bit(5, EssFeneconCommercial40Impl.ChannelId.STATE_45) //
								.bit(6, EssFeneconCommercial40Impl.ChannelId.STATE_46) //
								.bit(7, EssFeneconCommercial40Impl.ChannelId.STATE_47) //
								.bit(8, EssFeneconCommercial40Impl.ChannelId.STATE_48) //
								.bit(9, EssFeneconCommercial40Impl.ChannelId.STATE_49) //
								.bit(10, EssFeneconCommercial40Impl.ChannelId.STATE_50) //
								.bit(11, EssFeneconCommercial40Impl.ChannelId.STATE_51) //
								.bit(12, EssFeneconCommercial40Impl.ChannelId.STATE_52) //
								.bit(13, EssFeneconCommercial40Impl.ChannelId.STATE_53) //
								.bit(14, EssFeneconCommercial40Impl.ChannelId.STATE_54) //
								.bit(15, EssFeneconCommercial40Impl.ChannelId.STATE_55)),
						m(new BitsWordElement(0x0184, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_56) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_57) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_58) //
								.bit(3, EssFeneconCommercial40Impl.ChannelId.STATE_59) //
								.bit(4, EssFeneconCommercial40Impl.ChannelId.STATE_60) //
								.bit(5, EssFeneconCommercial40Impl.ChannelId.STATE_61) //
								.bit(6, EssFeneconCommercial40Impl.ChannelId.STATE_62) //
								.bit(7, EssFeneconCommercial40Impl.ChannelId.STATE_63) //
								.bit(8, EssFeneconCommercial40Impl.ChannelId.STATE_64) //
								.bit(9, EssFeneconCommercial40Impl.ChannelId.STATE_65) //
								.bit(10, EssFeneconCommercial40Impl.ChannelId.STATE_66) //
								.bit(11, EssFeneconCommercial40Impl.ChannelId.STATE_67) //
								.bit(12, EssFeneconCommercial40Impl.ChannelId.STATE_68) //
								.bit(13, EssFeneconCommercial40Impl.ChannelId.STATE_69) //
								.bit(14, EssFeneconCommercial40Impl.ChannelId.STATE_70) //
								.bit(15, EssFeneconCommercial40Impl.ChannelId.STATE_71)),
						m(new BitsWordElement(0x0185, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_72) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_73) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_74) //
								.bit(3, EssFeneconCommercial40Impl.ChannelId.STATE_75) //
								.bit(4, EssFeneconCommercial40Impl.ChannelId.STATE_76) //
								.bit(5, EssFeneconCommercial40Impl.ChannelId.STATE_77) //
								.bit(6, EssFeneconCommercial40Impl.ChannelId.STATE_78) //
								.bit(7, EssFeneconCommercial40Impl.ChannelId.STATE_79) //
								.bit(8, EssFeneconCommercial40Impl.ChannelId.STATE_80) //
								.bit(9, EssFeneconCommercial40Impl.ChannelId.STATE_81) //
								.bit(10, EssFeneconCommercial40Impl.ChannelId.STATE_82) //
								.bit(11, EssFeneconCommercial40Impl.ChannelId.STATE_83) //
								.bit(12, EssFeneconCommercial40Impl.ChannelId.STATE_84) //
								.bit(13, EssFeneconCommercial40Impl.ChannelId.STATE_85)),
						m(new BitsWordElement(0x0186, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_86) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_87) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_88) //
								.bit(3, EssFeneconCommercial40Impl.ChannelId.STATE_89) //
								.bit(4, EssFeneconCommercial40Impl.ChannelId.STATE_90) //
								.bit(5, EssFeneconCommercial40Impl.ChannelId.STATE_91) //
								.bit(6, EssFeneconCommercial40Impl.ChannelId.STATE_92) //
								.bit(7, EssFeneconCommercial40Impl.ChannelId.STATE_93) //
								.bit(8, EssFeneconCommercial40Impl.ChannelId.STATE_94) //
								.bit(9, EssFeneconCommercial40Impl.ChannelId.STATE_95) //
								.bit(10, EssFeneconCommercial40Impl.ChannelId.STATE_96) //
								.bit(11, EssFeneconCommercial40Impl.ChannelId.STATE_97) //
								.bit(12, EssFeneconCommercial40Impl.ChannelId.STATE_98) //
								.bit(13, EssFeneconCommercial40Impl.ChannelId.STATE_99) //
								.bit(14, EssFeneconCommercial40Impl.ChannelId.STATE_100) //
								.bit(15, EssFeneconCommercial40Impl.ChannelId.STATE_101)),
						m(new BitsWordElement(0x0187, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_102) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_103) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_104) //
								.bit(3, EssFeneconCommercial40Impl.ChannelId.STATE_105) //
								.bit(4, EssFeneconCommercial40Impl.ChannelId.STATE_106) //
								.bit(5, EssFeneconCommercial40Impl.ChannelId.STATE_107) //
								.bit(6, EssFeneconCommercial40Impl.ChannelId.STATE_108) //
								.bit(7, EssFeneconCommercial40Impl.ChannelId.STATE_109) //
								.bit(8, EssFeneconCommercial40Impl.ChannelId.STATE_110) //
								.bit(9, EssFeneconCommercial40Impl.ChannelId.STATE_111) //
								.bit(10, EssFeneconCommercial40Impl.ChannelId.STATE_112) //
								.bit(11, EssFeneconCommercial40Impl.ChannelId.STATE_113) //
								.bit(12, EssFeneconCommercial40Impl.ChannelId.STATE_114) //
								.bit(13, EssFeneconCommercial40Impl.ChannelId.STATE_115) //
								.bit(14, EssFeneconCommercial40Impl.ChannelId.STATE_116)),
						m(new BitsWordElement(0x0188, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_117) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_118) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_119) //
								.bit(3, EssFeneconCommercial40Impl.ChannelId.STATE_120) //
								.bit(4, EssFeneconCommercial40Impl.ChannelId.STATE_121) //
								.bit(5, EssFeneconCommercial40Impl.ChannelId.STATE_122) //
								.bit(6, EssFeneconCommercial40Impl.ChannelId.STATE_123) //
								.bit(14, EssFeneconCommercial40Impl.ChannelId.STATE_124))),
				new FC3ReadRegistersTask(0x0200, Priority.HIGH, //
						m(EssFeneconCommercial40Impl.ChannelId.BATTERY_VOLTAGE, new SignedWordElement(0x0200),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.BATTERY_CURRENT, new SignedWordElement(0x0201),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.BATTERY_POWER, new SignedWordElement(0x0202),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0x0203, 0x0207),
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0x0208).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0x020A).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0x020C, 0x020F), //
						m(EssFeneconCommercial40Impl.ChannelId.GRID_ACTIVE_POWER, new SignedWordElement(0x0210),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x0211),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.APPARENT_POWER, new UnsignedWordElement(0x0212),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.CURRENT_L1, new SignedWordElement(0x0213),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.CURRENT_L2, new SignedWordElement(0x0214),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.CURRENT_L3, new SignedWordElement(0x0215),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0x0216, 0x218), //
						m(EssFeneconCommercial40Impl.ChannelId.VOLTAGE_L1, new UnsignedWordElement(0x0219),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.VOLTAGE_L2, new UnsignedWordElement(0x021A),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.VOLTAGE_L3, new UnsignedWordElement(0x021B),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.FREQUENCY, new UnsignedWordElement(0x021C)), //
						new DummyRegisterElement(0x021D, 0x0221), //
						m(EssFeneconCommercial40Impl.ChannelId.INVERTER_VOLTAGE_L1, new UnsignedWordElement(0x0222),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.INVERTER_VOLTAGE_L2, new UnsignedWordElement(0x0223),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.INVERTER_VOLTAGE_L3, new UnsignedWordElement(0x0224),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.INVERTER_CURRENT_L1, new SignedWordElement(0x0225),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.INVERTER_CURRENT_L2, new SignedWordElement(0x0226),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.INVERTER_CURRENT_L3, new SignedWordElement(0x0227),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x0228),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0x0229, 0x022F), //
						m(ChannelId.ORIGINAL_ALLOWED_CHARGE_POWER, new SignedWordElement(0x0230),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(ChannelId.ORIGINAL_ALLOWED_DISCHARGE_POWER, new UnsignedWordElement(0x0231),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER, new UnsignedWordElement(0x0232),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						new DummyRegisterElement(0x0233, 0x23F),
						m(EssFeneconCommercial40Impl.ChannelId.IPM_TEMPERATURE_L1, new SignedWordElement(0x0240)), //
						m(EssFeneconCommercial40Impl.ChannelId.IPM_TEMPERATURE_L2, new SignedWordElement(0x0241)), //
						m(EssFeneconCommercial40Impl.ChannelId.IPM_TEMPERATURE_L3, new SignedWordElement(0x0242)), //
						new DummyRegisterElement(0x0243, 0x0248), //
						m(EssFeneconCommercial40Impl.ChannelId.TRANSFORMER_TEMPERATURE_L2,
								new SignedWordElement(0x0249))), //
				new FC16WriteRegistersTask(0x0500, //
						m(EssFeneconCommercial40Impl.ChannelId.SET_WORK_STATE, new UnsignedWordElement(0x0500))), //
				new FC16WriteRegistersTask(0x0501, //
						m(EssFeneconCommercial40Impl.ChannelId.SET_ACTIVE_POWER, new SignedWordElement(0x0501),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.SET_REACTIVE_POWER, new SignedWordElement(0x0502),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(EssFeneconCommercial40Impl.ChannelId.SET_PV_POWER_LIMIT, new UnsignedWordElement(0x0503),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(0xA000, Priority.LOW, //
						m(EssFeneconCommercial40Impl.ChannelId.BMS_DCDC_WORK_STATE, new UnsignedWordElement(0xA000)), //
						m(EssFeneconCommercial40Impl.ChannelId.BMS_DCDC_WORK_MODE, new UnsignedWordElement(0xA001))), //
				new FC3ReadRegistersTask(0xA100, Priority.LOW, //
						m(new BitsWordElement(0xA100, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_125) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_126) //
								.bit(6, EssFeneconCommercial40Impl.ChannelId.STATE_127) //
								.bit(7, EssFeneconCommercial40Impl.ChannelId.STATE_128) //
								.bit(8, EssFeneconCommercial40Impl.ChannelId.STATE_129) //
								.bit(9, EssFeneconCommercial40Impl.ChannelId.STATE_130)),
						m(new BitsWordElement(0xA101, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_131) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_132) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_133) //
								.bit(3, EssFeneconCommercial40Impl.ChannelId.STATE_134) //
								.bit(4, EssFeneconCommercial40Impl.ChannelId.STATE_135) //
								.bit(5, EssFeneconCommercial40Impl.ChannelId.STATE_136) //
								.bit(6, EssFeneconCommercial40Impl.ChannelId.STATE_137) //
								.bit(7, EssFeneconCommercial40Impl.ChannelId.STATE_138) //
								.bit(8, EssFeneconCommercial40Impl.ChannelId.STATE_139) //
								.bit(9, EssFeneconCommercial40Impl.ChannelId.STATE_140) //
								.bit(10, EssFeneconCommercial40Impl.ChannelId.STATE_141) //
								.bit(11, EssFeneconCommercial40Impl.ChannelId.STATE_142) //
								.bit(12, EssFeneconCommercial40Impl.ChannelId.STATE_143) //
								.bit(13, EssFeneconCommercial40Impl.ChannelId.STATE_144) //
								.bit(14, EssFeneconCommercial40Impl.ChannelId.STATE_145) //
								.bit(15, EssFeneconCommercial40Impl.ChannelId.STATE_146)),
						m(new BitsWordElement(0xA102, this) //
								.bit(0, EssFeneconCommercial40Impl.ChannelId.STATE_147) //
								.bit(1, EssFeneconCommercial40Impl.ChannelId.STATE_148) //
								.bit(2, EssFeneconCommercial40Impl.ChannelId.STATE_149))),
				new FC3ReadRegistersTask(0x1500, Priority.LOW,
						m(EssFeneconCommercial40Impl.ChannelId.CELL_1_VOLTAGE, new UnsignedWordElement(0x1500)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_2_VOLTAGE, new UnsignedWordElement(0x1501)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_3_VOLTAGE, new UnsignedWordElement(0x1502)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_4_VOLTAGE, new UnsignedWordElement(0x1503)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_5_VOLTAGE, new UnsignedWordElement(0x1504)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_6_VOLTAGE, new UnsignedWordElement(0x1505)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_7_VOLTAGE, new UnsignedWordElement(0x1506)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_8_VOLTAGE, new UnsignedWordElement(0x1507)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_9_VOLTAGE, new UnsignedWordElement(0x1508)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_10_VOLTAGE, new UnsignedWordElement(0x1509)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_11_VOLTAGE, new UnsignedWordElement(0x150A)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_12_VOLTAGE, new UnsignedWordElement(0x150B)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_13_VOLTAGE, new UnsignedWordElement(0x150C)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_14_VOLTAGE, new UnsignedWordElement(0x150D)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_15_VOLTAGE, new UnsignedWordElement(0x150E)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_16_VOLTAGE, new UnsignedWordElement(0x150F)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_17_VOLTAGE, new UnsignedWordElement(0x1510)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_18_VOLTAGE, new UnsignedWordElement(0x1511)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_19_VOLTAGE, new UnsignedWordElement(0x1512)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_20_VOLTAGE, new UnsignedWordElement(0x1513)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_21_VOLTAGE, new UnsignedWordElement(0x1514)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_22_VOLTAGE, new UnsignedWordElement(0x1515)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_23_VOLTAGE, new UnsignedWordElement(0x1516)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_24_VOLTAGE, new UnsignedWordElement(0x1517)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_25_VOLTAGE, new UnsignedWordElement(0x1518)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_26_VOLTAGE, new UnsignedWordElement(0x1519)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_27_VOLTAGE, new UnsignedWordElement(0x151A)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_28_VOLTAGE, new UnsignedWordElement(0x151B)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_29_VOLTAGE, new UnsignedWordElement(0x151C)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_30_VOLTAGE, new UnsignedWordElement(0x151D)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_31_VOLTAGE, new UnsignedWordElement(0x151E)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_32_VOLTAGE, new UnsignedWordElement(0x151F)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_33_VOLTAGE, new UnsignedWordElement(0x1520)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_34_VOLTAGE, new UnsignedWordElement(0x1521)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_35_VOLTAGE, new UnsignedWordElement(0x1522)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_36_VOLTAGE, new UnsignedWordElement(0x1523)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_37_VOLTAGE, new UnsignedWordElement(0x1524)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_38_VOLTAGE, new UnsignedWordElement(0x1525)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_39_VOLTAGE, new UnsignedWordElement(0x1526)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_40_VOLTAGE, new UnsignedWordElement(0x1527)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_41_VOLTAGE, new UnsignedWordElement(0x1528)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_42_VOLTAGE, new UnsignedWordElement(0x1529)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_43_VOLTAGE, new UnsignedWordElement(0x152A)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_44_VOLTAGE, new UnsignedWordElement(0x152B)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_45_VOLTAGE, new UnsignedWordElement(0x152C)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_46_VOLTAGE, new UnsignedWordElement(0x152D)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_47_VOLTAGE, new UnsignedWordElement(0x152E)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_48_VOLTAGE, new UnsignedWordElement(0x152F)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_49_VOLTAGE, new UnsignedWordElement(0x1530)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_50_VOLTAGE, new UnsignedWordElement(0x1531)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_51_VOLTAGE, new UnsignedWordElement(0x1532)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_52_VOLTAGE, new UnsignedWordElement(0x1533)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_53_VOLTAGE, new UnsignedWordElement(0x1534)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_54_VOLTAGE, new UnsignedWordElement(0x1535)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_55_VOLTAGE, new UnsignedWordElement(0x1536)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_56_VOLTAGE, new UnsignedWordElement(0x1537)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_57_VOLTAGE, new UnsignedWordElement(0x1538)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_58_VOLTAGE, new UnsignedWordElement(0x1539)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_59_VOLTAGE, new UnsignedWordElement(0x153A)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_60_VOLTAGE, new UnsignedWordElement(0x153B)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_61_VOLTAGE, new UnsignedWordElement(0x153C)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_62_VOLTAGE, new UnsignedWordElement(0x153D)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_63_VOLTAGE, new UnsignedWordElement(0x153E)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_64_VOLTAGE, new UnsignedWordElement(0x153F)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_65_VOLTAGE, new UnsignedWordElement(0x1540)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_66_VOLTAGE, new UnsignedWordElement(0x1541)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_67_VOLTAGE, new UnsignedWordElement(0x1542)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_68_VOLTAGE, new UnsignedWordElement(0x1543)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_69_VOLTAGE, new UnsignedWordElement(0x1544)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_70_VOLTAGE, new UnsignedWordElement(0x1545)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_71_VOLTAGE, new UnsignedWordElement(0x1546)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_72_VOLTAGE, new UnsignedWordElement(0x1547)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_73_VOLTAGE, new UnsignedWordElement(0x1548)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_74_VOLTAGE, new UnsignedWordElement(0x1549)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_75_VOLTAGE, new UnsignedWordElement(0x154A)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_76_VOLTAGE, new UnsignedWordElement(0x154B)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_77_VOLTAGE, new UnsignedWordElement(0x154C)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_78_VOLTAGE, new UnsignedWordElement(0x154D)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_79_VOLTAGE, new UnsignedWordElement(0x154E)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_80_VOLTAGE, new UnsignedWordElement(0x154F))),
				new FC3ReadRegistersTask(0x1550, Priority.LOW,
						m(EssFeneconCommercial40Impl.ChannelId.CELL_81_VOLTAGE, new UnsignedWordElement(0x1550)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_82_VOLTAGE, new UnsignedWordElement(0x1551)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_83_VOLTAGE, new UnsignedWordElement(0x1552)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_84_VOLTAGE, new UnsignedWordElement(0x1553)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_85_VOLTAGE, new UnsignedWordElement(0x1554)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_86_VOLTAGE, new UnsignedWordElement(0x1555)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_87_VOLTAGE, new UnsignedWordElement(0x1556)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_88_VOLTAGE, new UnsignedWordElement(0x1557)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_89_VOLTAGE, new UnsignedWordElement(0x1558)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_90_VOLTAGE, new UnsignedWordElement(0x1559)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_91_VOLTAGE, new UnsignedWordElement(0x155A)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_92_VOLTAGE, new UnsignedWordElement(0x155B)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_93_VOLTAGE, new UnsignedWordElement(0x155C)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_94_VOLTAGE, new UnsignedWordElement(0x155D)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_95_VOLTAGE, new UnsignedWordElement(0x155E)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_96_VOLTAGE, new UnsignedWordElement(0x155F)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_97_VOLTAGE, new UnsignedWordElement(0x1560)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_98_VOLTAGE, new UnsignedWordElement(0x1561)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_99_VOLTAGE, new UnsignedWordElement(0x1562)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_100_VOLTAGE, new UnsignedWordElement(0x1563)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_101_VOLTAGE, new UnsignedWordElement(0x1564)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_102_VOLTAGE, new UnsignedWordElement(0x1565)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_103_VOLTAGE, new UnsignedWordElement(0x1566)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_104_VOLTAGE, new UnsignedWordElement(0x1567)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_105_VOLTAGE, new UnsignedWordElement(0x1568)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_106_VOLTAGE, new UnsignedWordElement(0x1569)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_107_VOLTAGE, new UnsignedWordElement(0x156A)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_108_VOLTAGE, new UnsignedWordElement(0x156B)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_109_VOLTAGE, new UnsignedWordElement(0x156C)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_110_VOLTAGE, new UnsignedWordElement(0x156D)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_111_VOLTAGE, new UnsignedWordElement(0x156E)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_112_VOLTAGE, new UnsignedWordElement(0x156F)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_113_VOLTAGE, new UnsignedWordElement(0x1570)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_114_VOLTAGE, new UnsignedWordElement(0x1571)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_115_VOLTAGE, new UnsignedWordElement(0x1572)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_116_VOLTAGE, new UnsignedWordElement(0x1573)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_117_VOLTAGE, new UnsignedWordElement(0x1574)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_118_VOLTAGE, new UnsignedWordElement(0x1575)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_119_VOLTAGE, new UnsignedWordElement(0x1576)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_120_VOLTAGE, new UnsignedWordElement(0x1577)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_121_VOLTAGE, new UnsignedWordElement(0x1578)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_122_VOLTAGE, new UnsignedWordElement(0x1579)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_123_VOLTAGE, new UnsignedWordElement(0x157A)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_124_VOLTAGE, new UnsignedWordElement(0x157B)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_125_VOLTAGE, new UnsignedWordElement(0x157C)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_126_VOLTAGE, new UnsignedWordElement(0x157D)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_127_VOLTAGE, new UnsignedWordElement(0x157E)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_128_VOLTAGE, new UnsignedWordElement(0x157F)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_129_VOLTAGE, new UnsignedWordElement(0x1580)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_130_VOLTAGE, new UnsignedWordElement(0x1581)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_131_VOLTAGE, new UnsignedWordElement(0x1582)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_132_VOLTAGE, new UnsignedWordElement(0x1583)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_133_VOLTAGE, new UnsignedWordElement(0x1584)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_134_VOLTAGE, new UnsignedWordElement(0x1585)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_135_VOLTAGE, new UnsignedWordElement(0x1586)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_136_VOLTAGE, new UnsignedWordElement(0x1587)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_137_VOLTAGE, new UnsignedWordElement(0x1588)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_138_VOLTAGE, new UnsignedWordElement(0x1589)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_139_VOLTAGE, new UnsignedWordElement(0x158A)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_140_VOLTAGE, new UnsignedWordElement(0x158B)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_141_VOLTAGE, new UnsignedWordElement(0x158C)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_142_VOLTAGE, new UnsignedWordElement(0x158D)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_143_VOLTAGE, new UnsignedWordElement(0x158E)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_144_VOLTAGE, new UnsignedWordElement(0x158F)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_145_VOLTAGE, new UnsignedWordElement(0x1590)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_146_VOLTAGE, new UnsignedWordElement(0x1591)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_147_VOLTAGE, new UnsignedWordElement(0x1592)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_148_VOLTAGE, new UnsignedWordElement(0x1593)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_149_VOLTAGE, new UnsignedWordElement(0x1594)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_150_VOLTAGE, new UnsignedWordElement(0x1595)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_151_VOLTAGE, new UnsignedWordElement(0x1596)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_152_VOLTAGE, new UnsignedWordElement(0x1597)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_153_VOLTAGE, new UnsignedWordElement(0x1598)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_154_VOLTAGE, new UnsignedWordElement(0x1599)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_155_VOLTAGE, new UnsignedWordElement(0x159A)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_156_VOLTAGE, new UnsignedWordElement(0x159B)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_157_VOLTAGE, new UnsignedWordElement(0x159C)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_158_VOLTAGE, new UnsignedWordElement(0x159D)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_159_VOLTAGE, new UnsignedWordElement(0x159E)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_160_VOLTAGE, new UnsignedWordElement(0x159F))),
				new FC3ReadRegistersTask(0x15A0, Priority.LOW,
						m(EssFeneconCommercial40Impl.ChannelId.CELL_161_VOLTAGE, new UnsignedWordElement(0x15A0)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_162_VOLTAGE, new UnsignedWordElement(0x15A1)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_163_VOLTAGE, new UnsignedWordElement(0x15A2)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_164_VOLTAGE, new UnsignedWordElement(0x15A3)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_165_VOLTAGE, new UnsignedWordElement(0x15A4)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_166_VOLTAGE, new UnsignedWordElement(0x15A5)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_167_VOLTAGE, new UnsignedWordElement(0x15A6)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_168_VOLTAGE, new UnsignedWordElement(0x15A7)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_169_VOLTAGE, new UnsignedWordElement(0x15A8)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_170_VOLTAGE, new UnsignedWordElement(0x15A9)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_171_VOLTAGE, new UnsignedWordElement(0x15AA)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_172_VOLTAGE, new UnsignedWordElement(0x15AB)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_173_VOLTAGE, new UnsignedWordElement(0x15AC)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_174_VOLTAGE, new UnsignedWordElement(0x15AD)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_175_VOLTAGE, new UnsignedWordElement(0x15AE)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_176_VOLTAGE, new UnsignedWordElement(0x15AF)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_177_VOLTAGE, new UnsignedWordElement(0x15B0)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_178_VOLTAGE, new UnsignedWordElement(0x15B1)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_179_VOLTAGE, new UnsignedWordElement(0x15B2)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_180_VOLTAGE, new UnsignedWordElement(0x15B3)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_181_VOLTAGE, new UnsignedWordElement(0x15B4)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_182_VOLTAGE, new UnsignedWordElement(0x15B5)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_183_VOLTAGE, new UnsignedWordElement(0x15B6)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_184_VOLTAGE, new UnsignedWordElement(0x15B7)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_185_VOLTAGE, new UnsignedWordElement(0x15B8)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_186_VOLTAGE, new UnsignedWordElement(0x15B9)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_187_VOLTAGE, new UnsignedWordElement(0x15BA)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_188_VOLTAGE, new UnsignedWordElement(0x15BB)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_189_VOLTAGE, new UnsignedWordElement(0x15BC)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_190_VOLTAGE, new UnsignedWordElement(0x15BD)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_191_VOLTAGE, new UnsignedWordElement(0x15BE)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_192_VOLTAGE, new UnsignedWordElement(0x15BF)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_193_VOLTAGE, new UnsignedWordElement(0x15C0)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_194_VOLTAGE, new UnsignedWordElement(0x15C1)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_195_VOLTAGE, new UnsignedWordElement(0x15C2)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_196_VOLTAGE, new UnsignedWordElement(0x15C3)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_197_VOLTAGE, new UnsignedWordElement(0x15C4)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_198_VOLTAGE, new UnsignedWordElement(0x15C5)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_199_VOLTAGE, new UnsignedWordElement(0x15C6)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_200_VOLTAGE, new UnsignedWordElement(0x15C7)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_201_VOLTAGE, new UnsignedWordElement(0x15C8)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_202_VOLTAGE, new UnsignedWordElement(0x15C9)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_203_VOLTAGE, new UnsignedWordElement(0x15CA)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_204_VOLTAGE, new UnsignedWordElement(0x15CB)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_205_VOLTAGE, new UnsignedWordElement(0x15CC)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_206_VOLTAGE, new UnsignedWordElement(0x15CD)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_207_VOLTAGE, new UnsignedWordElement(0x15CE)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_208_VOLTAGE, new UnsignedWordElement(0x15CF)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_209_VOLTAGE, new UnsignedWordElement(0x15D0)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_210_VOLTAGE, new UnsignedWordElement(0x15D1)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_211_VOLTAGE, new UnsignedWordElement(0x15D2)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_212_VOLTAGE, new UnsignedWordElement(0x15D3)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_213_VOLTAGE, new UnsignedWordElement(0x15D4)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_214_VOLTAGE, new UnsignedWordElement(0x15D5)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_215_VOLTAGE, new UnsignedWordElement(0x15D6)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_216_VOLTAGE, new UnsignedWordElement(0x15D7)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_217_VOLTAGE, new UnsignedWordElement(0x15D8)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_218_VOLTAGE, new UnsignedWordElement(0x15D9)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_219_VOLTAGE, new UnsignedWordElement(0x15DA)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_220_VOLTAGE, new UnsignedWordElement(0x15DB)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_221_VOLTAGE, new UnsignedWordElement(0x15DC)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_222_VOLTAGE, new UnsignedWordElement(0x15DD)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_223_VOLTAGE, new UnsignedWordElement(0x15DE)),
						m(EssFeneconCommercial40Impl.ChannelId.CELL_224_VOLTAGE, new UnsignedWordElement(0x15DF))),
				new FC3ReadRegistersTask(0x1402, Priority.LOW, //
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(0x1402))));
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridMode().value().asOptionString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.defineWorkState();
			break;
		}
	}

	private LocalDateTime lastDefineWorkState = null;

	private long surplus = 0L;
	private boolean surplusOn = false;
	private int calculatedPower;

	private void defineWorkState() {
		/*
		 * Set ESS in running mode
		 */
		// TODO this should be smarter: set in energy saving mode if there was no output
		// power for a while and we don't need emergency power.
		LocalDateTime now = LocalDateTime.now();
		if (lastDefineWorkState == null || now.minusMinutes(1).isAfter(this.lastDefineWorkState)) {
			this.lastDefineWorkState = now;
			EnumWriteChannel setWorkStateChannel = this.channel(ChannelId.SET_WORK_STATE);
			try {
				setWorkStateChannel.setNextWriteValue(SetWorkState.START);
			} catch (OpenemsNamedException e) {
				logError(this.log, "Unable to start: " + e.getMessage());
			}
		}
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 100; // the modbus field for SetActivePower has the unit 0.1 kW
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsException {
		// Read-Only-Mode
		if (this.config.readOnlyMode()) {
			return new Constraint[] { //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0), //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) //
			};
		}

		// Reactive Power constraints
		List<Constraint> result = new ArrayList<>();
		result.add(this.createPowerConstraint("Commercial40 Min Reactive Power", Phase.ALL, Pwr.REACTIVE,
				Relationship.GREATER_OR_EQUALS, MIN_REACTIVE_POWER));
		result.add(this.createPowerConstraint("Commercial40 Max Reactive Power", Phase.ALL, Pwr.REACTIVE,
				Relationship.LESS_OR_EQUALS, MAX_REACTIVE_POWER));

		// Activate Surplus-Feed-In?
		result.addAll(this.getSurplusFeedInConstraints());

		return result.toArray(new Constraint[result.size()]);
	}

	/**
	 * Gets Constraints for Surplus-Feed-In; empty list if no Constraints are added.
	 * 
	 * @return list of Constraints
	 * @throws OpenemsException
	 */
	private List<Constraint> getSurplusFeedInConstraints() throws OpenemsException {
		if (this.lastSurplusFeedInActivated == null
				|| this.lastSurplusFeedInActivated.isBefore(LocalDateTime.now().minusMinutes(10))) {
			if (
			// Is a Charger set? (i.e. is this a Commercial 40-40 DC)
			this.charger == null
					// Is Surplus Feed-In not activated?
					|| !this.config.activateSurplusFeedIn()
					// Is battery not full?
					|| this.getAllowedCharge().value().orElse(0) < this.config.surplusAllowedChargePowerLimit()
					// Is time before 'Surplus Off Time'?
					|| LocalTime.now().isAfter(LocalTime.parse(this.config.surplusOffTime()))
					// Is PV producing?
					|| Math.max(//
							// InputVoltage 0
							((IntegerReadChannel) this.charger
									.channel(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC0_INPUT_VOLTAGE)).value()
											.orElse(0), //
							// InputVoltage 1
							((IntegerReadChannel) this.charger
									.channel(EssDcChargerFeneconCommercial40.ChannelId.PV_DCDC1_INPUT_VOLTAGE)).value()
											.orElse(0) //
					) < 250_000) {
				this.lastSurplusFeedInActivated = null;
				this.channel(ChannelId.SURPLUS_FEED_IN_POWER).setNextValue(0);
				return new ArrayList<>();
			}
		}
		// Active Surplus feed-in
		int surplusFeedInPower = this.charger.getActualPower().value().orElse(0);

		List<Constraint> result = new ArrayList<>();
		result.add(this.createPowerConstraint("Enforce Surplus Feed-In", Phase.ALL, Pwr.ACTIVE,
				Relationship.GREATER_OR_EQUALS, surplusFeedInPower));
		this.channel(ChannelId.SURPLUS_FEED_IN_POWER).setNextValue(surplusFeedInPower);

		if (this.lastSurplusFeedInActivated == null) {
			this.lastSurplusFeedInActivated = LocalDateTime.now();
		}
		return result;
	}

	private LocalDateTime lastSurplusFeedInActivated = null;

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public void setCharger(EssDcChargerFeneconCommercial40 charger) {
		this.charger = charger;
	}

}
