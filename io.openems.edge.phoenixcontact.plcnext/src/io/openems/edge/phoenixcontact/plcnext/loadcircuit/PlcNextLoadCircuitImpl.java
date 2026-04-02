package io.openems.edge.phoenixcontact.plcnext.loadcircuit;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextAuthConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataAccessConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappedValue;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappingException;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapper;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PLCnext.LoadCircuit.Device", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class PlcNextLoadCircuitImpl extends AbstractOpenemsComponent
		implements PlcNextLoadCircuit, OpenemsComponent, EventHandler {

	private static final Logger log = LoggerFactory.getLogger(PlcNextLoadCircuitImpl.class);

	private static final JsonObject DEFAULT_RESPONSE = JsonUtils.buildJsonObject()//
			.add("variables", JsonUtils.buildJsonArray().build()).build();

	@Reference
	private ConfigurationAdmin configAdmin;
	@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private PlcNextGdsDataProvider gdsDataProvider;
	@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private PlcNextGdsDataToChannelMapper gdsDataToChannelMapper;

	
	private Config config;
	private PlcNextAuthConfig authConfig;
	private PlcNextGdsDataAccessConfig gdsDataAccessConfig;

	public PlcNextLoadCircuitImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				PlcNextLoadCircuit.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		log.info("StationID '{}': Activating component", config.id());
		super.activate(context, config.id(), config.alias(), config.enabled());
		applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsException {
		log.info("StationID '{}': Modifing component ", config.id());
		super.modified(context, config.id(), config.alias(), config.enabled());
		applyConfig(config);
	}

	private void applyConfig(Config config) {
		log.info("StationID '{}': Applying config", config.id());
		this.config = config;
		this.authConfig = new PlcNextAuthConfig(config.baseUrl(), config.username(), config.password());
		this.gdsDataAccessConfig = new PlcNextGdsDataAccessConfig(config.baseUrl(), config.dataInstanceName(),
				config.id());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		log.info("StationID '{}': Deactivating component", config.id());
		gdsDataProvider.deactivateSessionMaintenance(gdsDataAccessConfig);

		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "MaxActPow:" + this.getMaxActivePowerExport().asString() + ";" + //
				this.getMaxActivePowerImport().asString() + "|" +
				"MaxReactPow: " + this.getMaxReactivePower().asString();
	}

	@Override
	public void handleEvent(Event event) {
		log.debug("Handling event '" + event.getTopic() + "'");
		if (!this.isEnabled()) {
			log.warn("StationID '{}': Module deactivated, skipping event processing of event",
					this.gdsDataAccessConfig.stationId());
			return;
		}
		if (EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE == event.getTopic()) {
			processDataOnBeforeProcessImageEvent();
		}
	}

	/**
	 * Triggers fetching and mapping data and pushing to channels
	 */
	void processDataOnBeforeProcessImageEvent() {
		log.info("StationID '{}': Reading LOAD CIRCUIT data from URL '{}'", gdsDataAccessConfig.stationId(), 
				gdsDataAccessConfig.dataUrl());
		List<String> variableIdentifiers = Stream.of(PlcNextLoadCircuitGdsDataReadMappingDefinition.values())//
				.map(PlcNextGdsDataMappingDefinition::getIdentifier) //
				.distinct() //
 				.toList();
		gdsDataProvider
				.readDataFromRestApi(variableIdentifiers, gdsDataAccessConfig, authConfig) //
				.thenApply(apiResponseBody -> {
					if (Objects.isNull(apiResponseBody)) {
						apiResponseBody = DEFAULT_RESPONSE;
					}
					try {
						log.info("StationID '{}': Mapping LOAD CIRCUIT data", this.gdsDataAccessConfig.stationId());
						List<PlcNextGdsDataMappedValue> mappedValues = gdsDataToChannelMapper.mapAllValuesToChannels(
								apiResponseBody.getAsJsonArray(PlcNextGdsDataProvider.PLC_NEXT_VARIABLES),
								gdsDataAccessConfig.dataInstanceName(), gdsDataAccessConfig.stationId(), 
								PlcNextLoadCircuitGdsDataReadMappingDefinition.values());
						
						if (!mappedValues.isEmpty()) {
							log.info("StationID '{}': Pushing LOAD CIRCUIT data to channels", this.gdsDataAccessConfig.stationId());
							setNextValuesToChannels(mappedValues);
						}
					} catch (PlcNextGdsDataMappingException e) {
						log.error("StationID '{}': Mapping error!", this.gdsDataAccessConfig.stationId(), e);
					}
					return null;
				});
	}

	/**
	 * Writes values fetched from PLCnext GDS to device channels
	 * 
	 * @param mappedValues represent value objects containing the channel ID and 
	 * 						the value to set to channel
	 */
	void setNextValuesToChannels(List<PlcNextGdsDataMappedValue> mappedValues) {
		for (PlcNextGdsDataMappedValue mappedValue : mappedValues) {
			log.debug("StationID '{}': Providing value '{}' to channel named '{}'", this.gdsDataAccessConfig.stationId(),
					mappedValue.getValue(), mappedValue.getChannelId());
			channel(mappedValue.getChannelId()).setNextValue(mappedValue.getValue());
			log.info("StationID '{}': Next value provided to channel named '{}' is: {}", this.gdsDataAccessConfig.stationId(),
					channel(mappedValue.getChannelId()).getNextValue(), mappedValue.getChannelId());
		}
	}
}
