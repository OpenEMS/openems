import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Ibn } from '../../installation-systems/abstract-ibn';
import { HomeFeneconIbn } from '../../installation-systems/home-fenecon';
import { HomeHeckertIbn } from '../../installation-systems/home-heckert';

export type DcPv = {
  isSelected: boolean,
  alias: string,
  value: number,
  orientation: string,
  moduleType: string,
  modulesPerString: number
}

@Component({
  selector: "protocol-pv",
  templateUrl: './protocol-pv.component.html'
})
export class ProtocolPv implements OnInit {

  @Input() public ibn: HomeFeneconIbn | HomeHeckertIbn;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<Ibn>();
  @Output() public setIbnEvent = new EventEmitter<Ibn>();

  public forms: Array<{ formGroup: FormGroup, fields: FormlyFieldConfig[], model: any }> = new Array();

  public ngOnInit() {

    // Initialize PV-Object
    this.ibn.pv ??= {};
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

    this.forms[0].model = this.ibn.batteryInverter ?? {
      shadowManagementDisabled: false
    }
    this.forms[1].model = this.ibn.pv.dc1 ?? {
      isSelected: false
    };
    this.forms[2].model = this.ibn.pv.dc2 ?? {
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

    this.ibn.batteryInverter = this.forms[0].model;
    this.ibn.pv.dc1 = this.forms[1].model;
    this.ibn.pv.dc2 = this.forms[2].model;

    this.setIbnEvent.emit(this.ibn);
    this.nextViewEvent.emit();
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