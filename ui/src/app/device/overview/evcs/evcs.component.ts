import { Component, Input, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';

import { Utils } from '../../../shared/service/utils';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { CurrentDataAndSummary } from '../../../shared/device/currentdata';

@Component({
  selector: 'evcs',
  templateUrl: './evcs.component.html'
})
export class EvcsComponent {

  @Input()
  public currentData: CurrentDataAndSummary;

  @Input()
  public config: DefaultTypes.Config;

  constructor(public utils: Utils) { }
}
