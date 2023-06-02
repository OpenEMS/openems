package io.openems.edge.ess.fenecon.commercial40;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.fenecon.commercial40.charger.EssDcChargerFeneconCommercial40;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Fenecon.Commercial40", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})
public class EssFeneconCommercial40Impl extends AbstractOpenemsModbusComponent
		implements EssFeneconCommercial40, ManagedSymmetricEss, SymmetricEss, HybridEss, ModbusComponent,
		OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {

	protected static final int MAX_APPARENT_POWER = 40000;
	protected static final int NET_CAPACITY = 40000;

	private static final int UNIT_ID = 100;
	private static final int MIN_REACTIVE_POWER = -10000;
	private static final int MAX_REACTIVE_POWER = 10000;

	private final Logger log = LoggerFactory.getLogger(EssFeneconCommercial40Impl.class);

	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateAcDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_DISCHARGE_ENERGY);
	private final List<EssDcChargerFeneconCommercial40> chargers = new ArrayList<>();
	private final SurplusFeedInHandler surplusFeedInHandler = new SurplusFeedInHandler(this);

	@Reference
	protected ComponentManager componentManager;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config;

	public EssFeneconCommercial40Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				EssFeneconCommercial40.SystemErrorChannelId.values(), //
				EssFeneconCommercial40.InsufficientGridParametersChannelId.values(), //
				EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.values(), //
				EssFeneconCommercial40.ChannelId.values() //
		);
		this._setCapacity(NET_CAPACITY);
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		if (this.config.readOnlyMode()) {
			return;
		}

		IntegerWriteChannel setActivePowerChannel = this.channel(EssFeneconCommercial40.ChannelId.SET_ACTIVE_POWER);
		setActivePowerChannel.setNextWriteValue(activePower);
		IntegerWriteChannel setReactivePowerChannel = this.channel(EssFeneconCommercial40.ChannelId.SET_REACTIVE_POWER);
		setReactivePowerChannel.setNextWriteValue(reactivePower);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0x0101, Priority.LOW, //
						m(EssFeneconCommercial40.ChannelId.SYSTEM_STATE, new UnsignedWordElement(0x0101)),
						m(EssFeneconCommercial40.ChannelId.CONTROL_MODE, new UnsignedWordElement(0x0102)),
						new DummyRegisterElement(0x0103), // WorkMode: RemoteDispatch
						m(EssFeneconCommercial40.ChannelId.BATTERY_MAINTENANCE_STATE, new UnsignedWordElement(0x0104)),
						m(EssFeneconCommercial40.ChannelId.INVERTER_STATE, new UnsignedWordElement(0x0105)),
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(0x0106), //
								new ElementToChannelConverter(value -> {
									var intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
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
						m(EssFeneconCommercial40.ChannelId.PROTOCOL_VERSION, new UnsignedWordElement(0x0108)),
						m(EssFeneconCommercial40.ChannelId.SYSTEM_MANUFACTURER, new UnsignedWordElement(0x0109)),
						m(EssFeneconCommercial40.ChannelId.SYSTEM_TYPE, new UnsignedWordElement(0x010A)),
						new DummyRegisterElement(0x010B, 0x010F), //
						m(new BitsWordElement(0x0110, this) //
								.bit(2, EssFeneconCommercial40.ChannelId.EMERGENCY_STOP_ACTIVATED) //
								.bit(6, EssFeneconCommercial40.ChannelId.KEY_MANUAL_ACTIVATED)),
						m(new BitsWordElement(0x0111, this) //
								.bit(3, EssFeneconCommercial40.SystemErrorChannelId.STATE_2) //
								.bit(12, EssFeneconCommercial40.SystemErrorChannelId.STATE_3)),
						new DummyRegisterElement(0x0112, 0x0124), //
						m(new BitsWordElement(0x0125, this) //
								.bit(0, EssFeneconCommercial40.SystemErrorChannelId.STATE_4) //
								.bit(1, EssFeneconCommercial40.SystemErrorChannelId.STATE_5) //
								.bit(2, EssFeneconCommercial40.SystemErrorChannelId.STATE_6) //
								.bit(4, EssFeneconCommercial40.SystemErrorChannelId.STATE_7) //
								.bit(8, EssFeneconCommercial40.SystemErrorChannelId.STATE_8) //
								.bit(9, EssFeneconCommercial40.SystemErrorChannelId.STATE_9)),
						m(new BitsWordElement(0x0126, this) //
								.bit(3, EssFeneconCommercial40.SystemErrorChannelId.STATE_10)), //
						new DummyRegisterElement(0x0127, 0x014F), //
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_SWITCH_STATE,
								new UnsignedWordElement(0x0150))), //
				new FC3ReadRegistersTask(0x0180, Priority.LOW, //
						m(new BitsWordElement(0x0180, this) //
								.bit(0, EssFeneconCommercial40.SystemErrorChannelId.STATE_11) //
								.bit(1, EssFeneconCommercial40.SystemErrorChannelId.STATE_12) //
								.bit(2, EssFeneconCommercial40.SystemErrorChannelId.STATE_13) //
								.bit(3, EssFeneconCommercial40.SystemErrorChannelId.STATE_14) //
								.bit(4, EssFeneconCommercial40.SystemErrorChannelId.STATE_15) //
								.bit(5, EssFeneconCommercial40.SystemErrorChannelId.STATE_16) //
								.bit(6, EssFeneconCommercial40.SystemErrorChannelId.STATE_17) //
								.bit(7, EssFeneconCommercial40.SystemErrorChannelId.STATE_18) //
								.bit(8, EssFeneconCommercial40.SystemErrorChannelId.STATE_19) //
								.bit(9, EssFeneconCommercial40.SystemErrorChannelId.STATE_20) //
								.bit(10, EssFeneconCommercial40.SystemErrorChannelId.STATE_21) //
								.bit(11, EssFeneconCommercial40.SystemErrorChannelId.STATE_22) //
								.bit(12, EssFeneconCommercial40.SystemErrorChannelId.STATE_23)),
						new DummyRegisterElement(0x0181), //
						m(new BitsWordElement(0x0182, this) //
								.bit(0, EssFeneconCommercial40.SystemErrorChannelId.STATE_24) //
								.bit(1, EssFeneconCommercial40.SystemErrorChannelId.STATE_25) //
								.bit(2, EssFeneconCommercial40.SystemErrorChannelId.STATE_26) //
								.bit(3, EssFeneconCommercial40.ChannelId.BECU_UNIT_DEFECTIVE) //
								.bit(4, EssFeneconCommercial40.SystemErrorChannelId.STATE_28) //
								.bit(5, EssFeneconCommercial40.SystemErrorChannelId.STATE_29) //
								.bit(6, EssFeneconCommercial40.SystemErrorChannelId.STATE_30) //
								.bit(7, EssFeneconCommercial40.SystemErrorChannelId.STATE_31) //
								.bit(8, EssFeneconCommercial40.SystemErrorChannelId.STATE_32) //
								.bit(9, EssFeneconCommercial40.SystemErrorChannelId.STATE_33) //
								.bit(10, EssFeneconCommercial40.SystemErrorChannelId.STATE_34) //
								.bit(11, EssFeneconCommercial40.SystemErrorChannelId.STATE_35) //
								.bit(12, EssFeneconCommercial40.SystemErrorChannelId.STATE_36) //
								.bit(13, EssFeneconCommercial40.SystemErrorChannelId.STATE_37) //
								.bit(14, EssFeneconCommercial40.SystemErrorChannelId.STATE_38) //
								.bit(15, EssFeneconCommercial40.SystemErrorChannelId.STATE_39)),
						m(new BitsWordElement(0x0183, this) //
								.bit(0, EssFeneconCommercial40.SystemErrorChannelId.STATE_40) //
								.bit(1, EssFeneconCommercial40.SystemErrorChannelId.STATE_41) //
								.bit(2, EssFeneconCommercial40.SystemErrorChannelId.STATE_42) //
								.bit(3, EssFeneconCommercial40.SystemErrorChannelId.STATE_43) //
								.bit(4, EssFeneconCommercial40.SystemErrorChannelId.STATE_44) //
								.bit(5, EssFeneconCommercial40.SystemErrorChannelId.STATE_45) //
								.bit(6, EssFeneconCommercial40.SystemErrorChannelId.STATE_46) //
								.bit(7, EssFeneconCommercial40.SystemErrorChannelId.STATE_47) //
								.bit(8, EssFeneconCommercial40.SystemErrorChannelId.STATE_48) //
								.bit(9, EssFeneconCommercial40.SystemErrorChannelId.STATE_49) //
								.bit(10, EssFeneconCommercial40.SystemErrorChannelId.STATE_50) //
								.bit(11, EssFeneconCommercial40.SystemErrorChannelId.STATE_51) //
								.bit(12, EssFeneconCommercial40.SystemErrorChannelId.STATE_52) //
								.bit(13, EssFeneconCommercial40.SystemErrorChannelId.STATE_53) //
								.bit(14, EssFeneconCommercial40.SystemErrorChannelId.STATE_54) //
								.bit(15, EssFeneconCommercial40.SystemErrorChannelId.STATE_55)),
						m(new BitsWordElement(0x0184, this) //
								.bit(0, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_56) //
								.bit(1, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_57) //
								.bit(2, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_58) //
								.bit(3, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_59) //
								.bit(4, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_60) //
								.bit(5, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_61) //
								.bit(6, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_62) //
								.bit(7, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_63) //
								.bit(8, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_64) //
								.bit(9, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_65) //
								.bit(10, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_66) //
								.bit(11, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_67) //
								.bit(12, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_68) //
								.bit(13, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_69) //
								.bit(14, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_70) //
								.bit(15, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_71)),
						m(new BitsWordElement(0x0185, this) //
								.bit(0, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_72) //
								.bit(1, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_73) //
								.bit(2, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_74) //
								.bit(3, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_75) //
								.bit(4, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_76) //
								.bit(5, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_77) //
								.bit(6, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_78) //
								.bit(7, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_79) //
								.bit(8, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_80) //
								.bit(9, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_81) //
								.bit(10, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_82) //
								.bit(11, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_83) //
								.bit(12, EssFeneconCommercial40.InsufficientGridParametersChannelId.STATE_84) //
								.bit(13, EssFeneconCommercial40.SystemErrorChannelId.STATE_85)),
						m(new BitsWordElement(0x0186, this) //
								.bit(0, EssFeneconCommercial40.SystemErrorChannelId.STATE_86) //
								.bit(1, EssFeneconCommercial40.SystemErrorChannelId.STATE_87) //
								.bit(2, EssFeneconCommercial40.SystemErrorChannelId.STATE_88) //
								.bit(3, EssFeneconCommercial40.SystemErrorChannelId.STATE_89) //
								.bit(4, EssFeneconCommercial40.SystemErrorChannelId.STATE_90) //
								.bit(5, EssFeneconCommercial40.SystemErrorChannelId.STATE_91) //
								.bit(6, EssFeneconCommercial40.SystemErrorChannelId.STATE_92) //
								.bit(7, EssFeneconCommercial40.SystemErrorChannelId.STATE_93) //
								.bit(8, EssFeneconCommercial40.SystemErrorChannelId.STATE_94) //
								.bit(9, EssFeneconCommercial40.SystemErrorChannelId.STATE_95) //
								.bit(10, EssFeneconCommercial40.SystemErrorChannelId.STATE_96) //
								.bit(11, EssFeneconCommercial40.SystemErrorChannelId.STATE_97) //
								.bit(12, EssFeneconCommercial40.SystemErrorChannelId.STATE_98) //
								.bit(13, EssFeneconCommercial40.SystemErrorChannelId.STATE_99) //
								.bit(14, EssFeneconCommercial40.SystemErrorChannelId.STATE_100) //
								.bit(15, EssFeneconCommercial40.SystemErrorChannelId.STATE_101)),
						m(new BitsWordElement(0x0187, this) //
								.bit(0, EssFeneconCommercial40.SystemErrorChannelId.STATE_102) //
								.bit(1, EssFeneconCommercial40.SystemErrorChannelId.STATE_103) //
								.bit(2, EssFeneconCommercial40.SystemErrorChannelId.STATE_104) //
								.bit(3, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_105) //
								.bit(4, EssFeneconCommercial40.SystemErrorChannelId.STATE_106) //
								.bit(5, EssFeneconCommercial40.SystemErrorChannelId.STATE_107) //
								.bit(6, EssFeneconCommercial40.SystemErrorChannelId.STATE_108) //
								.bit(7, EssFeneconCommercial40.SystemErrorChannelId.STATE_109) //
								.bit(8, EssFeneconCommercial40.SystemErrorChannelId.STATE_110) //
								.bit(9, EssFeneconCommercial40.SystemErrorChannelId.STATE_111) //
								.bit(10, EssFeneconCommercial40.SystemErrorChannelId.STATE_112) //
								.bit(11, EssFeneconCommercial40.SystemErrorChannelId.STATE_113) //
								.bit(12, EssFeneconCommercial40.SystemErrorChannelId.STATE_114) //
								.bit(13, EssFeneconCommercial40.SystemErrorChannelId.STATE_115) //
								.bit(14, EssFeneconCommercial40.SystemErrorChannelId.STATE_116)),
						m(new BitsWordElement(0x0188, this) //
								.bit(0, EssFeneconCommercial40.SystemErrorChannelId.STATE_117) //
								.bit(1, EssFeneconCommercial40.SystemErrorChannelId.STATE_118) //
								.bit(2, EssFeneconCommercial40.SystemErrorChannelId.STATE_119) //
								.bit(3, EssFeneconCommercial40.SystemErrorChannelId.STATE_120) //
								.bit(4, EssFeneconCommercial40.SystemErrorChannelId.STATE_121) //
								.bit(5, EssFeneconCommercial40.SystemErrorChannelId.STATE_122) //
								.bit(6, EssFeneconCommercial40.SystemErrorChannelId.STATE_123) //
								.bit(14, EssFeneconCommercial40.SystemErrorChannelId.STATE_124))),
				new FC3ReadRegistersTask(0x0200, Priority.HIGH, //
						m(EssFeneconCommercial40.ChannelId.BATTERY_VOLTAGE, new SignedWordElement(0x0200),
								SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.BATTERY_CURRENT, new SignedWordElement(0x0201),
								SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.BATTERY_POWER, new SignedWordElement(0x0202),
								SCALE_FACTOR_2), //
						new DummyRegisterElement(0x0203, 0x0207),
						m(EssFeneconCommercial40.ChannelId.ORIGINAL_ACTIVE_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0x0208).wordOrder(WordOrder.LSWMSW), SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.ORIGINAL_ACTIVE_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0x020A).wordOrder(WordOrder.LSWMSW), SCALE_FACTOR_2), //
						new DummyRegisterElement(0x020C, 0x020F), //
						m(EssFeneconCommercial40.ChannelId.GRID_ACTIVE_POWER, new SignedWordElement(0x0210),
								SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.REACTIVE_POWER, new SignedWordElement(0x0211), SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.APPARENT_POWER, new UnsignedWordElement(0x0212),
								SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.CURRENT_L1, new SignedWordElement(0x0213), SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.CURRENT_L2, new SignedWordElement(0x0214), SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.CURRENT_L3, new SignedWordElement(0x0215), SCALE_FACTOR_2), //
						new DummyRegisterElement(0x0216, 0x218), //
						m(EssFeneconCommercial40.ChannelId.VOLTAGE_L1, new UnsignedWordElement(0x0219), SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.VOLTAGE_L2, new UnsignedWordElement(0x021A), SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.VOLTAGE_L3, new UnsignedWordElement(0x021B), SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.FREQUENCY, new UnsignedWordElement(0x021C)), //
						new DummyRegisterElement(0x021D, 0x0221), //
						m(EssFeneconCommercial40.ChannelId.INVERTER_VOLTAGE_L1, new UnsignedWordElement(0x0222),
								SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.INVERTER_VOLTAGE_L2, new UnsignedWordElement(0x0223),
								SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.INVERTER_VOLTAGE_L3, new UnsignedWordElement(0x0224),
								SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.INVERTER_CURRENT_L1, new SignedWordElement(0x0225),
								SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.INVERTER_CURRENT_L2, new SignedWordElement(0x0226),
								SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.INVERTER_CURRENT_L3, new SignedWordElement(0x0227),
								SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(0x0228), SCALE_FACTOR_2), //
						new DummyRegisterElement(0x0229, 0x022F), //
						m(EssFeneconCommercial40.ChannelId.ORIGINAL_ALLOWED_CHARGE_POWER, new SignedWordElement(0x0230),
								SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.ORIGINAL_ALLOWED_DISCHARGE_POWER,
								new UnsignedWordElement(0x0231), SCALE_FACTOR_2), //
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER, new UnsignedWordElement(0x0232), SCALE_FACTOR_2), //
						new DummyRegisterElement(0x0233, 0x23F),
						m(EssFeneconCommercial40.ChannelId.IPM_TEMPERATURE_L1, new SignedWordElement(0x0240)), //
						m(EssFeneconCommercial40.ChannelId.IPM_TEMPERATURE_L2, new SignedWordElement(0x0241)), //
						m(EssFeneconCommercial40.ChannelId.IPM_TEMPERATURE_L3, new SignedWordElement(0x0242)), //
						new DummyRegisterElement(0x0243, 0x0248), //
						m(EssFeneconCommercial40.ChannelId.TRANSFORMER_TEMPERATURE_L2, new SignedWordElement(0x0249))), //
				new FC16WriteRegistersTask(0x0500, //
						m(EssFeneconCommercial40.ChannelId.SET_WORK_STATE, new UnsignedWordElement(0x0500))), //
				new FC16WriteRegistersTask(0x0501, //
						m(EssFeneconCommercial40.ChannelId.SET_ACTIVE_POWER, new SignedWordElement(0x0501),
								SCALE_FACTOR_2), //
						m(EssFeneconCommercial40.ChannelId.SET_REACTIVE_POWER, new SignedWordElement(0x0502),
								SCALE_FACTOR_2)), //
				new FC3ReadRegistersTask(0xA000, Priority.LOW, //
						m(EssFeneconCommercial40.ChannelId.BMS_DCDC_WORK_STATE, new UnsignedWordElement(0xA000)), //
						m(EssFeneconCommercial40.ChannelId.BMS_DCDC_WORK_MODE, new UnsignedWordElement(0xA001))), //
				new FC3ReadRegistersTask(0xA100, Priority.LOW, //
						m(new BitsWordElement(0xA100, this) //
								.bit(0, EssFeneconCommercial40.SystemErrorChannelId.STATE_125) //
								.bit(1, EssFeneconCommercial40.SystemErrorChannelId.STATE_126) //
								.bit(6, EssFeneconCommercial40.SystemErrorChannelId.STATE_127) //
								.bit(7, EssFeneconCommercial40.SystemErrorChannelId.STATE_128) //
								.bit(8, EssFeneconCommercial40.SystemErrorChannelId.STATE_129) //
								.bit(9, EssFeneconCommercial40.SystemErrorChannelId.STATE_130)),
						m(new BitsWordElement(0xA101, this) //
								.bit(0, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_131) //
								.bit(1, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_132) //
								.bit(2, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_133) //
								.bit(3, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_134) //
								.bit(4, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_135) //
								.bit(5, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_136) //
								.bit(6, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_137) //
								.bit(7, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_138) //
								.bit(8, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_139) //
								.bit(9, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_140) //
								.bit(10, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_141) //
								.bit(11, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_142) //
								.bit(12, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_143) //
								.bit(13, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_144) //
								.bit(14, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_145) //
								.bit(15, EssFeneconCommercial40.PowerDecreaseCausedByOvertemperatureChannelId.STATE_146)),
						m(new BitsWordElement(0xA102, this) //
								.bit(0, EssFeneconCommercial40.SystemErrorChannelId.STATE_147) //
								.bit(1, EssFeneconCommercial40.SystemErrorChannelId.STATE_148) //
								.bit(2, EssFeneconCommercial40.SystemErrorChannelId.STATE_149))),
				new FC3ReadRegistersTask(0x1500, Priority.LOW,
						m(EssFeneconCommercial40.ChannelId.CELL_1_VOLTAGE, new UnsignedWordElement(0x1500)),
						m(EssFeneconCommercial40.ChannelId.CELL_2_VOLTAGE, new UnsignedWordElement(0x1501)),
						m(EssFeneconCommercial40.ChannelId.CELL_3_VOLTAGE, new UnsignedWordElement(0x1502)),
						m(EssFeneconCommercial40.ChannelId.CELL_4_VOLTAGE, new UnsignedWordElement(0x1503)),
						m(EssFeneconCommercial40.ChannelId.CELL_5_VOLTAGE, new UnsignedWordElement(0x1504)),
						m(EssFeneconCommercial40.ChannelId.CELL_6_VOLTAGE, new UnsignedWordElement(0x1505)),
						m(EssFeneconCommercial40.ChannelId.CELL_7_VOLTAGE, new UnsignedWordElement(0x1506)),
						m(EssFeneconCommercial40.ChannelId.CELL_8_VOLTAGE, new UnsignedWordElement(0x1507)),
						m(EssFeneconCommercial40.ChannelId.CELL_9_VOLTAGE, new UnsignedWordElement(0x1508)),
						m(EssFeneconCommercial40.ChannelId.CELL_10_VOLTAGE, new UnsignedWordElement(0x1509)),
						m(EssFeneconCommercial40.ChannelId.CELL_11_VOLTAGE, new UnsignedWordElement(0x150A)),
						m(EssFeneconCommercial40.ChannelId.CELL_12_VOLTAGE, new UnsignedWordElement(0x150B)),
						m(EssFeneconCommercial40.ChannelId.CELL_13_VOLTAGE, new UnsignedWordElement(0x150C)),
						m(EssFeneconCommercial40.ChannelId.CELL_14_VOLTAGE, new UnsignedWordElement(0x150D)),
						m(EssFeneconCommercial40.ChannelId.CELL_15_VOLTAGE, new UnsignedWordElement(0x150E)),
						m(EssFeneconCommercial40.ChannelId.CELL_16_VOLTAGE, new UnsignedWordElement(0x150F)),
						m(EssFeneconCommercial40.ChannelId.CELL_17_VOLTAGE, new UnsignedWordElement(0x1510)),
						m(EssFeneconCommercial40.ChannelId.CELL_18_VOLTAGE, new UnsignedWordElement(0x1511)),
						m(EssFeneconCommercial40.ChannelId.CELL_19_VOLTAGE, new UnsignedWordElement(0x1512)),
						m(EssFeneconCommercial40.ChannelId.CELL_20_VOLTAGE, new UnsignedWordElement(0x1513)),
						m(EssFeneconCommercial40.ChannelId.CELL_21_VOLTAGE, new UnsignedWordElement(0x1514)),
						m(EssFeneconCommercial40.ChannelId.CELL_22_VOLTAGE, new UnsignedWordElement(0x1515)),
						m(EssFeneconCommercial40.ChannelId.CELL_23_VOLTAGE, new UnsignedWordElement(0x1516)),
						m(EssFeneconCommercial40.ChannelId.CELL_24_VOLTAGE, new UnsignedWordElement(0x1517)),
						m(EssFeneconCommercial40.ChannelId.CELL_25_VOLTAGE, new UnsignedWordElement(0x1518)),
						m(EssFeneconCommercial40.ChannelId.CELL_26_VOLTAGE, new UnsignedWordElement(0x1519)),
						m(EssFeneconCommercial40.ChannelId.CELL_27_VOLTAGE, new UnsignedWordElement(0x151A)),
						m(EssFeneconCommercial40.ChannelId.CELL_28_VOLTAGE, new UnsignedWordElement(0x151B)),
						m(EssFeneconCommercial40.ChannelId.CELL_29_VOLTAGE, new UnsignedWordElement(0x151C)),
						m(EssFeneconCommercial40.ChannelId.CELL_30_VOLTAGE, new UnsignedWordElement(0x151D)),
						m(EssFeneconCommercial40.ChannelId.CELL_31_VOLTAGE, new UnsignedWordElement(0x151E)),
						m(EssFeneconCommercial40.ChannelId.CELL_32_VOLTAGE, new UnsignedWordElement(0x151F)),
						m(EssFeneconCommercial40.ChannelId.CELL_33_VOLTAGE, new UnsignedWordElement(0x1520)),
						m(EssFeneconCommercial40.ChannelId.CELL_34_VOLTAGE, new UnsignedWordElement(0x1521)),
						m(EssFeneconCommercial40.ChannelId.CELL_35_VOLTAGE, new UnsignedWordElement(0x1522)),
						m(EssFeneconCommercial40.ChannelId.CELL_36_VOLTAGE, new UnsignedWordElement(0x1523)),
						m(EssFeneconCommercial40.ChannelId.CELL_37_VOLTAGE, new UnsignedWordElement(0x1524)),
						m(EssFeneconCommercial40.ChannelId.CELL_38_VOLTAGE, new UnsignedWordElement(0x1525)),
						m(EssFeneconCommercial40.ChannelId.CELL_39_VOLTAGE, new UnsignedWordElement(0x1526)),
						m(EssFeneconCommercial40.ChannelId.CELL_40_VOLTAGE, new UnsignedWordElement(0x1527)),
						m(EssFeneconCommercial40.ChannelId.CELL_41_VOLTAGE, new UnsignedWordElement(0x1528)),
						m(EssFeneconCommercial40.ChannelId.CELL_42_VOLTAGE, new UnsignedWordElement(0x1529)),
						m(EssFeneconCommercial40.ChannelId.CELL_43_VOLTAGE, new UnsignedWordElement(0x152A)),
						m(EssFeneconCommercial40.ChannelId.CELL_44_VOLTAGE, new UnsignedWordElement(0x152B)),
						m(EssFeneconCommercial40.ChannelId.CELL_45_VOLTAGE, new UnsignedWordElement(0x152C)),
						m(EssFeneconCommercial40.ChannelId.CELL_46_VOLTAGE, new UnsignedWordElement(0x152D)),
						m(EssFeneconCommercial40.ChannelId.CELL_47_VOLTAGE, new UnsignedWordElement(0x152E)),
						m(EssFeneconCommercial40.ChannelId.CELL_48_VOLTAGE, new UnsignedWordElement(0x152F)),
						m(EssFeneconCommercial40.ChannelId.CELL_49_VOLTAGE, new UnsignedWordElement(0x1530)),
						m(EssFeneconCommercial40.ChannelId.CELL_50_VOLTAGE, new UnsignedWordElement(0x1531)),
						m(EssFeneconCommercial40.ChannelId.CELL_51_VOLTAGE, new UnsignedWordElement(0x1532)),
						m(EssFeneconCommercial40.ChannelId.CELL_52_VOLTAGE, new UnsignedWordElement(0x1533)),
						m(EssFeneconCommercial40.ChannelId.CELL_53_VOLTAGE, new UnsignedWordElement(0x1534)),
						m(EssFeneconCommercial40.ChannelId.CELL_54_VOLTAGE, new UnsignedWordElement(0x1535)),
						m(EssFeneconCommercial40.ChannelId.CELL_55_VOLTAGE, new UnsignedWordElement(0x1536)),
						m(EssFeneconCommercial40.ChannelId.CELL_56_VOLTAGE, new UnsignedWordElement(0x1537)),
						m(EssFeneconCommercial40.ChannelId.CELL_57_VOLTAGE, new UnsignedWordElement(0x1538)),
						m(EssFeneconCommercial40.ChannelId.CELL_58_VOLTAGE, new UnsignedWordElement(0x1539)),
						m(EssFeneconCommercial40.ChannelId.CELL_59_VOLTAGE, new UnsignedWordElement(0x153A)),
						m(EssFeneconCommercial40.ChannelId.CELL_60_VOLTAGE, new UnsignedWordElement(0x153B)),
						m(EssFeneconCommercial40.ChannelId.CELL_61_VOLTAGE, new UnsignedWordElement(0x153C)),
						m(EssFeneconCommercial40.ChannelId.CELL_62_VOLTAGE, new UnsignedWordElement(0x153D)),
						m(EssFeneconCommercial40.ChannelId.CELL_63_VOLTAGE, new UnsignedWordElement(0x153E)),
						m(EssFeneconCommercial40.ChannelId.CELL_64_VOLTAGE, new UnsignedWordElement(0x153F)),
						m(EssFeneconCommercial40.ChannelId.CELL_65_VOLTAGE, new UnsignedWordElement(0x1540)),
						m(EssFeneconCommercial40.ChannelId.CELL_66_VOLTAGE, new UnsignedWordElement(0x1541)),
						m(EssFeneconCommercial40.ChannelId.CELL_67_VOLTAGE, new UnsignedWordElement(0x1542)),
						m(EssFeneconCommercial40.ChannelId.CELL_68_VOLTAGE, new UnsignedWordElement(0x1543)),
						m(EssFeneconCommercial40.ChannelId.CELL_69_VOLTAGE, new UnsignedWordElement(0x1544)),
						m(EssFeneconCommercial40.ChannelId.CELL_70_VOLTAGE, new UnsignedWordElement(0x1545)),
						m(EssFeneconCommercial40.ChannelId.CELL_71_VOLTAGE, new UnsignedWordElement(0x1546)),
						m(EssFeneconCommercial40.ChannelId.CELL_72_VOLTAGE, new UnsignedWordElement(0x1547)),
						m(EssFeneconCommercial40.ChannelId.CELL_73_VOLTAGE, new UnsignedWordElement(0x1548)),
						m(EssFeneconCommercial40.ChannelId.CELL_74_VOLTAGE, new UnsignedWordElement(0x1549)),
						m(EssFeneconCommercial40.ChannelId.CELL_75_VOLTAGE, new UnsignedWordElement(0x154A)),
						m(EssFeneconCommercial40.ChannelId.CELL_76_VOLTAGE, new UnsignedWordElement(0x154B)),
						m(EssFeneconCommercial40.ChannelId.CELL_77_VOLTAGE, new UnsignedWordElement(0x154C)),
						m(EssFeneconCommercial40.ChannelId.CELL_78_VOLTAGE, new UnsignedWordElement(0x154D)),
						m(EssFeneconCommercial40.ChannelId.CELL_79_VOLTAGE, new UnsignedWordElement(0x154E)),
						m(EssFeneconCommercial40.ChannelId.CELL_80_VOLTAGE, new UnsignedWordElement(0x154F))),
				new FC3ReadRegistersTask(0x1550, Priority.LOW,
						m(EssFeneconCommercial40.ChannelId.CELL_81_VOLTAGE, new UnsignedWordElement(0x1550)),
						m(EssFeneconCommercial40.ChannelId.CELL_82_VOLTAGE, new UnsignedWordElement(0x1551)),
						m(EssFeneconCommercial40.ChannelId.CELL_83_VOLTAGE, new UnsignedWordElement(0x1552)),
						m(EssFeneconCommercial40.ChannelId.CELL_84_VOLTAGE, new UnsignedWordElement(0x1553)),
						m(EssFeneconCommercial40.ChannelId.CELL_85_VOLTAGE, new UnsignedWordElement(0x1554)),
						m(EssFeneconCommercial40.ChannelId.CELL_86_VOLTAGE, new UnsignedWordElement(0x1555)),
						m(EssFeneconCommercial40.ChannelId.CELL_87_VOLTAGE, new UnsignedWordElement(0x1556)),
						m(EssFeneconCommercial40.ChannelId.CELL_88_VOLTAGE, new UnsignedWordElement(0x1557)),
						m(EssFeneconCommercial40.ChannelId.CELL_89_VOLTAGE, new UnsignedWordElement(0x1558)),
						m(EssFeneconCommercial40.ChannelId.CELL_90_VOLTAGE, new UnsignedWordElement(0x1559)),
						m(EssFeneconCommercial40.ChannelId.CELL_91_VOLTAGE, new UnsignedWordElement(0x155A)),
						m(EssFeneconCommercial40.ChannelId.CELL_92_VOLTAGE, new UnsignedWordElement(0x155B)),
						m(EssFeneconCommercial40.ChannelId.CELL_93_VOLTAGE, new UnsignedWordElement(0x155C)),
						m(EssFeneconCommercial40.ChannelId.CELL_94_VOLTAGE, new UnsignedWordElement(0x155D)),
						m(EssFeneconCommercial40.ChannelId.CELL_95_VOLTAGE, new UnsignedWordElement(0x155E)),
						m(EssFeneconCommercial40.ChannelId.CELL_96_VOLTAGE, new UnsignedWordElement(0x155F)),
						m(EssFeneconCommercial40.ChannelId.CELL_97_VOLTAGE, new UnsignedWordElement(0x1560)),
						m(EssFeneconCommercial40.ChannelId.CELL_98_VOLTAGE, new UnsignedWordElement(0x1561)),
						m(EssFeneconCommercial40.ChannelId.CELL_99_VOLTAGE, new UnsignedWordElement(0x1562)),
						m(EssFeneconCommercial40.ChannelId.CELL_100_VOLTAGE, new UnsignedWordElement(0x1563)),
						m(EssFeneconCommercial40.ChannelId.CELL_101_VOLTAGE, new UnsignedWordElement(0x1564)),
						m(EssFeneconCommercial40.ChannelId.CELL_102_VOLTAGE, new UnsignedWordElement(0x1565)),
						m(EssFeneconCommercial40.ChannelId.CELL_103_VOLTAGE, new UnsignedWordElement(0x1566)),
						m(EssFeneconCommercial40.ChannelId.CELL_104_VOLTAGE, new UnsignedWordElement(0x1567)),
						m(EssFeneconCommercial40.ChannelId.CELL_105_VOLTAGE, new UnsignedWordElement(0x1568)),
						m(EssFeneconCommercial40.ChannelId.CELL_106_VOLTAGE, new UnsignedWordElement(0x1569)),
						m(EssFeneconCommercial40.ChannelId.CELL_107_VOLTAGE, new UnsignedWordElement(0x156A)),
						m(EssFeneconCommercial40.ChannelId.CELL_108_VOLTAGE, new UnsignedWordElement(0x156B)),
						m(EssFeneconCommercial40.ChannelId.CELL_109_VOLTAGE, new UnsignedWordElement(0x156C)),
						m(EssFeneconCommercial40.ChannelId.CELL_110_VOLTAGE, new UnsignedWordElement(0x156D)),
						m(EssFeneconCommercial40.ChannelId.CELL_111_VOLTAGE, new UnsignedWordElement(0x156E)),
						m(EssFeneconCommercial40.ChannelId.CELL_112_VOLTAGE, new UnsignedWordElement(0x156F)),
						m(EssFeneconCommercial40.ChannelId.CELL_113_VOLTAGE, new UnsignedWordElement(0x1570)),
						m(EssFeneconCommercial40.ChannelId.CELL_114_VOLTAGE, new UnsignedWordElement(0x1571)),
						m(EssFeneconCommercial40.ChannelId.CELL_115_VOLTAGE, new UnsignedWordElement(0x1572)),
						m(EssFeneconCommercial40.ChannelId.CELL_116_VOLTAGE, new UnsignedWordElement(0x1573)),
						m(EssFeneconCommercial40.ChannelId.CELL_117_VOLTAGE, new UnsignedWordElement(0x1574)),
						m(EssFeneconCommercial40.ChannelId.CELL_118_VOLTAGE, new UnsignedWordElement(0x1575)),
						m(EssFeneconCommercial40.ChannelId.CELL_119_VOLTAGE, new UnsignedWordElement(0x1576)),
						m(EssFeneconCommercial40.ChannelId.CELL_120_VOLTAGE, new UnsignedWordElement(0x1577)),
						m(EssFeneconCommercial40.ChannelId.CELL_121_VOLTAGE, new UnsignedWordElement(0x1578)),
						m(EssFeneconCommercial40.ChannelId.CELL_122_VOLTAGE, new UnsignedWordElement(0x1579)),
						m(EssFeneconCommercial40.ChannelId.CELL_123_VOLTAGE, new UnsignedWordElement(0x157A)),
						m(EssFeneconCommercial40.ChannelId.CELL_124_VOLTAGE, new UnsignedWordElement(0x157B)),
						m(EssFeneconCommercial40.ChannelId.CELL_125_VOLTAGE, new UnsignedWordElement(0x157C)),
						m(EssFeneconCommercial40.ChannelId.CELL_126_VOLTAGE, new UnsignedWordElement(0x157D)),
						m(EssFeneconCommercial40.ChannelId.CELL_127_VOLTAGE, new UnsignedWordElement(0x157E)),
						m(EssFeneconCommercial40.ChannelId.CELL_128_VOLTAGE, new UnsignedWordElement(0x157F)),
						m(EssFeneconCommercial40.ChannelId.CELL_129_VOLTAGE, new UnsignedWordElement(0x1580)),
						m(EssFeneconCommercial40.ChannelId.CELL_130_VOLTAGE, new UnsignedWordElement(0x1581)),
						m(EssFeneconCommercial40.ChannelId.CELL_131_VOLTAGE, new UnsignedWordElement(0x1582)),
						m(EssFeneconCommercial40.ChannelId.CELL_132_VOLTAGE, new UnsignedWordElement(0x1583)),
						m(EssFeneconCommercial40.ChannelId.CELL_133_VOLTAGE, new UnsignedWordElement(0x1584)),
						m(EssFeneconCommercial40.ChannelId.CELL_134_VOLTAGE, new UnsignedWordElement(0x1585)),
						m(EssFeneconCommercial40.ChannelId.CELL_135_VOLTAGE, new UnsignedWordElement(0x1586)),
						m(EssFeneconCommercial40.ChannelId.CELL_136_VOLTAGE, new UnsignedWordElement(0x1587)),
						m(EssFeneconCommercial40.ChannelId.CELL_137_VOLTAGE, new UnsignedWordElement(0x1588)),
						m(EssFeneconCommercial40.ChannelId.CELL_138_VOLTAGE, new UnsignedWordElement(0x1589)),
						m(EssFeneconCommercial40.ChannelId.CELL_139_VOLTAGE, new UnsignedWordElement(0x158A)),
						m(EssFeneconCommercial40.ChannelId.CELL_140_VOLTAGE, new UnsignedWordElement(0x158B)),
						m(EssFeneconCommercial40.ChannelId.CELL_141_VOLTAGE, new UnsignedWordElement(0x158C)),
						m(EssFeneconCommercial40.ChannelId.CELL_142_VOLTAGE, new UnsignedWordElement(0x158D)),
						m(EssFeneconCommercial40.ChannelId.CELL_143_VOLTAGE, new UnsignedWordElement(0x158E)),
						m(EssFeneconCommercial40.ChannelId.CELL_144_VOLTAGE, new UnsignedWordElement(0x158F)),
						m(EssFeneconCommercial40.ChannelId.CELL_145_VOLTAGE, new UnsignedWordElement(0x1590)),
						m(EssFeneconCommercial40.ChannelId.CELL_146_VOLTAGE, new UnsignedWordElement(0x1591)),
						m(EssFeneconCommercial40.ChannelId.CELL_147_VOLTAGE, new UnsignedWordElement(0x1592)),
						m(EssFeneconCommercial40.ChannelId.CELL_148_VOLTAGE, new UnsignedWordElement(0x1593)),
						m(EssFeneconCommercial40.ChannelId.CELL_149_VOLTAGE, new UnsignedWordElement(0x1594)),
						m(EssFeneconCommercial40.ChannelId.CELL_150_VOLTAGE, new UnsignedWordElement(0x1595)),
						m(EssFeneconCommercial40.ChannelId.CELL_151_VOLTAGE, new UnsignedWordElement(0x1596)),
						m(EssFeneconCommercial40.ChannelId.CELL_152_VOLTAGE, new UnsignedWordElement(0x1597)),
						m(EssFeneconCommercial40.ChannelId.CELL_153_VOLTAGE, new UnsignedWordElement(0x1598)),
						m(EssFeneconCommercial40.ChannelId.CELL_154_VOLTAGE, new UnsignedWordElement(0x1599)),
						m(EssFeneconCommercial40.ChannelId.CELL_155_VOLTAGE, new UnsignedWordElement(0x159A)),
						m(EssFeneconCommercial40.ChannelId.CELL_156_VOLTAGE, new UnsignedWordElement(0x159B)),
						m(EssFeneconCommercial40.ChannelId.CELL_157_VOLTAGE, new UnsignedWordElement(0x159C)),
						m(EssFeneconCommercial40.ChannelId.CELL_158_VOLTAGE, new UnsignedWordElement(0x159D)),
						m(EssFeneconCommercial40.ChannelId.CELL_159_VOLTAGE, new UnsignedWordElement(0x159E)),
						m(EssFeneconCommercial40.ChannelId.CELL_160_VOLTAGE, new UnsignedWordElement(0x159F))),
				new FC3ReadRegistersTask(0x15A0, Priority.LOW,
						m(EssFeneconCommercial40.ChannelId.CELL_161_VOLTAGE, new UnsignedWordElement(0x15A0)),
						m(EssFeneconCommercial40.ChannelId.CELL_162_VOLTAGE, new UnsignedWordElement(0x15A1)),
						m(EssFeneconCommercial40.ChannelId.CELL_163_VOLTAGE, new UnsignedWordElement(0x15A2)),
						m(EssFeneconCommercial40.ChannelId.CELL_164_VOLTAGE, new UnsignedWordElement(0x15A3)),
						m(EssFeneconCommercial40.ChannelId.CELL_165_VOLTAGE, new UnsignedWordElement(0x15A4)),
						m(EssFeneconCommercial40.ChannelId.CELL_166_VOLTAGE, new UnsignedWordElement(0x15A5)),
						m(EssFeneconCommercial40.ChannelId.CELL_167_VOLTAGE, new UnsignedWordElement(0x15A6)),
						m(EssFeneconCommercial40.ChannelId.CELL_168_VOLTAGE, new UnsignedWordElement(0x15A7)),
						m(EssFeneconCommercial40.ChannelId.CELL_169_VOLTAGE, new UnsignedWordElement(0x15A8)),
						m(EssFeneconCommercial40.ChannelId.CELL_170_VOLTAGE, new UnsignedWordElement(0x15A9)),
						m(EssFeneconCommercial40.ChannelId.CELL_171_VOLTAGE, new UnsignedWordElement(0x15AA)),
						m(EssFeneconCommercial40.ChannelId.CELL_172_VOLTAGE, new UnsignedWordElement(0x15AB)),
						m(EssFeneconCommercial40.ChannelId.CELL_173_VOLTAGE, new UnsignedWordElement(0x15AC)),
						m(EssFeneconCommercial40.ChannelId.CELL_174_VOLTAGE, new UnsignedWordElement(0x15AD)),
						m(EssFeneconCommercial40.ChannelId.CELL_175_VOLTAGE, new UnsignedWordElement(0x15AE)),
						m(EssFeneconCommercial40.ChannelId.CELL_176_VOLTAGE, new UnsignedWordElement(0x15AF)),
						m(EssFeneconCommercial40.ChannelId.CELL_177_VOLTAGE, new UnsignedWordElement(0x15B0)),
						m(EssFeneconCommercial40.ChannelId.CELL_178_VOLTAGE, new UnsignedWordElement(0x15B1)),
						m(EssFeneconCommercial40.ChannelId.CELL_179_VOLTAGE, new UnsignedWordElement(0x15B2)),
						m(EssFeneconCommercial40.ChannelId.CELL_180_VOLTAGE, new UnsignedWordElement(0x15B3)),
						m(EssFeneconCommercial40.ChannelId.CELL_181_VOLTAGE, new UnsignedWordElement(0x15B4)),
						m(EssFeneconCommercial40.ChannelId.CELL_182_VOLTAGE, new UnsignedWordElement(0x15B5)),
						m(EssFeneconCommercial40.ChannelId.CELL_183_VOLTAGE, new UnsignedWordElement(0x15B6)),
						m(EssFeneconCommercial40.ChannelId.CELL_184_VOLTAGE, new UnsignedWordElement(0x15B7)),
						m(EssFeneconCommercial40.ChannelId.CELL_185_VOLTAGE, new UnsignedWordElement(0x15B8)),
						m(EssFeneconCommercial40.ChannelId.CELL_186_VOLTAGE, new UnsignedWordElement(0x15B9)),
						m(EssFeneconCommercial40.ChannelId.CELL_187_VOLTAGE, new UnsignedWordElement(0x15BA)),
						m(EssFeneconCommercial40.ChannelId.CELL_188_VOLTAGE, new UnsignedWordElement(0x15BB)),
						m(EssFeneconCommercial40.ChannelId.CELL_189_VOLTAGE, new UnsignedWordElement(0x15BC)),
						m(EssFeneconCommercial40.ChannelId.CELL_190_VOLTAGE, new UnsignedWordElement(0x15BD)),
						m(EssFeneconCommercial40.ChannelId.CELL_191_VOLTAGE, new UnsignedWordElement(0x15BE)),
						m(EssFeneconCommercial40.ChannelId.CELL_192_VOLTAGE, new UnsignedWordElement(0x15BF)),
						m(EssFeneconCommercial40.ChannelId.CELL_193_VOLTAGE, new UnsignedWordElement(0x15C0)),
						m(EssFeneconCommercial40.ChannelId.CELL_194_VOLTAGE, new UnsignedWordElement(0x15C1)),
						m(EssFeneconCommercial40.ChannelId.CELL_195_VOLTAGE, new UnsignedWordElement(0x15C2)),
						m(EssFeneconCommercial40.ChannelId.CELL_196_VOLTAGE, new UnsignedWordElement(0x15C3)),
						m(EssFeneconCommercial40.ChannelId.CELL_197_VOLTAGE, new UnsignedWordElement(0x15C4)),
						m(EssFeneconCommercial40.ChannelId.CELL_198_VOLTAGE, new UnsignedWordElement(0x15C5)),
						m(EssFeneconCommercial40.ChannelId.CELL_199_VOLTAGE, new UnsignedWordElement(0x15C6)),
						m(EssFeneconCommercial40.ChannelId.CELL_200_VOLTAGE, new UnsignedWordElement(0x15C7)),
						m(EssFeneconCommercial40.ChannelId.CELL_201_VOLTAGE, new UnsignedWordElement(0x15C8)),
						m(EssFeneconCommercial40.ChannelId.CELL_202_VOLTAGE, new UnsignedWordElement(0x15C9)),
						m(EssFeneconCommercial40.ChannelId.CELL_203_VOLTAGE, new UnsignedWordElement(0x15CA)),
						m(EssFeneconCommercial40.ChannelId.CELL_204_VOLTAGE, new UnsignedWordElement(0x15CB)),
						m(EssFeneconCommercial40.ChannelId.CELL_205_VOLTAGE, new UnsignedWordElement(0x15CC)),
						m(EssFeneconCommercial40.ChannelId.CELL_206_VOLTAGE, new UnsignedWordElement(0x15CD)),
						m(EssFeneconCommercial40.ChannelId.CELL_207_VOLTAGE, new UnsignedWordElement(0x15CE)),
						m(EssFeneconCommercial40.ChannelId.CELL_208_VOLTAGE, new UnsignedWordElement(0x15CF)),
						m(EssFeneconCommercial40.ChannelId.CELL_209_VOLTAGE, new UnsignedWordElement(0x15D0)),
						m(EssFeneconCommercial40.ChannelId.CELL_210_VOLTAGE, new UnsignedWordElement(0x15D1)),
						m(EssFeneconCommercial40.ChannelId.CELL_211_VOLTAGE, new UnsignedWordElement(0x15D2)),
						m(EssFeneconCommercial40.ChannelId.CELL_212_VOLTAGE, new UnsignedWordElement(0x15D3)),
						m(EssFeneconCommercial40.ChannelId.CELL_213_VOLTAGE, new UnsignedWordElement(0x15D4)),
						m(EssFeneconCommercial40.ChannelId.CELL_214_VOLTAGE, new UnsignedWordElement(0x15D5)),
						m(EssFeneconCommercial40.ChannelId.CELL_215_VOLTAGE, new UnsignedWordElement(0x15D6)),
						m(EssFeneconCommercial40.ChannelId.CELL_216_VOLTAGE, new UnsignedWordElement(0x15D7)),
						m(EssFeneconCommercial40.ChannelId.CELL_217_VOLTAGE, new UnsignedWordElement(0x15D8)),
						m(EssFeneconCommercial40.ChannelId.CELL_218_VOLTAGE, new UnsignedWordElement(0x15D9)),
						m(EssFeneconCommercial40.ChannelId.CELL_219_VOLTAGE, new UnsignedWordElement(0x15DA)),
						m(EssFeneconCommercial40.ChannelId.CELL_220_VOLTAGE, new UnsignedWordElement(0x15DB)),
						m(EssFeneconCommercial40.ChannelId.CELL_221_VOLTAGE, new UnsignedWordElement(0x15DC)),
						m(EssFeneconCommercial40.ChannelId.CELL_222_VOLTAGE, new UnsignedWordElement(0x15DD)),
						m(EssFeneconCommercial40.ChannelId.CELL_223_VOLTAGE, new UnsignedWordElement(0x15DE)),
						m(EssFeneconCommercial40.ChannelId.CELL_224_VOLTAGE, new UnsignedWordElement(0x15DF))),
				new FC3ReadRegistersTask(0x1400, Priority.LOW, //
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_TOTAL_VOLTAGE,
								new UnsignedWordElement(0x1400), SCALE_FACTOR_2),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_TOTAL_CURRENT, new SignedWordElement(0x1401),
								SCALE_FACTOR_2),
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(0x1402)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_SOH, new UnsignedWordElement(0x1403)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_AVG_TEMPERATURE,
								new SignedWordElement(0x1404)),
						new DummyRegisterElement(0x1405),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_CHARGE_CURRENT_LIMIT,
								new UnsignedWordElement(0x1406), SCALE_FACTOR_2),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_DISCHARGE_CURRENT_LIMIT,
								new UnsignedWordElement(0x1407), SCALE_FACTOR_2),
						new DummyRegisterElement(0x1408, 0x1409),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_CYCLES,
								new UnsignedDoublewordElement(0x140A).wordOrder(WordOrder.LSWMSW)),
						new DummyRegisterElement(0x140C, 0x1417),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_CHARGE_ENERGY,
								new UnsignedDoublewordElement(0x1418).wordOrder(WordOrder.LSWMSW)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_DISCHARGE_ENERGY,
								new UnsignedDoublewordElement(0x141A).wordOrder(WordOrder.LSWMSW)),
						new DummyRegisterElement(0x141C, 0x141F),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_POWER, new SignedWordElement(0x1420),
								SCALE_FACTOR_2),
						new DummyRegisterElement(0x1421, 0x142F),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MAX_CELL_VOLTAGE_NO,
								new UnsignedWordElement(0x1430)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MAX_CELL_VOLTAGE,
								new UnsignedWordElement(0x1431)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MAX_CELL_VOLTAGE_TEMPERATURE,
								new SignedWordElement(0x1432)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MIN_CELL_VOLTAGE_NO,
								new UnsignedWordElement(0x1433)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MIN_CELL_VOLTAGE,
								new UnsignedWordElement(0x1434)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MIN_CELL_VOLTAGE_TEMPERATURE,
								new SignedWordElement(0x1435)),
						new DummyRegisterElement(0x1436, 0x1439),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MAX_CELL_TEMPERATURE_NO,
								new UnsignedWordElement(0x143A)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MAX_CELL_TEMPERATURE,
								new SignedWordElement(0x143B)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MAX_CELL_TEMPERATURE_VOLTAGE,
								new SignedWordElement(0x143C)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MIN_CELL_TEMPERATURE_NO,
								new UnsignedWordElement(0x143D)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MIN_CELL_TEMPERATURE,
								new SignedWordElement(0x143E)),
						m(EssFeneconCommercial40.ChannelId.BATTERY_STRING_MIN_CELL_TEMPERATURE_VOLTAGE,
								new SignedWordElement(0x143F))));
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString() //
				+ "|Feed-In:"
				+ this.channel(EssFeneconCommercial40.ChannelId.SURPLUS_FEED_IN_STATE_MACHINE).value().asOptionString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.applyPowerLimitOnPowerDecreaseCausedByOvertemperatureError();
			this.calculateEnergy();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.defineWorkState();
			break;
		}
	}

	private LocalDateTime lastDefineWorkState = null;

	private void defineWorkState() {
		/*
		 * Set ESS in running mode
		 */
		// TODO this should be smarter: set in energy saving mode if there was no output
		// power for a while and we don't need emergency power.
		var now = LocalDateTime.now();
		if (this.lastDefineWorkState == null || now.minusMinutes(1).isAfter(this.lastDefineWorkState)) {
			this.lastDefineWorkState = now;
			EnumWriteChannel setWorkStateChannel = this.channel(EssFeneconCommercial40.ChannelId.SET_WORK_STATE);
			try {
				setWorkStateChannel.setNextWriteValue(SetWorkState.START);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Unable to start: " + e.getMessage());
			}
		}
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public boolean isManaged() {
		return !this.config.readOnlyMode();
	}

	@Override
	public int getPowerPrecision() {
		return 100; // the modbus field for SetActivePower has the unit 0.1 kW
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsNamedException {
		// Read-Only-Mode
		if (this.config.readOnlyMode()) {
			return new Constraint[] { //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0), //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) //
			};
		}

		// Reactive Power constraints
		return new Constraint[] { //
				this.createPowerConstraint("Commercial40 Min Reactive Power", Phase.ALL, Pwr.REACTIVE,
						Relationship.GREATER_OR_EQUALS, MIN_REACTIVE_POWER), //
				this.createPowerConstraint("Commercial40 Max Reactive Power", Phase.ALL, Pwr.REACTIVE,
						Relationship.LESS_OR_EQUALS, MAX_REACTIVE_POWER) };
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public void addCharger(EssDcChargerFeneconCommercial40 charger) {
		this.chargers.add(charger);
	}

	@Override
	public void removeCharger(EssDcChargerFeneconCommercial40 charger) {
		this.chargers.remove(charger);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	private void applyPowerLimitOnPowerDecreaseCausedByOvertemperatureError() {
		if (this.config.powerLimitOnPowerDecreaseCausedByOvertemperatureChannel() != 0) {
			StateChannel powerDecreaseCausedByOvertemperatureChannel = this
					.channel(EssFeneconCommercial40.ChannelId.POWER_DECREASE_CAUSED_BY_OVERTEMPERATURE);
			if (powerDecreaseCausedByOvertemperatureChannel.value().orElse(false)) {
				/*
				 * Apply limit on ESS charge/discharge power
				 */
				try {
					this.power.addConstraintAndValidate(
							this.createPowerConstraint("Limit On PowerDecreaseCausedByOvertemperature Error", Phase.ALL,
									Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS,
									this.config.powerLimitOnPowerDecreaseCausedByOvertemperatureChannel() * -1));
					this.power.addConstraintAndValidate(
							this.createPowerConstraint("Limit On PowerDecreaseCausedByOvertemperature Error", Phase.ALL,
									Pwr.ACTIVE, Relationship.LESS_OR_EQUALS,
									this.config.powerLimitOnPowerDecreaseCausedByOvertemperatureChannel()));
				} catch (OpenemsException e) {
					this.logError(this.log, e.getMessage());
				}
				/*
				 * Apply limit on Charger
				 */
				if (this.chargers.size() > 0) {
					IntegerWriteChannel setPvPowerLimit = this.chargers.get(0)
							.channel(EssDcChargerFeneconCommercial40.ChannelId.SET_PV_POWER_LIMIT);
					try {
						setPvPowerLimit.setNextWriteValue(
								this.config.powerLimitOnPowerDecreaseCausedByOvertemperatureChannel());
					} catch (OpenemsNamedException e) {
						this.logError(this.log, e.getMessage());
					}
				}

			}
		}
	}

	@Override
	public Integer getSurplusPower() {
		return this.surplusFeedInHandler.run(this.chargers, this.config, this.componentManager);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	private void calculateEnergy() {
		/*
		 * Calculate AC Energy
		 */
		var acActivePower = this.getActivePowerChannel().getNextValue().get();
		if (acActivePower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			this.calculateAcDischargeEnergy.update(null);
		} else if (acActivePower > 0) {
			// Discharge
			this.calculateAcChargeEnergy.update(0);
			this.calculateAcDischargeEnergy.update(acActivePower);
		} else {
			// Charge
			this.calculateAcChargeEnergy.update(acActivePower * -1);
			this.calculateAcDischargeEnergy.update(0);
		}
		/*
		 * Calculate DC Power and Energy
		 */
		var dcDischargePower = acActivePower;
		for (EssDcChargerFeneconCommercial40 charger : this.chargers) {
			dcDischargePower = TypeUtils.subtract(dcDischargePower,
					charger.getActualPowerChannel().getNextValue().get());
		}
		this._setDcDischargePower(dcDischargePower);

		if (dcDischargePower == null) {
			// Not available
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		} else if (dcDischargePower > 0) {
			// Discharge
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(dcDischargePower);
		} else {
			// Charge
			this.calculateDcChargeEnergy.update(dcDischargePower * -1);
			this.calculateDcDischargeEnergy.update(0);
		}
	}

}
