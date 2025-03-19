package io.openems.edge.kostal.pvinverter;

import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;

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
import org.slf4j.Logger;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(
		//
		name = "PV-Inverter.Kostal.Plenticore", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION", //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class KostalPvInverterImpl extends AbstractOpenemsModbusComponent
		implements
			KostalPvInverter,
			// ManagedSymmetricPvInverter,
			EssDcCharger,
			// ElectricityMeter,
			ModbusComponent,
			OpenemsComponent,
			EventHandler,
			ModbusSlave,
			TimedataProvider {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(
			this, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	protected Config config;

	public KostalPvInverterImpl() {
		super(
				//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				// ElectricityMeter.ChannelId.values(), //
				EssDcCharger.ChannelId.values(),
				ManagedSymmetricPvInverter.ChannelId.values(), //
				KostalPvInverter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config)
			throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(),
				config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.config = config;
		// this._setMaxApparentPower(config.maxActivePower());

		// Stop if component is disabled
		if (!config.enabled()) {
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
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(531, Priority.HIGH, //
						m(EssDcCharger.ChannelId.MAX_ACTUAL_POWER,
								new FloatDoublewordElement(531)
										.wordOrder(LSWMSW))),

				new FC3ReadRegistersTask(1066, Priority.HIGH, //
						m(EssDcCharger.ChannelId.ACTUAL_POWER,
								new FloatDoublewordElement(1066)
										.wordOrder(LSWMSW))));
	}

	// @Override
	// public MeterType getMeterType() {
	// return MeterType.PRODUCTION;
	// }

	@Override
	public String debugLog() {
		// return "L:" + this.getActivePower().asString();
		return "L:" + this.getActualPower().asString();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(
				//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter
						.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub

	}
}
