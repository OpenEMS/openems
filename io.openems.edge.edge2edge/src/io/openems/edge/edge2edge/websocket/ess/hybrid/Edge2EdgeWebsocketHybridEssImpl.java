package io.openems.edge.edge2edge.websocket.ess.hybrid;

import static io.openems.common.utils.IntUtils.maxInteger;
import static io.openems.common.utils.IntUtils.minInteger;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static java.util.stream.Collectors.toSet;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonPrimitive;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.ChannelCategory;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.function.Disposable;
import io.openems.common.jsonrpc.request.GetChannelsOfComponent;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.edge2edge.websocket.Edge2EdgeWebsocket;
import io.openems.edge.edge2edge.websocket.bridge.BridgeComponentStateHandler;
import io.openems.edge.edge2edge.websocket.bridge.ChannelSubscriber;
import io.openems.edge.edge2edge.websocket.bridge.Edge2EdgeWebsocketBridge;
import io.openems.edge.edge2edge.websocket.dccharger.Edge2EdgeWebsocketDcCharger;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Edge2Edge.Websocket.HybridEss", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics(TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
@GenerateTargetsFromReferences({ "Bridge", "DcCharger" })
public class Edge2EdgeWebsocketHybridEssImpl extends AbstractOpenemsComponent
		implements ManagedSymmetricEss, AsymmetricEss, HybridEss, SymmetricEss, Edge2EdgeWebsocket,
		Edge2EdgeWebsocketHybridEss, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(Edge2EdgeWebsocketHybridEssImpl.class);

	@Reference(cardinality = OPTIONAL, policyOption = GREEDY, policy = DYNAMIC)
	private volatile Power power;

	private Config config;
	private final ChannelSubscriber channelSubscriber = new ChannelSubscriber();
	private final BridgeComponentStateHandler bridgeStateHandler = new BridgeComponentStateHandler();
	private Disposable channelSubscriberDisposable;
	private final List<EssDcCharger> dcChargers = new CopyOnWriteArrayList<>();

	@Reference(name = "DcCharger", policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE, //
			target = "(&(id=${config.dcChargerIds})(enabled=true))")
	protected void addDcCharger(Edge2EdgeWebsocketDcCharger dcCharger) {
		this.dcChargers.add(dcCharger);
	}

	protected void removeDcCharger(EssDcCharger dcCharger) {
		this.dcChargers.remove(dcCharger);
	}

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

	public Edge2EdgeWebsocketHybridEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Edge2EdgeWebsocket.ChannelId.values(), //
				Edge2EdgeWebsocketHybridEss.ChannelId.values() //
		);
		this._setMaxApparentPower(Integer.MAX_VALUE); // has no effect, as long as AllowedCharge/DischargePower are null

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
	public void handleEvent(Event event) {
		if (this.isEnabled() && TOPIC_CYCLE_AFTER_PROCESS_IMAGE.equals(event.getTopic())) {
			this.updateDerivedPvChannels();
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|DC:" + this.getDcDischargePower().asString() //
				+ "|PV:" + this.getPvProduction() //
				+ "|Allowed:" //
				+ maxInteger(//
						this.getAllowedChargePower().get(), //
						TypeUtils.multiply(this.getMaxApparentPower().get(), -1))
				+ ";" //
				+ minInteger(//
						this.getAllowedDischargePower().get(), //
						this.getMaxApparentPower().get()) //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Power getPower() {
		final var powerRef = this.power;
		if (powerRef == null) {
			throw new OpenemsRuntimeException("Ess.Power is not yet available");
		}
		return powerRef;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		if (this.config.remoteAccessMode() == AccessMode.READ_ONLY) {
			return;
		}

		this.bridgeStateHandler.setChannelValue(ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS.id(),
				new JsonPrimitive(activePower));
		this.bridgeStateHandler.setChannelValue(ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS.id(),
				new JsonPrimitive(reactivePower));
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	private IntegerReadChannel getPvProductionChannel() {
		return this.channel(Edge2EdgeWebsocketHybridEss.ChannelId.PV_PRODUCTION);
	}

	public Integer getPvProduction() {
		return this.getPvProductionChannel().value().get();
	}

	private IntegerReadChannel getSurplusPowerChannel() {
		return this.channel(Edge2EdgeWebsocketHybridEss.ChannelId.SURPLUS_POWER_VALUE);
	}

	@Override
	public Integer getSurplusPower() {
		return this.getSurplusPowerChannel().value().get();
	}

	private void updateDerivedPvChannels() {
		final var chargerPowers = this.dcChargers.stream() //
				.map(dcCharger -> dcCharger.getActualPower().get()) //
				.filter(Objects::nonNull) //
				.toList();
		if (chargerPowers.isEmpty()) {
			setValue(this, Edge2EdgeWebsocketHybridEss.ChannelId.PV_PRODUCTION, null);
			setValue(this, Edge2EdgeWebsocketHybridEss.ChannelId.SURPLUS_POWER_VALUE, null);
			return;
		}

		final var pvProduction = chargerPowers.stream() //
				.mapToInt(Integer::intValue) //
				.sum();
		setValue(this, Edge2EdgeWebsocketHybridEss.ChannelId.PV_PRODUCTION, pvProduction);

		final var surplus = pvProduction + this.getAllowedChargePower().orElse(0);
		setValue(this, Edge2EdgeWebsocketHybridEss.ChannelId.SURPLUS_POWER_VALUE,
				pvProduction < 100 || surplus < 0 ? null : surplus);
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
