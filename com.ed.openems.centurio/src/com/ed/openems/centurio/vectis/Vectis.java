package com.ed.openems.centurio.vectis;

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

import com.ed.data.VectisData;
import com.ed.openems.centurio.datasource.api.EdComData;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "EnergyDepot.Vectis", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "="
				+ EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class Vectis extends AbstractOpenemsComponent
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

	public Vectis() {
		VectisUtils.initializeChannels(this).forEach(channel -> this.addChannel(channel));

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

		VectisData vectis = this.datasource.getVectis();

		int reaL1 = Math.round(vectis.getReactivePower(0));
		int reaL2 = Math.round(vectis.getReactivePower(1));
		int reaL3 = Math.round(vectis.getReactivePower(2));
		
		this.getReactivePowerL1().setNextValue(reaL1);
		this.getReactivePowerL2().setNextValue(reaL2);
		this.getReactivePowerL3().setNextValue(reaL3);
		this.getReactivePower().setNextValue(reaL1 + reaL2 + reaL3);
		
		int acL1 = Math.round(vectis.getACPower(0));
		int acL2 = Math.round(vectis.getACPower(1));
		int acL3 = Math.round(vectis.getACPower(2));
		
		this.getActivePowerL1().setNextValue(acL1);
		this.getActivePowerL2().setNextValue(acL2);
		this.getActivePowerL3().setNextValue(acL3);
		this.getActivePower().setNextValue(acL1 + acL2 + acL3);

	}

	@Override
	public String debugLog() {

		return "Active Power: " + this.getActivePowerL1().value().toString()
				+ this.getActivePowerL2().value().toString() + this.getActivePowerL3().value().toString();

	}

	@Override
	public MeterType getMeterType() {

		return MeterType.GRID;

	}
}
