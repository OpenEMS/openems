import { Component, computed, effect, inject, OnDestroy } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { PlatFormService } from "src/app/platform.service";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { ForwardNavigationOptions } from "../../bottom-bar/forward-navigation-options/forward-navigation-options";
import { NavigationLabelLineComponent } from "../../label-line/label-line";
import { NavigationService } from "../../service/navigation.service";
import { NavigationTree } from "../../shared";
import cz from "../i18n/cz.json";
import de from "../i18n/de.json";
import en from "../i18n/en.json";
import es from "../i18n/es.json";
import fr from "../i18n/fr.json";
import ja from "../i18n/ja.json";
import lt from "../i18n/lt.json";
import nl from "../i18n/nl.json";
import sv from "../i18n/sv.json";

@Component({
    selector: "oe-favorite-page",
    templateUrl: "./favorite-page.html",
    imports: [CommonUiModule, ComponentsBaseModule, ForwardNavigationOptions],
})
export class FavoritePageComponent implements OnDestroy {
    protected readonly service = inject(Service);
    protected readonly navigationService = inject(NavigationService);
    protected readonly translate = inject(TranslateService);
    protected readonly platFormService = inject(PlatFormService);

    protected readonly children = computed(() => {
        const currentNode = this.navigationService.currentNode();
        if (currentNode == null) {
            return null;
        }
        return this.filterVisibleNodes(this.navigationService.currentNode()?.getChildren() ?? []);
    });

    protected readonly description = computed(() => {
        const currentEdge = this.service.currentEdge();
        if (currentEdge == null) {
            return null;
        }
        return currentEdge.role === Role.OWNER
            ? this.translate.instant("FAVORITES.DESCRIPTION_OWNER")
            : this.translate.instant("FAVORITES.DESCRIPTION_OTHERS");
    });

    constructor() {
        Language.normalizeAdditionalTranslationFiles({
            cs: cz,
            de: de,
            en: en,
            es: es,
            fr: fr,
            ja: ja,
            lt: lt,
            nl: nl,
            sv: sv,
        }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });

        effect(() => {
            const currentNode = this.navigationService.currentNode();
            if (currentNode == null) {
                return;
            }
            const headerTitle = NavigationLabelLineComponent.getDisplayLabel(currentNode.label, this.platFormService);
            this.navigationService.headerTitle.set({
                text: headerTitle,
                useCiFont: true,
            });
        });
    }

    /**
     * Filters out the nodes to hide in navigation.
     *
     * @param nodes The navigation tree nodes
     * @returns The adjusted nodes
     */
    public filterVisibleNodes(nodes: NavigationTree[]): NavigationTree[] {
        const newNodes = nodes
            .filter((node) => node.showOrder !== "HIDE") // keep only locally visible nodes
            .map((node) => ({
                ...node,
                children: node.children ? this.filterVisibleNodes(node.children) : [],
            })) as NavigationTree[];
        return newNodes;
    }

    ngOnDestroy() {
        this.navigationService.headerTitle.set({ text: null });
    }
}
