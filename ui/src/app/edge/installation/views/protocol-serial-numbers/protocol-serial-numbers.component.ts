import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Edge, Service, Websocket } from 'src/app/shared/shared';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';
import { ComponentData, SerialNumberFormData } from '../../shared/ibndatatypes';

@Component({
  selector: ProtocolSerialNumbersComponent.SELECTOR,
  templateUrl: './protocol-serial-numbers.component.html'
})
export class ProtocolSerialNumbersComponent implements OnInit {
  private static readonly SELECTOR = 'protocol-serial-numbers';

  @Input() public ibn: AbstractIbn;
  @Input() public edge: Edge;
  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<AbstractIbn>();

  public forms: SerialNumberFormData[];

  public formSettings: FormGroup;
  public fieldsSettings: FormlyFieldConfig[];
  public modelSettings;

  public numberOfTowers: number;
  public numberOfModulesPerTower: number;

  public spinnerId: string;
  public isWaiting = false;
  private duplicateSerialNumbers: string[] = [];

  constructor(
    private service: Service,
    private websocket: Websocket,
    private translate: TranslateService
  ) { }

  public ngOnInit() {

    // Start spinner
    this.spinnerId = 'installation-serial-number-spinner';
    this.setIsWaiting(true);

    // Read settings
    this.ibn.getPreSettingInformationFromEdge(this.edge, this.websocket).then((settings) => {

      // 'Tower' is same as 'String' in Commercial, though the technical meaning is different.
      this.numberOfTowers = settings.numberOfTowers;
      this.numberOfModulesPerTower = settings.numberOfModulesPerTower;

      // Read all module serial numbers
      this.initializeAllFields().then(() => {
        this.setIsWaiting(false);
      }).then(() => {
        if (this.numberOfModulesPerTower < this.ibn.defaultNumberOfModules) {
          this.service.toast(this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.READ_DESCRIPTION'), 'danger');
        }
      });
    });
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    if (this.formSettings.invalid) {
      this.service.toast(this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.READ_DESCRIPTION'), 'danger');
      return;
    }

    for (const form of this.forms) {
      for (const fieldSetting of form.fieldSettings) {
        if (fieldSetting.form.invalid) {
          return;
        }
      }
      if (form.formTower.invalid) {
        return;
      }
    }

    this.ibn.serialNumbers = {
      modules: []
    };

    for (const form of this.forms) {
      let serialNumbers: ComponentData[] = [];
      serialNumbers = this.extractSerialNumbers(form.fieldSettings);
      for (const sn of serialNumbers) {
        this.ibn.serialNumbers.modules.push(sn);
      }
    }

    // Duplicates check.
    if (this.duplicateSerialNumbers.length !== 0) {
      this.service.toast(this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.SAME_SERIAL_NUMBERS', { serialNumbers: this.duplicateSerialNumbers }), 'warning');
      return;
    }

    // Submit the setup protocol
    this.isWaiting = true;

    this.submitSetupProtocol().then((protocolId) => {
      this.service.toast(this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.SENT_SUCCESSFULLY'), 'success');
      this.ibn.setupProtocolId = protocolId;
    }).catch((reason) => {
      this.service.toast(this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.ERROR_SENDING'), 'danger');
      console.warn(reason);
    }).finally(() => {
      this.isWaiting = false;
      this.nextViewEvent.emit(this.ibn);
    });
  }

  public setIsWaiting(isWaiting: boolean) {
    this.isWaiting = isWaiting;

    if (isWaiting) {
      this.service.startSpinner(this.spinnerId);
    } else {
      this.service.stopSpinner(this.spinnerId);
    }
  }

  /**
   * Initializes all fields such as number of tower and modules per tower and 
   * also indiviaul components fo the system and modules. 
   * All the information that can be read directly are read directly from the registers.
   * 
   * @returns The Settings fields for towers and also individual modules fields.
   */
  public initializeAllFields(): Promise<void> {
    return new Promise((resolve) => {
      Promise.all(this.readSerialNumbersFromRegisters()).then((models) => {

        // Settings fields
        this.formSettings = new FormGroup({});
        this.fieldsSettings = this.ibn.getPreSettingsFields(this.numberOfModulesPerTower, this.numberOfTowers);
        this.modelSettings = {};

        // Battery modules fields.
        this.forms = new Array(this.numberOfTowers);
        this.forms = this.ibn.fillSerialNumberForms(this.numberOfTowers, this.numberOfModulesPerTower, models, this.forms);

        resolve();
      });
    });
  }

  /**
   * Restricts the reading of serial numbers to specific towers configured.
   */
  private readSerialNumbersFromRegisters(): Promise<Object>[] {

    let result: Promise<Object>[] = [];
    for (let i = 0; i < this.numberOfTowers; i++) {
      result.push(this.ibn.getSerialNumbersFromEdge(i, this.edge, this.websocket, this.numberOfModulesPerTower));
    }

    return result;
  }

  /**
   * Updates the tower and module information based on manual update.
   * 
   * @returns the updated number of towers and modules per tower.
   */
  public saveSettings() {
    if (this.formSettings.invalid) {
      this.service.toast(this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.ENTER_VALID_ADDRESS'), 'warning');
      return;
    }

    // Model settings consists of Number of towers information in home and number of strings in Commercial, 
    // but towers are used as keys for both individual implementations.
    this.numberOfTowers = this.modelSettings.numberOfTowers;
    this.numberOfModulesPerTower = this.modelSettings.numberOfModulesPerTower;

    this.setIsWaiting(true);
    this.initializeAllFields().then(() => {
      this.setIsWaiting(false);
    });
  }

  public extractSerialNumbers(fields: FormlyFieldConfig[]): ComponentData[] {
    const serialNumbers: ComponentData[] = [];
    this.duplicateSerialNumbers = [];

    for (const field of fields) {
      const label = field.props.label;
      const value = (field.props.prefix ?? '') + field.formControl.value;

      serialNumbers.push({
        label: label,
        value: value
      });
    }

    // check if there are duplicates and add it to duplicates array to display later in alert.
    // Collect the duplicate values..
    const duplicateValues = serialNumbers
      .map(v => v.value)
      .filter((v, i, valueArray) => valueArray.indexOf(v) !== i);

    // Based on the Id's collect the Labels to display in the alert.
    serialNumbers.filter(obj => duplicateValues.includes(obj.value))
      .forEach((value) => this.duplicateSerialNumbers.push(value.label));;

    return serialNumbers;
  }

  /**
   * Submits the setup protocol to the backend.
   *
   * @returns a promise promising a string that contains the protocol id
   */
  public submitSetupProtocol(): Promise<string> {
    return this.ibn.getProtocol(this.edge, this.websocket);
  }
}
