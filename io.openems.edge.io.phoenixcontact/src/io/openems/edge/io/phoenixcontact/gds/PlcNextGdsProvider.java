package io.openems.edge.io.phoenixcontact.gds;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.io.phoenixcontact.PlcNextDataClient;
import io.openems.edge.io.phoenixcontact.utils.PlcNextJsonElementHelper;
import io.openems.edge.meter.api.ElectricityMeter;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextGdsProvider.class)
public class PlcNextGdsProvider {
	
	private static Logger log = LoggerFactory.getLogger(PlcNextGdsProvider.class);

	private static final Map<PlcNextGdsDataAspect, ChannelId> CHANNEL_MAPPING_READ = Map.of(
			PlcNextGdsDataAspect.READ_TEST_VALUE, ElectricityMeter.ChannelId.ACTIVE_POWER);
	
//	private static final Map<ChannelId, PlcNextGdsDataAspect> CHANNEL_MAPPING_WRITE = Map.of(
//			ElectricityMeter.ChannelId.VOLTAGE, PlcNextGdsDataAspect.WRITE_TEST_VALUE);

	private final PlcNextDataClient dataClient;
	
	@Activate
	public PlcNextGdsProvider(@Reference(scope = ReferenceScope.BUNDLE) PlcNextDataClient dataClient) {
		this.dataClient = dataClient;
	}
	
//	@Reference( referenceInterface = MyService.class, policy = ReferencePolicy.DYNAMIC, 
//			cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, bind = “bindMyService”, unbind = “unbindMyService”) 
//	private Map<String, MyService> myServiceMap = new HashMap<String, MyService>;
//	protected void bindMyService(MyService myService, Map properties) { 
//		if (properties.containsKey(“type”)) { 
//			String type = properties.get(“type”).toString(); 
//			myServiceMap.put(type, myService); 
//		}
//	}
//	protected void unbindMyService(MyService myService) { 
//		myServiceMap.values().remove(myService); 
//	} 	

	/**
	 * Fetch data and fill corresponding channels
	 * 
	 * @param instanceName name of the instance to pickup the data for
	 */
	public void readFromApiToChannels(String instanceName, Collection<Channel<?>> availableChannels) {
		log.debug("Building channelID to channel mapping");
		Map<ChannelId, Channel<?>> mappedChannels = availableChannels.parallelStream()
			.filter(channel -> CHANNEL_MAPPING_READ.values().contains(channel.channelId()))
			.collect(Collectors.toMap(Channel::channelId, Function.identity()));
		
		log.info("Fetching GDS data for instance '" + instanceName + "'");
		JsonObject apiResponseBody = dataClient.fetchAllGdsDataAspects(instanceName);

		log.info("Processing data and filling channels of instance '" + instanceName + "'");
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
