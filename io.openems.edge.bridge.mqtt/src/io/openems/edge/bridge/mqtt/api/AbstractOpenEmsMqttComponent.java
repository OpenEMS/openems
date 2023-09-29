package io.openems.edge.bridge.mqtt.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mqtt.api.payloads.Payload;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * An abstract OpenEmsMqttComponent. OpenEms Components that connects one or
 * multiple referenced OpenEmsComponents to a Broker by providing publish/subscribe tasks
 * should extend this component.
 * When implementing your own OpenEmsMqttComponent, it is recommended to also consider putting a "channels" property
 * into the configuration.
 * This is used to display all channel of the referenced Component.
 */
public abstract class AbstractOpenEmsMqttComponent extends AbstractOpenemsComponent
		implements OpenemsComponent, MqttComponent {

	private static final String CONFIG_SPLITTER = "=";
	private final AtomicReference<BridgeMqtt> mqtt = new AtomicReference<>(null);
	private final AtomicReference<OpenemsComponent> referenceComponent = new AtomicReference<>(null);

	private MqttProtocol protocol = null;

	private static final String MEMBER_MQTT = "Mqtt";

	private static final String MEMBER_REFERENCED_COMPONENT = "ReferencedComponent";

	protected AbstractOpenEmsMqttComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() for Mqtt components!");
	}

	protected void activate(String id) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	/**
	 * Call this method from Component implementations activate().
	 *
	 * @param context     ComponentContext of this component. Receive it from
	 *                    parameter for @Activate
	 * @param id          ID of this component. Typically 'config.id()'
	 * @param alias       Human-readable name of this Component. Typically
	 *                    'config.alias()'. Defaults to 'id' if empty
	 * @param enabled     Whether the component should be enabled. Typically
	 *                    'config.enabled()'
	 * @param cm          An instance of ConfigurationAdmin. Receive it
	 *                    using @Reference
	 * @param mqttId      The ID of the Mqtt bridge. Typically 'config.mqtt_id()'
	 * @param referenceId the Component referenced by the MqttComponent.
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 * @throws OpenemsException on error
	 */
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled,
			ConfigurationAdmin cm, String mqttId, String referenceId) throws OpenemsException {
		super.activate(context, id, alias, enabled);
		// Initialize MqttCommunicationFailed State
		this._setMqttCommunicationFailed(false);
		this._setConfigurationFail(false);
		// update filter for 'Mqtt'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), MEMBER_MQTT, mqttId)) {
			return true;
		}
		// update filter for 'ReferencedComponent'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), MEMBER_REFERENCED_COMPONENT, referenceId)) {
			return true;
		}
		var mqtt = this.mqtt.get();
		if (this.isEnabled() && mqtt != null) {
			mqtt.addMqttProtocol(this.id(), this.getMqttProtocol());
		}
		return false;
	}

	/**
	 * Call this method from Component implementations modified().
	 *
	 * @param context     ComponentContext of this component. Receive it from
	 *                    parameter for @Modified
	 * @param id          ID of this component. Typically 'config.id()'
	 * @param alias       Human-readable name of this Component. Typically
	 *                    'config.alias()'. Defaults to 'id' if empty
	 * @param enabled     Whether the component should be enabled. Typically
	 *                    'config.enabled()'
	 * @param cm          An instance of ConfigurationAdmin. Receive it
	 *                    using @Reference
	 * @param mqttId    The ID of the mqtt bridge. Typically 'config.mqtt_id()'
	 * @param referenceId The component the MqttComponent is Referencing
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 * @throws OpenemsException on error
	 */
	protected boolean modified(ComponentContext context, String id, String alias, boolean enabled,
			ConfigurationAdmin cm, String mqttId, String referenceId) throws OpenemsException {
		super.modified(context, id, alias, enabled);
		this._setMqttCommunicationFailed(false);
		this._setConfigurationFail(false);
		// update filter for 'Mqtt'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), MEMBER_MQTT, mqttId)) {
			return true;
		}
		// update filter for 'ReferencedComponent'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), MEMBER_REFERENCED_COMPONENT, referenceId)) {
			return true;
		}
		var mqtt = this.mqtt.get();
		mqtt.removeMqttProtocol(this.id());
		if (this.isEnabled()) {
			mqtt.addMqttProtocol(this.id(), this.getMqttProtocol());
		}
		return false;
	}

	/**
	 * This method updates the Component configuration, by collecting all channel of
	 * the {@link #referenceComponent}, and putting them into the "channels"
	 * property of the component.
	 * 
	 * @param channelConfigLength the length of the channels property
	 * @param cm                  An instance of ConfigurationAdmin. Receive it *
	 *                            using @Reference
	 * @throws OpenemsException thrown when the configuration couldn't be updated
	 *                          (IOException)
	 */
	protected void updateChannelInConfig(int channelConfigLength, ConfigurationAdmin cm) throws OpenemsException {
		var refComponentChannel = this.referenceComponent.get().channels();
		if (refComponentChannel.size() != channelConfigLength) {
			try {
				var configuration = cm.getConfiguration(this.servicePid(), "?");
				List<String> channelIds = new ArrayList<>();
				refComponentChannel.stream().sorted(Comparator.comparing(a -> a.channelId().id())).toList() //
						.forEach(entry -> channelIds.add(entry.channelId().id()));
				Dictionary<String, Object> properties = configuration.getProperties();
				properties.put("channels", this.propertyInput(Arrays.toString(channelIds.toArray())));
				configuration.update(properties);
			} catch (IOException e) {
				throw new OpenemsException("Couldn't update Config of: " + this.id());
			}
		}
	}

	/**
	 * Workaround for OSGi Arrays to String -> Otherwise Property input won't work
	 * correctly.
	 *
	 * @param channelIds ChannelIds
	 * @return String Array which will be put to new Config
	 */
	private String[] propertyInput(String channelIds) {
		channelIds = channelIds.replaceAll("\\[", "");
		channelIds = channelIds.replaceAll("]", "");
		channelIds = channelIds.replace(" ", "");
		return channelIds.split(",");
	}

	/**
	 * Gets the {@link MqttProtocol}. Creates it via {@link #defineMqttProtocol()}
	 * if it does not yet exist.
	 *
	 * @return the {@link MqttProtocol}
	 * @throws OpenemsException on error
	 */
	protected MqttProtocol getMqttProtocol() throws OpenemsException {
		var protocol = this.protocol;
		if (protocol != null) {
			return protocol;
		}
		this.protocol = this.defineMqttProtocol();
		return this.protocol;
	}

	/**
	 * Defines the Mqtt protocol.
	 *
	 * @return the MqttProtocol
	 * @throws OpenemsException on error
	 */
	protected abstract MqttProtocol defineMqttProtocol() throws OpenemsException;

	/**
	 * Set the Mqtt bridge. Should be called by @Reference
	 *
	 * @param mqtt the BridgeMqtt Reference
	 */
	protected void setMqtt(BridgeMqtt mqtt) {
		this.mqtt.set(mqtt);
	}

	/**
	 * Unset the Mqtt bridge. Should be called by @Reference
	 *
	 * @param mqtt the BridgeMqtt Reference
	 */
	protected void unsetMqtt(BridgeMqtt mqtt) {
		this.mqtt.compareAndSet(mqtt, null);
		if (mqtt != null) {
			mqtt.removeMqttProtocol(this.id());
		}
	}

	/**
	 * Gets the Mqtt-Bridge.
	 *
	 * @return the {@link BridgeMqtt}.
	 */
	public BridgeMqtt getBridgeMqtt() {
		return this.mqtt.get();
	}

	/**
	 * Set the Reference Component. Should be called by @Reference
	 *
	 * @param component the BridgeMqtt Reference
	 */
	protected void setReferenceComponent(OpenemsComponent component) {
		this.referenceComponent.set(component);
	}

	/**
	 * Unset the referenced Component. Should be called by @Reference
	 *
	 * @param component the OpenemsComponent Reference
	 */
	protected void unsetReferenceComponent(OpenemsComponent component) {
		this.referenceComponent.compareAndSet(component, null);
	}

	/**
	 * Gets the ReferencedComponent.
	 *
	 * @return the {@link OpenemsComponent}.
	 */
	@Override
	public OpenemsComponent getReferenceComponent() {
		return this.referenceComponent.get();
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		var mqtt = this.mqtt.getAndSet(null);
		if (mqtt != null) {
			mqtt.removeMqttProtocol(this.id());
		}
	}

	/**
	 * Creates the Configured KeyToChannelId Map used within {@link Payload}. E.g.
	 * Channel ActivePower should be published to "Foo" 1 entry of the configuration
	 * should look like "Foo=ActivePower".
	 * A basic Payload would create a Json with an entry being {"foo": 1000}, if ActivePower was 1000 W.
	 * 
	 * @param keyToChannelConfig Configuration of a Key to Channel value.
	 * @return the Map ChannelId to String.
	 */
	protected Map<io.openems.edge.common.channel.ChannelId, String> createMap(List<String> keyToChannelConfig)
			throws OpenemsException {

		Map<io.openems.edge.common.channel.ChannelId, String> idToKeyMap = new HashMap<>();
		var channels = this.referenceComponent.get().channels();
		try {
			keyToChannelConfig.forEach(entry -> {
				String[] config = entry.split(CONFIG_SPLITTER);
				if (config.length != 2) {
					this._setConfigurationFail(true);
					throw new RuntimeException(new OpenemsException(
							"Configuration Issue in: " + this.id() + " The Configuration of entry: " + entry));
				}
				var channelOpt = channels.stream().filter(channel -> //
								channel.channelId().id().equals(config[1]))
						.findAny(); //
				if (channelOpt.isPresent()) {
					idToKeyMap.put(channelOpt.get().channelId(), config[0]);
				} else {
					try {
						throw new OpenemsException("Unable to add Channel: " + config[1]);
					} catch (OpenemsException e) {
						throw new RuntimeException(e);
					}
				}
			});
			this.getConfigurationFailedChannel().setNextValue(false);
		} catch (RuntimeException e) {
			if (e.getCause()instanceof OpenemsException ex) {
				throw ex;
			}
			this.getConfigurationFailedChannel().setNextValue(true);
		}
		return idToKeyMap;
	}
}
