import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'settings',
    templateUrl: './settings.component.html'
})
export class SettingsComponent {
    constructor(public translate: TranslateService) {

    }
}
