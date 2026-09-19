package io.openems.edge.meter.virtual.subtract;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.sum.SumOptions;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Virtual.Subtract", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
@GenerateTargetsFromReferences({ "minuend", "subtrahends" })
public class VirtualSubtractMeterImpl extends AbstractOpenemsComponent
		implements VirtualSubtractMeter, ElectricityMeter, OpenemsComponent, ModbusSlave, SumOptions {

	private final SubtractChannelManager channelManager = new SubtractChannelManager(this);

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = OPTIONAL, //
			target = "(&(id=${config.minuend_id})(enabled=true))")
	private OpenemsComponent minuend;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MULTIPLE, //
			target = "(&(id=${config.subtrahends_ids})(enabled=true))")
	private List<OpenemsComponent> subtrahends;

	private Config config;

	public VirtualSubtractMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		this.channelManager.activate(this.minuend, this.subtrahends);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.channelManager.deactivate();

		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public boolean addToSum() {
		return this.config.addToSum();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTableWithoutIndividualPhases(accessMode));
	}

}
