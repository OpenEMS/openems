package io.openems.edge.kostal.gridmeter.sunspec;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;
//

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S201;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S202;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S203;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S204;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.sunspec.AbstractSunSpecMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(
		//
		name = "Grid-Meter.Kostal.KSEM.Inverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID", //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class KostalGridMeterImpl extends AbstractSunSpecMeter
		implements
			KostalGridMeter,
			ElectricityMeter,
			ModbusComponent,
			OpenemsComponent,
			EventHandler,
			TimedataProvider {

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap
			.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_120, Priority.HIGH) //
			.put(DefaultSunSpecModel.S_203, Priority.HIGH) //
			//.put(DefaultSunSpecModel.S_213, Priority.HIGH) //
			.build();

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

//	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(
//			this, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
//	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(
//			this, ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);
	
	public KostalGridMeterImpl() {
		super(
				//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				KostalGridMeter.ChannelId.values() //
		);
	}

	@Override
	protected void onSunSpecInitializationCompleted() {
		//super.onSunSpecInitializationCompleted();

		// override inverted values from sunspec model because this is a meter
		this.mapFirstPointToChannel(
				//
				ElectricityMeter.ChannelId.ACTIVE_POWER, //
				DIRECT_1_TO_1, //
				S204.W, S203.W, S202.W, S201.W);
		this.mapFirstPointToChannel(
				//
				ElectricityMeter.ChannelId.REACTIVE_POWER, //
				DIRECT_1_TO_1, //
				S204.VAR, S203.VAR, S202.VAR, S201.VAR);
		this.mapFirstPointToChannel(
				//
				ElectricityMeter.ChannelId.ACTIVE_POWER_L1, //
				DIRECT_1_TO_1, //
				S204.WPH_A, S203.WPH_A, S202.WPH_A, S201.WPH_A);
		this.mapFirstPointToChannel(
				//
				ElectricityMeter.ChannelId.ACTIVE_POWER_L2, //
				DIRECT_1_TO_1, //
				S204.WPH_B, S203.WPH_B, S202.WPH_B, S201.WPH_B);
		this.mapFirstPointToChannel(
				//
				ElectricityMeter.ChannelId.ACTIVE_POWER_L3, //
				DIRECT_1_TO_1, //
				S204.WPH_C, S203.WPH_C, S202.WPH_C, S201.WPH_C);
		this.mapFirstPointToChannel(
				//
				ElectricityMeter.ChannelId.REACTIVE_POWER_L1, //
				DIRECT_1_TO_1, //
				S204.V_A_RPH_A, S203.V_A_RPH_A, S202.V_A_RPH_A, S201.V_A_RPH_A);
		this.mapFirstPointToChannel(
				//
				ElectricityMeter.ChannelId.REACTIVE_POWER_L2, //
				DIRECT_1_TO_1, //
				S204.V_A_RPH_B, S203.V_A_RPH_B, S202.V_A_RPH_B, S201.V_A_RPH_B);
		this.mapFirstPointToChannel(
				//
				ElectricityMeter.ChannelId.REACTIVE_POWER_L3, //
				DIRECT_1_TO_1, //
				S204.V_A_RPH_C, S203.V_A_RPH_C, S202.V_A_RPH_C, S201.V_A_RPH_C);
		this.mapFirstPointToChannel(
				//TODO or calculated?
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
				SCALE_FACTOR_3, //
				S204.TOT_WH_EXP, S203.TOT_WH_EXP, S202.TOT_WH_EXP,
				S201.TOT_WH_EXP);
		this.mapFirstPointToChannel(
				//TODO or calculated?
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
				SCALE_FACTOR_3,
				S204.TOT_WH_IMP, S203.TOT_WH_IMP, S202.TOT_WH_IMP,
				S201.TOT_WH_IMP);
		
//		this.mapFirstPointToChannel(//
//				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, //
//				DIRECT_1_TO_1, //
//				S204.TOT_WH_EXP, S203.TOT_WH_EXP, S202.TOT_WH_EXP, S201.TOT_WH_EXP);
//		this.mapFirstPointToChannel(//
//				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
//				DIRECT_1_TO_1, //
//				S204.TOT_WH_IMP, S203.TOT_WH_IMP, S202.TOT_WH_IMP, S201.TOT_WH_IMP);
		
	}

	@Activate
	private void activate(ComponentContext context, Config config)
			throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(),
				config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}
		this.config = config;
	}

//	/**
//	 * Calculate the Energy values from ActivePower.
//	 */
//	private void calculateEnergy() {
//		// Calculate Energy
//		var activePower = this.getActivePower().get();
//		if (activePower == null) {
//			// Not available
//			this.calculateProductionEnergy.update(null);
//			this.calculateConsumptionEnergy.update(null);
//		} else if (activePower > 0) {
//			this.calculateProductionEnergy.update(activePower);
//			this.calculateConsumptionEnergy.update(0);
//		} else {
//			this.calculateProductionEnergy.update(0);
//			this.calculateConsumptionEnergy.update(activePower * -1);
//		}
//	}
	
	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE :
				//TODO clarify usage - modbus register or calculated
				//this.calculateEnergy();
				break;
		}
	}
	
	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}
}
