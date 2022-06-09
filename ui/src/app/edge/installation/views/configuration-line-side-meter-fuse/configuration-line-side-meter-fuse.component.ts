import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';

import { Ibn } from '../../installation-systems/abstract-ibn';

@Component({
  selector: ConfigurationLineSideMeterFuseComponent.SELECTOR,
  templateUrl: './configuration-line-side-meter-fuse.component.html'
})
export class ConfigurationLineSideMeterFuseComponent implements OnInit {

  private static readonly SELECTOR = "configuration-line-side-meter-fuse";

  @Input() public ibn: Ibn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<Ibn>();
  @Output() public setIbnEvent = new EventEmitter<Ibn>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  constructor() { }

  public ngOnInit() {
    this.form = new FormGroup({});
    this.fields = this.getFields();
    this.model = this.ibn.lineSideMeterFuse ?? {};
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {

    if (this.form.invalid) {
      return;
    }

    this.ibn.lineSideMeterFuse = this.model;
    this.setIbnEvent.emit(this.ibn);
    this.nextViewEvent.emit();
  }

  public getFields(): FormlyFieldConfig[] {

    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "fixedValue",
      type: "select",
      templateOptions: {
        label: "Wert [A]",
        description: "Mit welcher Stromstärke ist der Zähler abgesichert?",
        options: [
          { label: "35", value: 35 },
          { label: "50", value: 50 },
          { label: "63", value: 63 },
          { label: "80", value: 80 },
          { label: "Sonstige", value: -1 },
        ],
        required: true
      },
      parsers: [Number]
    });

    fields.push({
      key: "otherValue",
      type: "input",
      templateOptions: {
        type: "number",
        label: "Eigener Wert",
        min: 0,
        required: true
      },
      parsers: [Number],
      hideExpression: model => model.fixedValue !== -1
    });
    return fields;
  }

}