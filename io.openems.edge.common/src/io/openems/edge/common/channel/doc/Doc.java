package io.openems.edge.common.channel.doc;

import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.base.CaseFormat;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.openems.edge.common.channel.Channel;

public class Doc {

	/*
	 * Description
	 */
	private String text = "";

	public Doc text(String text) {
		this.text = text;
		return this;
	}

	String getText() {
		return this.text;
	}

	/*
	 * Unit
	 */
	private Unit unit = Unit.NONE;

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

	public Doc option(int value, Enum<?> option) {
		this.options.put(value, option.name());
		return this;
	}

	public Doc option(Enum<?> option) {
		this.options.put(option.ordinal(), option);
		// this.options.put(option.ordinal(), option.name());
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
	 * @param value
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
	 * @param value
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
	 * On Channel Initalization Callback
	 */
	private Optional<Consumer<Channel<?>>> onInitCallbackOpt = Optional.empty();

	/**
	 * Provides a callback on initialization of the actual Channel
	 * 
	 * @param channel
	 * @return
	 */
	public Doc onInit(Consumer<Channel<?>> channel) {
		this.onInitCallbackOpt = Optional.ofNullable(channel);
		return this;
	}

	public Optional<Consumer<Channel<?>>> getOnInitCallback() {
		return onInitCallbackOpt;
	}
}
