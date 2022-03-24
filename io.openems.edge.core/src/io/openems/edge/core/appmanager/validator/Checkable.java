package io.openems.edge.core.appmanager.validator;

public interface Checkable {

	public static class Or implements Checkable {

		private final Checkable first;
		private final Checkable second;

		public Or(Checkable first, Checkable second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public boolean check() {
			return this.first.check() || this.second.check();
		}

		@Override
		public String getErrorMessage() {
			return this.first.getErrorMessage() + " or " + this.second.getErrorMessage();
		}

	}

	/**
	 * Checks if the implemented task was successful or not.
	 *
	 * @return true if the check was successful else false
	 */
	public boolean check();

	/**
	 * Gets the error message if the check was incorrect completed.
	 *
	 * @return the message
	 */
	public String getErrorMessage();

	/**
	 * Gets a new {@link Checkable} which returns true if at least one of the
	 * {@link Checkable} are true.
	 *
	 * @param checkable the other {@link Checkable}
	 * @return a new {@link Checkable}
	 */
	public default Checkable or(Checkable checkable) {
		return new Or(this, checkable);
	}

}
