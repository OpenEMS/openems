import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { environment } from 'src/environments';
import { InstallationData } from '../../installation.component';

@Component({
  selector: ConfigurationSystemComponent.SELECTOR,
  templateUrl: './configuration-system.component.html'
})
export class ConfigurationSystemComponent implements OnInit {

  private static readonly SELECTOR = "configuration-system";
  private readonly LINK_HOME_MANUAL = "https://www.fenecon.de/download/home-anleitung/";
  private readonly LINK_SYMPHON_E_MANUAL = "https://www.heckertsolar.com/wp-content/uploads/2022/03/Montage_und-Serviceanleitung-Symphon-E.pdf";

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

    let label: string;
    switch (environment.theme) {
      case 'Heckert':
        label = "Symphon-E";
        break;
      case 'FENECON':
      default:
        label = "FENECON Home";
        break;
    }
    fields.push({
      key: "type",
      type: "radio",
      templateOptions: {
        label: "Speichertyp",
        type: "radio",
        options: [
          { value: "fenecon-home", label: label }
        ],
        required: true
      }
    });

    return fields;

  }

  public openManual() {
    if (environment.theme == 'Heckert') {
      window.open(this.LINK_SYMPHON_E_MANUAL);
    } else {
      window.open(this.LINK_HOME_MANUAL);
    }
  }
}
