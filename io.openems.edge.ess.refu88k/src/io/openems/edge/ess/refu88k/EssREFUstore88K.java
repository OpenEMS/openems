package io.openems.edge.ess.refu88k;

import io.openems.common.types.OpenemsType;

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
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.AccessMode;
import io.openems.edge.common.channel.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
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

	private Battery battery;

//	Konstruktor - Initialisierung der Channels
	public EssREFUstore88K() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				ChannelId.values() //
		);
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

	private void handleStateMachine() {

		// by default: block Power
		this.isActivePowerAllowed = false;
		
		// do always
		setBatteryRanges();
		
		IntegerReadChannel operatingStateChannel = this.channel(ChannelId.ST);
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
		IntegerWriteChannel pcsSetOperation = this.channel(ChannelId.PCS_SET_OPERATION);
		try {
			pcsSetOperation.setNextWriteValue(4);
		} catch (OpenemsNamedException e) {
			log.error("problem occurred while trying to start grid mode" + e.getMessage());
		}
	}

	private void enterStandbyMode() {
		IntegerWriteChannel pcsSetOperation = this.channel(ChannelId.PCS_SET_OPERATION);
		try {
			pcsSetOperation.setNextWriteValue(3);
		} catch (OpenemsNamedException e) {
			log.error("problem occurred while trying to start grid mode" + e.getMessage());
		}
	}

	private void doGridConnectedHandling() {

		IntegerWriteChannel pcsSetOperation = this.channel(ChannelId.PCS_SET_OPERATION);
		try {
			pcsSetOperation.setNextWriteValue(1);
		} catch (OpenemsNamedException e) {
			log.error("problem occurred while trying to start grid mode" + e.getMessage());
		}

	}

	private void doFaultHandling() {
		// find out the reason what is wrong an react
		// for a first try, switch system off, it will be restarted
		stopSystem();
	}

	private void stopSystem() {
		IntegerWriteChannel pcsSetOperation = this.channel(ChannelId.PCS_SET_OPERATION);
		try {
			pcsSetOperation.setNextWriteValue(2);
		} catch (OpenemsNamedException e) {
			log.error("problem occurred while trying to stop system" + e.getMessage());
		}
	}

	private void setBatteryRanges() {
		if (battery == null) {
			return;
		}

		// Read some Channels from Battery
		int disMinV = battery.getDischargeMinVoltage().value().orElse(0);
		int chaMaxV = battery.getChargeMaxVoltage().value().orElse(0);
		int disMaxA = battery.getDischargeMaxCurrent().value().orElse(0);
		int chaMaxA = battery.getChargeMaxCurrent().value().orElse(0);
		int batSoC = battery.getSoc().value().orElse(0);
		int batSoH = battery.getSoh().value().orElse(0);
		int batTemp = battery.getMaxCellTemperature().value().orElse(0);

		if (disMinV == 0 || chaMaxV == 0) {
			return;
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricEss.getModbusSlaveNatureTable(accessMode) //
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

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {

		IntegerWriteChannel wMaxChannel = this.channel(ChannelId.W_MAX_LIM_PCT);
		IntegerWriteChannel wMaxLimPctChannel = this.channel(ChannelId.W_MAX_LIM_PCT);
		IntegerWriteChannel wMaxLim_EnaChannel = this.channel(ChannelId.W_MAX_LIM_ENA);
		IntegerWriteChannel varMaxLimPctChannel = this.channel(ChannelId.VAR_W_MAX_PCT);
		IntegerWriteChannel varMaxLim_EnaChannel = this.channel(ChannelId.VAR_PCT_ENA);

		// Vorgabe WMax
		wMaxChannel.setNextWriteValue(MAX_APPARENT_POWER);
		
		// Wirkleistungsvorgabe an den Umrichter als Prozentwert bezogen auf WMAX!!
		int wSetPct = ((100 * activePower) / MAX_APPARENT_POWER);
		wMaxLimPctChannel.setNextWriteValue(wSetPct);
		wMaxLim_EnaChannel.setNextWriteValue(1);
		
		//Blindleistungsvorgabe an den Umrichter als Prozentwert bezogen auf WMAX
		int varSetPct = ((100 * reactivePower) / MAX_APPARENT_POWER);
		varMaxLimPctChannel.setNextWriteValue(varSetPct);
		varMaxLim_EnaChannel.setNextWriteValue(1);

	}

	@Override
	public int getPowerPrecision() {
		// TODO Auto-generated method stub
		return 500;
	}

	/*
	 * ID Zuweisung der Channels
	 */

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/*
		 * Model SUNSPEC_1 (Common)
		 */
		ID_1(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		L_1(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		MN(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		MD(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		OPT(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		VR(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		SN(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		DA(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		PAD_1(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //

		/*
		 * Model SUNSPEC_103 (Inverter Three Phase)
		 */
		ID_103(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		L_103(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		A(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		APH_A(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		APH_B(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		APH_C(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		A_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		PP_VPH_AB(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		PP_VPH_BC(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		PP_VPH_CA(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		PH_VPH_A(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		PH_VPH_B(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		PH_VPH_C(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)), //
		V_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		W(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		W_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		HZ(Doc.of(OpenemsType.INTEGER).unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)), //
		HZ_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		VA(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)), //
		VA_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		VA_R(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)), //
		VA_R_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		WH(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		WH_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		DCA(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		DCA_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		DCV(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		DCV_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		DCW(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		DCW_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		TMP_CAB(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		TMP_SNK(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		TMP_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		ST(Doc.of(OperatingState.values())), //
		ST_VND(Doc.of(VendorOperatingState.values())), //
		EVT_1(Doc.of(Event1.values())), //
		EVT_2(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		EVT_VND_1(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		EVT_VND_2(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		EVT_VND_3(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		EVT_VND_4(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //

		/*
		 * Model SUNSPEC_120 (Inverter Controls Nameplate Ratings)
		 */
		ID_120(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		L_120(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		DER_TYP(Doc.of(DerTyp.values())), //
		W_RTG(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //
		W_RTG_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		VA_RTG(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)), //
		VA_RTG_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		VAR_RTG_Q1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)), //
		VAR_RTG_Q2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)), //
		VAR_RTG_Q3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)), //
		VAR_RTG_Q4(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)), //
		VAR_RTG_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		A_RTG(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)), //
		A_RTG_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		PF_RTG_Q1(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), // // cos()
		PF_RTG_Q2(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), // // cos()
		PF_RTG_Q3(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), // // cos()
		PF_RTG_Q4(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), // // cos()
		PF_RTG_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		PAD_120(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //

		/*
		 * Model SUNSPEC_121 (Inverter Controls Basic Settings)
		 */
		ID_121(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		L_121(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		W_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)), //
		V_REF(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		V_REF_OFS(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
		W_MAX_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		V_REF_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		V_REF_OFS_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //

		/*
		 * Model SUNSPEC_123 (Immediate Inverter Controls)
		 */
		ID_123(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		L_123(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		CONN(Doc.of(Conn.values())), //
		W_MAX_LIM_PCT(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), // % WMax
		W_MAX_LIM_ENA(Doc.of(WMaxLimEna.values())), //
		OUT_PF_SET(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), // // cos()
		OUT_PF_SET_ENA(Doc.of(OutPFSetEna.values())), //
		VAR_W_MAX_PCT(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), // // % WMax
		VAR_PCT_ENA(Doc.of(VArPctEna.values())), //
		W_MAX_LIM_PCT_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		OUT_PF_SET_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		VAR_PCT_SF(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //

		/*
		 * Model SUNSPEC_64040 (Request REFU Parameter ID)
		 */
		ID_64040(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		L_64040(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		READ_WRITE_PARAM_ID(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		READ_WRITE_PARAM_INDEX(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //

		/*
		 * Model SUNSPEC_64041 (Request REFU Parameter ID)
		 */
		ID_64041(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		L_64041(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)), //
		READ_WRITE_PARAM_VALUE_U32(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		READ_WRITE_PARAM_VALUE_S32(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		READ_WRITE_PARAM_VALUE_F32(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		READ_WRITE_PARAM_VALUE_U16(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		READ_WRITE_PARAM_VALUE_S16(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		READ_WRITE_PARAM_VALUE_U8(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		READ_WRITE_PARAM_VALUE_S8(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //

		/*
		 * Sunspec Model No: 64800
		 */
		PCS_SET_OPERATION(Doc.of(OperatingState.values())), //
//		PCSSetOperation(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
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
//						m(EssREFUstore88K.ChannelId.ID_1, new UnsignedWordElement(SUNSPEC_1)),
//						m(EssREFUstore88K.ChannelId.L_1, new UnsignedWordElement(SUNSPEC_1 + 1)),
//						m(EssREFUstore88K.ChannelId.MN, new StringWordElement(SUNSPEC_1 + 2, 16)),
//						m(EssREFUstore88K.ChannelId.MD, new StringWordElement(SUNSPEC_1 + 18, 16)),
//						m(EssREFUstore88K.ChannelId.OPT, new StringWordElement(SUNSPEC_1 + 34, 8)),
//						m(EssREFUstore88K.ChannelId.VR, new StringWordElement(SUNSPEC_1 + 42, 8)),
//						m(EssREFUstore88K.ChannelId.SN, new StringWordElement(SUNSPEC_1 + 50, 16)),
//						m(EssREFUstore88K.ChannelId.DA, new UnsignedWordElement(SUNSPEC_1 + 66))),

				new FC3ReadRegistersTask(SUNSPEC_103, Priority.LOW, //
						m(EssREFUstore88K.ChannelId.ID_103, new UnsignedWordElement(SUNSPEC_103)),
						m(EssREFUstore88K.ChannelId.L_103, new UnsignedWordElement(SUNSPEC_103 + 1)),
						m(EssREFUstore88K.ChannelId.A, new UnsignedWordElement(SUNSPEC_103 + 2),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.APH_A, new UnsignedWordElement(SUNSPEC_103 + 3),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.APH_B, new UnsignedWordElement(SUNSPEC_103 + 4),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.APH_C, new UnsignedWordElement(SUNSPEC_103 + 5),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.A_SF, new UnsignedWordElement(SUNSPEC_103 + 6)),
						m(EssREFUstore88K.ChannelId.PP_VPH_AB, new UnsignedWordElement(SUNSPEC_103 + 7),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.PP_VPH_BC, new UnsignedWordElement(SUNSPEC_103 + 8),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.PP_VPH_CA, new UnsignedWordElement(SUNSPEC_103 + 9),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.PH_VPH_A, new UnsignedWordElement(SUNSPEC_103 + 10),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.PH_VPH_B, new UnsignedWordElement(SUNSPEC_103 + 11),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.PH_VPH_B, new UnsignedWordElement(SUNSPEC_103 + 12),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.V_SF, new UnsignedWordElement(SUNSPEC_103 + 13)),
						m(EssREFUstore88K.ChannelId.W, new SignedWordElement(SUNSPEC_103 + 14),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.W_SF, new SignedWordElement(SUNSPEC_103 + 15)),
						m(EssREFUstore88K.ChannelId.HZ, new SignedWordElement(SUNSPEC_103 + 16),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.HZ_SF, new SignedWordElement(SUNSPEC_103 + 17)),
						m(EssREFUstore88K.ChannelId.VA, new SignedWordElement(SUNSPEC_103 + 18),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.VA_SF, new SignedWordElement(SUNSPEC_103 + 19)),
						m(EssREFUstore88K.ChannelId.VA_R, new SignedWordElement(SUNSPEC_103 + 20),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.VA_R_SF, new SignedWordElement(SUNSPEC_103 + 21)),
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
						m(EssREFUstore88K.ChannelId.TMP_CAB, new SignedWordElement(SUNSPEC_103 + 33),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(EssREFUstore88K.ChannelId.TMP_SNK, new SignedWordElement(SUNSPEC_103 + 34),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(SUNSPEC_103 + 35, SUNSPEC_103 + 36),
						m(EssREFUstore88K.ChannelId.TMP_SF, new UnsignedWordElement(SUNSPEC_103 + 37)),
						m(EssREFUstore88K.ChannelId.ST, new UnsignedWordElement(SUNSPEC_103 + 38)),
						m(EssREFUstore88K.ChannelId.ST_VND, new UnsignedWordElement(SUNSPEC_103 + 39)),
						m(EssREFUstore88K.ChannelId.EVT_1, new UnsignedDoublewordElement(SUNSPEC_103 + 40)),
						m(EssREFUstore88K.ChannelId.EVT_2, new UnsignedDoublewordElement(SUNSPEC_103 + 42)),
						m(EssREFUstore88K.ChannelId.EVT_VND_1, new UnsignedDoublewordElement(SUNSPEC_103 + 44)),
						m(EssREFUstore88K.ChannelId.EVT_VND_2, new UnsignedDoublewordElement(SUNSPEC_103 + 46)),
						m(EssREFUstore88K.ChannelId.EVT_VND_3, new UnsignedDoublewordElement(SUNSPEC_103 + 48)),
						m(EssREFUstore88K.ChannelId.EVT_VND_4, new UnsignedDoublewordElement(SUNSPEC_103 + 50))),

				new FC3ReadRegistersTask(SUNSPEC_120, Priority.LOW, //
						m(EssREFUstore88K.ChannelId.ID_120, new UnsignedWordElement(SUNSPEC_120)),
						m(EssREFUstore88K.ChannelId.L_120, new UnsignedWordElement(SUNSPEC_120 + 1)),
						m(EssREFUstore88K.ChannelId.DER_TYP, new UnsignedWordElement(SUNSPEC_120 + 2)),
						m(EssREFUstore88K.ChannelId.W_RTG, new UnsignedWordElement(SUNSPEC_120 + 3),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.W_RTG_SF, new UnsignedWordElement(SUNSPEC_120 + 4)),
						m(EssREFUstore88K.ChannelId.VA_RTG, new UnsignedWordElement(SUNSPEC_120 + 5),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.VA_RTG_SF, new UnsignedWordElement(SUNSPEC_120 + 6)),
						m(EssREFUstore88K.ChannelId.VAR_RTG_Q1, new SignedWordElement(SUNSPEC_120 + 7),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.VAR_RTG_Q2, new SignedWordElement(SUNSPEC_120 + 8),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.VAR_RTG_Q3, new SignedWordElement(SUNSPEC_120 + 9),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.VAR_RTG_Q4, new SignedWordElement(SUNSPEC_120 + 10),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.VAR_RTG_SF, new SignedWordElement(SUNSPEC_120 + 11)),
						m(EssREFUstore88K.ChannelId.A_RTG, new UnsignedWordElement(SUNSPEC_120 + 12),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						m(EssREFUstore88K.ChannelId.A_RTG_SF, new SignedWordElement(SUNSPEC_120 + 13)),
						m(EssREFUstore88K.ChannelId.PF_RTG_Q1, new SignedWordElement(SUNSPEC_120 + 14),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(EssREFUstore88K.ChannelId.PF_RTG_Q2, new SignedWordElement(SUNSPEC_120 + 15),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(EssREFUstore88K.ChannelId.PF_RTG_Q3, new SignedWordElement(SUNSPEC_120 + 16),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(EssREFUstore88K.ChannelId.PF_RTG_Q4, new SignedWordElement(SUNSPEC_120 + 17),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
						m(EssREFUstore88K.ChannelId.PF_RTG_SF, new SignedWordElement(SUNSPEC_120 + 18))),

				new FC3ReadRegistersTask(SUNSPEC_121, Priority.LOW, //
						m(EssREFUstore88K.ChannelId.ID_121, new UnsignedWordElement(SUNSPEC_121)),
						m(EssREFUstore88K.ChannelId.L_121, new UnsignedWordElement(SUNSPEC_121 + 1))),
				new FC3ReadRegistersTask(SUNSPEC_121 + 22, Priority.LOW, //
						m(EssREFUstore88K.ChannelId.W_MAX_SF, new UnsignedWordElement(SUNSPEC_121 + 22)),
						m(EssREFUstore88K.ChannelId.V_REF_SF, new UnsignedWordElement(SUNSPEC_121 + 23)),
						m(EssREFUstore88K.ChannelId.V_REF_OFS_SF, new UnsignedWordElement(SUNSPEC_121 + 24))),

				new FC16WriteRegistersTask(SUNSPEC_121 + 2, //
						m(EssREFUstore88K.ChannelId.W_MAX, new UnsignedWordElement(SUNSPEC_121 + 2),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.V_REF, new UnsignedWordElement(SUNSPEC_121 + 3),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(EssREFUstore88K.ChannelId.V_REF_OFS, new UnsignedWordElement(SUNSPEC_121 + 4),
								ElementToChannelConverter.SCALE_FACTOR_1)),

				new FC3ReadRegistersTask(SUNSPEC_123, Priority.LOW, //
						m(EssREFUstore88K.ChannelId.ID_123, new UnsignedWordElement(SUNSPEC_123)),
						m(EssREFUstore88K.ChannelId.L_123, new UnsignedWordElement(SUNSPEC_123 + 1))),
				new FC3ReadRegistersTask(SUNSPEC_123 + 23, Priority.LOW, //
						m(EssREFUstore88K.ChannelId.W_MAX_LIM_PCT_SF, new UnsignedWordElement(SUNSPEC_123 + 23)),
						m(EssREFUstore88K.ChannelId.OUT_PF_SET_SF, new UnsignedWordElement(SUNSPEC_123 + 24)),
						m(EssREFUstore88K.ChannelId.VAR_PCT_SF, new UnsignedWordElement(SUNSPEC_123 + 25))),

				new FC16WriteRegistersTask(SUNSPEC_123 + 4, //
						m(EssREFUstore88K.ChannelId.CONN, new UnsignedWordElement(SUNSPEC_123 + 4)),
						m(EssREFUstore88K.ChannelId.W_MAX_LIM_PCT, new SignedWordElement(SUNSPEC_123 + 5),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				new FC16WriteRegistersTask(SUNSPEC_123 + 9, //
						m(EssREFUstore88K.ChannelId.W_MAX_LIM_ENA, new UnsignedWordElement(SUNSPEC_123 + 9)),
						m(EssREFUstore88K.ChannelId.OUT_PF_SET, new SignedWordElement(SUNSPEC_123 + 10),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3)),
				new FC16WriteRegistersTask(SUNSPEC_123 + 14, //
						m(EssREFUstore88K.ChannelId.OUT_PF_SET_ENA, new UnsignedWordElement(SUNSPEC_123 + 14)),
						m(EssREFUstore88K.ChannelId.VAR_W_MAX_PCT, new SignedWordElement(SUNSPEC_123 + 15),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				new FC16WriteRegistersTask(SUNSPEC_123 + 22, //
						m(EssREFUstore88K.ChannelId.VAR_PCT_ENA, new UnsignedWordElement(SUNSPEC_123 + 22))),

				new FC3ReadRegistersTask(SUNSPEC_64040, Priority.LOW, //
						m(EssREFUstore88K.ChannelId.ID_64040, new UnsignedWordElement(SUNSPEC_64040)),
						m(EssREFUstore88K.ChannelId.L_64040, new UnsignedWordElement(SUNSPEC_64040 + 1))),

				new FC16WriteRegistersTask(SUNSPEC_64040 + 2, //
						m(EssREFUstore88K.ChannelId.READ_WRITE_PARAM_ID, new UnsignedDoublewordElement(SUNSPEC_64040 + 2)),
						m(EssREFUstore88K.ChannelId.READ_WRITE_PARAM_INDEX,
								new UnsignedDoublewordElement(SUNSPEC_64040 + 4))),

				new FC3ReadRegistersTask(SUNSPEC_64041, Priority.LOW, //
						m(EssREFUstore88K.ChannelId.ID_64041, new UnsignedWordElement(SUNSPEC_64041)),
						m(EssREFUstore88K.ChannelId.L_64041, new UnsignedWordElement(SUNSPEC_64041 + 1))),

				new FC16WriteRegistersTask(SUNSPEC_64041 + 2, //
						m(EssREFUstore88K.ChannelId.READ_WRITE_PARAM_VALUE_U32,
								new UnsignedDoublewordElement(SUNSPEC_64041 + 2)),
						m(EssREFUstore88K.ChannelId.READ_WRITE_PARAM_VALUE_S32,
								new SignedDoublewordElement(SUNSPEC_64041 + 4)),
						m(EssREFUstore88K.ChannelId.READ_WRITE_PARAM_VALUE_F32,
								new SignedDoublewordElement(SUNSPEC_64041 + 6)),
						m(EssREFUstore88K.ChannelId.READ_WRITE_PARAM_VALUE_U16,
								new UnsignedWordElement(SUNSPEC_64041 + 8)),
						m(EssREFUstore88K.ChannelId.READ_WRITE_PARAM_VALUE_S16, new SignedWordElement(SUNSPEC_64041 + 9)),
						m(EssREFUstore88K.ChannelId.READ_WRITE_PARAM_VALUE_U8,
								new UnsignedWordElement(SUNSPEC_64041 + 10)),
						m(EssREFUstore88K.ChannelId.READ_WRITE_PARAM_VALUE_S8, new SignedWordElement(SUNSPEC_64041 + 11))),

				new FC16WriteRegistersTask(SUNSPEC_64800 + 6, //
						m(EssREFUstore88K.ChannelId.PCS_SET_OPERATION, new SignedWordElement(SUNSPEC_64800 + 6))));
	}

	@Override
	public String debugLog() {
		return "State:" + this.channel(ChannelId.ST).value().asOptionString() //
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
