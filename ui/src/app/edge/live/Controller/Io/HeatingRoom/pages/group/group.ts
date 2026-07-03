import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { Router } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

@Component({
    selector: "oe-heating-room-group",
    standalone: true,
    imports: [CommonModule, IonicModule, TranslateModule],
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    templateUrl: "./group.html",
    styles: [`
        ion-card {
            margin: 16px;
        }
    `],
})
export class HeatingRoomGroupComponent {

    protected components: EdgeConfig.Component[] = [];

    private readonly service: Service = inject(Service);
    private readonly router: Router = inject(Router);

    public constructor() {
        this.service.getCurrentEdge().then(async edge => {

            const config = edge.getCurrentConfig();
            AssertionUtils.assertIsDefined(config);

            this.components = config.getComponentIdsByFactory("Controller.IO.Heating.Room")
                ?.map(id => config.getComponent(id))
                .filter((c): c is EdgeConfig.Component => c != null && c.isEnabled) ?? [];
        });
    }

    public navigateTo(componentId: string): void {
        this.service.getCurrentEdge().then(async edge => {
            this.router.navigate(["/device", edge.id, "live", "io-heating-room", componentId]);
        });
    }
}
