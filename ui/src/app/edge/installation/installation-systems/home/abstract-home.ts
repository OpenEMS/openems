import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { AppCenterUtil } from '../../shared/appcenterutil';
import { Category } from '../../shared/category';
import { FeedInSetting, FeedInType } from '../../shared/enums';
import { AcPv, ComponentData, DcPv, SerialNumberFormData } from '../../shared/ibndatatypes';
import { Meter } from '../../shared/meter';
import { BaseMode, ComponentConfigurator, ConfigurationMode } from '../../views/configuration-execute/component-configurator';
import { SafetyCountry } from '../../views/configuration-execute/safety-country';
import { AbstractIbn, SchedulerIdBehaviour, View } from '../abstract-ibn';

type FeneconHome = {
  SAFETY_COUNTRY: SafetyCountry,
  RIPPLE_CONTROL_RECEIVER_ACTIV: boolean,
  MAX_FEED_IN_POWER?: number,
  FEED_IN_SETTING: string,
  HAS_AC_METER: boolean,
  AC_METER_TYPE?: string,
  HAS_DC_PV1: boolean,
  DC_PV1_ALIAS?: string,
  HAS_DC_PV2: boolean,
  DC_PV2_ALIAS?: string,
  HAS_EMERGENCY_RESERVE: boolean,
  EMERGENCY_RESERVE_ENABLED?: boolean,
  EMERGENCY_RESERVE_SOC?: number,
  SHADOW_MANAGEMENT_DISABLED?: boolean
}

export abstract class AbstractHomeIbn extends AbstractIbn {
  private static readonly SELECTOR = 'Home';

  constructor(public views: View[], public translate: TranslateService) {
    super(views, translate);
  }

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

  // Protocol line side meter fuse
  public lineSideMeterFuse?: {
    category: Category;
    fixedValue?: number;
    otherValue?: number;
  } = {
      category: Category.LINE_SIDE_METER_FUSE_HOME
    };

  public readonly imageUrl: string = 'assets/img/Home-Typenschild-web.jpg';

  public readonly showRundSteuerManual: boolean = true;

  public readonly defaultNumberOfModules: number = 5;

  public abstract readonly emsBoxLabel: Category;

  public showViewCount: boolean = true;

  private numberOfModulesPerTower: number;

  public getLineSideMeterFuseFields() {
    const fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "fixedValue",
      type: "select",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.VALUE'),
        description: this.translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.FIXED_VALUE_DESCRIPTION'),
        options: [
          { label: "25 A", value: 25 },
          { label: "32 A", value: 32 },
          { label: "35 A", value: 35 },
          { label: "40 A", value: 40 },
          { label: "50 A", value: 50 },
          { label: "63 A", value: 63 },
          { label: "80 A", value: 80 },
          { label: this.translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.OTHER'), value: -1 },
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
        label: this.translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.OTHER_VALUE'),
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
        header: i === 0 ? this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BESS_COMPONENTS') : (this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_TOWER', { towerNumber: i }))
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
        label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.NUMBER_OF_TOWERS'),
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
        label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.NUMBER_OF_MODULES_PER_TOWER'),
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
    const emsBoxSerialNumber: string = sessionStorage.getItem('emsBoxSerialNumber');

    switch (towerNr) {
      case 0:
        fields.push({
          key: 'batteryInverter',
          type: 'input',
          templateOptions: {
            label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.INVERTER'),
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
            label: Category.toTranslatedString(this.emsBoxLabel, this.translate),
            required: true,
            prefix: emsBoxSerialNumber ? '' : 'FH',
            placeholder: 'xxxxxxxxxx'
          },
          defaultValue: emsBoxSerialNumber,
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
        label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BMS_BOX'),
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
          label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_MODULE') + (moduleNr + 1),
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
              model[key] = serialNumber.substring(12);
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
        edge.unsubscribeChannels(websocket, 'home');
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
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CHOOSE'),
        placeholder: "Select Option",
        options: [
          { label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.DYNAMIC_LIMITATION'), value: FeedInType.DYNAMIC_LIMITATION },
          { label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.EXTERNAL_LIMITATION'), value: FeedInType.EXTERNAL_LIMITATION }
        ],
        required: true,
      }
    });

    fields.push({
      key: 'maximumFeedInPower',
      type: 'input',
      templateOptions: {
        type: 'number',
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.MAXIMUM_FEED_IN_VALUE'),
        description: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.MAXIMUM_VALUE_DESCRIPTION'),
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
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CHOOSE'),
        description: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.FEED_IN_SETTING_DESCRIPTION'),
        options: [
          { label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.QU_ENABLED_CURVE'), value: FeedInSetting.QuEnableCurve },
          { label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.PU_ENABLED_CURVE'), value: FeedInSetting.PuEnableCurve },
          { label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.FIXED_POWER_FACTOR'), value: FeedInSetting.FixedPowerFactor }
        ],
        required: true
      }
    });

    fields.push({
      key: 'fixedPowerFactor',
      type: 'select',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CONSTANT_VALUE'),
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
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.EXTERNAL_CONTROLLER_CHECK'),
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
    this.requiredControllerIds = [];
    if (this.emergencyReserve.isEnabled) {
      this.requiredControllerIds.push({
        componentId: "ctrlEmergencyCapacityReserve0"
        , behaviour: SchedulerIdBehaviour.MANAGED_BY_APP_MANAGER
      });
    }
    this.requiredControllerIds.push(
      {
        componentId: "ctrlGridOptimizedCharge0"
        , behaviour: SchedulerIdBehaviour.MANAGED_BY_APP_MANAGER
      },
      {
        componentId: "ctrlEssSurplusFeedToGrid0"
        , behaviour: SchedulerIdBehaviour.MANAGED_BY_APP_MANAGER
      },
      {
        componentId: "ctrlBalancing0"
        , behaviour: SchedulerIdBehaviour.MANAGED_BY_APP_MANAGER
      }
    );
  }

  public addCustomBatteryData(batteryData: ComponentData[]) {
    batteryData.push({
      label: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.IS_ACTIVATED'),
      value: this.emergencyReserve.isEnabled ? this.translate.instant('General.yes') : this.translate.instant('General.no'),
    });

    if (this.emergencyReserve.isEnabled) {
      batteryData.push({
        label: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.EMERGENCY_RESERVE_VALUE'),
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
          label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.MAXIMUM_FEED_IN_VALUE'),
          value: feedInLimitation.maximumFeedInPower,
        },
        {
          label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.SHADE_MANAGEMENT_DEACTIVATE'),
          value: this.batteryInverter?.shadowManagementDisabled ? this.translate.instant('General.yes') : this.translate.instant('General.no'),
        },
        {
          label: this.translate.instant('Index.type'),
          value: feedInLimitation.feedInSetting ?? FeedInSetting.Undefined
        }
      )
      : batteryInverterData.push(
        {
          label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.SHADE_MANAGEMENT_DEACTIVATED'),
          value: this.translate.instant('General.yes')
        });

    if (
      feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor
    ) {
      batteryInverterData.push({
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CONSTANT_VALUE'),
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
          { label: this.translate.instant('INSTALLATION.ALIAS_WITH_LABEL', { label: 'MPPT', number: dcNr }), value: dc.alias },
          { label: this.translate.instant('INSTALLATION.VALUE_WITH_LABEL', { label: 'MPPT', number: dcNr, symbol: '' }), value: dc.value },
        ]);
        if (dc.orientation) {
          pvData.push({
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ORIENTATION_WITH_LABEL', { label: 'MPPT', number: dcNr }),
            value: dc.orientation,
          });
        }
        if (dc.moduleType) {
          pvData.push({
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODULE_TYPE_WITH_LABEL', { label: 'MPPT', number: dcNr }),
            value: dc.moduleType
          });
        }
        if (dc.modulesPerString) {
          pvData.push({
            label: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.NUMBER_OF_MODULES_WITH_LABEL', { label: 'MPPT', number: dcNr }),
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
      name: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.EMERGENCY_RESERVE_LABEL', { symbol: '?' }),
      value: emergencyReserve.isEnabled ? this.translate.instant('General.yes') : this.translate.instant('General.no'),
    });

    if (emergencyReserve.isEnabled) {
      protocol.items.push({
        category: Category.EMERGENCY_RESERVE,
        name: this.translate.instant('INSTALLATION.CONFIGURATION_EMERGENCY_RESERVE.EMERGENCY_RESERVE', { symbol: '[%]' }),
        value: emergencyReserve.value ? emergencyReserve.value.toString() : '',
      });
    }

    let lineSideMeterFuseValue: number;
    if (lineSideMeterFuse.otherValue) {
      lineSideMeterFuseValue = lineSideMeterFuse.otherValue;
    } else {
      lineSideMeterFuseValue = lineSideMeterFuse.fixedValue;
    }

    protocol.items.push({
      category: this.lineSideMeterFuse.category,
      name: this.translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.VALUE'),
      value: lineSideMeterFuseValue ? lineSideMeterFuseValue.toString() : '',
    });

    // DC-PV 1
    if (dc1.isSelected) {
      protocol.items.push(
        {
          category: Category.DC_PV_INSTALLATION,
          name: this.translate.instant('INSTALLATION.ALIAS_WITH_LABEL', { label: 'MPPT', number: 1 }),
          value: dc1.alias,
        },
        {
          category: Category.DC_PV_INSTALLATION,
          name: this.translate.instant('INSTALLATION.VALUE_WITH_LABEL', { label: 'MPPT', number: 1, symbol: '[Wp]' }),
          value: dc1.value ? dc1.value.toString() : '',
        });

      dc1.orientation && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ORIENTATION_WITH_LABEL', { label: 'MPPT', number: 1 }),
        value: dc1.orientation,
      });

      dc1.moduleType && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODULE_TYPE_WITH_LABEL', { label: 'MPPT', number: 1 }),
        value: dc1.moduleType,
      });

      dc1.modulesPerString && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.NUMBER_OF_MODULES_WITH_LABEL', { label: 'MPPT', number: 1 }),
        value: dc1.modulesPerString ? dc1.modulesPerString.toString() : '',
      });
    }

    // DC-PV 2
    if (dc2.isSelected) {
      protocol.items.push(
        {
          category: Category.DC_PV_INSTALLATION,
          name: this.translate.instant('INSTALLATION.VALUE_WITH_LABEL', { label: 'MPPT', number: 2, symbol: '[Wp]' }),
          value: dc2.value ? dc2.value.toString() : '',
        },
        {
          category: Category.DC_PV_INSTALLATION,
          name: this.translate.instant('INSTALLATION.ALIAS_WITH_LABEL', { label: 'MPPT', number: 2 }),
          value: dc2.alias,
        });

      dc2.orientation && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ORIENTATION', { label: 'MPPT', number: 2 }),
        value: dc2.orientation,
      });

      dc2.moduleType && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODULE_TYPE_WITH_LABEL', { label: 'MPPT', number: 2 }),
        value: dc2.moduleType,
      });

      dc2.modulesPerString && protocol.items.push({
        category: Category.DC_PV_INSTALLATION,
        name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.NUMBER_OF_MODULES_WITH_LABEL', { label: 'MPPT', number: 2 }),
        value: dc2.modulesPerString ? dc2.modulesPerString.toString() : '',
      });
    }

    protocol.items.push({
      category: Category.FEED_IN_MANAGEMENT,
      name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.EXTERNAL_LIMITATION_ACTIVATED'),
      value: feedInLimitation.feedInType == FeedInType.EXTERNAL_LIMITATION
        ? this.translate.instant('General.yes')
        : this.translate.instant('General.no')
    });

    protocol.items.push({
      category: Category.FEED_IN_MANAGEMENT,
      name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.DYNAMIC_LIMITATION_ACTIVATED'),

      value: feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
        ? this.translate.instant('General.yes')
        : this.translate.instant('General.no')
    });

    if (feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION) {
      protocol.items.push(
        {
          category: Category.FEED_IN_MANAGEMENT,
          name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.MAXIMUM_FEED_IN_VALUE'),
          value: feedInLimitation.maximumFeedInPower
            ? feedInLimitation.maximumFeedInPower.toString()
            : (0).toString(),
        }, {
        category: Category.FEED_IN_MANAGEMENT,
        name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CHOOSE'),
        value: feedInLimitation.feedInSetting,
      });

      if (feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor) {
        protocol.items.push({
          category: Category.FEED_IN_MANAGEMENT,
          name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CONSTANT_VALUE'),
          value: feedInLimitation.fixedPowerFactor,
        });
      }
    }

    for (let index = 0; index < ac.length; index++) {
      const element = ac[index];
      const label = 'AC';
      const acNr = (index + 1);

      protocol.items.push(
        {
          category: Category.ADDITIONAL_AC_PRODUCERS,
          name: this.translate.instant('INSTALLATION.ALIAS_WITH_LABEL', { label: label, number: acNr }),
          value: element.alias,
        },
        {
          category: Category.ADDITIONAL_AC_PRODUCERS,
          name: this.translate.instant('INSTALLATION.VALUE_WITH_LABEL', { label: label, number: acNr, symbol: '[Wp]' }),
          value: element.value ? element.value.toString() : '',
        });

      element.orientation && protocol.items.push({
        category: Category.ADDITIONAL_AC_PRODUCERS,
        name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.ORIENTATION_WITH_LABEL', { label: label, number: acNr }),
        value: element.orientation,
      });

      element.moduleType && protocol.items.push({
        category: Category.ADDITIONAL_AC_PRODUCERS,
        name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODULE_TYPE_WITH_LABEL', { label: label, number: acNr }),
        value: element.moduleType,
      });

      element.modulesPerString && protocol.items.push({
        category: Category.ADDITIONAL_AC_PRODUCERS,
        name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.NUMBER_OF_MODULES_WITH_LABEL', { label: label, number: acNr }),
        value: element.modulesPerString
          ? element.modulesPerString.toString()
          : '',
      });

      element.meterType && protocol.items.push({
        category: Category.ADDITIONAL_AC_PRODUCERS,
        name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.METER_TYPE_WITH_LABEL', { label: label, number: acNr }),
        value: Meter.toLabelString(element.meterType),
      });

      element.modbusCommunicationAddress && protocol.items.push({
        category: Category.ADDITIONAL_AC_PRODUCERS,
        name: this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.MODBUS_WITH_LABEL', { label: label, number: acNr }),
        value: element.modbusCommunicationAddress
          ? element.modbusCommunicationAddress.toString()
          : '',
      });
    }

    protocol.items.push({
      category: Category.EMS_DETAILS,
      name: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.EDGE_NUMBER', { edgeShortName: environment.edgeShortName }),
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
        var name: string = this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.SINGLE_SERIAL_NUMBER', { label: serialNumbers.modules[componentCount].label });
        var serialNumber: string = serialNumbers.modules[componentCount].value;

        // Tower 1
        if (componentCount < numberOfComponentsTower1) {
          protocol.lots.push({
            category: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BESS_COMPONENTS'),
            name: name,
            serialNumber: serialNumber,
          });
        }
        // Tower 2 
        else if (componentCount < numberOfComponentsTower2) {
          protocol.lots.push({
            category: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_TOWER', { towerNumber: 2 }),
            name: name,
            serialNumber: serialNumber,
          });
        }
        // tower 3
        else if (componentCount < numberOfComponentsTower3) {
          protocol.lots.push({
            category: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_TOWER', { towerNumber: 3 }),
            name: name,
            serialNumber: serialNumber,
          });
        }
      }
    }

    return new Promise((resolve, reject) => {
      websocket
        .sendRequest(SubmitSetupProtocolRequest.translateFrom(protocol, this.translate))
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
    if (
      feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor
    ) {
      feedInSetting = feedInLimitation.fixedPowerFactor;
    } else {
      feedInSetting = feedInLimitation.feedInSetting;
    }

    // meter1
    const acArray = this.pv.ac;
    const isAcCreated: boolean = acArray.length >= 1;
    const acAlias: string = isAcCreated ? acArray[0].alias : '';
    const acModbusUnitId: number = isAcCreated
      ? acArray[0].modbusCommunicationAddress
      : 0;
    const acMeterType: Meter = isAcCreated ? acArray[0].meterType : Meter.SOCOMEC;

    let homeAppProperties: FeneconHome = {
      SAFETY_COUNTRY: safetyCountry,
      ...(feedInLimitation.feedInType == FeedInType.EXTERNAL_LIMITATION && { RIPPLE_CONTROL_RECEIVER_ACTIV: true }),
      ...(feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION && { MAX_FEED_IN_POWER: feedInLimitation.maximumFeedInPower }),
      FEED_IN_SETTING: feedInSetting,
      HAS_AC_METER: isAcCreated,
      ...(isAcCreated && { AC_METER_TYPE: Meter.toAppAcMeterType(acMeterType) }),
      HAS_DC_PV1: this.pv.dc1.isSelected,
      ...(this.pv.dc1.isSelected && { DC_PV1_ALIAS: this.pv.dc1.alias }),
      HAS_DC_PV2: this.pv.dc2.isSelected,
      ...(this.pv.dc2.isSelected && { DC_PV2_ALIAS: this.pv.dc2.alias }),
      HAS_EMERGENCY_RESERVE: this.emergencyReserve.isEnabled,
      ...(this.emergencyReserve.isEnabled && { EMERGENCY_RESERVE_ENABLED: this.emergencyReserve.isReserveSocEnabled }),
      ...(this.emergencyReserve.isReserveSocEnabled && { EMERGENCY_RESERVE_SOC: this.emergencyReserve.value }),
      ...(this.batteryInverter?.shadowManagementDisabled && { SHADOW_MANAGEMENT_DISABLED: true })
    };

    // TODO remove
    // system not updated => newest appManager not available
    const isAppManagerAvailable: boolean = AppCenterUtil.isAppManagerAvailable(edge);
    const baseMode = isAppManagerAvailable ? BaseMode.AppManager : BaseMode.UI;

    if (isAppManagerAvailable) {
      componentConfigurator.addInstallAppCallback(() => {
        return new Promise((resolve, reject) => {
          AppCenterUtil.createOrUpdateApp(edge, websocket, "App.FENECON.Home", "FENECON Home", homeAppProperties, AppCenterUtil.keyForIntegratedSystems())
            .then(instance => {
              if (!isAcCreated) {
                resolve(instance);
              }
              let acMeters = instance.dependencies.filter(dependency => {
                return dependency.key == "AC_METER";
              });
              if (acMeters.length == 0) {
                reject(this.translate.instant('INSTALLATION.PROTOCOL_PV_AND_ADDITIONAL_AC.AC_NOT_CREATED'));
              }

              AppCenterUtil.getAppInstance(edge, websocket, Meter.toAppId(acMeterType), acMeters[0].instanceId)
                .then(instance => {
                  // update meter with existing properties
                  instance.properties["MODBUS_UNIT_ID"] = acModbusUnitId;
                  AppCenterUtil.updateApp(edge, websocket, instance.instanceId, acAlias, instance.properties)
                    .then(resolve)
                    .catch(reject);
                }).catch(reject);
            }).catch(reject);
        });
      });
    }

    // TODO remove components later when progress is shown by appManager
    // modbus1
    componentConfigurator.add({
      factoryId: 'Bridge.Modbus.Serial',
      componentId: 'modbus1',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.COMMUNICATION_WITH_BATTERY_INVERTER'),
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
      baseMode: baseMode
    });

    // modbus0
    componentConfigurator.add({
      factoryId: 'Bridge.Modbus.Serial',
      componentId: 'modbus0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.COMMUNICATION_WITH_BATTERY'),
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
      baseMode: baseMode
    });

    // meter0
    componentConfigurator.add({
      factoryId: 'GoodWe.Grid-Meter',
      componentId: 'meter0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.GRID_METER'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode
    });

    // io0
    componentConfigurator.add({
      factoryId: 'IO.KMtronic.4Port',
      componentId: 'io0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.RELAY_BOARD'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus0' },
        { name: 'modbusUnitId', value: 2 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode
    });

    // battery0
    componentConfigurator.add({
      factoryId: 'Battery.Fenecon.Home',
      componentId: 'battery0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.BATTERY'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'startStop', value: 'AUTO' },
        { name: 'modbus.id', value: 'modbus0' },
        { name: 'modbusUnitId', value: 1 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode
    });

    // batteryInverter0
    let goodweconfig = {
      factoryId: 'GoodWe.BatteryInverter',
      componentId: 'batteryInverter0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.BATTERY_INVERTER'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
        { name: 'safetyCountry', value: safetyCountry },
        {
          name: 'backupEnable',
          value: this.emergencyReserve.isEnabled ? 'ENABLE' : 'DISABLE',
        },
        { name: 'setfeedInPowerSettings', value: feedInSetting },
        {
          name: 'mpptForShadowEnable',
          value: this.batteryInverter?.shadowManagementDisabled
            ? 'DISABLE'
            : 'ENABLE',
        },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode
    };

    feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
      ? goodweconfig.properties.push({
        name: 'feedPowerPara',
        value: feedInLimitation.maximumFeedInPower,
      },
        { name: 'feedPowerEnable', value: 'ENABLE' },
      )
      : goodweconfig.properties.push(
        { name: 'feedPowerEnable', value: 'DISABLE' },
      );

    componentConfigurator.add(goodweconfig);

    // PV Meter optional
    componentConfigurator.add(super.addAcPvMeter('modbus1'));

    // charger0
    componentConfigurator.add({
      factoryId: 'GoodWe.Charger-PV1',
      componentId: 'charger0',
      alias: this.pv.dc1.alias ?? "MPPT 1",
      properties: [
        { name: 'enabled', value: true },
        { name: 'essOrBatteryInverter.id', value: 'batteryInverter0' },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: this.pv.dc1.isSelected ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly,
      baseMode: baseMode
    });

    // charger1
    componentConfigurator.add({
      factoryId: 'GoodWe.Charger-PV2',
      componentId: 'charger1',
      alias: this.pv.dc2.alias ?? "MPPT 2",
      properties: [
        { name: 'enabled', value: true },
        { name: 'essOrBatteryInverter.id', value: 'batteryInverter0' },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: this.pv.dc2.isSelected ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly,
      baseMode: baseMode
    });

    // ess0
    componentConfigurator.add({
      factoryId: 'Ess.Generic.ManagedSymmetric',
      componentId: 'ess0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.STORAGE_SYSTEM'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'startStop', value: 'START' },
        { name: 'batteryInverter.id', value: 'batteryInverter0' },
        { name: 'battery.id', value: 'battery0' },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode
    });

    // predictor0
    componentConfigurator.add({
      factoryId: 'Predictor.PersistenceModel',
      componentId: 'predictor0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.PROGNOSIS'),
      properties: [
        { name: 'enabled', value: true },
        {
          name: 'channelAddresses',
          value: ['_sum/ProductionActivePower', '_sum/ConsumptionActivePower'],
        },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode
    });

    // ctrlGridOptimizedCharge0
    let gridOptimizedCharge = {
      factoryId: 'Controller.Ess.GridOptimizedCharge',
      componentId: 'ctrlGridOptimizedCharge0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.GRID_OPTIMIZED_CHARGE'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
        { name: 'meter.id', value: 'meter0' },
        { name: 'delayChargeRiskLevel', value: 'MEDIUM' },
        { name: 'manualTargetTime', value: '17:00' },
        { name: 'debugMode', value: false },
        { name: 'sellToGridLimitRampPercentage', value: 2 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode
    };

    feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
      ? gridOptimizedCharge.properties.push(
        { name: 'maximumSellToGridPower', value: feedInLimitation.maximumFeedInPower },
        { name: "sellToGridLimitEnabled", value: true },
        { name: 'mode', value: 'AUTOMATIC' }
      )
      :
      gridOptimizedCharge.properties.push(
        { name: "sellToGridLimitEnabled", value: false },
        { name: 'mode', value: 'OFF' }
      );

    componentConfigurator.add(gridOptimizedCharge);

    // ctrlEssSurplusFeedToGrid0
    componentConfigurator.add({
      factoryId: 'Controller.Ess.Hybrid.Surplus-Feed-To-Grid',
      componentId: 'ctrlEssSurplusFeedToGrid0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.SURPLUS_ENERGY_FEEDIN'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode
    });

    // ctrlBalancing0
    componentConfigurator.add({
      factoryId: 'Controller.Symmetric.Balancing',
      componentId: 'ctrlBalancing0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.SELF_CONSUMPTION'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
        { name: 'meter.id', value: 'meter0' },
        { name: 'targetGridSetpoint', value: 0 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode
    });

    componentConfigurator.add({
      factoryId: 'GoodWe.EmergencyPowerMeter',
      componentId: 'meter2',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.EMERGENCY_POWER_CONSUMER'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: this.emergencyReserve.isEnabled ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly,
      baseMode: baseMode
    });
    componentConfigurator.add({
      factoryId: 'Controller.Ess.EmergencyCapacityReserve',
      componentId: 'ctrlEmergencyCapacityReserve0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.EMERGENCY_CAPACITY_RESERVE'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'ess.id', value: 'ess0' },
        { name: 'isReserveSocEnabled', value: this.emergencyReserve.isReserveSocEnabled },
        {
          name: 'reserveSoc',
          value: this.emergencyReserve.value ?? 5 /* minimum allowed value */,
        },
      ],
      mode: this.emergencyReserve.isEnabled ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly,
      baseMode: baseMode
    });

    componentConfigurator.add({
      factoryId: 'Ess.Power',
      componentId: '_power',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.POWER'),
      properties: [
        { name: 'enablePid', value: false },
      ],
      mode: ConfigurationMode.UpdateOnly,
      baseMode: baseMode
    });

    return componentConfigurator;
  }
}
