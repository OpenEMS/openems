import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { InstallationData } from '../../installation.component';

@Component({
  selector: ConfigurationSystemComponent.SELECTOR,
  templateUrl: './configuration-system.component.html'
})
export class ConfigurationSystemComponent implements OnInit {

  private static readonly SELECTOR = "configuration-system";
  private static readonly LINK_HOME_MANUAL = "https://www.fenecon.de/download/home-anleitung/";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<InstallationData>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  constructor() { }

  public ngOnInit() {

    // Initialize battery object
    this.installationData.battery ??= {};

    this.form = new FormGroup({});
    this.fields = this.getFields();
    this.model = this.installationData.battery.type ? { type: this.installationData.battery.type } : {};
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    if (this.form.invalid) {
      return;
    }

    this.installationData.battery.type = this.model.type;
    this.nextViewEvent.emit(this.installationData);
  }

  public getFields(): FormlyFieldConfig[] {

    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "type",
      type: "radio",
      templateOptions: {
        label: "Speichertyp",
        type: "radio",
        options: [
          { value: "fenecon-home", label: "FENECON Home" }
        ],
        required: true
      }
    });

    return fields;

  }

  public openInfocenter() {

    window.open(ConfigurationSystemComponent.LINK_HOME_MANUAL);

  }

}
