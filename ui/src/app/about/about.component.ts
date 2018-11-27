import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Websocket } from '../shared/shared';


@Component({
  selector: 'about',
  templateUrl: './about.component.html'
})
export class AboutComponent {
  constructor(
    public translate: TranslateService
  ) { }

}
