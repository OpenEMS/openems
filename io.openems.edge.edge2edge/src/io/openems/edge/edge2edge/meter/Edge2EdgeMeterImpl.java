package io.openems.edge.edge2edge.meter;

import java.util.function.Consumer;

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

import com.google.common.collect.Lists;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusRecord;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.edge2edge.common.AbstractEdge2Edge;
import io.openems.edge.edge2edge.common.Edge2Edge;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
	name = "Edge2Edge.Meter", //
	immediate = true, //
	configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Edge2EdgeMeterImpl extends AbstractEdge2Edge
	implements AsymmetricMeter, SymmetricMeter, Edge2Edge, ModbusComponent, OpenemsComponent {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
	super.setModbus(modbus);
    }

    private Config config;

    public Edge2EdgeMeterImpl() throws OpenemsException {
	super(//
		Lists.newArrayList(//
			OpenemsComponent::getModbusSlaveNatureTable, //
			SymmetricMeter::getModbusSlaveNatureTable, //
			AsymmetricMeter::getModbusSlaveNatureTable //
		), //
		OpenemsComponent.ChannelId.values(), //
		ModbusComponent.ChannelId.values(), //
		Edge2Edge.ChannelId.values(), //
		SymmetricMeter.ChannelId.values(), //
		AsymmetricMeter.ChannelId.values() //
	);
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsException {
	this.config = config;
	if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
		"Modbus", config.modbus_id(), config.remoteComponentId(), AccessMode.READ_ONLY)) {
	    return;
	}
    }

    @Deactivate
    protected void deactivate() {
	super.deactivate();
    }

    @Override
    protected Consumer<Object> getOnUpdateCallback(ModbusSlaveNatureTable modbusSlaveNatureTable, ModbusRecord record) {
	return null;
    }

    @Override
    protected io.openems.edge.common.channel.ChannelId getWriteChannelId(ModbusSlaveNatureTable modbusSlaveNatureTable,
	    ModbusRecord record) {
	return null;
    }

    @Override
    public String debugLog() {
	return "L:" + this.getActivePower().asString();
    }

    @Override
    public MeterType getMeterType() {
	return this.config.type();
    }

}
