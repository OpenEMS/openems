package io.openems.edge.common.channel.internal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
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
		return type;
	}

	/**
	 * Allowed Access-Mode for this Channel.
	 */
	private AccessMode accessMode = AccessMode.READ_ONLY;

	/**
	 * Sets the Access-Mode for the Channel.
	 * 
	 * <p>
	 * This is validated on construction of the Channel by
	 * {@link AbstractReadChannel}
	 * 
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

	/*
	 * Initial Value
	 */
	private T initialValue = null;

	/**
	 * Initial-Value. Default: none
	 * 
	 * @param initialValue
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

	/**
	 * Descriptive text. Default: empty string
	 * 
	 * @param text the text
	 * @return myself
	 */
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
	 * Provides a callback on initialization of the actual Channel
	 * 
	 * @param callback the method to call on initialization
	 * @return myself
	 */
	public AbstractDoc<T> onInit(Consumer<Channel<T>> callback) {
		this.onInitCallback.add(callback);
		return this.self();
	}

	/**
	 * Gets the callbacks for initialization of the actual Channel
	 * 
	 * @param callback the method to call on initialization
	 * @return myself
	 */
	protected List<Consumer<Channel<T>>> getOnInitCallbacks() {
		return onInitCallback;
	}

	/**
	 * Creates an instance of {@link Channel} for the given Channel-ID using its
	 * Channel-{@link AbstractDoc}.
	 * 
	 * @param channelId the Channel-ID
	 * @return the Channel
	 */
	public abstract <C extends Channel<?>> C createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.ChannelId channelId);
}
