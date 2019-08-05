package io.openems.common.jsonrpc.shared;

import java.util.Collection;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.accesscontrol.RoleId;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

public class EdgeMetadata {

	/**
	 * Converts a collection of EdgeMetadatas to a JsonArray.
	 * 
	 * <pre>
	 * [{
	 *   "id": String,
	 *   "comment": String,
	 *   "producttype: String,
	 *   "version: String,
	 *   "roleId: "admin" | "installer" | "owner" | "guest",
	 *   "isOnline: boolean
	 * }]
	 * </pre>
	 * 
	 * @param metadatas the EdgeMetadatas
	 * @return a JsonArray
	 */
	public static JsonArray toJson(Collection<EdgeMetadata> metadatas) {
		JsonArray result = new JsonArray();
		for (EdgeMetadata metadata : metadatas) {
			result.add(metadata.toJsonObject());
		}
		return result;
	}

	private final String id;
	private final String comment;
	private final String producttype;
	private final SemanticVersion version;
	private final RoleId roleId;
	private final boolean isOnline;

	public EdgeMetadata(String id, String comment, String producttype, SemanticVersion version, RoleId roleId,
						boolean isOnline) {
		this.id = id;
		this.comment = comment;
		this.producttype = producttype;
		this.version = version;
		this.roleId = roleId;
		this.isOnline = isOnline;
	}

	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("id", this.id) //
				.addProperty("comment", this.comment) //
				.addProperty("producttype", this.producttype) //
				.addProperty("version", this.version.toString()) //
				.addProperty("roleId", this.roleId != null ? this.roleId.toString() : null) //
				.addProperty("isOnline", this.isOnline) //
				.build();
	}

}