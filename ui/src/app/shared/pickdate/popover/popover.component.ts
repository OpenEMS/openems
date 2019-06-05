import { Component } from '@angular/core';
import { Service } from '../../shared';
import { PopoverController } from '@ionic/angular';

@Component({
    selector: 'pickdatepopover',
    templateUrl: './popover.component.html'
})
export class PickDatePopoverComponent {

    constructor(
        public service: Service,
        public popoverController: PopoverController
    ) { }

    ngOnInit() { }

    ngOnDestroy() { }
}
