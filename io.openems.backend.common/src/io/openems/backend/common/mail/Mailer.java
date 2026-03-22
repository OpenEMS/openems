package io.openems.backend.common.mail;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Mailer {

	/**
	 * Send alerting mail at stamp to users. Sends a mail for each MailContext using the given template.
	 *
	 * @param sendAt     time stamp at which the mail was sent
	 * @param templateId Id of the Template to use
	 * @param context    mail data (e.g. recipients, edgeId, subject, ...)
	 * 
	 * @return Future with the number of sent mails, or an exception if sending failed
	 */
	public CompletableFuture<Integer> sendMail(ZonedDateTime sendAt, String templateId, List<MailContext> context);

}
