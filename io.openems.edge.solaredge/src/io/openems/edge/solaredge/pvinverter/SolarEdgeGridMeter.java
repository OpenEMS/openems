package io.openems.edge.solaredge.pvinverter;

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

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.sunspec.AbstractSunSpecMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarEdge.PV-Inverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class SolarEdgeGridMeter extends AbstractSunSpecMeter
		implements AsymmetricMeter, SymmetricMeter, ModbusComponent, OpenemsComponent {

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
<<<<<<< Updated upstream:io.openems.edge.solaredge/src/io/openems/edge/solaredge/pvinverter/SolarEdgeGridMeter.java
			.put(DefaultSunSpecModel.S_201, Priority.LOW) //
			.put(DefaultSunSpecModel.S_202, Priority.LOW) //
			.put(DefaultSunSpecModel.S_203, Priority.LOW) //
			.put(DefaultSunSpecModel.S_204, Priority.LOW) //
=======
//			.put(DefaultSunSpecModel.S_101, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_102, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_111, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_112, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_113, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_120, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_121, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_122, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_123, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_124, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_125, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_127, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_128, Priority.LOW) //
//			.put(DefaultSunSpecModel.S_145, Priority.LOW) //		
//			.put(DefaultSunSpecModel.S_802, Priority.LOW) //			
>>>>>>> Stashed changes:io.openems.edge.solaredge/src/io/openems/edge/solaredge/ess/SolarEdgeEss.java
			.build();

	private static final int READ_FROM_MODBUS_BLOCK = 2;

	private Config config;

	public SolarEdgeGridMeter() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values() //
		);
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
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
	public MeterType getMeterType() {
		return this.config.type();
	}
}
