package io.openems.edge.evcs.hypercharger;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;

import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.AlpitronicHypercharger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class HyperchargerImpl extends AbstractOpenemsModbusComponent
		implements Evcs, ManagedEvcs, OpenemsComponent, ModbusComponent, EventHandler, Hypercharger, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(HyperchargerImpl.class);

	protected Config config;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	/*
	 * Modbus offset for multiple connectors.
	 */
	private IntFunction<Integer> offset = addr -> addr + this.config.connector().modbusOffset;

	/**
	 * Calculates the value for total energy in [Wh_Σ].
	 * 
	 * <p>
	 * Accumulates the energy by calling this.calculateTotalEnergy.update(power);
	 */
	private CalculateEnergyFromPower calculateTotalEnergy;

	/**
	 * Handles charge states.
	 */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/**
	 * Processes the controller's writes to this evcs component.
	 */
	private final WriteHandler writeHandler = new WriteHandler(this);

	public HyperchargerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Hypercharger.ChannelId.values());
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limit and the phases used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);

		this.applyConfig(context, config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(context, config);
	}

	private void applyConfig(ComponentContext context, Config config) {
		this.config = config;
		this.calculateTotalEnergy = new CalculateEnergyFromPower(this, Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY);
		this._setFixedMinimumHardwarePower(config.minHwPower());
		this._setFixedMaximumHardwarePower(config.maxHwPower());
		this._setPowerPrecision(1);
		this._setPhases(3);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.calculateTotalEnergy.update(this.getChargePower().orElse(0));
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.writeHandler.run();
			break;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		var modbusProtocol = new ModbusProtocol(this,

				new FC3ReadRegistersTask(this.offset.apply(0), Priority.LOW,
						m(Hypercharger.ChannelId.RAW_CHARGE_POWER_SET,
								new UnsignedDoublewordElement(this.offset.apply(0)))),

				new FC16WriteRegistersTask(this.offset.apply(0),
						m(Hypercharger.ChannelId.APPLY_CHARGE_POWER_LIMIT,
								new UnsignedDoublewordElement(this.offset.apply(0))),
						m(Hypercharger.ChannelId.SETPOINT_REACTIVE_POWER,
								new UnsignedDoublewordElement(this.offset.apply(2)))),

				new FC4ReadInputRegistersTask(this.offset.apply(0), Priority.LOW,
						m(Hypercharger.ChannelId.RAW_STATUS, new UnsignedWordElement(this.offset.apply(0))),
						m(Hypercharger.ChannelId.CHARGING_VOLTAGE, new UnsignedDoublewordElement(this.offset.apply(1)),
								SCALE_FACTOR_MINUS_2),
						m(Hypercharger.ChannelId.CHARGING_CURRENT, new UnsignedWordElement(this.offset.apply(3)),
								SCALE_FACTOR_MINUS_2),
						/*
						 * TODO: Test charge power register with newer firmware versions. Register value
						 * was always 0 with versions < 1.7.2.
						 */
						m(Hypercharger.ChannelId.RAW_CHARGE_POWER, new UnsignedDoublewordElement(this.offset.apply(4))),
						m(Hypercharger.ChannelId.CHARGED_TIME, new UnsignedWordElement(this.offset.apply(6))),
						m(Hypercharger.ChannelId.CHARGED_ENERGY, new UnsignedWordElement(this.offset.apply(7)),
								SCALE_FACTOR_MINUS_2).onUpdateCallback(e -> {
									if (e == null) {
										return;
									}

									/**
									 * The internal session energy is set to 0 when the charging process has
									 * finished. The SessionEnergy Channel should still contain the current value
									 * for visualization.
									 */
									if (e == 0) {
										switch (this.getStatus()) {
										case UNDEFINED:
										case NOT_READY_FOR_CHARGING:
										case STARTING:
											this._setEnergySession(0);
											return;
										case CHARGING:
										case CHARGING_FINISHED:
										case CHARGING_REJECTED:
										case ENERGY_LIMIT_REACHED:
										case ERROR:
										case READY_FOR_CHARGING:
											// Ignore 0 value
											return;
										}
									}
									this._setEnergySession(e * 10);
								}),
						// TODO: Implement SocEvcs Nature & map SoC register
						m(Hypercharger.ChannelId.EV_SOC, new UnsignedWordElement(this.offset.apply(8)),
								SCALE_FACTOR_MINUS_2),
						m(Hypercharger.ChannelId.CONNECTOR_TYPE, new UnsignedWordElement(this.offset.apply(9))),

						/*
						 * Not equals MaximumPower or MinimumPower e.g. EvMaxChargingPower is 99kW, but
						 * ChargePower is 40kW because of temperature, current SoC or
						 * MaximumHardwareLimit.
						 */
						m(Hypercharger.ChannelId.EV_MAX_CHARGING_POWER,
								new UnsignedDoublewordElement(this.offset.apply(10))),
						m(Hypercharger.ChannelId.EV_MIN_CHARGING_POWER,
								new UnsignedDoublewordElement(this.offset.apply(12))),
						m(Hypercharger.ChannelId.VAR_REACTIVE_MAX,
								new UnsignedDoublewordElement(this.offset.apply(14))),
						m(Hypercharger.ChannelId.VAR_REACTIVE_MIN, new UnsignedDoublewordElement(this.offset.apply(16)),
								INVERT))

		);

		// Calculates charge power by existing Channels.
		this.addCalculatePowerListeners();

		// Map raw status to evcs status.
		this.addStatusListener();

		return modbusProtocol;
	}

	/*
	 * TODO: Remove if the charge power register returns valid values with newer
	 * firmware versions.
	 */
	private void addCalculatePowerListeners() {

		// Calculate power from voltage and current
		final Consumer<Value<Double>> calculatePower = ignore -> {
			this._setChargePower(TypeUtils.getAsType(OpenemsType.INTEGER, TypeUtils.multiply(//
					this.getChargingVoltageChannel().getNextValue().get(), //
					this.getChargingCurrentChannel().getNextValue().get() //
			)));
		};
		this.getChargingVoltageChannel().onSetNextValue(calculatePower);
		this.getChargingCurrentChannel().onSetNextValue(calculatePower);
	}

	private void addStatusListener() {
		this.channel(Hypercharger.ChannelId.RAW_STATUS).onSetNextValue(s -> {
			AvailableState rawState = s.asEnum();
			/**
			 * Maps the raw state into a {@link Status}.
			 */
			switch (rawState) {
			case AVAILABLE:
				this._setStatus(Status.NOT_READY_FOR_CHARGING);
				break;
			case PREPARING_TAG_ID_READY:
				this._setStatus(Status.READY_FOR_CHARGING);
				break;
			case CHARGING:
			case PREPARING_EV_READY:
				this._setStatus(Status.CHARGING);
				break;
			case RESERVED:
			case SUSPENDED_EV:
			case SUSPENDED_EV_SE:
				this._setStatus(Status.CHARGING_REJECTED);
				break;
			case FINISHING:
				this._setStatus(Status.CHARGING_FINISHED);
				break;
			case FAULTED:
			case UNAVAILABLE:
			case UNAVAILABLE_CONNECTION_OBJECT:
				this._setStatus(Status.ERROR);
				break;
			case UNAVAILABLE_FW_UPDATE:
			case UNDEFINED:
			default:
				this._setStatus(Status.UNDEFINED);
			}
		});
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return this.config.minHwPower();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return this.config.maxHwPower();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		this.setApplyChargePowerLimit(power);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		// Alpitronic is running into a fault state if the applied power is 0
		return this.applyChargePowerLimit(this.config.minHwPower());
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 10;
	}

	@Override
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}

	@Override
	public void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.getSetChargePowerLimit().orElse(null) + "|" + this.getStatus().getName();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
