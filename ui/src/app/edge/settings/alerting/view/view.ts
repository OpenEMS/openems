import { Component, ChangeDetectionStrategy } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { AlertingComponent } from "../component/alerting.component";

@Component({
    selector: "oe-alerting-view",
    template: `
        <ion-content>
            <alerting></alerting>
        </ion-content>
    `,
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, AlertingComponent],
})
export class AlertingViewComponent {}
