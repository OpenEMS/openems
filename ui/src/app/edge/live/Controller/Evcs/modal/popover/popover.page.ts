import { Component, Input } from '@angular/core';
import { EdgeConfig } from '../../../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'Controller_Evcs-popover',
    templateUrl: './popover.page.html'
})
export class Controller_EvcsPopoverComponent {

    @Input() public controller: EdgeConfig.Component;

    constructor(
        protected translate: TranslateService
    ) { }
}