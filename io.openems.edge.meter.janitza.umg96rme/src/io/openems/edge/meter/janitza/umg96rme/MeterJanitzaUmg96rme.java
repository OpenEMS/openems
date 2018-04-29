package io.openems.edge.meter.janitza.umg96rme;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Priority;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.Meter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.asymmetric.api.AsymmetricMeter;
import io.openems.edge.meter.symmetric.api.SymmetricMeter;

/**
 * Implements the Janitza UMG 96RM-E power analyser
 * 
 * https://www.janitza.com/umg-96rm-e.html
 * 
 * @author stefan.feilmeier
 *
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Janitza.UMG96RME")
public class MeterJanitzaUmg96rme extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

	private final static int UNIT_ID = 1;

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterJanitzaUmg96rme() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbusTcp modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		// get Meter Type:
		try {
			this.meterType = MeterType.valueOf(config.type().toUpperCase());
		} catch (IllegalArgumentException e) {
			this.meterType = MeterType.PRODUCTION; // default
		}

		super.activate(context, config.service_pid(), config.id(), config.enabled(), UNIT_ID, this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
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
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		/*
		 * We are using the FLOAT registers from the modbus table, because they are all
		 * reachable within one ReadMultipleRegistersRequest.
		 */
		return new ModbusProtocol(unitId, //
				new FC3ReadRegistersTask(800, Priority.HIGH, //
						m(Meter.ChannelId.FREQUENCY, new FloatDoublewordElement(800),
								ElementToChannelConverter.FLOAT_TO_INT_AND_SCALE_FACTOR_3),
						new DummyRegisterElement(802, 807), //
						cm(new FloatDoublewordElement(808)) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1,
										ElementToChannelConverter.FLOAT_TO_INT_AND_SCALE_FACTOR_3) //
								.m(SymmetricMeter.ChannelId.VOLTAGE,
										ElementToChannelConverter.FLOAT_TO_INT_AND_SCALE_FACTOR_3) //
								.build(), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(810),
								ElementToChannelConverter.FLOAT_TO_INT_AND_SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(812),
								ElementToChannelConverter.FLOAT_TO_INT_AND_SCALE_FACTOR_3),
						new DummyRegisterElement(814, 859), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(860),
								ElementToChannelConverter.FLOAT_TO_INT_AND_SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(862),
								ElementToChannelConverter.FLOAT_TO_INT_AND_SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(864),
								ElementToChannelConverter.FLOAT_TO_INT_AND_SCALE_FACTOR_3),
						m(SymmetricMeter.ChannelId.CURRENT, new FloatDoublewordElement(866),
								ElementToChannelConverter.FLOAT_TO_INT_AND_SCALE_FACTOR_3),
						cm(new FloatDoublewordElement(868)) //
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, ElementToChannelConverter.FLOAT_TO_INT) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_ACTIVE_POWER_L1,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_ACTIVE_POWER_L1,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(870)) //
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, ElementToChannelConverter.FLOAT_TO_INT) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_ACTIVE_POWER_L2,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_ACTIVE_POWER_L2,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(872)) //
								.m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, ElementToChannelConverter.FLOAT_TO_INT) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_ACTIVE_POWER_L3,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_ACTIVE_POWER_L3,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(874)) //
								.m(SymmetricMeter.ChannelId.ACTIVE_POWER, ElementToChannelConverter.FLOAT_TO_INT) //
								.m(SymmetricMeter.ChannelId.CONSUMPTION_ACTIVE_POWER,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_NEGATIVE_INVERT) //
								.m(SymmetricMeter.ChannelId.PRODUCTION_ACTIVE_POWER,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(876)) //
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, ElementToChannelConverter.FLOAT_TO_INT) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_REACTIVE_POWER_L1,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_REACTIVE_POWER_L1,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(878)) //
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, ElementToChannelConverter.FLOAT_TO_INT) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_REACTIVE_POWER_L2,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_REACTIVE_POWER_L2,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(880)) //
								.m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, ElementToChannelConverter.FLOAT_TO_INT) //
								.m(AsymmetricMeter.ChannelId.CONSUMPTION_REACTIVE_POWER_L3,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_NEGATIVE_INVERT) //
								.m(AsymmetricMeter.ChannelId.PRODUCTION_REACTIVE_POWER_L3,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_POSITIVE) //
								.build(), //
						cm(new FloatDoublewordElement(882)) //
								.m(SymmetricMeter.ChannelId.REACTIVE_POWER, ElementToChannelConverter.FLOAT_TO_INT) //
								.m(SymmetricMeter.ChannelId.CONSUMPTION_REACTIVE_POWER,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_NEGATIVE_INVERT) //
								.m(SymmetricMeter.ChannelId.PRODUCTION_REACTIVE_POWER,
										ElementToChannelConverter.FLOAT_TO_INT_AND_CONVERT_POSITIVE) //
								.build() //
				));
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().format();
	}
}
