import { Component, Input, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';


import { Device, Data, Config, TemplateHelper, LABELS } from '../../../shared/shared';

@Component({
  selector: 'energytable',
  templateUrl: './energytable.component.html'
})
export class EnergytableComponent {

  @Input()
  public currentData: Data;

  @Input()
  public config: Config;

  public labels = LABELS;

  constructor(public tmpl: TemplateHelper) { }
}
