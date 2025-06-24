package io.openems.edge.core.update;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.update.jsonrpc.ExecuteUpdate;
import io.openems.edge.common.update.jsonrpc.GetUpdateState;
import io.openems.edge.common.update.jsonrpc.GetUpdateables;

@Component(//
		immediate = true, //
		scope = ServiceScope.SINGLETON //
)
public class UpdateManager implements ComponentJsonApi {

	private record UpdateableInfo(Updateable updateable) {

	}

	private final Map<String, UpdateableInfo> updateables = new ConcurrentHashMap<>();

	/**
	 * Binds a {@link Updateable}.
	 *
	 * @param updateable the {@link Updateable} to bind
	 * @param ref        the properties of the service to bind
	 */
	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policyOption = ReferencePolicyOption.GREEDY, //
			policy = ReferencePolicy.DYNAMIC //
	)
	public void bindUpdateable(Updateable updateable, Map<String, ?> ref) {
		final var servicePid = String.valueOf((Long) ref.get(Constants.SERVICE_ID));
		this.updateables.put(servicePid, new UpdateableInfo(updateable));
	}

	/**
	 * Unbinds a {@link Updateable}.
	 *
	 * @param updateable the {@link Updateable} to unbind
	 * @param ref        the properties of the service to unbind
	 */
	public void unbindUpdateable(Updateable updateable, Map<String, ?> ref) {
		final var servicePid = String.valueOf((Long) ref.get(Constants.SERVICE_ID));
		this.updateables.remove(servicePid);
	}

	@Override
	public String id() {
		return "_updateManager";
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(new GetUpdateables(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.ADMIN));
		}, call -> {
			final var user = call.get(EdgeKeys.USER_KEY);

			return new GetUpdateables.Response(this.updateables.entrySet().stream() //
					.map(t -> {
						final var metaInfo = t.getValue().updateable().getMetaInfo(user.getLanguage());

						if (!user.getRole().isAtLeast(metaInfo.requiredMinRole())) {
							return null;
						}

						return new GetUpdateables.Response.UpdateableMetadata(t.getKey(), metaInfo.name(),
								metaInfo.description());
					}) //
					.filter(Objects::nonNull) //
					.toList());
		});

		builder.handleRequest(new ExecuteUpdate(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.ADMIN));
		}, call -> {
			final var updateable = this.updateables.get(call.getRequest().id());

			if (updateable == null) {
				throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(call.getRequest().id());
			}

			final var user = call.get(EdgeKeys.USER_KEY);
			final var metaInfo = updateable.updateable().getMetaInfo(Language.DEFAULT);

			if (!user.getRole().isAtLeast(metaInfo.requiredMinRole())) {
				throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(call.getRequest().id());
			}

			updateable.updateable().executeUpdate();

			return EmptyObject.INSTANCE;
		});

		builder.handleRequest(new GetUpdateState(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.ADMIN));
		}, call -> {
			final var updateable = this.updateables.get(call.getRequest().id());

			if (updateable == null) {
				throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(call.getRequest().id());
			}

			final var user = call.get(EdgeKeys.USER_KEY);
			final var metaInfo = updateable.updateable().getMetaInfo(Language.DEFAULT);

			if (!user.getRole().isAtLeast(metaInfo.requiredMinRole())) {
				throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(call.getRequest().id());
			}

			return new GetUpdateState.Response(updateable.updateable().getUpdateState());
		});
	}

}
