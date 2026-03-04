package io.openems.edge.solaredge.charger;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.solaredge.ess.SolarEdgeEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarEdge.ESS.Charger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class SolarEdgeChargerImpl extends AbstractOpenemsComponent
		implements SolarEdgeCharger, EssDcCharger, OpenemsComponent, EventHandler, TimedataProvider, ModbusSlave {
		
	private SolarEdgeListener voltageListener;
		
	@Reference
	private ConfigurationAdmin cm;
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SolarEdgeEss essInverter;	
	
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;	
	
	public SolarEdgeChargerImpl() throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				SolarEdgeCharger.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		
		this.voltageListener = new SolarEdgeListener(this, this.essInverter, SolarEdgeEss.ChannelId.VOLTAGE_DC,
				EssDcCharger.ChannelId.VOLTAGE);	
		
		this.essInverter.addCharger(this);
		
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "essInverter",
				config.essInverter_id())) {
			return;
		}	
	}
	
	@Override
	@Deactivate
	protected void deactivate() {
		this.essInverter.removeCharger(this);
		this.voltageListener.deactivate();
		super.deactivate();
	}
	
	
	@Override
	public String debugLog() {
		return "L:" + this.getActualPower().asString();
	}	

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}	
		
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			this.updateActualEnergy();
		}
		}
	}
	
	/**
	 * Update the Energy values using data from SolarEdgeEssChannel.
	 */
	private void updateActualEnergy() {
		this._setActualEnergy(this.essInverter.getActiveProductionEnergy().get());
	}
	
	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				EssDcCharger.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SolarEdgeCharger.class, accessMode, 100) //
						.build());
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
	
	private static class SolarEdgeListener implements Consumer<Value<Integer>> {

		private final IntegerReadChannel solarEdgeChannel;
		private final IntegerReadChannel mirrorChannel;

		public SolarEdgeListener(SolarEdgeChargerImpl parent, SolarEdgeEss essInverter,
				io.openems.edge.common.channel.ChannelId solarEdgeChannel, io.openems.edge.common.channel.ChannelId mirrorChannel) {
			this.solarEdgeChannel = essInverter.channel(solarEdgeChannel);
			this.mirrorChannel = parent.channel(mirrorChannel);
			this.solarEdgeChannel.onSetNextValue(this);
		}

		public void deactivate() {
			this.solarEdgeChannel.removeOnSetNextValueCallback(this);
		}

		@Override
		public void accept(Value<Integer> t) {
			this.mirrorChannel.setNextValue(t);
		}
	}

}
