import { Component, Input } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { Subject } from 'rxjs/Subject';

import { Websocket, Utils } from '../../../shared/shared';
import { CustomFieldDefinition } from '../../../shared/type/customfielddefinition';
import { CurrentDataAndSummary } from '../../../shared/device/currentdata';

@Component({
  selector: 'fieldstatus',
  templateUrl: './fieldstatus.component.html'
})
export class FieldstatusComponent {

  @Input()
  public currentData: CurrentDataAndSummary;

  @Input()
  public fielddefinition: CustomFieldDefinition;

  constructor(public utils: Utils) { }
}
