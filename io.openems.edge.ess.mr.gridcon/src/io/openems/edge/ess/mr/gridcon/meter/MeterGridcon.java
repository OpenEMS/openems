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
			
			_setVoltageL1((int) (gridconPcs.getVoltageU1U2() * 1000.0));
			_setVoltageL2((int) (gridconPcs.getVoltageU2U3() * 1000.0));
			_setVoltageL3((int) (gridconPcs.getVoltageU3U1() * 1000.0));
			
			_setCurrentL1((int) (gridconPcs.getCurrentIL1() * 1000.0));
			_setCurrentL2((int) (gridconPcs.getCurrentIL2() * 1000.0));
			_setCurrentL3((int) (gridconPcs.getCurrentIL3() * 1000.0));

			_setActivePower((int) (gridconPcs.getPowerP() * 1000.0));
			_setReactivePower((int) (gridconPcs.getPowerQ() * 1000.0));			

			_setFrequency((int) (gridconPcs.getFrequency() * 1000.0));			
			
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
