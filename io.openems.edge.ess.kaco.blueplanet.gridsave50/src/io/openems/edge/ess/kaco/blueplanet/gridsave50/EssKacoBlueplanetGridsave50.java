package io.openems.edge.ess.kaco.blueplanet.gridsave50;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Priority;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.symmetric.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Kaco.BlueplanetGridsave50", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EssKacoBlueplanetGridsave50 extends AbstractOpenemsModbusComponent
		implements ManagedSymmetricEss, Ess, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EssKacoBlueplanetGridsave50.class);

	private final static int UNIT_ID = 1;
	protected final static int MAX_APPARENT_POWER = 50000;

	private final Power power;

	@Reference
	protected ConfigurationAdmin cm;

	public EssKacoBlueplanetGridsave50() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		/*
		 * Initialize Power
		 */
		this.power = new Power(this);
		// Max Apparent
		this.power.setMaxApparentPower(this, MAX_APPARENT_POWER);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum Conn {
		DISCONNECT, CONNECT
	}

	public enum StatePowerUnit {
		DISCONNECT, PRECHARGE_SYSTEM_BOOT, STANDBY, ACTIVE, ERROR
	}

	public enum WSetEna {
		ENABLED, DISABLED
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/*
		 * SUNSPEC_103
		 */
		VENDOR_OPERATING_STATE(new Doc() //
				.option(2, "Battery voltage too low")),
		// see error codes in user manual "10.10 Troubleshooting" (page 48)
		/*
		 * SUNSPEC_64201
		 */
		CONN(new Doc() //
				.option(0, Conn.DISCONNECT) //
				.option(1, Conn.CONNECT)), //
		STATE_POWER_UNIT(new Doc() //
				.option(0, StatePowerUnit.DISCONNECT) //
				.option(16, StatePowerUnit.PRECHARGE_SYSTEM_BOOT) //
				.option(3, StatePowerUnit.STANDBY) //
				.option(11, StatePowerUnit.ACTIVE) //
				.option(15, StatePowerUnit.ERROR) //
		// note: 'Startup' is not handled. It is deprecated with next firmware version
		), //
		W_SET_PCT(new Doc().text("Set power output to specified level. unscaled: -100 to 100")), //
		W_SET_ENA(new Doc().text("WSet_Ena control") //
				.option(0, WSetEna.DISABLED) //
				.option(1, WSetEna.ENABLED) //
		), //
		/*
		 * SUNSPEC_64202
		 */
		V_SF(new Doc().text("scale factor for voltage")), //
		A_SF(new Doc().text("scale factor for ampere")), //
		DIS_MIN_V(new Doc().text("min. discharge voltage")), // TODO scale factor
		DIS_MAX_A(new Doc().text("max. discharge current")), // TODO scale factor
		DIS_CUTOFF_A(new Doc().text("Disconnect if discharge current lower than DisCutoffA")), // TODO scale factor
		CHA_MAX_V(new Doc().text("max. charge voltage")), // TODO scale factor
		CHA_MAX_A(new Doc().text("max. charge current")), // TODO scale factor
		CHA_CUTOFF_A(new Doc().text("Disconnect if charge current lower than ChaCuttoffA")), // TODO scale factor
		EN_LIMIT(new Doc().text("new battery limits are activated when EnLimit is 1")) //
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	private final static int SUNSPEC_103 = 40071 - 1;
	private final static int SUNSPEC_64201 = 40823 - 1;
	private final static int SUNSPEC_64202 = 40855 - 1;

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC16WriteRegistersTask(SUNSPEC_64201 + 4, //
						m(EssKacoBlueplanetGridsave50.ChannelId.CONN, new UnsignedWordElement(SUNSPEC_64201 + 4))),
				new FC16WriteRegistersTask(SUNSPEC_64201 + 10, //
						m(EssKacoBlueplanetGridsave50.ChannelId.W_SET_PCT, new UnsignedWordElement(SUNSPEC_64201 + 10)),
						m(EssKacoBlueplanetGridsave50.ChannelId.W_SET_ENA,
								new UnsignedWordElement(SUNSPEC_64201 + 11))),
				new FC3ReadRegistersTask(SUNSPEC_103 + 39, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.VENDOR_OPERATING_STATE,
								new UnsignedWordElement(SUNSPEC_103 + 39))),
				new FC3ReadRegistersTask(SUNSPEC_64201 + 4, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.CONN, new UnsignedWordElement(SUNSPEC_64201 + 4)),
						m(EssKacoBlueplanetGridsave50.ChannelId.STATE_POWER_UNIT,
								new UnsignedWordElement(SUNSPEC_64201 + 5))), //
				new FC3ReadRegistersTask(SUNSPEC_64202 + 6, Priority.LOW, //
						m(EssKacoBlueplanetGridsave50.ChannelId.V_SF, new UnsignedWordElement(SUNSPEC_64202 + 6)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.A_SF, new UnsignedWordElement(SUNSPEC_64202 + 7)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.DIS_MIN_V, new UnsignedWordElement(SUNSPEC_64202 + 8)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.DIS_MAX_A, new UnsignedWordElement(SUNSPEC_64202 + 9)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.DIS_CUTOFF_A,
								new UnsignedWordElement(SUNSPEC_64202 + 10)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.CHA_MAX_V, new UnsignedWordElement(SUNSPEC_64202 + 11)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.CHA_MAX_A, new UnsignedWordElement(SUNSPEC_64202 + 12)), //
						m(EssKacoBlueplanetGridsave50.ChannelId.CHA_CUTOFF_A,
								new UnsignedWordElement(SUNSPEC_64202 + 13)), //
						new DummyRegisterElement(SUNSPEC_64202 + 14),
						m(EssKacoBlueplanetGridsave50.ChannelId.EN_LIMIT, new UnsignedWordElement(SUNSPEC_64202 + 15))) //
		);
	}

	@Override
	public String debugLog() {
		return "Conn: " + this.channel(ChannelId.CONN).value().asOptionString() + ", State: "
				+ this.channel(ChannelId.STATE_POWER_UNIT).value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		/*
		 * Get channels
		 */
		IntegerWriteChannel disMinV = this.channel(ChannelId.DIS_MIN_V);
		IntegerWriteChannel disMaxA = this.channel(ChannelId.DIS_MAX_A);
		IntegerWriteChannel chaMaxV = this.channel(ChannelId.CHA_MAX_V);
		IntegerWriteChannel chaMaxA = this.channel(ChannelId.CHA_MAX_A);
		IntegerWriteChannel enLimit = this.channel(ChannelId.EN_LIMIT);
		IntegerWriteChannel conn = this.channel(ChannelId.CONN);
		IntegerWriteChannel wSetEna = this.channel(ChannelId.W_SET_ENA);
		IntegerWriteChannel wSetPct = this.channel(ChannelId.W_SET_PCT);
		IntegerReadChannel statePowerUnit = this.channel(ChannelId.STATE_POWER_UNIT);

		/*
		 * Handle state machine
		 */
		Value<Integer> stateValue = statePowerUnit.value();
		if (stateValue.get() == null) {
			return;
		}

		StatePowerUnit state;
		try {
			state = (StatePowerUnit) stateValue.asEnum();
		} catch (InvalidValueException e1) {
			e1.printStackTrace();
			return;
		}

		switch (state) {
		case PRECHARGE_SYSTEM_BOOT: // Transitive state -> Wait...
			break;

		case DISCONNECT: // DSP has no power supply -> start battery
		case STANDBY:
			try {
				conn.setNextWriteValue(1 /* TODO use enum */);
			} catch (OpenemsException e) {
				e.printStackTrace();
			}

		case ACTIVE:
			// TODO replace static value with the one from Sunspec 103 * scale factor
			// TODO round properly
			// the base formula is (activePower * 1000) / 52000
			int activePowerPct = activePower / 52;

			try {
				disMinV.setNextWriteValue(696);
				disMaxA.setNextWriteValue(3);
				chaMaxV.setNextWriteValue(854);
				chaMaxA.setNextWriteValue(3);
				enLimit.setNextWriteValue(1);
				wSetEna.setNextWriteValue(1 /* TODO use enum */);
				wSetPct.setNextWriteValue(activePowerPct);
			} catch (OpenemsException e) {
				e.printStackTrace();
			}
			break;

		case ERROR: {
			/*
			 * Error
			 */
			log.warn("ERROR");
			try {
				// clear error
				conn.setNextWriteValue(0);
			} catch (OpenemsException e) {
				e.printStackTrace();
			}
			break;
		}
		}
	}

	@Override
	public int getPowerPrecision() {
		// TODO Calculate automatically
		return 52;
	}

}
