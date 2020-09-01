import { Component, Input } from '@angular/core';
import { EdgeConfig } from '../../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'evcs-popover',
    templateUrl: './popover.page.html'
})
export class EvcsPopoverComponent {

    @Input() public controller: EdgeConfig.Component | null = null;

    constructor(
        protected translate: TranslateService,
    ) { }
}