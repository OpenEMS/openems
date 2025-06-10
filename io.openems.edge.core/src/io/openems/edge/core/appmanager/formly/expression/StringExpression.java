package io.openems.edge.core.appmanager.formly.expression;

public record StringExpression(String expression) {

	/**
	 * Creates a {@link StringExpression} of the given string.
	 * 
	 * @param string the string to created the {@link StringExpression} from
	 * @return the created {@link StringExpression}
	 */
	public static StringExpression of(String string) {
		return new StringExpression("'" + string + "'");
	}

	/**
	 * Used when adding a {@link StringExpression} inside a translation.
	 * 
	 * <p>
	 * Usage:
	 * 
	 * <pre>
	 * var insideStringExpression = StringExpression.of("inside");
	 * StringExpression.of(TranslationUtil.getTranslation(bundle, "key", //
	 * 		insideStringExpression.insideTranslation()));
	 * </pre>
	 * 
	 * @return the string to use in the translation
	 */
	public String insideTranslation() {
		return "' + " + this.expression() + " + '";
	}

}
