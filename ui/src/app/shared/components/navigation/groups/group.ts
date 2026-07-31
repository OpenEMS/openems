import { CommonModule } from "@angular/common";
import { Component, OnInit, inject, signal } from "@angular/core";
import { Router } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { RouteService } from "src/app/shared/service/route.service";
import { Service } from "src/app/shared/shared";
import { Widgets } from "src/app/shared/type/widgets";
import { EdgeConfig } from "../../edge/edgeconfig";

@Component({
    standalone: true,
    imports: [CommonModule, IonicModule, TranslateModule],
    templateUrl: "./group.html",
})
export class ControllerGroupListComponent implements OnInit {
    protected components = signal<EdgeConfig.Component[]>([]);
    protected titleKey = signal<string | null>(null);

    private readonly service = inject(Service);
    private readonly router = inject(Router);
    private readonly routeService = inject(RouteService);
    private readonly translate = inject(TranslateService);

    ngOnInit(): void {
        void this.initialize();
    }

    public async navigateTo(componentId: string): Promise<void> {
        const currentUrl = this.routeService.getCurrentUrl();
        const cleanedUrl = currentUrl?.split("?")[0] ?? "/";
        await this.router.navigate([cleanedUrl, componentId]);
    }

    private async initialize(): Promise<void> {
        const factoryId = this.routeService.getQueryParam<string>("factoryId");

        if (factoryId == null) {
            return;
        }

        const edge = await this.service.getCurrentEdge();
        const config = edge.getCurrentConfig();

        if (config == null) {
            return;
        }

        this.components.set(
            (config?.getComponentIdsByFactory(factoryId) ?? [])
                .map((id) => config?.getComponentSafely(id))
                .filter((c): c is EdgeConfig.Component => !!c && c.isEnabled),
        );

        const componentIds = this.components().map((c) => c.id);

        const groupedTree =
            Widgets.GROUPED_FACTORIES[factoryId]?.grouped(this.translate, componentIds, config, factoryId) ?? null;

        if (groupedTree == null) {
            return;
        }
        this.titleKey.set(groupedTree.label);
    }
}
