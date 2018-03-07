package io.openems.edge.api.device;

import java.time.LocalDateTime;
import java.util.Locale;

import org.osgi.annotation.versioning.ProviderType;
@ProviderType
public interface DeviceMessage {

	DeviceInterface getDevice();
	
	String getId();
	
	String getName(Locale locale);
	
	String getDescription(Locale locale);
	
	DeviceMessageType getType();
	
}
