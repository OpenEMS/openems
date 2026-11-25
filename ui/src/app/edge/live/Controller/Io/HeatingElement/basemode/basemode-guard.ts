import { inject } from "@angular/core";
import { CanActivateFn, createUrlTreeFromSnapshot, RedirectCommand } from "@angular/router";
import { Edge, Service } from "src/app/shared/shared";
import { HeatingElementComponent } from "../util/utils";

export const canActivateHeatingElementBaseMode: CanActivateFn = async (route, state) => {
    const service = inject(Service);
    const edgeId = route.pathFromRoot.map((snapshot) => snapshot.paramMap.get("edgeId")).find((id) => id != null);

    // Resolve the routed Edge before activation; the routing parent selects it only after guards have completed.
    if (edgeId != null) {
        let edge: Edge | null = null;
        try {
            edge = await service.updateCurrentEdge(edgeId);
        } catch (error) {
            console.warn(`Failed to load Edge [${edgeId}] for HeatingElement base mode`, error);
        }
        if (
            edge != null &&
            edge.id === edgeId &&
            edge.role != null &&
            new HeatingElementComponent(null, edge).canUseTwoHeatingElementsAndFastHeat()
        ) {
            return true;
        }
    }

    console.warn(`Routing Failed. Reason: HeatingElement base mode not available for Edge [${edgeId}]`);
    return new RedirectCommand(createUrlTreeFromSnapshot(route, ["../"], state.root.queryParams, state.root.fragment), {
        replaceUrl: true,
    });
};
