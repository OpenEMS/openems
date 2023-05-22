import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { HomeFeneconIbn } from '../../installation-systems/home/home-fenecon';
import { HomeHeckertIbn } from '../../installation-systems/home/home-heckert';
import { DIRECTIONS_OPTIONS } from '../../shared/options';

@Component({
  selector: "protocol-pv",
  templateUrl: './protocol-pv.component.html'
})
export class ProtocolPvComponent implements OnInit {

  @Input() public ibn: HomeFeneconIbn | HomeHeckertIbn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  public constructor(private translate: TranslateService) { }

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
      });

    this.forms[0].model = this.ibn.batteryInverter ?? {
      shadowManagementDisabled: false
    };
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
        return;
      }
    }

    this.ibn.batteryInverter = this.forms[0].model;
    this.ibn.pv.dc1 = this.forms[1].model;
    this.ibn.pv.dc2 = this.forms[2].model;

    this.nextViewEvent.emit(this.ibn);
  }

  public getFields(): void {

    this.forms[0].fields.push({
      key: "shadowManagementDisabled",
      type: "checkbox",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.SHADE_MANAGEMENT_DEACTIVATE'),
        description: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.SHADE_MANAGEMENT_DESCRIPTION'),
      },
    });

    //  For 2 DC-PVs
    for (let forMpptNr = 1; forMpptNr <= 2; forMpptNr++) {

      this.forms[forMpptNr]?.fields.push({
        key: "isSelected",
        type: "checkbox",
        templateOptions: {
          label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MARKED_AS', { number: forMpptNr }),
        }
      },
        {
          key: "alias",
          type: "input",
          templateOptions: {
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ALIAS'),
            description: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ALIAS_DESCRIPTION_PV'),
            required: true
          },
          hideExpression: model => !model.isSelected
        },
        {
          key: "value",
          type: "input",
          templateOptions: {
            type: "number",
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.INSTALLED_POWER'),
            min: 1000,
            required: true
          },
          parsers: [Number],
          validators: {
            validation: ["onlyPositiveInteger"]
          },
          hideExpression: model => !model.isSelected
        },
        {
          key: "orientation",
          type: "select",
          templateOptions: {
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ORIENTATION'),
            options: DIRECTIONS_OPTIONS(this.translate)
          },
          hideExpression: model => !model.isSelected
        },
        {
          key: "moduleType",
          type: "input",
          templateOptions: {
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODULE_TYPE'),
            description: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODULE_TYPE_DESCRIPTION'),
          },
          hideExpression: model => !model.isSelected
        },
        {
          key: "modulesPerString",
          type: "input",
          templateOptions: {
            type: "number",
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.NUMBER_OF_MODULES'),
          },
          parsers: [Number],
          hideExpression: model => !model.isSelected
        });
    }
  }
}