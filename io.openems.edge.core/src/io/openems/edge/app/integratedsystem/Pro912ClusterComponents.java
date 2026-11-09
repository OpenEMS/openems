package io.openems.edge.app.integratedsystem;

import java.util.List;

import com.google.gson.JsonPrimitive;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;

public class Pro912ClusterComponents {

	/**
	 * Creates a default ess cluster component for a FENECON Pro 9-12.
	 *
	 * @param clusterEssId the id of the ess cluster
	 * @param essIds       es id of the ess components
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef essCluster(//
			String clusterEssId, //
			List<String> essIds //
	) {
		return new ComponentDef(clusterEssId, clusterEssId, "Ess.Cluster", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("startStop", "START") //
						.add("ess.ids", essIds.stream() //
								.map(JsonPrimitive::new) //
								.collect(JsonUtils.toJsonArray())) //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}

	private Pro912ClusterComponents() {
	}
}
