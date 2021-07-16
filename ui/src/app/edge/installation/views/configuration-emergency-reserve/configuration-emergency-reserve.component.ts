import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { InstallationData } from '../../installation.component';

@Component({
  selector: ConfigurationEmergencyReserveComponent.SELECTOR,
  templateUrl: './configuration-emergency-reserve.component.html'
})
export class ConfigurationEmergencyReserveComponent implements OnInit {

  private static readonly SELECTOR = "configuration-emergency-reserve";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<InstallationData>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  constructor() { }

  public ngOnInit() {

    this.form = new FormGroup({});
    this.fields = this.getFields();
    this.model = this.installationData.battery.emergencyReserve ?? {};

  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {

    if (this.form.invalid) {
      return;
    }

    this.installationData.battery.emergencyReserve = this.model;
    this.nextViewEvent.emit(this.installationData);

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
      key: "value",
      type: "range",
      templateOptions: {
        label: "Wert [%]",
        description: "Aktuell: 20",
        required: true,
        min: 0,
        max: 100,
        change: (field, event) => { field.templateOptions.description = "Aktuell: " + field.formControl.value; }
      },
      hideExpression: model => !model.isEnabled
    });

    return fields;

  }

}
