package io.openems.edge.deye.ess;

import static io.openems.edge.common.cycle.Cycle.DEFAULT_CYCLE_TIME;

import java.time.LocalDateTime;
import java.util.Arrays;

import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.taskmanager.Priority;

import io.openems.edge.deye.battery.DeyeSunBattery;
import io.openems.edge.deye.dccharger.DeyeDcCharger;
import io.openems.edge.deye.enums.BatteryRunState;
import io.openems.edge.deye.enums.EmsPowerMode;
import io.openems.edge.deye.enums.WorkState;

import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.common.CycleProvider;
import io.openems.edge.ess.power.api.Power;

import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Deye.BatteryInverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class DeyeSunHybridImpl extends AbstractOpenemsModbusComponent
		implements DeyeSunHybrid, HybridEss, ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent,
		EventHandler, ModbusSlave, TimedataProvider, CycleProvider {

	// protected static final int MAX_APPARENT_POWER = 20000;

	// protected static final int NET_CAPACITY = 10000;

	private final Logger log = LoggerFactory.getLogger(DeyeSunHybridImpl.class);

	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateAcDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_CHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_DISCHARGE_ENERGY);

	private LocalDateTime lastDefineWorkState =  LocalDateTime.now();

	private boolean chargeMode = false;

	private BatteryRunState lastBatteryRunState = null;
	private boolean lastHadCommError = false;
	
	@Reference
	private ComponentManager componentManager;

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Cycle cycle;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(name = "Battery", bind = "setBattery", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL, unbind = "unsetBattery")
	private volatile DeyeSunBattery battery;

	@Reference(name = "DcCharger", bind = "setDcCharger", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL, unbind = "unsetDcCharger")
	private volatile DeyeDcCharger dcCharger = null;

	// private final AllowedChargeDischargeHandler allowedChargeDischargeHandler =
	// new AllowedChargeDischargeHandler(this,this.battery);
	private ApplyPowerHandler applyPowerHandler = null;
	private AllowedChargeDischargeHandler allowedChargeDischargeHandler = null;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config;

	public DeyeSunHybridImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				DeyeSunHybrid.ChannelId.values() //
		);
		// this._setCapacity(NET_CAPACITY); // ToDo. Comes from Goodwe. Needed? We can
		// calculate capacity out of own values
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.unit_id(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		if (!config.battery_id().isEmpty()) {
			OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Battery", config.battery_id());
		}

		if (!config.dccharger_id().isEmpty()) {
			OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "DcCharger", config.dccharger_id());
		}

		this.config = config;
		this.setWorkState(WorkState.UNDEFINED);
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
		log.debug("\n\n applyPower called by {} with {} W", Thread.currentThread().getStackTrace()[2].getClassName(),
				activePower);
		// AC 1/28/2024
		// IntegerWriteChannel setGridLoadOffPowerChannel =
		// this.channel(DeyeSunHybrid.ChannelId.SET_GRID_LOAD_OFF_POWER);
		// setGridLoadOffPowerChannel.setNextWriteValue(93);

		if (this.applyPowerHandler != null) {
			this.applyPowerHandler.apply(activePower, reactivePower, this.config.maxApparentPower());
		}

	}

	public EmsPowerMode getEmsPowerMode() {
		return this.config.emsPowerMode();
	}

	@Override
	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //


				new FC16WriteRegistersTask(141,
						m(DeyeSunHybrid.ChannelId.ENERGY_MANAGEMENT_MODEL, new UnsignedWordElement(141)),
						m(DeyeSunHybrid.ChannelId.LIMIT_CONTROL_FUNCTION, new UnsignedWordElement(142)),
						m(DeyeSunHybrid.ChannelId.POWER_TO_GRID_TARGET, new UnsignedWordElement(143)),
						new DummyRegisterElement(144),
						m(DeyeSunHybrid.ChannelId.SOLAR_SELL_MODE, new UnsignedWordElement(145))),
				// m(DeyeSunHybrid.ChannelId.SET_TIME_OF_USE_SELLING_ENABLED, new
				// UnsignedWordElement(146)),
				new FC16WriteRegistersTask(146,
						m(new BitsWordElement(146, this).bit(0, DeyeSunHybrid.ChannelId.SET_TIME_OF_USE_SELLING_ENABLED) // Common
																															// switch
								.bit(1, DeyeSunHybrid.ChannelId.SET_TIME_OF_USE_MONDAY) //
								.bit(2, DeyeSunHybrid.ChannelId.SET_TIME_OF_USE_TUESDAY) //
								.bit(3, DeyeSunHybrid.ChannelId.SET_TIME_OF_USE_WEDNESDAY) //
								.bit(4, DeyeSunHybrid.ChannelId.SET_TIME_OF_USE_THURSDAY) //
								.bit(5, DeyeSunHybrid.ChannelId.SET_TIME_OF_USE_FRIDAY) //
								.bit(6, DeyeSunHybrid.ChannelId.SET_TIME_OF_USE_SATURDAY) //
								.bit(7, DeyeSunHybrid.ChannelId.SET_TIME_OF_USE_SUNDAY) //

						),

						new DummyRegisterElement(147),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_1, new UnsignedWordElement(148)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_2, new UnsignedWordElement(149)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_3, new UnsignedWordElement(150)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_4, new UnsignedWordElement(151)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_5, new UnsignedWordElement(152)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_6, new UnsignedWordElement(153)),

						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_1_POWER, new UnsignedWordElement(154)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_2_POWER, new UnsignedWordElement(155)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_3_POWER, new UnsignedWordElement(156)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_4_POWER, new UnsignedWordElement(157)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_5_POWER, new UnsignedWordElement(158)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_6_POWER, new UnsignedWordElement(159)),
						new DummyRegisterElement(160, 165),

						//
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_1_CAPACITY, new UnsignedWordElement(166)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_2_CAPACITY, new UnsignedWordElement(167)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_3_CAPACITY, new UnsignedWordElement(168)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_4_CAPACITY, new UnsignedWordElement(169)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_5_CAPACITY, new UnsignedWordElement(170)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_6_CAPACITY, new UnsignedWordElement(171)),

						// Bit 0 -> Charge from grid enabled, Bit 1 -> Charge from generator
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_1, new UnsignedWordElement(172)),
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_2, new UnsignedWordElement(173)),
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_3, new UnsignedWordElement(174)),
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_4, new UnsignedWordElement(175)),
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_5, new UnsignedWordElement(176)),
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_6, new UnsignedWordElement(177))),

				// Read registers

				new FC3ReadRegistersTask(1, Priority.LOW,
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(1)), new DummyRegisterElement(2),
						m(DeyeSunHybrid.ChannelId.SERIAL_NUMBER, new StringWordElement(3, 5)),
						new DummyRegisterElement(8, 19),
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER,
								new UnsignedDoublewordElement(20).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),

				new FC3ReadRegistersTask(60, Priority.HIGH,

						m(DeyeSunHybrid.ChannelId.REMOTE_LOCK_STATE, new UnsignedWordElement(60)),

						new DummyRegisterElement(61, 76),
						m(DeyeSunHybrid.ChannelId.ACTIVE_POWER_REGULATION, new SignedWordElement(77),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(DeyeSunHybrid.ChannelId.REACTIVE_POWER_REGULATION, new SignedWordElement(78),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(DeyeSunHybrid.ChannelId.APPARENT_POWER_REGULATION, new SignedWordElement(79),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(DeyeSunHybrid.ChannelId.ENABLE_SWITCH_STATE, new UnsignedWordElement(80)),
						m(DeyeSunHybrid.ChannelId.FACTORY_RESET_STATE, new UnsignedWordElement(81)),
						m(DeyeSunHybrid.ChannelId.SELF_CHECKING_TIME, new SignedWordElement(82)),
						m(DeyeSunHybrid.ChannelId.ISLAND_PROTECTION_ENABLE, new SignedWordElement(83)),
						m(DeyeSunHybrid.ChannelId.MPPT_NUMBER, new UnsignedWordElement(84)),
						m(DeyeSunHybrid.ChannelId.GFDI_STATE, new SignedWordElement(85)), // What is this?

						new DummyRegisterElement(86),
						m(DeyeSunHybrid.ChannelId.RISO_STATE, new UnsignedWordElement(87)), // What is this?
						m(DeyeSunHybrid.ChannelId.GRID_STANDARD, new SignedWordElement(88)), //
						new DummyRegisterElement(89, 120),

						// Generator / grid charge settings
						m(DeyeSunHybrid.ChannelId.GENERATOR_MAX_OPERATING_TIME, new UnsignedWordElement(121),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.GENERATOR_COOLING_TIME, new UnsignedWordElement(122),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.GENERATOR_CHARGING_START_VOLTAGE, new UnsignedWordElement(123),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.GENERATOR_CHARGING_START_CAPACITY, new UnsignedWordElement(124)),
						m(DeyeSunHybrid.ChannelId.GENERATOR_CHARGE_CURRENT, new UnsignedWordElement(125)),
						m(DeyeSunHybrid.ChannelId.GRID_CHARGING_START_VOLTAGE, new UnsignedWordElement(126),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.GRID_CHARGING_START_CAPACITY, new UnsignedWordElement(127)),
						m(DeyeSunHybrid.ChannelId.GRID_CHARGE_CURRENT, new UnsignedWordElement(128)),
						m(DeyeSunHybrid.ChannelId.GENERATOR_CHARGING_ENABLE, new UnsignedWordElement(129)),
						m(DeyeSunHybrid.ChannelId.GRID_CHARGING_ENABLE, new UnsignedWordElement(130)),

						// Power management and sell mode settings
						m(DeyeSunHybrid.ChannelId.AC_COUPLE_FREQUENCY_LIMIT, new UnsignedWordElement(131),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.FORCE_GENERATOR_AS_LOAD, new UnsignedWordElement(132)),
						m(DeyeSunHybrid.ChannelId.GENERATOR_INPUT_AS_LOAD_ENABLE, new UnsignedWordElement(133)),
						m(DeyeSunHybrid.ChannelId.SMARTLOAD_OFF_BATT_VOLTAGE, new UnsignedWordElement(134),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.SMARTLOAD_OFF_BATT_CAPACITY, new UnsignedWordElement(135)),
						m(DeyeSunHybrid.ChannelId.SMARTLOAD_ON_BATT_VOLTAGE, new UnsignedWordElement(136),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.SMARTLOAD_ON_BATT_CAPACITY, new UnsignedWordElement(137)),
						m(DeyeSunHybrid.ChannelId.OUTPUT_VOLTAGE_LEVEL, new UnsignedWordElement(138)),
						m(DeyeSunHybrid.ChannelId.MIN_SOLAR_POWER_TO_START_GENERATOR, new UnsignedWordElement(139)),
						m(DeyeSunHybrid.ChannelId.GEN_GRID_SIGNAL_ON, new UnsignedWordElement(140)),
						m(DeyeSunHybrid.ChannelId.ENERGY_MANAGEMENT_MODEL, new UnsignedWordElement(141)),
						m(DeyeSunHybrid.ChannelId.LIMIT_CONTROL_FUNCTION, new UnsignedWordElement(142)),
						// m(DeyeSunHybrid.ChannelId.LIMIT_MAX_GRID_OUTPUT_POWER,new
						// UnsignedWordElement(143)),
						m(DeyeSunHybrid.ChannelId.POWER_TO_GRID_TARGET, new UnsignedWordElement(143)),
						m(DeyeSunHybrid.ChannelId.EXTERNAL_CURRENT_SENSOR_CLAMP_PHASE, new UnsignedWordElement(144)),
						m(DeyeSunHybrid.ChannelId.SOLAR_SELL_MODE, new UnsignedWordElement(145))),
				// m(DeyeSunHybrid.ChannelId.TIME_OF_USE_SELLING_ENABLED, new
				// UnsignedWordElement(146)),
				new FC3ReadRegistersTask(146, Priority.HIGH,
						m(new BitsWordElement(146, this).bit(0, DeyeSunHybrid.ChannelId.TIME_OF_USE_SELLING_ENABLED) // Common
																														// switch
								.bit(1, DeyeSunHybrid.ChannelId.TIME_OF_USE_MONDAY) //
								.bit(2, DeyeSunHybrid.ChannelId.TIME_OF_USE_TUESDAY) //
								.bit(3, DeyeSunHybrid.ChannelId.TIME_OF_USE_WEDNESDAY) //
								.bit(4, DeyeSunHybrid.ChannelId.TIME_OF_USE_THURSDAY) //
								.bit(5, DeyeSunHybrid.ChannelId.TIME_OF_USE_FRIDAY) //
								.bit(6, DeyeSunHybrid.ChannelId.TIME_OF_USE_SATURDAY) //
								.bit(7, DeyeSunHybrid.ChannelId.TIME_OF_USE_SUNDAY) //
						), m(DeyeSunHybrid.ChannelId.GRID_PHASE_SEQUENCE, new UnsignedWordElement(147)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_1, new UnsignedWordElement(148)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_2, new UnsignedWordElement(149)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_3, new UnsignedWordElement(150)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_4, new UnsignedWordElement(151)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_5, new UnsignedWordElement(152)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_6, new UnsignedWordElement(153)),

						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_1_POWER, new UnsignedWordElement(154)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_2_POWER, new UnsignedWordElement(155)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_3_POWER, new UnsignedWordElement(156)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_4_POWER, new UnsignedWordElement(157)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_5_POWER, new UnsignedWordElement(158)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_6_POWER, new UnsignedWordElement(159)),

						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_1_VOLTAGE, new UnsignedWordElement(160),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_2_VOLTAGE, new UnsignedWordElement(161),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_3_VOLTAGE, new UnsignedWordElement(162),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_4_VOLTAGE, new UnsignedWordElement(163),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_5_VOLTAGE, new UnsignedWordElement(164),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_6_VOLTAGE, new UnsignedWordElement(165),
								ElementToChannelConverter.SCALE_FACTOR_1),

						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_1_CAPACITY, new UnsignedWordElement(166)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_2_CAPACITY, new UnsignedWordElement(167)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_3_CAPACITY, new UnsignedWordElement(168)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_4_CAPACITY, new UnsignedWordElement(169)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_5_CAPACITY, new UnsignedWordElement(170)),
						m(DeyeSunHybrid.ChannelId.SELL_MODE_TIME_POINT_6_CAPACITY, new UnsignedWordElement(171)),

						// Charge Mode points
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_1, new UnsignedWordElement(172)),
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_2, new UnsignedWordElement(173)),
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_3, new UnsignedWordElement(174)),
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_4, new UnsignedWordElement(175)),
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_5, new UnsignedWordElement(176)),
						m(DeyeSunHybrid.ChannelId.CHARGE_MODE_TIME_POINT_6, new UnsignedWordElement(177))

				),

				new FC3ReadRegistersTask(586, Priority.LOW,
						m(DeyeSunHybrid.ChannelId.BATTERY_TEMPERATURE, new UnsignedWordElement(586),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(DeyeSunHybrid.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(587),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(588)), new DummyRegisterElement(589),
						m(DeyeSunHybrid.ChannelId.BATTERY_OUTPUT_POWER, new SignedWordElement(590)),
						m(DeyeSunHybrid.ChannelId.BATTERY_OUTPUT_CURRENT, new SignedWordElement(591),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.BATTERY_CORRECTED_AH, new UnsignedWordElement(592))),

				new FC3ReadRegistersTask(607, Priority.HIGH, // Outputs
						m(DeyeSunHybrid.ChannelId.GRID_OUTPUT_ACTIVE_POWER, new SignedWordElement(607)),						
						new DummyRegisterElement(608, 621),
						// not totally clear. Maybe external generator is included?
						m(DeyeSunHybrid.ChannelId.GRID_OUTPUT_ACTIVE_POWER_L1, new SignedWordElement(622)),
						m(DeyeSunHybrid.ChannelId.GRID_OUTPUT_ACTIVE_POWER_L2, new SignedWordElement(623)),
						m(DeyeSunHybrid.ChannelId.GRID_OUTPUT_ACTIVE_POWER_L3, new SignedWordElement(624)),
						//m(DeyeSunHybrid.ChannelId.GRID_OUTPUT_ACTIVE_POWER, new SignedWordElement(625)),
						new DummyRegisterElement(625,626),
						m(DeyeSunHybrid.ChannelId.GRID_OUTPUT_VOLTAGE_L1, new UnsignedWordElement(627),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(DeyeSunHybrid.ChannelId.GRID_OUTPUT_VOLTAGE_L2, new UnsignedWordElement(628),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(DeyeSunHybrid.ChannelId.GRID_OUTPUT_VOLTAGE_L3, new UnsignedWordElement(629),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(DeyeSunHybrid.ChannelId.GRID_OUTPUT_CURRENT_L1, new SignedWordElement(630),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.GRID_OUTPUT_CURRENT_L2, new SignedWordElement(631),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(DeyeSunHybrid.ChannelId.GRID_OUTPUT_CURRENT_L3, new SignedWordElement(632),
								ElementToChannelConverter.SCALE_FACTOR_1),

						m(DeyeSunHybrid.ChannelId.POWER_L1, new SignedWordElement(633)),
						m(DeyeSunHybrid.ChannelId.POWER_L2, new SignedWordElement(634)),
						m(DeyeSunHybrid.ChannelId.POWER_L3, new SignedWordElement(635)),
						// m(DeyeSunHybrid.ChannelId.ACTIVE_POWER, new SignedWordElement(636)),
						m(SymmetricEss.ChannelId.ACTIVE_POWER, new SignedWordElement(636)), // negative values for
																							// Charge; positive for
																							// Discharge
						m(DeyeSunHybrid.ChannelId.APPARENT_POWER, new SignedWordElement(637)))

		// m(DeyeSunHybrid.ChannelId.MAX_SOLAR_SELL_POWER, new
		// UnsignedWordElement(340))),

		);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	public String collectDebugData() {
		// Collect channel values in one stream
		return Stream
				.of(DeyeSunHybrid.ChannelId.values(), SymmetricEss.ChannelId.values(),
						ManagedSymmetricEss.ChannelId.values(), HybridEss.ChannelId.values())
				.flatMap(Arrays::stream).map(id -> {
					try {
						return id.name() + "=" + this.channel(id).value().asString();
					} catch (Exception e) {
						return id.name() + "=n/a";
					}
				}).collect(Collectors.joining("; \n"));
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {

			if (this.config.extendedDebugMode()) {
				this.logInfo(log,
						"\n ############################################## ESS Values Start #############################################");
				this.logInfo(log, this.collectDebugData());
				this.logInfo(log,
						"\n ############################################## ESS Values End #############################################");
			}
			this.logInfo(log, message);

		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Active Power:" + this.channel(SymmetricEss.ChannelId.ACTIVE_POWER).value().asStringWithoutUnit() // negative
																														// values
																														// for
																														// Charge;
																														// positive
																														// for
																														// Discharge
				+ ";" + "|Allowed:"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";"
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			// this.applyPowerLimitOnPowerDecreaseCausedByOvertemperatureError();

			this.calculateEnergy();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			// this.calculateAllowedChargeDischargePower();
			this.getAndSetChannels();
			this.allowedChargeDischargeHandler.accept(this.componentManager);
			/*
			 * if (this.applyPowerHandler != null) {
			 * this.applyPowerHandler.calculateMaxAcPower(this.getMaxApparentPower().orElse(
			 * 0)); }
			 */
			this.defineWorkState();
			break;
		}
	}

	
	
	private void defineWorkState() {
		/*
		 * Set ESS in running mode
		 */
		// TODO this should be smarter: set in energy saving mode if there was no output
		// power for a while and we don't need emergency power.
		if (this.getWorkState() != WorkState.NORMAL || this.lastDefineWorkState == null) {
			
			if (this.battery == null || this.battery.getStartStop() != StartStop.START || this.battery.getRunState() != BatteryRunState.NORMAL ) {
				this.changeState(WorkState.WARNING);
				this.logWarn(log, "No battery connected or not fully initialzied");
				return;
			}

	        // --- BMS COMMUNICATION ERROR handling ---
	        boolean commError = DeyeSunHybrid.isBmsCommError(this);
	        if (commError) {
	            // set battery offine
	            battery.setOfflineByExternal("BMS Communication Error");
	            lastHadCommError = true;
	        } else if (lastHadCommError) {
	            // 
	            battery.clearExternalOffline();
	            lastHadCommError = false;
	        }			
			
			if (this.dcCharger == null) {
				this.changeState(WorkState.WARNING);						
				this.logWarn(log, "DC Charger not connected or not fully initialized");
				return;
			}

			// Charge from grid is not allowed at startup
			if (!this.checkEssInitialValues()) {
				this.setEssInitialValues();
				this.changeState(WorkState.INITIALIZING);				
				return;
			}

			if (this.battery.hasError()) {
				
				this.changeState(WorkState.ERROR);				
				this.logError(log, "Error in battery component");
				return;
			}

			if (this.dcCharger.hasError()) {
				this.changeState(WorkState.ERROR);
				this.logError(log, "Error in DC Charger component");
				return;
			}

			this.setWorkState(WorkState.NORMAL);

		}
	}
	
	/**
	 * Changes the state if hysteresis time passed, to avoid too quick changes.
	 *
	 * @param nextState the target state
	 * @return whether the state was changed
	 */
	private boolean changeState(WorkState nextState) {
		var now = LocalDateTime.now();
		
		// avoid early transistions
		if(!now.minusSeconds(20).isAfter(this.lastDefineWorkState)) {
			return false;
		}
		this.lastDefineWorkState = now;
		
		if (this.getWorkState() == nextState) {
			return false;
		}
		
		this.setWorkState(nextState);
		return true;
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	public Logger getLogger() {
		return this.log;
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
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}


	public void getAndSetChannels() {
		if (this.battery == null) {
			this.logError(log, "No battery connected. No value for DC Discharge available");
			return;
		}

		Integer dcPower = this.battery.getDcPower().get();
		this._setDcDischargePower(dcPower); // channel of HybridEss

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
		var dcDischargePower = this.getDcDischargePower().get();

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

	@Override
	public int getCycleTime() {
		return this.cycle != null ? this.cycle.getCycleTime() : DEFAULT_CYCLE_TIME;
	}

	// References for battery and dc-charger have to be up-to-date
	protected void setBattery(DeyeSunBattery battery) {
		this.battery = battery;
		this.applyPowerHandler = new ApplyPowerHandler(this, this.battery, this.dcCharger);
		this.allowedChargeDischargeHandler = new AllowedChargeDischargeHandler(this, this.battery, this.dcCharger);
	}

	protected void unsetBattery(DeyeSunBattery battery) {
		this.battery = null;
		this.applyPowerHandler = null;
		this.allowedChargeDischargeHandler = null;
	}

	//
	protected void setDcCharger(DeyeDcCharger dcCharger) {
		this.dcCharger = dcCharger;
		this.applyPowerHandler = new ApplyPowerHandler(this, this.battery, this.dcCharger);
		this.allowedChargeDischargeHandler = new AllowedChargeDischargeHandler(this, this.battery, this.dcCharger);
	}

	protected void unsetDcCharger(DeyeDcCharger dcCharger) {
		this.dcCharger = null;
		this.applyPowerHandler = null;
		this.allowedChargeDischargeHandler = null;
	}

	@Override
	public Integer getSurplusPower() {
		if (dcCharger == null) {
			return null;
		}

		Integer pvPower = dcCharger.getActualPower().orElse(null);
		if (pvPower == null || pvPower <= 0) {
			return 0;
		}

		// Check if battery is full → can't use PV to charge
		Integer soc = this.getSoc().orElse(null);
		if (soc != null && soc >= 100) {
			return 0;
		}

		return pvPower;
	}

	public boolean checkEssInitialValues() {
		boolean ok = false;


		try {
			// should be 255 on register 146
			ok = this.getTimeOfUseSellingEnabled() && this.getTimeOfUseMonday() && this.getTimeOfUseTuesday()
					&& this.getTimeOfUseWednesday() && this.getTimeOfUseThursday() && this.getTimeOfUseFriday()
					&& this.getTimeOfUseSaturday() && this.getTimeOfUseSunday()
					// Time Points
					&& this.getSellModeTimePoint1().get() == 0 && this.getSellModeTimePoint2().get() == 2355
					&& this.getChargeModeTimePoint1().get() == 3 && this.getChargeModeTimePoint2().get() == 3
					//&& (this.getLimitControlFunction() == LimitControlFunction.SELLING_ACTIVE)
					&& (this.getPowerToGridTarget().get() == this.config.maxSellToGridPower());
		} catch (Exception e) {
			this.logError(this.log, "Unable to get initial values. ERROR: " + e.getMessage());
		}
		return ok;
	}

	public void setEssInitialValues() {

		try {
			this.setTimeOfUseSellingEnabled(true);
			this.setTimeOfUseMonday(true);
			this.setTimeOfUseTuesday(true);
			this.setTimeOfUseWednesday(true);
			this.setTimeOfUseThursday(true);
			this.setTimeOfUseFriday(true);
			this.setTimeOfUseSaturday(true);
			this.setTimeOfUseSunday(true);

			this.setSellModeTimePoint1(0);
			this.setSellModeTimePoint2(2355);

			// Allow Charge from grid / generator
			this.setChargeModeTimePoint1(3);
			//this.setLimitControlFunction(LimitControlFunction.SELLING_ACTIVE); 
			
			// max power to grid including pv production
			this._setPowerToGridTarget(this.config.maxSellToGridPower());

		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Unable to set initial values for ESS: " + e.getMessage());
		}

	}

	public boolean getChargeMode() {
		return this.chargeMode;
	}

	// used for charge from grid and discharging
	public void setChargeDischargeMode(boolean enableChargeMode, int targetPower) {


		int capacity = this.config.minBatteryCapacity();

		try {
			if (enableChargeMode) {
				capacity = this.battery.getBatteryCapacity().getOrError();
			}

			// To allow charging, set target capacity to 5%, to allow charge from grid we
			// have to set full battery capacity
			if (this.getSellModeTimePoint1Capacity().get() == null || this.getSellModeTimePoint1Capacity().get() != capacity ) {
				this.setSellModeTimePoint1Capacity(capacity);
			}
			
			this.setSellModeTimePoint1Power(targetPower);

			this.chargeMode = enableChargeMode;
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Unable to get capacity " + e.getMessage());
		}

	}

}