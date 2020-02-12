package io.openems.edge.goodwe.et.gridmeter;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.AsymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "GoodWe.ET.Grid-Meter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE, //
				"type=GRID" //
		})
public class GoodWeEtGridMeter extends AbstractOpenemsModbusComponent
		implements AsymmetricMeter, SymmetricMeter, OpenemsComponent {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
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

	public GoodWeEtGridMeter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				GridMeterChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.unit_id(), this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //

				new FC3ReadRegistersTask(35123, Priority.LOW, //
						m(GridMeterChannelId.F_GRID_R, new UnsignedWordElement(35123),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(35124, 35127), //
						m(GridMeterChannelId.F_GRID_S, new UnsignedWordElement(35128),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(35129, 35132), //
						m(GridMeterChannelId.F_GRID_T, new UnsignedWordElement(35133),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2)),
				// Safety
				new FC16WriteRegistersTask(45400,
						m(GridMeterChannelId.GRID_VOLT_HIGH_S1, new UnsignedWordElement(45400),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_HIGH_S1_TIME, new UnsignedWordElement(45401)), //
						m(GridMeterChannelId.GRID_VOLT_LOW_S1, new UnsignedWordElement(45402),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_LOW_S1_TIME, new UnsignedWordElement(45403)), //
						m(GridMeterChannelId.GRID_VOLT_HIGH_S2, new UnsignedWordElement(45404),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_HIGH_S2_TIME, new UnsignedWordElement(45405)), //
						m(GridMeterChannelId.GRID_VOLT_LOW_S2, new UnsignedWordElement(45406),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_LOW_S2_TIME, new UnsignedWordElement(45407)), //
						m(GridMeterChannelId.GRID_VOLT_QUALITY, new UnsignedWordElement(45408),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_FREQ_HIGH_S1, new UnsignedWordElement(45409),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_HIGH_S1_TIME, new UnsignedWordElement(45410)), //
						m(GridMeterChannelId.GRID_FREQ_LOW_S1, new UnsignedWordElement(45411),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_LOW_S1_TIME, new UnsignedWordElement(45412)), //
						m(GridMeterChannelId.GRID_FREQ_HIGH_S2, new UnsignedWordElement(45413),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_HIGH_S2_TIME, new UnsignedWordElement(45414)), //
						m(GridMeterChannelId.GRID_FREQ_LOW_S2, new UnsignedWordElement(45415),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_LOW_S2_TIME, new UnsignedWordElement(45416)), //
						m(GridMeterChannelId.GRID_VOLT_HIGH, new UnsignedWordElement(45417),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_LOW, new UnsignedWordElement(45418),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_FREQ_HIGH, new UnsignedWordElement(45419),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_LOW, new UnsignedWordElement(45420),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_RECOVER_TIME, new UnsignedWordElement(45421))), //

				new FC3ReadRegistersTask(45400, Priority.LOW,
						m(GridMeterChannelId.GRID_VOLT_HIGH_S1, new UnsignedWordElement(45400),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_HIGH_S1_TIME, new UnsignedWordElement(45401)), //
						m(GridMeterChannelId.GRID_VOLT_LOW_S1, new UnsignedWordElement(45402),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_LOW_S1_TIME, new UnsignedWordElement(45403)), //
						m(GridMeterChannelId.GRID_VOLT_HIGH_S2, new UnsignedWordElement(45404),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_HIGH_S2_TIME, new UnsignedWordElement(45405)), //
						m(GridMeterChannelId.GRID_VOLT_LOW_S2, new UnsignedWordElement(45406),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_LOW_S2_TIME, new UnsignedWordElement(45407)), //
						m(GridMeterChannelId.GRID_VOLT_QUALITY, new UnsignedWordElement(45408),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_FREQ_HIGH_S1, new UnsignedWordElement(45409),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_HIGH_S1_TIME, new UnsignedWordElement(45410)), //
						m(GridMeterChannelId.GRID_FREQ_LOW_S1, new UnsignedWordElement(45411),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_LOW_S1_TIME, new UnsignedWordElement(45412)), //
						m(GridMeterChannelId.GRID_FREQ_HIGH_S2, new UnsignedWordElement(45413),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_HIGH_S2_TIME, new UnsignedWordElement(45414)), //
						m(GridMeterChannelId.GRID_FREQ_LOW_S2, new UnsignedWordElement(45415),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_LOW_S2_TIME, new UnsignedWordElement(45416)), //
						m(GridMeterChannelId.GRID_VOLT_HIGH, new UnsignedWordElement(45417),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_LOW, new UnsignedWordElement(45418),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_FREQ_HIGH, new UnsignedWordElement(45419),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_LOW, new UnsignedWordElement(45420),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_RECOVER_TIME, new UnsignedWordElement(45421))), //

				new FC16WriteRegistersTask(45422,
						m(GridMeterChannelId.GRID_VOLT_RECOVER_HIGH, new UnsignedWordElement(45422),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_RECOVER_LOW, new UnsignedWordElement(45423),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_FREQ_RECOVER_HIGH, new UnsignedWordElement(45424),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_RECOVER_LOW, new UnsignedWordElement(45425),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_VOLT_RECOVER_TIME, new UnsignedWordElement(45426)), //
						m(GridMeterChannelId.GRID_FREQ_RECOVER_TIME, new UnsignedWordElement(45427)), //
						m(GridMeterChannelId.POWER_RATE_LIMIT_GENERATE, new UnsignedWordElement(45428),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.POWER_RATE_LIMIT_RECONNECT, new UnsignedWordElement(45429),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.POWER_RATE_LIMIT_REDUCTION, new UnsignedWordElement(45430),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_PROTECT, new UnsignedWordElement(45431)), //
						m(GridMeterChannelId.POWER_SLOPE_ENABLE, new UnsignedWordElement(45432))), //

				new FC3ReadRegistersTask(45422, Priority.LOW,
						m(GridMeterChannelId.GRID_VOLT_RECOVER_HIGH, new UnsignedWordElement(45422),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_VOLT_RECOVER_LOW, new UnsignedWordElement(45423),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(GridMeterChannelId.GRID_FREQ_RECOVER_HIGH, new UnsignedWordElement(45424),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_FREQ_RECOVER_LOW, new UnsignedWordElement(45425),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_VOLT_RECOVER_TIME, new UnsignedWordElement(45426)), //
						m(GridMeterChannelId.GRID_FREQ_RECOVER_TIME, new UnsignedWordElement(45427)), //
						m(GridMeterChannelId.POWER_RATE_LIMIT_GENERATE, new UnsignedWordElement(45428),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.POWER_RATE_LIMIT_RECONNECT, new UnsignedWordElement(45429),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.POWER_RATE_LIMIT_REDUCTION, new UnsignedWordElement(45430),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						m(GridMeterChannelId.GRID_PROTECT, new UnsignedWordElement(45431)), //
						m(GridMeterChannelId.POWER_SLOPE_ENABLE, new UnsignedWordElement(45432))), //

				new FC3ReadRegistersTask(35121, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(35121),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(35122),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35123, 35124),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(35125),
								ElementToChannelConverter.INVERT), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(35126),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(35127),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35128, 35129),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(35130),
								ElementToChannelConverter.INVERT), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(35131),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(35132),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(35133, 35134),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(35135), //
								ElementToChannelConverter.INVERT) //
				), //
				new FC3ReadRegistersTask(35140, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(35140),
								ElementToChannelConverter.INVERT), //
						new DummyRegisterElement(35141),
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedWordElement(35142))));
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

}
