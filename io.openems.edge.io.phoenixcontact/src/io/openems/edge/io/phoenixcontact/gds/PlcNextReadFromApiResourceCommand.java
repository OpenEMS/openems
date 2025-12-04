package io.openems.edge.io.phoenixcontact.gds;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.event.EdgeEventConstants;

public class PlcNextReadFromApiResourceCommand implements PlcNextApiCommand {

	private static final Logger log = LoggerFactory.getLogger(PlcNextReadFromApiResourceCommand.class);

	private final PlcNextGdsProvider gdsProvider;

	private PlcNextGdsDataClientConfig gdsDataClientConfig;

	public PlcNextReadFromApiResourceCommand(PlcNextGdsProvider gdsProvider, PlcNextGdsDataClientConfig config) {
		this.gdsProvider = gdsProvider;
		this.gdsDataClientConfig = config;
	}
	
	@Override
	public List<String> eventTriggers() {
		return List.of(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE);
	}
	
	@Override
	public void setDataClientConfig(PlcNextGdsDataClientConfig config) {
		this.gdsDataClientConfig = config;

	}

	@Override
	public void execute() {
		log.info("Reading GDS data from instance '" + gdsDataClientConfig + "'");
		gdsProvider.readFromApiToChannels(gdsDataClientConfig);
	}
}
