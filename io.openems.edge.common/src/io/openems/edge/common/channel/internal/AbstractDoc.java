package io.openems.edge.common.channel.internal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiConsumer;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides static meta information for a {@link Channel} using Builder pattern.
 */
public abstract class AbstractDoc<T> implements Doc {

	private final OpenemsType type;

	protected AbstractDoc(OpenemsType type) {
		this.type = type;
	}

	/**
	 * Gets an instance of the correct subclass of myself.
	 *
	 * @return myself
	 */
	protected abstract AbstractDoc<T> self();

	@Override
	public OpenemsType getType() {
		return this.type;
	}

	/**
	 * Allowed {@link AccessMode} for this Channel.
	 */
	private AccessMode accessMode = AccessMode.READ_ONLY;

	/**
	 * Sets the {@link AccessMode} for the Channel.
	 *
	 * <p>
	 * This is validated on construction of the Channel by
	 * {@link AbstractReadChannel}
	 *
	 * @param accessMode the {@link AccessMode}
	 * @return myself
	 */
	public AbstractDoc<T> accessMode(AccessMode accessMode) {
		this.accessMode = accessMode;
		return this.self();
	}

	@Override
	public AccessMode getAccessMode() {
		return this.accessMode;
	}

	/**
	 * PersistencePriority for this Channel.
	 */
	private PersistencePriority persistencePriority = PersistencePriority.VERY_LOW;

	/**
	 * Sets the {@link PersistencePriority}. Defaults to
	 * {@link PersistencePriority#VERY_LOW}.
	 *
	 * <p>
	 * This parameter may be used by persistence services to decide, if the Channel
	 * should be persisted to the hard disk.
	 *
	 * @param persistencePriority the {@link PersistencePriority}
	 * @return myself
	 */
	public AbstractDoc<T> persistencePriority(PersistencePriority persistencePriority) {
		this.persistencePriority = persistencePriority;
		return this.self();
	}

	@Override
	public PersistencePriority getPersistencePriority() {
		return this.persistencePriority;
	}

	/*
	 * Initial Value
	 */
	private T initialValue = null;

	/**
	 * Initial-Value. Default: none
	 *
	 * @param initialValue the initial value
	 * @return myself
	 */
	public AbstractDoc<T> initialValue(T initialValue) {
		this.initialValue = initialValue;
		return this.self();
	}

	/**
	 * Gets the initial value.
	 *
	 * @return the initial value
	 */
	public T getInitialValue() {
		return this.initialValue;
	}

	/*
	 * Description
	 */
	private String text = "";

	@Override
	public AbstractDoc<T> text(String text) {
		this.text = text;
		return this.self();
	}

	@Override
	public String getText() {
		return this.text;
	}

	@Override
	public Unit getUnit() {
		return Unit.NONE;
	}

	/*
	 * Verbose Debug mode
	 */
	private boolean debug = false;

	/**
	 * Activates the more verbose debug mode.
	 *
	 * @return myself
	 */
	public AbstractDoc<T> debug() {
		this.debug = true;
		return this.self();
	}

	@Override
	public boolean isDebug() {
		return this.debug;
	}

	/*
	 * On Channel initialization Callback
	 */
	private final List<Consumer<Channel<T>>> onInitCallback = new CopyOnWriteArrayList<>();

	/**
	 * Provides a callback on initialization of the actual Channel.
	 *
	 * @param callback the method to call on initialization
	 * @return myself
	 */
	public AbstractDoc<T> onInit(Consumer<Channel<T>> callback) {
		this.onInitCallback.add(callback);
		return this.self();
	}

	/**
	 * Provides a callback on Channel onChange event.
	 * 
	 * <p>
	 * This is a convenience method to react on a
	 * {@link Channel#onChange(java.util.function.BiConsumer)} event
	 *
	 * @param <COMPONENT> the type of the {@link OpenemsComponent}
	 * @param callback    the method to call at value change event
	 * @return myself
	 */
	@SuppressWarnings("unchecked")
	public <COMPONENT extends OpenemsComponent> AbstractDoc<T> onChannelChange(Consumer<COMPONENT> callback) {
		this.onInitCallback.add(channel -> {
			channel.onChange((ignore, value) -> {
				callback.accept((COMPONENT) channel.getComponent());
			});
		});
		return this.self();
	}

	/**
	 * Provides a callback on Channel onChange event.
	 * 
	 * <p>
	 * This is a convenience method to react on a
	 * {@link Channel#onChange(BiConsumer)} event
	 *
	 * @param <COMPONENT> the type of the {@link OpenemsComponent}
	 * @param callback    the method to call at value change event, value is the new
	 *                    value after change
	 * @return myself
	 */
	@SuppressWarnings("unchecked")
	public <COMPONENT extends OpenemsComponent> AbstractDoc<T> onChannelChange(
			BiConsumer<COMPONENT, Value<T>> callback) {
		this.onInitCallback.add(channel -> {
			channel.onChange((ignore, value) -> {
				callback.accept((COMPONENT) channel.getComponent(), value);
			});
		});
		return this.self();
	}

	/**
	 * Provides a callback on Channel setNextValue event.
	 * 
	 * <p>
	 * This is a convenience method to react on a
	 * {@link Channel#onSetNextValue(Consumer)} event
	 *
	 * @param <COMPONENT> the type of the {@link OpenemsComponent}
	 * @param callback    the method to call at value change event, value is the new
	 *                    value after change
	 * @return myself
	 */
	@SuppressWarnings("unchecked")
	public <COMPONENT extends OpenemsComponent> AbstractDoc<T> onChannelSetNextValue(
			BiConsumer<COMPONENT, Value<T>> callback) {
		this.onInitCallback.add(channel -> {
			channel.onSetNextValue(value -> {
				callback.accept((COMPONENT) channel.getComponent(), value);
			});
		});
		return this.self();
	}

	/**
	 * Provides a callback on Channel setNextWriteValue event.
	 * 
	 * <p>
	 * This is a convenience method to react on a
	 * {@link WriteChannel#onSetNextWrite(ThrowingConsumer)} event
	 *
	 * @param <COMPONENT> the type of the {@link OpenemsComponent}
	 * @param callback    the method to call at setNextWriteValue event, value can
	 *                    be null
	 * @return myself
	 */
	@SuppressWarnings("unchecked")
	public <COMPONENT extends OpenemsComponent> AbstractDoc<T> onChannelSetNextWrite(
			ThrowingBiConsumer<COMPONENT, T, OpenemsNamedException> callback) {
		this.onInitCallback.add(channel -> {
			if (!(channel instanceof WriteChannel<?>)) {
				throw new IllegalArgumentException("Channel [" + channel.address()
						+ "] is not a WriteChannel ('AbstractDoc.onChannelSetNextWrite')");
			}
			((WriteChannel<T>) channel).onSetNextWrite(value -> {
				callback.accept((COMPONENT) channel.getComponent(), value);
			});
		});
		return this.self();
	}

	/**
	 * Registers a Mirror-To-Debug-Channel on Channel setNextWriteValue event.
	 * 
	 * <p>
	 * After calling this method, on every setNextWriteValue event, the
	 * 'nextWriteValue' will be mirrored to the 'targetChannelId' of the same
	 * Component.
	 *
	 * @param targetChannelId the target Channel-ID of the same component
	 * @return myself
	 */
	public AbstractDoc<T> onChannelSetNextWriteMirrorToDebugChannel(ChannelId targetChannelId) {
		this.onChannelSetNextWrite((component, value) -> {
			component.channel(targetChannelId).setNextValue(value);
		});
		return this.self();
	}

	/**
	 * Gets the callbacks for initialization of the actual Channel.
	 *
	 * @return a list of callbacks
	 */
	protected List<Consumer<Channel<T>>> getOnInitCallbacks() {
		return this.onInitCallback;
	}

	/**
	 * Creates an instance of {@link Channel} for the given Channel-ID using its
	 * Channel-{@link AbstractDoc}.
	 *
	 * @param <C>       the {@link Channel} type
	 * @param component the {@link OpenemsComponent}
	 * @param channelId the {@link ChannelId}
	 * @return the Channel
	 */
	@Override
	public abstract <C extends Channel<?>> C createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.ChannelId channelId);
}
