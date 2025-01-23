package io.openems.common.oem;

import io.openems.common.session.AbstractUser;

public interface OpenemsBackendOem {

	/**
	 * Gets the App-Center Master-Key.
	 * 
	 * @return the value
	 */
	public default String getAppCenterMasterKey() {
		return "DUMMY_MASTER_KEY";
	}

	/**
	 * The measurement Tag used to write data to InfluxDB.
	 * 
	 * <p>
	 * Note: this value defaults to "edge"
	 * 
	 * @return the value
	 */
	public default String getInfluxdbTag() {
		return "edge";
	}

	/**
	 * Anonymize edge comment, dependent on user id.
	 * 
	 * @param user    the current user
	 * @param comment the edge comment
	 * @param edgeId  the edge id
	 * @return the edge comment
	 */
	public default String anonymizeEdgeComment(AbstractUser user, String comment, String edgeId) {
		return comment;
	}
}
