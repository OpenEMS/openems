package io.openems.edge.evcs.ocpp.keywatt.singleccs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.ocpp.api.AbstractOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.api.OcppInformations;
import io.openems.edge.evcs.ocpp.api.OcppProfileType;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Evcs.Ocpp.IesKeywattSingle", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class IesKeywattSingleCCS extends AbstractOcppEvcsComponent implements Evcs, MeasuringEvcs, OpenemsComponent{

	// Profiles that a Ies KeyWatt is supporting
	private static final OcppProfileType[] PROFILE_TYPES = { //
			OcppProfileType.CORE //
	};

	// Values that a Ies KeyWatt is supporting
	private static final HashSet<OcppInformations> MEASUREMENTS = new HashSet<OcppInformations>(
			Arrays.asList(OcppInformations.values()));

	private Config config;

	public IesKeywattSingleCCS() {
		super( //
				PROFILE_TYPES, //
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				MeasuringEvcs.ChannelId.values() //
		);
	}

	@Activate
	public void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	public Set<OcppInformations> getSupportedMeasurements() {
		return MEASUREMENTS;
	}

	@Override
	public String getConfiguredOcppId() {
		return this.config.ocpp_id();
	}

	@Override
	public Integer getConfiguredConnectorId() {
		return this.config.connectorId();
	}

	@Override
	public Integer getConfiguredMaximumHardwarePower() {
		return this.config.maxHwPower();
	}

	@Override
	public Integer getConfiguredMinimumHardwarePower() {
		return this.config.minHwPower();
	}
}
