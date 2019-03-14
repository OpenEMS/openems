package io.openems.edge.ess.refu88k;

//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.Optional;

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
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
//import io.openems.common.exceptions.OpenemsException;
//import io.openems.common.types.OpenemsType;
//import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
//import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
//import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
//import io.openems.edge.common.channel.IntegerReadChannel;
//import io.openems.edge.common.channel.IntegerWriteChannel;
//import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
//import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
//import io.openems.edge.ess.power.api.Constraint;
//import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
//import io.openems.edge.ess.power.api.Pwr;
//import io.openems.edge.ess.power.api.Relationship;

import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Refu.REFUstore88k", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,//
		}

)
public class EssREFUstore88K extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave {

//	private final Logger log = LoggerFactory.getLogger(EssREFUstore88K.class);

	public static final int DEFAULT_UNIT_ID = 1;
	protected static final int MAX_APPARENT_POWER = 50000;

	/*
	 * Is Power allowed? This is set to false on error or if the inverter is not
	 * fully initialized.
	 */
	private boolean isActivePowerAllowed = true;

	@Reference
	private Power power;
	
	@Reference
	protected ConfigurationAdmin cm;

//	Konstruktor - Initialisierung der Channels
	public EssREFUstore88K() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus", config.modbus_id()); //
	}

	@ObjectClassDefinition
	@interface Config {
		String name()

		default "World";

		String id();

		boolean enabled();

		String modbus_id();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

//	private void initializePower() {
//		this.isActivePowerAllowed = true;
//
//		this.channel(ChannelId.W_MAX).onChange(value -> {
//			@SuppressWarnings("unchecked")
//			Optional<Integer> valueOpt = (Optional<Integer>) value.asOptional();
//			if (!valueOpt.isPresent()) {
//				return;
//			}
//			maxApparentPowerUnscaled = TypeUtils.getAsType(OpenemsType.INTEGER, value);
//			refreshPower();
//		});
//		this.channel(ChannelId.W_MAX_SF).onChange(value -> {
//			@SuppressWarnings("unchecked")
//			Optional<Integer> valueOpt = (Optional<Integer>) value.asOptional();
//			if (!valueOpt.isPresent()) {
//				return;
//			}
//			Integer i = TypeUtils.getAsType(OpenemsType.INTEGER, value);
//			maxApparentPowerScaleFactor = (int) Math.pow(10, i);
//			refreshPower();
//		});
//	}
//
//	private void refreshPower() {
//		maxApparentPower = maxApparentPowerUnscaled * maxApparentPowerScaleFactor;
//		if (maxApparentPower > 0) {
//			this.getMaxApparentPower().setNextValue(maxApparentPower);
//		}
//	}

//	@Override
//	public Constraint[] getStaticConstraints() {
//		if (this.isActivePowerAllowed) {
//			return new Constraint[] { 
//				this.createPowerConstraint("Reactive power is not allowed", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0)
//			};
//		} else {
//			return new Constraint[] { 
//					this.createPowerConstraint("KACO inverter not ready", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 0),
//					this.createPowerConstraint("Reactive power is not allowed", Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0)
//			};
//		}
//	}
//	

	private void handleStateMachine() {

		IntegerReadChannel operatingStateChannel = this.channel(ChannelId.St);
		OperatingState operatingState = operatingStateChannel.value().asEnum();
		switch (operatingState) {
		case OFF:
			/*
			 * 1) Inverter is OFF (St = 1), because no power is provided from the DC side.
			 * 2) The EMS has to initiate a precharge of the DC link capacities of the
			 * inverter 3) The EMS closes the DC relais of the battery 4) The inverter’s
			 * control board starts up (firmware booting) and enters the STANDBY state
			 * automatically
			 */
			break;
		case STANDBY:
			/*
			 * The inverter is initialised but not grid connected. The IGBT's are locked and
			 * AC relays are open.
			 */
			doStandbyHandling();
			break;
		case SLEEPING:
			break;
		case STARTING:
			/*
			 * The inverter is connecting to the grid. The inverter switches to STARTED
			 * automatically after few seconds.
			 */
			break;
		case STARTED:
			/*
			 * The inverter is grid connected. AC Relays are closed. The Hardware (IGBT's)
			 * are locked.
			 */

			doGridConnectedHandling();
			break;
		case THROTTLED:
			/*
			 * The inverter feeds and derating is active. The IGBT's are working and AC
			 * relays are closed.
			 */
			break;
		case MPPT:
			/*
			 * The inverter feeds with max possible power. The IGBT's are working and AC
			 * relays are closed.
			 */
			break;
		case SHUTTING_DOWN:
			/*
			 * The inverter is shutting down. The IGBT's are locked and AC relays are open.
			 */
			break;
		case FAULT:
			/*
			 * The inverter is in fault state. The IGBT's are locked and AC relays are open.
			 */

			doFaultHandling();
			break;

		case UNDEFINED:
			// Do nothing because these states are only temporarily reached
			break;
		}
	}

	private void doStandbyHandling() {
		this.isActivePowerAllowed = false;
		exitStandbyMode();
	}

	private void exitStandbyMode() {
		IntegerWriteChannel pcsSetOperation = this.channel(ChannelId.PCSSetOperation);
		try {
			pcsSetOperation.setNextWriteValue(4);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to start grid mode" + e.getMessage());
		}
	}

	private void enterStandbyMode() {
		IntegerWriteChannel pcsSetOperation = this.channel(ChannelId.PCSSetOperation);
		try {
			pcsSetOperation.setNextWriteValue(3);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to start grid mode" + e.getMessage());
		}
	}
	
	private void doGridConnectedHandling() {

		IntegerWriteChannel pcsSetOperation = this.channel(ChannelId.PCSSetOperation);
		try {
			pcsSetOperation.setNextWriteValue(1);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to start grid mode" + e.getMessage());
		}

	}

	private void doFaultHandling() {
		// find out the reason what is wrong an react
		// for a first try, switch system off, it will be restarted
		stopSystem();
	}

	private void stopSystem() {
		IntegerWriteChannel pcsSetOperation = this.channel(ChannelId.PCSSetOperation);
		try {
			pcsSetOperation.setNextWriteValue(2);
		} catch (OpenemsException e) {
			log.error("problem occurred while trying to stop system" + e.getMessage());
		}
	}

//
//	private void setBatteryRanges() {
//		if (battery == null) {
//			return;
//		}
//
//		// Read some Channels from Battery
//		int disMinV = battery.getDischargeMinVoltage().value().orElse(0);
//		int chaMaxV = battery.getChargeMaxVoltage().value().orElse(0);
//		int disMaxA = battery.getDischargeMaxCurrent().value().orElse(0);
//		int chaMaxA = battery.getChargeMaxCurrent().value().orElse(0);
//		int batSoC = battery.getSoc().value().orElse(0);
//		int batSoH = battery.getSoh().value().orElse(0);
//		int batTemp = battery.getMaxCellTemperature().value().orElse(0);
//
//		// Update Power Constraints
//		// TODO: The actual AC allowed charge and discharge should come from the KACO
//		// Blueplanet instead of calculating it from DC parameters.
//		final double EFFICIENCY_FACTOR = 0.9;
//		this.getAllowedCharge().setNextValue(chaMaxA * chaMaxV * -1 * EFFICIENCY_FACTOR);
//		this.getAllowedDischarge().setNextValue(disMaxA * disMinV * EFFICIENCY_FACTOR);
//
//		if (disMinV == 0 || chaMaxV == 0) {
//			return; // according to setup manual 64202.DisMinV and 64202.ChaMaxV must not be zero
//		}
//
//		// Set Battery values to inverter
//		try {
//			this.getDischargeMinVoltageChannel().setNextWriteValue(disMinV);
//			this.getChargeMaxVoltageChannel().setNextWriteValue(chaMaxV);
//			this.getDischargeMaxAmpereChannel().setNextWriteValue(disMaxA);
//			this.getChargeMaxAmpereChannel().setNextWriteValue(chaMaxA);
//			this.getEnLimitChannel().setNextWriteValue(1);
//
//			// battery stats to display on inverter
//			this.getBatterySocChannel().setNextWriteValue(batSoC);
//			this.getBatterySohChannel().setNextWriteValue(batSoH);
//			this.getBatteryTempChannel().setNextWriteValue(batTemp);
//		} catch (OpenemsException e) {
//			log.error("Error during setBatteryRanges, " + e.getMessage());
//		}
//	}	

	@Override
	public ModbusSlaveTable getModbusSlaveTable() {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(), //
				SymmetricEss.getModbusSlaveNatureTable(), //
				ManagedSymmetricEss.getModbusSlaveNatureTable() //
		);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			handleStateMachine();
			break;
		}
	}

//	@Override
//	public void handleEvent(Event event) {
//		if (!this.isEnabled()) {
//			return;
//		}
//		switch (event.getTopic()) {
//		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
//			calculateEnergy();
//			handleStateMachine();
//			break;
//		}
//	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {

		IntegerReadChannel wMaxLimPct_SFChannel = this.channel(ChannelId.WMaxLimPct_SF);
		IntegerWriteChannel wMaxLimPctChannel = this.channel(ChannelId.WMaxLimPct);
		IntegerWriteChannel wMaxLim_EnaChannel = this.channel(ChannelId.WMaxLim_Ena);
		int wSetPct = ((100 * activePower) / MAX_APPARENT_POWER);
		try {
			wMaxLimPctChannel.setNextWriteValue(wSetPct);
			wMaxLim_EnaChannel.setNextWriteValue(1);
		} catch (OpenemsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public int getPowerPrecision() {
		// TODO Auto-generated method stub
		return 500;
	}

//	@Override
//	public int getPowerPrecision() {
//		IntegerReadChannel wSetPct_SFChannel = this.channel(ChannelId.W_SET_PCT_SF);
//		Optional<Integer> wSetPctOpt = wSetPct_SFChannel.value().asOptional();
//		int scalefactor = wSetPctOpt.orElse(0);
//		return (int) (MAX_APPARENT_POWER * 0.01 * Math.pow(10, scalefactor));
//	}

//	// These variables are used to calculate the energy 
//		LocalDateTime lastPowerValuesTimestamp = null;
//		double lastCurrentValue = 0;
//		double lastVoltageValue = 0;
//		double lastActivePowerValue = 0;	
//		double accumulatedChargeEnergy = 0;
//		double accumulatedDischargeEnergy = 0;

//	/*
//	 * This calculates charge/discharge energy using voltage value given from the connected battery and current value from the inverter 
//	 * */
//	private void calculateEnergy() {
//		if (this.lastPowerValuesTimestamp != null) {						
//			
//			long passedTimeInMilliSeconds = Duration.between(this.lastPowerValuesTimestamp, LocalDateTime.now()).toMillis();
//			this.lastPowerValuesTimestamp = LocalDateTime.now();
//			
//			double lastPowerValue = this.lastCurrentValue * this.lastVoltageValue; 
//			double energy = lastPowerValue  * ( ((double) passedTimeInMilliSeconds) / 1000.0) / 3600.0; // calculate energy in watt hours
//			
//			if (this.lastActivePowerValue < 0) {
//				this.accumulatedChargeEnergy = this.accumulatedChargeEnergy + energy;
//				this.getActiveChargeEnergy().setNextValue(accumulatedChargeEnergy);
//			} else if (this.lastActivePowerValue > 0) {
//				this.accumulatedDischargeEnergy = this.accumulatedDischargeEnergy + energy;
//				this.getActiveDischargeEnergy().setNextValue(accumulatedDischargeEnergy);
//			}
//			
//			log.debug("accumulated charge energy :" + accumulatedChargeEnergy);
//			log.debug("accumulated discharge energy :" + accumulatedDischargeEnergy);
//			
//		} else {
//			this.lastPowerValuesTimestamp = LocalDateTime.now();			
//		}
//		
//		this.lastActivePowerValue = this.getActivePower().value().orElse(0);
//				
//		IntegerReadChannel lastCurrentValueChannel = this.channel(ChannelId.DC_CURRENT);
//		this.lastCurrentValue = lastCurrentValueChannel.value().orElse(0) / 1000.0;
//
//		this.lastVoltageValue = this.battery.getVoltage().value().orElse(0);
//	}

//	private void startGridMode() {
//		IntegerWriteChannel requestedState = this.channel(ChannelId.REQUESTED_STATE);
//		try {
//			requestedState.setNextWriteValue(RequestedState.GRID_CONNECTED.value);
//		} catch (OpenemsException e) {
//			log.error("problem occurred while trying to start grid mode" + e.getMessage());
//		}
//	}
//
//	private void startSystem() {
//		IntegerWriteChannel requestedState = this.channel(ChannelId.REQUESTED_STATE);
//		try {
//			requestedState.setNextWriteValue(RequestedState.STANDBY.value);
//		} catch (OpenemsException e) {
//			log.error("problem occurred while trying to start inverter" + e.getMessage());
//		}
//	}
//

//
//	private void setWatchdog() {
//		// according to 3.5.2.2 in the manual write watchdog register
//		IntegerWriteChannel watchdogChannel = this.channel(ChannelId.WATCHDOG);
//		try {
//			watchdogChannel.setNextWriteValue(watchdogInterval);
//		} catch (OpenemsException e) {
//			log.error("Watchdog timer could not be written!" + e.getMessage());
//		}
//	}
//	

//	private void doChannelMapping() {
//		this.battery.getSoc().onChange(value -> {
//			this.getSoc().setNextValue(value.get());
//			this.channel(ChannelId.BAT_SOC).setNextValue(value.get());
//			this.channel(SymmetricEss.ChannelId.SOC).setNextValue(value.get());
//		});
//
//		this.battery.getSoh().onChange(value -> {
//			this.channel(ChannelId.BAT_SOH).setNextValue(value.get());
//		});
//
//		this.battery.getMaxCellTemperature().onChange(value -> {
//			this.channel(ChannelId.BAT_TEMP).setNextValue(value.get());
//		});
//	}

	/*
	 * ID Zuweisung der Channels
	 */

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		// Sunspec Model No: 64800, Offset: 7

		/*
		 * Model SUNSPEC_1 (Common)
		 */
		Id_1(new Doc().unit(Unit.NONE)), //
		L_1(new Doc().unit(Unit.NONE)), //
		Mn(new Doc().unit(Unit.NONE)), //
		Md(new Doc().unit(Unit.NONE)), //
		Opt(new Doc().unit(Unit.NONE)), //
		Vr(new Doc().unit(Unit.NONE)), //
		SN(new Doc().unit(Unit.NONE)), //
		DA(new Doc().unit(Unit.NONE)), //
		Pad_1(new Doc().unit(Unit.NONE)), //

		/*
		 * Model SUNSPEC_103 (Inverter Three Phase)
		 */
		Id_103(new Doc().unit(Unit.NONE)), //
		L_103(new Doc().unit(Unit.NONE)), //
		A(new Doc().unit(Unit.AMPERE)), //
		AphA(new Doc().unit(Unit.AMPERE)), //
		AphB(new Doc().unit(Unit.AMPERE)), //
		AphC(new Doc().unit(Unit.AMPERE)), //
		A_SF(new Doc().unit(Unit.NONE)), //
		PPVphAB(new Doc().unit(Unit.VOLT)), //
		PPVphBC(new Doc().unit(Unit.VOLT)), //
		PPVphCA(new Doc().unit(Unit.VOLT)), //
		PhVphA(new Doc().unit(Unit.VOLT)), //
		PhVphB(new Doc().unit(Unit.VOLT)), //
		PhVphC(new Doc().unit(Unit.VOLT)), //
		V_SF(new Doc().unit(Unit.NONE)), //
		W(new Doc().unit(Unit.WATT)), //
		W_SF(new Doc().unit(Unit.NONE)), //
		Hz(new Doc().unit(Unit.HERTZ)), //
		Hz_SF(new Doc().unit(Unit.NONE)), //
		VA(new Doc().unit(Unit.VOLT_AMPERE)), //
		VA_SF(new Doc().unit(Unit.NONE)), //
		VAr(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		VAr_SF(new Doc().unit(Unit.NONE)), //
		WH(new Doc().unit(Unit.NONE)), //
		WH_SF(new Doc().unit(Unit.NONE)), //
		DCA(new Doc().unit(Unit.NONE)), //
		DCA_SF(new Doc().unit(Unit.NONE)), //
		DCV(new Doc().unit(Unit.NONE)), //
		DCV_SF(new Doc().unit(Unit.NONE)), //
		DCW(new Doc().unit(Unit.NONE)), //
		DCW_SF(new Doc().unit(Unit.NONE)), //
		TmpCab(new Doc().unit(Unit.NONE)), //
		TmpSnk(new Doc().unit(Unit.NONE)), //
		Tmp_SF(new Doc().unit(Unit.NONE)), //
		St(new Doc().options(OperatingState.values())), //
		StVnd(new Doc().options(VendorOperatingState.values())), //
		Evt1(new Doc().options(Event1.values())), //
		Evt2(new Doc().unit(Unit.NONE)), //
		EvtVnd1(new Doc().unit(Unit.NONE)), //
		EvtVnd2(new Doc().unit(Unit.NONE)), //
		EvtVnd3(new Doc().unit(Unit.NONE)), //
		EvtVnd4(new Doc().unit(Unit.NONE)), //

		/*
		 * Model SUNSPEC_120 (Inverter Controls Nameplate Ratings)
		 */
		Id_120(new Doc().unit(Unit.NONE)), //
		L_120(new Doc().unit(Unit.NONE)), //
		DERTyp(new Doc().options(DerTyp.values())), //
		WRtg(new Doc().unit(Unit.WATT)), //
		WRtg_SF(new Doc().unit(Unit.NONE)), //
		VARtg(new Doc().unit(Unit.VOLT_AMPERE)), //
		VARtg_SF(new Doc().unit(Unit.NONE)), //
		VArRtgQ1(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		VArRtgQ2(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		VArRtgQ3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		VArRtgQ4(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		VArRtg_SF(new Doc().unit(Unit.NONE)), //
		ARtg(new Doc().unit(Unit.AMPERE)), //
		ARtg_SF(new Doc().unit(Unit.NONE)), //
		PFRtgQ1(new Doc().unit(Unit.NONE)), // cos()
		PFRtgQ2(new Doc().unit(Unit.NONE)), // cos()
		PFRtgQ3(new Doc().unit(Unit.NONE)), // cos()
		PFRtgQ4(new Doc().unit(Unit.NONE)), // cos()
		PFRtg_SF(new Doc().unit(Unit.NONE)), Pad_120(new Doc().unit(Unit.NONE)), //

		/*
		 * Model SUNSPEC_121 (Inverter Controls Basic Settings)
		 */
		Id_121(new Doc().unit(Unit.NONE)), //
		L_121(new Doc().unit(Unit.NONE)), //
		WMax(new Doc().unit(Unit.WATT)), //
		VRef(new Doc().unit(Unit.VOLT)), //
		VRefOfs(new Doc().unit(Unit.VOLT)), //
		WMax_SF(new Doc().unit(Unit.NONE)), //
		VRef_SF(new Doc().unit(Unit.NONE)), //
		VRefOfs_SF(new Doc().unit(Unit.NONE)), //

		/*
		 * Model SUNSPEC_123 (Immediate Inverter Controls)
		 */
		Id_123(new Doc().unit(Unit.NONE)), //
		L_123(new Doc().unit(Unit.NONE)), //
		CONN(new Doc().options(Conn.values())), //
		WMaxLimPct(new Doc().unit(Unit.NONE)), // % WMax x
		WMaxLim_Ena(new Doc().options(WMaxLimEna.values())), //
		OutPFSet(new Doc().unit(Unit.NONE)), // cos()
		OutPFSet_Ena(new Doc().options(OutPFSetEna.values())), //
		VArWMaxPct(new Doc().unit(Unit.NONE)), // % WMax x
		VArPct_Ena(new Doc().options(VArPctEna.values())), //
		WMaxLimPct_SF(new Doc().unit(Unit.NONE)), //
		OutPFSet_SF(new Doc().unit(Unit.NONE)), //
		VArPct_SF(new Doc().unit(Unit.NONE)), //

		/*
		 * Model SUNSPEC_64040 (Request REFU Parameter ID)
		 */
		Id_64040(new Doc().unit(Unit.NONE)), //
		L_64040(new Doc().unit(Unit.NONE)), //
		ReadWriteParamId(new Doc().unit(Unit.NONE)), //
		ReadWriteParamIndex(new Doc().unit(Unit.NONE)), //

		/*
		 * Model SUNSPEC_64041 (Request REFU Parameter ID)
		 */
		Id_64041(new Doc().unit(Unit.NONE)), //
		L_64041(new Doc().unit(Unit.NONE)), //
		ReadWriteParamValue_U32(new Doc().unit(Unit.NONE)), //
		ReadWriteParamValue_S32(new Doc().unit(Unit.NONE)), //
		ReadWriteParamValue_F32(new Doc().unit(Unit.NONE)), //
		ReadWriteParamValue_U16(new Doc().unit(Unit.NONE)), //
		ReadWriteParamValue_S16(new Doc().unit(Unit.NONE)), //
		ReadWriteParamValue_U8(new Doc().unit(Unit.NONE)), //
		ReadWriteParamValue_S8(new Doc().unit(Unit.NONE)), //

		/*
		 * Model SUNSPEC_64041 (Request REFU Parameter ID)
		 */
		PCSSetOperation(new Doc().unit(Unit.NONE)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/*
	 * Supportet Models First available Model = Start Address + 2 = 40002 Then 40002
	 * + Length of Model ....
	 */
	private final static int START_ADDRESS = 40000;
	private final static int SUNSPEC_1 = START_ADDRESS + 2; // Common
	private final static int SUNSPEC_103 = 40070; // Inverter (Three Phase)
	private final static int SUNSPEC_120 = 40122; // Nameplate
	private final static int SUNSPEC_121 = 40150; // Basic Settings
	private final static int SUNSPEC_123 = 40182; // Immediate Controls
	private final static int SUNSPEC_64040 = 40208; // REFU Parameter
	private final static int SUNSPEC_64041 = 40213; // REFU Parameter Value
	private final static int SUNSPEC_64800 = 40225; // MESA-PCS Extensions

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
//				new FC3ReadRegistersTask(SUNSPEC_1, Priority.LOW, //
//						m(EssREFUstore88K.ChannelId.Id_1, new UnsignedWordElement(SUNSPEC_1)),
//						m(EssREFUstore88K.ChannelId.L_1, new UnsignedWordElement(SUNSPEC_1 + 1)),
//						m(EssREFUstore88K.ChannelId.Mn, new StringWordElement(SUNSPEC_1 + 2, 16)),
//						m(EssREFUstore88K.ChannelId.Md, new StringWordElement(SUNSPEC_1 + 18, 16)),
//						m(EssREFUstore88K.ChannelId.Opt, new StringWordElement(SUNSPEC_1 + 34, 8)),
//						m(EssREFUstore88K.ChannelId.Vr, new StringWordElement(SUNSPEC_1 + 42, 8)),
//						m(EssREFUstore88K.ChannelId.SN, new StringWordElement(SUNSPEC_1 + 50, 16)),
//						m(EssREFUstore88K.ChannelId.DA, new UnsignedWordElement(SUNSPEC_1 + 66))),

				new FC3ReadRegistersTask(SUNSPEC_103, Priority.LOW, //
						m(EssREFUstore88K.ChannelId.Id_103, new UnsignedWordElement(SUNSPEC_103)),
						m(EssREFUstore88K.ChannelId.L_103, new UnsignedWordElement(SUNSPEC_103 + 1)),
						m(EssREFUstore88K.ChannelId.A, new UnsignedWordElement(SUNSPEC_103 + 2),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.AphA, new UnsignedWordElement(SUNSPEC_103 + 3),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.AphB, new UnsignedWordElement(SUNSPEC_103 + 4),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.AphC, new UnsignedWordElement(SUNSPEC_103 + 5),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.A_SF, new UnsignedWordElement(SUNSPEC_103 + 6)),
						m(EssREFUstore88K.ChannelId.PPVphAB, new UnsignedWordElement(SUNSPEC_103 + 7),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.PPVphBC, new UnsignedWordElement(SUNSPEC_103 + 8),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.PPVphCA, new UnsignedWordElement(SUNSPEC_103 + 9),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.PhVphA, new UnsignedWordElement(SUNSPEC_103 + 10),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.PhVphB, new UnsignedWordElement(SUNSPEC_103 + 11),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.PhVphB, new UnsignedWordElement(SUNSPEC_103 + 12),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.V_SF, new UnsignedWordElement(SUNSPEC_103 + 13)),
						m(EssREFUstore88K.ChannelId.W, new SignedWordElement(SUNSPEC_103 + 14),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.W_SF, new SignedWordElement(SUNSPEC_103 + 15)),
						m(EssREFUstore88K.ChannelId.Hz, new SignedWordElement(SUNSPEC_103 + 16),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.Hz_SF, new SignedWordElement(SUNSPEC_103 + 17)),
						m(EssREFUstore88K.ChannelId.VA, new SignedWordElement(SUNSPEC_103 + 18),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.VA_SF, new SignedWordElement(SUNSPEC_103 + 19)),
						m(EssREFUstore88K.ChannelId.VAr, new SignedWordElement(SUNSPEC_103 + 20),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.VAr_SF, new SignedWordElement(SUNSPEC_103 + 21)),
						new DummyRegisterElement(SUNSPEC_103 + 22, SUNSPEC_103 + 23),
						m(EssREFUstore88K.ChannelId.WH, new UnsignedDoublewordElement(SUNSPEC_103 + 24),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(EssREFUstore88K.ChannelId.WH_SF, new UnsignedWordElement(SUNSPEC_103 + 26)),
						m(EssREFUstore88K.ChannelId.DCA, new UnsignedWordElement(SUNSPEC_103 + 27),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.DCA_SF, new UnsignedWordElement(SUNSPEC_103 + 28)),
						m(EssREFUstore88K.ChannelId.DCV, new UnsignedWordElement(SUNSPEC_103 + 29),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.DCV_SF, new UnsignedWordElement(SUNSPEC_103 + 30)),
						m(EssREFUstore88K.ChannelId.DCW, new SignedWordElement(SUNSPEC_103 + 31),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.DCW_SF, new SignedWordElement(SUNSPEC_103 + 32)),
						m(EssREFUstore88K.ChannelId.TmpCab, new SignedWordElement(SUNSPEC_103 + 33),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.TmpSnk, new SignedWordElement(SUNSPEC_103 + 34),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(SUNSPEC_103 + 35, SUNSPEC_103 + 36),
						m(EssREFUstore88K.ChannelId.Tmp_SF, new UnsignedWordElement(SUNSPEC_103 + 37)),
						m(EssREFUstore88K.ChannelId.St, new UnsignedWordElement(SUNSPEC_103 + 38)),
						m(EssREFUstore88K.ChannelId.StVnd, new UnsignedWordElement(SUNSPEC_103 + 39)),
						m(EssREFUstore88K.ChannelId.Evt1, new UnsignedDoublewordElement(SUNSPEC_103 + 40)),
						m(EssREFUstore88K.ChannelId.Evt2, new UnsignedDoublewordElement(SUNSPEC_103 + 42)),
						m(EssREFUstore88K.ChannelId.EvtVnd1, new UnsignedDoublewordElement(SUNSPEC_103 + 44)),
						m(EssREFUstore88K.ChannelId.EvtVnd2, new UnsignedDoublewordElement(SUNSPEC_103 + 46)),
						m(EssREFUstore88K.ChannelId.EvtVnd3, new UnsignedDoublewordElement(SUNSPEC_103 + 48)),
						m(EssREFUstore88K.ChannelId.EvtVnd4, new UnsignedDoublewordElement(SUNSPEC_103 + 50))),
//
//				new FC3ReadRegistersTask(SUNSPEC_120, Priority.LOW, //
//						m(EssREFUstore88K.ChannelId.Id_120, new UnsignedWordElement(SUNSPEC_120)),
//						m(EssREFUstore88K.ChannelId.L_120, new UnsignedWordElement(SUNSPEC_120 + 1)),
//						m(EssREFUstore88K.ChannelId.DERTyp, new UnsignedWordElement(SUNSPEC_120 + 2)),
//						m(EssREFUstore88K.ChannelId.WRtg, new UnsignedWordElement(SUNSPEC_120 + 3),
//								ElementToChannelConverter.SCALE_FACTOR_1),
//						m(EssREFUstore88K.ChannelId.WRtg_SF, new UnsignedWordElement(SUNSPEC_120 + 4)),
//						m(EssREFUstore88K.ChannelId.VARtg, new UnsignedWordElement(SUNSPEC_120 + 5),
//								ElementToChannelConverter.SCALE_FACTOR_1),
//						m(EssREFUstore88K.ChannelId.VARtg_SF, new UnsignedWordElement(SUNSPEC_120 + 6)),
//						m(EssREFUstore88K.ChannelId.VArRtgQ1, new SignedWordElement(SUNSPEC_120 + 7),
//								ElementToChannelConverter.SCALE_FACTOR_1),
//						m(EssREFUstore88K.ChannelId.VArRtgQ2, new SignedWordElement(SUNSPEC_120 + 8),
//								ElementToChannelConverter.SCALE_FACTOR_1),
//						m(EssREFUstore88K.ChannelId.VArRtgQ3, new SignedWordElement(SUNSPEC_120 + 9),
//								ElementToChannelConverter.SCALE_FACTOR_1),
//						m(EssREFUstore88K.ChannelId.VArRtgQ4, new SignedWordElement(SUNSPEC_120 + 10),
//								ElementToChannelConverter.SCALE_FACTOR_1),
//						m(EssREFUstore88K.ChannelId.VArRtg_SF, new SignedWordElement(SUNSPEC_120 + 11)),
//						m(EssREFUstore88K.ChannelId.ARtg, new UnsignedWordElement(SUNSPEC_120 + 12),
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
//						m(EssREFUstore88K.ChannelId.ARtg_SF, new SignedWordElement(SUNSPEC_120 + 13)),
//						m(EssREFUstore88K.ChannelId.PFRtgQ1, new SignedWordElement(SUNSPEC_120 + 14),
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
//						m(EssREFUstore88K.ChannelId.PFRtgQ2, new SignedWordElement(SUNSPEC_120 + 15),
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
//						m(EssREFUstore88K.ChannelId.PFRtgQ3, new SignedWordElement(SUNSPEC_120 + 16),
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
//						m(EssREFUstore88K.ChannelId.PFRtgQ4, new SignedWordElement(SUNSPEC_120 + 17),
//								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
//						m(EssREFUstore88K.ChannelId.PFRtg_SF, new SignedWordElement(SUNSPEC_120 + 18))),
//
//				new FC3ReadRegistersTask(SUNSPEC_121, Priority.LOW, //
//						m(EssREFUstore88K.ChannelId.Id_121, new UnsignedWordElement(SUNSPEC_121)),
//						m(EssREFUstore88K.ChannelId.L_121, new UnsignedWordElement(SUNSPEC_121 + 1)),
//						m(EssREFUstore88K.ChannelId.WMax_SF, new UnsignedWordElement(SUNSPEC_121 + 22)),
//						m(EssREFUstore88K.ChannelId.VRef_SF, new UnsignedWordElement(SUNSPEC_121 + 23)),
//						m(EssREFUstore88K.ChannelId.VRefOfs_SF, new UnsignedWordElement(SUNSPEC_121 + 24))),
//
//				new FC16WriteRegistersTask(SUNSPEC_121, //
//						m(EssREFUstore88K.ChannelId.WMax, new UnsignedWordElement(SUNSPEC_121 + 2),
//								ElementToChannelConverter.SCALE_FACTOR_1),
//						m(EssREFUstore88K.ChannelId.VRef, new UnsignedWordElement(SUNSPEC_121 + 3),
//								ElementToChannelConverter.SCALE_FACTOR_1),
//						m(EssREFUstore88K.ChannelId.VRefOfs, new UnsignedWordElement(SUNSPEC_121 + 4),
//								ElementToChannelConverter.SCALE_FACTOR_1)),
//
				new FC3ReadRegistersTask(SUNSPEC_123, Priority.LOW, //
						m(EssREFUstore88K.ChannelId.Id_123, new UnsignedWordElement(SUNSPEC_123)),
						m(EssREFUstore88K.ChannelId.L_123, new UnsignedWordElement(SUNSPEC_123 + 1)),
						new DummyRegisterElement(SUNSPEC_123 + 2, SUNSPEC_123 + 22),
						m(EssREFUstore88K.ChannelId.WMaxLimPct_SF, new UnsignedWordElement(SUNSPEC_123 + 23)),
						m(EssREFUstore88K.ChannelId.OutPFSet_SF, new UnsignedWordElement(SUNSPEC_123 + 24)),
						m(EssREFUstore88K.ChannelId.VArPct_SF, new UnsignedWordElement(SUNSPEC_123 + 25))),

				new FC16WriteRegistersTask(SUNSPEC_123 + 4, //
						m(EssREFUstore88K.ChannelId.CONN, new UnsignedWordElement(SUNSPEC_123 + 4)),
						m(EssREFUstore88K.ChannelId.WMaxLimPct, new SignedWordElement(SUNSPEC_123 + 5),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				new FC16WriteRegistersTask(SUNSPEC_123 + 9, //
						m(EssREFUstore88K.ChannelId.WMaxLim_Ena, new UnsignedWordElement(SUNSPEC_123 + 9)),
						m(EssREFUstore88K.ChannelId.OutPFSet, new SignedWordElement(SUNSPEC_123 + 10),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3)),
				new FC16WriteRegistersTask(SUNSPEC_123 + 14, //
						m(EssREFUstore88K.ChannelId.OutPFSet_Ena, new UnsignedWordElement(SUNSPEC_123 + 14)),
						m(EssREFUstore88K.ChannelId.VArWMaxPct, new SignedWordElement(SUNSPEC_123 + 15),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				new FC16WriteRegistersTask(SUNSPEC_123 + 22, //
						m(EssREFUstore88K.ChannelId.VArPct_Ena, new UnsignedWordElement(SUNSPEC_123 + 22))),
//
//				new FC3ReadRegistersTask(SUNSPEC_64040, Priority.LOW, //
//						m(EssREFUstore88K.ChannelId.Id_64040, new UnsignedWordElement(SUNSPEC_64040)),
//						m(EssREFUstore88K.ChannelId.L_64040, new UnsignedWordElement(SUNSPEC_64040 + 1))
//
//				),
//
//				new FC16WriteRegistersTask(SUNSPEC_64040, //
//						m(EssREFUstore88K.ChannelId.ReadWriteParamId, new UnsignedDoublewordElement(SUNSPEC_64040 + 2)),
//						m(EssREFUstore88K.ChannelId.ReadWriteParamIndex,
//								new UnsignedDoublewordElement(SUNSPEC_64040 + 4))),
//
//				new FC3ReadRegistersTask(SUNSPEC_64041, Priority.LOW, //
//						m(EssREFUstore88K.ChannelId.Id_64041, new UnsignedWordElement(SUNSPEC_64041)),
//						m(EssREFUstore88K.ChannelId.L_64041, new UnsignedWordElement(SUNSPEC_64041 + 1))
//
//				),
//
//				new FC16WriteRegistersTask(SUNSPEC_64041, //
//						m(EssREFUstore88K.ChannelId.ReadWriteParamValue_U32,
//								new UnsignedDoublewordElement(SUNSPEC_64041 + 2)),
//						m(EssREFUstore88K.ChannelId.ReadWriteParamValue_S32,
//								new SignedDoublewordElement(SUNSPEC_64041 + 4)),
//						m(EssREFUstore88K.ChannelId.ReadWriteParamValue_F32,
//								new SignedDoublewordElement(SUNSPEC_64041 + 6)),
//						m(EssREFUstore88K.ChannelId.ReadWriteParamValue_U16,
//								new UnsignedWordElement(SUNSPEC_64041 + 8)),
//						m(EssREFUstore88K.ChannelId.ReadWriteParamValue_S16, new SignedWordElement(SUNSPEC_64041 + 9)),
//						m(EssREFUstore88K.ChannelId.ReadWriteParamValue_U8,
//								new UnsignedWordElement(SUNSPEC_64041 + 10)),
//						m(EssREFUstore88K.ChannelId.ReadWriteParamValue_S8, new SignedWordElement(SUNSPEC_64041 + 11))),

				new FC16WriteRegistersTask(SUNSPEC_64800, //
						m(EssREFUstore88K.ChannelId.PCSSetOperation, new SignedWordElement(SUNSPEC_64800 + 6))));
	}

	@Override
	public String debugLog() {
		return "State:" + this.channel(ChannelId.St).value().asOptionString() //
				+ ",L:" + this.channel(SymmetricEss.ChannelId.ACTIVE_POWER).value().asString(); //
	}

//	private IntegerWriteChannel getDischargeMinVoltageChannel() {
//		return this.channel(ChannelId.DIS_MIN_V);
//	}
//
//	private IntegerWriteChannel getDischargeMaxAmpereChannel() {
//		return this.channel(ChannelId.DIS_MAX_A);
//	}
//
//	private IntegerWriteChannel getChargeMaxVoltageChannel() {
//		return this.channel(ChannelId.CHA_MAX_V);
//	}
//
//	private IntegerWriteChannel getChargeMaxAmpereChannel() {
//		return this.channel(ChannelId.CHA_MAX_A);
//	}
//
//	private IntegerWriteChannel getEnLimitChannel() {
//		return this.channel(ChannelId.EN_LIMIT);
//	}
//
//	private IntegerWriteChannel getBatterySocChannel() {
//		return this.channel(ChannelId.BAT_SOC);
//	}
//
//	private IntegerWriteChannel getBatterySohChannel() {
//		return this.channel(ChannelId.BAT_SOH);
//	}
//
//	private IntegerWriteChannel getBatteryTempChannel() {
//		return this.channel(ChannelId.BAT_TEMP);
//	}

}
