package io.openems.edge.io.phoenixcontact.gds;

import java.util.List;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.edge.common.event.EdgeEventConstants;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextReadFromApiResourceCommand.class)
public class PlcNextReadFromApiResourceCommand implements PlcNextApiCommand {

	public final PlcNextGdsProvider gdsProvider;

	@Activate
	public PlcNextReadFromApiResourceCommand(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) PlcNextGdsProvider gdsProvider) {
		this.gdsProvider = gdsProvider;
	}
	
	@Override
	public List<String> eventTriggers() {
		return List.of(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE);
	}
	
	@Override
	public void execute() {
		gdsProvider.readFromApiToChannels();
	}

}
