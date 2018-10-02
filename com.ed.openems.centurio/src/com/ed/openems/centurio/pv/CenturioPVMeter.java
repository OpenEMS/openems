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
import com.ed.openems.centurio.datasource.api.EdComData;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "EnergyDepot.CenturioPVMeter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "="
				+ EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class CenturioPVMeter extends AbstractOpenemsComponent
		implements SymmetricMeter, OpenemsComponent, EventHandler {

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

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
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
		if(this.datasource.isConnected()) {
			InverterData inverter = this.datasource.getInverterData();
			this.getActivePower().setNextValue(Math.round(inverter.getPvPower() / 10) * 10);
		}else {
			this.getActivePower().setNextValue(0);
		}
		

	}

	@Override
	public String debugLog() {

		return "PV Power: " + this.getActivePower().value().toString();

	}

	@Override
	public MeterType getMeterType() {

		return MeterType.PRODUCTION;

	}

}