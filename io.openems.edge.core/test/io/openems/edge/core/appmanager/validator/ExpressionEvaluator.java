package io.openems.edge.core.appmanager.validator;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;

public class ExpressionEvaluator {

	private static final Pattern MODEL_REFERENCE_PATTERN = Pattern.compile("model\\.([A-Z_]+)");

	/**
	 * Evaluates a BooleanExpression and returns the result as a boolean value.
	 * 
	 * @param expression the expression to evaluate
	 * @param model      the model to use for variable substitution
	 * @return the result of the comparison
	 */
	public static boolean evaluateExpression(String expression, JsonObject model) {
		var eval = expression;

		final var matcher = MODEL_REFERENCE_PATTERN.matcher(eval);
		final var references = new LinkedHashSet<String>();

		while (matcher.find()) {
			references.add(matcher.group(1));
		}

		for (final var ref : references) {
			eval = eval.replace("model." + ref, getReplacement(model, ref));
		}

		return evaluateSimpleExpression(eval);
	}

	private static String getReplacement(JsonObject model, String ref) {
		final var value = model.get(ref);

		if (value == null || value.isJsonNull()) {
			return "null";
		}

		if (!value.isJsonPrimitive()) {
			return "null";
		}

		final var primitive = value.getAsJsonPrimitive();

		if (primitive.isNumber()) {
			return primitive.getAsNumber().toString();
		}

		if (primitive.isBoolean()) {
			return primitive.getAsBoolean() ? "true" : "false";
		}

		return "'" + primitive.getAsString() + "'";
	}

	private static boolean evaluateSimpleExpression(String expr) {
		final var trimmed = expr.trim();

		if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
			return evaluateSimpleExpression(trimmed.substring(1, trimmed.length() - 1));
		}

		if (trimmed.contains("||")) {
			final var parts = trimmed.split("\\|\\|");
			for (final var part : parts) {
				if (evaluateSimpleExpression(part)) {
					return true;
				}
			}
			return false;
		}

		if (trimmed.contains("&&")) {
			final var parts = trimmed.split("&&");
			for (final var part : parts) {
				if (!evaluateSimpleExpression(part)) {
					return false;
				}
			}
			return true;
		}

		if (trimmed.startsWith("!!")) {
			return evaluateSimpleExpression(trimmed.substring(2));
		}

		if (trimmed.startsWith("!")) {
			return !evaluateSimpleExpression(trimmed.substring(1));
		}

		if (trimmed.contains("==")) {
			final var parts = trimmed.split("==");
			return compareValues(parts[0].trim(), parts[1].trim(), "==");
		}
		if (trimmed.contains("!=")) {
			final var parts = trimmed.split("!=");
			return compareValues(parts[0].trim(), parts[1].trim(), "!=");
		}
		if (trimmed.contains(">=")) {
			final var parts = trimmed.split(">=");
			return compareValues(parts[0].trim(), parts[1].trim(), ">=");
		}
		if (trimmed.contains("<=")) {
			final var parts = trimmed.split("<=");
			return compareValues(parts[0].trim(), parts[1].trim(), "<=");
		}
		if (trimmed.contains(">")) {
			final var parts = trimmed.split(">");
			return compareValues(parts[0].trim(), parts[1].trim(), ">");
		}
		if (trimmed.contains("<")) {
			final var parts = trimmed.split("<");
			return compareValues(parts[0].trim(), parts[1].trim(), "<");
		}

		return isTruthy(parseValue(trimmed));
	}

	private static boolean compareValues(String left, String right, String operator) {
		final var leftVal = parseValue(left);
		final var rightVal = parseValue(right);

		return switch (operator) {
		case "==" -> Objects.equals(leftVal, rightVal);
		case "!=" -> !Objects.equals(leftVal, rightVal);
		case ">=" -> leftVal != null && rightVal != null && compareNumeric(leftVal, rightVal) >= 0;
		case "<=" -> leftVal != null && rightVal != null && compareNumeric(leftVal, rightVal) <= 0;
		case ">" -> leftVal != null && rightVal != null && compareNumeric(leftVal, rightVal) > 0;
		case "<" -> leftVal != null && rightVal != null && compareNumeric(leftVal, rightVal) < 0;
		default -> false;
		};
	}

	private static Object parseValue(String value) {
		final var trimmed = value.trim();

		return switch (trimmed) {
		case "null" -> null;
		case "true" -> true;
		case "false" -> false;
		default -> parseNumberOrString(trimmed);
		};
	}

	private static Object parseNumberOrString(String value) {
		if (value.startsWith("'") && value.endsWith("'")) {
			return value.substring(1, value.length() - 1);
		}

		try {
			if (value.contains(".")) {
				return Double.parseDouble(value);
			}
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return value;
		}
	}

	private static boolean isTruthy(Object value) {
		if (value == null) {
			return false;
		}

		if (value instanceof Boolean bool) {
			return bool;
		}

		if (value instanceof Number number) {
			return number.doubleValue() != 0;
		}

		if (value instanceof String string) {
			return !string.isEmpty();
		}

		return true;
	}

	private static int compareNumeric(Object left, Object right) {
		if (left instanceof Number leftNumber && right instanceof Number rightNumber) {
			return Double.compare(leftNumber.doubleValue(), rightNumber.doubleValue());
		}

		return left.toString().compareTo(right.toString());
	}
}
