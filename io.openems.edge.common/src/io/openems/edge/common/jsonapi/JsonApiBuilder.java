package io.openems.edge.common.jsonapi;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.FunctionUtils;

/**
 * TODO route interceptor/observer/notifications.
 */
public class JsonApiBuilder {

	private final Logger log = LoggerFactory.getLogger(JsonApiBuilder.class);

	private boolean debug = false;

	private final Map<String, JsonApiEndpoint> endpoints = new TreeMap<>();

	private final List<JsonApiBuilder> builder = new ArrayList<>();
	private final List<Consumer<JsonApiEndpoint>> endpointAddedListeners = new ArrayList<>();
	private final List<Consumer<JsonApiEndpoint>> endpointRemovedListeners = new ArrayList<>();

	private final Consumer<JsonApiEndpoint> endpointAddedListener = e -> {
		this.endpoints.put(e.getMethod(), e);
	};

	private final Consumer<JsonApiEndpoint> endpointRemovedListener = e -> {
		this.endpoints.remove(e.getMethod());
	};

	/**
	 * Adds a rpc request handler to the current builder.
	 * 
	 * @param method  the method of the handled rpc request
	 * @param handler the handler of the request
	 * @return this
	 */
	public JsonApiBuilder handleRequest(//
			final String method, //
			final ThrowingFunction<Call<JsonrpcRequest, JsonrpcResponse>, JsonrpcResponse, Exception> handler //
	) {
		return this.rpc(method, call -> {
			call.setResponse(handler.apply(call));
		});
	}

	/**
	 * Adds a rpc request handler to the current builder.
	 * 
	 * @param method     the method of the handled rpc request
	 * @param defBuilder the builder for the {@link EndpointDefinitionBuilder}
	 * @param handler    the handler of the request
	 * @return this
	 */
	public JsonApiBuilder handleRequest(//
			final String method, //
			final Consumer<EndpointDefinitionBuilder<JsonrpcRequest, JsonrpcResponse>> defBuilder, //
			final ThrowingFunction<Call<JsonrpcRequest, JsonrpcResponse>, JsonrpcResponse, Exception> handler //
	) {
		return this.rpc(method, defBuilder, call -> {
			call.setResponse(handler.apply(call));
		});
	}

	/**
	 * Adds a rpc request handler to the current builder.
	 * 
	 * @param <REQUEST>    the type of the request
	 * @param <RESPONSE>   the type of the response
	 * @param endpointType the {@link EndpointRequestType} of the handled rpc
	 *                     request
	 * @param defBuilder   the builder for the {@link EndpointDefinitionBuilder}
	 * @param handler      the handler of the request
	 * @return this
	 */
	public <REQUEST, RESPONSE> JsonApiBuilder handleRequest(//
			final EndpointRequestType<REQUEST, RESPONSE> endpointType, //
			final Consumer<EndpointDefinitionBuilder<REQUEST, RESPONSE>> defBuilder, //
			final ThrowingFunction<Call<REQUEST, RESPONSE>, RESPONSE, Exception> handler //
	) {
		return this.rpc(endpointType.getMethod(), defBuilder, call -> {
			call.setResponse(handler.apply(call));
		}, endpointType.getRequestSerializer(), endpointType.getResponseSerializer());
	}

	/**
	 * Adds a rpc handler to the current builder.
	 * 
	 * @param method  the method of the handled rpc request
	 * @param handler the handler of the request
	 * @return this
	 */
	public JsonApiBuilder rpc(//
			final String method, //
			final ThrowingConsumer<Call<JsonrpcRequest, JsonrpcResponse>, Exception> handler //
	) {
		this.addEndpoint(new JsonApiEndpoint(method, (b, call) -> {
			try {
				handler.accept(call);
			} catch (Exception e) {
				call.setResponse(this.handleException(call, e));
			}
		}));
		return this;
	}

	/**
	 * Adds a rpc handler to the current builder.
	 * 
	 * @param method     the method of the handled rpc request
	 * @param defBuilder the builder for the {@link EndpointDefinitionBuilder}
	 * @param handler    the handler of the request
	 * @return this
	 */
	public JsonApiBuilder rpc(//
			final String method, //
			final Consumer<EndpointDefinitionBuilder<JsonrpcRequest, JsonrpcResponse>> defBuilder, //
			final ThrowingConsumer<Call<JsonrpcRequest, JsonrpcResponse>, Exception> handler //
	) {
		final var endpointDef = new EndpointDefinitionBuilder<JsonrpcRequest, JsonrpcResponse>();
		defBuilder.accept(endpointDef);
		this.addEndpoint(new JsonApiEndpoint(method, (b, call) -> {
			try {
				for (var guard : endpointDef.getGuards()) {
					guard.test(call);
				}
				handler.accept(call);
			} catch (Exception e) {
				call.setResponse(this.handleException(call, e));
			}
		}, endpointDef));
		return this;
	}

	/**
	 * Adds a rpc handler to the current builder.
	 * 
	 * @param <REQUEST>          the type of the request
	 * @param <RESPONSE>         the type of the response
	 * @param method             the method of the handled rpc request
	 * @param defBuilder         the builder for the
	 *                           {@link EndpointDefinitionBuilder}
	 * @param handler            the handler of the request
	 * @param requestSerializer  the {@link JsonSerializer} of the request
	 * @param responseSerializer the {@link JsonSerializer} of the response
	 * @return this
	 */
	public <REQUEST, RESPONSE> JsonApiBuilder rpc(//
			final String method, //
			final Consumer<EndpointDefinitionBuilder<REQUEST, RESPONSE>> defBuilder, //
			final ThrowingConsumer<Call<REQUEST, RESPONSE>, Exception> handler, //
			final JsonSerializer<REQUEST> requestSerializer, //
			final JsonSerializer<RESPONSE> responseSerializer //
	) {
		final var endpointDef = new EndpointDefinitionBuilder<REQUEST, RESPONSE>();
		endpointDef.setRequestSerializer(requestSerializer);
		endpointDef.setResponseSerializer(responseSerializer);
		defBuilder.accept(endpointDef);

		this.addEndpoint(new JsonApiEndpoint(method, (b, t) -> {
			for (var guard : endpointDef.getGuards()) {
				try {
					guard.test(t);
				} catch (Exception e) {
					t.setResponse(this.handleException(t, e));
					return;
				}
			}
			try {
				final var mappedCall = t.mapRequest(requestSerializer.deserialize(t.getRequest().getParams())) //
						.<RESPONSE>mapResponse();

				handler.accept(mappedCall);

				t.setResponse(new GenericJsonrpcResponseSuccess(t.getRequest().getId(),
						responseSerializer.serialize(mappedCall.getResponse()).getAsJsonObject()));
			} catch (Exception e) {
				t.setResponse(this.handleException(t, e));
			}
		}, endpointDef));
		return this;
	}

	/**
	 * Adds a rpc handler to the current builder.
	 * 
	 * @param <REQUEST>  the type of the request
	 * @param method     the method of the handled rpc request
	 * @param defBuilder the builder for the {@link EndpointDefinitionBuilder}
	 * @param subroutes  the subroutes which can be reached with this handler
	 * @param handler    the handler of the request
	 * @param serializer the {@link JsonSerializer} of the request
	 * @return this
	 */
	public <REQUEST> JsonApiBuilder rpc(//
			final String method, //
			final Consumer<EndpointDefinitionBuilder<REQUEST, JsonrpcResponse>> defBuilder, //
			final Supplier<List<Subrequest>> subroutes, //
			final ThrowingConsumer<Call<REQUEST, JsonrpcResponse>, Exception> handler, //
			final JsonSerializer<REQUEST> serializer //
	) {
		final var endpointDef = new EndpointDefinitionBuilder<REQUEST, JsonrpcResponse>();
		endpointDef.setRequestSerializer(serializer);
		defBuilder.accept(endpointDef);

		this.addEndpoint(new JsonApiEndpoint(method, (b, t) -> {
			try {
				for (var guard : endpointDef.getGuards()) {
					guard.test(t);
				}

				var call = t.mapRequest(serializer.deserialize(t.getRequest().getParams()));
				handler.accept(call);
				if (call.getResponse() != null) {
					t.setResponse(call.getResponse());
				}
			} catch (Exception e) {
				t.setResponse(this.handleException(t, e));
			}
		}, endpointDef, subroutes));
		return this;
	}

	/**
	 * Adds a rpc handler to the current builder.
	 * 
	 * @param method     the method of the handled rpc request
	 * @param defBuilder the builder for the {@link EndpointDefinitionBuilder}
	 * @param subroutes  the subroutes which can be reached with this handler
	 * @param handler    the handler of the request
	 * @return this
	 */
	public JsonApiBuilder rpc(//
			final String method, //
			final Consumer<EndpointDefinitionBuilder<JsonrpcRequest, JsonrpcResponse>> defBuilder, //
			final Supplier<List<Subrequest>> subroutes, //
			final ThrowingConsumer<Call<JsonrpcRequest, JsonrpcResponse>, Exception> handler //
	) {
		final var endpointDef = new EndpointDefinitionBuilder<JsonrpcRequest, JsonrpcResponse>();
		defBuilder.accept(endpointDef);

		this.addEndpoint(new JsonApiEndpoint(method, (b, t) -> {
			try {
				for (var guard : endpointDef.getGuards()) {
					guard.test(t);
				}

				handler.accept(t);
			} catch (Exception e) {
				t.setResponse(this.handleException(t, e));
			}
		}, endpointDef, subroutes));
		return this;
	}

	/**
	 * Delegates the handled request to another endpoint.
	 * 
	 * @param method         the method of the handled rpc request
	 * @param defBuilder     the builder for the {@link EndpointDefinitionBuilder}
	 * @param handler        the handler of the request which returns the delegated
	 *                       request
	 * @param builder        the path to the builder which handles the delegated
	 *                       request
	 * @param responseMapper the mapper of the response
	 * @param subroutes      the subroutes which can be reached with this handler
	 * @return this
	 */
	public JsonApiBuilder delegate(//
			final String method, //
			final Consumer<EndpointDefinitionBuilder<JsonrpcRequest, JsonrpcResponse>> defBuilder, //
			final ThrowingFunction<Call<JsonrpcRequest, JsonrpcResponse>, JsonrpcRequest, Exception> handler, //
			final Function<JsonApiBuilder, JsonApiBuilder> builder, //
			final Function<JsonrpcResponse, JsonrpcResponse> responseMapper, //
			final Supplier<List<Subrequest>> subroutes //
	) {
		final var endpointDef = new EndpointDefinitionBuilder<JsonrpcRequest, JsonrpcResponse>();
		defBuilder.accept(endpointDef);
		this.addEndpoint(new JsonApiEndpoint(method, (b, t) -> {
			try {
				final var call = t.mapRequest(handler.apply(t));
				builder.apply(b).handle(call);
				if (call.getResponse() != null) {
					t.setResponse(responseMapper.apply(call.getResponse()));
				}
			} catch (Exception e) {
				this.handleException(t, e);
			}
		}, endpointDef, subroutes));
		return this;
	}

	/**
	 * Delegates the handled request to another endpoint.
	 * 
	 * @param method    the method of the handled rpc request
	 * @param handler   the handler of the request which returns the delegated
	 *                  request
	 * @param builder   the path to the builder which handles the delegated request
	 * @param subroutes the subroutes which can be reached with this handler
	 * @return this
	 */
	public JsonApiBuilder delegate(//
			final String method, //
			final ThrowingFunction<Call<JsonrpcRequest, JsonrpcResponse>, JsonrpcRequest, Exception> handler, //
			final Function<JsonApiBuilder, JsonApiBuilder> builder, //
			final Supplier<List<Subrequest>> subroutes //
	) {
		return this.delegate(method, FunctionUtils::doNothing, handler, builder, Function.identity(), subroutes);
	}

	/**
	 * Delegates the handled request to another endpoint.
	 * 
	 * @param method     the method of the handled rpc request
	 * @param defBuilder the builder for the {@link EndpointDefinitionBuilder}
	 * @param handler    the handler of the request which returns the delegated
	 *                   request
	 * @param builder    the path to the builder which handles the delegated request
	 * @return this
	 */
	public JsonApiBuilder delegate(//
			final String method, //
			final Consumer<EndpointDefinitionBuilder<JsonrpcRequest, JsonrpcResponse>> defBuilder, //
			final ThrowingFunction<Call<JsonrpcRequest, JsonrpcResponse>, JsonrpcRequest, Exception> handler, //
			final Function<JsonApiBuilder, JsonApiBuilder> builder //
	) {
		return this.delegate(method, defBuilder, handler, builder, Function.identity(), null);
	}

	/**
	 * Delegates the handled request to another endpoint.
	 * 
	 * @param method  the method of the handled rpc request
	 * @param handler the handler of the request which returns the delegated request
	 * @param builder the path to the builder which handles the delegated request
	 * @return this
	 */
	public JsonApiBuilder delegate(//
			final String method, //
			final ThrowingFunction<Call<JsonrpcRequest, JsonrpcResponse>, JsonrpcRequest, Exception> handler, //
			final Function<JsonApiBuilder, JsonApiBuilder> builder //
	) {
		return this.delegate(method, FunctionUtils::doNothing, handler, builder, Function.identity(), null);
	}

	/**
	 * Delegates the handled request to another endpoint.
	 * 
	 * @param method     the method of the handled rpc request
	 * @param defBuilder the builder for the {@link EndpointDefinitionBuilder}
	 * @param handler    the handler of the request which returns the delegated
	 *                   request
	 * @return this
	 */
	public JsonApiBuilder delegate(//
			final String method, //
			final Consumer<EndpointDefinitionBuilder<JsonrpcRequest, JsonrpcResponse>> defBuilder, //
			final ThrowingFunction<Call<JsonrpcRequest, JsonrpcResponse>, JsonrpcRequest, Exception> handler //
	) {
		return this.delegate(method, defBuilder, handler, Function.identity());
	}

	/**
	 * Delegates the handled request to another endpoint.
	 * 
	 * @param method  the method of the handled rpc request
	 * @param handler the handler of the request which returns the delegated request
	 * @return this
	 */
	public JsonApiBuilder delegate(//
			final String method, //
			final ThrowingFunction<Call<JsonrpcRequest, JsonrpcResponse>, JsonrpcRequest, Exception> handler //
	) {
		return this.delegate(method, FunctionUtils::doNothing, handler, Function.identity());
	}

	private void addEndpoint(JsonApiEndpoint endpoint) {
		synchronized (this.endpoints) {
			final var previous = this.endpoints.put(endpoint.getMethod(), endpoint);
			if (previous != null) {
				this.endpointRemovedListeners.forEach(t -> t.accept(previous));
				this.log.error("Duplicated endpoint defined for method '" + endpoint.getMethod()
						+ "'. Override with last defined endpoint");
			}
			this.endpointAddedListeners.forEach(t -> t.accept(endpoint));

			if (this.isDebug()) {
				this.log.info("Added handler for method '" + endpoint.getMethod() + "'");
			}
		}
	}

	/**
	 * Removes an endpoint by its method.
	 * 
	 * @param method the method of the endpoint to remove
	 * @return the endpoint which got removed or null if no endpoint with this
	 *         method exists
	 */
	public JsonApiEndpoint removeEndpoint(String method) {
		synchronized (this.endpoints) {
			final var removedEndpoint = this.endpoints.remove(method);
			if (removedEndpoint == null) {
				return null;
			}
			this.endpointRemovedListeners.forEach(t -> t.accept(removedEndpoint));

			if (this.isDebug()) {
				this.log.info("Removed handler for method '" + method + "'");
			}
			return removedEndpoint;
		}
	}

	/**
	 * Adds a {@link JsonApiBuilder} to the current builder. All methods are
	 * "copied" to this builder and available for both then.
	 * 
	 * @param builder the {@link JsonApiBuilder} to add
	 * @see JsonApiBuilder#removeBuilder(JsonApiBuilder)
	 */
	public void addBuilder(JsonApiBuilder builder) {
		synchronized (builder.endpoints) {
			this.builder.add(builder);
			builder.addEndpointAddedListener(this.endpointAddedListener);
			builder.addEndpointRemovedListener(this.endpointRemovedListener);
			builder.getEndpoints().forEach((t, u) -> this.addEndpoint(u));
		}
	}

	/**
	 * Removes a {@link JsonApiBuilder} and all its methods from this builder.
	 * 
	 * @param builder the {@link JsonApiBuilder} to remove
	 * @see JsonApiBuilder#addBuilder(JsonApiBuilder)
	 */
	public void removeBuilder(JsonApiBuilder builder) {
		synchronized (builder.endpoints) {
			this.builder.remove(builder);
			builder.removeEndpointAddedListener(this.endpointAddedListener);
			builder.removeEndpointRemovedListener(this.endpointRemovedListener);
			builder.getEndpoints().forEach((t, u) -> this.removeEndpoint(t));
		}
	}

	/**
	 * Adds a listener to call when a endpoint got added to the current builder.
	 * 
	 * @param listener the listener to call when the event happened
	 */
	public void addEndpointAddedListener(Consumer<JsonApiEndpoint> listener) {
		this.endpointAddedListeners.add(listener);
	}

	/**
	 * Removes a listener which was subscribed to the endpoint added event.
	 * 
	 * @param listener the listener to remove
	 */
	public void removeEndpointAddedListener(Consumer<JsonApiEndpoint> listener) {
		this.endpointAddedListeners.remove(listener);
	}

	/**
	 * Adds a listener to call when a endpoint got removed to the current builder.
	 * 
	 * @param listener the listener to call when the event happened
	 */
	public void addEndpointRemovedListener(Consumer<JsonApiEndpoint> listener) {
		this.endpointRemovedListeners.add(listener);
	}

	/**
	 * Removes a listener which was subscribed to the endpoint removed event.
	 * 
	 * @param listener the listener to remove
	 */
	public void removeEndpointRemovedListener(Consumer<JsonApiEndpoint> listener) {
		this.endpointRemovedListeners.remove(listener);
	}

	public Map<String, JsonApiEndpoint> getEndpoints() {
		return ImmutableMap.copyOf(this.endpoints);
	}

	public boolean isDebug() {
		return this.debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * Closes this builder.
	 * 
	 * <p>
	 * Removes all {@link JsonApiBuilder} from this builder.
	 */
	public void close() {
		final var builderToRemove = new ArrayList<>(this.builder);
		builderToRemove.forEach(this::removeBuilder);
	}

	private JsonrpcResponseError handleException(Call<JsonrpcRequest, JsonrpcResponse> call, Throwable t) {
		if (this.isDebug()) {
			this.log.error(t.getMessage(), t);
		}

		// Get JSON-RPC Response Error
		if (t instanceof OpenemsNamedException ex) {
			return new JsonrpcResponseError(call.getRequest().getId(), ex);
		} else {
			return new JsonrpcResponseError(call.getRequest().getId(), t.getMessage());
		}
	}

	private static final Key<Integer> DEPTH = new Key<Integer>("depth", Integer.class);
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static final Key<List<JsonHandlerTrace>> TRACE = (Key<List<JsonHandlerTrace>>) new Key("trace", List.class);

	private static record JsonHandlerTrace(//
			String method, //
			boolean exists //
	) {

	}

	/**
	 * Handles a {@link Call}.
	 * 
	 * <p>
	 * A {@link Call} is usually created and passed to a builder when a request from
	 * an user is called in the UI or via code directly (BackendController,
	 * WebsocketController, RestApi, ...).
	 * 
	 * @param call the call to handle
	 */
	public void handle(Call<JsonrpcRequest, JsonrpcResponse> call) {
		var depth = call.get(DEPTH);
		final var trace = call.get(TRACE) != null ? call.get(TRACE) : new ArrayList<JsonHandlerTrace>();
		this.handle(//
				depth == null ? 0 : depth, //
				trace, //
				call //
		);

		if (depth != null) {
			return;
		}

		if (!this.isDebug()) {
			return;
		}
		this.log.info("Debug info for Request " + call.getRequest().getId() + "" + System.lineSeparator() //
				+ IntStream.range(0, trace.size()) //
						.mapToObj(i -> {
							var t = trace.get(i);
							return Strings.repeat(" ", i) + "\\ " + t.method() + (t.exists() ? "" : " (missing)");
						}).collect(joining(System.lineSeparator())) //
				+ System.lineSeparator() + Strings.repeat(" ", trace.size()) + " -> "//
				+ (call.getResponse() == null ? "Missing Response" : call.getResponse().toJsonObject()));
	}

	private void handle(//
			final int depth, //
			final List<JsonHandlerTrace> trace, //
			Call<JsonrpcRequest, JsonrpcResponse> call //
	) {
		final var endpoint = this.endpoints.get(call.getRequest().getMethod());
		trace.add(new JsonHandlerTrace(call.getRequest().getMethod(), endpoint != null));
		call.put(DEPTH, depth + 1);
		call.put(TRACE, trace);

		if (endpoint == null) {
			// TODO later for notifications there should not be a response
			call.setResponse(new JsonrpcResponseError(call.getRequest().getId(),
					"Endpoint with method \"" + call.getRequest().getMethod() + "\" is not defined!"));
			return;
		}

		// handle request
		endpoint.handle(this, call);
	}

}