package io.openems.edge.victron.battery;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery.Victron", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE) //
public class VictronCANBusBatteryImpl extends AbstractOpenemsModbusComponent implements Battery, VictronBattery,
		ModbusComponent, OpenemsComponent {

	@Reference
	protected ConfigurationAdmin cm;

	protected Config config;

	public VictronCANBusBatteryImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				VictronBattery.ChannelId.values() //
		);
	}
	
	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), DEFAULT_UNIT_ID, this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.installListener();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}


	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, 
				new FC3ReadRegistersTask(259, Priority.LOW, 
						m(Battery.ChannelId.VOLTAGE, new UnsignedWordElement(259), ElementToChannelConverter.SCALE_FACTOR_MINUS_2),
						new DummyRegisterElement(260),
						m(Battery.ChannelId.CURRENT, new SignedWordElement(261), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(262, 265),
						m(Battery.ChannelId.SOC, new UnsignedWordElement(266), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(267, 304),
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new UnsignedWordElement(305), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new UnsignedWordElement(306), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedWordElement(307), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedWordElement(308), ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						m(Battery.ChannelId.CAPACITY, new UnsignedWordElement(309), new ElementToChannelConverter(value -> {
							Integer intValue = TypeUtils.getAsType(OpenemsType.INTEGER, value);
							return intValue * 10/getSoc().get() * VictronBattery.VOLTAGE;
						})))
				);
	}
	
	@Override
	public StartStop getStartStop() {
		return StartStop.START;
	}

}
