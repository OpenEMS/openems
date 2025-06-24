package io.openems.edge.edge2edge.websocket.bridge;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithPasswordRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.type.SetChannelValue;
import io.openems.common.jsonrpc.type.SubscribeChannelUpdate;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.PasswordUtils;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.CreateAccountFromSetupKey;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.ReplaceUser;
import io.openems.edge.common.jsonapi.Subrequest;
import io.openems.edge.common.jsonapi.Subrequest.Subroute;
import io.openems.edge.edge2edge.websocket.bridge.jsonrpc.AuthenticateToRemoteEdge;
import io.openems.edge.edge2edge.websocket.bridge.jsonrpc.GetConnectionState;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Bridge.Edge2Edge.Websocket", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Edge2EdgeWebsocketBridgeImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, Edge2EdgeWebsocketBridge, ComponentJsonApi {

	private record Subscriber(ChannelSubscriber channelSubscriber, Consumer<Map<ChannelAddress, JsonElement>> onData) {

	}

	private final Logger log = LoggerFactory.getLogger(Edge2EdgeWebsocketBridgeImpl.class);

	private volatile ConnectionState connectionState = ConnectionState.NOT_CONNECTED;
	private WebsocketClient client;

	private volatile Config config;
	private final AtomicInteger subscribeChannelsCounter = new AtomicInteger(1);
	private final Map<ChannelAddress, Integer> subscribedChannels = new ConcurrentHashMap<>();

	private List<Consumer<EdgeConfig>> onEdgeConfigChangeListener = new ArrayList<>();
	private List<Runnable> onChannelChangeListener = new ArrayList<>();
	private List<BiConsumer<ConnectionState, ConnectionState>> onConnectionStateChangeListener = new CopyOnWriteArrayList<>();
	private List<Subscriber> subscriber = new CopyOnWriteArrayList<>();

	@Reference
	private ConfigurationAdmin configurationAdmin;

	public Edge2EdgeWebsocketBridgeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Edge2EdgeWebsocketBridge.ChannelId.values() //
		);

		final var channelSubscriber = new ChannelSubscriber();
		final var sumStateChannel = new ChannelAddress("_sum", "State");
		this.onConnectionStateChangeListener.add((prev, curr) -> {
			this._setConnectionState(curr.option);

			switch (curr) {
			case NOT_CONNECTED, CONNECTING -> {
				this._setNoConnection(true);
				this._setNotAuthenticated(false);
				channelSubscriber.unsubscribeAll();
			}
			case AUTHENTICATING -> {
				this._setNoConnection(false);
				this._setNotAuthenticated(true);
			}
			case CONNECTED -> {
				this._setNotAuthenticated(false);
				channelSubscriber.subscribeChannel(sumStateChannel);
			}
			}
		});
		this.onConnectionStateChangeListener.add((prev, curr) -> {
			if (curr != ConnectionState.AUTHENTICATING) {
				return;
			}

			this.client
					.sendRequest(
							new AuthenticateWithPasswordRequest(Optional.of(this.id()), this.config.remotePassword()))
					.thenCompose(response -> {
						return this.sendRequest(GenericJsonrpcRequest.createRequest(new SubscribeChannelUpdate(),
								new SubscribeChannelUpdate.Request(true)));
					}) //
					.whenComplete((response, error) -> {
						if (response != null) {
							this.setConnectionState(ConnectionState.CONNECTED);
							return;
						}
						this.log.error("Unable to authenticate to remote edge", error);
					});
		});

		this.addChannelSubscriber(channelSubscriber, values -> {
			final var remoteSumState = values.get(sumStateChannel);
			if (remoteSumState == null || remoteSumState.isJsonNull()) {
				this._setRemoteSumState(null);
				return;
			}

			this._setRemoteSumState(Level.fromValue(remoteSumState.getAsInt()).orElse(null));
		});

		this.getRemoteSumStateChannel().onChange((previousValue, newValue) -> {
			final var level = newValue.asOptional() //
					.flatMap(Level::fromValue) //
					.orElse(null);

			this._setRemoteSumStateFault(level == Level.FAULT);
			this._setRemoteSumStateWarning(level == Level.WARNING);
			this._setRemoteSumStateInfo(level == Level.INFO);
			this._setRemoteSumStateOk(level == Level.OK);
		});
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		this.client = new WebsocketClient(config.id(), URI.create("ws://" + config.ip() + ":" + config.port()),
				emptyMap(), AbstractWebsocketClient.NO_PROXY, this::setConnectionState, this::onCurrentData,
				this::onEdgeConfig, this::onChannelChange);

		this.client.start();
	}

	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
		this.client.stop();
	}

	private synchronized void setConnectionState(ConnectionState connectionState) {
		if (this.connectionState == connectionState) {
			return;
		}
		final var previousState = this.connectionState;
		this.connectionState = connectionState;

		for (var listener : this.onConnectionStateChangeListener) {
			try {
				listener.accept(previousState, this.connectionState);
			} catch (Exception e) {
				this.log.error("Error in connection state listener", e);
			}
		}
	}

	@Override
	public Disposable addChannelSubscriber(//
			final ChannelSubscriber channelSubscriber, //
			final Consumer<Map<ChannelAddress, JsonElement>> onData //
	) {
		final var subscriber = new Subscriber(channelSubscriber, onData);
		this.subscriber.add(subscriber);

		final var subscribeListener = channelSubscriber.addSubscribeListener(t -> {
			var change = false;
			for (var channelAddress : t) {
				final var value = this.subscribedChannels.compute(channelAddress, (channel, current) -> {
					if (current == null) {
						return 1;
					}

					return current + 1;
				});

				if (value == 1) {
					change = true;
				}
			}

			if (change) {
				this.updateSubscribe();
			}
		});

		final var unsubscribeListener = channelSubscriber.addUnsubscribeListener(t -> {
			var change = false;
			for (var channelAddress : t) {
				final var value = this.subscribedChannels.compute(channelAddress, (channel, current) -> {
					if (current == null) {
						return null;
					}

					if (--current <= 0) {
						return null;
					}

					return current;
				});

				if (value == null) {
					change = true;
				}
			}

			if (change) {
				this.updateSubscribe();
			}
		});

		return () -> {
			this.subscriber.remove(subscriber);
			final var subscribedChannels = channelSubscriber.getSubscribedChannels();
			channelSubscriber.removeSubscribeListener(subscribeListener);
			channelSubscriber.removeUnsubscribeListener(unsubscribeListener);
			unsubscribeListener.accept(subscribedChannels);
		};
	}

	private void onCurrentData(Map<ChannelAddress, JsonElement> currentData) {
		for (var subscriber : this.subscriber) {
			final var data = subscriber.channelSubscriber.getSubscribedChannels().stream() //
					.filter(currentData::containsKey) //
					.collect(toMap(Function.identity(), currentData::get));
			if (data.isEmpty()) {
				continue;
			}
			subscriber.onData().accept(data);
		}
	}

	private void onEdgeConfig(EdgeConfig edgeConfig) {
		for (var listener : this.onEdgeConfigChangeListener) {
			listener.accept(edgeConfig);
		}
	}

	private void onChannelChange() {
		for (var listener : this.onChannelChangeListener) {
			listener.run();
		}
	}

	@Override
	public void setChannelValue(String componentId, String channelId, JsonElement value) {
		this.sendRequest(GenericJsonrpcRequest.createRequest(new SetChannelValue(),
				new SetChannelValue.Request(componentId, channelId, value)));
	}

	@Override
	public BiConsumer<ConnectionState, ConnectionState> addOnConnectionStateChangeListener(
			BiConsumer<ConnectionState, ConnectionState> listener) {
		this.onConnectionStateChangeListener.add(listener);
		return listener;
	}

	@Override
	public boolean removeOnConnectionStateChangeListener(BiConsumer<ConnectionState, ConnectionState> listener) {
		return this.onConnectionStateChangeListener.remove(listener);
	}

	@Override
	public Consumer<EdgeConfig> addOnEdgeConfigChangeListener(Consumer<EdgeConfig> listener) {
		this.onEdgeConfigChangeListener.add(listener);
		return listener;
	}

	@Override
	public boolean removeOnEdgeConfigChangeListener(Consumer<EdgeConfig> listener) {
		return this.onEdgeConfigChangeListener.remove(listener);
	}

	@Override
	public Runnable addOnChannelChangeListener(Runnable listener) {
		this.onChannelChangeListener.add(listener);
		return listener;
	}

	@Override
	public boolean removeOnChannelChangeListener(Runnable listener) {
		return this.onChannelChangeListener.remove(listener);
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> sendRequest(JsonrpcRequest request) {
		return this.client.sendRequest(new EdgeRpcRequest("", request));
	}

	@Override
	public ConnectionState getConnectionState() {
		return this.connectionState;
	}

	@Override
	public String debugLog() {
		return "ConnectionState: " + this.connectionState.name() //
				+ ", Channels[" + this.subscribedChannels.size() + "]";
	}

	private void updateSubscribe() {
		final var subscribeChannelsRequest = new SubscribeChannelsRequest(
				this.subscribeChannelsCounter.getAndIncrement());

		subscribeChannelsRequest.getChannels()
				.addAll(this.subscribedChannels.keySet().stream().map(ChannelAddress::toString).toList());

		this.sendRequest(subscribeChannelsRequest);
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.delegate("remoteEdgeRpc", (b, call) -> {
			final var user = call.get(EdgeKeys.USER_KEY);
			try {
				final var request = new ReplaceUser.Request(user.getRole(), user.getLanguage(),
						call.getRequest().getParams().get("payload"));

				final var response = this.sendRequest(GenericJsonrpcRequest.createRequest(new ReplaceUser(), request))
						.get();
				call.setResponse(response);
			} catch (InterruptedException | ExecutionException e) {
				call.setResponse(
						new JsonrpcResponseError(call.getRequest().getId(), new OpenemsException(e.getMessage())));
			}
		}, () -> {
			final var subrequest = new Subrequest(new JsonObject());
			subrequest.getSubrouteToBuilder().add(new Subroute(new String[] {}, null, () -> {
				try {
					var response = this.sendRequest(new GenericJsonrpcRequest("routes", new JsonObject())).get()
							.getResult();

					final var a = JsonrpcResponseSuccess.from(response.get("payload").getAsJsonObject());

					return a.getResult().get("endpoints").getAsJsonArray();
				} catch (InterruptedException | ExecutionException | OpenemsNamedException e) {
					this.log.info("Unable to get remote endpoints.", e);
					return new JsonArray();
				}
			}));
			return List.of(subrequest);
		});

		builder.handleRequest(new AuthenticateToRemoteEdge(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.INSTALLER));
		}, call -> {

			final var password = PasswordUtils.generateRandomPassword(24);

			this.client
					.sendRequest(GenericJsonrpcRequest.createRequest(new CreateAccountFromSetupKey(),
							new CreateAccountFromSetupKey.Request(call.getRequest().setupKey(), this.id(), password)))
					.get();

			this.saveRemotePassword(password);

			return EmptyObject.INSTANCE;
		});

		builder.handleRequest(new GetConnectionState(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
		}, call -> {
			return new GetConnectionState.Response(this.connectionState);
		});
	}

	private void saveRemotePassword(String password) {
		try {
			final var configuration = this.configurationAdmin.getConfiguration(this.servicePid(), "?");
			final var properties = configuration.getProperties();
			properties.put("remotePassword", password);
			configuration.updateIfDifferent(properties);
		} catch (IOException e) {
			this.log.error("Unable to update configuration", e);
		}
	}

}
