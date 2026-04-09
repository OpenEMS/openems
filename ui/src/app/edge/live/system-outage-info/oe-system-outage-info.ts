import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { IonicModule } from "@ionic/angular";
import { TranslateModule } from "@ngx-translate/core";
import { SystemStateService } from "src/app/shared/service/systemStateService";
import { environment } from "src/environments";

@Component({
    selector: "oe-system-outage-info",
    standalone: true,
    imports: [
        IonicModule,
        CommonModule,
        TranslateModule,
    ],
    templateUrl: "./oe-system-outage-info.html",
})
export class SystemOutageInfoComponent {
    protected readonly environment = environment;
    protected systemState: SystemStateService = inject(SystemStateService);
}
