import { Component, EventEmitter, Input, Output } from "@angular/core";
import { PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";

import { Service } from "../../shared";
// @ts-strict-ignore
import { ChartOptionsPopoverComponent } from "./popover/POPOVER.COMPONENT";

@Component({
    selector: "chartOptions",
    templateUrl: "./CHARTOPTIONS.COMPONENT.HTML",
    standalone: false,
})
export class ChartOptionsComponent {

    @Input({ required: true }) public showPhases!: boolean | null;
    @Input({ required: true }) public showTotal!: boolean | null;
    @Output() public setShowPhases = new EventEmitter<boolean>();
    @Output() public setShowTotal = new EventEmitter<boolean>();

    constructor(
        public service: Service,
        public translate: TranslateService,
        public popoverCtrl: PopoverController,
    ) { }

    async presentPopover(ev: any) {
        const componentProps = {};
        if (THIS.SHOW_PHASES !== null) {
            componentProps["showPhases"] = THIS.SHOW_PHASES;
        }
        if (THIS.SHOW_TOTAL !== null) {
            componentProps["showTotal"] = THIS.SHOW_TOTAL;
        }
        const popover = await THIS.POPOVER_CTRL.CREATE({
            component: ChartOptionsPopoverComponent,
            event: ev,
            translucent: false,
            componentProps: componentProps,
        });
        await POPOVER.PRESENT();
        POPOVER.ON_DID_DISMISS().then((data) => {
            if (data["role"] == "Phases" && data["data"] == true) {
                THIS.SET_SHOW_PHASES.EMIT(true);
            } else if (data["role"] == "Phases" && data["data"] == false) {
                THIS.SET_SHOW_PHASES.EMIT(false);
            }
            if (data["role"] == "Total" && data["data"] == true) {
                THIS.SET_SHOW_TOTAL.EMIT(true);
            } else if (data["role"] == "Total" && data["data"] == false) {
                THIS.SET_SHOW_TOTAL.EMIT(false);
            }
        });
        await POPOVER.PRESENT();
    }
}
