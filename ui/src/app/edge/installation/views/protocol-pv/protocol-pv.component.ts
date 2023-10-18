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
  templateUrl: './protocol-pv.component.html'
})
export class ProtocolPvComponent implements OnInit {

  @Input() public ibn: AbstractHomeIbn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  public constructor(private translate: TranslateService) { }

  public forms: Array<dcForm> = new Array();

  public ngOnInit() {

    // Initialize PV-Object
    this.ibn.pv ??= {};
    this.ibn.pv.dc ??= [];

    for (let formNr = 0; formNr <= this.ibn.maxNumberOfPvStrings; formNr++) {
      const form: dcForm = {
        formGroup: new FormGroup({}),
        fields: [],
        model: {}
      };

      this.forms.push(form);

      // form 0 is always for shadow management and later forms are for MPPT's.
      if (formNr === 0) {
        this.forms[formNr].model = this.ibn.batteryInverter ?? {
          shadowManagementDisabled: false
        };
      } else {
        // forms[1], forms[2]... = dc[0], dc[1]...
        this.forms[formNr].model = this.ibn.pv.dc[formNr - 1] ?? {
          isSelected: false
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
    for (let form of this.forms) {
      if (form.formGroup.invalid) {
        return;
      }
    }

    // Resetting to avoiding duplicate entries in the array.
    this.ibn.pv.dc = [];

    for (let formNr = 0; formNr <= this.ibn.maxNumberOfPvStrings; formNr++) {
      if (formNr === 0) {
        this.ibn.batteryInverter = this.forms[formNr].model;
      } else {
        if (Object.keys(this.forms[formNr].formGroup.controls).length) {
          this.ibn.pv.dc.push(Utils.deepCopy(this.forms[formNr].model));
        } else {
          this.ibn.pv.dc.push({
            isSelected: false
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
        label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.SHADE_MANAGEMENT_DEACTIVATE'),
        description: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.SHADE_MANAGEMENT_DESCRIPTION')
      }
    });

    //  For DC-PVs
    for (let strings = 1; strings <= this.ibn.maxNumberOfPvStrings; strings++) {

      // If the maxNumberOfPvStrings is '2', then it is Home 10.
      // In Home 20 & 30, It is always possible to configure two pv strings under one mppt.
      // So dividing the strings with 2 for actual mppt number.
      const mppt = this.ibn.maxNumberOfPvStrings === 2 ? strings : Math.ceil(strings / 2);
      const key: string = 'mppt' + mppt + 'pv' + strings;
      if (this.ibn.mppt[key] === true) {
        this.forms[strings]?.fields.push(
          {
            key: "isSelected",
            type: "checkbox",
            hooks: {
              onInit: (field: FormlyFieldConfig) => {

                if (this.ibn.maxNumberOfPvStrings <= 2 || strings % 2) {
                  return;
                }

                // subscription and unsubscribe is handled by formly
                return field.form.get('isSelected').valueChanges.pipe(
                  tap(isSelected => {
                    if (isSelected) {
                      const field = this.forms[strings - 1].formGroup.value;

                      const value: number = field.value;
                      const moduleType: string = field.moduleType;
                      const modulesPerString: number = field.modulesPerString;

                      this.forms[strings].fields.find(field => field.key == 'value').formControl.setValue(value);
                      this.forms[strings].fields.find(field => field.key == 'moduleType').formControl.setValue(moduleType);
                      this.forms[strings].fields.find(field => field.key == 'modulesPerString').formControl.setValue(modulesPerString);
                    }
                  })
                );
              }
            },
            props: {
              label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MARKED_AS', { mppt: mppt, pv: strings })
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
            defaultValue: 1000, // Acts as minimum value through "defaultAsMinimumValue" validator
            templateOptions: {
              type: "number",
              label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.INSTALLED_POWER'),
              required: true
            },
            validators: {
              validation: ["onlyPositiveInteger", "defaultAsMinimumValue"]
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
              description: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODULE_TYPE_DESCRIPTION')
            },
            hideExpression: model => !model.isSelected
          },
          {
            key: "modulesPerString",
            type: "input",
            templateOptions: {
              type: "number",
              label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.NUMBER_OF_MODULES')
            },
            parsers: [Number],
            hideExpression: model => !model.isSelected
          });
      }
    }
  }
}