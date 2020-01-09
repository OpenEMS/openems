package io.openems.edge.evcs.ocpp.abl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.ChangeConfigurationRequest;
import eu.chargetime.ocpp.model.core.DataTransferRequest;
import eu.chargetime.ocpp.model.remotetrigger.TriggerMessageRequest;
import eu.chargetime.ocpp.model.remotetrigger.TriggerMessageRequestType;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.ocpp.core.AbstractOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.core.OcppInformations;
import io.openems.edge.evcs.ocpp.core.OcppProfileType;
import io.openems.edge.evcs.ocpp.core.OcppRequests;
import io.openems.edge.evcs.ocpp.server.OcppServerImpl;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Evcs.Ocpp.Abl", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class Abl extends AbstractOcppEvcsComponent
		implements Evcs, MeasuringEvcs, ManagedEvcs, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(Abl.class);

	// Profiles that a ABL is supporting
	private static final OcppProfileType[] PROFILE_TYPES = { //
			OcppProfileType.CORE //
	};

	// Values that a ABL is supporting
	private static final HashSet<OcppInformations> MEASUREMENTS = new HashSet<OcppInformations>(
			Arrays.asList(OcppInformations.values()));

	private Config config;

	@Reference
	protected ComponentManager componentManager;

	public Abl() {
		super( //
				PROFILE_TYPES, //
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				AbstractOcppEvcsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), MeasuringEvcs.ChannelId.values() //
		);
	}

	@Activate
	public void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.getChargingType().setNextValue(ChargingType.AC);
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

	public int dynamicMaximumHardwarePower = 0;

	@Override
	public Integer getConfiguredMaximumHardwarePower() {
		String sessionId = this.getChargingSessionId().getNextValue().orElse("");
		if (sessionId.isEmpty()) {
			return this.config.maxHwPower();
		}
		DataTransferRequest request = new DataTransferRequest();
		request.setVendorId("ABL");
		request.setMessageId("GetLimit");
		request.setData(this.config.limitId());

		UUID sessionUUID = UUID.fromString(sessionId);

		try {
			this.getConfiguredOcppServer().send(sessionUUID, request).whenComplete((confirmation, throwable) -> {

				dynamicMaximumHardwarePower = Integer.valueOf(confirmation.toString());
				this.logInfo(log, confirmation.toString());
			});
			return dynamicMaximumHardwarePower;
		} catch (OccurenceConstraintException e) {
			e.printStackTrace();
		} catch (UnsupportedFeatureException e) {
			e.printStackTrace();
		} catch (NotConnectedException e) {
			e.printStackTrace();
		}
		return this.config.maxHwPower();
	}

	@Override
	public Integer getConfiguredMinimumHardwarePower() {
		return this.config.minHwPower();
	}

	@Override
	public OcppServerImpl getConfiguredOcppServer() {
		try {
			return this.componentManager.getComponent(this.config.ocppServerId());
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}

	@Override
	public OcppRequests getSupportedRequests() {
		AbstractOcppEvcsComponent evcs = this;

		return new OcppRequests() {

			@Override
			public Request setChargePowerLimit(String chargePower) {
				DataTransferRequest request = new DataTransferRequest();

				int phases = evcs.getPhases().getNextValue().orElse(3);
				int target = Math.round(Integer.valueOf(chargePower) / phases / 230) /* voltage */ ;

				int maxCurrent = evcs.getMaximumHardwarePower().getNextValue().orElse(0) / phases / 230;
				target = target > maxCurrent ? maxCurrent : target;

				request.setVendorId("ABL");
				request.setMessageId("SetOutletLimit");
				request.setData("logicalid=" + config.logicalId() + ";value=" + target);
				return request;
			}
		};
	}

	@Override
	public List<Request> getRequiredRequestsAfterConnection() {
		List<Request> requests = new ArrayList<Request>();

		ChangeConfigurationRequest setMeterValueSampleInterval = new ChangeConfigurationRequest();
		setMeterValueSampleInterval.setKey("MeterValueSampleInterval");
		setMeterValueSampleInterval.setValue("5");
		requests.add(setMeterValueSampleInterval);

		ChangeConfigurationRequest setMeterValueSampledData = new ChangeConfigurationRequest();
		setMeterValueSampledData.setKey("MeterValuesSampledData");
		setMeterValueSampledData.setValue("POWER_ACTIVE_IMPORT");
		requests.add(setMeterValueSampledData);

		return requests;
	}

	@Override
	public List<Request> getRequiredRequestsDuringConnection() {
		List<Request> requests = new ArrayList<Request>();

		TriggerMessageRequest request = new TriggerMessageRequest();
		request.setConnectorId(this.getConfiguredConnectorId());
		request.setRequestedMessage(TriggerMessageRequestType.MeterValues);
		requests.add(request);

		return requests;
	}
}
