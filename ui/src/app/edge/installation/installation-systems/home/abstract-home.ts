import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { Category, FeedInSetting, FeedInType } from '../../shared/enums';
import { ComponentData, SerialNumberFormData } from '../../shared/ibndatatypes';
import { ComponentConfigurator, ConfigurationMode } from '../../views/configuration-execute/component-configurator';
import { SafetyCountry } from '../../views/configuration-execute/safety-country';
import { AcPv } from '../../views/protocol-additional-ac-producers/protocol-additional-ac-producers.component';
import { DcPv } from '../../views/protocol-pv/protocol-pv.component';
import { AbstractIbn } from '../abstract-ibn';

export abstract class AbstractHomeIbn extends AbstractIbn {
  private static readonly SELECTOR = 'Home';

  // Protocol-pv-component
  public batteryInverter?: {
    shadowManagementDisabled?: boolean;
  };

  // configuration-emergency-reserve
  public emergencyReserve?= {
    isEnabled: true,
    minValue: 5,
    value: 20,
    isReserveSocEnabled: false,
  };

  // protocol-dynamic-feed-in-limitation
  public feedInLimitation?: {
    feedInType: FeedInType,
    maximumFeedInPower?: number;
    feedInSetting?: FeedInSetting;
    fixedPowerFactor?: FeedInSetting;
    isManualProperlyFollowedAndRead?: Boolean;
  } = {
      feedInType: FeedInType.DYNAMIC_LIMITATION,
      maximumFeedInPower: 0,
      feedInSetting: FeedInSetting.QuEnableCurve,
      fixedPowerFactor: FeedInSetting.Undefined,
      isManualProperlyFollowedAndRead: false
    };

  // protocol-pv
  public pv?: {
    dc1?: DcPv;
    dc2?: DcPv;
    ac?: AcPv[];
  };

  public readonly imageUrl: string = 'assets/img/Home-Typenschild-web.jpg';

  public readonly lineSideMeterFuseTitle = Category.LINE_SIDE_METER_FUSE;

  public readonly showRundSteuerManual: boolean = true;

  public readonly defaultNumberOfModules: number = 5;

  public showViewCount: boolean = true;

  private numberOfModulesPerTower: number;

  public getLineSideMeterFuseFields() {
    const fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "fixedValue",
      type: "select",
      templateOptions: {
        label: "Wert [A]",
        description: "Mit welcher Stromstärke ist der Zähler abgesichert?",
        options: [
          { label: "35", value: 35 },
          { label: "50", value: 50 },
          { label: "63", value: 63 },
          { label: "80", value: 80 },
          { label: "Sonstige", value: -1 },
        ],
        required: true
      },
      parsers: [Number]
    });

    fields.push({
      key: "otherValue",
      type: "input",
      templateOptions: {
        type: "number",
        label: "Eigener Wert",
        min: 0,
        required: true
      },
      parsers: [Number],
      validators: {
        validation: ["onlyPositiveInteger"]
      },
      hideExpression: model => model.fixedValue !== -1
    });
    return fields;
  }

  public addPeakShavingData(peakShavingData: ComponentData[]) {
    return peakShavingData;
  }

  public fillForms(
    numberOfTowers: number,
    numberOfModulesPerTower: number,
    models: any,
    forms: SerialNumberFormData[]) {
    this.numberOfModulesPerTower = numberOfModulesPerTower;
    for (let i = 0; i < numberOfTowers; i++) {
      forms[i] = {
        fieldSettings: this.getFields(i, numberOfModulesPerTower),
        model: models[i],
        formTower: new FormGroup({}),
        header: i === 0 ? 'Speichersystemkomponenten' : ('Batterieturm ' + i)
      };
    }
    return forms;
  }

  public getSettingsFields(numberOfModulesPerTower: number, numberOfTowers: number) {
    const fields: FormlyFieldConfig[] = [];

    fields.push({
      key: 'numberOfTowers',
      type: 'input',
      templateOptions: {
        type: 'number',
        label: 'Anzahl Türme',
        min: 1,
        max: 3,
        required: true
      },
      parsers: [Number],
      defaultValue: numberOfTowers
    });

    fields.push({
      key: 'numberOfModulesPerTower',
      type: 'input',
      templateOptions: {
        type: 'number',
        label: 'Anzahl Module pro Turm',
        min: 4,
        max: 10,
        required: true
      },
      parsers: [Number],
      defaultValue: numberOfModulesPerTower
    });

    return fields;
  }

  public getFields(towerNr: number, numberOfModulesPerTower: number) {
    // TODO add validation: no duplicate serial number entries
    const fields: FormlyFieldConfig[] = [];

    switch (towerNr) {
      case 0:
        fields.push({
          key: 'batteryInverter',
          type: 'input',
          templateOptions: {
            label: 'Wechselrichter',
            required: true,
            placeholder: 'xxxxxxxxxxxxxxxx'
          },
          validators: {
            validation: ['batteryInverterSerialNumber']
          },
          wrappers: ['input-serial-number']
        });
        fields.push({
          key: 'emsBox',
          type: 'input',
          templateOptions: {
            label: 'EMS Box (FEMS Box)',
            required: true,
            prefix: 'FH',
            placeholder: 'xxxxxxxxxx'
          },
          validators: {
            validation: ['emsBoxSerialNumber']
          },
          wrappers: ['input-serial-number']
        });
        break;
      case 1:
        fields.push({
          key: 'parallelBox',
          type: 'input',
          templateOptions: {
            label: 'Parallel Box',
            required: true,
            prefix: 'FHP',
            placeholder: 'xxxxxxxxx'
          },
          validators: {
            validation: ['boxSerialNumber']
          },
          wrappers: ['input-serial-number']
        });
        break;
      case 2:
        fields.push({
          key: 'extensionBox',
          type: 'input',
          templateOptions: {
            label: 'Extension Box',
            required: true,
            prefix: 'FHE',
            placeholder: 'xxxxxxxxx'
          },
          validators: {
            validation: ['boxSerialNumber']
          },
          wrappers: ['input-serial-number']
        });
        break;
    }

    fields.push({
      key: 'bmsBox',
      type: 'input',
      templateOptions: {
        label: 'BMS Box & Sockel',
        required: true,
        placeholder: 'xxxxxxxxxxxxxxxxxxxxxxxx'
      },
      validators: {
        validation: ['bmsBoxSerialNumber']
      },
      wrappers: ['input-serial-number']
    });

    for (let moduleNr = 0; moduleNr < numberOfModulesPerTower; moduleNr++) {
      fields.push({
        key: 'module' + moduleNr,
        type: 'input',
        templateOptions: {
          label: 'Batteriemodul ' + (moduleNr + 1),
          required: true,
          // Note: Edit also validator (substring 12) if removing prefix
          prefix: '519110001210',
          placeholder: 'xxxxxxxxxxxx'
        },
        validators: {
          validation: ['batterySerialNumber']
        },
        wrappers: ['input-serial-number']
      });
    }

    return fields;
  }

  public getSerialNumbers(towerNr: number, edge: Edge, websocket: Websocket, numberOfModulesPerTower: number) {
    return new Promise((resolve) => {
      let isResolved = false;
      const channelAddresses: { [key: string]: ChannelAddress } = {};
      const subscriptionId = AbstractHomeIbn.SELECTOR + '-tower' + towerNr;
      const model: Object = {};

      // Gather channel addresses
      channelAddresses['batteryInverter'] = new ChannelAddress('batteryInverter0', 'SerialNumber');
      channelAddresses['bmsBox'] = new ChannelAddress('battery0', 'Tower' + towerNr + 'BmsSerialNumber');

      for (let moduleNr = 0; moduleNr < numberOfModulesPerTower; moduleNr++) {
        channelAddresses['module' + moduleNr] = new ChannelAddress('battery0', 'Tower' + towerNr + 'Module' + moduleNr + 'SerialNumber');
      }

      // Edge-subscribe
      edge.subscribeChannels(websocket, subscriptionId, Object.values(channelAddresses));

      // Subject to stop the subscription to currentData
      const stopOnRequest: Subject<void> = new Subject<void>();

      // Read data
      edge.currentData.pipe(
        takeUntil(stopOnRequest),
        filter(currentData => currentData != null)
      ).subscribe((currentData) => {
        for (const key in channelAddresses) {
          if (channelAddresses.hasOwnProperty(key)) {
            const channelAddress: ChannelAddress = channelAddresses[key];
            const serialNumber: string = currentData.channel[channelAddress.componentId + '/' + channelAddress.channelId];

            // If one serial number is undefined return
            if (!serialNumber) {
              return;
            }

            // Only take a part of the characters if the serial number has a fixed prefix
            if (key.startsWith('module')) {
              model[key] = serialNumber.substr(12, 12);
            } else {
              model[key] = serialNumber;
            }
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
        edge.unsubscribeChannels(websocket, subscriptionId);
      }, 5000);

    });
  }

  public getSettings(edge: Edge, websocket: Websocket): Promise<{ numberOfTowers: number, numberOfModulesPerTower: number }> {
    return new Promise((resolve) => {
      let isResolved = false;

      // Edge-subscribe
      edge.subscribeChannels(websocket, '', [
        new ChannelAddress('battery0', 'NumberOfTowers'),
        new ChannelAddress('battery0', 'NumberOfModulesPerTower')
      ]);

      // Subject to stop the subscription to currentData
      const stopOnRequest: Subject<void> = new Subject<void>();

      // Read tower and module numbers
      edge.currentData.pipe(
        takeUntil(stopOnRequest),
        filter(currentData => currentData != null)
      ).subscribe((currentData) => {
        const numberOfTowers = currentData.channel['battery0/NumberOfTowers'];
        const numberOfModulesPerTower = currentData.channel['battery0/NumberOfModulesPerTower'];

        // If values are available, resolve the promise with them
        if (numberOfTowers && numberOfModulesPerTower) {
          isResolved = true;
          // 10 is given as radix parameter.
          // 2 = binary, 8 = octal, 10 = decimal, 16 = hexadecimal.
          resolve({ numberOfTowers: parseInt(numberOfTowers, 10), numberOfModulesPerTower: parseInt(numberOfModulesPerTower, 10) });
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
        edge.unsubscribeChannels(websocket, '');
      }, 5000);
    });
  }

  public getFeedInLimitFields() {

    const fields: FormlyFieldConfig[] = [];
    const pv = this.pv;
    let totalPvPower = 0;

    totalPvPower += (pv.dc1.isSelected ? pv.dc1.value : 0);
    totalPvPower += (pv.dc2.isSelected ? pv.dc2.value : 0);

    if (pv.ac) {
      for (const ac of pv.ac) {
        totalPvPower += ac.value ?? 0;
      }
    }

    // Update the feedInlimitation field
    this.feedInLimitation.maximumFeedInPower = totalPvPower;
    this.feedInLimitation.isManualProperlyFollowedAndRead = this.feedInLimitation.isManualProperlyFollowedAndRead ? this.feedInLimitation.isManualProperlyFollowedAndRead : null;

    fields.push({
      key: "feedInType",
      type: "select",
      className: "white-space-initial",
      templateOptions: {
        label: "Typ",
        placeholder: "Select Option",
        options: [
          { label: "Dynamische Begrenzung der Einspeisung (z.B. 70% Abregelung)", value: FeedInType.DYNAMIC_LIMITATION },
          { label: "Rundsteuerempfänger (Externe Abregelung durch Netzbetreiber)", value: FeedInType.EXTERNAL_LIMITATION }
        ],
        required: true,
      }
    })

    fields.push({
      key: 'maximumFeedInPower',
      type: 'input',
      templateOptions: {
        type: 'number',
        label: 'Maximale Einspeiseleistung [W]',
        description: 'Diesen Wert entnehmen Sie der Anschlussbestätigung des Netzbetreibers',
        required: true
      },
      parsers: [Number],
      // 10 is given as radix parameter.
      // 2 = binary, 8 = octal, 10 = decimal, 16 = hexadecimal.
      defaultValue: parseInt((totalPvPower * 0.7).toFixed(0), 10),
      hideExpression: model => model.feedInType != FeedInType.DYNAMIC_LIMITATION
    });

    fields.push({
      key: 'feedInSetting',
      type: 'radio',
      templateOptions: {
        label: 'Typ',
        description: 'Wirkleistungsreduzierung bei Überfrequenz',
        options: [
          { label: 'Blindleistungs-Spannungskennlinie Q(U)', value: FeedInSetting.QuEnableCurve },
          { label: 'Verschiebungsfaktor-/Wirkleistungskennlinie Cos φ (P)', value: FeedInSetting.PuEnableCurve },
          { label: 'Fester Verschiebungsfaktor Cos φ', value: FeedInSetting.FixedPowerFactor }
        ],
        required: true
      },
      hideExpression: model => model.feedInType != FeedInType.DYNAMIC_LIMITATION
    });

    fields.push({
      key: 'fixedPowerFactor',
      type: 'select',
      templateOptions: {
        label: 'Cos φ Festwert',
        options: [
          // Leading
          { label: '0.80', value: FeedInSetting.Leading_0_80 },
          { label: '0.81', value: FeedInSetting.Leading_0_81 },
          { label: '0.82', value: FeedInSetting.Leading_0_82 },
          { label: '0.83', value: FeedInSetting.Leading_0_83 },
          { label: '0.84', value: FeedInSetting.Leading_0_84 },
          { label: '0.85', value: FeedInSetting.Leading_0_85 },
          { label: '0.86', value: FeedInSetting.Leading_0_86 },
          { label: '0.87', value: FeedInSetting.Leading_0_87 },
          { label: '0.88', value: FeedInSetting.Leading_0_88 },
          { label: '0.89', value: FeedInSetting.Leading_0_89 },
          { label: '0.90', value: FeedInSetting.Leading_0_90 },
          { label: '0.91', value: FeedInSetting.Leading_0_91 },
          { label: '0.92', value: FeedInSetting.Leading_0_92 },
          { label: '0.93', value: FeedInSetting.Leading_0_93 },
          { label: '0.94', value: FeedInSetting.Leading_0_94 },
          { label: '0.95', value: FeedInSetting.Leading_0_95 },
          { label: '0.96', value: FeedInSetting.Leading_0_96 },
          { label: '0.97', value: FeedInSetting.Leading_0_97 },
          { label: '0.98', value: FeedInSetting.Leading_0_98 },
          { label: '0.99', value: FeedInSetting.Leading_0_99 },
          { label: '1', value: FeedInSetting.Leading_1 },
          // Lagging
          { label: '-0.80', value: FeedInSetting.Lagging_0_80 },
          { label: '-0.81', value: FeedInSetting.Lagging_0_81 },
          { label: '-0.82', value: FeedInSetting.Lagging_0_82 },
          { label: '-0.83', value: FeedInSetting.Lagging_0_83 },
          { label: '-0.84', value: FeedInSetting.Lagging_0_84 },
          { label: '-0.85', value: FeedInSetting.Lagging_0_85 },
          { label: '-0.86', value: FeedInSetting.Lagging_0_86 },
          { label: '-0.87', value: FeedInSetting.Lagging_0_87 },
          { label: '-0.88', value: FeedInSetting.Lagging_0_88 },
          { label: '-0.89', value: FeedInSetting.Lagging_0_89 },
          { label: '-0.90', value: FeedInSetting.Lagging_0_90 },
          { label: '-0.91', value: FeedInSetting.Lagging_0_91 },
          { label: '-0.92', value: FeedInSetting.Lagging_0_92 },
          { label: '-0.93', value: FeedInSetting.Lagging_0_93 },
          { label: '-0.94', value: FeedInSetting.Lagging_0_94 },
          { label: '-0.95', value: FeedInSetting.Lagging_0_95 },
          { label: '-0.96', value: FeedInSetting.Lagging_0_96 },
          { label: '-0.97', value: FeedInSetting.Lagging_0_97 },
          { label: '-0.98', value: FeedInSetting.Lagging_0_98 },
          { label: '-0.99', value: FeedInSetting.Lagging_0_99 }
        ],
        required: true
      },
      hideExpression: model => model.feedInSetting !== FeedInSetting.FixedPowerFactor
    });

    fields.push({
      key: "isManualProperlyFollowedAndRead",
      type: "checkbox",
      templateOptions: {
        label: "Der Rundsteuerempfänger wurde lt. Anleitung ordnungsgemäß und vollständig installiert.",
        required: true,
      },
      hideExpression: model => model.feedInType != FeedInType.EXTERNAL_LIMITATION
    });

    return fields;
  }

  public setFeedInLimitsFields(model: any) {

    this.feedInLimitation.feedInType = model.feedInType;
    this.feedInLimitation.feedInSetting = model.feedInSetting ?? FeedInSetting.Undefined;
    this.feedInLimitation.fixedPowerFactor = model.fixedPowerFactor ?? FeedInSetting.Undefined;
    this.feedInLimitation.isManualProperlyFollowedAndRead = model.isManualProperlyFollowedAndRead;

    return this.feedInLimitation;
  }

  public setRequiredControllers() {
    let requiredControllerIds: string[];
    if (this.emergencyReserve.isEnabled) {
      requiredControllerIds = [
        'ctrlEmergencyCapacityReserve0',
        'ctrlGridOptimizedCharge0',
        'ctrlEssSurplusFeedToGrid0',
        'ctrlBalancing0',
      ];
    } else {
      requiredControllerIds = [
        'ctrlGridOptimizedCharge0',
        'ctrlEssSurplusFeedToGrid0',
        'ctrlBalancing0',
      ];
    }
    this.requiredControllerIds = requiredControllerIds;
  }

  public addCustomBatteryData(batteryData: ComponentData[]) {
    batteryData.push({
      label: 'Notstromfunktion aktiviert?',
      value: this.emergencyReserve.isEnabled ? 'ja' : 'nein',
    });

    if (this.emergencyReserve.isEnabled) {
      batteryData.push({
        label: 'Notstromfunktion Wert',
        value: this.emergencyReserve.value,
      });
    }
    return batteryData;
  }

  public addCustomBatteryInverterData(batteryInverterData: ComponentData[]) {
    const feedInLimitation = this.feedInLimitation;

    feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
      ? batteryInverterData.push(
        {
          label: 'Maximale Einspeiseleistung',
          value: feedInLimitation.maximumFeedInPower,
        },
        {
          label: 'Schattenmanagement deaktiviert',
          value: this.batteryInverter?.shadowManagementDisabled ? 'ja' : 'nein',
        },
        {
          label: 'Typ',
          value: feedInLimitation.feedInSetting ?? FeedInSetting.Undefined
        }
      )
      : batteryInverterData.push(
        {
          label: "Rundsteuerempfänger aktiviert",
          value: "ja"
        })

    if (
      feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor
    ) {
      batteryInverterData.push({
        label: 'Cos ɸ Festwert ',
        value: feedInLimitation.fixedPowerFactor,
      });
    }
    return batteryInverterData;
  }

  public addCustomPvData(pvData: ComponentData[]) {
    let dcNr = 1;
    for (const dc of [this.pv.dc1, this.pv.dc2]) {
      if (dc.isSelected) {
        pvData = pvData.concat([
          { label: 'Alias MPPT' + dcNr, value: dc.alias },
          { label: 'Wert MPPT' + dcNr, value: dc.value },
        ]);
        if (dc.orientation) {
          pvData.push({
            label: 'Ausrichtung MPPT' + dcNr,
            value: dc.orientation,
          });
        }
        if (dc.moduleType) {
          pvData.push({ label: 'Modultyp MPPT' + dcNr, value: dc.moduleType });
        }
        if (dc.modulesPerString) {
          pvData.push({
            label: 'Anzahl PV-Module MPPT' + dcNr,
            value: dc.modulesPerString,
          });
        }
        dcNr++;
      }
    }
    return pvData;
  }

  public getProtocol(edge: Edge, websocket: Websocket): Promise<string> {
    const installer = this.installer;
    const customer = this.customer;
    const feedInLimitation = this.feedInLimitation;
    const pv = this.pv;
    const emergencyReserve = this.emergencyReserve;
    const lineSideMeterFuse = this.lineSideMeterFuse;
    const serialNumbers = this.serialNumbers;
    const dc1 = pv.dc1;
    const dc2 = pv.dc2;
    const ac = pv.ac;

    const installerObj: any = {
      firstname: installer.firstName,
      lastname: installer.lastName,
    };

    const customerObj: any = {
      firstname: customer.firstName,
      lastname: customer.lastName,
      email: customer.email,
      phone: customer.phone,
      address: {
        street: customer.street,
        city: customer.city,
        zip: customer.zip,
        country: customer.country,
      },
    };

    if (customer.isCorporateClient) {
      customerObj.company = {
        name: customer.companyName,
      };
    }

    const protocol: SetupProtocol = {
      fems: {
        id: edge.id,
      },
      installer: installerObj,
      customer: customerObj,
      oem: environment.theme
    };

    // If location data is different to customer data, the location
    // data gets sent too
    if (!this.location.isEqualToCustomerData) {
      const location = this.location;
      protocol.location = {
        firstname: location.firstName,
        lastname: location.lastName,
        email: location.email,
        phone: location.phone,
        address: {
          street: location.street,
          city: location.city,
          zip: location.zip,
          country: location.country,
        },
        company: {
          name: location.companyName,
        },
      };
    }

    protocol.items = [];
    protocol.items.push({
      category: Category.EMERGENCY_RESERVE,
      name: 'Notstrom?',
      value: emergencyReserve.isEnabled ? 'ja' : 'nein',
    });

    if (emergencyReserve.isEnabled) {
      protocol.items.push({
        category: Category.EMERGENCY_RESERVE,
        name: 'Notstromreserve [%]',
        value: emergencyReserve.value ? emergencyReserve.value.toString() : '',
      });
    }

    let lineSideMeterFuseValue: number;
    const lineSideMeterFuseTitle = this.lineSideMeterFuseTitle;
    if (lineSideMeterFuse.otherValue) {
      lineSideMeterFuseValue = lineSideMeterFuse.otherValue;
    } else {
      lineSideMeterFuseValue = lineSideMeterFuse.fixedValue;
    }

    protocol.items.push({
      category: lineSideMeterFuseTitle,
      name: 'Wert [A]',
      value: lineSideMeterFuseValue ? lineSideMeterFuseValue.toString() : '',
    });

    // DC-PV 1
    if (dc1.isSelected) {
      protocol.items.push(
        {
          category: Category.DC_PV_INSTALLATION,
          name: 'Alias MPPT1',
          value: dc1.alias,
        },
        {
          category: Category.DC_PV_INSTALLATION,
          name: 'Wert MPPT1 [Wp]',
          value: dc1.value ? dc1.value.toString() : '',
        });

      dc1.orientation && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: 'Ausrichtung MPPT1',
        value: dc1.orientation,
      });

      dc1.moduleType && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: 'Modultyp MPPT1',
        value: dc1.moduleType,
      });

      dc1.modulesPerString && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: 'Anzahl PV-Module MPPT1',
        value: dc1.modulesPerString ? dc1.modulesPerString.toString() : '',
      });
    }

    // DC-PV 2
    if (dc2.isSelected) {
      protocol.items.push(
        {
          category: Category.DC_PV_INSTALLATION,
          name: 'Wert MPPT2 [Wp]',
          value: dc2.value ? dc2.value.toString() : '',
        },
        {
          category: Category.DC_PV_INSTALLATION,
          name: 'Alias MPPT2',
          value: dc2.alias,
        });

      dc2.orientation && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: 'Ausrichtung MPPT2',
        value: dc2.orientation,
      });

      dc2.moduleType && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: 'Modultyp MPPT2',
        value: dc2.moduleType,
      });

      dc2.modulesPerString && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: 'Anzahl PV-Module MPPT2',
        value: dc2.modulesPerString ? dc2.modulesPerString.toString() : '',
      });
    }

    protocol.items.push({
      category: Category.FEED_IN_MANAGEMENT,
      name: 'Rundsteuerempfänger aktiviert?',
      value: feedInLimitation.feedInType == FeedInType.EXTERNAL_LIMITATION
        ? "ja"
        : "nein"
    });

    protocol.items.push({
      category: Category.FEED_IN_MANAGEMENT,
      name: 'Dynamische Begrenzung der Einspeisung aktiviert?',

      value: feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
        ? "ja"
        : "nein"
    });

    if (feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION) {
      protocol.items.push(
        {
          category: Category.FEED_IN_MANAGEMENT,
          name: 'Maximale Einspeiseleistung [W]',
          value: feedInLimitation.maximumFeedInPower
            ? feedInLimitation.maximumFeedInPower.toString()
            : (0).toString(),
        }, {
        category: Category.FEED_IN_MANAGEMENT,
        name: 'Typ',
        value: feedInLimitation.feedInSetting,
      });

      if (feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor) {
        protocol.items.push({
          category: Category.FEED_IN_MANAGEMENT,
          name: 'Cos φ Festwert',
          value: feedInLimitation.fixedPowerFactor,
        });
      }
    }


    for (let index = 0; index < ac.length; index++) {
      const element = ac[index];
      const label = 'AC' + (index + 1);

      protocol.items.push(
        {
          category: Category.ADDITIONAL_AC_PRODUCERS,
          name: 'Alias ' + label,
          value: element.alias,
        },
        {
          category: Category.ADDITIONAL_AC_PRODUCERS,
          name: 'Wert ' + label + ' [Wp]',
          value: element.value ? element.value.toString() : '',
        });

      element.orientation && protocol.items.push({
        category: Category.ADDITIONAL_AC_PRODUCERS,
        name: 'Ausrichtung ' + label,
        value: element.orientation,
      });

      element.moduleType && protocol.items.push({
        category: Category.ADDITIONAL_AC_PRODUCERS,
        name: 'Modultyp ' + label,
        value: element.moduleType,
      });

      element.modulesPerString && protocol.items.push({
        category: Category.ADDITIONAL_AC_PRODUCERS,
        name: 'Anzahl PV-Module ' + label,
        value: element.modulesPerString
          ? element.modulesPerString.toString()
          : '',
      });

      element.meterType && protocol.items.push({
        category: Category.ADDITIONAL_AC_PRODUCERS,
        name: 'Zählertyp ' + label,
        value: element.meterType,
      });

      element.modbusCommunicationAddress && protocol.items.push({
        category: Category.ADDITIONAL_AC_PRODUCERS,
        name: 'Modbus Kommunikationsadresse ' + label,
        value: element.modbusCommunicationAddress
          ? element.modbusCommunicationAddress.toString()
          : '',
      });
    }

    protocol.items.push({
      category: Category.FEMS_DETAILS,
      name: 'FEMS Nummer',
      value: edge.id,
    });

    // Speichersystemkomponenten
    protocol.lots = [];

    // Initial tower has 3 static components other than modules such as Welcherischter, BMS and EMS box.
    const initialStaticTowerComponents: number = 3;

    // Subsequent towers will have only 2 static components. Paralell box and BMS box.
    const subsequentStaticTowerComponents: number = 2;

    // Total number of components each tower contains, so that easier to categorize the serial numbers based on towers.
    const numberOfComponentsTower1: number = this.numberOfModulesPerTower + initialStaticTowerComponents;
    const numberOfComponentsTower2: number = numberOfComponentsTower1 + this.numberOfModulesPerTower + subsequentStaticTowerComponents;
    const numberOfComponentsTower3: number = numberOfComponentsTower2 + this.numberOfModulesPerTower + subsequentStaticTowerComponents;

    for (let componentCount = 0; componentCount < serialNumbers.modules.length; componentCount++) {
      if (serialNumbers.modules[componentCount].value !== null && serialNumbers.modules[componentCount].value !== '') {
        // Tower 1
        if (componentCount < numberOfComponentsTower1) {
          protocol.lots.push({
            category: 'Speichersystemkomponenten',
            name: serialNumbers.modules[componentCount].label + ' Seriennummer',
            serialNumber: serialNumbers.modules[componentCount].value,
          });
        }
        // Tower 2 
        else if (componentCount < numberOfComponentsTower2) {
          protocol.lots.push({
            category: 'Batterie Turm 2',
            name: serialNumbers.modules[componentCount].label + ' Seriennummer',
            serialNumber: serialNumbers.modules[componentCount].value,
          });
        }
        // tower 3
        else if (componentCount < numberOfComponentsTower3) {
          protocol.lots.push({
            category: 'Batterie Turm 3',
            name: serialNumbers.modules[componentCount].label + ' Seriennummer',
            serialNumber: serialNumbers.modules[componentCount].value,
          });
        }
      }
    }

    return new Promise((resolve, reject) => {
      websocket
        .sendRequest(new SubmitSetupProtocolRequest({ protocol }))
        .then((response: JsonrpcResponseSuccess) => {
          resolve(response.result['setupProtocolId']);
        })
        .catch((reason) => {
          reject(reason);
        });
    });
  }

  public getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket, service?: Service) {
    const componentConfigurator: ComponentConfigurator =
      new ComponentConfigurator(edge, config, websocket);
    // modbus1
    componentConfigurator.add({
      factoryId: 'Bridge.Modbus.Serial',
      componentId: 'modbus1',
      alias: 'Kommunikation mit dem Batterie-Wechselrichter',
      properties: [
        { name: 'enabled', value: true },
        { name: 'portName', value: '/dev/busUSB2' },
        { name: 'baudRate', value: 9600 },
        { name: 'databits', value: 8 },
        { name: 'stopbits', value: 'ONE' },
        { name: 'parity', value: 'NONE' },
        { name: 'logVerbosity', value: 'NONE' },
        { name: 'invalidateElementsAfterReadErrors', value: 1 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // modbus0
    componentConfigurator.add({
      factoryId: 'Bridge.Modbus.Serial',
      componentId: 'modbus0',
      alias: 'Kommunikation mit der Batterie',
      properties: [
        { name: 'enabled', value: true },
        { name: 'portName', value: '/dev/busUSB1' },
        { name: 'baudRate', value: 19200 },
        { name: 'databits', value: 8 },
        { name: 'stopbits', value: 'ONE' },
        { name: 'parity', value: 'NONE' },
        { name: 'logVerbosity', value: 'NONE' },
        { name: 'invalidateElementsAfterReadErrors', value: 1 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // meter0
    componentConfigurator.add({
      factoryId: 'GoodWe.Grid-Meter',
      componentId: 'meter0',
      alias: 'Netzzähler',
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // io0
    componentConfigurator.add({
      factoryId: 'IO.KMtronic.4Port',
      componentId: 'io0',
      alias: 'Relaisboard',
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus0' },
        { name: 'modbusUnitId', value: 2 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // battery0
    componentConfigurator.add({
      factoryId: 'Battery.Fenecon.Home',
      componentId: 'battery0',
      alias: 'Batterie',
      properties: [
        { name: 'enabled', value: true },
        { name: 'startStop', value: 'AUTO' },
        { name: 'modbus.id', value: 'modbus0' },
        { name: 'modbusUnitId', value: 1 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // Determine safety country
    let safetyCountry: SafetyCountry;
    if (this.location.isEqualToCustomerData) {
      safetyCountry = SafetyCountry.getSafetyCountry(this.customer.country);
    } else {
      safetyCountry = SafetyCountry.getSafetyCountry(this.location.country);
    }

    // Determine feed-in-setting
    let feedInSetting: FeedInSetting;
    const feedInLimitation = this.feedInLimitation;
    if (feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor) {
      feedInSetting = feedInLimitation.fixedPowerFactor;
    } else {
      feedInSetting = feedInLimitation.feedInSetting;
    }

    // batteryInverter0
    let goodweconfig = {
      factoryId: 'GoodWe.BatteryInverter',
      componentId: 'batteryInverter0',
      alias: 'Batterie-Wechselrichter',
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
        { name: 'safetyCountry', value: safetyCountry },
        {
          name: 'backupEnable',
          value: this.emergencyReserve.isEnabled ? 'ENABLE' : 'DISABLE',
        },
        { name: 'feedPowerEnable', value: 'ENABLE' },
        { name: 'setfeedInPowerSettings', value: feedInSetting },
        {
          name: 'mpptForShadowEnable',
          value: this.batteryInverter?.shadowManagementDisabled
            ? 'DISABLE'
            : 'ENABLE',
        },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    }

    feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION && goodweconfig.properties.push({
      name: 'feedPowerPara',
      value: feedInLimitation.maximumFeedInPower,
    })

    componentConfigurator.add(goodweconfig);

    // meter1
    const acArray = this.pv.ac;
    const isAcCreated: boolean = acArray.length >= 1;

    // TODO if more than 1 meter should be created, this logic must be changed
    const acAlias = isAcCreated ? acArray[0].alias : '';
    const acModbusUnitId = isAcCreated
      ? acArray[0].modbusCommunicationAddress
      : 0;

    componentConfigurator.add({
      factoryId: 'Meter.Socomec.Threephase',
      componentId: 'meter1',
      alias: acAlias,
      properties: [
        { name: 'enabled', value: true },
        { name: 'type', value: 'PRODUCTION' },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: acModbusUnitId },
        { name: 'invert', value: false },
      ],
      mode: isAcCreated
        ? ConfigurationMode.RemoveAndConfigure
        : ConfigurationMode.RemoveOnly,
    });

    // charger0
    componentConfigurator.add({
      factoryId: 'GoodWe.Charger-PV1',
      componentId: 'charger0',
      alias: this.pv.dc1.alias,
      properties: [
        { name: 'enabled', value: true },
        { name: 'essOrBatteryInverter.id', value: 'batteryInverter0' },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: this.pv.dc1.isSelected
        ? ConfigurationMode.RemoveAndConfigure
        : ConfigurationMode.RemoveOnly,
    });

    // charger1
    componentConfigurator.add({
      factoryId: 'GoodWe.Charger-PV2',
      componentId: 'charger1',
      alias: this.pv.dc2.alias,
      properties: [
        { name: 'enabled', value: true },
        { name: 'essOrBatteryInverter.id', value: 'batteryInverter0' },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: this.pv.dc2.isSelected
        ? ConfigurationMode.RemoveAndConfigure
        : ConfigurationMode.RemoveOnly,
    });

    // ess0
    componentConfigurator.add({
      factoryId: 'Ess.Generic.ManagedSymmetric',
      componentId: 'ess0',
      alias: 'Speichersystem',
      properties: [
        { name: 'enabled', value: true },
        { name: 'startStop', value: 'START' },
        { name: 'batteryInverter.id', value: 'batteryInverter0' },
        { name: 'battery.id', value: 'battery0' },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // predictor0
    componentConfigurator.add({
      factoryId: 'Predictor.PersistenceModel',
      componentId: 'predictor0',
      alias: 'Prognose',
      properties: [
        { name: 'enabled', value: true },
        {
          name: 'channelAddresses',
          value: ['_sum/ProductionActivePower', '_sum/ConsumptionActivePower'],
        },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // ctrlGridOptimizedCharge0
    let gridOptimizedCharge = {
      factoryId: 'Controller.Ess.GridOptimizedCharge',
      componentId: 'ctrlGridOptimizedCharge0',
      alias: 'Netzdienliche Beladung',
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
        { name: 'meter.id', value: 'meter0' },
        { name: 'sellToGridLimitEnabled', value: true },
        {
          name: 'maximumSellToGridPower',
          value: feedInLimitation.maximumFeedInPower,
        },
        { name: 'delayChargeRiskLevel', value: 'MEDIUM' },
        { name: 'mode', value: 'AUTOMATIC' },
        { name: 'manualTargetTime', value: '17:00' },
        { name: 'debugMode', value: false },
        { name: 'sellToGridLimitRampPercentage', value: 2 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    }
    componentConfigurator.add(gridOptimizedCharge);

    // ctrlEssSurplusFeedToGrid0
    componentConfigurator.add({
      factoryId: 'Controller.Ess.Hybrid.Surplus-Feed-To-Grid',
      componentId: 'ctrlEssSurplusFeedToGrid0',
      alias: 'Überschusseinspeisung',
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    // ctrlBalancing0
    componentConfigurator.add({
      factoryId: 'Controller.Symmetric.Balancing',
      componentId: 'ctrlBalancing0',
      alias: 'Eigenverbrauchsoptimierung',
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
        { name: 'meter.id', value: 'meter0' },
        { name: 'targetGridSetpoint', value: 0 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
    });

    const emergencyReserve = this.emergencyReserve;
    componentConfigurator.add({
      factoryId: 'GoodWe.EmergencyPowerMeter',
      componentId: 'meter2',
      alias: 'Notstromverbraucher',
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: emergencyReserve.isEnabled
        ? ConfigurationMode.RemoveAndConfigure
        : ConfigurationMode.RemoveOnly,
    });

    componentConfigurator.add({
      factoryId: 'Controller.Ess.EmergencyCapacityReserve',
      componentId: 'ctrlEmergencyCapacityReserve0',
      alias: 'Ansteuerung der Notstromreserve',
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
        { name: 'isReserveSocEnabled', value: emergencyReserve.isReserveSocEnabled },
        {
          name: 'reserveSoc',
          value: emergencyReserve.value ?? 5 /* minimum allowed value */,
        },
      ],
      mode: emergencyReserve.isEnabled
        ? ConfigurationMode.RemoveAndConfigure
        : ConfigurationMode.RemoveOnly,
    });
    return componentConfigurator;
  }
}
