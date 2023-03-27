package io.openems.edge.core.appmanager;

import io.openems.common.session.Role;

public class OpenemsAppPermissions {

	public final Role canSee;

	public static final class Builder {
		private Role canSee = Role.OWNER;

		private Builder() {
			super();
		}

		public Builder setCanSee(Role canSee) {
			this.canSee = canSee;
			return this;
		}

		public OpenemsAppPermissions build() {
			return new OpenemsAppPermissions(//
					this.canSee //
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

	private OpenemsAppPermissions(Role canSee) {
		super();
		this.canSee = canSee;
	}

}
