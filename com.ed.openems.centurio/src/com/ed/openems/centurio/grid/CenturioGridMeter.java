package com.ed.openems.centurio.grid;

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
import io.openems.edge.meter.api.AsymmetricMeter;

import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "EnergyDepot.CenturioMeter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "="
				+ EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class CenturioGridMeter extends AbstractOpenemsComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent, EventHandler {

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
		
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public CenturioGridMeter() {
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

		InverterData inverter = this.datasource.getInverterData();
		
		this.getReactivePowerL1().setNextValue(inverter.getReactivPower(0));
		this.getReactivePowerL2().setNextValue(inverter.getReactivPower(1));
		this.getReactivePowerL3().setNextValue(inverter.getReactivPower(2));
		this.getReactivePower().setNextValue(inverter.getReactivPower(0) + inverter.getReactivPower(1) + inverter.getReactivPower(2));
		
		
		this.getActivePowerL1().setNextValue(inverter.getAcPower(0));
		this.getActivePowerL2().setNextValue(inverter.getAcPower(1));
		this.getActivePowerL3().setNextValue(inverter.getAcPower(2));
		this.getActivePower().setNextValue(inverter.getAcPower(0) + inverter.getAcPower(1) +inverter.getAcPower(2));

	}

	@Override
	public String debugLog() {

		return "Active Power: " + this.getActivePowerL1().value().toString();

	}

	@Override
	public MeterType getMeterType() {

		return MeterType.GRID;

	}

}
