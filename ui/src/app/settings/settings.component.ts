import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Websocket } from '../shared/shared';

@Component({
    selector: 'settings',
    templateUrl: './settings.component.html'
})
export class SettingsComponent {
    constructor(
        public translate: TranslateService
    ) { }

    ngOnInit() {

    }
}
