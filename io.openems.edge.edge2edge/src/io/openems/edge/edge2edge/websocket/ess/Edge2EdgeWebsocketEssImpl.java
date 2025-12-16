package io.openems.edge.edge2edge.websocket.ess;

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
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.ChannelCategory;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.jsonrpc.request.GetChannelsOfComponent;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.edge2edge.websocket.Edge2EdgeWebsocket;
import io.openems.edge.edge2edge.websocket.bridge.BridgeComponentStateHandler;
import io.openems.edge.edge2edge.websocket.bridge.ChannelSubscriber;
import io.openems.edge.edge2edge.websocket.bridge.Disposable;
import io.openems.edge.edge2edge.websocket.bridge.Edge2EdgeWebsocketBridge;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Edge2Edge.Websocket.Ess", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Edge2EdgeWebsocketEssImpl extends AbstractOpenemsComponent implements ManagedSymmetricEss, AsymmetricEss,
		SymmetricEss, Edge2EdgeEss, Edge2EdgeWebsocket, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(Edge2EdgeWebsocketEssImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY, policy = ReferencePolicy.DYNAMIC)
	private volatile Power power;

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
					this.log.info("Unable to parse remote channel value.", e);
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

	public Edge2EdgeWebsocketEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Edge2EdgeWebsocket.ChannelId.values(), //
				Edge2EdgeEss.ChannelId.values() //
		);
		this._setMaxApparentPower(Integer.MAX_VALUE); // has no effect, as long as AllowedCharge/DischargePower are null

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

		this.getSetActivePowerEqualsChannel().onSetNextWrite(t -> {
			if (this.config.remoteAccessMode() == AccessMode.READ_ONLY) {
				return;
			}
			
			this.bridgeStateHandler.setChannelValue(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS.id(),
					new JsonPrimitive(t));
		});

		this.getSetReactivePowerEqualsChannel().onSetNextWrite(t -> {
			if (this.config.remoteAccessMode() == AccessMode.READ_ONLY) {
				return;
			}

			this.bridgeStateHandler.setChannelValue(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS.id(),
					new JsonPrimitive(t));
		});
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Bridge", config.bridge_id());

		this.bridgeStateHandler.updateComponentId(config.enabled() ? config.remoteComponentId() : null);
	}

	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
		this.bridgeStateHandler.deactivate();
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:"
				+ TypeUtils.max(this.getAllowedChargePower().get(),
						TypeUtils.multiply(this.getMaxApparentPower().get(), -1))
				+ ";" //
				+ TypeUtils.min(this.getAllowedDischargePower().get(), this.getMaxApparentPower().get()) //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Power getPower() {
		final var power = this.power;
		if (power == null) {
			throw new OpenemsRuntimeException("Ess.Power is not yet available");
		}
		return power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		this.setActivePowerEquals(activePower);
		this.setReactivePowerEquals(reactivePower);
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@SuppressWarnings("deprecation")
	private boolean hasChannel(String channelId) {
		return this._channel(channelId) != null;
	}

	@SuppressWarnings("deprecation")
	private void createChannelIfNotExisting(GetChannelsOfComponent.ChannelRecord channel) {
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
		this.addChannel(new io.openems.edge.common.channel.ChannelId.ChannelIdImpl(channelName,
				doc.persistencePriority(channel.persistencePriority()) //
						.accessMode(AccessMode.READ_ONLY) //
						.text(channel.text())));
	}

}
