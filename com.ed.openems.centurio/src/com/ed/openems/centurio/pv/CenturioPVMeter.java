package com.ed.openems.centurio.pv;

import java.net.UnknownHostException;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import com.ed.data.InverterData;
import com.ed.openems.centurio.CenturioConstants;
import com.ed.openems.centurio.datasource.api.EdComData;
import com.ed.openems.centurio.ess.CenturioEss;

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "KACO.bpPVMeter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class CenturioPVMeter extends AbstractOpenemsComponent
		implements SymmetricMeter, SymmetricPvInverter, OpenemsComponent, EventHandler {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected EdComData datasource;

	@Reference
	protected ConfigurationAdmin cm;

	@Activate
	void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		// update filter for 'datasource'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "Datasource", config.datasource_id())) {
			return;
		}
		this.getMaxActivePower().setNextValue(config.maxP());
		this.getMinActivePower().setNextValue(0);
		this.getActivePowerLimit().setNextValue(config.maxP());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		
		
		PV_VOLTAGE0((new Doc().unit(Unit.VOLT))), //
		PV_VOLTAGE1((new Doc().unit(Unit.VOLT))); //
		
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public CenturioPVMeter() {
		MeterUtils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannels();
			break;
		}
	}

	private void updateChannels() {
		Integer activePower = null;
		float pvVoltage0 = 0;
		float pvVoltage1 = 0;
		
		if (this.datasource.isConnected()) {
			InverterData inverter = this.datasource.getInverterData();

			activePower = CenturioConstants.roundToPowerPrecision(inverter.getPvPower());
			pvVoltage0 = inverter.getPvVoltage(0);
			pvVoltage1 = inverter.getPvVoltage(1);
		}

		this.getActivePower().setNextValue(activePower);
		this.channel(CenturioPVMeter.ChannelId.PV_VOLTAGE0).setNextValue(pvVoltage0);
		this.channel(CenturioPVMeter.ChannelId.PV_VOLTAGE1).setNextValue(pvVoltage1);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}
	
	
	

}