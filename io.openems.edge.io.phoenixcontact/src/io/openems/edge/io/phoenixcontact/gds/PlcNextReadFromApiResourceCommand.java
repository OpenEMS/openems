package io.openems.edge.io.phoenixcontact.gds;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.event.EdgeEventConstants;

public class PlcNextReadFromApiResourceCommand implements PlcNextApiCommand {

	private static final Logger log = LoggerFactory.getLogger(PlcNextReadFromApiResourceCommand.class);

	private final PlcNextGdsProvider gdsProvider;
	private final String instanceName;
	private final Collection<Channel<?>> availableChannels;

	public PlcNextReadFromApiResourceCommand(PlcNextGdsProvider gdsProvider, String instanceName,
			Collection<Channel<?>> availableChannels) {
		this.gdsProvider = gdsProvider;
		this.instanceName = instanceName;
		this.availableChannels = availableChannels;
	}
	
	@Override
	public List<String> eventTriggers() {
		return List.of(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE);
	}
	
	@Override
	public void execute() {
		log.info("Reading GDS data from instance '" + instanceName + "'");
		gdsProvider.readFromApiToChannels(instanceName, availableChannels);
	}
}
