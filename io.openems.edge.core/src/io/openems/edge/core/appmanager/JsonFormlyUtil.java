package io.openems.edge.core.appmanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

/**
 * Source https://formly.dev/examples/introduction.
 */
public class JsonFormlyUtil {

	/**
	 * Builds a checkbox for a enum constant.
	 *
	 * @param <T> the type of the enum
	 * @param p   the enum constant
	 * @return the checkbox as a {@link JsonObject}
	 */
	public static <T extends Enum<T>> JsonObject buildCheckbox(T p) {
		return buildCheckbox(p, false);
	}

	/**
	 * Builds a checkbox for a enum constant with the required property.
	 *
	 * @param <T>        the type of the enum
	 * @param p          the enum constant
	 * @param isRequired if the input should be required
	 * @return the checkbox as a {@link JsonObject}
	 */
	public static <T extends Enum<T>> JsonObject buildCheckbox(T p, boolean isRequired) {
		return JsonUtils.buildJsonObject() //
				.addProperty("key", p.name()) //
				.addProperty("type", "checkbox") //
				.add("templateOptions", JsonUtils.buildJsonObject() //
						.addProperty("label", p.toString()) //
						.addPropertyIfNotNull("required", isRequired ? true : null) //
						.build()) //
				.build();
	}

	/**
	 * Builds an Input for the given DefaultEnum.
	 *
	 * @param p the DefaultEnum
	 * @return the Input Object
	 */
	public static JsonObject buildInput(DefaultEnum p) {
		return buildInput(p, false);
	}

	/**
	 * Builds an Input for the given DefaultEnum.
	 *
	 * @param p          the DefaultEnum
	 * @param isRequired if the input should be required
	 * @return the Input Object
	 */
	public static JsonObject buildInput(DefaultEnum p, boolean isRequired) {
		return buildInput(p, isRequired, false);
	}

	/**
	 * Builds an Input for the given DefaultEnum.
	 *
	 * @param p          the DefaultEnum
	 * @param isRequired if the input should be required
	 * @param isNumber   if the input is a number
	 * @return the Input Object
	 */
	public static JsonObject buildInput(DefaultEnum p, boolean isRequired, boolean isNumber) {
		return JsonUtils.buildJsonObject() //
				.addProperty("key", p.name()) //
				.addProperty("type", "input") //
				.add("templateOptions", JsonUtils.buildJsonObject() //
						.onlyIf(isNumber, t -> t.addProperty("type", "number")) //
						.addProperty("label", p.toString()) //
						.addPropertyIfNotNull("required", isRequired ? true : null) //
						.build())
				.addProperty("defaultValue", p.getDefaultValue()) //
				.build();
	}

	/**
	 * Builds an Input for the given enum.
	 *
	 * @param <T> the type of the enum
	 * @param p   the enum
	 * @return the Input Object
	 */
	public static <T extends Enum<T>> JsonObject buildInput(T p) {
		return buildInput(p, null, false);
	}

	/**
	 * Builds an Input for the given enum.
	 *
	 * @param <T>          the type of the enum
	 * @param p            the enum
	 * @param defaultValue the default Value of the field
	 * @param isRequired   if the input is required
	 * @return the Input Object
	 */
	public static <T extends Enum<T>> JsonObject buildInput(T p, String defaultValue, boolean isRequired) {
		return buildInput(p, defaultValue, isRequired, false);
	}

	/**
	 * Builds an Input for the given enum.
	 *
	 * @param <T>          the type of the enum
	 * @param p            the enum
	 * @param defaultValue the default Value of the field
	 * @param isRequired   if the input is required
	 * @param isNumber     if the input is a number
	 * @return the Input Object
	 */
	public static <T extends Enum<T>> JsonObject buildInput(T p, String defaultValue, boolean isRequired,
			boolean isNumber) {
		return JsonUtils.buildJsonObject() //
				.addProperty("key", p.name()) //
				.addProperty("type", "input") //
				.add("templateOptions", JsonUtils.buildJsonObject() //
						.onlyIf(isNumber, t -> t.addProperty("type", "number")) //
						.addProperty("label", p.toString()) //
						.addPropertyIfNotNull("required", isRequired ? true : null) //
						.build())
				.addPropertyIfNotNull("defaultValue", defaultValue) //
				.build();
	}

	/**
	 * Builds a Select for the given PROPERTY with the given options.
	 *
	 * @param <T>     the type of the enum
	 * @param p       the PROPERTY
	 * @param options the options of the select
	 * @return the build JsonObject
	 */
	public static <T extends Enum<T>> JsonObject buildSelect(T p, JsonArray options) {
		return buildSelect(p, options, false, false);
	}

	/**
	 * Builds a Select for the given PROPERTY with the given options.
	 *
	 * @param <T>        the type of the enum
	 * @param p          the PROPERTY
	 * @param options    the options of the select
	 * @param isRequired if the select should be required
	 * @param isMulti    if the select should be a multi-select
	 * @return the build JsonObject
	 */
	public static <T extends Enum<T>> JsonObject buildSelect(T p, JsonArray options, boolean isRequired,
			boolean isMulti) {
		return JsonUtils.buildJsonObject() //
				.addProperty("key", p.name()) //
				.addProperty("type", "select") //
				.add("templateOptions", JsonUtils.buildJsonObject() //
						.addProperty("label", p.toString()) //
						.addPropertyIfNotNull("required", isRequired ? true : null) //
						.add("options", options) //
						.onlyIf(isMulti, t -> t.addProperty("multiple", true)).build())
				.build();
	}

	private JsonFormlyUtil() {
	}

}
