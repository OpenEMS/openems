import { Component, Input, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';

import { Utils } from '../../../shared/service/utils';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { CurrentDataAndSummary } from '../../../shared/device/currentdata';

@Component({
  selector: 'energytable',
  templateUrl: './energytable.component.html'
})
export class EnergytableComponent {

  @Input()
  public currentData: CurrentDataAndSummary;

  @Input()
  public config: DefaultTypes.Config;

  constructor(public utils: Utils) { }
}
