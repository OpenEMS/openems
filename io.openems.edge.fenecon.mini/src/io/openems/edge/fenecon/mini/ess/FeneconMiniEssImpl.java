package io.openems.edge.fenecon.mini.ess;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SUBTRACT;

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
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoReadAndWrite;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhase;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.fenecon.mini.FeneconMiniConstants;
import io.openems.edge.fenecon.mini.ess.statemachine.Context;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Fenecon.Mini.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class FeneconMiniEssImpl extends AbstractOpenemsModbusComponent
		implements FeneconMiniEss, ManagedSinglePhaseEss, ManagedAsymmetricEss, ManagedSymmetricEss, SinglePhaseEss,
		AsymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent, ModbusSlave, TimedataProvider, EventHandler {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected Power power;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private final Logger log = LoggerFactory.getLogger(FeneconMiniEssImpl.class);
	private final MaxApparentPowerHandler maxApparentPowerHandler = new MaxApparentPowerHandler(this);

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private final CalculateEnergyFromPower calculateChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	private Config config;

	public FeneconMiniEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				SinglePhaseEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ManagedAsymmetricEss.ChannelId.values(), //
				ManagedSinglePhaseEss.ChannelId.values(), //
				FeneconMiniEss.SystemErrorChannelId.values(), //
				FeneconMiniEss.ServiceInfoChannelId.values(), //
				FeneconMiniEss.ChannelId.values() //
		);
		this._setMaxApparentPower(FeneconMiniEss.MAX_APPARENT_POWER);
		this._setCapacity(3_000);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), FeneconMiniConstants.UNIT_ID,
				this.cm, "Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		SinglePhaseEss.initializeCopyPhaseChannel(this, config.phase());

		// Calculate Allowed Charge and Discharge Power
		final Consumer<Value<Integer>> calculateAllowedChargeDischargePower = ignore -> {
			var voltage = this.getBecu1TotalVoltageChannel().getNextValue();
			var chargeCurrent = this.getBecu1AllowedChargeCurrentChannel().getNextValue();
			var dischargeCurrent = this.getBecu1AllowedDischargeCurrentChannel().getNextValue();
			final Integer allowedCharge;
			final Integer allowedDischarge;
			if (voltage.isDefined() && chargeCurrent.isDefined() && dischargeCurrent.isDefined()) {
				allowedCharge = Math.round(voltage.get() /* [mV] */ / 1000F /* convert to [V] */
						* chargeCurrent.get() /* [mA] */ / 1000F /* convert to [A] */
						* -1 /* charge */);
				allowedDischarge = Math.round(voltage.get() /* [mV] */ / 1000F /* convert to [V] */
						* dischargeCurrent.get() /* [mA] */ / 1000F /* convert to [A] */);
			} else {
				allowedCharge = null;
				allowedDischarge = null;
			}
			this._setAllowedChargePower(allowedCharge);
			this._setAllowedDischargePower(allowedDischarge);
		};
		this.getBecu1TotalVoltageChannel().onSetNextValue(calculateAllowedChargeDischargePower);
		this.getBecu1AllowedChargeCurrentChannel().onSetNextValue(calculateAllowedChargeDischargePower);
		this.getBecu1AllowedDischargeCurrentChannel().onSetNextValue(calculateAllowedChargeDischargePower);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public SinglePhase getPhase() {
		return this.config.phase();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(100, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.SYSTEM_STATE, new UnsignedWordElement(100)), //
						m(FeneconMiniEss.ChannelId.CONTROL_MODE, new UnsignedWordElement(101)), //
						new DummyRegisterElement(102, 107), //
						m(FeneconMiniEss.ChannelId.BATTERY_GROUP_STATE, new UnsignedWordElement(108)), //
						new DummyRegisterElement(109), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE, new UnsignedWordElement(110)), //
						m(FeneconMiniEss.ChannelId.BATTERY_CURRENT, new SignedWordElement(111)), //
						m(FeneconMiniEss.ChannelId.BATTERY_POWER, new SignedWordElement(112))), //
				new FC3ReadRegistersTask(2007, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L1, new UnsignedWordElement(2007),
								UNSIGNED_POWER_CONVERTER)), //
				new FC3ReadRegistersTask(2107, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L2, new UnsignedWordElement(2107),
								UNSIGNED_POWER_CONVERTER)), //
				new FC3ReadRegistersTask(2207, Priority.HIGH, //
						m(AsymmetricEss.ChannelId.ACTIVE_POWER_L3, new UnsignedWordElement(2207),
								UNSIGNED_POWER_CONVERTER)), //
				new FC3ReadRegistersTask(3000, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.BECU1_ALLOWED_CHARGE_CURRENT, new UnsignedWordElement(3000),
								SCALE_FACTOR_2), //
						m(FeneconMiniEss.ChannelId.BECU1_ALLOWED_DISCHARGE_CURRENT, new UnsignedWordElement(3001),
								SCALE_FACTOR_2), //
						m(FeneconMiniEss.ChannelId.BECU1_TOTAL_VOLTAGE, new UnsignedWordElement(3002), SCALE_FACTOR_2), //
						m(FeneconMiniEss.ChannelId.BECU1_TOTAL_CURRENT, new UnsignedWordElement(3003)), //
						m(FeneconMiniEss.ChannelId.BECU1_SOC, new UnsignedWordElement(3004)), //
						m(new BitsWordElement(3005, this) //
								.bit(0, FeneconMiniEss.ChannelId.STATE_1) //
								.bit(1, FeneconMiniEss.ChannelId.STATE_2) //
								.bit(2, FeneconMiniEss.ChannelId.STATE_3) //
								.bit(3, FeneconMiniEss.ChannelId.STATE_4) //
								.bit(4, FeneconMiniEss.ChannelId.STATE_5) //
								.bit(5, FeneconMiniEss.ChannelId.STATE_6) //
								.bit(6, FeneconMiniEss.ServiceInfoChannelId.STATE_7) //
								.bit(7, FeneconMiniEss.ChannelId.STATE_8) //
								.bit(8, FeneconMiniEss.ChannelId.STATE_9) //
								.bit(9, FeneconMiniEss.ChannelId.STATE_10) //
								.bit(10, FeneconMiniEss.ServiceInfoChannelId.STATE_11) //
								.bit(11, FeneconMiniEss.ServiceInfoChannelId.STATE_12) //
								.bit(12, FeneconMiniEss.ServiceInfoChannelId.STATE_13) //
								.bit(13, FeneconMiniEss.ServiceInfoChannelId.STATE_14) //
								.bit(14, FeneconMiniEss.ServiceInfoChannelId.STATE_15) //
								.bit(15, FeneconMiniEss.ChannelId.STATE_16)), //
						m(new BitsWordElement(3006, this) //
								.bit(0, FeneconMiniEss.ChannelId.STATE_17) //
								.bit(1, FeneconMiniEss.ChannelId.STATE_18) //
								.bit(2, FeneconMiniEss.ChannelId.STATE_19) //
								.bit(4, FeneconMiniEss.ChannelId.STATE_20) //
								.bit(5, FeneconMiniEss.ServiceInfoChannelId.STATE_21) //
								.bit(6, FeneconMiniEss.ChannelId.STATE_22) //
								.bit(7, FeneconMiniEss.ChannelId.STATE_23) //
								.bit(8, FeneconMiniEss.ChannelId.STATE_24) //
								.bit(9, FeneconMiniEss.ChannelId.STATE_25) //
								.bit(10, FeneconMiniEss.ChannelId.STATE_26) //
								.bit(11, FeneconMiniEss.ChannelId.STATE_27) //
								.bit(12, FeneconMiniEss.ChannelId.STATE_28) //
								.bit(13, FeneconMiniEss.ServiceInfoChannelId.STATE_29) //
								.bit(14, FeneconMiniEss.ServiceInfoChannelId.STATE_30) //
								.bit(15, FeneconMiniEss.ServiceInfoChannelId.STATE_31)), //
						m(new BitsWordElement(3007, this) //
								.bit(0, FeneconMiniEss.ServiceInfoChannelId.STATE_32) //
								.bit(1, FeneconMiniEss.ServiceInfoChannelId.STATE_33) //
								.bit(2, FeneconMiniEss.SystemErrorChannelId.STATE_34) //
								.bit(3, FeneconMiniEss.ServiceInfoChannelId.STATE_35) //
								.bit(4, FeneconMiniEss.ChannelId.STATE_36) //
								.bit(5, FeneconMiniEss.ServiceInfoChannelId.STATE_37) //
								.bit(6, FeneconMiniEss.ServiceInfoChannelId.STATE_38) //
								.bit(7, FeneconMiniEss.ServiceInfoChannelId.STATE_39) //
								.bit(8, FeneconMiniEss.ServiceInfoChannelId.STATE_40) //
								.bit(9, FeneconMiniEss.ServiceInfoChannelId.STATE_41) //
								.bit(10, FeneconMiniEss.ChannelId.STATE_42) //
								.bit(13, FeneconMiniEss.ServiceInfoChannelId.STATE_43) //
								.bit(14, FeneconMiniEss.ChannelId.STATE_44) //
								.bit(15, FeneconMiniEss.ChannelId.STATE_45)), //
						m(new BitsWordElement(3008, this) //
								.bit(0, FeneconMiniEss.ChannelId.STATE_46) //
								.bit(1, FeneconMiniEss.ChannelId.STATE_47) //
								.bit(2, FeneconMiniEss.ServiceInfoChannelId.STATE_48) //
								.bit(9, FeneconMiniEss.ServiceInfoChannelId.STATE_49) //
								.bit(10, FeneconMiniEss.ServiceInfoChannelId.STATE_50) //
								.bit(12, FeneconMiniEss.ServiceInfoChannelId.STATE_51) //
								.bit(13, FeneconMiniEss.ServiceInfoChannelId.STATE_52)), //
						m(FeneconMiniEss.ChannelId.BECU1_VERSION, new UnsignedWordElement(3009)), //
						m(FeneconMiniEss.ChannelId.BECU1_NOMINAL_CAPACITY, new UnsignedWordElement(3010)), //
						m(FeneconMiniEss.ChannelId.BECU1_CURRENT_CAPACITY, new UnsignedWordElement(3011)), //
						m(FeneconMiniEss.ChannelId.BECU1_MINIMUM_VOLTAGE_NO, new UnsignedWordElement(3012)), //
						m(FeneconMiniEss.ChannelId.BECU1_MINIMUM_VOLTAGE, new UnsignedWordElement(3013)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAXIMUM_VOLTAGE_NO, new UnsignedWordElement(3014)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAXIMUM_VOLTAGE, new UnsignedWordElement(3015)), //
						m(FeneconMiniEss.ChannelId.BECU1_MINIMUM_TEMPERATURE_NO, new UnsignedWordElement(3016)), //
						m(FeneconMiniEss.ChannelId.BECU1_MINIMUM_TEMPERATURE, new UnsignedWordElement(3017),
								SUBTRACT(40)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAXIMUM_TEMPERATURE_NO, new UnsignedWordElement(3018)), //
						m(FeneconMiniEss.ChannelId.BECU1_MAXIMUM_TEMPERATURE, new UnsignedWordElement(3019),
								SUBTRACT(40))),
				new FC3ReadRegistersTask(3020, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_1, new UnsignedWordElement(3020)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_2, new UnsignedWordElement(3021)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_3, new UnsignedWordElement(3022)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_4, new UnsignedWordElement(3023)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_5, new UnsignedWordElement(3024)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_6, new UnsignedWordElement(3025)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_7, new UnsignedWordElement(3026)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_8, new UnsignedWordElement(3027)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_9, new UnsignedWordElement(3028)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_10, new UnsignedWordElement(3029)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_11, new UnsignedWordElement(3030)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_12, new UnsignedWordElement(3031)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_13, new UnsignedWordElement(3032)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_14, new UnsignedWordElement(3033)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_15, new UnsignedWordElement(3034)), //
						m(FeneconMiniEss.ChannelId.BATTERY_VOLTAGE_SECTION_16, new UnsignedWordElement(3035)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_1, new UnsignedWordElement(3036)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_2, new UnsignedWordElement(3037)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_3, new UnsignedWordElement(3038)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_4, new UnsignedWordElement(3039)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_5, new UnsignedWordElement(3040)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_6, new UnsignedWordElement(3041)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_7, new UnsignedWordElement(3042)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_8, new UnsignedWordElement(3043)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_9, new UnsignedWordElement(3044)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_10, new UnsignedWordElement(3045)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_11, new UnsignedWordElement(3046)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_12, new UnsignedWordElement(3047)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_13, new UnsignedWordElement(3048)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_14, new UnsignedWordElement(3049)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_15, new UnsignedWordElement(3050)), //
						m(FeneconMiniEss.ChannelId.BATTERY_TEMPERATURE_SECTION_16, new UnsignedWordElement(3051))), //
				new FC3ReadRegistersTask(3200, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.BECU2_CHARGE_CURRENT, new UnsignedWordElement(3200)), //
						m(FeneconMiniEss.ChannelId.BECU2_DISCHARGE_CURRENT, new UnsignedWordElement(3201)), //
						m(FeneconMiniEss.ChannelId.BECU2_VOLT, new UnsignedWordElement(3202)), //
						m(FeneconMiniEss.ChannelId.BECU2_CURRENT, new UnsignedWordElement(3203)), //
						m(FeneconMiniEss.ChannelId.BECU2_SOC, new UnsignedWordElement(3204))), //
				new FC3ReadRegistersTask(3205, Priority.LOW, //
						m(new BitsWordElement(3205, this) //
								.bit(0, FeneconMiniEss.ServiceInfoChannelId.STATE_53) //
								.bit(1, FeneconMiniEss.ServiceInfoChannelId.STATE_54) //
								.bit(2, FeneconMiniEss.ServiceInfoChannelId.STATE_55) //
								.bit(3, FeneconMiniEss.ServiceInfoChannelId.STATE_56) //
								.bit(4, FeneconMiniEss.ServiceInfoChannelId.STATE_57) //
								.bit(5, FeneconMiniEss.ServiceInfoChannelId.STATE_58) //
								.bit(6, FeneconMiniEss.ServiceInfoChannelId.STATE_59) //
								.bit(7, FeneconMiniEss.ServiceInfoChannelId.STATE_60) //
								.bit(8, FeneconMiniEss.ServiceInfoChannelId.STATE_61) //
								.bit(9, FeneconMiniEss.ServiceInfoChannelId.STATE_62) //
								.bit(10, FeneconMiniEss.ServiceInfoChannelId.STATE_63) //
								.bit(11, FeneconMiniEss.ServiceInfoChannelId.STATE_64) //
								.bit(12, FeneconMiniEss.ServiceInfoChannelId.STATE_65) //
								.bit(13, FeneconMiniEss.ServiceInfoChannelId.STATE_66) //
								.bit(14, FeneconMiniEss.ServiceInfoChannelId.STATE_67) //
								.bit(15, FeneconMiniEss.ServiceInfoChannelId.STATE_68)), //
						m(new BitsWordElement(3206, this) //
								.bit(0, FeneconMiniEss.ServiceInfoChannelId.STATE_69) //
								.bit(1, FeneconMiniEss.ServiceInfoChannelId.STATE_70) //
								.bit(2, FeneconMiniEss.ServiceInfoChannelId.STATE_71) //
								.bit(4, FeneconMiniEss.ServiceInfoChannelId.STATE_72) //
								.bit(5, FeneconMiniEss.ServiceInfoChannelId.STATE_73) //
								.bit(6, FeneconMiniEss.ServiceInfoChannelId.STATE_74) //
								.bit(7, FeneconMiniEss.ServiceInfoChannelId.STATE_75) //
								.bit(8, FeneconMiniEss.ServiceInfoChannelId.STATE_76) //
								.bit(9, FeneconMiniEss.ServiceInfoChannelId.STATE_77) //
								.bit(10, FeneconMiniEss.ServiceInfoChannelId.STATE_78) //
								.bit(11, FeneconMiniEss.ServiceInfoChannelId.STATE_79) //
								.bit(12, FeneconMiniEss.ServiceInfoChannelId.STATE_80) //
								.bit(13, FeneconMiniEss.ServiceInfoChannelId.STATE_81) //
								.bit(14, FeneconMiniEss.ServiceInfoChannelId.STATE_82) //
								.bit(15, FeneconMiniEss.ServiceInfoChannelId.STATE_83)),
						m(new BitsWordElement(3207, this) //
								.bit(0, FeneconMiniEss.ServiceInfoChannelId.STATE_84) //
								.bit(1, FeneconMiniEss.ServiceInfoChannelId.STATE_85) //
								.bit(2, FeneconMiniEss.ServiceInfoChannelId.STATE_86) //
								.bit(3, FeneconMiniEss.ServiceInfoChannelId.STATE_87) //
								.bit(4, FeneconMiniEss.ChannelId.STATE_88) //
								.bit(5, FeneconMiniEss.ServiceInfoChannelId.STATE_89) //
								.bit(6, FeneconMiniEss.ServiceInfoChannelId.STATE_90) //
								.bit(7, FeneconMiniEss.ServiceInfoChannelId.STATE_91) //
								.bit(8, FeneconMiniEss.ServiceInfoChannelId.STATE_92) //
								.bit(9, FeneconMiniEss.ServiceInfoChannelId.STATE_93) //
								.bit(10, FeneconMiniEss.ServiceInfoChannelId.STATE_94) //
								.bit(13, FeneconMiniEss.ServiceInfoChannelId.STATE_95) //
								.bit(14, FeneconMiniEss.ChannelId.STATE_96) //
								.bit(15, FeneconMiniEss.ChannelId.STATE_97)), //
						m(new BitsWordElement(3208, this) //
								.bit(0, FeneconMiniEss.ChannelId.STATE_98) //
								.bit(1, FeneconMiniEss.ChannelId.STATE_99) //
								.bit(2, FeneconMiniEss.ServiceInfoChannelId.STATE_100) //
								.bit(9, FeneconMiniEss.ServiceInfoChannelId.STATE_101) //
								.bit(10, FeneconMiniEss.ServiceInfoChannelId.STATE_102) //
								.bit(12, FeneconMiniEss.ServiceInfoChannelId.STATE_103) //
								.bit(13, FeneconMiniEss.ServiceInfoChannelId.STATE_104)),
						m(FeneconMiniEss.ChannelId.BECU2_VERSION, new UnsignedWordElement(3209)), //
						new DummyRegisterElement(3210, 3211), //
						m(FeneconMiniEss.ChannelId.BECU2_MIN_VOLT_NO, new UnsignedWordElement(3212)), //
						m(FeneconMiniEss.ChannelId.BECU2_MIN_VOLT, new UnsignedWordElement(3213)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_VOLT_NO, new UnsignedWordElement(3214)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_VOLT, new UnsignedWordElement(3215)), // ^
						m(FeneconMiniEss.ChannelId.BECU2_MIN_TEMP_NO, new UnsignedWordElement(3216)), //
						m(FeneconMiniEss.ChannelId.BECU2_MIN_TEMP, new UnsignedWordElement(3217)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_TEMP_NO, new UnsignedWordElement(3218)), //
						m(FeneconMiniEss.ChannelId.BECU2_MAX_TEMP, new UnsignedWordElement(3219))), //
				new FC3ReadRegistersTask(4000, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.SYSTEM_WORK_STATE, new UnsignedDoublewordElement(4000)), //
						m(FeneconMiniEss.ChannelId.SYSTEM_WORK_MODE_STATE, new UnsignedDoublewordElement(4002))), //
				new FC3ReadRegistersTask(4800, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.BECU_NUM, new UnsignedWordElement(4800)), //
						// TODO BECU_WORK_STATE has been implemented with both registers(4801 and 4807)
						m(FeneconMiniEss.ChannelId.BECU_WORK_STATE, new UnsignedWordElement(4801)), //
						new DummyRegisterElement(4802), //
						m(FeneconMiniEss.ChannelId.BECU_CHARGE_CURRENT, new UnsignedWordElement(4803)), //
						m(FeneconMiniEss.ChannelId.BECU_DISCHARGE_CURRENT, new UnsignedWordElement(4804)), //
						m(FeneconMiniEss.ChannelId.BECU_VOLT, new UnsignedWordElement(4805)), //
						m(FeneconMiniEss.ChannelId.BECU_CURRENT, new UnsignedWordElement(4806))), //
				new FC16WriteRegistersTask(4809, //
						m(new BitsWordElement(4809, this) //
								.bit(0, FeneconMiniEss.ServiceInfoChannelId.STATE_111) //
								.bit(1, FeneconMiniEss.ServiceInfoChannelId.STATE_112))), //
				new FC3ReadRegistersTask(4808, Priority.LOW, //
						m(new BitsWordElement(4808, this) //
								.bit(0, FeneconMiniEss.SystemErrorChannelId.STATE_105) //
								.bit(1, FeneconMiniEss.ServiceInfoChannelId.STATE_106) //
								.bit(2, FeneconMiniEss.ServiceInfoChannelId.STATE_107) //
								.bit(3, FeneconMiniEss.ServiceInfoChannelId.STATE_108) //
								.bit(4, FeneconMiniEss.ServiceInfoChannelId.STATE_109) //
								.bit(9, FeneconMiniEss.ServiceInfoChannelId.STATE_110)), //
						m(new BitsWordElement(4809, this) //
								.bit(0, FeneconMiniEss.ServiceInfoChannelId.STATE_111) //
								.bit(1, FeneconMiniEss.ServiceInfoChannelId.STATE_112)), //
						m(new BitsWordElement(4810, this) //
								.bit(0, FeneconMiniEss.ServiceInfoChannelId.STATE_113) //
								.bit(1, FeneconMiniEss.ServiceInfoChannelId.STATE_114) //
								.bit(2, FeneconMiniEss.ServiceInfoChannelId.STATE_115) //
								.bit(3, FeneconMiniEss.ServiceInfoChannelId.STATE_116) //
								.bit(4, FeneconMiniEss.ChannelId.STATE_117) //
								.bit(5, FeneconMiniEss.ChannelId.STATE_118) //
								.bit(6, FeneconMiniEss.ServiceInfoChannelId.STATE_119) //
								.bit(7, FeneconMiniEss.ServiceInfoChannelId.STATE_120) //
								.bit(8, FeneconMiniEss.ServiceInfoChannelId.STATE_121) //
								.bit(9, FeneconMiniEss.ServiceInfoChannelId.STATE_122) //
								.bit(10, FeneconMiniEss.ServiceInfoChannelId.STATE_123) //
								.bit(11, FeneconMiniEss.ServiceInfoChannelId.STATE_124) //
								.bit(12, FeneconMiniEss.ServiceInfoChannelId.STATE_125) //
								.bit(13, FeneconMiniEss.ServiceInfoChannelId.STATE_126) //
								.bit(14, FeneconMiniEss.ServiceInfoChannelId.STATE_127) //
								.bit(15, FeneconMiniEss.ServiceInfoChannelId.STATE_128)), //
						m(new BitsWordElement(4811, this) //
								.bit(0, FeneconMiniEss.ServiceInfoChannelId.STATE_129) //
								.bit(1, FeneconMiniEss.ServiceInfoChannelId.STATE_130) //
								.bit(2, FeneconMiniEss.ServiceInfoChannelId.STATE_131) //
								.bit(3, FeneconMiniEss.ServiceInfoChannelId.STATE_132) //
								.bit(4, FeneconMiniEss.ServiceInfoChannelId.STATE_133) //
								.bit(5, FeneconMiniEss.ChannelId.STATE_134) //
								.bit(6, FeneconMiniEss.ServiceInfoChannelId.STATE_135) //
								.bit(7, FeneconMiniEss.ServiceInfoChannelId.STATE_136) //
								.bit(8, FeneconMiniEss.ServiceInfoChannelId.STATE_137) //
								.bit(9, FeneconMiniEss.ChannelId.STATE_138) //
								.bit(10, FeneconMiniEss.ChannelId.STATE_139) //
								.bit(11, FeneconMiniEss.ServiceInfoChannelId.STATE_140) //
								.bit(12, FeneconMiniEss.ServiceInfoChannelId.STATE_141) //
								.bit(14, FeneconMiniEss.ChannelId.STATE_143)),
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(4812),
								new ElementToChannelConverter(value -> {
									// Set SoC to 100 % if battery is full and AllowedCharge is zero
									if (value == null) {
										return null;
									}
									int soc = (Integer) value;
									if (soc > 95 && this.getAllowedChargePower().orElse(-1) == 0
											&& this.getAllowedDischargePower().orElse(0) != 0) {
										return 100;
									}
									return value;
								}, //
										value -> value)) //
				), //

				new FC16WriteRegistersTask(9014, //
						m(FeneconMiniEss.ChannelId.RTC_YEAR, new UnsignedWordElement(9014)), //
						m(FeneconMiniEss.ChannelId.RTC_MONTH, new UnsignedWordElement(9015)), //
						m(FeneconMiniEss.ChannelId.RTC_DAY, new UnsignedWordElement(9016)), //
						m(FeneconMiniEss.ChannelId.RTC_HOUR, new UnsignedWordElement(9017)), //
						m(FeneconMiniEss.ChannelId.RTC_MINUTE, new UnsignedWordElement(9018)), //
						m(FeneconMiniEss.ChannelId.RTC_SECOND, new UnsignedWordElement(9019))), //
				new FC16WriteRegistersTask(30526, //
						m(FeneconMiniEss.ChannelId.GRID_MAX_CHARGE_CURRENT, new UnsignedWordElement(30526),
								SCALE_FACTOR_2, new ChannelMetaInfoReadAndWrite(30126, 30526)), //
						m(FeneconMiniEss.ChannelId.GRID_MAX_DISCHARGE_CURRENT, new UnsignedWordElement(30527),
								SCALE_FACTOR_2, new ChannelMetaInfoReadAndWrite(30127, 30527))), //
				new FC16WriteRegistersTask(30558, //
						m(FeneconMiniEss.ChannelId.SETUP_MODE, new UnsignedWordElement(30558),
								new ChannelMetaInfoReadAndWrite(30157, 30558))), //
				new FC16WriteRegistersTask(30559, //
						m(FeneconMiniEss.ChannelId.PCS_MODE, new UnsignedWordElement(30559),
								new ChannelMetaInfoReadAndWrite(30158, 30559))), //

				new FC3ReadRegistersTask(30126, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.GRID_MAX_CHARGE_CURRENT, new UnsignedWordElement(30126),
								SCALE_FACTOR_2, new ChannelMetaInfoReadAndWrite(30126, 30526)), //
						m(FeneconMiniEss.ChannelId.GRID_MAX_DISCHARGE_CURRENT, new UnsignedWordElement(30127),
								SCALE_FACTOR_2, new ChannelMetaInfoReadAndWrite(30127, 30527)), //
						new DummyRegisterElement(30128, 30156), //
						m(FeneconMiniEss.ChannelId.SETUP_MODE, new UnsignedWordElement(30157),
								new ChannelMetaInfoReadAndWrite(30157, 30558)), //
						m(FeneconMiniEss.ChannelId.PCS_MODE, new UnsignedWordElement(30158),
								new ChannelMetaInfoReadAndWrite(30158, 30559)), //
						new DummyRegisterElement(30159, 30165), //
						m(SymmetricEss.ChannelId.GRID_MODE, new UnsignedWordElement(30166),
								new ElementToChannelConverter(
										// element -> channel
										value -> {
											var intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
											if (intValue != null) {
												switch (intValue) {
												case 0:
													return GridMode.ON_GRID;
												case 1:
													return GridMode.OFF_GRID;
												}
											}
											return GridMode.UNDEFINED;
										}, //

										// channel -> element
										value -> value))), //

				new FC3ReadRegistersTask(2992, Priority.LOW, //
						m(FeneconMiniEss.ChannelId.DEBUG_RUN_STATE, new UnsignedWordElement(2992))), //
				new FC16WriteRegistersTask(2992, //
						m(FeneconMiniEss.ChannelId.DEBUG_RUN_STATE, new UnsignedWordElement(2992))) //
		);//
	}

	@Override
	public String debugLog() {
		if (this.config == null || this.config.readonly()) {
			return "SoC:" + this.getSoc().asString() //
					+ "|L:" + this.getActivePower().asString(); //
		}
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";"
				+ this.getAllowedDischargePower().asString() //
				+ "|" + this.channel(FeneconMiniEss.ChannelId.STATE_MACHINE).value().asEnum().asCamelCase();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				AsymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(FeneconMiniEssImpl.class, accessMode, 300) //
						.build());
	}

	private static final ElementToChannelConverter UNSIGNED_POWER_CONVERTER = new ElementToChannelConverter(//
			value -> {
				if (value == null) {
					return null;
				}
				int intValue = (Integer) value;
				if (intValue == 0) {
					return 0; // ignore '0'
				}
				return intValue - 10_000; // apply delta of 10_000
			}, //
			value -> value);

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public boolean isManaged() {
		return !this.config.readonly();
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		// Store the current State
		this.channel(FeneconMiniEss.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Prepare Context
		var context = new Context(this, this.config, activePower, reactivePower);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(FeneconMiniEss.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(FeneconMiniEss.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public Constraint[] getStaticConstraints() throws OpenemsNamedException {
		if (this.config.readonly()) {
			return new Constraint[] { //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0), //
					this.createPowerConstraint("Read-Only-Mode", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) //
			};

		}
		if (this.stateMachine.getCurrentState() == State.WRITE_MODE) {
			return new Constraint[] { //
					this.createPowerConstraint("No reactive power", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) //
			};

		} else {
			return new Constraint[] { //
					this.createPowerConstraint("Not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0), //
					this.createPowerConstraint("Not ready", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0) //
			};
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			if (!this.config.readonly()) {
				this.maxApparentPowerHandler.calculateMaxApparentPower();
			}
			break;
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateChargeEnergy.update(null);
			this.calculateDischargeEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Grid
			this.calculateChargeEnergy.update(0);
			this.calculateDischargeEnergy.update(activePower);
		} else {
			// Sell-To-Grid
			this.calculateChargeEnergy.update(activePower * -1);
			this.calculateDischargeEnergy.update(0);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}
}