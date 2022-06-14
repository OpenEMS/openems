import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';

import { Ibn } from '../../installation-systems/abstract-ibn';
import { HomeFeneconIbn } from '../../installation-systems/home-fenecon';

@Component({
  selector: ConfigurationEmergencyReserveComponent.SELECTOR,
  templateUrl: './configuration-emergency-reserve.component.html'
})
export class ConfigurationEmergencyReserveComponent implements OnInit {

  private static readonly SELECTOR = "configuration-emergency-reserve";

  @Input() public ibn: HomeFeneconIbn;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<any>();
  @Output() public setIbnEvent = new EventEmitter<Ibn>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  constructor() { }

  public ngOnInit() {

    this.form = new FormGroup({});
    this.fields = this.getFields();
    this.model = this.ibn.emergencyReserve ?? {
      isEnabled: true,
      value: 20,
      isReserveSocEnabled: false
    };

  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {

    if (this.form.invalid) {
      return;
    }

    this.ibn.emergencyReserve = this.model;
    this.setIbnEvent.emit(this.ibn);
    this.ibn.setRequiredControllers();
    this.nextViewEvent.emit();
  }

  public getFields(): FormlyFieldConfig[] {

    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "isEnabled",
      type: "checkbox",
      templateOptions: {
        label: "Soll die Notstromfunktion aktiviert werden?",
        required: true
      }
    });

    fields.push({
      key: "isReserveSocEnabled",
      type: "toggle",
      templateOptions: {
        label: "Notstromreserve aktivieren?",
        required: true,
      },
      hideExpression: model => !model.isEnabled
    });

    fields.push({
      key: "value",
      type: "range",
      templateOptions: {
        label: "Wert [%]",
        description: "Aktuell: 20",
        required: true,
        min: 5,
        max: 100,
        attributes: {
          pin: "true"
        },
        change: (field, event) => { field.templateOptions.description = "Aktuell: " + field.formControl.value; }
      },
      parsers: [Number],
      hideExpression: model => !model.isEnabled || !model.isReserveSocEnabled
    });

    return fields;

  }

}
