import { Component, Input } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { CommonUiModule } from "../../common-ui.module";
import { DefaultTypes } from "../../type/defaulttypes";
import { PickdateComponentModule } from "./pickdate.module";

@Component({
    selector: "oe-pickdate-standalone",
    template: `
        <ion-buttons class="ion-justify-content-center">
            <ion-button>
                <pickdate [historyPeriods]="historyPeriods"></pickdate>
            </ion-button>
        </ion-buttons>
    `,
    imports: [CommonUiModule, PickdateComponentModule],
})
export class PickDateStandaloneComponent {
    @Input() public historyPeriods: DefaultTypes.PeriodStringValues[] = [];
    @Input() public form: FormGroup = new FormGroup({});
}
