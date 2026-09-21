package io.openems.edge.system.fenecon.masterbox2v0.meter;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.system.fenecon.masterbox2v0.MasterBox2v0;
import io.openems.edge.system.fenecon.masterbox2v0.utils.IocReadValueMapping;
import io.openems.edge.system.fenecon.masterbox2v0.utils.MasterBoxModbusComponent;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Fenecon.MasterBox2V0.Meter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
@GenerateTargetsFromReferences("ioc")
public class MasterBox2v0MeterImpl extends AbstractOpenemsComponent implements MasterBox2v0Meter, OpenemsComponent,
		EventHandler, ElectricityMeter, TimedataProvider, MasterBoxModbusComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(MasterBox2v0MeterImpl.class);

	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.ioc_id})(enabled=true))")
	private MasterBox2v0 ioc;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	public MasterBox2v0MeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				MasterBox2v0Meter.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);

		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Modified
	protected void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
	}

	@Override
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
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			try {
				this.setChannels();
			} catch (Exception e) {
				this.logWarn(this.log, "Cannot set the MasterBox Meter Channels");
			}
			this.calculateEnergy();
		}
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.MANAGED_CONSUMPTION_METERED;
	}

	@Override
	public List<IocReadValueMapping<?>> getReadValueMappings() {
		return List.of(//
				new IocReadValueMapping<>(this.ioc::getTimeStampEnergyMeter, MasterBox2v0Meter.ChannelId.TIME_STAMP), //
				new IocReadValueMapping<>(this.ioc::getStatusEnergyMeter, MasterBox2v0Meter.ChannelId.STATUS), //
				new IocReadValueMapping<>(this.ioc::getVoltageL1EnergyMeter, ElectricityMeter.ChannelId.VOLTAGE_L1), //
				new IocReadValueMapping<>(this.ioc::getVoltageL2EnergyMeter, ElectricityMeter.ChannelId.VOLTAGE_L2), //
				new IocReadValueMapping<>(this.ioc::getVoltageL3EnergyMeter, ElectricityMeter.ChannelId.VOLTAGE_L3), //
				new IocReadValueMapping<>(this.ioc::getCurrentL1EnergyMeter, ElectricityMeter.ChannelId.CURRENT_L1), //
				new IocReadValueMapping<>(this.ioc::getCurrentL2EnergyMeter, ElectricityMeter.ChannelId.CURRENT_L2), //
				new IocReadValueMapping<>(this.ioc::getCurrentL3EnergyMeter, ElectricityMeter.ChannelId.CURRENT_L3), //
				new IocReadValueMapping<>(this.ioc::getActivePowerL1EnergyMeter,
						ElectricityMeter.ChannelId.ACTIVE_POWER_L1), //
				new IocReadValueMapping<>(this.ioc::getActivePowerL2EnergyMeter,
						ElectricityMeter.ChannelId.ACTIVE_POWER_L2), //
				new IocReadValueMapping<>(this.ioc::getActivePowerL3EnergyMeter,
						ElectricityMeter.ChannelId.ACTIVE_POWER_L3) //
		);
	}

	@Override
	public boolean hasIoc() {
		return this.ioc != null;
	}

	private void calculateEnergy() {
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateEnergy.update(null);
		} else if (activePower > 0) {
			this.calculateEnergy.update(activePower);
		} else {
			this.calculateEnergy.update(0);
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(MasterBox2v0Meter.class, accessMode, 100) //
						.build() //
		);
	}
}
