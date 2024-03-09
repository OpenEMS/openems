package io.openems.edge.io.shelly.shellypro3em;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Shelly.Pro.3EM", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IoShellyPro3EmImpl extends AbstractOpenemsModbusComponent implements IoShellyPro3Em, ElectricityMeter,
		ModbusComponent, OpenemsComponent, ModbusSlave, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;

	public IoShellyPro3EmImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				IoShellyPro3Em.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this,

				new FC4ReadInputRegistersTask(1011, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.CURRENT, //
								new FloatDoublewordElement(1011).wordOrder(WordOrder.LSWMSW), SCALE_FACTOR_3)),

				new FC4ReadInputRegistersTask(1013, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, //
								new FloatDoublewordElement(1013).wordOrder(WordOrder.LSWMSW), DIRECT_1_TO_1)),

				new FC4ReadInputRegistersTask(1015, Priority.HIGH, //
						m(IoShellyPro3Em.ChannelId.TOTAL_APPARENT_POWER, //
								new FloatDoublewordElement(1015).wordOrder(WordOrder.LSWMSW), DIRECT_1_TO_1)),

				new FC4ReadInputRegistersTask(1020, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, //
								new FloatDoublewordElement(1020).wordOrder(WordOrder.LSWMSW), SCALE_FACTOR_3)),

				new FC4ReadInputRegistersTask(1022, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.CURRENT_L1, //
								new FloatDoublewordElement(1022).wordOrder(WordOrder.LSWMSW), SCALE_FACTOR_3)),

				new FC4ReadInputRegistersTask(1024, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, //
								new FloatDoublewordElement(1024).wordOrder(WordOrder.LSWMSW), DIRECT_1_TO_1)),

				new FC4ReadInputRegistersTask(1026, Priority.HIGH, //
						m(IoShellyPro3Em.ChannelId.APPARENT_POWER_L1, //
								new FloatDoublewordElement(1026).wordOrder(WordOrder.LSWMSW), DIRECT_1_TO_1)),

				new FC4ReadInputRegistersTask(1028, Priority.HIGH, //
						m(IoShellyPro3Em.ChannelId.COS_PHI_L1, //
								new FloatDoublewordElement(1028).wordOrder(WordOrder.LSWMSW), DIRECT_1_TO_1)),

				new FC4ReadInputRegistersTask(1040, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, //
								new FloatDoublewordElement(1040).wordOrder(WordOrder.LSWMSW), SCALE_FACTOR_3)),

				new FC4ReadInputRegistersTask(1042, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.CURRENT_L2, //
								new FloatDoublewordElement(1042).wordOrder(WordOrder.LSWMSW), SCALE_FACTOR_3)),

				new FC4ReadInputRegistersTask(1044, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, //
								new FloatDoublewordElement(1044).wordOrder(WordOrder.LSWMSW), DIRECT_1_TO_1)),

				new FC4ReadInputRegistersTask(1046, Priority.HIGH, //
						m(IoShellyPro3Em.ChannelId.APPARENT_POWER_L2, //
								new FloatDoublewordElement(1046).wordOrder(WordOrder.LSWMSW), DIRECT_1_TO_1)),

				new FC4ReadInputRegistersTask(1048, Priority.HIGH, //
						m(IoShellyPro3Em.ChannelId.COS_PHI_L2, //
								new FloatDoublewordElement(1048).wordOrder(WordOrder.LSWMSW), DIRECT_1_TO_1)),

				new FC4ReadInputRegistersTask(1060, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, //
								new FloatDoublewordElement(1060).wordOrder(WordOrder.LSWMSW), SCALE_FACTOR_3)),

				new FC4ReadInputRegistersTask(1062, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.CURRENT_L3, //
								new FloatDoublewordElement(1062).wordOrder(WordOrder.LSWMSW), SCALE_FACTOR_3)),

				new FC4ReadInputRegistersTask(1064, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, //
								new FloatDoublewordElement(1064).wordOrder(WordOrder.LSWMSW), DIRECT_1_TO_1)),

				new FC4ReadInputRegistersTask(1066, Priority.HIGH, //
						m(IoShellyPro3Em.ChannelId.APPARENT_POWER_L3, //
								new FloatDoublewordElement(1066).wordOrder(WordOrder.LSWMSW), DIRECT_1_TO_1)),

				new FC4ReadInputRegistersTask(1028, Priority.HIGH, //
						m(IoShellyPro3Em.ChannelId.COS_PHI_L1, //
								new FloatDoublewordElement(1028).wordOrder(WordOrder.LSWMSW), DIRECT_1_TO_1)

				));
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(-activePower);
		}
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
