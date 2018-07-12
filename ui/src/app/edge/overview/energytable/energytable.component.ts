import { Component, Input, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';

import { Utils } from '../../../shared/service/utils';
import { DefaultTypes } from '../../../shared/service/defaulttypes';
import { CurrentDataAndSummary } from '../../../shared/edge/currentdata';
import { Edge } from '../../../shared/edge/edge';

@Component({
  selector: 'energytable',
  templateUrl: './energytable.component.html'
})
export class EnergytableComponent {

  @Input()
  public currentData: CurrentDataAndSummary;

  @Input()
  public config: DefaultTypes.Config;

  // TODO this is only used to check the version
  @Input()
  public edge: Edge;

  constructor(public utils: Utils) { }
}
