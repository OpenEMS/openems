import { CommonModule } from "@angular/common";
import { Component, effect, inject, signal } from "@angular/core";
import { Router } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { RouteService } from "src/app/shared/service/route.service";
import { Edge, Service } from "src/app/shared/shared";
import { Widgets } from "src/app/shared/type/widgets";
import { ArrayUtils } from "src/app/shared/utils/array/array.utils";
import { EdgeConfig } from "../../edge/edgeconfig";
import { ForwardNavigationOptions } from "../bottom-bar/forward-navigation-options/forward-navigation-options";
import { NavigationLabelLineComponent } from "../label-line/label-line";
import { NavigationTree } from "../shared";

@Component({
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
        TranslateModule,
        PipeComponentsModule,
        NavigationLabelLineComponent,
        ForwardNavigationOptions,
    ],
    templateUrl: "./group.html",
})
export class ControllerGroupListComponent {
    protected titleKey = signal<NavigationTree["label"] | null>(null);

    private readonly service = inject(Service);
    private readonly router = inject(Router);
    private readonly routeService = inject(RouteService);
    private readonly translate = inject(TranslateService);

    constructor() {
        effect(() => {
            const edge = this.service.currentEdge();
            if (edge != null) {
                this.initialize(edge);
            }
        });
        this.titleKey.set(null);
    }

    public async navigateTo(componentId: string): Promise<void> {
        const currentUrl = this.routeService.getCurrentUrl();
        const cleanedUrl = currentUrl?.split("?")[0] ?? "/";
        await this.router.navigate([cleanedUrl, componentId]);
    }

    private async initialize(edge: Edge): Promise<void> {
        const factoryId = this.routeService.getQueryParam<string>("factoryId");

        if (factoryId == null) {
            return;
        }

        const config = await edge.getFirstValidConfig(this.service.websocket);

        if (config == null) {
            return;
        }

        const componentIds = ArrayUtils.sortedAlphabetically(
            (config?.getComponentIdsByFactory(factoryId) ?? [])
                .map((id) => config?.getComponentSafely(id))
                .filter((c): c is EdgeConfig.Component => !!c && c.isEnabled),
            (c) => c.alias,
        ).map((el) => el.id);

        const groupedTree =
            Widgets.GROUPED_FACTORIES[factoryId]?.grouped(this.translate, componentIds, config, factoryId) ?? null;
        if (groupedTree == null) {
            return;
        }

        this.titleKey.set(groupedTree.label);
    }
}
