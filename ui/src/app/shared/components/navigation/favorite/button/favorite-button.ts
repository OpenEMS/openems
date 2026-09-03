import { Component, computed, effect, inject, signal, untracked } from "@angular/core";

import { TranslateService } from "@ngx-translate/core";

import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { User } from "src/app/shared/jsonrpc/shared";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { RouteService } from "src/app/shared/service/route/route.service";
import { UserService } from "src/app/shared/service/user.service";
import { Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { OeSet } from "src/app/shared/type/set";
import { ArrayUtils } from "src/app/shared/utils/array/array.utils";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { StringUtils } from "src/app/shared/utils/string/string.utils";
import { environment } from "src/environments";

import { Edge, EdgeSettings } from "../../../edge/edge";
import { NavigationService } from "../../service/navigation.service";
import { NavigationId, NavigationTree } from "../../shared";
import de from "../i18n/de.json";
import en from "../i18n/en.json";
import { SharedFavorite } from "../shared/shared";

@Component({
    selector: "oe-favorite-button",
    templateUrl: "./favorite-button.html",
    styleUrl: "./favorite-button.scss",
    imports: [CommonUiModule, PipeComponentsModule],
})
export class FavoriteButtonComponent {
    protected readonly navigationService = inject(NavigationService);
    protected readonly routeService = inject(RouteService);
    protected readonly service = inject(Service);
    protected readonly routesForbidden = signal<string[]>([]);

    protected readonly currentUrl = computed(() => {
        const _currentUrl = this.routeService.currentUrl();
        return this.routeService.getCurrentUrlWithoutLeading()?.split("?")?.[0] ?? "";
    });

    protected readonly currentNode = computed(() => this.navigationService.currentNode());
    protected readonly favoriteIds = signal<string[]>([]);
    protected readonly favoriteMessage = signal<string | null>(null);
    protected readonly isVisible = computed<boolean>(() => {
        const currentEdge = this.service.currentEdge();
        const currentNode = this.navigationService.currentNode();
        if (currentEdge == null || currentEdge.role != Role.OWNER) {
            return false;
        }

        return (
            currentNode?.hideFavorite == false &&
            currentEdge.roleIsAtLeast(Role.OWNER) &&
            currentEdge.isOnline &&
            environment.backend == "OpenEMS Backend"
        );
    });

    private readonly translate = inject(TranslateService);
    private readonly userService = inject(UserService);
    private readonly platFormService = inject(PlatFormService);
    private favoriteMessageTimeout: ReturnType<typeof setTimeout> | null = null;

    constructor() {
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });
        effect(() => {
            const tree = this.navigationService.navigationTree();

            untracked(() => {
                this.routesForbidden.set([
                    ...(tree?.getChildren()?.map((child) => child.routerLink.baseString) ?? []),
                    ...(tree
                        ?.getAbsoluteNavigations()
                        ?.filter((child) => child.hideFavorite)
                        ?.map((child) => child.routerLink.baseString) ?? []),
                ]);
                const currentEdge = this.service.currentEdge();
                const config = currentEdge?.getConfigSignal()();

                if (config == null) {
                    return;
                }

                this.favoriteIds.set([
                    ...(currentEdge?.getFavoritesFromSettings()?.includes ?? []),
                    ...(ArrayUtils.removeMatching(
                        SharedFavorite.getPredefinedNodes(this.translate, config, currentEdge),
                        currentEdge?.getFavoritesFromSettings()?.excludes ?? [],
                    ) ?? []),
                ]);
            });
        });
    }

    /**
     * Gets the favorites children of the navigation tree.
     *
     * @param navigationTree The navigationTree
     * @returns All navigation tree children for the favorites route
     */
    private static getFavoriteRouteChildren(navigationTree: NavigationTree | null): NavigationTree[] {
        return (
            navigationTree?.children
                .filter((el) => el.id === NavigationId.FAVORITES)
                .map((el) => el.children)
                .flat()
                .map((el) => {
                    /* Replacing segment after live with {@link NavigationId.FAVORITES} **/
                    // StringUtils.replaceSegment(el.routerLink.baseString, 2, NavigationId.FAVORITES);
                    return el;
                }) ?? []
        );
    }

    /**
     * Toggles the favorite status of the current navigation node. If the current node is already a favorite, it will be
     * removed from the favorites list; otherwise, it will be added. The method updates the edge settings and refreshes
     * the navigation tree accordingly.
     *
     * @returns
     */
    protected async toggleFavorite(): Promise<void> {
        const edge = this.service.currentEdge();
        const currentNode = this.navigationService.currentNode();
        const user = this.userService.currentUser();

        if (currentNode == null || edge == null || user == null) {
            return;
        }

        const currentNavigationTree =
            this.navigationService
                .navigationTree()
                ?.getAbsoluteNavigations()
                ?.filter((node) => node.id === currentNode?.id)?.[0] ?? null;

        AssertionUtils.assertIsDefined(currentNavigationTree, "Id for node not found");

        const predefinedNodes = SharedFavorite.getPredefinedNodes(this.translate, edge.getConfigSignal()(), edge);

        const favorites = {
            includes: new OeSet(edge?.getFavoritesFromSettings()?.includes ?? []),
            excludes: new OeSet(edge?.getFavoritesFromSettings()?.excludes ?? []),
        };

        let isRemoved: boolean = false;

        if (StringUtils.isInArr(currentNavigationTree.id, predefinedNodes)) {
            const excludesSize = favorites.excludes.size;
            favorites.excludes.toggle(currentNavigationTree.id);
            isRemoved = favorites.excludes.size > excludesSize;
        } else {
            const includesSize = favorites.includes.size;
            favorites.includes.toggle(currentNavigationTree.id);
            isRemoved = favorites.includes.size < includesSize;
        }

        await edge.updateEdgeSettingsWithProperty(
            EdgeSettings.FAVORITES,
            {
                includes: Array.from(favorites.includes),
                excludes: Array.from(favorites.excludes),
            },
            this.service.websocket,
        );

        this.showFavoriteMessage(isRemoved);

        const updatedNavigationTree = await this.getUpdatedFavoriteNavigationTree(edge, user);
        this.navigationService.navigationTree.set(updatedNavigationTree);
        this.favoriteIds.set(Array.from(favorites.includes));
    }

    private showFavoriteMessage(isRemoved: boolean): void {
        if (this.favoriteMessageTimeout !== null) {
            clearTimeout(this.favoriteMessageTimeout);
        }

        this.favoriteMessage.set(
            isRemoved
                ? this.translate.instant("FAVORITES.REMOVED_FAVORITE")
                : this.translate.instant("FAVORITES.ADDED_FAVORITE"),
        );

        this.favoriteMessageTimeout = setTimeout(() => {
            this.favoriteMessage.set(null);
            this.favoriteMessageTimeout = null;
        }, 3000);
    }

    private async getUpdatedFavoriteNavigationTree(edge: Edge, user: User): Promise<NavigationTree | null> {
        let navigationTree = await NavigationService.createNavigationTree(
            this.translate,
            edge,
            this.service,
            user,
            this.platFormService,
        );

        if (navigationTree == null) {
            return null;
        }

        navigationTree.children = navigationTree.children.map((el) => {
            if (el.id === NavigationId.FAVORITES) {
                // Update navigation tree children in favorites
                el.children = FavoriteButtonComponent.getFavoriteRouteChildren(navigationTree);
            }
            return el;
        });

        // Finalize new tree
        navigationTree = navigationTree.setParentRecursively();
        navigationTree.reorderByShowOrder(navigationTree);
        return NavigationTree.of(NavigationService.convertRelativeToAbsoluteLink(structuredClone(navigationTree)));
    }
}
