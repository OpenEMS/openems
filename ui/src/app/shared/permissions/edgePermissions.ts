import { Edge } from "../shared";

/**
 * Checks if the edge has the _PropertyMaximumGridFeedInLimit in the _meta component.
 *
 * @param edge The edge to check
 * @returns True if the edge has the _PropertyMaximumGridFeedInLimit in the _meta component, false otherwise
 */
export function hasMaximumGridFeedInLimitInMeta(edge: Edge): boolean {
    return edge.isVersionAtLeast("2025.9.1");
}
