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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.collect.ImmutableMap;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.FilteredSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.pvinverter.AbstractSunSpecPvInverter;
import io.openems.edge.bridge.modbus.sunspec.pvinverter.SunSpecPvInverter;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

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
public class SolarEdgePvInverterImpl extends AbstractSunSpecPvInverter
		implements SolarEdgePvInverter, SunSpecPvInverter, ManagedSymmetricPvInverter, ElectricityMeter,
		ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave {

	private static final SunSpecModel S_101_WITHOUT_EVENTS =
			FilteredSunSpecModel.withoutPoints(
					DefaultSunSpecModel.S_101,
					DefaultSunSpecModel.S101.EVT1,
					DefaultSunSpecModel.S101.EVT2,
					DefaultSunSpecModel.S101.EVT_VND1,
					DefaultSunSpecModel.S101.EVT_VND2,
					DefaultSunSpecModel.S101.EVT_VND3,
					DefaultSunSpecModel.S101.EVT_VND4
			);

	private static final SunSpecModel S_102_WITHOUT_EVENTS =
			FilteredSunSpecModel.withoutPoints(
					DefaultSunSpecModel.S_102,
					DefaultSunSpecModel.S102.EVT1,
					DefaultSunSpecModel.S102.EVT2,
					DefaultSunSpecModel.S102.EVT_VND1,
					DefaultSunSpecModel.S102.EVT_VND2,
					DefaultSunSpecModel.S102.EVT_VND3,
					DefaultSunSpecModel.S102.EVT_VND4
			);

	private static final SunSpecModel S_103_WITHOUT_EVENTS =
			FilteredSunSpecModel.withoutPoints(
					DefaultSunSpecModel.S_103,
					DefaultSunSpecModel.S103.EVT1,
					DefaultSunSpecModel.S103.EVT2,
					DefaultSunSpecModel.S103.EVT_VND1,
					DefaultSunSpecModel.S103.EVT_VND2,
					DefaultSunSpecModel.S103.EVT_VND3,
					DefaultSunSpecModel.S103.EVT_VND4
			);

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(S_101_WITHOUT_EVENTS, Priority.LOW) //
			.put(S_102_WITHOUT_EVENTS, Priority.LOW) //
			.put(S_103_WITHOUT_EVENTS, Priority.LOW) //
			.build();

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public SolarEdgePvInverterImpl() {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				SunSpecPvInverter.ChannelId.values(), //
				SolarEdgePvInverter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.readOnly(),
				config.modbusUnitId(), this.cm, "Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK, config.phase())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SolarEdgePvInverterImpl.class, accessMode, 100) //
						.build());
	}

}
