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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.io.phoenixcontact.PlcNextDataClient;
import io.openems.edge.io.phoenixcontact.PlcNextDevice;
import io.openems.edge.io.phoenixcontact.utils.PlcNextJsonElementHelper;
import io.openems.edge.meter.api.ElectricityMeter;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextGdsProvider.class)
public class PlcNextGdsProvider {
	
	private static final Map<PlcNextGdsDataAspect, ChannelId> CHANNEL_MAPPING_READ = Map.of(
			PlcNextGdsDataAspect.READ_TEST_VALUE, ElectricityMeter.ChannelId.ACTIVE_POWER);
	
//	private static final Map<ChannelId, PlcNextGdsDataAspect> CHANNEL_MAPPING_WRITE = Map.of(
//			ElectricityMeter.ChannelId.VOLTAGE, PlcNextGdsDataAspect.WRITE_TEST_VALUE);

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
	public void readFromApiToChannels(String namespace) {
		Map<ChannelId, Channel<?>> mappedChannels = this.deviceComponent.channels().parallelStream()
			.filter(channel -> CHANNEL_MAPPING_READ.values().contains(channel.channelId()))
			.collect(Collectors.toMap(Channel::channelId, Function.identity()));
		
		JsonObject apiResponseBody = dataClient.fetchAllGdsDataAspects(namespace);
		CHANNEL_MAPPING_READ.keySet().forEach(mappedDataAspect -> {
			ChannelId dataAspectChannelId = CHANNEL_MAPPING_READ.get(mappedDataAspect);
			Channel<?> dataAspectChannel = mappedChannels.get(dataAspectChannelId);
			JsonElement responseElement = apiResponseBody.get(mappedDataAspect.getIdentifier());
			Object jsonValue = PlcNextJsonElementHelper.getJsonValue(responseElement, dataAspectChannel.getType());
			
			dataAspectChannel.setNextValue(jsonValue);
		});		
	}
	
//	public void writeToApiFromChannels() {
//		// TODO: implement me!
//	}
	
}
