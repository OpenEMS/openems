package io.openems.edge.controller.api.common.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.ComponentJsonApiExtension;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.JsonApiEndpoint;
import io.openems.edge.common.jsonapi.Subrequest;
import io.openems.edge.common.jsonapi.Tag;

/**
 * This class handles {@link ComponentJsonApiRequest} and delegates the request
 * to their component.
 */
@Component(service = { ComponentRequestHandler.class, JsonApi.class }, scope = ServiceScope.SINGLETON)
public class ComponentRequestHandler implements JsonApi {

	private record BoundComponentJsonApi(//
			ComponentJsonApi jsonApi, //
			long serviceId, //
			JsonApiBuilder apiBuilder, //
			Map<ComponentJsonApiExtension<?>, JsonApiBuilder> classBuilder, //
			Consumer<JsonApiEndpoint> endpointListener //
	) {

		public ComponentRequestHandler.BoundComponentJsonApi withEndpointListener(
				Consumer<JsonApiEndpoint> endpointListener) {
			return new BoundComponentJsonApi(this.jsonApi, this.serviceId, this.apiBuilder, this.classBuilder,
					endpointListener);
		}

	}

	private final Logger log = LoggerFactory.getLogger(ComponentRequestHandler.class);

	private final ReadWriteLock updateLock = new ReentrantReadWriteLock();
	private final Map<String, BoundComponentJsonApi> jsonApis = new TreeMap<>();
	private final Map<Class<?>, List<ComponentJsonApiExtension<?>>> extensions = new HashMap<>();

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			bind = "bindJsonApi", unbind = "unbindJsonApi", //
			updated = "updateJsonApi", //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE //
	)
	public void bindJsonApi(ComponentJsonApi jsonApi, Map<String, ?> ref) {
		final var builder = new JsonApiBuilder();

		final var boundComponent = new BoundComponentJsonApi(//
				jsonApi, //
				(Long) ref.get(Constants.SERVICE_ID), //
				builder, //
				new HashMap<>(), //
				t -> t.getDef().getTags().add(new Tag(jsonApi.id())) //
		);

		builder.addEndpointAddedListener(boundComponent.endpointListener());

		this.updateLock.writeLock().lock();
		try {
			for (var extensionsEntry : this.extensions.entrySet()) {
				if (extensionsEntry.getKey().isInstance(jsonApi)) {
					for (var extension : extensionsEntry.getValue()) {
						this.bindExtensionTyped(extension, boundComponent);
					}
				}
			}

			jsonApi.buildJsonApiRoutes(builder);
			this.jsonApis.put(jsonApi.id(), boundComponent);
			this.log.info("Added '{}' to Component Apis.", jsonApi.id());
		} finally {
			this.updateLock.writeLock().unlock();
		}

	}

	public void updateJsonApi(ComponentJsonApi jsonApi, Map<String, ?> ref) {
		this.updateLock.writeLock().lock();
		try {
			final long servicePid = (Long) ref.get(Constants.SERVICE_ID);
			for (var entry : this.jsonApis.entrySet()) {
				if (entry.getValue().serviceId() != servicePid) {
					continue;
				}
				final var previousComponentId = entry.getKey();
				final var prevBinding = this.jsonApis.remove(previousComponentId);
				for (var endpoint : prevBinding.apiBuilder().getEndpoints().values()) {
					endpoint.getDef().getTags().removeIf(t -> t.name().equals(previousComponentId));
					endpoint.getDef().getTags().add(new Tag(jsonApi.id()));
				}
				prevBinding.apiBuilder().removeEndpointAddedListener(prevBinding.endpointListener());
				final var newBinding = prevBinding
						.withEndpointListener(t -> t.getDef().getTags().add(new Tag(jsonApi.id())));
				prevBinding.apiBuilder().addEndpointAddedListener(newBinding.endpointListener());

				this.jsonApis.put(jsonApi.id(), newBinding);
				break;
			}
			this.log.info("Updated Component Api {}", jsonApi.id());
		} finally {
			this.updateLock.writeLock().unlock();
		}
	}

	public void unbindJsonApi(ComponentJsonApi jsonApi, Map<String, ?> ref) {
		this.updateLock.writeLock().lock();
		try {
			this.jsonApis.remove(jsonApi.id());
		} finally {
			this.updateLock.writeLock().unlock();
		}
		this.log.info("Removed '{}' from Component Apis.", jsonApi.id());
	}

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			bind = "bindExtension", unbind = "unbindExtension", //
			updated = "updateExtension", //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE //
	)
	public void bindExtension(ComponentJsonApiExtension<?> extension) {
		this.log.info("Bound ComponentJsonApiExtension: {}", extension.getClass().getName());

		this.updateLock.writeLock().lock();
		try {
			this.extensions.compute(extension.extendClass(), (clazz, componentJsonApiExtensions) -> {
				final var list = componentJsonApiExtensions == null//
						? new ArrayList<ComponentJsonApiExtension<?>>()//
						: new ArrayList<>(componentJsonApiExtensions);
				list.add(extension);
				return list;
			});

			for (var boundJsonApi : this.jsonApis.values()) {
				final var service = boundJsonApi.jsonApi();
				if (!extension.extendClass().isInstance(service)) {
					continue;
				}
				this.bindExtensionTyped(extension, boundJsonApi);
			}
		} finally {
			this.updateLock.writeLock().unlock();
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends ComponentJsonApi> void bindExtensionTyped(//
			ComponentJsonApiExtension<T> extension, //
			BoundComponentJsonApi boundJsonApi //
	) {
		final var jsonApiBuilder = new JsonApiBuilder();
		extension.buildJsonApiRoutes((T) boundJsonApi.jsonApi, jsonApiBuilder);
		boundJsonApi.apiBuilder().addBuilder(jsonApiBuilder);
		boundJsonApi.classBuilder.put(extension, jsonApiBuilder);
		this.log.info("Bound ComponentJsonApiExtension {} to ComponentJsonApi {}", extension.getClass().getName(),
				boundJsonApi.jsonApi().getClass().getName());
	}

	public void updateExtension(ComponentJsonApiExtension<?> extension) {
		this.log.info("Update ComponentJsonApiExtension: {}", extension.getClass().getName());

		this.updateLock.writeLock().lock();
		try {
			for (var boundJsonApi : this.jsonApis.values()) {
				final var service = boundJsonApi.jsonApi();
				if (!extension.extendClass().isInstance(service)) {
					continue;
				}
				final var builder = boundJsonApi.classBuilder.remove(extension);
				if (builder != null) {
					boundJsonApi.apiBuilder().removeBuilder(builder);
				}
				this.bindExtensionTyped(extension, boundJsonApi);
			}
		} finally {
			this.updateLock.writeLock().unlock();
		}
	}

	public void unbindExtension(ComponentJsonApiExtension<?> extension) {
		this.log.info("Unbind ComponentJsonApiExtension: {}", extension.getClass().getName());
		this.updateLock.writeLock().lock();
		try {
			this.extensions.compute(extension.extendClass(), (clazz, componentJsonApiExtensions) -> {
				if (componentJsonApiExtensions == null || componentJsonApiExtensions.isEmpty()) {
					return null;
				}

				if (componentJsonApiExtensions.remove(extension) && componentJsonApiExtensions.isEmpty()) {
					return null;
				}

				return componentJsonApiExtensions;
			});
			for (var boundJsonApi : this.jsonApis.values()) {
				final var service = boundJsonApi.jsonApi();
				if (!extension.extendClass().isInstance(service)) {
					continue;
				}
				final var builder = boundJsonApi.classBuilder.remove(extension);
				if (builder == null) {
					continue;
				}
				boundJsonApi.apiBuilder().removeBuilder(builder);
			}
		} finally {
			this.updateLock.writeLock().unlock();
		}
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.rpc(ComponentJsonApiRequest.METHOD, endpoint -> {
			endpoint.setDescription("Handles a ComponentJsonApiRequest.");

		}, () -> {
			return this.jsonApis.entrySet().stream() //
					.map(jsonApiEntry -> {
						final var subrequest = new Subrequest(JsonUtils.buildJsonObject() //
								.addProperty("componentId", jsonApiEntry.getKey()) //
								.build());

						subrequest.addRpcBuilderFor(jsonApiEntry.getValue().apiBuilder(), "payload");
						return subrequest;
					}).toList();
		}, call -> {
			final var request = ComponentJsonApiRequest.from(call.getRequest());
			this.updateLock.readLock().lock();
			try {
				final var component = this.jsonApis.get(request.getComponentId());
				if (component == null) {
					throw new RuntimeException("Component with id '" + request.getComponentId() + "' was not found");
				}
				var mapped = call.mapRequest(request.getPayload());
				component.apiBuilder().handle(mapped);

				call.setResponse(mapped.getResponse());
			} finally {
				this.updateLock.readLock().unlock();
			}
		});
	}

}