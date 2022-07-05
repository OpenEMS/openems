import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Edge, Service, Websocket } from 'src/app/shared/shared';
import { ComponentData, SerialNumberFormData } from 'src/app/shared/type/componentData';
import { AbstractIbn } from '../../installation-systems/abstract-ibn';

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

  public forms: Array<SerialNumberFormData>;

  public formSettings: FormGroup;
  public fieldsSettings: FormlyFieldConfig[];
  public modelSettings;

  public numberOfTowers: number;
  public numberOfModulesPerTower: number;

  public spinnerId: string;
  public isWaiting = false;

  constructor(private service: Service, private websocket: Websocket) { }

  public ngOnInit() {
    // Start spinner
    this.spinnerId = 'installation-serial-number-spinner';
    this.setIsWaiting(true);

    // Read settings
    this.ibn.getSettings(this.edge, this.websocket).then((settings) => {

      // 'Tower' is same as 'String' in Commercial, though the technical meaning is different.
      this.numberOfTowers = settings.numberOfTowers;
      this.numberOfModulesPerTower = settings.numberOfModulesPerTower;

      // Read all module serial numbers
      this.initializeAllFields().then(() => {
        this.setIsWaiting(false);
      });
    });
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    if (this.formSettings.invalid) {
      return;
    }

    for (const form of this.forms) {
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

    // Submit the setup protocol
    this.isWaiting = true;

    this.submitSetupProtocol().then((protocolId) => {
      this.service.toast('Das Protokoll wurde erfolgreich versendet.', 'success');
      this.ibn.setupProtocolId = protocolId;
    }).catch((reason) => {
      this.service.toast('Fehler beim Versenden des Protokolls.', 'danger');
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
        this.fieldsSettings = this.ibn.getSettingsFields(this.numberOfModulesPerTower, this.numberOfTowers);
        this.modelSettings = {};

        // Battery modules fields.
        this.forms = new Array(this.numberOfTowers);
        this.forms = this.ibn.fillForms(this.numberOfTowers, this.numberOfModulesPerTower, models, this.forms);

        resolve();
      });
    });
  }

  /**
   * Restricts the reading of serial numbers to specific towers configured.
   */
  private readSerialNumbersFromRegisters(): Promise<Object>[] {

    let result: Promise<Object>[] = []
    for (let i = 0; i < this.numberOfTowers; i++) {
      result.push(this.ibn.getSerialNumbers(i, this.edge, this.websocket, this.numberOfModulesPerTower));
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
      this.service.toast('Um die Einstellungen zu übernehmen, geben Sie gültige Werte ein.', 'warning');
      return;
    }

    // Model settings consists of Number of towers information in home and number of strings in Commercial.
    this.numberOfTowers = <number>Object.values(this.modelSettings)[0];
    this.numberOfModulesPerTower = <number>Object.values(this.modelSettings)[1];

    this.setIsWaiting(true);
    this.initializeAllFields().then(() => {
      this.setIsWaiting(false);
    });
  }

  public extractSerialNumbers(fields: FormlyFieldConfig[]): ComponentData[] {
    const serialNumbers: ComponentData[] = [];

    for (const field of fields) {
      serialNumbers.push({
        label: field.templateOptions.label + ' Seriennummer',
        value: (field.templateOptions.prefix ?? '') + field.formControl.value
      });
    }
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
