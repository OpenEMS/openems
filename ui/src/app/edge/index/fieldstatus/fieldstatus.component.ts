import { Component, Input } from '@angular/core';
import { Utils } from '../../../shared/shared';
import { CustomFieldDefinition } from '../../../shared/type/customfielddefinition';
import { CurrentDataAndSummary } from '../../../shared/edge/currentdata';

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
