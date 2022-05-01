package io.openems.backend.common.metadata;

import java.time.ZonedDateTime;
import java.util.List;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Mailer {

	/**
	 * Send alerting mail at stamp to users.
	 * 
	 * @param stamp is timestamp at which to send
	 * @param users to which to send
	 */
	public void sendAlertingMail(ZonedDateTime stamp, List<EdgeUser> users);

}
