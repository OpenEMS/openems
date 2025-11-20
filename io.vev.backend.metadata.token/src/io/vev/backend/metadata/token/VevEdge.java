package io.vev.backend.metadata.token;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;

import org.bson.Document;

import io.openems.backend.common.metadata.Edge;
import io.openems.common.types.SemanticVersion;

public class VevEdge extends Edge {

	private volatile String apikey;
	private volatile String setupPassword;

	public VevEdge(MetadataToken parent, Document doc) {
		super(parent, VevEdge.buildEdgeId(doc), //
				VevEdge.getStringOrEmpty(doc, "comment"), //
				doc.getString("version"), //
				VevEdge.getStringOrEmpty(doc, "producttype"), //
				VevEdge.getLastmessage(doc));
		this.apikey = doc.getString("apikey");
		this.setupPassword = doc.getString("setupPassword");
	}

	public String getApikey() {
		return this.apikey;
	}

	public String getSetupPassword() {
		return this.setupPassword;
	}

	void updateFromDocument(Document doc) {
		this.setComment(VevEdge.getStringOrEmpty(doc, "comment"));
		this.setVersion(SemanticVersion.fromStringOrZero(doc.getString("version")));
		this.setProducttype(doc.getString("producttype"));

		var lastmessage = VevEdge.getLastmessage(doc);
		if (lastmessage != null) {
			this.setLastmessage(lastmessage);
		}

		var newApikey = doc.getString("apikey");
		if (!Objects.equals(this.apikey, newApikey)) {
			this.apikey = newApikey;
		}

		var newSetupPassword = doc.getString("setupPassword");
		if (!Objects.equals(this.setupPassword, newSetupPassword)) {
			this.setupPassword = newSetupPassword;
		}
	}

	static String buildEdgeId(Document doc) {
		var tenantId = doc.getObjectId("tenantID");
		var edgeId = doc.getObjectId("_id");
		if (tenantId == null || edgeId == null) {
			throw new IllegalArgumentException("Missing tenantID or _id on edge document");
		}
	return tenantId.toHexString() + ":" + edgeId.toHexString();
}

	private static ZonedDateTime getLastmessage(Document doc) {
		var value = doc.get("lastmessage");
		if (value == null) {
			value = doc.get("lastMessage");
		}
		if (value instanceof Date date) {
			return ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
		}
		if (value instanceof Number number) {
			return ZonedDateTime.ofInstant(Instant.ofEpochMilli(number.longValue()), ZoneOffset.UTC);
		}
		return null;
	}

	private static String getStringOrEmpty(Document doc, String key) {
		var value = doc.getString(key);
		return value == null ? "" : value;
	}
}
