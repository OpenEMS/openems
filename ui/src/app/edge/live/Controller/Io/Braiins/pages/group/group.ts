import { CommonModule } from "@angular/common";
import { Component, inject, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { EdgeConfig, Service } from "src/app/shared/shared";

@Component({
    selector: "oe-controller-braiins-group",
    standalone: true,
    imports: [CommonModule, IonicModule, TranslateModule],
    providers: [{ provide: DataService, useClass: LiveDataService }],
    templateUrl: "./group.html",
})
export class ControllerBraiinsGroupComponent implements OnInit {
    protected components: EdgeConfig.Component[] = [];

    private readonly service: Service = inject(Service);
    private readonly router: Router = inject(Router);

    public async ngOnInit(): Promise<void> {
        const edge = await this.service.getCurrentEdge();
        const config = edge.getCurrentConfig();

        if (config == null) {
            this.components = [];
            return;
        }

        const componentIds =
            config.getComponentIdsByFactory("Controller.BraiinsOS.Single") ??
            [];
        this.components = componentIds
            .map((id) => config.getComponentSafely(id))
            .filter(
                (component): component is EdgeConfig.Component =>
                    component != null && component.isEnabled,
            );
    }

    public async navigateTo(componentId: string): Promise<void> {
        const edge = await this.service.getCurrentEdge();
        await this.router.navigate([
            "/device",
            edge.id,
            "live",
            "controller",
            "braiins",
            componentId,
        ]);
    }
}
