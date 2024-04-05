package io.openems.edge.core.appmanager;

import io.openems.common.session.Role;

public record OpenemsAppPermissions(//
		/**
		 * Defines if an app can be seen by an user with a role greater or equal this
		 * role.
		 */
		Role canSee, //
		/**
		 * Defines if an app can be deleted by an user with a role greater or equal this
		 * role.
		 */
		Role canDelete //
) {

	public static final class Builder {
		private Role canSee = Role.OWNER;
		private Role canDelete = Role.OWNER;

		private Builder() {
			super();
		}

		public Builder setCanSee(Role canSee) {
			this.canSee = canSee;
			return this;
		}

		public Builder setCanDelete(Role canDelete) {
			this.canDelete = canDelete;
			return this;
		}

		public OpenemsAppPermissions build() {
			return new OpenemsAppPermissions(//
					this.canSee, //
					this.canDelete //
			);
		}

	}

	/**
	 * Creates a {@link Builder} for {@link OpenemsAppPermissions}.
	 * 
	 * @return the builder
	 */
	public static final Builder create() {
		return new Builder();
	}

}
