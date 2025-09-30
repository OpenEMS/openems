import { Component, Input } from "@angular/core";
import { PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Service } from "src/app/shared/shared";

@Component({
    selector: "chartoptionspopover",
    templateUrl: "./POPOVER.COMPONENT.HTML",
    standalone: false,
})
export class ChartOptionsPopoverComponent {

    @Input() public showPhases: boolean | null = null;
    @Input() public showTotal: boolean | null = null;

    constructor(
        public service: Service,
        public popoverCtrl: PopoverController,
        public translate: TranslateService,
    ) { }

    public setPhases() {
        if (THIS.SHOW_PHASES == true) {
            THIS.SHOW_PHASES = false;
        } else if (THIS.SHOW_PHASES == false) {
            THIS.SHOW_PHASES = true;
        }
        THIS.POPOVER_CTRL.DISMISS(THIS.SHOW_PHASES, "Phases");
    }

    public setTotal() {
        if (THIS.SHOW_TOTAL == true) {
            THIS.SHOW_TOTAL = false;
        } else if (THIS.SHOW_TOTAL == false) {
            THIS.SHOW_TOTAL = true;
        }
        THIS.POPOVER_CTRL.DISMISS(THIS.SHOW_TOTAL, "Total");
    }

}
