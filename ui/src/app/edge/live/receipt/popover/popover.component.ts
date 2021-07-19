import { Component, Input } from '@angular/core';
import { EdgeConfig, Service } from 'src/app/shared/shared';
import { EvcsComponents } from 'src/app/shared/edge/edgeconfig';
import { PopoverController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';


@Component({
    selector: 'receiptpopover',
    templateUrl: './popover.component.html'
})
export class ReceiptPopoverComponent {

    @Input() public evcsComponents: EvcsComponents[] = [];

    constructor(
        public service: Service,
        public popoverCtrl: PopoverController,
        public translate: TranslateService,
    ) { }

    public getComponentClosePopover(component: EdgeConfig.Component) {
        this.popoverCtrl.dismiss(component);
    }
}
