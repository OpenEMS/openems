package io.openems.edge.common.channel.doc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.google.common.base.CaseFormat;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.Channel;

/**
 * Provides static meta information for a {@link Channel} using Builder pattern.
 * 
 * Possible meta information:
 * <ul>
 * <li>access-mode (read-only/read-write/write-only) flag {@link #accessMode(AccessMode)}
 * <li>expected OpenemsType via {@link #getType()}
 * <li>descriptive text via {@link #getText()}
 * <li>a Unit via {@link #getUnit()}
 * <li>possible named option values as String or Enum via
 * {@link #getOption(String)}, {@link #getOption(int)}, {@link #getOption(Enum)} methods
 * <li>importance {@link Level} via {@link #getLevel()}
 * <li>is debug mode activated via {@link #isDebug()}
 * <li>callback on initialisation of a Channel via
 * {@link #getOnInitCallback()}
 * </ul>
 */
public class Doc {

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
	private Optional<OpenemsType> type = Optional.empty();

	/**
	 * Sets the OpenemsType. This is validated on construction of the Channel by
	 * {@link AbstractReadChannel}
	 * 
	 * @param type
	 * @return
	 */
	public Doc type(OpenemsType type) {
		this.type = Optional.ofNullable(type);
		return this;
	}

	public Optional<OpenemsType> getType() {
		return type;
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
	 * Options. Value is either a String or an Enum.
	 */
	private BiMap<Integer, Object> options = HashBiMap.create();

	public boolean hasOptions() {
		return this.options.size() > 0;
	}

	public Doc option(int value, Enum<?> option) {
		this.options.put(value, option);
		return this;
	}

	public Doc option(Enum<?> option) {
		this.options.put(option.ordinal(), option);
		// this.options.put(option.ordinal(), option.name());
		return this;
	}

	public Doc options(Enum<? extends OptionsEnum>[] options) {
		for (Enum<? extends OptionsEnum> option : options) {
			this.option(((OptionsEnum) option).getValue(), option);
		}
		return this;
	}

	public Doc option(int value, String option) {
		this.options.put(value, option);
		return this;
	}

	/**
	 * Get the Option value. Throws IllegalArgumentException if there is no option
	 * with that name
	 * 
	 * @param name
	 * @return
	 */
	public int getOption(String name) {
		Integer value = this.options.inverse().get(name);
		if (value == null) {
			throw new IllegalArgumentException(
					"Channel has no option [" + name + "]! Existing options: " + this.options.values());
		}
		return value;
	}

	/**
	 * Get the Option value. Throws IllegalArgumentException if there is no option
	 * with that name
	 * 
	 * @param nameEnum
	 * @return
	 */
	public int getOption(Enum<?> nameEnum) {
		Integer value = this.options.inverse().get(nameEnum);
		if (value != null) {
			return value;
		}
		return this.getOption(nameEnum.name());
	}

	/**
	 * Get the Option name. Throws IllegalArgumentException if there is no option
	 * with that value
	 * 
	 * @param value
	 * @return
	 */
	public String getOption(int value) {
		Object option = this.options.get(value);
		if (option == null) {
			throw new IllegalArgumentException(
					"Channel has no option value [" + value + "]! Existing options: " + this.options.values());
		}
		if (option instanceof Enum<?>) {
			return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, ((Enum<?>) option).name());
		}
		return option.toString();
	}

	/**
	 * Get the Option Enum. Throws IllegalArgumentException if there is no Enum
	 * option with that value
	 * 
	 * @param value
	 * @return
	 */
	public Enum<?> getOptionEnum(int value) {
		Object option = this.options.get(value);
		if (option == null) {
			throw new IllegalArgumentException(
					"Channel has no option value [" + value + "]! Existing options: " + this.options.values());
		}
		if (!(option instanceof Enum<?>)) {
			throw new IllegalArgumentException(
					"Channel has no Enum option value [" + value + "]! Existing options: " + this.options.values());
		}
		return (Enum<?>) option;
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
}
