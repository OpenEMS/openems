package io.openems.edge.heat.askoma;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.meter.api.ElectricityMeter.calculatePhasesFromActivePower;

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
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.ManagedHeatElement;
import io.openems.edge.heat.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Heat.Askoma", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class HeatAskomaImpl extends AbstractOpenemsModbusComponent implements HeatAskoma, ModbusComponent,
		OpenemsComponent, ElectricityMeter, Heat, ManagedHeatElement, TimedataProvider, EventHandler {

	// gets the total energy consumption in kWh
	private final CalculateEnergyFromPower totalEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public HeatAskomaImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				HeatAskoma.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Heat.ChannelId.values(), //
				ManagedHeatElement.ChannelId.values() //
		);

		calculatePhasesFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this,
				new FC4ReadInputRegistersTask(109, Priority.HIGH, m(new BitsWordElement(109, this) //
						.bit(0, HeatAskoma.ChannelId.HEATER1_ACTIVE) //
						.bit(1, HeatAskoma.ChannelId.HEATER2_ACTIVE) //
						.bit(2, HeatAskoma.ChannelId.HEATER3_ACTIVE) //
						.bit(3, HeatAskoma.ChannelId.PUMP_ACTIVE) //
						.bit(4, HeatAskoma.ChannelId.RELAYBOARD_IS_CONNECTED) //
						.bit(5, HeatAskoma.ChannelId.HEATER_1_2_3_CURRENT_FLOW) //
						.bit(6, HeatAskoma.ChannelId.HEAT_PUMP_REQUEST_ACTIVE) //
						.bit(7, HeatAskoma.ChannelId.EMERGENCY_MODE_ACTIVE) //
						.bit(8, HeatAskoma.ChannelId.LEGIONELLA_PROTECTION_ACTIVE) //
						.bit(9, HeatAskoma.ChannelId.ANALOG_INPUT_ACTIVE) //
						.bit(10, HeatAskoma.ChannelId.LOAD_SETPOINT_ACTIVE) //
						.bit(11, HeatAskoma.ChannelId.LOAD_FEEDIN_ACTIVE) //
						.bit(12, HeatAskoma.ChannelId.AUTO_HEATER_OFF_ACTIVE) //
						.bit(13, HeatAskoma.ChannelId.PUMP_RELAY_FOLLOW_UP_ACTIVE) //
						.bit(14, HeatAskoma.ChannelId.TEMPERATURE_LIMIT_REACHED) //
						.bit(15, HeatAskoma.ChannelId.ANY_ERROR_OCCURRED) //
				), m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(110))),
				new FC4ReadInputRegistersTask(638, Priority.HIGH, m(new UnsignedWordElement(638)) //
						.m(Heat.ChannelId.TEMPERATURE, SCALE_FACTOR_1) //
						.build())); //
	}

	@Override
	public String debugLog() {
		return "Power: " + this.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).value() //
				+ " | Power L1: " + this.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L1).value() //
				+ " | Power L2: " + this.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L2).value() //
				+ " | Power L3: " + this.channel(ElectricityMeter.ChannelId.ACTIVE_POWER_L3).value() //
				+ " | Temp0: " + this.channel(Heat.ChannelId.TEMPERATURE).value() //
				+ " | HEATER_1_2_3_CURRENT_FLOW: "
				+ this.channel(HeatAskoma.ChannelId.HEATER_1_2_3_CURRENT_FLOW).value() //
				+ " | TEMPERATURE_LIMIT_REACHED: "
				+ this.channel(HeatAskoma.ChannelId.TEMPERATURE_LIMIT_REACHED).value() //
				+ " | ANY_ERROR_OCCURRED: " + this.channel(HeatAskoma.ChannelId.ANY_ERROR_OCCURRED).value() //
		;

	}

	@Override
	public MeterType getMeterType() {
		return MeterType.CONSUMPTION_METERED;
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			this.totalEnergy.update(this.getActivePower().orElse(0));

			this.setStatus();
		}
		}
	}

	protected void setStatus() {
		Status status = Status.NO_CONTROL_SIGNAL;

		if (getHeaterCurrentFlow().orElse(false)) {
			// heating
			status = Status.EXCESS;
		} else {
			if (getTemperatureLimiteReached().orElse(false)) {
				// Temperature limit reached
				status = Status.TEMPERATURE_REACHED;
			} else {
				// no heating
				status = Status.NO_CONTROL_SIGNAL;
			}
		}
		this.channel(Heat.ChannelId.STATUS).setNextValue(status);
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
