package io.openems.edge.core.appmanager.formly;

import java.util.ArrayList;
import java.util.stream.Collector;
import java.util.stream.Stream;

import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.formly.expression.ArrayExpression;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;
import io.openems.edge.core.appmanager.formly.expression.StringExpression;
import io.openems.edge.core.appmanager.formly.expression.Variable;

public final class Exp {

	/**
	 * Creates a {@link Variable} of the given property of the current value in the
	 * form control.
	 * 
	 * <p>
	 * The difference between this method and the
	 * {@link Exp#currentModelValue(Nameable)} method is, that the value of the
	 * {@link Exp#currentValue(Nameable)} method may be null when a default value
	 * was set and the field is readonly.
	 * 
	 * @param property the property to access the value
	 * @return the {@link Variable}
	 */
	public static Variable currentValue(Nameable property) {
		return new Variable("control.value." + property.name());
	}

	/**
	 * Creates a {@link Variable} of the given property of the current value in the
	 * model.
	 * 
	 * <p>
	 * The difference between this method and the {@link Exp#currentValue(Nameable)}
	 * method is, that the value of the {@link Exp#currentValue(Nameable)} method
	 * may be null when a default value was set and the field is readonly.
	 * 
	 * @param property the property of the value
	 * @return the {@link Variable}
	 */
	public static Variable currentModelValue(Nameable property) {
		return new Variable("model." + property.name());
	}

	/**
	 * Creates a {@link Variable} to access the initial value of a property. Only
	 * helpful for already installed instances, otherwise this value is undefined.
	 * 
	 * @param property the property of the value
	 * @return the {@link Variable}
	 */
	public static Variable initialModelValue(Nameable property) {
		return new Variable("initialModel." + property.name());
	}

	/**
	 * Creates a {@link Variable} for a static {@link String} value.
	 * 
	 * @param value the value of the variable
	 * @return the {@link Variable}
	 */
	public static Variable staticValue(String value) {
		return new Variable("'" + value + "'");
	}

	/**
	 * Creates a {@link Variable} for a static {@link Number} value.
	 * 
	 * @param value the value of the variable
	 * @return the {@link Variable}
	 */
	public static Variable staticValue(Number value) {
		return new Variable(value.toString());
	}

	/**
	 * Creates a dynamic {@link Variable}.
	 * 
	 * @param name the name of the variable
	 * @return the created {@link Variable}
	 */
	public static Variable dynamic(String name) {
		return new Variable(name);
	}

	/**
	 * Creates a array of the given values.
	 * 
	 * @param values the values of the array
	 * @return a {@link ArrayExpression}
	 */
	public static ArrayExpression array(String... values) {
		return ArrayExpression.of(values);
	}

	/**
	 * Creates a collector which collects a {@link Stream} of {@link String Strings}
	 * to an {@link ArrayExpression}.
	 * 
	 * @return the {@link Collector}
	 */
	public static Collector<String, ?, ArrayExpression> toArrayExpression() {
		return Collector.of(ArrayList::new, ArrayList::add, (t, u) -> {
			t.addAll(u);
			return t;
		}, t -> {
			return ArrayExpression.of(t.toArray(String[]::new));
		});
	}

	/**
	 * Creates a combined {@link StringExpression} of the given
	 * {@link BooleanExpression} and {@link StringExpression}, which returns the
	 * first {@link StringExpression} if the given {@link BooleanExpression} returns
	 * true otherwise the second {@link StringExpression} gets returned.
	 * 
	 * @param statement the {@link BooleanExpression} to determine which
	 *                  {@link StringExpression} should be used
	 * @param ifTrue    the {@link StringExpression} to use when the statement
	 *                  returns true
	 * @param ifFalse   the {@link StringExpression} to use when the statement
	 *                  returns false
	 * @return the final {@link StringExpression}
	 */
	public static StringExpression ifElse(BooleanExpression statement, StringExpression ifTrue,
			StringExpression ifFalse) {
		return new StringExpression(
				statement.expression() + " ? " + ifTrue.expression() + " : " + ifFalse.expression());
	}

	private Exp() {
	}

}
