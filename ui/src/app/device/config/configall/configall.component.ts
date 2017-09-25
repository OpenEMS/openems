import { Component } from '@angular/core';

import { AbstractConfigComponent } from '../shared/abstractconfig.component';
import { ConfigImpl } from '../../../shared/device/config';

import * as moment from 'moment';

@Component({
  selector: 'configall',
  templateUrl: '../shared/abstractconfig.component.html'
})
export class ConfigAllComponent extends AbstractConfigComponent {
}