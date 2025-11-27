package io.openems.edge.io.phoenixcontact.gds;

import java.util.List;

public interface PlcNextApiCommand {
	
	List<String> eventTriggers();
	
	void execute();

}
