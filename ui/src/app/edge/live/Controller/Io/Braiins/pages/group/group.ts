import { CommonModule } from "@angular/common";
import { Component, inject, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { EdgeConfig, Service } from "src/app/shared/shared";

@Component({
    selector: "oe-braiins-group",
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
        this.components =
            config
                ?.getComponentIdsByFactory("Controller.BraiinsOS.Single")
                ?.map((id) => config.getComponentSafely(id))
                .filter((c): c is EdgeConfig.Component => !!c && c.isEnabled) ??
            [];
    }

    public navigateTo(componentId: string): void {
        this.service.getCurrentEdge().then(async (edge) => {
            this.router.navigate([
                "/device",
                edge.id,
                "live",
                "controller",
                "braiins",
                componentId,
            ]);
        });
    }
}
