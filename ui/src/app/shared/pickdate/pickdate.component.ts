import { Component } from '@angular/core';
import { Service } from '../shared';
import { PopoverController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { PickDatePopoverComponent } from './popover/popover.component';
import { DateService } from '../service/date.service';


@Component({
    selector: 'pickdate',
    templateUrl: './pickdate.component.html'
})


export class PickDateComponent {

    constructor(
        public service: Service,
        public translate: TranslateService,
        public popoverController: PopoverController,
        public dateService: DateService
    ) { }

    ngOnInit() {
        this.dateService.setPeriod('today');
    }


    async presentPopover(ev: any) {
        const popover = await this.popoverController.create({
            component: PickDatePopoverComponent,
            event: ev,
            translucent: false,
            cssClass: 'pickdate-popover',
        });
        return await popover.present();
    }
}
