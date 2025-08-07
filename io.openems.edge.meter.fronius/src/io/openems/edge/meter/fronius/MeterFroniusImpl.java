package io.openems.edge.meter.fronius;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

import java.util.Map;

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
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S213;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.meter.AbstractSunSpecMeter;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Fronius", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})

public class MeterFroniusImpl extends AbstractSunSpecMeter
		implements MeterFronius, ElectricityMeter, ModbusComponent, OpenemsComponent {

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_213, Priority.HIGH) //
			.build();

	private final Logger log = LoggerFactory.getLogger(MeterFroniusImpl.class);

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;

	public MeterFroniusImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterFronius.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
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
	protected void onSunSpecInitializationCompleted() {
		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.FREQUENCY, //
				SCALE_FACTOR_3, //
				S213.HZ);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_POWER, //
				DIRECT_1_TO_1, //
				S213.W);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.REACTIVE_POWER, //
				DIRECT_1_TO_1, //
				S213.VAR);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
				DIRECT_1_TO_1, //
				S213.TOT_WH_EXP);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
				DIRECT_1_TO_1, //
				S213.TOT_WH_IMP);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.VOLTAGE, //
				SCALE_FACTOR_3, //
				S213.PH_V);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.CURRENT, //
				SCALE_FACTOR_3, //
				S213.A);

		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_POWER_L1, //
				DIRECT_1_TO_1, //
				S213.WPH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_POWER_L2, //
				DIRECT_1_TO_1, //
				S213.WPH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_POWER_L3, //
				DIRECT_1_TO_1, //
				S213.WPH_C);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.CURRENT_L1, //
				SCALE_FACTOR_3, //
				S213.APH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.CURRENT_L2, //
				SCALE_FACTOR_3, //
				S213.APH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.CURRENT_L3, //
				SCALE_FACTOR_3, //
				S213.APH_C);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.REACTIVE_POWER_L1, //
				DIRECT_1_TO_1, //
				S213.V_A_RPH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.REACTIVE_POWER_L2, //
				DIRECT_1_TO_1, //
				S213.V_A_RPH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.REACTIVE_POWER_L3, //
				DIRECT_1_TO_1, //
				S213.V_A_RPH_C);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.VOLTAGE_L1, //
				SCALE_FACTOR_3, //
				S213.PH_VPH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.VOLTAGE_L2, //
				SCALE_FACTOR_3, //
				S213.PH_VPH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.VOLTAGE_L3, //
				SCALE_FACTOR_3, //
				S213.PH_VPH_C);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, //
				DIRECT_1_TO_1, //
				S213.TOT_WH_IMP_PH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, //
				DIRECT_1_TO_1, //
				S213.TOT_WH_IMP_PH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, //
				DIRECT_1_TO_1, //
				S213.TOT_WH_IMP_PH_C);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, //
				DIRECT_1_TO_1, //
				S213.TOT_WH_EXP_PH_A);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, //
				DIRECT_1_TO_1, //
				S213.TOT_WH_EXP_PH_B);
		this.mapFirstPointToChannel(//
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, //
				DIRECT_1_TO_1, //
				S213.TOT_WH_EXP_PH_C);
	}

}
