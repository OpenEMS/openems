import { Component, Input, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';

import { Device } from '../../../shared/device/device';
import { Utils } from '../../../shared/service/utils';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { CurrentDataAndSummary } from '../../../shared/device/currentdata';

@Component({
  selector: 'state',
  templateUrl: './state.component.html'
})
export class StateComponent {

  @Input()
  public device: Device;

  @Input()
  public currentData: CurrentDataAndSummary;

  @Input()
  public config: DefaultTypes.Config;

  constructor(public utils: Utils) { }
}
