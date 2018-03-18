package io.openems.edge.api.message;

import java.util.Locale;

import org.osgi.annotation.versioning.ProviderType;
@ProviderType
public interface Message {

	String getId();
	
	String getName(Locale locale);
	
	String getDescription(Locale locale);
	
	MessageType getType();
	
}
