package io.openems.edge.evcs.goe.modbus;

import static io.openems.edge.evcs.api.Evcs.calculatePhasesFromActivePowerAndPhaseCurrents;
import static io.openems.edge.evcs.api.Evcs.calculateUsedPhasesFromCurrent;
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

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.CalculateEnergySession;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.goe.api.EvcsGoe;
import io.openems.edge.evcs.goe.api.StatusConverter;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Goe.Modbus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class EvcsGoeModbusImpl extends AbstractOpenemsModbusComponent implements EvcsGoeModbus, EvcsGoe, Evcs, ElectricityMeter,
		ModbusComponent, EventHandler, TimedataProvider, OpenemsComponent {

	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergySession calculateEnergySession = new CalculateEnergySession(this);
	private Config config;
	private StatusConverter statusConverter = new StatusConverter(this);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvcsGoeModbusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				EvcsGoe.ChannelId.values() //
		);
		calculateAverageVoltageFromPhases(this);

		calculateUsedPhasesFromCurrent(this);

		calculatePhasesFromActivePowerAndPhaseCurrents(this);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
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
			this.updateErrorChannels();
		}
		}
	}

	private void installStateListener() {
		this.channel(EvcsGoe.ChannelId.GOE_STATE).onUpdate((value) -> {
			this.statusConverter.applyGoeStatus(value);
		});
	}

	private void updateErrorChannels() {
		this._setChargingstationCommunicationFailed(this.getModbusCommunicationFailed());

		// we want to report charging station errors to the customer
		this._setError(this.getStatus() == Status.ERROR);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var phaseRotated = this.getPhaseRotation();
		return new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(100, Priority.HIGH, //
						m(EvcsGoe.ChannelId.GOE_STATE, new UnsignedWordElement(100)),
						new DummyRegisterElement(101, 107), //
						m(phaseRotated.channelVoltageL1(), new UnsignedDoublewordElement(108)),
						m(phaseRotated.channelVoltageL2(), new UnsignedDoublewordElement(110)),
						m(phaseRotated.channelVoltageL3(), new UnsignedDoublewordElement(112)),
						m(phaseRotated.channelCurrentL1(), new UnsignedDoublewordElement(114), //
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(phaseRotated.channelCurrentL2(), new UnsignedDoublewordElement(116), //
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(phaseRotated.channelCurrentL3(), new UnsignedDoublewordElement(118), //
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedDoublewordElement(120), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1))); //
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

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

}