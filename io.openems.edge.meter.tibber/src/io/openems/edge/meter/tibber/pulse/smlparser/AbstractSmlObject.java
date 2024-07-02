package io.openems.edge.meter.tibber.pulse.smlparser;

import java.lang.reflect.Field;
import java.util.List;

public abstract class AbstractSmlObject {

	/**
	 * Converts this object to its string representation. This method builds a
	 * string representation of the object by appending its details to a
	 * StringBuilder, starting with an initial indentation level and optionally
	 * including verbose information.
	 * 
	 * @return A string representation of the object.
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.appendToString(sb, 0, true);
		return sb.toString();
	}

	protected void appendToString(StringBuilder sb, int indent, boolean printClassName) {
		if (printClassName) {
			this.indentAppend(sb, indent, getClass().getSimpleName());
			indent++;
		}
		for (Field field : getClass().getDeclaredFields()) {
			field.setAccessible(true);
			Object value;
			try {
				value = field.get(this);
			} catch (Exception ex) {
				value = ex.toString();
			}
			this.indentAppend(sb, indent, field.getName(), value);
		}
	}

	protected void indentAppend(StringBuilder sb, int indent, String name, Object value) {

		if (value instanceof List) {
			List<?> list = (List<?>) value;
			this.indentAppend(sb, indent, name + " = List[" + list.size() + "]");
			indent++;
			for (int i = 0; i < list.size(); i++) {
				this.indentAppend(sb, indent, "[" + i + "]", list.get(i));
			}
			return;
		}

		if (value instanceof AbstractSmlObject) {
			this.indentAppend(sb, indent, name + " = " + value.getClass().getSimpleName());
			((AbstractSmlObject) value).appendToString(sb, indent + 1, false);
			return;
		}
		if (value instanceof byte[]) {
			this.indentAppend(sb, indent, name + " = 0x" + ByteUtil.toHex((byte[]) value));
			return;
		}

		this.indentAppend(sb, indent, name + " = " + String.valueOf(value));
	}

	protected void indentAppend(StringBuilder sb, int indent, String line) {
		if (sb.length() > 0) {
			sb.append("\n");
		}
		for (int i = 0; i < indent; i++) {
			sb.append("  ");
		}
		sb.append(line);
	}

}
