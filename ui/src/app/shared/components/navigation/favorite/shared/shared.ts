import { TranslateService } from "@ngx-translate/core";
import { EnergyJourneyShared } from "src/app/edge/settings/energy-journey/shared/shared";
import { EdgeConfig, Edge } from "src/app/shared/shared";
import { ArrayUtils } from "src/app/shared/utils/array/array.utils";
import { StringUtils } from "src/app/shared/utils/string/string.utils";
import { SharedBottomNavigationBar } from "../../bottom-bar/shared";
import { NavigationTree, NavigationId } from "../../shared";

export namespace SharedFavorite {
    /**
     * Gets the favorite children of the navigation tree based on the edge's favorites settings.
     *
     * @param edge The edge
     * @param navigationTree The navigation tree
     * @returns All navigation trees that have been marked as favorites.
     */
    export function getFixedRouteChildren(
        edge: Edge,
        navigationTree: NavigationTree | null,
        translate: TranslateService,
    ): NavigationTree[] {
        if (navigationTree == null) {
            return [];
        }
        // Convert to absolute links for comparison with favorites

        const favorites: string[] = [
            ...(edge?.getFavoritesFromSettings()?.includes ?? []),
            ...(ArrayUtils.removeMatching(
                SharedFavorite.getPredefinedNodes(translate, edge?.getConfigSignal()(), edge),
                edge?.getFavoritesFromSettings()?.excludes ?? [],
            ) ?? []),
        ];

        // Build absolute links
        const absoluteNavigationTrees = navigationTree?.getAbsoluteNavigations()?.map((el) => el ?? null) ?? [];

        if (absoluteNavigationTrees == null) {
            return [];
        }

        const favoriteNodes = new Set<NavigationTree>();
        for (const favorite of favorites) {
            for (const absoluteNavigationTree of absoluteNavigationTrees) {
                const found = NavigationTree.findNodeByAbsoluteTree(absoluteNavigationTree, favorite);
                if (found == null || Array.from(favoriteNodes).some((node) => node.id === found.id)) {
                    continue;
                }
                favoriteNodes.add(found);
            }
        }

        // Convert to absolute links and set parent to favorites route
        const absoluteFavoriteNodes = Array.from(favoriteNodes).map((node) => {
            node.parent = absoluteNavigationTrees.find((tree) => tree.id === NavigationId.FAVORITES) ?? null;
            node = node.setParentRecursively();
            node.routerLink.baseString = StringUtils.replaceSegmentOrElse(
                node.routerLink.baseString,
                2,
                NavigationId.FAVORITES,
                node.routerLink.baseString,
            );

            if (node == null) {
                throw new Error("Route not found in navigation tree");
            }

            return node;
        });

        return absoluteFavoriteNodes;
    }

    export function getPredefinedNodes(translate: TranslateService, config: EdgeConfig, edge: Edge): string[] {
        return [
            EnergyJourneyShared.historyNavigationTree(translate).id,
            ...SharedBottomNavigationBar.getConsumptionChildren(config, edge, translate)
                .filter((el) => StringUtils.isNotInArr(el.id, edge?.getFavoritesFromSettings()?.excludes ?? []))
                .map((el) => el.id),
        ];
    }
}
