import { Component } from '@angular/core';
import { PopoverController } from '@ionic/angular';
import { DateService } from '../../service/date.service';

@Component({
    selector: 'pickdatepopover',
    templateUrl: './popover.component.html'
})


export class PickDatePopoverComponent {

    constructor(
        public popoverController: PopoverController,
        public dateService: DateService
    ) { }

    ngOnInit() { }

    ngOnDestroy() { }

}
