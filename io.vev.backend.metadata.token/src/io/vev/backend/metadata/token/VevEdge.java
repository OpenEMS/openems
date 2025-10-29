package io.vev.backend.metadata.token;

import io.openems.backend.common.metadata.Edge;
import org.bson.Document;

public class VevEdge extends Edge {

	private final String apikey;
	private final String setupPassword;

    public VevEdge(MetadataToken parent, Document doc) {
        super(parent, doc.getObjectId("tenantID").toHexString() + "." + doc.getObjectId("_id").toHexString(), doc.getString("comment"),
                doc.getString("version"), doc.getString("producttype"), null);
        this.apikey = doc.getString("apikey");
        this.setupPassword = doc.getString("setupPassword");
    }

	public VevEdge(MetadataToken parent, String id, String apikey, String setupPassword, String comment, String version,
                   String producttype) {
		super(parent, id, comment, version, producttype, null);
		this.apikey = apikey;
		this.setupPassword = setupPassword;
	}

	public String getApikey() {
		return this.apikey;
	}

	public String getSetupPassword() {
		return this.setupPassword;
	}
}