package io.openems.edge.goodwe.genset;

import static io.openems.common.utils.IntUtils.maxInt;
import static io.openems.common.utils.IntUtils.minInt;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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

import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.Genset", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID_GENSET" //
		})
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
@GenerateTargetsFromReferences("Modbus")
public class GoodWeStsBoxGensetMeterImpl extends AbstractOpenemsModbusComponent
		implements OpenemsComponent, GoodWeStsBoxGensetMeter, ElectricityMeter, EventHandler, TimedataProvider {

	@Reference
	private Sum sum;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	public GoodWeStsBoxGensetMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				GoodWeStsBoxGensetMeter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this,
				new FC3ReadRegistersTask(35169, Priority.HIGH,
						m(GoodWeStsBoxGensetMeter.ChannelId.BACKUP_POWER, new SignedDoublewordElement(35169))),
				new FC3ReadRegistersTask(36197, Priority.HIGH,
						m(GoodWeStsBoxGensetMeter.ChannelId.GENSET_OPERATING_MODE, new UnsignedWordElement(36197))));
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID_GENSET;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			final var activePower = this.getGensetOperatingMode().isDefined() && this.getGensetOperatingMode().get() //
					? calculatePower(this.sum, this.getBackupPower().get()) //
					: 0; /* not in Genset Operating Mode */
			setValue(this, ElectricityMeter.ChannelId.ACTIVE_POWER, activePower);
			this.calculateEnergy();
			break;
		}
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

	protected static int calculatePower(Sum sum, Long backupPower) {
		var ess = minInt(0, sum.getEssDischargePowerChannel().value().get());
		var production = maxInt(0, sum.getProductionActivePowerChannel().value().get());
		var backup = backupPower != null ? backupPower : 0;
		var genPower = Math.max(0, backup - ess - production);
		return (int) genPower;
	}
}
