package io.openems.edge.io.phoenixcontact.gds;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonObject;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.io.phoenixcontact.PlcNextDataClient;
import io.openems.edge.io.phoenixcontact.PlcNextDevice;
import io.openems.edge.meter.api.ElectricityMeter;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextGdsProvider.class)
public class PlcNextGdsProvider {
	
	private static final Map<PlcNextGdsDataAspect, ChannelId> CHANNEL_MAPPING_READ = Map.of(
			PlcNextGdsDataAspect.READ_TEST_VALUE, ElectricityMeter.ChannelId.ACTIVE_POWER);
	
	private static final Map<ChannelId, PlcNextGdsDataAspect> CHANNEL_MAPPING_WRITE = Map.of(
			ElectricityMeter.ChannelId.VOLTAGE, PlcNextGdsDataAspect.WRITE_TEST_VALUE);

	private final PlcNextDataClient dataClient;
	
	private PlcNextDevice deviceComponent;

	@Activate
	public PlcNextGdsProvider(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) PlcNextDataClient dataClient) {
		this.dataClient = dataClient;
	}
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	public void setPlcNextDeviceComponent(PlcNextDevice deviceComponent) {
		this.deviceComponent = deviceComponent;
	}

	// TODO: just a try of generic implementation
	public void readFromApiToChannels() {
		Map<ChannelId, Channel<?>> mappedChannels = this.deviceComponent.channels().stream()
			.filter(channel -> CHANNEL_MAPPING_READ.values().contains(channel.channelId()))
			.collect(Collectors.toMap(Channel::channelId, Function.identity()));
		
		JsonObject apiResponseBody = dataClient.fetchAllGdsDataAspects("test");
		CHANNEL_MAPPING_READ.keySet().forEach(mappedDataAspect -> {
			ChannelId dataAspectChannelId = CHANNEL_MAPPING_READ.get(mappedDataAspect);
			Channel<?> dataAspectChannel = mappedChannels.get(dataAspectChannelId);
			
			if (dataAspectChannel.getType() == OpenemsType.BOOLEAN) {
				dataAspectChannel.setNextValue(
						apiResponseBody.get(mappedDataAspect.getIdentifier()).getAsBoolean());
			} else if (dataAspectChannel.getType() == OpenemsType.DOUBLE) {
					dataAspectChannel.setNextValue(
							apiResponseBody.get(mappedDataAspect.getIdentifier()).getAsDouble());
			} else if (dataAspectChannel.getType() == OpenemsType.FLOAT) {
				dataAspectChannel.setNextValue(
						apiResponseBody.get(mappedDataAspect.getIdentifier()).getAsFloat());
			} else if (dataAspectChannel.getType() == OpenemsType.INTEGER) {
				dataAspectChannel.setNextValue(
						apiResponseBody.get(mappedDataAspect.getIdentifier()).getAsInt());
			} else if (dataAspectChannel.getType() == OpenemsType.LONG) {
				dataAspectChannel.setNextValue(
						apiResponseBody.get(mappedDataAspect.getIdentifier()).getAsLong());
			} else if (dataAspectChannel.getType() == OpenemsType.SHORT) {
				dataAspectChannel.setNextValue(
						apiResponseBody.get(mappedDataAspect.getIdentifier()).getAsShort());
			} else {
				dataAspectChannel.setNextValue(
						apiResponseBody.get(mappedDataAspect.getIdentifier()).getAsString());
				
			}
		});		
	}
	
	public void writeToApiFromChannels() {
		// TODO: implement me!
	}
	
}
