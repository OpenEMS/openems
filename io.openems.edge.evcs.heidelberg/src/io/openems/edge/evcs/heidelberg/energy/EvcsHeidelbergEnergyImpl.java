package io.openems.edge.evcs.heidelberg.energy;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.meter.api.ElectricityMeter.calculateAverageVoltageFromPhases;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;

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

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.CalculateEnergySession;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Heidelberg.Energy", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class EvcsHeidelbergEnergyImpl extends AbstractOpenemsModbusComponent implements EvcsHeidelbergEnergy, Evcs,
		ElectricityMeter, ModbusComponent, EventHandler, TimedataProvider, OpenemsComponent {

	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergySession calculateEnergySession = new CalculateEnergySession(this);
	private Config config;
	private StatusConverter statusConverter = new StatusConverter(this);

	private static final int WATCHDOG_DISABLE = 0;
	private static final int STANDBY_DISABLE = 4;
	// The Heidelberg Evcs has a scale factor of 10 for Current (i.e. 160 = 16A)
	private static final int FAILSAFE_CURRENT = 160;
	private static final int MAXIMUM_ALLOWED_CURRENT = 160;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvcsHeidelbergEnergyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				EvcsHeidelbergEnergy.ChannelId.values() //
		);
		// Provide missing voltages
		calculateAverageVoltageFromPhases(this);

		// Provide phases
		Evcs.calculateUsedPhasesFromCurrent(this);

		// Provide ActivePowerL1,2,3 based on ActivePower
		Evcs.calculatePhasesFromActivePowerAndPhaseCurrents(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.installStateListener();

		this._setMinimumPower(EvcsUtils.milliampereToWatt(this.config.minHwCurrent(), 3));
		this._setMaximumPower(EvcsUtils.milliampereToWatt(this.config.maxHwCurrent(), 3));

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			this.calculateEnergy.update(this.getActivePower().get());
			this.calculateEnergySession.update(this.statusConverter.isVehicleConnected());
			this.updateErrorChannel();
			this.applyEvcsConfiguration();
		}
		}
	}

	private void installStateListener() {
		this.channel(EvcsHeidelbergEnergy.ChannelId.HEIDELBERG_STATE).onUpdate((value) -> {
			this.statusConverter.applyHeidelbergStatus(value);
		});
	}

	private void updateErrorChannel() {
		this._setChargingstationCommunicationFailed(this.getModbusCommunicationFailed());

		// we want to report charging station errors to the customer
		this._setError(this.getStatus() == Status.ERROR);
	}

	/*
	 * The Heidelberg Energy Evcs has a 'Watchdog' and 'Standby' functionality that
	 * will disable the Modbus communication if the values are not set from an
	 * external Modbus source (i.e OpenEms). This Method will also set the necessary
	 * Maximum allowed Current and Failsafe Current that will otherwise be
	 * initialized as 0 in the Evcs Hardware.
	 */
	private void applyEvcsConfiguration() {
		try {
			this.setWatchdog(WATCHDOG_DISABLE);
			/*
			 * The Heidelberg Energy will change to Standby mode if no car is connected and
			 * will disable Modbus communication The value 4 needs to be set to disable the
			 * Standby functionality
			 */
			this.setStandby(STANDBY_DISABLE);
			/*
			 * The Heidelberg will initialize the Maximum current and the Failsafe Current
			 * with 0 when Modbus communication is established. Everything below 60 (=6A) is
			 * interpreted as 0A. 160 (=16A) is the maximum.
			 */
			this.setFailsafeCurrent(FAILSAFE_CURRENT);
			this.setMaxCurrent(MAXIMUM_ALLOWED_CURRENT);
		} catch (OpenemsError.OpenemsNamedException e) {
			FunctionUtils.doNothing();
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var phaseRotated = this.getPhaseRotation();
		// Some Evcs can have an offset of 30001 so it might be necessary to
		// differentiate in the future
		int offset = 0;
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(offset + 5, Priority.HIGH, //
						m(EvcsHeidelbergEnergy.ChannelId.HEIDELBERG_STATE, new UnsignedWordElement(offset + 5)), //
						m(phaseRotated.channelCurrentL1(), new UnsignedWordElement(offset + 6), SCALE_FACTOR_2), //
						m(phaseRotated.channelCurrentL2(), new UnsignedWordElement(offset + 7), SCALE_FACTOR_2), //
						m(phaseRotated.channelCurrentL3(), new UnsignedWordElement(offset + 8), SCALE_FACTOR_2), //
						new DummyRegisterElement(offset + 9), //
						m(phaseRotated.channelVoltageL1(), new UnsignedWordElement(offset + 10)), //
						m(phaseRotated.channelVoltageL2(), new UnsignedWordElement(offset + 11)), //
						m(phaseRotated.channelVoltageL3(), new UnsignedWordElement(offset + 12)), //
						new DummyRegisterElement(offset + 13), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(offset + 14))), //
				new FC6WriteRegisterTask(offset + 257, //
						m(EvcsHeidelbergEnergy.ChannelId.WATCHDOG_TIME, new SignedWordElement(offset + 257))), //
				new FC6WriteRegisterTask(offset + 258, //
						m(EvcsHeidelbergEnergy.ChannelId.STANDBY, new SignedWordElement(offset + 258))), //
				new FC6WriteRegisterTask(offset + 261, //
						m(EvcsHeidelbergEnergy.ChannelId.MAX_CURRENT, new SignedWordElement(offset + 261))), //
				new FC6WriteRegisterTask(offset + 262, //
						m(EvcsHeidelbergEnergy.ChannelId.FAILSAFE_CURRENT, new SignedWordElement(offset + 262))));

	}

	@Override
	public MeterType getMeterType() {
		if (this.config.readOnly()) {
			return MeterType.CONSUMPTION_METERED;
		} else {
			return MeterType.MANAGED_CONSUMPTION_METERED;
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}