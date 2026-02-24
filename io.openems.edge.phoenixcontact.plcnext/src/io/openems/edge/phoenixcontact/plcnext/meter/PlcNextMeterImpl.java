package io.openems.edge.phoenixcontact.plcnext.meter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextAuthConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataAccessConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappedValue;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappingException;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapper;
import io.openems.edge.phoenixcontact.plcnext.common.utils.PlcNextMappingDefinitionHelper;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PLCnext.Meter.Device", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class PlcNextMeterImpl extends AbstractOpenemsComponent
		implements PlcNextMeter, ElectricityMeter, OpenemsComponent, EventHandler {

	private static final Logger log = LoggerFactory.getLogger(PlcNextMeterImpl.class);

	private static final JsonObject DEFAULT_RESPONSE = JsonUtils.buildJsonObject()//
			.add("variables", JsonUtils.buildJsonArray().build()).build();

	@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private PlcNextGdsDataProvider gdsDataProvider;
	@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private PlcNextGdsDataToChannelMapper gdsDataToChannelMapper;

	private Config config;
	private PlcNextAuthConfig authConfig;
	private PlcNextGdsDataAccessConfig gdsDataAccessConfig;

	private PlcNextGdsDataMappingDefinition[] readDataMappingDefinition;

	public PlcNextMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				PlcNextMeter.ChannelId.values() //
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

		if (!config.namespaceVariables().isBlank()) {
			this.readDataMappingDefinition = PlcNextMappingDefinitionHelper.joinMappings(null,
					PlcNextMeterGdsDataReadMappingDefinition.values(), config.namespaceVariables());
		} else {
			this.readDataMappingDefinition = PlcNextMeterGdsDataReadMappingDefinition.values();
		}
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
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
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
		log.info("StationID '{}': Reading METER data from URL '{}", gdsDataAccessConfig.stationId(), 
				gdsDataAccessConfig.dataUrl());
		List<String> variableIdentifiers = Stream.of(this.readDataMappingDefinition)//
				.map(PlcNextGdsDataMappingDefinition::getIdentifier).toList();

		gdsDataProvider
				.readDataFromRestApi(variableIdentifiers, gdsDataAccessConfig, authConfig) //
				.thenApply(apiResponseBody -> {
					if (Objects.isNull(apiResponseBody)) {
						apiResponseBody = DEFAULT_RESPONSE;
					}
					try {
						log.info("StationID '{}': Mapping METER data", this.gdsDataAccessConfig.stationId());
						List<PlcNextGdsDataMappedValue> mappedValues = gdsDataToChannelMapper.mapAllValuesToChannels(
								apiResponseBody.getAsJsonArray(PlcNextGdsDataProvider.PLC_NEXT_VARIABLES),
								gdsDataAccessConfig.dataInstanceName(), gdsDataAccessConfig.stationId(), this.readDataMappingDefinition);
						
						if (!mappedValues.isEmpty()) {
							log.info("StationID '{}': Pushing METER data to channels", this.gdsDataAccessConfig.stationId());
							setNextValuesToChannels(mappedValues);
						}
					} catch (PlcNextGdsDataMappingException e) {
						log.error("StationID '{}': Mapping error!", this.gdsDataAccessConfig.stationId(), e);
					}
					return null;
				});
	}

	/**
	 * Writes value fetched from PLCnext GDS to device channel
	 * 
	 * @param mappedValues represent value objects containing the channel ID and 
	 * 						the value to set to channel
	 * @param device      represents the device holding the channels
	 */
	void setNextValuesToChannels(List<PlcNextGdsDataMappedValue> mappedValues) {
		for (PlcNextGdsDataMappedValue mappedValue : mappedValues) {
			log.debug("StationID '{}': Providing value '{}' to channel named '{}'", this.gdsDataAccessConfig.stationId(),
					mappedValue.getValue(), mappedValue.getChannelId());
			channel(mappedValue.getChannelId()).setNextValue(mappedValue.getValue());
		}
	}
}
