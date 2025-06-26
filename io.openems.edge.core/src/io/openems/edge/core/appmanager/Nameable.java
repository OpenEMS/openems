package io.openems.edge.core.appmanager;

import java.util.Objects;

public interface Nameable {

	/**
	 * Creates a {@link Nameable} which returns the given name on the
	 * {@link Nameable#name()} method.
	 * 
	 * @param name the name of the {@link Nameable}
	 * @return the created {@link Nameable}
	 */
	public static Nameable of(String name) {
		return new StaticNameable(name);
	}

	/**
	 * Gets the name of the current instance.
	 * 
	 * @return the name
	 */
	public String name();

	public static final class StaticNameable implements Nameable, Comparable<StaticNameable> {

		private final String name;

		public StaticNameable(String name) {
			super();
			this.name = name;
		}

		@Override
		public int compareTo(StaticNameable o) {
			return this.name.compareTo(o.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.name);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (this.getClass() != obj.getClass()) {
				return false;
			}
			final var other = (StaticNameable) obj;
			return Objects.equals(this.name, other.name);
		}

		@Override
		public String name() {
			return this.name;
		}

	}

}
