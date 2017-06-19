import { Component, Input, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';


import { Device, Data, Config, TemplateHelper } from '../../../shared/shared';

@Component({
  selector: 'energytable',
  templateUrl: './energytable.component.html'
})
export class EnergytableComponent {

  @Input()
  private currentData: Data;

  @Input()
  public config: Config;

  constructor(public tmpl: TemplateHelper) { }
}
