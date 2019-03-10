package io.openems.edge.common.channel.doc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides static meta information for a {@link Channel} using Builder pattern.
 * 
 * Possible meta information:
 * <ul>
 * <li>access-mode (read-only/read-write/write-only) flag
 * {@link #accessMode(AccessMode)}
 * <li>expected OpenemsType via {@link #getType()}
 * <li>descriptive text via {@link #getText()}
 * <li>a Unit via {@link #getUnit()}
 * <li>possible named option values {@link OptionsEnum} via
 * {@link #getOptionFromEnumString(String)}, {@link #getOption(Integer)} methods
 * <li>importance {@link Level} via {@link #getLevel()}
 * <li>is debug mode activated via {@link #isDebug()}
 * <li>callback on initialization of a Channel via {@link #getOnInitCallback()}
 * </ul>
 */
// TODO this class should be abstract
public class Doc {

	/**
	 * Create a Channel-Doc with a specific OpenemsType.
	 * 
	 * @param type the OpenemsType
	 */
	public static OpenemsTypeDoc of(OpenemsType type) {
		return new OpenemsTypeDoc(type);
	}

	/**
	 * Create a Channel-Doc with specific options.
	 * 
	 * @param options the possible options as an OptionsEnum
	 */
	public static OptionsEnumDoc of(Enum<? extends OptionsEnum>[] options) {
		return new OptionsEnumDoc(options);
	}

	/**
	 * Create a Channel-Doc for a {@link StateChannel} with a given {@link Level}.
	 * 
	 * @param level the Level
	 */
	public static StateChannelDoc of(Level level) {
		return new StateChannelDoc(level);
	}

	private final Logger log = LoggerFactory.getLogger(Doc.class);

	@Deprecated
	public Doc() {
		this.type = null;
	}

	/**
	 * Allowed Access-Mode for this Channel.
	 */
	private AccessMode accessMode = AccessMode.READ_ONLY;

	/**
	 * Sets the Access-Mode for the Channel. This is validated on construction of
	 * the Channel by {@link AbstractReadChannel}
	 * 
	 * @return
	 */
	public Doc accessMode(AccessMode accessMode) {
		this.accessMode = accessMode;
		return this;
	}

	/**
	 * Gets the 'Access-Mode' information
	 * 
	 * @return
	 */
	public AccessMode getAccessMode() {
		return this.accessMode;
	}

	/*
	 * OpenEMS Type
	 */
	// TODO should be final
	private OpenemsType type;

	/**
	 * Sets the OpenemsType. This is validated on construction of the Channel by
	 * {@link AbstractReadChannel}
	 * 
	 * @param type
	 * @return
	 */
	@Deprecated
	public Doc type(OpenemsType type) {
		this.type = type;
		return this;
	}

	@Deprecated
	public Optional<OpenemsType> getType() {
		return Optional.ofNullable(type);
	}

	// TODO should be renamed to getType and replace the Optional variant.
	public OpenemsType getType2() {
		return type;
	}

	/*
	 * Initial Value
	 */
	private Object initialValue = null;

	/**
	 * Initial-Value. Default: none
	 * 
	 * @param unit
	 * @return
	 */
	public Doc initialValue(Object initialValue) {
		this.initialValue = initialValue;
		return this;
	}

	public Object getInitialValue() {
		return this.initialValue;
	}

	/*
	 * Description
	 */
	private String text = "";

	/**
	 * Descriptive text. Default: empty string
	 * 
	 * @param text
	 * @return
	 */
	public Doc text(String text) {
		this.text = text;
		return this;
	}

	public String getText() {
		return this.text;
	}

	/*
	 * Unit
	 */
	private Unit unit = Unit.NONE;

	/**
	 * Unit. Default: none
	 * 
	 * @param unit
	 * @return
	 */
	public Doc unit(Unit unit) {
		this.unit = unit;
		return this;
	}

	public Unit getUnit() {
		return this.unit;
	}

	/*
	 * Options.
	 */
	protected final BiMap<Integer, OptionsEnum> options = HashBiMap.create();

	/**
	 * Set the possible options using an OptionsEnum
	 * 
	 * @param options
	 * @return
	 */
	@Deprecated
	public Doc options(Enum<? extends OptionsEnum>[] options) {
		for (Enum<? extends OptionsEnum> option : options) {
			OptionsEnum o = (OptionsEnum) option;
			this.options.put(o.getValue(), o);
		}
		return this;
	}

	/**
	 * Gets whether this Doc has registered Options
	 * 
	 * @return
	 */
	public boolean hasOptions() {
		return !this.options.isEmpty();
	}

	/**
	 * Get the Option value. Throws IllegalArgumentException if there is no option
	 * with that name
	 * 
	 * @param name
	 * @return
	 */
	public int getOptionFromEnumString(String name) {
		for (OptionsEnum e : this.options.values()) {
			if (e.getName().equalsIgnoreCase(name)) {
				return e.getValue();
			}
		}
		throw new IllegalArgumentException(
				"Channel has no option [" + name + "]! Existing options: " + this.options.values());
	}

	/**
	 * Get the Option Enum.
	 * 
	 * @param value
	 * @return
	 */
	public OptionsEnum getOption(Integer value) {
		if (this.options.isEmpty()) {
			return null;
		}
		OptionsEnum undefined = this.options.values().iterator().next().getUndefined();
		if (value == null) {
			return undefined;
		}
		OptionsEnum option = this.options.get(value);
		if (option == null) {
			return undefined;
		}
		return option;
	}

	/**
	 * Get the Option name or Undefined if there is no option with that value
	 * 
	 * @param value
	 * @return
	 */
	public String getOptionString(Integer value) {
		OptionsEnum option = this.getOption(value);
		if (option == null) {
			return Value.UNDEFINED_VALUE_STRING;
		}
		return option.getName();
	}

	/*
	 * Levels
	 */
	private Level level = Level.INFO;

	public Doc level(Level level) {
		this.level = level;
		return this;
	}

	public Level getLevel() {
		return level;
	}

	/*
	 * Verbose Debug mode
	 */
	private boolean debug = false;

	public Doc debug() {
		this.debug = true;
		return this;
	}

	public boolean isDebug() {
		return this.debug;
	}

	/*
	 * On Channel initialisation Callback
	 */
	private final List<Consumer<Channel<?>>> onInitCallback = new CopyOnWriteArrayList<>();

	/**
	 * Provides a callback on initialisation of the actual Channel
	 * 
	 * @param callback
	 * @return
	 */
	public Doc onInit(Consumer<Channel<?>> callback) {
		this.onInitCallback.add(callback);
		return this;
	}

	public List<Consumer<Channel<?>>> getOnInitCallback() {
		return onInitCallback;
	}

	/**
	 * Creates an instance of {@link Channel} for the given Channel-ID using its
	 * Channel-{@link Doc}.
	 * 
	 * @param channelId the Channel-ID
	 * @return the Channel
	 */
	// TODO this method should be abstract
	public Channel<?> createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.doc.ChannelId channelId) {
		this.log.warn("createChannelInstance(" + component.id() + "/" + channelId.id()
				+ "). This method should have never been called.");
		return null;
	}
}
