package io.openems.edge.solaredge.charger;

import java.util.Map;

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

import com.google.common.collect.ImmutableMap;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;

import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;

import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;

import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.solaredge.common.AbstractSunSpecDcCharger;
import io.openems.edge.solaredge.common.AverageCalculator;

import io.openems.edge.solaredge.enums.ActiveInactive;
import io.openems.edge.solaredge.enums.PvMode;
import io.openems.edge.solaredge.hybrid.ess.SolarEdgeHybridEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarEdge.Hybrid.DcCharger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //

)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})

public class SolaredgeDcChargerImpl extends AbstractSunSpecDcCharger implements SolaredgeDcCharger, EssDcCharger,
		ModbusComponent, OpenemsComponent, EventHandler, TimedataProvider, ModbusSlave {

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			EssDcCharger.ChannelId.ACTUAL_ENERGY);

	private AverageCalculator pvDcProductionAverageCalculator = new AverageCalculator(5); // Calculates floating average
																							// over last 5 values

	private final Logger log = LoggerFactory.getLogger(SolaredgeDcChargerImpl.class);
	private Config config;

	public PvMode currentState = PvMode.UNDEFINED; // Default state

	private boolean isLimiting = false;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public SolaredgeDcChargerImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				SolaredgeDcCharger.ChannelId.values() //
		);

		this.addStaticModbusTasks(this.getModbusProtocol());
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SolarEdgeHybridEss ess;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), this.ess.getUnitId(), this.cm,
				"Modbus", this.ess.getModbusBridgeId(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}

		// update filter for 'Ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Ess", config.ess_id())) {
			return;
		}
		this.config = config;
		if (this.ess != null) {
			this.ess.addCharger(this);
		}
		this.setInitalPvLimit();

	}

	// Internal values for Current, Voltage, Power
	private int lastDcCurrentScale = 0;
	private int lastDcVoltageScale = 0;
	private int lastDcPowerScale = 0;

	private int dcCurrent = 0;
	private int dcCurrentScale = 0;
	private double dcCurrentValue = 0;

	private int dcBattCurrent = 0;

	private int dcVoltage = 0;
	private int dcVoltageScale = 0;
	private double dcVoltageValue = 0;

	private int dcPower = 0;
	private int dcPowerScale = 0;
	private double dcPowerValue = 0;

	private int dcDischargePower = 0;
	private int pvDcProduction = 0;

	private String cycleDebugMsg = "";

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(DefaultSunSpecModel.S_120, Priority.LOW) //
			// .put(DefaultSunSpecModel.S_160, Priority.LOW) //
			// .put(DefaultSunSpecModel.S_203, Priority.LOW) //

			// .put(DefaultSunSpecModel.S_802, Priority.LOW) //

			/*
			 * .put(DefaultSunSpecModel.S_203, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_101, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_102, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_103, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_111, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_112, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_113, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_120, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_121, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_122, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_123, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_124, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_125, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_127, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_128, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_145, Priority.LOW) //
			 */
			.build();

	@Override
	protected void onSunSpecInitializationCompleted() {
		// TODO Add mappings for registers from S1 and S103

		// Example:
		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.ACTIVE_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S103.W);

		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.CONSUMPTION_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S103.W);

		// DefaultSunSpecModel.S103.W);
		/*
		 * this.mapFirstPointToChannel(// EssDcCharger.ChannelId.VOLTAGE, //
		 * ElementToChannelConverter.DIRECT_1_TO_1, // DefaultSunSpecModel.S103.DCV);
		 * 
		 * this.mapFirstPointToChannel(// EssDcCharger.ChannelId.CURRENT, //
		 * ElementToChannelConverter.DIRECT_1_TO_1, // DefaultSunSpecModel.S103.DCA);
		 */
		/*
		 * 2023 09 18 this.mapFirstPointToChannel(//
		 * SolaredgeDcCharger.ChannelId.PRODUCTION_POWER, // AC-seitige Produktion
		 * Richtung Verbraucher ElementToChannelConverter.DIRECT_1_TO_1, //
		 * DefaultSunSpecModel.S103.DCW);
		 */
		/*
		 * Grundsätzlich nicht falsch. Es ergeben sich aber Skalierungs-Effekte
		 * this.mapFirstPointToChannel(// EssDcCharger.ChannelId.ACTUAL_POWER, //
		 * DC-seitige Produktion Richtung Verbraucher
		 * ElementToChannelConverter.DIRECT_1_TO_1, // DefaultSunSpecModel.S103.DCW);
		 */

		/*
		 * It is not yet clarified whether this value also includes production from the
		 * battery. As long as the calculatePowerFromEnergy class does not work properly
		 * with Influx, the assignment remains provisional.
		 * 
		 * 2023 11 09: getLatestValue method for influxdb fixed this is used by method
		 * calculateEngeryFromPower
		 */
		this.mapFirstPointToChannel(//
				SolaredgeDcCharger.ChannelId.ORIGINAL_ACTIVE_PRODUCTION_ENERGY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S103.WH); // 103.WH provides lifetime counter for production energy
	}

	/**
	 * Adds static modbus tasks.
	 * 
	 * @param protocol the {@link ModbusProtocol}
	 * @throws OpenemsException on error
	 */
	private void addStaticModbusTasks(ModbusProtocol protocol) throws OpenemsException {

		protocol.addTask(//
				new FC16WriteRegistersTask(61762, m(SolaredgeDcCharger.ChannelId.POWER_CONTROL_ENABLED, // Advanced
																										// Power Control
																										// Enabled? 1:
																										// enabled
																										// 0:disabled
						new SignedDoublewordElement(61762).wordOrder(WordOrder.LSWMSW))));

		protocol.addTask(//
				new FC16WriteRegistersTask(61696, m(SolaredgeDcCharger.ChannelId.COMMIT_PV_POWER_LIMIT, // Commit wanted
																										// power
																										// limitation
						new UnsignedWordElement(61696))));

		protocol.addTask(//
				new FC16WriteRegistersTask(61441, m(SolaredgeDcCharger.ChannelId.SET_PV_POWER_LIMIT_PERCENT, //
						new UnsignedWordElement(61441))));

		protocol.addTask(//
				new FC3ReadRegistersTask(61441, Priority.HIGH,
						m(SolaredgeDcCharger.ChannelId.POWER_PV_LIMIT_PERCENT, new UnsignedWordElement(61441))));

		protocol.addTask(//
				new FC3ReadRegistersTask(61762, Priority.HIGH, //
						m(SolaredgeDcCharger.ChannelId.POWER_CONTROL_ENABLED, // Advanced Power Control Enabled? 1:
																				// enabled 0:disabled
								new SignedDoublewordElement(61762).wordOrder(WordOrder.LSWMSW))));

		protocol.addTask(//
				new FC3ReadRegistersTask(62212, Priority.HIGH, //
						m(SolaredgeDcCharger.ChannelId.MAX_ACTIVE_POWER_PV_LIMIT,
								new FloatDoublewordElement(62212).wordOrder(WordOrder.LSWMSW)), // F304
						m(SolaredgeDcCharger.ChannelId.MAX_REACTIVE_POWER_PV_LIMIT,
								new FloatDoublewordElement(62214).wordOrder(WordOrder.LSWMSW)) // F306
				));

		protocol.addTask(//
				new FC3ReadRegistersTask(62220, Priority.HIGH, //
						m(SolaredgeDcCharger.ChannelId.ACTIVE_POWER_PV_LIMIT,
								new FloatDoublewordElement(62220).wordOrder(WordOrder.LSWMSW)), // F304
						m(SolaredgeDcCharger.ChannelId.REACTIVE_POWER_PV_LIMIT,
								new FloatDoublewordElement(62222).wordOrder(WordOrder.LSWMSW)) // F306
				));

		protocol.addTask(//
				new FC3ReadRegistersTask(0xE170, Priority.HIGH, // battery-side (DC charge / discharge)
						m(SolaredgeDcCharger.ChannelId.DC_VOLTAGE_BATT, // Instantaneous Voltage from Solaregde - no
																		// scaling
								new FloatDoublewordElement(0xE170).wordOrder(WordOrder.LSWMSW)),
						m(SolaredgeDcCharger.ChannelId.CURRENT_BATT_DC, // Instantaneous Current from Solaregde - no
																		// scaling
								new FloatDoublewordElement(0xE172).wordOrder(WordOrder.LSWMSW)),
						m(SolaredgeDcCharger.ChannelId.DC_DISCHARGE_POWER, // Instantaneous Power from Solaregde - no
																			// scaling
								new FloatDoublewordElement(0xE174).wordOrder(WordOrder.LSWMSW)) //

				));

		protocol.addTask(//
				new FC3ReadRegistersTask(0x9CA0, Priority.HIGH, // Inverter-side (PV production DC)

						// Current
						m(SolaredgeDcCharger.ChannelId.CURRENT_DC, //
								new SignedWordElement(0x9CA0)),
						m(SolaredgeDcCharger.ChannelId.CURRENT_DC_SCALE, //
								new SignedWordElement(0x9CA1)),
						// Voltage
						m(SolaredgeDcCharger.ChannelId.DC_VOLTAGE, //
								new SignedWordElement(0x9CA2)),
						m(SolaredgeDcCharger.ChannelId.DC_VOLTAGE_SCALE, //
								new SignedWordElement(0x9CA3)),
						// Power
						m(SolaredgeDcCharger.ChannelId.DC_POWER, //
								new SignedWordElement(0x9CA4)),
						m(SolaredgeDcCharger.ChannelId.DC_POWER_SCALE, //
								new SignedWordElement(0x9CA5))));

		/*
		 * Sunspec 160 only valid for synergy inverters protocol.addTask(// new
		 * FC3ReadRegistersTask(40123, Priority.HIGH, // Inverter-side (PV production
		 * DC)
		 * 
		 * // Scale factors m(SolaredgeDcCharger.ChannelId.STR_CURRENT_SF, // String DC
		 * Current Scale FACTOR new SignedWordElement(40123)),
		 * m(SolaredgeDcCharger.ChannelId.STR_VOLTAGE_SF, // String DC vOLATGE Scale
		 * FACTOR new SignedWordElement(40124)),
		 * m(SolaredgeDcCharger.ChannelId.STR_POWER_SF, // String DC vOLATGE Scale
		 * FACTOR new SignedWordElement(40125)), new DummyRegisterElement(40126, 40139),
		 * // Reserved
		 * 
		 * // m(SolaredgeDcCharger.ChannelId.ST1_DC_CURRENT, // new
		 * SignedWordElement(40140)), m(SolaredgeDcCharger.ChannelId.ST1_DC_VOLTAGE, //
		 * new SignedWordElement(40141)), m(SolaredgeDcCharger.ChannelId.ST1_DC_POWER,
		 * // new SignedWordElement(40142)),
		 * m(SolaredgeDcCharger.ChannelId.ST1_DC_ENERGY, new
		 * UnsignedDoublewordElement(40143)), new DummyRegisterElement(40145, 40159), //
		 * Reserved
		 * 
		 * m(SolaredgeDcCharger.ChannelId.ST2_DC_CURRENT, // new
		 * SignedWordElement(40160)), m(SolaredgeDcCharger.ChannelId.ST2_DC_VOLTAGE, //
		 * new SignedWordElement(40161)), m(SolaredgeDcCharger.ChannelId.ST2_DC_POWER,
		 * // new SignedWordElement(40162)),
		 * m(SolaredgeDcCharger.ChannelId.ST2_DC_ENERGY, new
		 * UnsignedDoublewordElement(40163))
		 * 
		 * ));
		 */
	}

	/**
	 * Calcultes Actual Power out of DC power from PV + DcDischargePower (positive
	 * while Charging).
	 */
	public void _calculateAndSetActualPower() {
		try {
			cycleDebugMsg = ""; // Debug message collected during cycle
			// ### Current
			// Inverter "PV"
			dcCurrent = this.getDcCurrent().getOrError(); // Power Inverter
			dcCurrentScale = this.getDcCurrentScale().getOrError(); //
			dcCurrentValue = dcCurrent * Math.pow(10, dcCurrentScale) * 1000; // Channel expects mA

			// Battery
			dcBattCurrent = this.getDcCurrentBatt().getOrError() * 1000; // Channel expects mA. Negative while charging
			if (lastDcCurrentScale == dcCurrentScale) {
				this._setCurrent((int) dcCurrentValue + (dcBattCurrent * -1));
			}

			// ### Voltage
			dcVoltage = this.getDcVoltage().getOrError();
			dcVoltageScale = this.getDcVoltageScale().getOrError();
			dcVoltageValue = dcVoltage * Math.pow(10, dcVoltageScale) * 1000;
			if (lastDcVoltageScale == dcVoltageScale) {
				this._setVoltage((int) dcVoltageValue);
			}

			// ### Power
			dcPower = this.getDcPower().getOrError(); // Power Inverter
			dcPowerScale = this.getDcPowerScale().getOrError(); // Power DC Scale factor
			dcPowerValue = dcPower * Math.pow(10, dcPowerScale); // Inverter power being converted to AC
			dcDischargePower = this.getDcDischargePower().getOrError(); // battery side. Positive while Charging
			pvDcProduction = (int) dcPowerValue + (int) dcDischargePower;

			if (pvDcProduction <= 0) {
				this.handleState(PvMode.STANDBY);
			} else {
				handleState(PvMode.PRODUCING); //
			}

			if (lastDcPowerScale == dcPowerScale) {

				if (pvDcProduction < 0) {
					pvDcProduction = 0; // Negative Values are not allowed for PV production
				}

				this.pvDcProductionAverageCalculator.addValue(pvDcProduction);

				// to avoid scaling effects only values are valid that do not differ more than
				// 1000W
				if (Math.abs(this.pvDcProductionAverageCalculator.getAverage() - pvDcProduction) < 500) {
					this._setActualPower(pvDcProduction);
					this.calculateActualEnergy.update(pvDcProduction);

				}
			} else { // Actual ScaleFactor is NOT used

				cycleDebugMsg = "|ScaleFactor " + this.getDcPowerScale().asString() + "/"
						+ String.valueOf(lastDcPowerScale);
				handleState(PvMode.WAITING); //
			}

			lastDcCurrentScale = dcCurrentScale;
			lastDcVoltageScale = dcVoltageScale;
			lastDcPowerScale = dcPowerScale;

		} catch (Exception e) {
			return;
		}
	}

	private void handleState(PvMode newState) {

		if (currentState == newState) {
			return;
		}
		// Perform actions based on the state transition
		switch (newState) {
		case WAITING: //

		case PRODUCING:
			if (this.isLimiting) {
				newState = PvMode.LIMIT_ACTIVE;
			}

		case ERROR:
			// ToDo: No error handler yet
			break;
		case LIMIT_ACTIVE:
			// Apply PV power limitation
			// applyPowerLimitation();
			break;
		case STANDBY:

			break;
		case NO_PV:
			// Actions when no PV array is detected
			// handleNoPV();
			break;
		default:
			this.setInitalPvLimit();
			break;
		}
		// Log the state transition
		this.logDebug(log, "Transitioning from " + currentState.getName() + " to " + newState.getName());
		// Update the current state
		currentState = newState;

		try {
			this.setPvMode(newState);
		} catch (OpenemsNamedException e) {
			this.logDebug(this.log, "Unable to set DC Charger pvMode: " + e.getMessage());

		}
	}

	private void setInitalPvLimit() {

		int newLimitPercent = 100;
		// Activate control mode if it's not already active
		if (this.getSetPvPowerControlMode() != ActiveInactive.ACTIVE) {
			try {
				this.setPvPowerControlMode(ActiveInactive.ACTIVE);
			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, "Failed to set Control Mode to ACTIVE: " + e.getMessage());
				this.handleState(PvMode.UNDEFINED);
				return; // Stop further execution if control mode activation fails
			}
		}

		// Attempt to set the new power limit percentage and commit changes
		try {
			this.setPowerPvLimitPercent(newLimitPercent);
			this.commitPvPowerLimit(1); // Send '1' to commit changes
			this.logDebug(this.log, "New Power Limit set to: " + newLimitPercent + "%");

		} catch (OpenemsNamedException e) {
			this.logDebug(this.log, "Failed to set PV Limit in percent: " + e.getMessage());
			this.handleState(PvMode.UNDEFINED);
		}

	}

	/**
	 * Sets Limits for PV-Production. The limitation refers to AC-side
	 * (PV-production + DC-Charging may exeed this value)
	 */
	@Override
	public void _calculateAndSetPvPowerLimit(int maxPvPower) {
		Integer maxActivePowerLimit = this.getMaxActivePowerPvLimit().get(); // max. hardware PV production power
		Integer currentPercent = this.getActivePvPowerLimitPercent().get(); // current PV production limit in percent

		Integer currentPowerLimit = this.getActivePowerPvLimit().get(); // current PV production limit in watts

		this.logDebug(this.log, "Limit Wanted: " + maxPvPower + "W");

		// Validate non-null and non-zero conditions to prevent calculation errors
		if (currentPowerLimit == null || currentPowerLimit == 0 || maxActivePowerLimit == null
				|| maxActivePowerLimit == 0 || currentPercent == null || currentPercent == 0) {
			this.logDebug(this.log, "CurrentPowerLimit or MaxActivePowerLimit is NULL or 0");
			return; // Early exit to prevent further processing
		}

		int pvLimit = maxActivePowerLimit;

		this.logDebug(this.log, "Current PV Limit: " + currentPowerLimit + "W");

		if (maxPvPower < maxActivePowerLimit) {
			pvLimit = maxPvPower;
		}

		// Calculate the new limit as a percentage of the maximum possible hardware
		// limit
		Integer newLimitPercent = (int) ((pvLimit * 100.0) / maxActivePowerLimit);

		// Check that the new limit does not exceed 100% of the hardware capability
		if (newLimitPercent > 100) {
			this.logDebug(this.log, "Configured PV limit exceeds hardware capabilities. Adjusting to 100%.");
			newLimitPercent = 100;
		}

		if (config.readOnlyMode() == true) {
			this.logDebug(this.log, "Limit not applied. Read Only mode is active");
			return;
		}

		if (currentPercent == newLimitPercent && newLimitPercent == 100) {
			this.logDebug(this.log, "New/old Limit " + newLimitPercent + "% - nothing to do");
			this.isLimiting = false;
			return;
		}
		if (this.getSetPvPowerControlMode() != ActiveInactive.ACTIVE) {
			try {
				this.setPvPowerControlMode(ActiveInactive.ACTIVE);
			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, "Failed to set Control Mode to ACTIVE: " + e.getMessage());
				return; // Stop further execution if control mode activation fails
			}
		}

		// Attempt to set the new power limit percentage and commit changes
		try {
			this.setPowerPvLimitPercent(newLimitPercent);
			this.commitPvPowerLimit(1); // Send '1' to commit changes
			this.logDebug(this.log, "New Power Limit set to: " + newLimitPercent + "%");
			this.isLimiting = true;
			this.handleState(PvMode.LIMIT_ACTIVE);
		} catch (OpenemsNamedException e) {
			this.logDebug(this.log, "Failed to set PV Limit in percent: " + e.getMessage());
		}

	}

	@Override
	public String debugLog() {
		if (config.debugMode()) {
			return "DC Power Scale:" + this.getDcPowerScale().asString() //
					+ cycleDebugMsg + "|DC Power:" + (int) this.dcPowerValue + "|DC DisCharge Power:"
					+ (int) this.dcDischargePower + "|DC PvProduction Power:" + this.pvDcProduction + "|Avg Power:"
					+ this.pvDcProductionAverageCalculator.getAverage() + "|Actual Power:"
					+ this.getActualPower().asString() + "|Power Limit:" + this.getActivePowerPvLimit()

			;

		} else {
			return "|Actual Power:" + this.getActualPower().asString();
		}

	}

	@Deactivate
	protected void deactivate() {
		this.ess.removeCharger(this);
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			// this._calculateAndSetActualPower();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this._calculateAndSetActualPower();
			// this._calculateAndSetPvPowerLimit(); called from ESS
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this._calculateAndSetActualPower();

			// this._calculateAndSetPvPowerLimit(); called from ESS
			break;
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				EssDcCharger.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SolaredgeDcCharger.class, accessMode, 100) //
						.build());
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

}
