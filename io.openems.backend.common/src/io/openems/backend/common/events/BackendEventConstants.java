package io.openems.backend.common.events;

public final class BackendEventConstants {
	
	private BackendEventConstants() {
		// avoid inheritance
	}
	
    public static final String TOPIC_BASE = "io/openems/backend/";
    
    public static final String TOPIC_EDGE = "io/openems/backend/edge/";
    
    public static final String TOPIC_EDGE_ONLINE = TOPIC_EDGE + "ONLINE";
    
    public static final String PROPERTY_KEY_EDGE_ID = "edgeId";
 
}
