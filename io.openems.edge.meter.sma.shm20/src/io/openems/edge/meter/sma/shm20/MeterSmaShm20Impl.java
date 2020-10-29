package io.openems.edge.meter.sma.shm20;

import java.nio.ByteOrder;

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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.SMA.SHM20", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterSmaShm20Impl extends AbstractOpenemsModbusComponent
		implements MeterSmaShm20, AsymmetricMeter, SymmetricMeter, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterSmaShm20Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterSmaShm20.ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final int offset = 30865;
		return new ModbusProtocol(this, 
				new FC3ReadRegistersTask(30865, Priority.HIGH, 
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(30865))
				)); 
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

}
