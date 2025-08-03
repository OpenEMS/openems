import { Component, Input, inject } from "@angular/core";
import { PopoverController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Service } from "src/app/shared/shared";

@Component({
    selector: "chartoptionspopover",
    templateUrl: "./popover.component.html",
    standalone: false,
})
export class ChartOptionsPopoverComponent {
    service = inject(Service);
    popoverCtrl = inject(PopoverController);
    translate = inject(TranslateService);


    @Input() public showPhases: boolean | null = null;
    @Input() public showTotal: boolean | null = null;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() { }

    public setPhases() {
        if (this.showPhases == true) {
            this.showPhases = false;
        } else if (this.showPhases == false) {
            this.showPhases = true;
        }
        this.popoverCtrl.dismiss(this.showPhases, "Phases");
    }

    public setTotal() {
        if (this.showTotal == true) {
            this.showTotal = false;
        } else if (this.showTotal == false) {
            this.showTotal = true;
        }
        this.popoverCtrl.dismiss(this.showTotal, "Total");
    }

}
