package io.openems.edge.ess.mr.gridcon.meter;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
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
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Implements a meter using values from a gridcon
 */
@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Meter.Gridcon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE } //
)
public class MeterGridcon extends AbstractOpenemsComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent, ModbusSlave, EventHandler {

	private MeterType meterType = MeterType.GRID;

	@Reference
	protected ConfigurationAdmin cm;
	
	@Reference
	ComponentManager componentManager;
	
	String gridconId; 
	

	public MeterGridcon() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
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
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			fillChannels();
			break;
		}
	}
	
	private void fillChannels() {
		try {
			GridconPcs gridconPcs = componentManager.getComponent(gridconId);
			
			if (gridconPcs == null) {
				return;
			}
						
			
			_setCurrentL1((int) (gridconPcs.getCurrentL1() * 1000.0));
			_setCurrentL2((int) (gridconPcs.getCurrentL2() * 1000.0));
			_setCurrentL3((int) (gridconPcs.getCurrentL3() * 1000.0));
			_setCurrent((int) (gridconPcs.getCurrentLN() * 1000.0));

			_setActivePowerL1((int) (gridconPcs.getActivePowerL1()));
			_setActivePowerL2((int) (gridconPcs.getActivePowerL2()));
			_setActivePowerL3((int) (gridconPcs.getActivePowerL3()));
			_setActivePower((int) (gridconPcs.getActivePowerSum()));
			
			_setReactivePowerL1((int) (gridconPcs.getReactivePowerL1()));
			_setReactivePowerL2((int) (gridconPcs.getReactivePowerL2()));
			_setReactivePowerL3((int) (gridconPcs.getReactivePowerL3()));
			_setReactivePower((int) (gridconPcs.getReactivePowerSum()));

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
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);

	}
}
