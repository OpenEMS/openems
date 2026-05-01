package io.openems.core.referencetarget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record StringWithParams(String rawString, List<Parameter> parameter) {

	private static final String VARIABLE_START = "${";
	private static final String VARIABLE_END = "}";

	public record Parameter(String topic, String variable) {

		@Override
		public String toString() {
			return this.topic() + "." + this.variable();
		}
	}

	public StringWithParams(String rawString) {
		this(rawString, getParametersFrom(rawString));
	}

	/**
	 * Creates a string with the provided values.
	 * 
	 * @param values the values to set in the string.
	 * @return the build string
	 */
	public String withParameters(Map<Parameter, Object> values) {
		var string = this.rawString;
		for (var entry : values.entrySet()) {

			switch (entry.getValue()) {
			case String[] strings -> {
				if (strings.length <= 1) {
					string = string.replace(VARIABLE_START + entry.getKey() + VARIABLE_END,
							strings.length == 0 ? "" : strings[0]);
					continue;
				}

				final var variableStart = string.indexOf(VARIABLE_START + entry.getKey() + VARIABLE_END);
				final var indexOfEquals = string.lastIndexOf("=", variableStart);
				final var indexOfBracket = string.lastIndexOf("(", indexOfEquals);
				final var indexOfEndBracket = string.indexOf(")", variableStart);

				final var variableName = string.substring(indexOfBracket + 1, indexOfEquals);
				final var filter = "(|" + Arrays.stream(strings) //
						.map(t -> "(" + variableName + "=" + t + ")") //
						.collect(Collectors.joining()) + ")";

				string = string.substring(0, indexOfBracket) + filter + string.substring(indexOfEndBracket + 1);
			}
			default -> {
				final var v = entry.getValue().toString();
				string = string.replace(VARIABLE_START + entry.getKey() + VARIABLE_END, v);
			}
			}

		}

		return string;
	}

	/**
	 * Gets the {@link Parameter Parameters} of a raw string.
	 * 
	 * <p>
	 * Simple Filter with variable:
	 * 
	 * <pre>
	 * target = "(&(enabled=true)(!(id=${config.component_id})))"
	 * Parameter(topic = "config", variable = "component_id", operator = null, matchVariable = null)
	 * </pre>
	 * </p>
	 * 
	 * @param string the ldap filter string
	 * @return a list of {@link Parameter Parameters}
	 */
	private static List<Parameter> getParametersFrom(String string) {

		final var parameters = new ArrayList<Parameter>();
		var startIndex = 0;
		while (true) {
			startIndex = string.indexOf(VARIABLE_START, startIndex);
			final var endIndex = string.indexOf(VARIABLE_END, startIndex);

			if (startIndex == -1 || endIndex == -1) {
				break;
			}
			startIndex = startIndex + VARIABLE_START.length();

			final var raw = string.substring(startIndex, endIndex);

			final var variableParts = raw.split("\\.");

			parameters.add(new Parameter(variableParts[0], variableParts[1]));
		}

		return parameters;
	}

}