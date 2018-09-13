import { Component, Input, OnDestroy } from '@angular/core';

import { Utils } from '../../../shared/service/utils';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { CurrentDataAndSummary_2018_8 } from '../../../shared/edge/currentdata.2018.8';

@Component({
  selector: 'energytable-2018-8',
  templateUrl: './energytable.component.html'
})
export class EnergytableComponent_2018_8 {

  @Input()
  public currentData: CurrentDataAndSummary_2018_8;

  @Input()
  public config: DefaultTypes.Config_2018_8;

  constructor(public utils: Utils) { }
}
