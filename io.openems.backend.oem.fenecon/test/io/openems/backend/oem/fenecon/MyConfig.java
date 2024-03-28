package io.openems.backend.oem.fenecon;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public class MyConfig implements Config {

	public static final class Builder {
		private final List<String> demoUserIds = new ArrayList<>();
		private String appCenterMasterKey;

		/**
		 * Adds a demo user id.
		 * 
		 * @param demoUserId the id to add
		 * @return this
		 */
		public Builder addDemoUserId(String demoUserId) {
			this.demoUserIds.add(demoUserId);
			return this;
		}

		private Builder setAppCenterMasterKey(String appCenterMasterKey) {
			this.appCenterMasterKey = appCenterMasterKey;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(//
					this.demoUserIds.toArray(String[]::new), //
					this.appCenterMasterKey //
			);
		}

	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final String[] demoUserIds;
	private final String appCenterMasterKey;

	public MyConfig(String[] demoUserIds, String appCenterMasterKey) {
		super();
		this.demoUserIds = demoUserIds;
		this.appCenterMasterKey = appCenterMasterKey;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Config.class;
	}

	@Override
	public String[] demoUserIds() {
		return this.demoUserIds;
	}

	@Override
	public String appCenterMasterKey() {
		return this.appCenterMasterKey;
	}

	@Override
	public String webconsole_configurationFactory_nameHint() {
		return "";
	}

}
