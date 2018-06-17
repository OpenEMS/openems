package io.openems.edge.ess.kaco.blueplanet50;

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

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Priority;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.power.symmetric.SymmetricPower;
import io.openems.edge.ess.symmetric.api.SymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Ess.Kaco.Blueplanet50", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EssKacoBlueplanet50 extends AbstractOpenemsModbusComponent implements SymmetricEss, Ess, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EssKacoBlueplanet50.class);

	private final static int UNIT_ID = 1;
	protected final static int MAX_APPARENT_POWER = 50000;

	private final SymmetricPower power;
	private String modbusBridgeId;

	@Reference
	protected ConfigurationAdmin cm;

	public EssKacoBlueplanet50() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		/*
		 * Initialize Power
		 */
		this.power = new SymmetricPower(this, EssKacoBlueplanet50.MAX_APPARENT_POWER, 1 /* TODO: POWER_PRECISION */, //
				(activePower, reactivePower) -> {
					/*
					 * Handle state
					 */
					Channel<Integer> stateChannel = this.channel(ChannelId.STATE_POWER_UNIT);
					int state = stateChannel.value().orElse(0);
					// StatePowerUnit state = (StatePowerUnit) stateChannel.value().asEnum();
					// if( state == StatePowerUnit.STANDBY)
					switch (state) {
					case 3:
						/*
						 * Standby
						 */

						// set battery limits and start inverter
						this.channel(ChannelId.DIS_MIN_V).setNextValue(0 /* TODO */);
						this.channel(ChannelId.DIS_MAX_A).setNextValue(0 /* TODO */);
						this.channel(ChannelId.CHA_MAX_V).setNextValue(0 /* TODO */);
						this.channel(ChannelId.CHA_MAX_A).setNextValue(0 /* TODO */);
						this.channel(ChannelId.W_SET_ENA).setNextValue(1 /* TODO use enum */);
						this.channel(ChannelId.CONN).setNextValue(1 /* TODO use enum */);
						break;

					case 11:
						/*
						 * Active
						 */

						// set power

						this.channel(ChannelId.W_SET_PCT)
								.setNextValue(0 /* TODO set power percentage with scalefactor from WSetPct_SF or 2 */);
						this.channel(ChannelId.W_SET_ENA).setNextValue(1);

					case 15:
						/*
						 * Error
						 */

						// TODO clear error
					}

					/*
					 * Apply Active/Reactive power
					 */
					// TODO
					// IntegerWriteChannel setActivePowerChannel =
					// this.channel(ChannelId.SET_ACTIVE_POWER);
					// IntegerWriteChannel setReactivePowerChannel =
					// this.channel(ChannelId.SET_REACTIVE_POWER);
					// try {
					// setActivePowerChannel.setNextWriteValue(activePower);
					// } catch (OpenemsException e) {
					// log.error("Unable to set ActivePower: " + e.getMessage());
					// }
					// try {
					// setReactivePowerChannel.setNextWriteValue(reactivePower);
					// } catch (OpenemsException e) {
					// log.error("Unable to set ReactivePower: " + e.getMessage());
					// }
				});
		// // Allowed Apparent
		// this.power.addStaticLimitation( //
		// this.allowedApparentLimit = new SMaxLimitation(this.power).setSMax(0, 0, 0)
		// //
		// );
		// this.channel(ChannelId.ALLOWED_APPARENT).onUpdate(value -> {
		// this.allowedApparentLimit.setSMax(TypeUtils.getAsType(OpenemsType.INTEGER,
		// value), 0, 0);
		// });
		// Allowed Charge
		// this.power.addStaticLimitation( //
		// this.allowedChargeLimit = new PGreaterEqualLimitation(this.power).setP(0) //
		// );
		// this.channel(ChannelId.ALLOWED_CHARGE).onUpdate(value -> {
		// this.allowedChargeLimit.setP(TypeUtils.getAsType(OpenemsType.INTEGER,
		// value));
		// });
		// // Allowed Discharge
		// this.power.addStaticLimitation( //
		// this.allowedDischargeLimit = new PSmallerEqualLimitation(this.power).setP(0)
		// //
		// );
		// this.channel(ChannelId.ALLOWED_DISCHARGE).onUpdate(value -> {
		// this.allowedDischargeLimit.setP(TypeUtils.getAsType(OpenemsType.INTEGER,
		// value));
		// });
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
		this.modbusBridgeId = config.modbus_id();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum Conn {
		DISCONNECT, CONNECT
	}

	public enum StatePowerUnit {
		OFF, SYSTEM_BOOT, STANDBY, STARTUP, ACTIVE, ERROR
	}

	public enum WSetEna {
		ENABLED, DISABLED
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/*
		 * SUNSPEC_64201
		 */
		CONN(new Doc() //
				.option(0, Conn.DISCONNECT) //
				.option(1, Conn.CONNECT)), //
		STATE_POWER_UNIT(new Doc() //
				.option(0, StatePowerUnit.OFF) //
				.option(16, StatePowerUnit.SYSTEM_BOOT) //
				.option(3, StatePowerUnit.STANDBY) //
				.option(11, StatePowerUnit.ACTIVE) //
				.option(15, StatePowerUnit.ERROR) //
		// TODO: 20 to 39, 7, 40 to 49: Startup
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

	private final static int SUNSPEC_64201 = 40823;
	private final static int SUNSPEC_64202 = 40855;
	private final static int SUNSPEC_64204 = 40871;

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				new FC16WriteRegistersTask(SUNSPEC_64201, //
						m(EssKacoBlueplanet50.ChannelId.CONN, new UnsignedWordElement(SUNSPEC_64201 + 4))),
				new FC16WriteRegistersTask(SUNSPEC_64201, //
						m(EssKacoBlueplanet50.ChannelId.W_SET_PCT, new UnsignedWordElement(SUNSPEC_64201 + 10)),
						m(EssKacoBlueplanet50.ChannelId.W_SET_ENA, new UnsignedWordElement(SUNSPEC_64201 + 11))),
				new FC3ReadRegistersTask(SUNSPEC_64201, Priority.LOW, //
						m(EssKacoBlueplanet50.ChannelId.STATE_POWER_UNIT, new UnsignedWordElement(SUNSPEC_64201 + 5))), //
				new FC3ReadRegistersTask(SUNSPEC_64202, Priority.LOW, //
						m(EssKacoBlueplanet50.ChannelId.V_SF, new UnsignedWordElement(SUNSPEC_64202 + 6)), //
						m(EssKacoBlueplanet50.ChannelId.A_SF, new UnsignedWordElement(SUNSPEC_64202 + 7)), //
						m(EssKacoBlueplanet50.ChannelId.DIS_MIN_V, new UnsignedWordElement(SUNSPEC_64202 + 8)), //
						m(EssKacoBlueplanet50.ChannelId.DIS_MAX_A, new UnsignedWordElement(SUNSPEC_64202 + 9)), //
						m(EssKacoBlueplanet50.ChannelId.DIS_CUTOFF_A, new UnsignedWordElement(SUNSPEC_64202 + 10)), //
						m(EssKacoBlueplanet50.ChannelId.CHA_MAX_V, new UnsignedWordElement(SUNSPEC_64202 + 11)), //
						m(EssKacoBlueplanet50.ChannelId.CHA_MAX_A, new UnsignedWordElement(SUNSPEC_64202 + 12)), //
						m(EssKacoBlueplanet50.ChannelId.CHA_CUTOFF_A, new UnsignedWordElement(SUNSPEC_64202 + 13)), //
						new DummyRegisterElement(SUNSPEC_64202 + 14),
						m(EssKacoBlueplanet50.ChannelId.EN_LIMIT, new UnsignedWordElement(SUNSPEC_64202 + 15))) //
		);
	}

	@Override
	public String debugLog() {
		return "Conn: " + this.channel(ChannelId.CONN).value().asOptionString() + ", State: "
				+ this.channel(ChannelId.STATE_POWER_UNIT).value().asOptionString();

		// return "SoC:" + this.getSoc().value().asString() //
		// + "|L:" + this.getActivePower().value().asString() //
		// + "|Allowed:" +
		// this.channel(ChannelId.ALLOWED_CHARGE).value().asStringWithoutUnit() + ";"
		// + this.channel(ChannelId.ALLOWED_DISCHARGE).value().asString() //
		// + "|" + this.getGridMode().value().asOptionString();
	}

	@Override
	public SymmetricPower getPower() {
		return this.power;
	}
}
