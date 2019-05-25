import { Component } from '@angular/core';
import { Service } from '../../shared';

@Component({
    selector: 'pickdatepopover',
    templateUrl: './pickdate-popover.component.html'
})
export class PickDatePopoverComponent {

    constructor(
        public service: Service,
    ) { }

    ngOnInit() { }

    ngOnDestroy() { }
}
