package io.openems.edge.edge2edge.websocket.dccharger;

import static java.util.stream.Collectors.toSet;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.ChannelCategory;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.Disposable;
import io.openems.common.jsonrpc.request.GetChannelsOfComponent;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.edge2edge.websocket.Edge2EdgeWebsocket;
import io.openems.edge.edge2edge.websocket.bridge.BridgeComponentStateHandler;
import io.openems.edge.edge2edge.websocket.bridge.ChannelSubscriber;
import io.openems.edge.edge2edge.websocket.bridge.Edge2EdgeWebsocketBridge;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Edge2Edge.Websocket.DcCharger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Bridge")
public class Edge2EdgeWebsocketDcChargerImpl extends AbstractOpenemsComponent
		implements EssDcCharger, Edge2EdgeWebsocket, Edge2EdgeWebsocketDcCharger, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(Edge2EdgeWebsocketDcChargerImpl.class);

	private Config config;
	private final ChannelSubscriber channelSubscriber = new ChannelSubscriber();
	private final BridgeComponentStateHandler bridgeStateHandler = new BridgeComponentStateHandler();
	private Disposable channelSubscriberDisposable;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL, //
			target = "(&(id=${config.bridge_id})(enabled=true))")
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
				} catch (IllegalArgumentException _) {
					this.channelSubscriber.unsubscribeChannel(entry.getKey());
				} catch (OpenemsNamedException e) {
					this.log.info("Unable to parse remote channel value.", e);
				}
			}
		});
	}

	public void unbindBridge(Edge2EdgeWebsocketBridge bridge) {
		this.bridgeStateHandler.unbindBridge(bridge);
		this.channelSubscriberDisposable.dispose();
	}

	public Edge2EdgeWebsocketDcChargerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				Edge2EdgeWebsocket.ChannelId.values(), //
				Edge2EdgeWebsocketDcCharger.ChannelId.values() //
		);

		this.bridgeStateHandler.addOnStateChangeListener((_, current) -> {
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

		this.bridgeStateHandler.addOnSubscribeListener((_, channels) -> {
			final var c = channels.stream() //
					.filter(t -> t.accessMode() != AccessMode.WRITE_ONLY) //
					.filter(t -> this.hasChannel(t.id()) || t.category() == ChannelCategory.STATE) //
					.map(t -> new ChannelAddress(this.config.remoteComponentId(), t.id())) //
					.collect(toSet());

			for (var channel : channels) {
				if (channel.category() != ChannelCategory.STATE) {
					continue;
				}
				this.createChannelIfNotExisting(channel);
			}

			this.channelSubscriber.setSubscribeChannels(c);
		});

		this.bridgeStateHandler.addOnUnsubscribeListener(_ -> {
			this.channelSubscriber.unsubscribeAll();
			this.resetAllChannelsToNull();
		});
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.bridgeStateHandler.updateComponentId(config.enabled() ? config.remoteComponentId() : null);
	}

	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
		this.bridgeStateHandler.deactivate();
	}

	@Override
	public final String debugLog() {
		return "L:" + this.getActualPower().asString();
	}

	private void createChannelIfNotExisting(GetChannelsOfComponent.ChannelRecord channel) {
		if (this.hasChannel(channel.id())) {
			return;
		}

		final var doc = switch (channel.category()) {
		case ENUM -> Doc.of(channel.type());
		case OPENEMS_TYPE -> Doc.of(channel.type()) //
				.unit(channel.unit());
		case STATE -> Doc.of(channel.level());
		};
		final var channelName = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, channel.id());
		this.addChannel(new io.openems.edge.common.channel.ChannelId.ChannelIdImpl(channelName,
				doc.persistencePriority(channel.persistencePriority()) //
						.accessMode(AccessMode.READ_ONLY) //
						.text(channel.text())));
	}
}
