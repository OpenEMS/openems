package io.openems.edge.batteryinverter.refu88k;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.refu88k.statemachine.Context;
import io.openems.edge.batteryinverter.refu88k.statemachine.StateMachine;
import io.openems.edge.batteryinverter.refu88k.statemachine.StateMachine.State;
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
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery-Inverter.Refu.REFUstore88k", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class RefuStore88kImpl extends AbstractOpenemsModbusComponent implements ManagedSymmetricBatteryInverter,
		SymmetricBatteryInverter, ModbusComponent, OpenemsComponent, RefuStore88k, TimedataProvider, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(RefuStore88kImpl.class);
	private Config config;

	public static final int DEFAULT_UNIT_ID = 1;
	protected static final double EFFICIENCY_FACTOR = 0.98;

	private final CalculateEnergyFromPower calculateChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	private int maxApparentPower = 0;

	@Reference
	private Power power;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	public RefuStore88kImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				RefuStore88kChannelId.values() //
		);
		this._setGridMode(GridMode.ON_GRID);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.config = config;
		this.channel(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER).onChange((oldValue, newValue) -> {
			@SuppressWarnings("unchecked")
			var valueOpt = (Optional<Integer>) newValue.asOptional();
			if (!valueOpt.isPresent()) {
				return;
			}
			IntegerWriteChannel wMaxChannel = this.channel(RefuStore88kChannelId.W_MAX);
			this.maxApparentPower = valueOpt.get();
			try {
				// Set WMax
				wMaxChannel.setNextWriteValue(this.maxApparentPower);
			} catch (OpenemsNamedException e) {
				this.log.error(e.getMessage());
			}
		});
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {
		// Store the current State
		this.channel(RefuStore88k.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		// Set Battery Limits
		this.setBatteryLimits(battery);

		// Calculate the Energy values from ActivePower.
		this.calculateEnergy();

		// Prepare Context
		var context = new Context(this, battery, this.config, setActivePower, setReactivePower);

		// Call the StateMachine
		try {
			this.stateMachine.run(context);

			this.channel(RefuStore88k.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(RefuStore88k.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public BatteryInverterConstraint[] getStaticConstraints() throws OpenemsException {
		var noReactivePower = new BatteryInverterConstraint("Reactive power is not allowed", Phase.ALL, Pwr.REACTIVE,
				Relationship.EQUALS, 0d);

		if (this.stateMachine.getCurrentState() == State.RUNNING) {
			return new BatteryInverterConstraint[] { noReactivePower };

		}
		// Block any power as long as we are not RUNNING
		return new BatteryInverterConstraint[] { //
				noReactivePower, //
				new BatteryInverterConstraint("Refu inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0d) //
		};
	}

	private void setBatteryLimits(Battery battery) throws OpenemsNamedException {

		int maxBatteryChargeValue = battery.getChargeMaxCurrent().orElse(0);
		int maxBatteryDischargeValue = battery.getDischargeMaxCurrent().orElse(0);

		IntegerWriteChannel maxBatAChaChannel = this.channel(RefuStore88kChannelId.MAX_BAT_A_CHA);
		maxBatAChaChannel.setNextWriteValue(maxBatteryChargeValue);

		IntegerWriteChannel maxBatADischaChannel = this.channel(RefuStore88kChannelId.MAX_BAT_A_DISCHA);
		maxBatADischaChannel.setNextWriteValue(maxBatteryDischargeValue);
	}

	@Override
	public int getPowerPrecision() {
		return this.maxApparentPower / 1000;
	}

	/*
	 * Supported Models First available Model = Start Address + 2 = 40002 Then 40002
	 * + Length of Model ....
	 */
	private static final int START_ADDRESS = 40000;
	private static final int SUNSPEC_1 = START_ADDRESS + 2; // Common
	private static final int SUNSPEC_103 = 40070; // Inverter (Three Phase)
	private static final int SUNSPEC_120 = 40122; // Nameplate
	private static final int SUNSPEC_121 = 40150; // Basic Settings
	private static final int SUNSPEC_123 = 40182; // Immediate Controls
	private static final int SUNSPEC_64040 = 40208; // REFU Parameter
	private static final int SUNSPEC_64041 = 40213; // REFU Parameter Value
	private static final int SUNSPEC_64800 = 40225; // MESA-PCS Extensions

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException { // Register
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(SUNSPEC_1, Priority.LOW, //
						m(RefuStore88kChannelId.ID_1, new UnsignedWordElement(SUNSPEC_1)), // 40002
						m(RefuStore88kChannelId.L_1, new UnsignedWordElement(SUNSPEC_1 + 1)), // 40003
						m(RefuStore88kChannelId.MN, new StringWordElement(SUNSPEC_1 + 2, 16)), // 40004
						m(RefuStore88kChannelId.MD, new StringWordElement(SUNSPEC_1 + 18, 16)), // 40020
						m(RefuStore88kChannelId.OPT, new StringWordElement(SUNSPEC_1 + 34, 8)), // 40036
						m(RefuStore88kChannelId.VR, new StringWordElement(SUNSPEC_1 + 42, 8)), // 40044
						m(RefuStore88kChannelId.SN, new StringWordElement(SUNSPEC_1 + 50, 16)), // 40052
						m(RefuStore88kChannelId.DA, new UnsignedWordElement(SUNSPEC_1 + 66)), // 40068
						m(RefuStore88kChannelId.PAD_1, new UnsignedWordElement(SUNSPEC_1 + 67))), // 40069

				new FC3ReadRegistersTask(SUNSPEC_103, Priority.HIGH, //
						m(RefuStore88kChannelId.ID_103, new UnsignedWordElement(SUNSPEC_103)), // 40070
						m(RefuStore88kChannelId.L_103, new UnsignedWordElement(SUNSPEC_103 + 1)), // 40071
						m(RefuStore88kChannelId.A, new UnsignedWordElement(SUNSPEC_103 + 2), // 40072
								SCALE_FACTOR_MINUS_2),
						m(RefuStore88kChannelId.APH_A, new UnsignedWordElement(SUNSPEC_103 + 3), // 40073
								SCALE_FACTOR_MINUS_2),
						m(RefuStore88kChannelId.APH_B, new UnsignedWordElement(SUNSPEC_103 + 4), // 40074
								SCALE_FACTOR_MINUS_2),
						m(RefuStore88kChannelId.APH_C, new UnsignedWordElement(SUNSPEC_103 + 5), // 40075
								SCALE_FACTOR_MINUS_2),
						m(RefuStore88kChannelId.A_SF, new UnsignedWordElement(SUNSPEC_103 + 6)), // 40076
						m(RefuStore88kChannelId.PP_VPH_AB, new UnsignedWordElement(SUNSPEC_103 + 7), // 40077
								SCALE_FACTOR_MINUS_1),
						m(RefuStore88kChannelId.PP_VPH_BC, new UnsignedWordElement(SUNSPEC_103 + 8), // 40078
								SCALE_FACTOR_MINUS_1),
						m(RefuStore88kChannelId.PP_VPH_CA, new UnsignedWordElement(SUNSPEC_103 + 9), // 40079
								SCALE_FACTOR_MINUS_1),
						m(RefuStore88kChannelId.PH_VPH_A, new UnsignedWordElement(SUNSPEC_103 + 10), // 40080
								SCALE_FACTOR_MINUS_1),
						m(RefuStore88kChannelId.PH_VPH_B, new UnsignedWordElement(SUNSPEC_103 + 11), // 40081
								SCALE_FACTOR_MINUS_1),
						m(RefuStore88kChannelId.PH_VPH_C, new UnsignedWordElement(SUNSPEC_103 + 12), // 40082
								SCALE_FACTOR_MINUS_1),
						m(RefuStore88kChannelId.V_SF, new UnsignedWordElement(SUNSPEC_103 + 13)), // 40083
						m(SymmetricBatteryInverter.ChannelId.ACTIVE_POWER, new SignedWordElement(SUNSPEC_103 + 14), // 40084
								SCALE_FACTOR_1), // REFUStore88KChannelId.W//
						m(RefuStore88kChannelId.W_SF, new SignedWordElement(SUNSPEC_103 + 15)), // 40085
						m(RefuStore88kChannelId.HZ, new SignedWordElement(SUNSPEC_103 + 16), // 40086
								SCALE_FACTOR_MINUS_2),
						m(RefuStore88kChannelId.HZ_SF, new SignedWordElement(SUNSPEC_103 + 17)), // 40087
						m(RefuStore88kChannelId.VA, new SignedWordElement(SUNSPEC_103 + 18), // 40088
								SCALE_FACTOR_1),
						m(RefuStore88kChannelId.VA_SF, new SignedWordElement(SUNSPEC_103 + 19)), // 40089
						m(SymmetricBatteryInverter.ChannelId.REACTIVE_POWER, new SignedWordElement(SUNSPEC_103 + 20), // 40090
								SCALE_FACTOR_1), // REFUStore88KChannelId.VA_R
						m(RefuStore88kChannelId.VA_R_SF, new SignedWordElement(SUNSPEC_103 + 21)), // 40091
						new DummyRegisterElement(SUNSPEC_103 + 22, SUNSPEC_103 + 23),
						m(RefuStore88kChannelId.WH, new UnsignedDoublewordElement(SUNSPEC_103 + 24), // 40094
								SCALE_FACTOR_2),
						m(RefuStore88kChannelId.WH_SF, new UnsignedWordElement(SUNSPEC_103 + 26)), // 40096
						m(RefuStore88kChannelId.DCA, new SignedWordElement(SUNSPEC_103 + 27), // 40097
								SCALE_FACTOR_MINUS_2),
						m(RefuStore88kChannelId.DCA_SF, new UnsignedWordElement(SUNSPEC_103 + 28)), // 40098
						m(RefuStore88kChannelId.DCV, new UnsignedWordElement(SUNSPEC_103 + 29), // 40099
								SCALE_FACTOR_MINUS_1),
						m(RefuStore88kChannelId.DCV_SF, new UnsignedWordElement(SUNSPEC_103 + 30)), // 40100
						m(RefuStore88kChannelId.DCW, new SignedWordElement(SUNSPEC_103 + 31), // 40101
								SCALE_FACTOR_1),
						m(RefuStore88kChannelId.DCW_SF, new SignedWordElement(SUNSPEC_103 + 32)), // 40102
						m(RefuStore88kChannelId.TMP_CAB, new SignedWordElement(SUNSPEC_103 + 33), // 40103
								SCALE_FACTOR_MINUS_1),
						m(RefuStore88kChannelId.TMP_SNK, new SignedWordElement(SUNSPEC_103 + 34), // 40104
								SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(SUNSPEC_103 + 35, SUNSPEC_103 + 36),
						m(RefuStore88kChannelId.TMP_SF, new UnsignedWordElement(SUNSPEC_103 + 37)), // 40107
						m(RefuStore88kChannelId.ST, new UnsignedWordElement(SUNSPEC_103 + 38)), // 40108
						m(RefuStore88kChannelId.ST_VND, new UnsignedWordElement(SUNSPEC_103 + 39)), // 40109
						m(new BitsWordElement(SUNSPEC_103 + 40, this) //
								.bit(0, RefuStore88kChannelId.OTHER_ALARM) //
								.bit(1, RefuStore88kChannelId.OTHER_WARNING) //
						), //
						m(new BitsWordElement(SUNSPEC_103 + 41, this) //
								.bit(0, RefuStore88kChannelId.GROUND_FAULT) //
								.bit(1, RefuStore88kChannelId.DC_OVER_VOLTAGE) //
								.bit(2, RefuStore88kChannelId.AC_DISCONNECT) //
								.bit(3, RefuStore88kChannelId.DC_DISCONNECT) //
								.bit(4, RefuStore88kChannelId.GRID_DISCONNECT) //
								.bit(5, RefuStore88kChannelId.CABINET_OPEN) //
								.bit(6, RefuStore88kChannelId.MANUAL_SHUTDOWN) //
								.bit(7, RefuStore88kChannelId.OVER_TEMP) //
								.bit(8, RefuStore88kChannelId.OVER_FREQUENCY) //
								.bit(9, RefuStore88kChannelId.UNDER_FREQUENCY) //
								.bit(10, RefuStore88kChannelId.AC_OVER_VOLT) //
								.bit(11, RefuStore88kChannelId.AC_UNDER_VOLT) //
								.bit(12, RefuStore88kChannelId.BLOWN_STRING_FUSE) //
								.bit(13, RefuStore88kChannelId.UNDER_TEMP) //
								.bit(14, RefuStore88kChannelId.MEMORY_LOSS) //
								.bit(15, RefuStore88kChannelId.HW_TEST_FAILURE) //
						), //
						m(RefuStore88kChannelId.EVT_2, new UnsignedDoublewordElement(SUNSPEC_103 + 42)), // 40112
						m(RefuStore88kChannelId.EVT_VND_1, new UnsignedDoublewordElement(SUNSPEC_103 + 44)), // 40114
						m(RefuStore88kChannelId.EVT_VND_2, new UnsignedDoublewordElement(SUNSPEC_103 + 46)), // 40116
						m(RefuStore88kChannelId.EVT_VND_3, new UnsignedDoublewordElement(SUNSPEC_103 + 48)), // 40118
						m(RefuStore88kChannelId.EVT_VND_4, new UnsignedDoublewordElement(SUNSPEC_103 + 50))), // 40120

				new FC3ReadRegistersTask(SUNSPEC_120, Priority.LOW, //
						m(RefuStore88kChannelId.ID_120, new UnsignedWordElement(SUNSPEC_120)), // 40122
						m(RefuStore88kChannelId.L_120, new UnsignedWordElement(SUNSPEC_120 + 1)), // 40123
						m(RefuStore88kChannelId.DER_TYP, new UnsignedWordElement(SUNSPEC_120 + 2)), // 40124
						m(RefuStore88kChannelId.W_RTG, new UnsignedWordElement(SUNSPEC_120 + 3), // 40125
								SCALE_FACTOR_1),
						m(RefuStore88kChannelId.W_RTG_SF, new UnsignedWordElement(SUNSPEC_120 + 4)), // 40126

						m(new UnsignedWordElement(SUNSPEC_120 + 5))
								.m(SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER, SCALE_FACTOR_1)
								.m(RefuStore88kChannelId.VA_RTG, SCALE_FACTOR_1).build(),
						m(RefuStore88kChannelId.VA_RTG_SF, new UnsignedWordElement(SUNSPEC_120 + 6)), // 40128
						m(RefuStore88kChannelId.VAR_RTG_Q1, new SignedWordElement(SUNSPEC_120 + 7), // 40129
								SCALE_FACTOR_1),
						m(RefuStore88kChannelId.VAR_RTG_Q2, new SignedWordElement(SUNSPEC_120 + 8), // 40130
								SCALE_FACTOR_1),
						m(RefuStore88kChannelId.VAR_RTG_Q3, new SignedWordElement(SUNSPEC_120 + 9), // 40131
								SCALE_FACTOR_1),
						m(RefuStore88kChannelId.VAR_RTG_Q4, new SignedWordElement(SUNSPEC_120 + 10), // 40132
								SCALE_FACTOR_1),
						m(RefuStore88kChannelId.VAR_RTG_SF, new SignedWordElement(SUNSPEC_120 + 11)), // 40133
						m(RefuStore88kChannelId.A_RTG, new UnsignedWordElement(SUNSPEC_120 + 12), // 40134
								SCALE_FACTOR_MINUS_2),
						m(RefuStore88kChannelId.A_RTG_SF, new SignedWordElement(SUNSPEC_120 + 13)), // 40135
						m(RefuStore88kChannelId.PF_RTG_Q1, new SignedWordElement(SUNSPEC_120 + 14), // 40136
								SCALE_FACTOR_MINUS_3),
						m(RefuStore88kChannelId.PF_RTG_Q2, new SignedWordElement(SUNSPEC_120 + 15), // 40137
								SCALE_FACTOR_MINUS_3),
						m(RefuStore88kChannelId.PF_RTG_Q3, new SignedWordElement(SUNSPEC_120 + 16), // 40138
								SCALE_FACTOR_MINUS_3),
						m(RefuStore88kChannelId.PF_RTG_Q4, new SignedWordElement(SUNSPEC_120 + 17), // 40139
								SCALE_FACTOR_MINUS_3),
						m(RefuStore88kChannelId.PF_RTG_SF, new SignedWordElement(SUNSPEC_120 + 18)), // 40140
						new DummyRegisterElement(SUNSPEC_120 + 19, SUNSPEC_120 + 26),
						m(RefuStore88kChannelId.PAD_120, new SignedWordElement(SUNSPEC_120 + 27))), // 40149

				new FC3ReadRegistersTask(SUNSPEC_121, Priority.LOW, //
						m(RefuStore88kChannelId.ID_121, new UnsignedWordElement(SUNSPEC_121)), // 40150
						m(RefuStore88kChannelId.L_121, new UnsignedWordElement(SUNSPEC_121 + 1)), // 40151
						new DummyRegisterElement(SUNSPEC_121 + 2, SUNSPEC_121 + 21),
						m(RefuStore88kChannelId.W_MAX_SF, new UnsignedWordElement(SUNSPEC_121 + 22)), // 40172
						m(RefuStore88kChannelId.V_REF_SF, new UnsignedWordElement(SUNSPEC_121 + 23)), // 40173
						m(RefuStore88kChannelId.V_REF_OFS_SF, new UnsignedWordElement(SUNSPEC_121 + 24))), // 40174

				new FC16WriteRegistersTask(SUNSPEC_121 + 2, //
						m(RefuStore88kChannelId.W_MAX, new UnsignedWordElement(SUNSPEC_121 + 2), // 40152
								SCALE_FACTOR_1),
						m(RefuStore88kChannelId.V_REF, new UnsignedWordElement(SUNSPEC_121 + 3), // 40153
								SCALE_FACTOR_1),
						m(RefuStore88kChannelId.V_REF_OFS, new UnsignedWordElement(SUNSPEC_121 + 4), // 40154
								SCALE_FACTOR_1)),

				new FC3ReadRegistersTask(SUNSPEC_123, Priority.LOW, //
						m(RefuStore88kChannelId.ID_123, new UnsignedWordElement(SUNSPEC_123)), // 40182
						m(RefuStore88kChannelId.L_123, new UnsignedWordElement(SUNSPEC_123 + 1)), // 40183
						new DummyRegisterElement(SUNSPEC_123 + 2, SUNSPEC_123 + 22),
						m(RefuStore88kChannelId.W_MAX_LIM_PCT_SF, new UnsignedWordElement(SUNSPEC_123 + 23)), // 40205
						m(RefuStore88kChannelId.OUT_PF_SET_SF, new UnsignedWordElement(SUNSPEC_123 + 24)), // 40206
						m(RefuStore88kChannelId.VAR_PCT_SF, new UnsignedWordElement(SUNSPEC_123 + 25))), // 40207

				new FC16WriteRegistersTask(SUNSPEC_123 + 4, //
						m(RefuStore88kChannelId.CONN, new UnsignedWordElement(SUNSPEC_123 + 4)), // 40186
						m(RefuStore88kChannelId.W_MAX_LIM_PCT, new SignedWordElement(SUNSPEC_123 + 5))), // 40187

				new FC16WriteRegistersTask(SUNSPEC_123 + 9, //
						m(RefuStore88kChannelId.W_MAX_LIM_ENA, new UnsignedWordElement(SUNSPEC_123 + 9)), // 40191
						m(RefuStore88kChannelId.OUT_PF_SET, new SignedWordElement(SUNSPEC_123 + 10), // 40192
								SCALE_FACTOR_MINUS_3)),

				new FC16WriteRegistersTask(SUNSPEC_123 + 14, //
						m(RefuStore88kChannelId.OUT_PF_SET_ENA, new UnsignedWordElement(SUNSPEC_123 + 14)), // 40196
						m(RefuStore88kChannelId.VAR_W_MAX_PCT, new SignedWordElement(SUNSPEC_123 + 15), // 40197
								SCALE_FACTOR_MINUS_1)),

				new FC16WriteRegistersTask(SUNSPEC_123 + 22, //
						m(RefuStore88kChannelId.VAR_PCT_ENA, new UnsignedWordElement(SUNSPEC_123 + 22))), // 40204

				new FC3ReadRegistersTask(SUNSPEC_64040, Priority.LOW, //
						m(RefuStore88kChannelId.ID_64040, new UnsignedWordElement(SUNSPEC_64040)), // 40208
						m(RefuStore88kChannelId.L_64040, new UnsignedWordElement(SUNSPEC_64040 + 1))), // 40209

				new FC16WriteRegistersTask(SUNSPEC_64040 + 2, //
						m(RefuStore88kChannelId.READ_WRITE_PARAM_ID, new UnsignedDoublewordElement(SUNSPEC_64040 + 2)), // 40210
						m(RefuStore88kChannelId.READ_WRITE_PARAM_INDEX,
								new UnsignedDoublewordElement(SUNSPEC_64040 + 4))), // 40212

				new FC3ReadRegistersTask(SUNSPEC_64041, Priority.LOW, //
						m(RefuStore88kChannelId.ID_64041, new UnsignedWordElement(SUNSPEC_64041)), // 40213
						m(RefuStore88kChannelId.L_64041, new UnsignedWordElement(SUNSPEC_64041 + 1))), // 40214

				new FC16WriteRegistersTask(SUNSPEC_64041 + 2, //
						m(RefuStore88kChannelId.READ_WRITE_PARAM_VALUE_U32,
								new UnsignedDoublewordElement(SUNSPEC_64041 + 2)), // 40215
						m(RefuStore88kChannelId.READ_WRITE_PARAM_VALUE_S32,
								new SignedDoublewordElement(SUNSPEC_64041 + 4)), // 40217
						m(RefuStore88kChannelId.READ_WRITE_PARAM_VALUE_F32,
								new SignedDoublewordElement(SUNSPEC_64041 + 6)), // 40219
						m(RefuStore88kChannelId.READ_WRITE_PARAM_VALUE_U16, new UnsignedWordElement(SUNSPEC_64041 + 8)), // 40221
						m(RefuStore88kChannelId.READ_WRITE_PARAM_VALUE_S16, new SignedWordElement(SUNSPEC_64041 + 9)), // 40222
						m(RefuStore88kChannelId.READ_WRITE_PARAM_VALUE_U8, new UnsignedWordElement(SUNSPEC_64041 + 10)), // 40223
						m(RefuStore88kChannelId.READ_WRITE_PARAM_VALUE_S8, new SignedWordElement(SUNSPEC_64041 + 11))), // 40224

				new FC16WriteRegistersTask(SUNSPEC_64800, //
						m(RefuStore88kChannelId.ID_64800, new UnsignedWordElement(SUNSPEC_64800)), // 40225
						m(RefuStore88kChannelId.L_64800, new UnsignedWordElement(SUNSPEC_64800 + 1)), // 40226
						m(RefuStore88kChannelId.LOC_REM_CTL, new SignedWordElement(SUNSPEC_64800 + 2))), // 40227

				new FC3ReadRegistersTask(SUNSPEC_64800 + 3, Priority.LOW, //
						m(RefuStore88kChannelId.PCS_HB, new SignedWordElement(SUNSPEC_64800 + 3)), // 40228
						m(RefuStore88kChannelId.CONTROLLER_HB, new SignedWordElement(SUNSPEC_64800 + 4)), // 40229
						new DummyRegisterElement(SUNSPEC_64800 + 5)),

				new FC16WriteRegistersTask(SUNSPEC_64800 + 6, //
						m(RefuStore88kChannelId.PCS_SET_OPERATION, new SignedWordElement(SUNSPEC_64800 + 6)), // 40231
						m(RefuStore88kChannelId.MAX_BAT_A_CHA, new UnsignedWordElement(SUNSPEC_64800 + 7), // 40232
								SCALE_FACTOR_MINUS_2),
						m(RefuStore88kChannelId.MAX_BAT_A_DISCHA, new UnsignedWordElement(SUNSPEC_64800 + 8), // 40233
								SCALE_FACTOR_MINUS_2),
						m(RefuStore88kChannelId.MAX_A, new UnsignedWordElement(SUNSPEC_64800 + 9)), // 40234
						m(RefuStore88kChannelId.MAX_A_CUR, new UnsignedWordElement(SUNSPEC_64800 + 10)), // 40235
						m(RefuStore88kChannelId.MAX_BAT_A_SF, new SignedWordElement(SUNSPEC_64800 + 11)), // 40236
						m(RefuStore88kChannelId.MAX_A_SF, new SignedWordElement(SUNSPEC_64800 + 12)), // 40237
						m(RefuStore88kChannelId.MAX_A_CUR_SF, new SignedWordElement(SUNSPEC_64800 + 13)), // 40238
						m(RefuStore88kChannelId.PADDING_1, new SignedWordElement(SUNSPEC_64800 + 14)), // 40239
						m(RefuStore88kChannelId.PADDING_2, new SignedWordElement(SUNSPEC_64800 + 15)))); // 40240

	}

	@Override
	public String debugLog() {
		return "P:" + this.getActivePower().asString() //
				+ "|Q:" + this.getReactivePower().asString() //
				+ "|DC:" + this.channel(RefuStore88kChannelId.DCV).value().asString() //
				+ "|" + this.channel(RefuStore88kChannelId.ST).value().asOptionString() //
				+ "|" + this.stateMachine.getCurrentState().asCamelCase();
	}

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	@Override
	public StartStop getStartStopTarget() {
		switch (this.config.startStop()) {
		case AUTO:
			// read StartStop-Channel
			return this.startStopTarget.get();

		case START:
			// force START
			return StartStop.START;

		case STOP:
			// force STOP
			return StartStop.STOP;
		}

		assert false;
		return StartStop.UNDEFINED; // can never happen
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
}
