package io.openems.edge.io.phoenixcontact.gds;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

@Component(scope = ServiceScope.SINGLETON, service = PlcNextGdsProvider.class)
public class PlcNextGdsProvider {
	
	private static Logger log = LoggerFactory.getLogger(PlcNextGdsProvider.class);

	private final PlcNextDataClient dataClient;
	
	@Activate
	public PlcNextGdsProvider(@Reference(scope = ReferenceScope.BUNDLE) PlcNextDataClient dataClient) {
		this.dataClient = dataClient;
	}
	
	/**
	 * Fetch data and fill corresponding channels
	 * 
	 * @param instanceName name of the instance to pickup the data for
	 */
	public void readFromApiToChannels(String instanceName, Collection<Channel<?>> availableChannels) {
		log.debug("Building channelID to channel mapping for readable aspects");
		List<PlcNextGdsDataAspect> gdsDataReadableAspects = Stream.of(PlcNextGdsDataAspect.values())
				.filter(item -> item.getType() == PlcNextGdsDataAspectType.READ).toList();
		List<ChannelId> gdsDataRedableAspectChannelIds = gdsDataReadableAspects.stream() //
				.map(item -> item.getChannelId()) //
				.toList();
		Map<ChannelId, Channel<?>> mappedChannels = availableChannels.parallelStream() //
				.filter(channel -> gdsDataRedableAspectChannelIds.contains(channel.channelId())) //
				.collect(Collectors.toMap(Channel::channelId, Function.identity()));
		
		log.info("Fetching GDS data for instance '" + instanceName + "'");
		JsonObject apiResponseBody = dataClient.fetchAllGdsDataAspects(instanceName);

		log.info("Processing data and filling channels of instance '" + instanceName + "'");
		gdsDataReadableAspects.forEach(mappedDataAspect -> {
			ChannelId dataAspectChannelId = mappedDataAspect.getChannelId();
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
