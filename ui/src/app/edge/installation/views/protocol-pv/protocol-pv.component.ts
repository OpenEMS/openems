// @ts-strict-ignore
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { tap } from 'rxjs/operators';
import { Utils } from 'src/app/shared/shared';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { AbstractHomeIbn } from '../../installation-systems/home/abstract-home';
import { dcForm } from '../../shared/ibndatatypes';
import { DIRECTIONS_OPTIONS } from '../../shared/options';

@Component({
  selector: "protocol-pv",
  templateUrl: './protocol-pv.component.html',
})
export class ProtocolPvComponent implements OnInit {

  @Input() public ibn: AbstractHomeIbn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  public constructor(private translate: TranslateService) { }

  public forms: Array<dcForm> = [];

  public ngOnInit() {

    // Initialize PV-Object
    this.ibn.pv ??= {};
    this.ibn.pv.dc ??= [];

    for (let formNr = 0; formNr <= (this.ibn.maxNumberOfMppt !== -1 ? this.ibn.maxNumberOfMppt : this.ibn.maxNumberOfPvStrings); formNr++) {
      const form: dcForm = {
        formGroup: new FormGroup({}),
        fields: [],
        model: {},
      };

      this.forms.push(form);

      // form 0 is always for shadow management and later forms are for MPPT's.
      if (formNr === 0) {
        form.model = this.ibn.batteryInverter ?? {
          shadowManagementDisabled: false,
        };
      } else {
        // forms[1], forms[2]... = dc[0], dc[1]...
        form.model = this.ibn.pv.dc[formNr - 1] ?? {
          isSelected: this.ibn.maxNumberOfMppt !== -1,
        };
      }
    }
    this.getFields();
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {

    // Iterate over forms and prohibit onNextClicked if forms are not valid
    for (const form of this.forms) {
      if (form.formGroup.invalid) {
        return;
      }
    }

    // Resetting to avoiding duplicate entries in the array.
    this.ibn.pv.dc = [];

    for (let formNr = 0; formNr <= (this.ibn.maxNumberOfMppt !== -1 ? this.ibn.maxNumberOfMppt : this.ibn.maxNumberOfPvStrings); formNr++) {
      if (formNr === 0) {
        this.ibn.batteryInverter = this.forms[formNr].model;
      } else {
        if (this.ibn.mppt['mppt' + formNr]) {
          this.ibn.pv.dc.push(Utils.deepCopy(this.forms[formNr].model));
        } else {
          this.ibn.pv.dc.push({
            isSelected: false,
          });
        }
      }
    }
    this.nextViewEvent.emit(this.ibn);
  }

  public getFields(): void {

    this.forms[0].fields.push({
      key: "shadowManagementDisabled",
      type: "checkbox",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_PV.SHADE_MANAGEMENT_DEACTIVATE'),
        description: this.translate.instant('INSTALLATION.PROTOCOL_PV.SHADE_MANAGEMENT_DESCRIPTION'),
      },
    });

    //  For DC-PVs
    for (let i = 0; i < (this.ibn.maxNumberOfMppt !== -1 ? this.ibn.maxNumberOfMppt : this.ibn.maxNumberOfPvStrings); i++) {

      // If the maxNumberOfPvStrings is '2', then it is Home 10.
      // In Home 20 & 30, It is always possible to configure two pv strings under one mppt.
      // So dividing the strings with 2 for actual mppt number.
      const mppt = i + 1;
      const key: string = 'mppt' + mppt;
      if (this.ibn.mppt[key] !== true) {
        continue;
      }

      this.forms[i + 1]?.fields.push(
        {
          key: "isSelected",
          type: "checkbox",
          hooks: {
            onInit: (field: FormlyFieldConfig) => {

              if (this.ibn.maxNumberOfPvStrings <= 2 || i % 2) {
                return;
              }

              // subscription and unsubscribe is handled by formly
              return field.form.get('isSelected').valueChanges.pipe(
                tap(isSelected => {
                  if (isSelected) {
                    const field = this.forms[i - 1].formGroup.value;

                    const value: number = field.value;
                    const moduleType: string = field.moduleType;
                    const modulesPerString: number = field.modulesPerString;

                    this.forms[i].fields.find(field => field.key == 'value').formControl.setValue(value);
                    this.forms[i].fields.find(field => field.key == 'moduleType').formControl.setValue(moduleType);
                    this.forms[i].fields.find(field => field.key == 'modulesPerString').formControl.setValue(modulesPerString);
                  }
                }),
              );
            },
          },
          props: {
            label: this.ibn.maxNumberOfMppt === -1
              ? this.translate.instant('INSTALLATION.PROTOCOL_PV.MARKED_AS', { mppt: mppt, pv: i + 1 })
              : this.translate.instant('INSTALLATION.PROTOCOL_PV.MARKED_AS_BOTH_STRINGS', { mppt: mppt, pv1: i * 2 + 1, pv2: i * 2 + 2 }),
          },
        },
        {
          key: "alias",
          type: "input",
          templateOptions: {
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV.ALIAS'),
            description: this.translate.instant('INSTALLATION.PROTOCOL_PV.ALIAS_DESCRIPTION_PV'),
            required: true,
          },
          hideExpression: model => !model.isSelected,
        },
        {
          key: "value",
          type: "input",
          defaultValue: 1000, // Acts as minimum value through "defaultAsMinimumValue" validator
          templateOptions: {
            type: "number",
            label: this.ibn.maxNumberOfMppt === -1
              ? this.translate.instant('INSTALLATION.PROTOCOL_PV.INSTALLED_POWER')
              : this.translate.instant('INSTALLATION.PROTOCOL_PV.INSTALLED_POWER_PER_STRING'),
            required: true,
          },
          validators: {
            validation: ["onlyPositiveInteger", "defaultAsMinimumValue"],
          },
          hideExpression: model => !model.isSelected,
        },
        {
          key: "orientation",
          type: "select",
          templateOptions: {
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV.ORIENTATION'),
            options: DIRECTIONS_OPTIONS(this.translate),
          },
          hideExpression: model => !model.isSelected,
        },
        {
          key: "moduleType",
          type: "input",
          templateOptions: {
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV.MODULE_TYPE'),
            description: this.translate.instant('INSTALLATION.PROTOCOL_PV.MODULE_TYPE_DESCRIPTION'),
          },
          hideExpression: model => !model.isSelected,
        },
        {
          key: "modulesPerString",
          type: "input",
          templateOptions: {
            type: "number",
            label: this.ibn.maxNumberOfMppt === -1
              ? this.translate.instant('INSTALLATION.PROTOCOL_PV.NUMBER_OF_MODULES')
              : this.translate.instant('INSTALLATION.PROTOCOL_PV.NUMBER_OF_MODULES_PER_STRING'),
          },
          parsers: [Number],
          hideExpression: model => !model.isSelected,
        });

      // Visible only for Home 20 and 30.
      if (this.ibn.maxNumberOfMppt !== -1) {
        this.forms[i + 1]?.fields.push({
          key: "portsConnected",
          type: "checkbox",
          props: {
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV.BOTH_SELECTED_LABEL', { pv1: i * 2 + 1, pv2: i * 2 + 2 }),
          },
          hideExpression: model => !model.isSelected,
        });
      }
    }
  }
}
