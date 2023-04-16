package io.openems.edge.ess.mr.gridcon.meter;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

/**
 * Implements a meter using values from a gridcon.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Gridcon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class MeterGridcon extends AbstractOpenemsComponent
		implements ElectricityMeter, OpenemsComponent, ModbusSlave, EventHandler {

	private MeterType meterType = MeterType.GRID;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	ComponentManager componentManager;

	String gridconId;

	public MeterGridcon() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.gridconId = config.gridcon_id();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public void handleEvent(Event event) {
		if (!isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.fillChannels();
			break;
		}
	}

	private void fillChannels() {
		try {
			GridconPcs gridconPcs = this.componentManager.getComponent(this.gridconId);

			if (gridconPcs == null) {
				return;
			}

			_setCurrentL1((int) (gridconPcs.getCurrentL1Grid() * 1000.0));
			_setCurrentL2((int) (gridconPcs.getCurrentL2Grid() * 1000.0));
			_setCurrentL3((int) (gridconPcs.getCurrentL3Grid() * 1000.0));
			_setCurrent((int) (gridconPcs.getCurrentLNGrid() * 1000.0)); // TODO correct?! ;)

			_setActivePowerL1((int) (gridconPcs.getActivePowerL1Grid()));
			_setActivePowerL2((int) (gridconPcs.getActivePowerL2Grid()));
			_setActivePowerL3((int) (gridconPcs.getActivePowerL3Grid()));
			_setActivePower((int) (gridconPcs.getActivePowerSumGrid()));

			_setReactivePowerL1((int) (gridconPcs.getReactivePowerL1Grid()));
			_setReactivePowerL2((int) (gridconPcs.getReactivePowerL2Grid()));
			_setReactivePowerL3((int) (gridconPcs.getReactivePowerL3Grid()));
			_setReactivePower((int) (gridconPcs.getReactivePowerSumGrid()));

		} catch (OpenemsNamedException e) {
			System.out.println("Error while reading meter values from gridcon!\n" + e.getMessage());
		}

	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);

	}
}
