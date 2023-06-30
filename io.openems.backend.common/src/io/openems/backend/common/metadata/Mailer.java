package io.openems.backend.common.metadata;

import java.time.ZonedDateTime;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonElement;

@ProviderType
public interface Mailer {

	/**
	 * Send alerting mail at stamp to users.
	 *
	 * @param sendAt   is dateTime at which to send
	 * @param template mail template to use
	 * @param params   mail data
	 */
	public void sendMail(ZonedDateTime sendAt, String template, JsonElement params);

}