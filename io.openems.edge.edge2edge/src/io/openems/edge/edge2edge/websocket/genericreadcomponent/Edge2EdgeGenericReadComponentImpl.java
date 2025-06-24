package io.openems.edge.edge2edge.websocket.genericreadcomponent;

import static java.util.stream.Collectors.toSet;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetChannelsOfComponent.ChannelRecord;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.edge2edge.websocket.Edge2EdgeWebsocket;
import io.openems.edge.edge2edge.websocket.bridge.BridgeComponentStateHandler;
import io.openems.edge.edge2edge.websocket.bridge.ChannelSubscriber;
import io.openems.edge.edge2edge.websocket.bridge.Disposable;
import io.openems.edge.edge2edge.websocket.bridge.Edge2EdgeWebsocketBridge;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Edge2Edge.Websocket.GenericReadComponent", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Edge2EdgeGenericReadComponentImpl extends AbstractOpenemsComponent
		implements Edge2EdgeGenericReadComponent, Edge2EdgeWebsocket, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(Edge2EdgeGenericReadComponentImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Power power;

	private Config config;
	private final ChannelSubscriber channelSubscriber = new ChannelSubscriber();
	private final BridgeComponentStateHandler bridgeStateHandler = new BridgeComponentStateHandler();
	private Disposable channelSubscriberDisposable;

	/**
	 * Binds the {@link Edge2EdgeWebsocketBridge}.
	 * 
	 * @param bridge the bridge to bind
	 */
	@Reference(policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.OPTIONAL)
	public void bindBridge(Edge2EdgeWebsocketBridge bridge) {
		this.bridgeStateHandler.bindBridge(bridge);

		this.channelSubscriberDisposable = bridge.addChannelSubscriber(this.channelSubscriber, t -> {
			for (var entry : t.entrySet()) {
				try {
					final var channel = this.channel(entry.getKey().getChannelId());
					if (channel.getType() == OpenemsType.BOOLEAN) {
						final var value = JsonUtils.getAsType(OpenemsType.INTEGER, entry.getValue());
						channel.setNextValue(value == null ? null : value.equals(1));
						continue;
					}
					channel.setNextValue(JsonUtils.getAsType(channel.getType(), entry.getValue()));
				} catch (IllegalArgumentException e) {
					this.channelSubscriber.unsubscribeChannel(entry.getKey());
				} catch (OpenemsNamedException e) {
					this.log.warn("Unable to parse value to type", e);
				}
			}
		});
	}

	/**
	 * Unbinds the {@link Edge2EdgeWebsocketBridge}.
	 * 
	 * @param bridge the bridge to unbind
	 */
	public void unbindBridge(Edge2EdgeWebsocketBridge bridge) {
		this.bridgeStateHandler.unbindBridge(bridge);
		this.channelSubscriberDisposable.dispose();
	}

	public Edge2EdgeGenericReadComponentImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Edge2EdgeWebsocket.ChannelId.values(), //
				Edge2EdgeGenericReadComponent.ChannelId.values() //
		);

		this.bridgeStateHandler.addOnStateChangeListener((prev, current) -> {
			switch (current) {
			case NOT_CONNECTED -> {
				this._setRemoteNoComponentFault(false);
				this._setRemoteNoConnection(true);
			}
			case CONNECTED -> {
				this._setRemoteNoComponentFault(false);
				this._setRemoteNoConnection(false);
			}
			case COMPONENT_NOT_AVAILABLE -> {
				this._setRemoteNoComponentFault(true);
				this._setRemoteNoConnection(false);
			}
			}
		});

		this.bridgeStateHandler.addOnSubscribeListener((bridge, channels) -> {
			for (var channel : channels) {
				this.createChannelIfNotExisting(channel);
			}

			final var c = channels.stream() //
					.filter(t -> t.accessMode() != AccessMode.WRITE_ONLY) //
					.map(t -> new ChannelAddress(this.config.remoteComponentId(), t.id())).collect(toSet());

			this.channelSubscriber.setSubscribeChannels(c);
		});

		this.bridgeStateHandler.addOnUnsubscribeListener(bridge -> {
			final var unsubscribedChannels = this.channelSubscriber.unsubscribeAll();

			// reset channel values
			for (var channelAddress : unsubscribedChannels) {
				try {
					final var channel = this.channel(channelAddress.getChannelId());
					channel.setNextValue(null);
				} catch (IllegalArgumentException e) {
					// channel does not exist
				}
			}
		});
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Bridge", config.bridge_id());

		this.bridgeStateHandler.updateComponentId(config.enabled() ? config.remoteComponentId() : null);
	}

	@Override
	public String debugLog() {
		return this.bridgeStateHandler.toString();
	}

	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
		this.bridgeStateHandler.deactivate();
	}

	@SuppressWarnings("deprecation")
	private void createChannelIfNotExisting(ChannelRecord channel) {
		if (this._channel(channel.id()) != null) {
			return;
		}

		final var doc = switch (channel.category()) {
		case ENUM -> Doc.of(channel.type());
		case OPENEMS_TYPE -> Doc.of(channel.type()) //
				.unit(channel.unit());
		case STATE -> Doc.of(channel.level());
		};
		final var channelName = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, channel.id());
		this.addChannel(new ChannelIdImpl(channelName, doc.persistencePriority(channel.persistencePriority()) //
				.accessMode(AccessMode.READ_ONLY) //
				.text(channel.text())));
	}
}
