package io.openems.edge.meter.virtual.subtract;

import java.util.List;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.VirtualMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Virtual.Subtract", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
public class VirtualSubtractMeterImpl extends AbstractOpenemsComponent
		implements VirtualSubtractMeter, VirtualMeter, ElectricityMeter, OpenemsComponent, ModbusSlave {

	private final SubtractChannelManager channelManager = new SubtractChannelManager(this);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private OpenemsComponent minuend;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private List<OpenemsComponent> subtrahends;

	private Config config;

	public VirtualSubtractMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		// update filter for 'minuend'
		if (config.minuend_id() == null || config.minuend_id().isBlank()) {
			// assume zero values for minuend and set reference filter to something
			// unresolvable
			if (OpenemsComponent.updateReferenceFilterRaw(this.cm, this.servicePid(), "minuend", "(false)")) {
				return;
			}
		} else {
			// use given minuend meter or ess
			if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "minuend", config.minuend_id())) {
				return;
			}
		}

		// update filter for 'subtrahends'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "subtrahends",
				config.subtrahends_ids())) {
			return;
		}

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
