import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Service, Utils } from 'src/app/shared/shared';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { Meter } from '../../shared/meter';
import { DIRECTIONS_OPTIONS } from '../../shared/options';

@Component({
  selector: "protocol-additional-ac-producers",
  templateUrl: './protocol-additional-ac-producers.component.html'
})
export class ProtocolAdditionalAcProducersComponent implements OnInit {

  @Input() public ibn: AbstractIbn;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  public form: FormGroup;
  public fields: FormlyFieldConfig[];
  protected model;
  public insertModeEnabled: boolean;
  protected manualLink: string | null = null;

  constructor(
    private service: Service,
    private translate: TranslateService
  ) { }

  public ngOnInit() {

    // Initialize PV-Object
    this.ibn.pv ??= {};
    this.ibn.pv.ac ??= [];
    this.form = new FormGroup({});
    this.fields = this.getFields();
    this.model = {};
    this.insertModeEnabled = false;
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    if (this.insertModeEnabled) {
      this.service.toast(this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.SAVE_TO_CONTINUE'), "warning");
      return;
    }

    this.nextViewEvent.emit(this.ibn);
  }

  public getFields(): FormlyFieldConfig[] {

    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "alias",
      type: "input",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ALIAS'),
        description: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ALIAS_DESCRIPTION_ADDITIONAL_AC'),
        required: true
      }
    });

    fields.push({
      key: "value",
      type: "input",
      defaultValue: 1000, // Acts as minimum value through "defaultAsMinimumValue" validator
      templateOptions: {
        type: "number",
        label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.INSTALLED_POWER'),
        required: true
      },
      parsers: [Number],
      validators: {
        validation: ["onlyPositiveInteger", "defaultAsMinimumValue"]
      }
    });

    fields.push({
      key: "orientation",
      type: "select",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ORIENTATION'),
        options: DIRECTIONS_OPTIONS(this.translate)
      }
    });

    fields.push({
      key: "moduleType",
      type: "input",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODULE_TYPE'),
        description: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODULE_TYPE_DESCRIPTION')
      }
    });

    fields.push({
      key: "modulesPerString",
      type: "input",
      templateOptions: {
        type: "number",
        label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.NUMBER_OF_MODULES')
      },
      parsers: [Number],
      validators: {
        validation: ["onlyPositiveInteger"]
      },
      defaultValue: 0
    });

    fields.push({
      key: "meterType",
      type: "select",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.METER_TYPE'),
        required: true,
        options: [
          { label: "SOCOMEC", value: Meter.SOCOMEC },
          { label: "KDK", value: Meter.KDK }
        ]
      },
      defaultValue: Meter.SOCOMEC
    });

    fields.push({
      key: "modbusCommunicationAddress",
      type: "input",
      templateOptions: {
        type: "number",
        label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODBUS'),
        required: true,
        min: 6
      },
      parsers: [Number],
      validators: {
        validation: ["onlyPositiveInteger"]
      },
      defaultValue: 6,
      expressions: {
        // Change the modbus description based on the meter selected above.
        'templateOptions.description': (form) => {
          var meterType = form.model.meterType;

          // Load the appropriate link based on the meter selected.
          this.manualLink = Meter.toManualLink(meterType);

          // Load the appropriate description based on the meter selected.
          if (meterType === Meter.SOCOMEC) {
            return this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODBUS_SOCOMEC_DESCRIPTION');
          } else {
            return this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODBUS_KDK_DESCRIPTION');
          }
        }
      }
    });

    return fields;
  }

  public switchMode() {

    if (this.insertModeEnabled) {

      // Test if form is valid
      if (this.form.invalid) {
        this.service.toast(this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.VALID_DATA'), "danger");
        return;
      }

      // Push data into array and reset the form
      this.ibn.pv.ac.push(Utils.deepCopy(this.model));
      this.form.reset();

    }

    // Switch
    this.insertModeEnabled = !this.insertModeEnabled;
  }

  public editElement(element) {
    this.model = element;

    if (!this.insertModeEnabled) {
      this.switchMode();
    }

    this.removeElement(element);
  }

  public removeElement(element) {
    let ac = this.ibn.pv.ac;
    ac.splice(ac.indexOf(element), 1);
  }

  public openManual() {
    if (!this.manualLink) {
      this.service.toast(this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.METER_SELECTION_WARNING'), 'warning');
    } else {
      window.open(this.manualLink);
    }
  }
}