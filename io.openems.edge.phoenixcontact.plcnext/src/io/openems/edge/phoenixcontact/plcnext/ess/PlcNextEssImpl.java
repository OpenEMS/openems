/**
 * 
 */
package io.openems.edge.phoenixcontact.plcnext.ess;

import java.util.List;
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.phoenixcontact.plcnext.common.auth.PlcNextAuthConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataAccessConfig;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataProvider;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextChannelToGdsDataMapper;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappedValue;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappingException;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataToChannelMapper;

/**
 * 
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PLCnext.ESS.Device", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class PlcNextEssImpl extends AbstractOpenemsComponent
		implements PlcNextEss, ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	private static final Logger log = LoggerFactory.getLogger(PlcNextEssImpl.class);

	private static final JsonObject defaultResponse = JsonUtils.buildJsonObject()//
			.add("variables", JsonUtils.buildJsonArray().build()).build();

	@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private PlcNextGdsDataProvider gdsDataProvider;
	@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private PlcNextGdsDataToChannelMapper gdsDataToChannelMapper;
	@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED)
	private PlcNextChannelToGdsDataMapper gdsChannelToGdsDataMapper;
	@Reference
	private Power power;

	private Config config;
	private PlcNextAuthConfig authConfig;
	private PlcNextGdsDataAccessConfig gdsDataAccessConfig;

	private final PlcNextGdsDataMappingDefinition[] readDataMappingDefinition;

	public PlcNextEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				PlcNextEss.ChannelId.values() //
		);
		readDataMappingDefinition = PlcNextEssGdsDataReadMappingDefinition.values();
		// NOTE: Adding meter channels doesn't work, because of channel duplication on
		// ACTIVE_POWER!
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
		gdsDataProvider.deactivateSessionMaintenance();

		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "AP:" + this.getActivePower().asString();
	}

	@Override
	public void handleEvent(Event event) {
		log.debug("Handling event '" + event.getTopic() + "'");
		if (!this.isEnabled()) {
			log.warn("StationID '{}': Module deactivated, skipping event processing of event",
					this.gdsDataAccessConfig.stationId());
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.processDataOnBeforeProcessImageEvent();
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> this.processDataOnExecuteWriteEvent();
		}
	}

	/**
	 * Triggers fetching and mapping data from PLCnext REST-API including push to
	 * channels
	 */
	void processDataOnBeforeProcessImageEvent() {
		log.info("StationID '{}': Reading ESS data from URL '{}'", gdsDataAccessConfig.stationId(),
				gdsDataAccessConfig.dataUrl());
		List<String> variableIdentifiers = Stream.of(readDataMappingDefinition)//
				.map(PlcNextGdsDataMappingDefinition::getIdentifier).toList();
		JsonObject apiResponseBody = gdsDataProvider
				.readDataFromRestApi(variableIdentifiers, gdsDataAccessConfig, authConfig).orElse(defaultResponse);

		try {
			log.info("StationID '{}': Mapping ESS data", this.gdsDataAccessConfig.stationId());
			List<PlcNextGdsDataMappedValue> mappedValues = gdsDataToChannelMapper.mapAllValuesToChannels(
					apiResponseBody.getAsJsonArray(PlcNextGdsDataProvider.PLC_NEXT_VARIABLES),
					config.dataInstanceName(), readDataMappingDefinition);

			if (!mappedValues.isEmpty()) {
				log.info("StationID '{}': Pushing ESS data to channels", this.gdsDataAccessConfig.stationId());
				setNextValuesToChannels(mappedValues);
			}
		} catch (PlcNextGdsDataMappingException e) {
			log.error("StationID '{}': Mapping error!", this.gdsDataAccessConfig.stationId(), e);
		}
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
		}
	}

	/**
	 * Triggers mapping and writing data from channels to PLCnext REST-API
	 */
	void processDataOnExecuteWriteEvent() {
		log.info("StationID '{}': Reading ESS data from channels", gdsDataAccessConfig.stationId());
		List<PlcNextGdsDataMappedValue> valuesInChannelsToWrite = Stream
				.of(PlcNextEssGdsDataWriteMappingDefinition.values())
				.map(PlcNextEssGdsDataWriteMappingDefinition::getChannelId).map(this::readNextValueFromChannel)
				.toList();

		try {
			log.info("StationID '{}': Mapping ESS data", this.gdsDataAccessConfig.stationId());
			List<JsonElement> mappedData = gdsChannelToGdsDataMapper.mapAllValuesToGdsData(valuesInChannelsToWrite,
					this.gdsDataAccessConfig.dataInstanceName(), PlcNextEssGdsDataWriteMappingDefinition.values());

			log.info("StationID '{}': Pushing ESS data to URL '{}'", gdsDataAccessConfig.stationId(),
					gdsDataAccessConfig.dataUrl());
			gdsDataProvider.writeDataToRestApi(mappedData, gdsDataAccessConfig, authConfig);
		} catch (PlcNextGdsDataMappingException e) {
			log.error("StationID '{}': Mapping error!", this.gdsDataAccessConfig.stationId(), e);
		}
	}

	/**
	 * Fetches next value from given channel ID to prepare it to be written to PLCnext device.
	 * The 'next value' needs to be taken to reflect changes of the business logic.
	 * 
	 * @param channelId	represents the channel ID of channel to be read
	 * @return	mapping object containing the channel ID and the next value
	 */
	PlcNextGdsDataMappedValue readNextValueFromChannel(io.openems.edge.common.channel.ChannelId channelId) {
		log.debug("StationID '{}': Reading value from channel named '{}'", this.gdsDataAccessConfig.stationId(),
				channelId);
		Object channelValue = null;
		
		if (OpenemsType.INTEGER == channelId.doc().getType()) {
			channelValue = ((IntegerWriteChannel)channel(channelId)).getNextWriteValue() //
					.orElse(null);			
		}
		return new PlcNextGdsDataMappedValue(channelId, channelValue);
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		log.info("StationID '{}': applyPower({}, {}) called.", this.config.id(), activePower, reactivePower);

		IntegerWriteChannel setActivePowerChannel = this.channel(PlcNextEss.ChannelId.SET_ACTIVE_POWER);
		IntegerWriteChannel setReactivePowerChannel = this.channel(PlcNextEss.ChannelId.SET_REACTIVE_POWER);

		setActivePowerChannel.setNextWriteValue(activePower);
		setReactivePowerChannel.setNextWriteValue(reactivePower);
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

}
