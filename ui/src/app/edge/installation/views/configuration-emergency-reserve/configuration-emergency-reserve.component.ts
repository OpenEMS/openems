// @ts-strict-ignore
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';

@Component({
  selector: ConfigurationEmergencyReserveComponent.SELECTOR,
  templateUrl: './configuration-emergency-reserve.component.html',
})
export class ConfigurationEmergencyReserveComponent implements OnInit {

  private static readonly SELECTOR = 'configuration-emergency-reserve';

  @Input() public ibn: AbstractIbn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<any>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  public model;

  constructor(private translate: TranslateService) { }

  public ngOnInit() {

    this.form = new FormGroup({});
    this.fields = this.getFields();
    this.model = this.ibn.emergencyReserve ?? {
      isEnabled: true,
      value: 20,
      isReserveSocEnabled: false,
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
    this.nextViewEvent.emit(this.ibn);
  }

  public getFields(): FormlyFieldConfig[] {

    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: 'isEnabled',
      type: 'checkbox',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.IS_ENABLED'),
      },
    });

    fields.push({
      key: 'isReserveSocEnabled',
      type: 'toggle',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.IS_ACTIVATED'),
      },
      hideExpression: model => !model.isEnabled,
    });

    fields.push({
      key: 'value',
      type: 'range',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.VALUE'),
        description: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.RECENT') + ' 20',
        required: true,
        min: this.ibn.emergencyReserve.minValue,
        max: 100,
        attributes: {
          pin: 'true',
        },
        change: (field) => { field.props.description = this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.RECENT') + ' ' + field.formControl.value; },
      },
      parsers: [Number],
      hideExpression: model => !model.isEnabled || !model.isReserveSocEnabled,
    });

    // Adds the fields only for Commercial-30 Netztrennstelle.
    fields = this.ibn.getAdditionalEmergencyReserveFields(fields);

    return fields;
  }

  public setEmergencyReserve(model: any) {
    this.ibn.emergencyReserve = model;
  }
}
