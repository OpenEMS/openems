import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { FeedInSetting } from '../protocol-feed-in-limitation/protocol-feed-in-limitation.component';
import { ChannelAddress, Edge, Service, Websocket } from 'src/app/shared/shared';
import { Ibn } from '../../installation-systems/abstract-ibn';
import { HomeFeneconIbn } from '../../installation-systems/home-fenecon';
import { HomeHeckertIbn } from '../../installation-systems/home-heckert';

@Component({
  selector: ProtocolSerialNumbersComponent.SELECTOR,
  templateUrl: './protocol-serial-numbers.component.html'
})
export class ProtocolSerialNumbersComponent implements OnInit {
  private static readonly SELECTOR = "protocol-serial-numbers";

  private readonly READ_TIMEOUT = 5000;

  @Input() public ibn: HomeFeneconIbn | HomeHeckertIbn;
  @Input() public edge: Edge;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent = new EventEmitter<Ibn>();
  @Output() public setIbnEvent = new EventEmitter<Ibn>();

  public formSettings: FormGroup;
  public formTower0: FormGroup;
  public formTower1: FormGroup;
  public formTower2: FormGroup;

  public fieldsSettings: FormlyFieldConfig[];
  public fieldsTower0: FormlyFieldConfig[];
  public fieldsTower1: FormlyFieldConfig[];
  public fieldsTower2: FormlyFieldConfig[];

  public modelSettings;
  public modelTower0;
  public modelTower1;
  public modelTower2;

  public numberOfTowers: number;
  public numberOfModulesPerTower: number;

  public spinnerId: string;
  public isWaiting: boolean = false;

  constructor(private service: Service, private websocket: Websocket) { }

  public ngOnInit() {
    // Start spinner
    this.spinnerId = "installation-serial-number-spinner";
    this.setIsWaiting(true);

    // Read settings
    this.getSettings().then((settings) => {
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
    if (
      this.formSettings.invalid ||
      this.formTower0.invalid ||
      this.formTower1.invalid ||
      this.formTower2.invalid
    ) {
      return;
    }

    // Initialize serial numbers object
    this.ibn.battery = {
      serialNumbers: {
        tower0: [],
        tower1: [],
        tower2: []
      }
    }

    // Fill data from field into the ibn object
    let serialNumbers = this.ibn.battery.serialNumbers;

    serialNumbers.tower0 = this.extractSerialNumbers(this.fieldsTower0);

    if (this.numberOfTowers >= 2) {
      serialNumbers.tower1 = this.extractSerialNumbers(this.fieldsTower1);
    }

    if (this.numberOfTowers === 3) {
      serialNumbers.tower2 = this.extractSerialNumbers(this.fieldsTower2);
    }

    // Submit the setup protocol
    this.isWaiting = true;

    this.submitSetupProtocol().then((protocolId) => {
      this.service.toast("Das Protokoll wurde erfolgreich versendet.", "success");
      this.ibn.setupProtocolId = protocolId;
    }).catch((reason) => {
      this.service.toast("Fehler beim Versenden des Protokolls.", "danger");
    }).finally(() => {
      this.isWaiting = false;
      this.setIbnEvent.emit(this.ibn);
      this.nextViewEvent.emit();
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

  public getSettings(): Promise<{ numberOfTowers: number, numberOfModulesPerTower: number }> {
    return new Promise((resolve) => {
      let isResolved: boolean = false;

      // Edge-subscribe
      this.edge.subscribeChannels(this.websocket, ProtocolSerialNumbersComponent.SELECTOR, [
        new ChannelAddress("battery0", "NumberOfTowers"),
        new ChannelAddress("battery0", "NumberOfModulesPerTower")
      ]);

      // Subject to stop the subscription to currentData
      let stopOnRequest: Subject<void> = new Subject<void>();

      // Read tower and module numbers
      this.edge.currentData.pipe(
        takeUntil(stopOnRequest),
        filter(currentData => currentData != null)
      ).subscribe((currentData) => {
        let numberOfTowers = currentData.channel["battery0/NumberOfTowers"];
        let numberOfModulesPerTower = currentData.channel["battery0/NumberOfModulesPerTower"];

        // If values are available, resolve the promise with them
        if (numberOfTowers && numberOfModulesPerTower) {
          isResolved = true;
          resolve({ numberOfTowers: parseInt(numberOfTowers), numberOfModulesPerTower: parseInt(numberOfModulesPerTower) });
        }
      });

      setTimeout(() => {
        // If data isn't available after the timeout, the
        // promise gets resolved with default values
        if (!isResolved) {
          resolve({ numberOfTowers: 1, numberOfModulesPerTower: 5 });
        }

        // Unsubscribe to currentData and channels after timeout
        stopOnRequest.next();
        stopOnRequest.complete();
        this.edge.unsubscribeChannels(this.websocket, ProtocolSerialNumbersComponent.SELECTOR);
      }, this.READ_TIMEOUT);
    });
  }

  public getSerialNumbers(towerNr: number): Promise<Object> {
    return new Promise((resolve) => {
      let isResolved: boolean = false;
      let channelAddresses: { [key: string]: ChannelAddress } = {};
      let subscriptionId = ProtocolSerialNumbersComponent.SELECTOR + "-tower" + towerNr;
      let model: Object = {};

      // Gather channel addresses
      channelAddresses["batteryInverter"] = new ChannelAddress("batteryInverter0", "SerialNumber");
      channelAddresses["bmsBox"] = new ChannelAddress("battery0", "Tower" + towerNr + "BmsSerialNumber");

      for (let moduleNr = 0; moduleNr < this.numberOfModulesPerTower; moduleNr++) {
        channelAddresses["module" + moduleNr] = new ChannelAddress("battery0", "Tower" + towerNr + "Module" + moduleNr + "SerialNumber");
      }

      // Edge-subscribe
      this.edge.subscribeChannels(this.websocket, subscriptionId, Object.values(channelAddresses));

      // Subject to stop the subscription to currentData
      let stopOnRequest: Subject<void> = new Subject<void>();

      // Read data
      this.edge.currentData.pipe(
        takeUntil(stopOnRequest),
        filter(currentData => currentData != null)
      ).subscribe((currentData) => {
        for (let key in channelAddresses) {
          let channelAddress: ChannelAddress = channelAddresses[key];
          let serialNumber: string = currentData.channel[channelAddress.componentId + "/" + channelAddress.channelId];

          // If one serial number is undefined return
          if (!serialNumber) {
            return;
          }

          // Only take a part of the characters if the serial number has a fixed prefix
          if (key.startsWith("module")) {
            model[key] = serialNumber.substr(12, 12)
          } else {
            model[key] = serialNumber;
          }
        }

        // Resolve the promise
        isResolved = true;
        resolve(model);
      });

      setTimeout(() => {
        // If data isn't available after the timeout, the
        // promise gets resolved with an empty object
        if (!isResolved) {
          resolve({});
        }

        // Unsubscribe to currentData and channels after timeout
        stopOnRequest.next();
        stopOnRequest.complete();
        this.edge.unsubscribeChannels(this.websocket, subscriptionId);
      }, this.READ_TIMEOUT);
    });
  }

  public initializeAllFields(): Promise<void> {
    return new Promise((resolve) => {
      Promise.all([
        this.getSerialNumbers(0),
        this.getSerialNumbers(1),
        this.getSerialNumbers(2)
      ]).then((models) => {
        this.formSettings = new FormGroup({});
        this.fieldsSettings = this.getSettingsFields();
        this.modelSettings = {};

        this.formTower0 = new FormGroup({});
        this.fieldsTower0 = this.getFields(0);
        this.modelTower0 = models[0];

        this.formTower1 = new FormGroup({});
        this.fieldsTower1 = this.getFields(1);
        this.modelTower1 = models[1];

        this.formTower2 = new FormGroup({});
        this.fieldsTower2 = this.getFields(2);
        this.modelTower2 = models[2];

        resolve();
      });
    });
  }

  public getSettingsFields(): FormlyFieldConfig[] {
    let fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "numberOfTowers",
      type: "input",
      templateOptions: {
        type: "number",
        label: "Anzahl Türme",
        min: 1,
        max: 3,
        required: true
      },
      parsers: [Number],
      defaultValue: this.numberOfTowers
    });

    fields.push({
      key: "numberOfModulesPerTower",
      type: "input",
      templateOptions: {
        type: "number",
        label: "Anzahl Module pro Turm",
        min: 4,
        max: 10,
        required: true
      },
      parsers: [Number],
      defaultValue: this.numberOfModulesPerTower
    });

    return fields;
  }

  /**
   * Generates the fields for the specific tower number (0 - 2).
   * 
   * @param towerNr 
   * @returns an array with the generated fields
   */
  public getFields(towerNr: number): FormlyFieldConfig[] {
    // TODO add validation: no duplicate serial number entries
    let fields: FormlyFieldConfig[] = [];

    switch (towerNr) {
      case 0:
        fields.push({
          key: "batteryInverter",
          type: "input",
          templateOptions: {
            label: "Wechselrichter",
            required: true,
            placeholder: "xxxxxxxxxxxxxxxx"
          },
          validators: {
            validation: ["batteryInverterSerialNumber"]
          },
          wrappers: ["input-serial-number"]
        });
        fields.push({
          key: "emsBox",
          type: "input",
          templateOptions: {
            label: "EMS Box (FEMS Box)",
            required: true,
            prefix: "FH",
            placeholder: "xxxxxxxxxx"
          },
          validators: {
            validation: ["emsBoxSerialNumber"]
          },
          wrappers: ["input-serial-number"]
        });
        break;
      case 1:
        fields.push({
          key: "parallelBox",
          type: "input",
          templateOptions: {
            label: "Parallel Box",
            required: true,
            prefix: "FHP",
            placeholder: "xxxxxxxxx"
          },
          validators: {
            validation: ["boxSerialNumber"]
          },
          wrappers: ["input-serial-number"]
        });
        break;
      case 2:
        fields.push({
          key: "extensionBox",
          type: "input",
          templateOptions: {
            label: "Extension Box",
            required: true,
            prefix: "FHE",
            placeholder: "xxxxxxxxx"
          },
          validators: {
            validation: ["boxSerialNumber"]
          },
          wrappers: ["input-serial-number"]
        });
        break;
    }

    fields.push({
      key: "bmsBox",
      type: "input",
      templateOptions: {
        label: "BMS Box & Sockel",
        required: true,
        placeholder: "xxxxxxxxxxxxxxxxxxxxxxxx"
      },
      validators: {
        validation: ["bmsBoxSerialNumber"]
      },
      wrappers: ["input-serial-number"]
    });

    for (let moduleNr = 0; moduleNr < this.numberOfModulesPerTower; moduleNr++) {
      fields.push({
        key: "module" + moduleNr,
        type: "input",
        templateOptions: {
          label: "Batteriemodul " + (moduleNr + 1),
          required: true,
          // Note: Edit also validator (substring 12) if removing prefix
          prefix: "519110001210",
          placeholder: "xxxxxxxxxxxx"
        },
        validators: {
          validation: ["batterySerialNumber"]
        },
        wrappers: ["input-serial-number"]
      });
    }

    return fields;
  }

  public saveSettings() {
    if (this.formSettings.invalid) {
      this.service.toast("Um die Einstellungen zu übernehmen, geben Sie gültige Werte ein.", "warning");
      return;
    }

    this.numberOfTowers = this.modelSettings.numberOfTowers;
    this.numberOfModulesPerTower = this.modelSettings.numberOfModulesPerTower;

    this.setIsWaiting(true);
    this.initializeAllFields().then(() => {
      this.setIsWaiting(false);
    });
  }

  public extractSerialNumbers(fields: FormlyFieldConfig[]): { label: string, value: string }[] {
    let serialNumbers: { label: string, value: string }[] = [];

    for (let field of fields) {
      serialNumbers.push({
        label: field.templateOptions.label + " Seriennummer",
        value: (field.templateOptions.prefix ?? "") + field.formControl.value
      })
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
