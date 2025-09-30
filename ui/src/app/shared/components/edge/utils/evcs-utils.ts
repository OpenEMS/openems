import { Edge } from "../edge";
import { EdgeConfig } from "../edgeconfig";

export class EvcsUtils {

    /**
     * Retrieves the appropriate power channel ID for an Electric Vehicle Charging Station (EVCS) component.
     *
     * The method returns "ActivePower", unless the given `edge` object does not meet the minimum
     * required version or the component implements DeprecatedEvcs, in which case it returns "ChargePower".
     *
     * @param component - The component for which to determine the power channel ID.
     * @param config - The EdgeConfig.
     * @param edge - The edge instance
     * @returns - Returns "ActivePower" if the edge version is at least "2024.10.2" and
     * the component is not deprecated. Otherwise, returns "ChargePower".
     */
    public static getEvcsPowerChannelId(component: EDGE_CONFIG.COMPONENT, config: EdgeConfig, edge: Edge | null): "ActivePower" | "ChargePower" {
        if (edge && component && config && (!CONFIG.HAS_COMPONENT_NATURE("IO.OPENEMS.EDGE.EVCS.API.DEPRECATED_EVCS", COMPONENT.ID) && EDGE.IS_VERSION_AT_LEAST("2024.10.2"))) {
            return "ActivePower";
        }
        return "ChargePower";
    }
}
