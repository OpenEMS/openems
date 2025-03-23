package io.openems.edge.kostal.ess;

import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;

import java.time.Duration;
import java.time.Instant;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(
		//
		name = "Ess.Kostal.Plenticore", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class KostalManagedESSImpl extends AbstractOpenemsModbusComponent
		implements
			KostalManagedESS,
			ManagedSymmetricEss,
			SymmetricEss,
			// HybridEss,
			// EssDcCharger,
			ModbusComponent,
			TimedataProvider,
			EventHandler,
			OpenemsComponent {

	private static final int WATCHDOG_SECONDS = 30;

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	private Instant lastApplyPower = Instant.MIN;
	private int lastSetActivePower = 0;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ElectricityMeter inverter;

	private Timedata timeData;

	public KostalManagedESSImpl() {
		super(
				//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				// EssDcCharger.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				KostalManagedESS.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config)
			throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(),
				config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}

		this._setGridMode(GridMode.ON_GRID);
		this._setCapacity(config.capacity());

		try {
			// reference PV inverter
			if (OpenemsComponent.updateReferenceFilter(this.cm,
					this.servicePid(), "inverter", config.inverter_id())) {
				return;
			}

		} catch (Exception e) {
			// Ignore exception for failed reference
		}

		try {
			// initialize
			this.applyPower(0, 0);

		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		inverter = null;
	}

	@Override
	public void applyPower(int activePower, int reactivePower)
			throws OpenemsNamedException {
		// TODO remove syso
		System.out.println("set active power: " + activePower);
		System.out.println("set reactive power: " + reactivePower);
		if (this.config.readOnlyMode()) {
			// TODO remove syso
			System.out.println("is readonly!");
			// this._setActivePower(0);
			return;
		}

		Instant now = Instant.now();
		if (this.lastSetActivePower == activePower
				&& Duration.between(this.lastApplyPower, now)
						.getSeconds() < WATCHDOG_SECONDS) {
			// no need to apply to new set-point
			// TODO remove syso
			System.out.println("skipped");
			return;
		}

		// TODO remove syso
		System.out.println("-- applying power values --");
		IntegerWriteChannel setActivePowerChannel = this
				.channel(KostalManagedESS.ChannelId.SET_ACTIVE_POWER);

		setActivePowerChannel.setNextWriteValue(activePower);

		// TODO clarify reactive setter - Kostal does not support?

		this.lastApplyPower = Instant.now();
		this.lastSetActivePower = activePower;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(14, Priority.LOW, //
						m(KostalManagedESS.ChannelId.SERIAL_NUMBER,
								new StringWordElement(14, 8))), //
				new FC3ReadRegistersTask(56, Priority.LOW, //
						m(KostalManagedESS.ChannelId.INVERTER_STATE,
								new UnsignedDoublewordElement(56)
										.wordOrder(LSWMSW))), //
				new FC3ReadRegistersTask(104, Priority.LOW, //
						m(KostalManagedESS.ChannelId.ENERGY_MANAGER_MODE,
								new UnsignedDoublewordElement(104)
										.wordOrder(LSWMSW))), //

				// new FC3ReadRegistersTask(174, Priority.HIGH, //
				// m(SymmetricEss.ChannelId.REACTIVE_POWER,
				// new FloatDoublewordElement(174)
				// .wordOrder(LSWMSW))), //

				// new FC3ReadRegistersTask(152, Priority.HIGH, //
				// m(KostalManagedESS.ChannelId.FREQUENCY,
				// new FloatDoublewordElement(152)
				// .wordOrder(LSWMSW))), //
				// new FC3ReadRegistersTask(158, Priority.HIGH, //
				// m(KostalManagedESS.ChannelId.GRID_VOLTAGE_L1,
				// new FloatDoublewordElement(158)
				// .wordOrder(LSWMSW))), //
				// new FC3ReadRegistersTask(164, Priority.HIGH, //
				// m(KostalManagedESS.ChannelId.GRID_VOLTAGE_L2,
				// new FloatDoublewordElement(164)
				// .wordOrder(LSWMSW))), //
				// new FC3ReadRegistersTask(170, Priority.HIGH, //
				// m(KostalManagedESS.ChannelId.GRID_VOLTAGE_L3,
				// new FloatDoublewordElement(170)
				// .wordOrder(LSWMSW))), //

				new FC3ReadRegistersTask(190, Priority.HIGH, //
						m(KostalManagedESS.ChannelId.BATTERY_CURRENT,
								new FloatDoublewordElement(190)
										.wordOrder(LSWMSW))),
				new FC3ReadRegistersTask(210, Priority.HIGH, //
						m(SymmetricEss.ChannelId.SOC,
								new FloatDoublewordElement(210)
										.wordOrder(LSWMSW))), //
				new FC3ReadRegistersTask(214, Priority.HIGH, //
						m(KostalManagedESS.ChannelId.BATTERY_TEMPERATURE,
								new FloatDoublewordElement(214)
										.wordOrder(LSWMSW)), //
						m(KostalManagedESS.ChannelId.BATTERY_VOLTAGE,
								new FloatDoublewordElement(216)
										.wordOrder(LSWMSW))), //

				// new FC3ReadRegistersTask(529, Priority.LOW, //
				// m(SymmetricEss.ChannelId.CAPACITY,
				// new UnsignedDoublewordElement(529)
				// .wordOrder(LSWMSW))), //

				new FC3ReadRegistersTask(531, Priority.LOW, //
						m(SymmetricEss.ChannelId.MAX_APPARENT_POWER,
								new UnsignedWordElement(531))), //
				// new FloatDoublewordElement(1040).wordOrder(LSWMSW))), //

				// new FC3ReadRegistersTask(582, Priority.HIGH, //
				// m(HybridEss.ChannelId.DC_DISCHARGE_POWER,
				// new SignedWordElement(582))), //

				new FC3ReadRegistersTask(582, Priority.HIGH, //
						m(SymmetricEss.ChannelId.ACTIVE_POWER,
								new SignedWordElement(582))),

//				new FC3ReadRegistersTask(1038, Priority.HIGH, //
//						m(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER,
//								new FloatDoublewordElement(1038)
//										.wordOrder(LSWMSW)), //
//						m(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER,
//								new FloatDoublewordElement(1040)
//										.wordOrder(LSWMSW))), //

				new FC3ReadRegistersTask(1046, Priority.LOW,
						m(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY,
								new FloatDoublewordElement(1046)
										.wordOrder(LSWMSW)), //
						m(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY,
								new FloatDoublewordElement(1048)
										.wordOrder(LSWMSW))), //

				new FC16WriteRegistersTask(1034, //
						m(KostalManagedESS.ChannelId.SET_ACTIVE_POWER,
								new FloatDoublewordElement(1034)
										.wordOrder(LSWMSW)))); //
	}

	@Override
	public String debugLog() {
		return ("SoC:" + this.getSoc().asString() + //
				"|Battery:" + this.getActivePower().asString() + //
				"|PV:" + Integer.toString(getInverterPower()) + " W" + //
				"|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit()
				+ ";" + //
				this.getAllowedDischargePower().asString() + //
				"|Power:" + this.lastSetActivePower) + //
				"|Managed:" + this.isManaged();
	}

//	@Override
//	public Constraint[] getStaticConstraints() throws OpenemsNamedException {
//		// Read-Only-Mode
//		if (this.config.readOnlyMode()) {
//			return new Constraint[]{ //
//					this.createPowerConstraint("Read-Only-Mode", Phase.ALL,
//							Pwr.ACTIVE, Relationship.EQUALS, 0), //
//			};
//		}
//
//		if ((this.getSoc().get() < 100) && (this.getSoc().get() > 5)) {
//			// Active Power constraints
//			return new Constraint[]{ //
//					this.createPowerConstraint("Plenticore Max Charge Power",
//							Phase.ALL, Pwr.ACTIVE,
//							Relationship.GREATER_OR_EQUALS,
//							-1 * this.getAllowedChargePower().get()), //
//					this.createPowerConstraint("Plenticore Max Discharge Power",
//							Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS,
//							this.getAllowedDischargePower().get())};
//		}
//		
//		if (this.getSoc().get() <= 5) {
//			// Active Power constraints
//			return new Constraint[]{ //
//					this.createPowerConstraint("Plenticore Max Charge Power",
//							Phase.ALL, Pwr.ACTIVE,
//							Relationship.GREATER_OR_EQUALS,
//							-1 * this.getAllowedChargePower().get()), //
//					this.createPowerConstraint("Plenticore Max Discharge Power",
//							Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS,
//							0)};
//		}
//		
//		if (this.getSoc().get() > 99) {
//			// Active Power constraints (im
//			return new Constraint[]{ //
//					this.createPowerConstraint("Plenticore Max Charge Power",
//							Phase.ALL, Pwr.ACTIVE,
//							Relationship.GREATER_OR_EQUALS, 0), //
//					this.createPowerConstraint("Plenticore Max Discharge Power",
//							Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS,
//							this.getAllowedDischargePower().get())};
//		}
//		
//		// TODO verify
//		// fail-safe
//		return new Constraint[]{ //
//				this.createPowerConstraint("Fail-Safe Mode", Phase.ALL,
//						Pwr.ACTIVE, Relationship.EQUALS, 0), //
//		};
//	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public Timedata getTimedata() {
		return this.timeData;
	}

	@Override
	public void handleEvent(Event event) {
		// switch (event.getTopic()) {
		// case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
		// //this._setActualPower(getInverterPower());
		// //this._setActualPower(100);
		// System.out.print("== update values before process image ==");
		// break;
		// case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE :
		// // TODO set EssDcCharger Power
		// // (EssDcCharger.ChannelId.ACTUAL_POWER)
		// //this._setActualPower(getInverterPower());
		// //this._setActualPower(100);
		// System.out.print("== update values after process image ==");
		// break;
		// default :
		// System.out.print("== other: " + event.getTopic() + " ==");
		// }
	}

	@Override
	public boolean isManaged() {
		return (this.config.enabled() && !this.config.readOnlyMode());
	}

	private int getInverterPower() {
		int power = -1;
		if (this.inverter != null) {
			try {
				power = this.inverter.getActivePower().get();
			} catch (NullPointerException e) {
				// ignore
				// e.printStackTrace();
			}
		}
		return power;
	}
}
