package io.openems.edge.evcs.mennekes;

import java.util.Collection;
import java.util.Iterator;
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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.Channel;
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
		name = "Evcs.Mennekes", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class MennekesAmtronImpl extends AbstractOpenemsModbusComponent implements Evcs, ManagedEvcs, OpenemsComponent,
		ModbusComponent, EventHandler, TimedataProvider, MennekesAmtron {

	private final Logger log = LoggerFactory.getLogger(MennekesAmtronImpl.class);

	protected Config config;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	/**
	 * Calculates the value for total energy in [Wh].
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

	public MennekesAmtronImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				MennekesAmtron.ChannelId.values());
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

		ModbusProtocol modbusProtocol = new ModbusProtocol(this,
				new FC3ReadRegistersTask(100, Priority.HIGH, 
				m(MennekesAmtron.ChannelId.RAW_FIRMWARE_VERSION,
						new UnsignedDoublewordElement(100).wordOrder(WordOrder.MSWLSW))
				));
				
		return modbusProtocol;
	}

	/*
	 * TODO: Remove if the charge power register returns valid values with newer
	 * firmware versions.
	 */
//	private void addCalculatePowerListeners() {
//
//		// Calculate power from voltage and current
//		final Consumer<Value<Double>> calculatePower = ignore -> {
//			this._setChargePower(TypeUtils.getAsType(OpenemsType.INTEGER, TypeUtils.multiply(//
//					this.getChargingVoltageChannel().getNextValue().get(), //
//					this.getChargingCurrentChannel().getNextValue().get() //
//			)));
//		};
//		this.getChargingVoltageChannel().onSetNextValue(calculatePower);
//		this.getChargingCurrentChannel().onSetNextValue(calculatePower);
//	}

//	private void addStatusListener() {
//		this.channel(MennekesAmtron.ChannelId.RAW_STATUS).onSetNextValue(s -> {
//			AvailableState rawState = s.asEnum();
//			/**
//			 * Maps the raw state into a {@link Status}.
//			 */
//			switch (rawState) {
//			case AVAILABLE:
//				this._setStatus(Status.NOT_READY_FOR_CHARGING);
//				break;
//			case PREPARING_TAG_ID_READY:
//				this._setStatus(Status.READY_FOR_CHARGING);
//				break;
//			case CHARGING:
//			case PREPARING_EV_READY:
//				this._setStatus(Status.CHARGING);
//				break;
//			case RESERVED:
//			case SUSPENDED_EV:
//			case SUSPENDED_EV_SE:
//				this._setStatus(Status.CHARGING_REJECTED);
//				break;
//			case FINISHING:
//				this._setStatus(Status.CHARGING_FINISHED);
//				break;
//			case FAULTED:
//			case UNAVAILABLE:
//			case UNAVAILABLE_CONNECTION_OBJECT:
//				this._setStatus(Status.ERROR);
//				break;
//			case UNAVAILABLE_FW_UPDATE:
//			case UNDEFINED:
//			default:
//				this._setStatus(Status.UNDEFINED);
//			}
//		});
//	}

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

//	@Override
//	public boolean applyChargePowerLimit(int power) throws Exception {
//		this.setApplyChargePowerLimit(power);
//		return true;
//	}

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

//	private void writeChargePowerChannel() {
//		int power = this.getActivePowerL1().orElse(0);
//		power = power + this.getActivePowerL2().orElse(0);
//		power = power + this.getActivePowerL3().orElse(0);
//		this._setChargePower(power);
//	}

//	private void writePhasesChannel() {
//		int phases = 0;
//		int powerThreshold = 50; // in W
//		if (this.getActivePowerL1().orElse(0) > powerThreshold) {
//			phases = phases + 1;
//		}
//		if (this.getActivePowerL2().orElse(0) > powerThreshold) {
//			phases = phases + 1;
//		}
//		if (this.getActivePowerL3().orElse(0) > powerThreshold) {
//			phases = phases + 1;
//		}
//		this._setPhases(phases);
//	}

	private void setEvcsStatus() {
		OcppStateMennekes currentOcppState = this.getOcppCpStatus();
		Status currentStatus = Status.UNDEFINED;
		switch (currentOcppState) {
		case AVAILABLE:
			currentStatus = Status.READY_FOR_CHARGING;
			break;
		case CHARGING:
			currentStatus = Status.CHARGING;
			break;
		case FAULTED:
			currentStatus = Status.ERROR;
			break;
		case FINISHING:
			currentStatus = Status.CHARGING_FINISHED;
			break;
		case PREPARING:
			currentStatus = Status.READY_FOR_CHARGING;
			break;
		case RESERVED:
			currentStatus = Status.NOT_READY_FOR_CHARGING;
			break;
		case SUSPENDEDEV:
			currentStatus = Status.CHARGING_REJECTED;
			break;
		case SUSPENDEDEVSE:
			currentStatus = Status.CHARGING_REJECTED;
			break;
//		case OCCUPIED:
//			if (this.isCharging() || this.isStoppedByEms()) {
//				currentStatus = Status.CHARGING;
//			}
//			if (!this.isCharging() && !this.isStoppedByEms() && this.chargingSession.isStarted()) {
//				currentStatus = Status.ENERGY_LIMIT_REACHED;
//			}
//			if (!this.isCharging() && !this.isStoppedByEms() && this.chargingSession.isInitialized()) {
//				currentStatus = Status.NOT_READY_FOR_CHARGING;
//			}
//			break;
//		case UNAVAILABLE:
//			this.logInfo("Charging Station is Unavailable.");
//			this._setChargingstationCommunicationFailed(true);
//			currentStatus = Status.ERROR;
//			break;
		}
	}

	private void setCommunicationStatusChannel() {
		this._setChargingstationCommunicationFailed(this.getModbusCommunicationFailed().orElse(true));
	}

//	private void setMinimumHardwarePowerChannel() {
//		int phases = this.getPhasesChannel().getNextValue().orElse(0);
//		if (phases == 0) {
//			phases = 3;
//		}
//		int minPower = this.voltageOnePhase * this.getMinCurrentLimit().orElse(6) * phases;
//		this._setMinimumHardwarePower(minPower);
//	}

//	private void setMaximumHardwarePowerChannel() {
//		int phases = this.getPhases().orElse(0);
//		if (phases == 0) {
//			phases = 3;
//		}
//		int maxPower = phases * MAX_HARDWARE_CURRENT * this.voltageOnePhase;
//		this._setMaximumHardwarePower(maxPower);
//	}

	private void printAllChannels() {
		Collection<Channel<?>> listOfChannels = this.channels();
		Iterator<Channel<?>> iterator = listOfChannels.iterator();
		for (iterator = (Iterator<Channel<?>>) listOfChannels.iterator(); ((Iterator<Channel<?>>) iterator)
				.hasNext();) {
			Channel<?> channel = iterator.next();
			String name = channel.channelId().id();
			System.out.println(name + ": " + channel.getNextValue().asString());
		}
	}

	private void printChannel(Channel<?> channel) {
		String name = channel.channelId().id();
		System.out.println(name + ": " + channel.getNextValue().asString());
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

}
