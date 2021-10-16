/**
 * 
 */
package io.openems.edge.shelly.core;

import com.google.gson.JsonObject;

/**
 * @author scholty
 *
 */
public interface ShellyComponent {
	
	Boolean wantsExtendedData();
	Boolean setBaseChannels();
	Integer wantedIndex();
	
	void setExtendedData(JsonObject o);

}
