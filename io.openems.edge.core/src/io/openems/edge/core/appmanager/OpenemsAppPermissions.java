package io.openems.edge.core.appmanager;

import java.util.List;

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
		Role canDelete, //

		/**
		 * Defines if an app can be installed by an user with one these roles.
		 */
		List<Role> canInstall //
) {

	public static final class Builder {
		private Role canSee = Role.OWNER;
		private Role canDelete = Role.OWNER;
		private List<Role> canInstall = List.of(Role.OWNER, Role.INSTALLER, Role.ADMIN);

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

		public Builder setCanInstall(List<Role> canInstall) {
			this.canInstall = canInstall;
			return this;
		}

		public Builder setCanInstall(Role... canInstall) {
			return this.setCanInstall(List.of(canInstall));
		}

		public OpenemsAppPermissions build() {
			return new OpenemsAppPermissions(//
					this.canSee, //
					this.canDelete, //
					this.canInstall //

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
