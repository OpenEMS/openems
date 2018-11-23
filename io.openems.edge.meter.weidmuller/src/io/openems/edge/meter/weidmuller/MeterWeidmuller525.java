package io.openems.edge.meter.weidmuller;

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

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Implements the Weidmuller 525 Energy Meter
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Weidmuller.525", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MeterWeidmuller525 extends AbstractOpenemsModbusComponent implements SymmetricMeter, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterWeidmuller525() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return null;
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
//		CURRENT_TRANSFORMER_L1_PRIMARY(new Doc().unit(Unit.AMPERE)), //
//		CURRENT_TRANSFORMER_L1_SECONDARY(new Doc().unit(Unit.AMPERE)), //
//		VOLTAGE_TRANSFORMER_L1_PRIMARY(new Doc().unit(Unit.VOLT)), //
//		VOLTAGE_TRANSFORMER_L1_SECONDARY(new Doc().unit(Unit.VOLT)), //
//
//		CURRENT_TRANSFORMER_L2_PRIMARY(new Doc().unit(Unit.AMPERE)), //
//		CURRENT_TRANSFORMER_L2_SECONDARY(new Doc().unit(Unit.AMPERE)), //
//		VOLTAGE_TRANSFORMER_L2_PRIMARY(new Doc().unit(Unit.VOLT)), //
//		VOLTAGE_TRANSFORMER_L2_SECONDARY(new Doc().unit(Unit.VOLT)), //
//
//		CURRENT_TRANSFORMER_L3_PRIMARY(new Doc().unit(Unit.AMPERE)), //
//		CURRENT_TRANSFORMER_L3_SECONDARY(new Doc().unit(Unit.AMPERE)), //
//		VOLTAGE_TRANSFORMER_L3_PRIMARY(new Doc().unit(Unit.VOLT)), //
//		VOLTAGE_TRANSFORMER_L3_SECONDARY(new Doc().unit(Unit.VOLT)), //
//		FREQUENCY_DETERMINATION(new Doc().unit(Unit.HERTZ)), //
//		CHANGE_OVERTIME(new Doc().unit(Unit.SECONDS)), //
//		AVERAGING_TIME_CURRENT(new Doc()), //
//		AVERAGING_TIME_POWER(new Doc()), //
//		AVERAGING_TIME_VOLT(new Doc()), //
//		RESPINSE_THRESHOLD_OF_CURRENT_MEASURING_I1_TO_I3(new Doc().unit(Unit.MILLIAMPERE)), //
//		RESULT_FROM_COMPARATOR_GROUP1(new Doc().unit(Unit.MILLIAMPERE)//
//				.option(1, "and")//
//				.option(0, "or")), //
//		COMPARATOR_1A_LIMIT_VALUE(new Doc()), //
//		COMPARATOR_1A_ADDRESS_OF_THE_MEASURED_VALUE(new Doc()), //
//		COMPARATOR_1A_MINIMUM_TURN_ON_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_1A_LEAD_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_1A_OPERATOR(new Doc()//
//				.option(0, ">=")//
//				.option(1, "<=")), //
//
//		COMPARATOR_1B_LIMIT_VALUE(new Doc()), //
//		COMPARATOR_1B_ADDRESS_OF_THE_MEASURED_VALUE(new Doc()), //
//		COMPARATOR_1B_MINIMUM_TURN_ON_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_1B_LEAD_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_1B_OPERATOR(new Doc()//
//				.option(0, ">=")//
//				.option(1, "<=")), //
//
//		COMPARATOR_1C_LIMIT_VALUE(new Doc()), //
//		COMPARATOR_1C_ADDRESS_OF_THE_MEASURED_VALUE(new Doc()), //
//		COMPARATOR_1C_MINIMUM_TURN_ON_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_1C_LEAD_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_1C_OPERATOR(new Doc()//
//				.option(0, ">=")//
//				.option(1, "<=")), //
//
//		COMPARATOR_2A_LIMIT_VALUE(new Doc()), //
//		COMPARATOR_2A_ADDRESS_OF_THE_MEASURED_VALUE(new Doc()), //
//		COMPARATOR_2A_MINIMUM_TURN_ON_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_2A_LEAD_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_2A_OPERATOR(new Doc()//
//				.option(0, ">=")//
//				.option(1, "<=")), //
//
//		COMPARATOR_2B_LIMIT_VALUE(new Doc()), //
//		COMPARATOR_2B_ADDRESS_OF_THE_MEASURED_VALUE(new Doc()), //
//		COMPARATOR_2B_MINIMUM_TURN_ON_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_2B_LEAD_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_2B_OPERATOR(new Doc()//
//				.option(0, ">=")//
//				.option(1, "<=")), //
//
//		COMPARATOR_2C_LIMIT_VALUE(new Doc()), //
//		COMPARATOR_2C_ADDRESS_OF_THE_MEASURED_VALUE(new Doc()), //
//		COMPARATOR_2C_MINIMUM_TURN_ON_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_2C_LEAD_TIME(new Doc().unit(Unit.SECONDS)), //
//		COMPARATOR_2C_OPERATOR(new Doc()//
//				.option(0, ">=")//
//				.option(1, "<=")), //
//
//		TERMINATE_ASSIGNMENT_CURRENT_L1(new Doc()), //
//		TERMINATE_ASSIGNMENT_CURRENT_L2(new Doc()), //
//		TERMINATE_ASSIGNMENT_CURRENT_L3(new Doc()), //
//		TERMINATE_ASSIGNMENT_VOLT_L1(new Doc()), //
//		TERMINATE_ASSIGNMENT_VOLT_L2(new Doc()), //
//		TERMINATE_ASSIGNMENT_VOLT_L3(new Doc()), //
//		CLEAR_MIN_AND_MAX_VALUES(new Doc()), //
//		CLEAR_ENERGY_METER(new Doc()), //
//		// Energy values, min and max values are written to the EEPROM in every 5 min
//		FORCE_WRITE_EEPROM(new Doc()), //
//		VOLTAGE_CoNNECTION_DIAGRAM(new Doc()), //
//		CURRENT_CONNECTION_DIAGRAM(new Doc()), //
//		// The voltage for THD and FFT can be shown on the display as L-N or L-L values.
//		// 0 = LN, 1 = LL
//		RELATIVE_VOLTAGE_FOR_THD_AND_FFT(new Doc()), //
//		METERING_RANGE_EXCEEDANCE(new Doc()), //
//		COMPARATOR_RESULT_1_OUTPUT_A(new Doc()), //
//		COMPARATOR_RESULT_1_OUTPUT_B(new Doc()), //
//		COMPARATOR_RESULT_1_OUTPUT_C(new Doc()), //
//		COMPARATOR_RESULT_2_OUTPUT_A(new Doc()), //
//		COMPARATOR_RESULT_2_OUTPUT_B(new Doc()), //
//		COMPARATOR_RESULT_2_OUTPUT_C(new Doc()), //
//		LINKAGE_RESULT_OF_COMPARATOR_GROUP_1(new Doc()), //
//		LINKAGE_RESULT_OF_COMPARATOR_GROUP_2(new Doc()), //
//		PERIOD_OF_TIME_AFTER_WHICH_THE_BACKLIGHT_WILL_SWITCH_TO_STANDBY(new Doc().unit(Unit.SECONDS)), //
//		BRIGHTNESS_OF_THE_STANDBY_BACKLIGHT(new Doc().unit(Unit.SECONDS)),//

		VOLTAGE_L1_N(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_L2_N(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_L3_N(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_L1_L2(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_L2_L3(new Doc().unit(Unit.VOLT)), //
		VOLTAGE_L1_L3(new Doc().unit(Unit.VOLT)), //
		CURRENT_L1(new Doc().unit(Unit.AMPERE)), //
		CURRENT_L2(new Doc().unit(Unit.AMPERE)), //
		CURRENT_L3(new Doc().unit(Unit.AMPERE)), //
		CURRENT_SUM(new Doc().unit(Unit.AMPERE)), //
		REAL_POWER_P1_L1N(new Doc().unit(Unit.WATT)), //
		REAL_POWER_P2_L2N(new Doc().unit(Unit.WATT)), //
		REAL_POWER_P3_L3N(new Doc().unit(Unit.WATT)), //
		REAL_POWER_SUM(new Doc().unit(Unit.WATT)), //
		APPARENT_POWER_S1_L1N(new Doc().unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_S2_L2N(new Doc().unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_S3_L3N(new Doc().unit(Unit.VOLT_AMPERE)), //
		APPARENT_POWER_SUM(new Doc().unit(Unit.VOLT_AMPERE)), //
		REACTIVE_POWER_Q1_L1N(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWER_Q2_L2N(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWER_Q3_L3N(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		REACTIVE_POWER_SUM(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE)), //
		COSPHI_L1(new Doc()), //
		COSPHI_L2(new Doc()), //
		COSPHI_L3(new Doc()), //
		MEASURED_FREQUENCY(new Doc().unit(Unit.HERTZ)), //
		ROTATION_FIELD(new Doc()//
				.option(1, "right")//
				.option(0, "none")//
				.option(-1, "left")), //
		REAL_ENERGY_L1(new Doc().unit(Unit.WATT_HOURS)), //
		REAL_ENERGY_L2(new Doc().unit(Unit.WATT_HOURS)), //
		REAL_ENERGY_L3(new Doc().unit(Unit.WATT_HOURS)), //
		REAL_ENERGY_L1_L3(new Doc().unit(Unit.WATT_HOURS)), //
		REAL_ENERGY_L1_CONSUMED(new Doc().unit(Unit.WATT_HOURS)), //
		REAL_ENERGY_L2_CONSUMED(new Doc().unit(Unit.WATT_HOURS)), //
		REAL_ENERGY_L3_CONSUMED(new Doc().unit(Unit.WATT_HOURS)), //
		REAL_ENERGY_L1_L3_CONSUMED_RATE_1(new Doc().unit(Unit.WATT_HOURS)), //
		REAL_ENERGY_L1_DELIVERED(new Doc().unit(Unit.WATT_HOURS)), //
		REAL_ENERGY_L2_DELIVERED(new Doc().unit(Unit.WATT_HOURS)), //
		REAL_ENERGY_L3_DELIVERED(new Doc().unit(Unit.WATT_HOURS)), //
		REAL_ENERGY_L1_L3_DELIVERED(new Doc().unit(Unit.WATT_HOURS)), //
		APPARENT_ENERGY_L1(new Doc().unit(Unit.VOLT_AMPERE_HOURS)), //
		APPARENT_ENERGY_L2(new Doc().unit(Unit.VOLT_AMPERE_HOURS)), //
		APPARENT_ENERGY_L3(new Doc().unit(Unit.VOLT_AMPERE_HOURS)), //
		APPARENT_ENERGY_L1_L3(new Doc().unit(Unit.VOLT_AMPERE_HOURS)), //
		REACTIVE_ENERGY_L1(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_ENERGY_L2(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_ENERGY_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_ENERGY_L1_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_ENERGY_INDUCTIVE_L1(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_ENERGY_INDUCTIVE_L2(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_ENERGY_INDUCTIVE_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_ENERGY_INDUCTIVE_L1_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_ENERGY_CAPACITIVE_L1(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_ENERGY_CAPACITIVE_L2(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_ENERGY_CAPACITIVE_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		REACTIVE_ENERGY_CAPACITIVE_L1_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
		HARMONIC_THD_VOLT_L1N(new Doc().unit(Unit.PERCENT)), //
		HARMONIC_THD_VOLT_L2N(new Doc().unit(Unit.PERCENT)), //
		HARMONIC_THD_VOLT_L3N(new Doc().unit(Unit.PERCENT)), //
		HARMONIC_THD_CURRENT_L1N(new Doc().unit(Unit.PERCENT)), //
		HARMONIC_THD_CURRENT_L2N(new Doc().unit(Unit.PERCENT)), //
		HARMONIC_THD_CURRENT_L3N(new Doc().unit(Unit.PERCENT)), //

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
	protected ModbusProtocol defineModbusProtocol() {
		ModbusProtocol protocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(102, Priority.HIGH, //
						m(MeterWeidmuller525.ChannelId.VOLTAGE_L1_N, new FloatDoublewordElement(19000)), //
						m(MeterWeidmuller525.ChannelId.VOLTAGE_L2_N, new FloatDoublewordElement(19002)), //
						m(MeterWeidmuller525.ChannelId.VOLTAGE_L3_N, new FloatDoublewordElement(19004)), //
						m(MeterWeidmuller525.ChannelId.VOLTAGE_L1_L2, new FloatDoublewordElement(19006)), //
						m(MeterWeidmuller525.ChannelId.VOLTAGE_L2_L3, new FloatDoublewordElement(19008)), //
						m(MeterWeidmuller525.ChannelId.VOLTAGE_L1_L3, new FloatDoublewordElement(19010)), //
						m(MeterWeidmuller525.ChannelId.CURRENT_L1, new FloatDoublewordElement(19012)), //
						m(MeterWeidmuller525.ChannelId.CURRENT_L2, new FloatDoublewordElement(19014)), //
						m(MeterWeidmuller525.ChannelId.CURRENT_L3, new FloatDoublewordElement(19016)), //
						m(MeterWeidmuller525.ChannelId.CURRENT_SUM, new FloatDoublewordElement(19018)), //
						m(MeterWeidmuller525.ChannelId.REAL_POWER_P1_L1N, new FloatDoublewordElement(19020)), //
						m(MeterWeidmuller525.ChannelId.REAL_POWER_P2_L2N, new FloatDoublewordElement(19022)), //
						m(MeterWeidmuller525.ChannelId.REAL_POWER_P3_L3N, new FloatDoublewordElement(19024)), //
						m(MeterWeidmuller525.ChannelId.REAL_POWER_SUM, new FloatDoublewordElement(19026)), //
						m(MeterWeidmuller525.ChannelId.APPARENT_POWER_S1_L1N, new FloatDoublewordElement(19028)), //
						m(MeterWeidmuller525.ChannelId.APPARENT_POWER_S2_L2N, new FloatDoublewordElement(19030)), //
						m(MeterWeidmuller525.ChannelId.APPARENT_POWER_S3_L3N, new FloatDoublewordElement(19032)), //
						m(MeterWeidmuller525.ChannelId.APPARENT_POWER_SUM, new FloatDoublewordElement(19034)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_POWER_Q1_L1N, new FloatDoublewordElement(19036)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_POWER_Q2_L2N, new FloatDoublewordElement(19038)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_POWER_Q3_L3N, new FloatDoublewordElement(19040)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_POWER_SUM, new FloatDoublewordElement(19042)), //
						m(MeterWeidmuller525.ChannelId.COSPHI_L1, new FloatDoublewordElement(19044)), //
						m(MeterWeidmuller525.ChannelId.COSPHI_L2, new FloatDoublewordElement(19046)), //
						m(MeterWeidmuller525.ChannelId.COSPHI_L3, new FloatDoublewordElement(19048)), //
						m(MeterWeidmuller525.ChannelId.MEASURED_FREQUENCY, new FloatDoublewordElement(19050)), //
						m(MeterWeidmuller525.ChannelId.ROTATION_FIELD, new FloatDoublewordElement(19052)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L1, new FloatDoublewordElement(19054)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L2, new FloatDoublewordElement(19056)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L3, new FloatDoublewordElement(19058)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L1_L3, new FloatDoublewordElement(19060)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L1_CONSUMED, new FloatDoublewordElement(19062)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L2_CONSUMED, new FloatDoublewordElement(19064)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L3_CONSUMED, new FloatDoublewordElement(19066)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L1_L3_CONSUMED_RATE_1,
								new FloatDoublewordElement(19068)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L1_DELIVERED, new FloatDoublewordElement(19070)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L2_DELIVERED, new FloatDoublewordElement(19072)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L3_DELIVERED, new FloatDoublewordElement(19074)), //
						m(MeterWeidmuller525.ChannelId.REAL_ENERGY_L1_L3_DELIVERED, new FloatDoublewordElement(19076)), //
						m(MeterWeidmuller525.ChannelId.APPARENT_ENERGY_L1, new FloatDoublewordElement(19078)), //
						m(MeterWeidmuller525.ChannelId.APPARENT_ENERGY_L2, new FloatDoublewordElement(19080)), //
						m(MeterWeidmuller525.ChannelId.APPARENT_ENERGY_L3, new FloatDoublewordElement(19082)), //
						m(MeterWeidmuller525.ChannelId.APPARENT_ENERGY_L1_L3, new FloatDoublewordElement(19084)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_L1, new FloatDoublewordElement(19086)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_L2, new FloatDoublewordElement(19088)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_L3, new FloatDoublewordElement(19090)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_L1_L3, new FloatDoublewordElement(19092)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_INDUCTIVE_L1, new FloatDoublewordElement(19094)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_INDUCTIVE_L2, new FloatDoublewordElement(19096)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_INDUCTIVE_L3, new FloatDoublewordElement(19098)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_INDUCTIVE_L1_L3,
								new FloatDoublewordElement(19100)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_CAPACITIVE_L1,
								new FloatDoublewordElement(19102)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_CAPACITIVE_L2,
								new FloatDoublewordElement(19104)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_CAPACITIVE_L3,
								new FloatDoublewordElement(19106)), //
						m(MeterWeidmuller525.ChannelId.REACTIVE_ENERGY_CAPACITIVE_L1_L3,
								new FloatDoublewordElement(19108)), //
						m(MeterWeidmuller525.ChannelId.HARMONIC_THD_VOLT_L1N, new FloatDoublewordElement(19110)), //
						m(MeterWeidmuller525.ChannelId.HARMONIC_THD_VOLT_L2N, new FloatDoublewordElement(19112)), //
						m(MeterWeidmuller525.ChannelId.HARMONIC_THD_VOLT_L3N, new FloatDoublewordElement(19114)), //
						m(MeterWeidmuller525.ChannelId.HARMONIC_THD_CURRENT_L1N, new FloatDoublewordElement(19116)), //
						m(MeterWeidmuller525.ChannelId.HARMONIC_THD_VOLT_L2N, new FloatDoublewordElement(19118)), //
						m(MeterWeidmuller525.ChannelId.HARMONIC_THD_CURRENT_L3N, new FloatDoublewordElement(19120))

				));

		return protocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

}
