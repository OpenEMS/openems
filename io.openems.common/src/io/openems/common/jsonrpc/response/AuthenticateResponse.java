package io.openems.common.jsonrpc.response;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithPasswordRequest;
import io.openems.common.jsonrpc.request.AuthenticateWithTokenRequest;
import io.openems.common.session.AbstractUser;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for {@link AuthenticateWithPasswordRequest} or
 * {@link AuthenticateWithTokenRequest}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "token": String,
 *     "user": {@link AbstractUser#toJsonObject()}
 *     "edges": {@link EdgeMetadata#toJson(java.util.Collection)}
 *   }
 * }
 * </pre>
 */
public class AuthenticateResponse extends JsonrpcResponseSuccess {

	public static class EdgeMetadata {

		/**
		 * Converts a collection of EdgeMetadatas to a JsonArray.
		 *
		 * <pre>
		 * [{
		 *   "id": String,
		 *   "comment": String,
		 *   "producttype": String,
		 *   "version": String,
		 *   "role": "admin" | "installer" | "owner" | "guest",
		 *   "isOnline": boolean
		 * }]
		 * </pre>
		 *
		 * @param metadatas the EdgeMetadatas
		 * @return a JsonArray
		 */
		public static JsonArray toJson(Collection<EdgeMetadata> metadatas) {
			var result = new JsonArray();
			for (EdgeMetadata metadata : metadatas) {
				result.add(metadata.toJsonObject());
			}
			return result;
		}

		private final String id;
		private final String comment;
		private final String producttype;
		private final SemanticVersion version;
		private final Role role;
		private final boolean isOnline;
		private final ZonedDateTime lastmessage;

		public EdgeMetadata(String id, String comment, String producttype, SemanticVersion version, Role role,
				boolean isOnline, ZonedDateTime lastmessage) {
			this.id = id;
			this.comment = comment;
			this.producttype = producttype;
			this.version = version;
			this.role = role;
			this.isOnline = isOnline;
			this.lastmessage = lastmessage;
		}

		protected JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.addProperty("id", this.id) //
					.addProperty("comment", this.comment) //
					.addProperty("producttype", this.producttype) //
					.addProperty("version", this.version.toString()) //
					.add("role", this.role.asJson()) //
					.addProperty("isOnline", this.isOnline) //
					.addPropertyIfNotNull("lastmessage", this.lastmessage) //
					.build();
		}
	}

	private final String token;
	private final AbstractUser user;
	private final List<EdgeMetadata> edges;
	private final Language language;

	public AuthenticateResponse(UUID id, String token, AbstractUser user, List<EdgeMetadata> edges, Language language) {
		super(id);
		this.token = token;
		this.user = user;
		this.edges = edges;
		this.language = language;
	}

	/**
	 * This method formats the {@link AuthenticateResponse} so that it contains the
	 * required information for OpenEMS UI.
	 */
	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.addProperty("token", this.token) //
				.add("user", JsonUtils.buildJsonObject() //
						.addProperty("id", this.user.getId()) //
						.addProperty("name", this.user.getName()) //
						.addProperty("language", this.language.name()) //
						.add("globalRole", this.user.getGlobalRole().asJson()) //
						.build()) //
				.add("edges", EdgeMetadata.toJson(this.edges)) //
				.build();
	}

}
