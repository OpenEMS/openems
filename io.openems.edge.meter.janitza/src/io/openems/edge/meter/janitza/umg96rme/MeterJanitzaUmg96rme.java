package io.openems.edge.meter.janitza.umg96rme;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Implements the Janitza UMG 96RM-E power analyzer.
 *
 * <p>
 * https://www.janitza.com/umg-96rm-e.html
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Janitza.UMG96RME", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterJanitzaUmg96rme extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private MeterType meterType = MeterType.PRODUCTION;

	/*
	 * Invert power values
	 */
	private boolean invert = false;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterJanitzaUmg96rme() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		this.invert = config.invert();

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

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		/*
		 * We are using the FLOAT registers from the modbus table, because they are all
		 * reachable within one ReadMultipleRegistersRequest.
		 */
		var modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(800, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(800), SCALE_FACTOR_3),
						new DummyRegisterElement(802, 807), //
						m(new FloatDoublewordElement(808)) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, SCALE_FACTOR_3) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, SCALE_FACTOR_3) //
								.build(), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(810), SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(812), SCALE_FACTOR_3),
						new DummyRegisterElement(814, 859), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(860), SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(862), SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(864), SCALE_FACTOR_3),
						m(SymmetricMeter.ChannelId.CURRENT, new FloatDoublewordElement(866), SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(868),
								INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(870),
								INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(872),
								INVERT_IF_TRUE(this.invert)),
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(874),
								INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(876),
								INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(878),
								INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(880),
								INVERT_IF_TRUE(this.invert)),
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(882),
								INVERT_IF_TRUE(this.invert))));

		if (this.invert) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(19068, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(19068)),
					new DummyRegisterElement(19070, 19075),
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(19076))));
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(19068, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(19068)),
					new DummyRegisterElement(19070, 19075),
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(19076))));
		}

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);

	}
}
