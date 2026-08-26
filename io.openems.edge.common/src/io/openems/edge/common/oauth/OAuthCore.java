package io.openems.edge.common.oauth;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.oauth.jsonrpc.ConnectOAuth;
import io.openems.edge.common.oauth.jsonrpc.DisconnectOAuthConnection;
import io.openems.edge.common.oauth.jsonrpc.GetAllOAuthProvider;
import io.openems.edge.common.oauth.jsonrpc.GetOAuthConnectionState;
import io.openems.edge.common.oauth.jsonrpc.InitiateOAuthConnect;

@Component
public class OAuthCore implements ComponentJsonApi {

	public static record OAuthMetaInfo(//
			String identifier, //
			String title, //
			String description //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link OAuthCore.OAuthMetaInfo}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<OAuthCore.OAuthMetaInfo> serializer() {
			return jsonObjectSerializer(OAuthCore.OAuthMetaInfo.class, json -> {
				return new OAuthCore.OAuthMetaInfo(//
						json.getString("identifier"), //
						json.getString("title"), //
						json.getString("description") //
				);
			}, obj -> {
				return JsonUtils.buildJsonObject() //
						.addProperty("identifier", obj.identifier()) //
						.addProperty("title", obj.title()) //
						.addProperty("description", obj.description()) //
						.build();
			});
		}

	}

	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	private volatile List<OAuthProvider> oauthProvider;

	@Override
	public String id() {
		return "_oauth2";
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new GetAllOAuthProvider(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
		}, call -> {
			return new GetAllOAuthProvider.Response(this.oauthProvider.stream()//
					.map(OAuthProvider::getMetaInfo) //
					.toList());
		});
		builder.handleRequest(new GetOAuthConnectionState(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
		}, call -> {
			final var oauthProvider = this.oauthProvider.stream()//
					.filter(t -> t.getMetaInfo().identifier().equals(call.getRequest().identifier())) //
					.findAny().orElseThrow();

			return new GetOAuthConnectionState.Response(oauthProvider.getOAuthConnectionState().asEnum());
		});
		builder.handleRequest(new InitiateOAuthConnect(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
		}, call -> {
			final var oauthProvider = this.oauthProvider.stream()//
					.filter(t -> t.getMetaInfo().identifier().equals(call.getRequest().identifier())) //
					.findAny().orElseThrow();

			return oauthProvider.initiateConnect().get();
		});
		builder.handleRequest(new ConnectOAuth(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
		}, call -> {
			final var oauthProvider = this.oauthProvider.stream()//
					.filter(t -> t.getMetaInfo().identifier().equals(call.getRequest().identifier())) //
					.findAny().orElseThrow();

			return oauthProvider.connectCode(call.getRequest().state(), call.getRequest().code())
					.thenApply(unused -> EmptyObject.INSTANCE).get();
		});
		builder.handleRequest(new DisconnectOAuthConnection(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
		}, call -> {
			final var oauthProvider = this.oauthProvider.stream()//
					.filter(t -> t.getMetaInfo().identifier().equals(call.getRequest().identifier())) //
					.findAny().orElseThrow();

			oauthProvider.disconnect();

			return EmptyObject.INSTANCE;
		});
	}

}
