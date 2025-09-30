import { Edge } from "src/app/shared/shared";

export namespace AssistantEdgePermission {

    /**
     * Checks if reactive chart is allowed
     *
     * @param edge the edge
     * @returns true, if edge is updated to at least 2025.7.4
     */
    export function isReactiveChartAllowed(edge: Edge | null): boolean {
        if (edge == null) {
            return false;
        }
        return EDGE.IS_VERSION_AT_LEAST("2025.8.1");
    }
}
