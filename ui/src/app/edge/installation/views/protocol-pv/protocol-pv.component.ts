import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyField, FormlyFieldConfig } from '@ngx-formly/core';
import { InstallationData } from '../../installation.component';

export type DcPv = {
  isSelected: boolean,
  alias: string,
  value: number,
  orientation: string,
  moduleType: string,
  modulesPerString: number
}

@Component({
  selector: ProtocolPv.SELECTOR,
  templateUrl: './protocol-pv.component.html'
})
export class ProtocolPv implements OnInit {

  private static readonly SELECTOR = "protocol-pv";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<InstallationData>();

  public forms: Array<{ formGroup: FormGroup, fields: FormlyFieldConfig[], model: any }> = new Array();

  public ngOnInit() {

    // Initialize PV-Object
    this.installationData.pv ??= {};
    this.forms.push({
      formGroup: new FormGroup({}),
      fields: [],
      model: {}
    },
      {
        formGroup: new FormGroup({}),
        fields: [],
        model: {}
      },
      {
        formGroup: new FormGroup({}),
        fields: [],
        model: {}
      })

    this.forms[0].model = this.installationData.batteryInverter ?? {
      shadowManagementDisabled: false
    }
    this.forms[1].model = this.installationData.pv.dc1 ?? {
      isSelected: false
    };
    this.forms[2].model = this.installationData.pv.dc2 ?? {
      isSelected: false
    };

    this.getFields();
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {

    // Iterate over forms and prohibit onNextClicked if forms are not valid
    for (let form of this.forms) {
      if (form.formGroup.invalid) {
        return
      }
    }

    this.installationData.batteryInverter = this.forms[0].model;
    this.installationData.pv.dc1 = this.forms[1].model;
    this.installationData.pv.dc2 = this.forms[2].model;

    this.nextViewEvent.emit(this.installationData);
  }

  public getFields(): void {

    this.forms[0].fields.push({
      key: "shadowManagementDisabled",
      type: "checkbox",
      templateOptions: {
        label: "Schattenmanagement deaktivieren",
        description: "Nur wenn Optimierer verbaut sind, muss das Schattenmanagement deaktiviert werden",
      },
    })

    //  For 2 DC-PVs
    for (let forMpptNr = 1; forMpptNr <= 2; forMpptNr++) {

      this.forms[forMpptNr]?.fields.push({
        key: "isSelected",
        type: "checkbox",
        templateOptions: {
          label: "MPPT " + forMpptNr + " (beschriftet mit ''PV" + forMpptNr + "'')",
        }
      },
        {
          key: "alias",
          type: "input",
          templateOptions: {
            label: "Bezeichnung",
            description: "wird im Online-Monitoring angezeigt, z. B. ''PV Hausdach''",
            required: true
          },
          hideExpression: model => !model.isSelected
        },
        {
          key: "value",
          type: "input",
          templateOptions: {
            type: "number",
            label: "Installierte Leistung [Wₚ]",
            min: 1000,
            required: true
          },
          parsers: [Number],
          hideExpression: model => !model.isSelected
        },
        {
          key: "orientation",
          type: "select",
          templateOptions: {
            label: "Ausrichtung",
            options: [
              { label: "Süd", value: "Sued" },
              { label: "Südwest", value: "Suedwest" },
              { label: "West", value: "West" },
              { label: "Südost", value: "Suedost" },
              { label: "Ost", value: "Ost" },
              { label: "Nordwest", value: "Nordwest" },
              { label: "Nordost", value: "Nordost" },
              { label: "Nord", value: "Nord" },
              { label: "", value: undefined }
            ]
          },
          hideExpression: model => !model.isSelected
        },
        {
          key: "moduleType",
          type: "input",
          templateOptions: {
            label: "Modultyp",
            description: "z. B. Hersteller und Leistung"
          },
          hideExpression: model => !model.isSelected
        },
        {
          key: "modulesPerString",
          type: "input",
          templateOptions: {
            type: "number",
            label: "Anzahl PV-Module"
          },
          parsers: [Number],
          hideExpression: model => !model.isSelected
        })
    }
  }

}