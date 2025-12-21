package io.openems.edge.evcs.abl;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.evcs.api.Evcs.calculatePhasesFromActivePowerAndPhaseCurrents;
import static io.openems.edge.evcs.api.Evcs.calculateUsedPhasesFromCurrent;
import static io.openems.edge.evcs.api.EvcsUtils.milliampereToWatt;
import static io.openems.edge.meter.api.ElectricityMeter.calculateSumCurrentFromPhases;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.CalculateEnergySession;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Abl", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE })
public class EvcsAblImpl extends AbstractOpenemsModbusComponent
		implements EvcsAbl, Evcs, ElectricityMeter, ModbusComponent, EventHandler, TimedataProvider, OpenemsComponent {

	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergySession calculateEnergySession = new CalculateEnergySession(this);

	/** Converts the ABL internal status into the EVCS-Status. */
	private final StatusConverter statusConverter = new StatusConverter(this);
	private Config config;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private static final int OFFSET = 0x100;

	public EvcsAblImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				EvcsAbl.ChannelId.values() //
		);

		calculateSumCurrentFromPhases(this);
		calculateUsedPhasesFromCurrent(this);
		calculatePhasesFromActivePowerAndPhaseCurrents(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.installStateListener();

		this._setMinimumPower(milliampereToWatt(this.config.minHwCurrent(), 3));
		this._setMaximumPower(milliampereToWatt(this.config.maxHwCurrent(), 3));
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
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			this.calculateEnergy.update(this.getActivePower().get());
			this.calculateEnergySession.update(this.statusConverter.isVehicleConnected());
			this.updateErrorChannel();
		}
		}
	}

	private void installStateListener() {
		this.channel(EvcsAbl.ChannelId.CHARGE_POINT_STATE).onUpdate(v -> {
			this.statusConverter.convertAblStatus();
		});
	}

	private void updateErrorChannel() {
		this._setChargingstationCommunicationFailed(this.getModbusCommunicationFailed());

		// we want to report charging station errors to the customer
		this._setError(this.getStatus() == Status.ERROR);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final var phaseRotated = this.getPhaseRotation();
		var offset = OFFSET * this.config.plug().plug;

		return new ModbusProtocol(this, new FC3ReadRegistersTask(12289 + offset, Priority.HIGH, //
				m(phaseRotated.channelCurrentL1(), new UnsignedDoublewordElement(12289 + offset), //
						SCALE_FACTOR_2), //
				m(phaseRotated.channelCurrentL2(), new UnsignedDoublewordElement(12291 + offset), //
						SCALE_FACTOR_2), //
				m(phaseRotated.channelCurrentL3(), new UnsignedDoublewordElement(12293 + offset), //
						SCALE_FACTOR_2), //
				new DummyRegisterElement(12295 + offset, 12300 + offset), //
				m(ElectricityMeter.ChannelId.ACTIVE_POWER, //
						new UnsignedDoublewordElement(12301 + offset)),
				m(EvcsAbl.ChannelId.RAW_ACTIVE_PRODUCTION_ENERGY, //
						new UnsignedDoublewordElement(12303 + offset), //
						SCALE_FACTOR_1), //
				new DummyRegisterElement(12305 + offset, 12336 + offset), //
				m(EvcsAbl.ChannelId.CHARGE_POINT_STATE, new UnsignedWordElement(12337 + offset)), //
				m(EvcsAbl.ChannelId.CURRENT_LIMIT, new UnsignedWordElement(12338 + offset), //
						SCALE_FACTOR_MINUS_1)));

		/*
		 * // this should be a FC6 write task, but ABL response to FC6 is corrupt if
		 * (!this.config.readOnly()) { protocol.addTask(new FC16WriteRegistersTask(12338
		 * + offset, // m(EvcsAbl.ChannelId.CHARGING_CURRENT, new
		 * UnsignedWordElement(12338 + offset), // SCALE_FACTOR_MINUS_1)));
		 * 
		 * }
		 */
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}
