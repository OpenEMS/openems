package io.openems.backend.metadata.odoo.odoo;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import io.openems.backend.metadata.odoo.Field;

public class Domain {
	public static enum Operator {
		/**
		 * Returns case sensitive (wildcards - '%open%'). <br/>
		 * e.g.: [('input', 'like', 'open')] => open, opensource, openerp, Odooopenerp
		 */
		LIKE("like"),
		/**
		 * Returns results not matched with case sensitive (wildcards - '%open%'). <br/>
		 * e.g.: [('input', 'not like', 'open')] => Openerp, Opensource, Open, Odoo,
		 * odoo, OdooOpenerp
		 */
		NOT_LIKE("not like"),
		/**
		 * Returns exact (= 'open') case sensitive search. <br/>
		 * e.g.: [('name', '=like', 'open')] => open
		 */
		EQ_LIKE("=like"),
		/**
		 * Returns exact case insensitive (wildcards - '%open%'). <br/>
		 * e.g.: [('name', 'ilike', 'open')] => Openerp, openerp, Opensource,
		 * opensource, Open, open, Odooopenerp, OdooOpenerp
		 */
		I_LIKE("ilike"),
		/**
		 * Returns results not matched with exact case insensitive (wildcards -
		 * '%open%'). <br/>
		 * e.g.: [('name', 'not ilike', 'open')] => Odoo, odoo
		 */
		NOT_I_LIKE("not ilike"),
		/**
		 * Returns exact (= 'open' or 'Open') case insensitive. <br/>
		 * e.g.: [('name', '=ilike', 'open')] => Open, open
		 */
		EQ_I_LIKE("=ilike"),
		/**
		 * in operator will check the value1 is present or not in list of right term.
		 */
		IN("in"),
		/**
		 * not in operator will check the value1 is not present in list of right term.
		 */
		NOT_IN("not in"),
		/**
		 * Returns case sensitive (exact match - 'open'). <br/>
		 * e.g.: [('input', '=', 'open')] => open
		 */
		EQ("="),
		/**
		 * Returns results not matched with given input (other than - 'open'). <br/>
		 * e.g.: [('input', '!=', 'open')] => "other than open records"
		 */
		NE("!="),
		/**
		 * Returns results greater than given input (other value greater than - '10').
		 * <br/>
		 * e.g.: [('input', '>', '10')] => 11, 12, 13
		 *
		 */
		GT(">"),
		/**
		 * Returns results greater or equal of given input. <br/>
		 * e.g.: [('input', '>=', '10')] => 10, 11, 12, 13
		 */
		GE(">="),
		/**
		 * Returns results less than given input (other value less than - '10'). <br/>
		 * e.g.: [('input', '<', '10')] => 9, 8, 7
		 */
		LT("<"),
		/**
		 * Returns results less or equal of given input. <br/>
		 * e.g.: [('input', '<=', '10')] => 10, 9, 8, 7
		 */
		LE("<=");

		private final String value;

		private Operator(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}
	}

	protected final String field;
	protected final String operator;
	protected final Object value;

	public Domain(String field, Operator operator, Object value) {
		this.field = field;
		this.operator = operator.value;
		this.value = value;
	}

	public Domain(Field field, Operator operator, Object value) {
		this(field.id(), operator, value);
	}

	/**
	 * Allows to create a domain with a dotted field. This allows following foreign
	 * key constraints.
	 *
	 * @param fields   in order of notation. e.g [Field[userId], Field[name]] =>
	 *                 'userId.name'.
	 * @param operator as comparison operator
	 * @param value    to compare against
	 */
	public Domain(Field[] fields, Operator operator, Object value) {
		this.field = Arrays.stream(fields).map(Field::id).collect(Collectors.joining("."));
		this.operator = operator.value;
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.field, this.operator, this.value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj instanceof Domain other) {
			return Objects.equals(this.field, other.field) //
					&& Objects.equals(this.operator, other.operator) //
					&& Objects.equals(this.value, other.value);
		} else {
			return false;
		}
	}
}
