package io.openems.edge.meter.virtual.symmetric.add;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.calculate.CalculateAverage;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.calculate.CalculateLongSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.api.VirtualMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Meter.Virtual.Symmetric.Add", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE//
) //
public class VirtualAdd extends AbstractOpenemsComponent
		implements VirtualMeter, SymmetricMeter, OpenemsComponent, EventHandler, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(VirtualAdd.class);

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	private volatile List<SymmetricMeter> symmetricMeter = new CopyOnWriteArrayList<>();

	public VirtualAdd() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.meterType = config.type();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.calculateChannelValues();
		}
	}

	private void calculateChannelValues() {
		// Find all configured SymmetricMeters
		List<SymmetricMeter> meters = new ArrayList<SymmetricMeter>();
		try {
			for (String meterId : this.config.meterIds()) {
				SymmetricMeter mts = this.componentManager.getComponent(meterId);
				meters.add(mts);
			}
		} catch (Exception e) {
			this.logError(this.log, e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		final CalculateAverage meterFrequency = new CalculateAverage();
		final CalculateIntegerSum meterMinActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum meterMaxActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum meterActivePower = new CalculateIntegerSum();
		final CalculateIntegerSum meterReactivePower = new CalculateIntegerSum();
		final CalculateLongSum meterActiveProductionEnergy = new CalculateLongSum();
		final CalculateLongSum meterActiveConsumptionEnergy = new CalculateLongSum();
		final CalculateAverage meterVoltage = new CalculateAverage();
		final CalculateIntegerSum meterCurrent = new CalculateIntegerSum();

		for (SymmetricMeter meter : meters) {
			meterFrequency.addValue(meter.getFrequency());
			meterMinActivePower.addValue(meter.getMinActivePower());
			meterMaxActivePower.addValue(meter.getMaxActivePower());
			meterActivePower.addValue(meter.getActivePower());
			meterReactivePower.addValue(meter.getReactivePower());
			meterActiveConsumptionEnergy.addValue(getActiveConsumptionEnergy());
			meterActiveProductionEnergy.addValue(meter.getActiveProductionEnergy());
			meterVoltage.addValue(meter.getVoltage());
			meterCurrent.addValue(meter.getCurrent());
		}

		this.getFrequency().setNextValue(meterFrequency.calculate());
		this.getMinActivePower().setNextValue(meterMinActivePower.calculate());
		this.getMaxActivePower().setNextValue(meterMaxActivePower.calculate());
		this.getActivePower().setNextValue(meterActivePower.calculate());
		this.getReactivePower().setNextValue(meterReactivePower.calculate());
		this.getActiveConsumptionEnergy().setNextValue(meterActiveConsumptionEnergy.calculate());
		this.getActiveProductionEnergy().setNextValue(meterActiveProductionEnergy.calculate());
		this.getVoltage().setNextValue(meterVoltage.calculate());
		this.getCurrent().setNextValue(meterCurrent.calculate());
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

	@Override
	public boolean addToSum() {
		return this.config.addToSum();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {		
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
