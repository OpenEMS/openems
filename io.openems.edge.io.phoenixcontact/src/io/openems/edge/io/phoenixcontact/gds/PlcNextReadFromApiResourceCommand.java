package io.openems.edge.io.phoenixcontact.gds;

import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.edge.common.event.EdgeEventConstants;

public class PlcNextReadFromApiResourceCommand implements PlcNextApiCommand {

	private final PlcNextGdsProvider gdsProvider;
	private final String instanceName;

	public PlcNextReadFromApiResourceCommand(PlcNextGdsProvider gdsProvider, String instanceName) {
		this.gdsProvider = gdsProvider;
		this.instanceName = instanceName;
	}
	
	@Override
	public List<String> eventTriggers() {
		return List.of(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE);
	}
	
	@Override
	public void execute() {
		gdsProvider.readFromApiToChannels(instanceName);
	}
}
