import { Component, inject, OnInit, ChangeDetectionStrategy } from "@angular/core";
import { Router } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { EdgeConfig, Service } from "src/app/shared/shared";

@Component({
    selector: "oe-heating-room-group",
    standalone: true,
    imports: [IonicModule, TranslateModule],
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    templateUrl: "./group.html",
})
export class IoHeatingRoomGroupComponent implements OnInit {
    protected components: EdgeConfig.Component[] = [];

    private readonly service: Service = inject(Service);
    private readonly router: Router = inject(Router);

    public async ngOnInit(): Promise<void> {
        const edge = await this.service.getCurrentEdge();
        const config = edge.getCurrentConfig();
        this.components =
            config
                ?.getComponentIdsByFactory("Controller.IO.Heating.Room")
                ?.map((id) => config.getComponentSafely(id))
                .filter((c): c is EdgeConfig.Component => !!c && c.isEnabled) ?? [];
    }

    public navigateTo(componentId: string): void {
        this.service.getCurrentEdge().then(async (edge) => {
            this.router.navigate(["/device", edge.id, "live", "io-heating-room", componentId]);
        });
    }
}
