package io.openems.backend.common.mail;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.utils.JsonUtils;

/**
 * Context for sending a mail. Contains basic information needed to send a mail,
 * such as the edgeId and the recipients.
 * 
 * <p>
 * Should be extended by specific mail contexts, which contain additional
 * information needed for the specific mail template.
 */
public abstract class MailContext {

	protected final String edgeId;
	protected final List<String> recipients;

	protected MailContext(String edgeId, List<String> recipients) {
		this.edgeId = edgeId;
		this.recipients = recipients;
	}

	/**
	 * Return this context as JsonObject, which can be used to fill the template for
	 * sending a mail.
	 * 
	 * <p>
	 * The default implementation only contains the edgeId and the recipients, which are common for all mail contexts. 
	 * Specific mail contexts should override this method
	 * 
	 * @return this context as JsonObject
	 */
	public JsonObject toJson() {
		return JsonUtils.buildJsonObject() //
				.add("recipients", JsonUtils.generateJsonArray(this.recipients, JsonPrimitive::new)) //
				.addProperty("edgeId", this.edgeId) //
				.build();
	}
	
	@Override
	public String toString() {
		return "MailContext" + this.toJson().toString();
	}

}
