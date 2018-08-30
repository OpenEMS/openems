import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../environments';

@Component({
    selector: 'settings',
    templateUrl: './settings.component.html'
})
export class SettingsComponent {

    public env = environment;

    constructor(
        public translate: TranslateService
    ) { }

    public toggleDebugMode($event: any /*MdSlideToggleChange*/) {
        this.env.debugMode = $event.checked;
    }
}
