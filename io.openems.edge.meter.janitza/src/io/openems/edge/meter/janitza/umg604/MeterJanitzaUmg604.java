package io.openems.edge.meter.janitza.umg604;

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
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
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
import io.openems.edge.meter.janitza.umg604.Config;

/**
 * Implements the Janitza UMG 604 power analyzer.
 * 
 * https://www.janitza.de/umg-604-pro.html
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Janitza.UMG604", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterJanitzaUmg604 extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent, ModbusSlave {

	private MeterType meterType = MeterType.PRODUCTION;

	/*
	 * Invert power values
	 */
	private boolean invert = false;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterJanitzaUmg604() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
		this.invert = config.invert();

		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
	}

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

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1317, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(1317), //
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(1319), //
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(1321), //
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(SymmetricMeter.ChannelId.VOLTAGE, new FloatDoublewordElement(1323),
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(1325), //
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(1327), //
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(1329), //
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)), //
						m(SymmetricMeter.ChannelId.CURRENT, new FloatDoublewordElement(1331),
								ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(1333), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(1335), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(1337), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(1339),
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(1341), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(1343), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(1345), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert)), //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(1345), //
								ElementToChannelConverter.INVERT_IF_TRUE(this.invert))), //
				new FC3ReadRegistersTask(1439, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(1439), //
								ElementToChannelConverter.SCALE_FACTOR_3))//
		);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
