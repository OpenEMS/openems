import { ChartOptionsPopoverComponent } from './popover/popover.component';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PopoverController } from '@ionic/angular';
import { Service } from '../shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'chartOptions',
    templateUrl: './chartoptions.component.html'
})
export class ChartOptionsComponent {

    @Input() public showPhases: boolean | null;
    @Input() public showTotal: boolean | null;
    @Output() public setShowPhases = new EventEmitter<boolean>();
    @Output() public setShowTotal = new EventEmitter<boolean>();

    constructor(
        public service: Service,
        public translate: TranslateService,
        public popoverCtrl: PopoverController
    ) { }

    async presentPopover(ev: any) {
        let componentProps = {};
        if (this.showPhases !== null) {
            componentProps['showPhases'] = this.showPhases;
        }
        if (this.showTotal !== null) {
            componentProps['showTotal'] = this.showTotal;
        }
        const popover = await this.popoverCtrl.create({
            component: ChartOptionsPopoverComponent,
            event: ev,
            translucent: false,
            componentProps: componentProps
        });
        await popover.present();
        popover.onDidDismiss().then((data) => {
            if (data['role'] == "Phases" && data['data'] == true) {
                this.setShowPhases.emit(true);
            } else if (data['role'] == "Phases" && data['data'] == false) {
                this.setShowPhases.emit(false);
            }
            if (data['role'] == "Total" && data['data'] == true) {
                this.setShowTotal.emit(true);
            } else if (data['role'] == "Total" && data['data'] == false) {
                this.setShowTotal.emit(false);
            }
        });
        await popover.present();
    }
}
