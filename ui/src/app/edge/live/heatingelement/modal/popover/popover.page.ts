import { Component, Input } from '@angular/core';
import { EdgeConfig } from '../../../../../shared/shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'heatingelement-popover',
    templateUrl: './popover.page.html'
})
export class HeatingelementPopoverComponent {

    @Input() public controller: EdgeConfig.Component;

    constructor(
        protected translate: TranslateService,
    ) { }
}