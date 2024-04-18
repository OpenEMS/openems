import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';
import { SetupProtocol, SubmitSetupProtocolRequest } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';

import { AppCenterUtil } from '../../shared/appcenterutil';
import { Category } from '../../shared/category';
import { FeedInSetting, FeedInType, View, WebLinks } from '../../shared/enums';
import { ComponentData, DcPv, SerialNumberFormData } from '../../shared/ibndatatypes';
import { Meter } from '../../shared/meter';
import { SystemId, SystemType } from '../../shared/system';
import { BaseMode, ComponentConfigurator, ConfigurationMode } from '../../views/configuration-execute/component-configurator';
import { SafetyCountry } from '../../views/configuration-execute/safety-country';
import { AbstractIbn, SchedulerIdBehaviour } from '../abstract-ibn';

/**
 * Represents common properties shared between Home 20 and Home 30 configurations.
 */
export type Home2030CommonApp = {
  SAFETY_COUNTRY: SafetyCountry,
  FEED_IN_TYPE: FeedInType,
  FEED_IN_SETTING: string,
  GRID_METER_CATEGORY: Meter.GridMeterCategory,
  CT_RATIO_FIRST: number,
  MAX_FEED_IN_POWER?: number,
  HAS_EMERGENCY_RESERVE: boolean,
  EMERGENCY_RESERVE_ENABLED?: boolean,
  EMERGENCY_RESERVE_SOC?: number,
  SHADOW_MANAGEMENT_DISABLED?: boolean
}

export abstract class AbstractHomeIbn extends AbstractIbn {
  private static readonly SELECTOR = 'Home';
  public override readonly type: SystemType = SystemType.FENECON_HOME;
  private static readonly MODULE_DATA_TIMEOUT = 12_000; // two minutes
  private static readonly TOWER_DATA_TIMEOUT = 5_000; // five seconds

  constructor(public override views: View[], public override translate: TranslateService) {
    super(views, translate);
  }

  // Protocol-pv-component
  public batteryInverter?: {
    shadowManagementDisabled?: boolean;
  };

  // configuration-emergency-reserve
  public override emergencyReserve? = {
    isEnabled: true,
    minValue: 5,
    value: 20,
    isReserveSocEnabled: false,
  };

  // protocol-dynamic-feed-in-limitation
  public override feedInLimitation?: {
    feedInType: FeedInType,
    maximumFeedInPower?: number;
    feedInSetting?: FeedInSetting;
    fixedPowerFactor?: FeedInSetting;
    isManualProperlyFollowedAndRead?: boolean;
  } = {
      feedInType: FeedInType.DYNAMIC_LIMITATION,
      maximumFeedInPower: 0,
      feedInSetting: FeedInSetting.QuEnableCurve,
      fixedPowerFactor: FeedInSetting.Undefined,
      isManualProperlyFollowedAndRead: false,
    };

  // protocol-pv
  public pv?: {
    dc?: DcPv[];
  };

  // Protocol line side meter fuse
  public override lineSideMeterFuse?: {
    category: Category;
    fixedValue?: number;
    otherValue?: number;
  } = {
      category: Category.LINE_SIDE_METER_FUSE_HOME,
    };

  //Configuration Summary
  public override gtcAndWarrantyLinks: {
    gtcLink: WebLinks;
    warrantyLink: WebLinks;
  } = {
      gtcLink: WebLinks.GTC_LINK,
      warrantyLink: WebLinks.WARRANTY_LINK_HOME,
    };

  // configuration Energu Flow Meter
  public energyFlowMeter: {
    meter: Meter.GridMeterCategory;
    value?: number;
  };

  public abstract mppt: any;

  public readonly imageUrl: string = 'assets/img/Home-Typenschild-web.jpg';
  public readonly relayFactoryId: string = 'IO.KMtronic.4Port'; // Default 'Home10' factoryId.

  public override readonly showRundSteuerManual: boolean = true;
  public override showViewCount: boolean = false;
  public override readonly defaultNumberOfModules: number = 5;
  private numberOfModulesPerTower: number;
  public readonly maxFeedInLimit: number = 29999;

  public abstract readonly emsBoxLabel: Category;
  public abstract readonly maxNumberOfPvStrings: number;
  public abstract readonly maxNumberOfMppt: number;
  public abstract readonly homeAppId: string;
  public abstract readonly homeAppAlias: string;
  public abstract readonly maxNumberOfTowers: number;
  public abstract readonly maxNumberOfModulesPerTower: number;
  public abstract readonly minNumberOfModulesPerTower: number;

  /**
 * Generates and returns the Specific properties required for Home APP variant.
 *
 * @param safetyCountry Safety Country configured.
 * @param feedInSetting Feed in Setting configured.
 * @returns The Home APP properties of the specific variant.
 */
  public abstract getHomeAppProperties(safetyCountry: SafetyCountry, feedInSetting: FeedInSetting): {};

  public override fillSerialNumberForms(
    numberOfTowers: number,
    numberOfModulesPerTower: number,
    models: any,
    forms: SerialNumberFormData[]) {
    this.numberOfModulesPerTower = numberOfModulesPerTower;
    for (let i = 0; i < numberOfTowers; i++) {
      forms[i] = {
        fieldSettings: this.getSerialNumberFields(i, numberOfModulesPerTower),
        model: models[i],
        formTower: new FormGroup({}),
        header: i === 0 ? this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BESS_COMPONENTS') : (this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_TOWER', { number: i })),
      };
    }
    return forms;
  }

  public override getPreSettingsFields(numberOfModulesPerTower: number, numberOfTowers: number) {
    const fields: FormlyFieldConfig[] = [];

    fields.push({
      key: 'numberOfTowers',
      type: 'input',
      templateOptions: {
        type: 'number',
        label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.NUMBER_OF_TOWERS'),
        min: 1,
        max: this.maxNumberOfTowers,
        required: true,
      },
      parsers: [Number],
      defaultValue: numberOfTowers,
    });

    fields.push({
      key: 'numberOfModulesPerTower',
      type: 'input',
      templateOptions: {
        type: 'number',
        label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.NUMBER_OF_MODULES_PER_TOWER'),
        min: this.minNumberOfModulesPerTower,
        max: this.maxNumberOfModulesPerTower,
        required: true,
      },
      parsers: [Number],
      defaultValue: numberOfModulesPerTower,
    });

    return fields;
  }

  public override getSerialNumberFields(towerNr: number, numberOfModulesPerTower: number) {
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
            placeholder: 'xxxxxxxxxxxxxxxx',
          },
          validators: {
            validation: ['batteryInverterSerialNumber'],
          },
          wrappers: ['input-serial-number'],
        });
        fields.push({
          key: 'emsBox',
          type: 'input',
          templateOptions: {
            label: Category.toTranslatedString(this.emsBoxLabel, this.translate),
            required: true,
            placeholder: 'xxxxxxxxxx',
          },
          defaultValue: emsBoxSerialNumber,
          validators: {
            validation: ['emsBoxSerialNumber'],
          },
          wrappers: ['input-serial-number'],
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
            placeholder: 'xxxxxxxxx',
          },
          validators: {
            validation: ['boxSerialNumber'],
          },
          wrappers: ['input-serial-number'],
        });
        break;
      default: // usually 2,3,4,5
        fields.push({
          key: 'extensionBox',
          type: 'input',
          templateOptions: {
            label: 'Extension Box',
            required: true,
            prefix: 'FHE',
            placeholder: 'xxxxxxxxx',
          },
          validators: {
            validation: ['boxSerialNumber'],
          },
          wrappers: ['input-serial-number'],
        });
        break;
    }

    fields.push({
      key: 'bmsBox',
      type: 'input',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BMS_BOX'),
        required: true,
        placeholder: 'xxxxxxxxxxxxxxxxxxxxxxxx',
      },
      validators: {
        validation: ['batteryAndBmsBoxSerialNumber'],
      },
      wrappers: ['input-serial-number'],
    });

    for (let moduleNr = 0; moduleNr < numberOfModulesPerTower; moduleNr++) {
      fields.push({
        key: 'module' + moduleNr,
        type: 'input',
        templateOptions: {
          label: this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_MODULE') + (moduleNr + 1),
          required: true,
          placeholder: 'xxxxxxxxxxxxxxxxxxxxxxxx',
        },
        validators: {
          validation: ['batteryAndBmsBoxSerialNumber'],
        },
        wrappers: ['input-serial-number'],
      });
    }

    return fields;
  }

  public override getSerialNumbersFromEdge(towerNr: number, edge: Edge, websocket: Websocket, numberOfModulesPerTower: number) {
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
        filter(currentData => currentData != null),
      ).subscribe((currentData) => {
        let anyNullOrUndefined: boolean = false;
        for (const key in channelAddresses) {
          if (key in channelAddresses) {
            const channelAddress: ChannelAddress = channelAddresses[key];
            const serialNumber: string = currentData.channel[channelAddress.componentId + '/' + channelAddress.channelId];

            // If one serial number is undefined return
            if (!serialNumber) {
              anyNullOrUndefined = true;
              continue;
            }

            // Add serial Number
            model[key] = serialNumber;
          }
        }

        if (!anyNullOrUndefined) {
          // Resolve the promise
          isResolved = true;
          resolve(model);
        }
      });
      setTimeout(() => {
        // If data isn't available after the timeout, the
        // promise gets resolved with an empty/partially filled object
        if (!isResolved) {
          resolve(model);
        }

        // Unsubscribe to currentData and channels after timeout
        stopOnRequest.next();
        stopOnRequest.complete();
        edge.unsubscribeChannels(websocket, subscriptionId);
      }, AbstractHomeIbn.MODULE_DATA_TIMEOUT);

    });
  }

  public override getPreSettingInformationFromEdge(edge: Edge, websocket: Websocket): Promise<{ numberOfTowers: number, numberOfModulesPerTower: number }> {
    return new Promise((resolve) => {
      let isResolved = false;

      // Edge-subscribe
      edge.subscribeChannels(websocket, '', [
        new ChannelAddress('battery0', 'NumberOfTowers'),
        new ChannelAddress('battery0', 'NumberOfModulesPerTower'),
      ]);

      // Subject to stop the subscription to currentData
      const stopOnRequest: Subject<void> = new Subject<void>();

      // Read tower and module numbers
      edge.currentData.pipe(
        takeUntil(stopOnRequest),
        filter(currentData => currentData != null),
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
      }, AbstractHomeIbn.TOWER_DATA_TIMEOUT);
    });
  }

  public getFeedInLimitFields() {

    const pv: {
      dc?: DcPv[];
    } = this.pv;

    let totalPvPower: number = 0;
    for (const dc of pv.dc) {
      totalPvPower += (dc.isSelected ? Number.parseInt(dc.value.toString()) : 0);
    }

    // maximum limit for feed in power is 30000.
    totalPvPower = Math.min(totalPvPower, this.maxFeedInLimit);

    // Update the feedInlimitation field
    this.feedInLimitation.maximumFeedInPower = totalPvPower;
    this.feedInLimitation.isManualProperlyFollowedAndRead = this.feedInLimitation.isManualProperlyFollowedAndRead ? this.feedInLimitation.isManualProperlyFollowedAndRead : null;

    const fields: FormlyFieldConfig[] = super.getCommonFeedInLimitsFields(totalPvPower);

    super.addAdditionalFeedInLimitsFields(fields);

    return fields;
  }

  public override setFeedInLimitFields(model: any) {

    this.feedInLimitation.feedInType = model.feedInType;
    this.feedInLimitation.feedInSetting = model.feedInSetting ?? FeedInSetting.Undefined;
    this.feedInLimitation.fixedPowerFactor = model.fixedPowerFactor ?? FeedInSetting.Undefined;
    this.feedInLimitation.isManualProperlyFollowedAndRead = model.isManualProperlyFollowedAndRead;
  }

  public override setRequiredControllers() {
    this.requiredControllerIds = [];
    if (this.emergencyReserve.isEnabled) {
      this.requiredControllerIds.push({
        componentId: "ctrlEmergencyCapacityReserve0",
        behaviour: SchedulerIdBehaviour.MANAGED_BY_APP_MANAGER,
      });
    }
    this.requiredControllerIds.push(
      {
        componentId: "ctrlGridOptimizedCharge0",
        behaviour: SchedulerIdBehaviour.MANAGED_BY_APP_MANAGER,
      },
      {
        componentId: "ctrlEssSurplusFeedToGrid0",
        behaviour: SchedulerIdBehaviour.MANAGED_BY_APP_MANAGER,
      },
      {
        componentId: "ctrlBalancing0",
        behaviour: SchedulerIdBehaviour.MANAGED_BY_APP_MANAGER,
      },
    );
  }

  public override addCustomBatteryData() {
    const batteryData: ComponentData[] = [];
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

  public override addCustomBatteryInverterData() {
    const batteryInverterData: ComponentData[] = [];

    this.feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
      ? batteryInverterData.push(
        {
          label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.MAXIMUM_FEED_IN_VALUE'),
          value: this.feedInLimitation.maximumFeedInPower,
        },
        {
          label: this.translate.instant('INSTALLATION.PROTOCOL_PV.SHADE_MANAGEMENT_DEACTIVATE'),
          value: this.batteryInverter?.shadowManagementDisabled ? this.translate.instant('General.yes') : this.translate.instant('General.no'),
        },
        {
          label: this.translate.instant('Index.TYPE'),
          value: this.feedInLimitation.feedInSetting ?? FeedInSetting.Undefined,
        },
      )
      : batteryInverterData.push(
        {
          label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.SHADE_MANAGEMENT_DEACTIVATED'),
          value: this.translate.instant('General.yes'),
        },
      );

    if (this.feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor) {
      batteryInverterData.push({
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CONSTANT_VALUE'),
        value: this.feedInLimitation.fixedPowerFactor,
      });
    }
    return batteryInverterData;
  }

  public override addCustomPvData() {
    const pvData: ComponentData[] = [];

    let pvNr: number = 1;
    for (const dc of this.pv.dc) {
      const mppt: number = pvNr;
      if (dc.isSelected) {
        pvData.push(
          {
            label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.ALIAS_WITH_LABEL_HOME_DC', { mppt: mppt }),
            value: dc.alias,
          },
          {
            label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.VALUE_WITH_LABEL_HOME_DC', { mppt: mppt, symbol: '' }),
            value: dc.value,
          },
        );
        if (dc.orientation) {
          pvData.push({
            label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.ORIENTATION_WITH_LABEL_HOME_DC', { mppt: mppt }),
            value: dc.orientation,
          });
        }
        if (dc.moduleType) {
          pvData.push({
            label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.MODULE_TYPE_WITH_LABEL_HOME_DC', { mppt: mppt }),
            value: dc.moduleType,
          });
        }
        if (dc.modulesPerString) {
          pvData.push({
            label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.NUMBER_OF_MODULES_WITH_LABEL_HOME_DC', { mppt: mppt }),
            value: dc.modulesPerString,
          });
        }
        if (this.maxNumberOfMppt !== -1) {
          pvData.push({
            label: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.BOTH_SELECTED_LABEL', { pv1: pvNr * 2 - 1, pv2: pvNr * 2 }),
            value: dc.portsConnected ? this.translate.instant('General.yes') : this.translate.instant('General.no'),
          });
        }
      }
      pvNr++;
    }
    return pvData;
  }

  public override getProtocol(edge: Edge, websocket: Websocket): Promise<string> {
    const protocol: SetupProtocol = super.getCommonProtocolItems(edge);

    const feedInLimitation = this.feedInLimitation;
    const emergencyReserve = this.emergencyReserve;
    const energyFlowMeter = this.energyFlowMeter;

    if (energyFlowMeter) {
      protocol.items.push({
        category: Category.GRID_METER_CATEGORY,
        name: this.translate.instant('INSTALLATION.CONFIGURATION_ENERGY_FLOW_METER.METER.LABEL'),
        value: Meter.toGridMeterCategoryLabelString(energyFlowMeter.meter, this.translate),
      });
      if (energyFlowMeter.meter === Meter.GridMeterCategory.COMMERCIAL_METER) {
        protocol.items.push({
          category: Category.GRID_METER_CATEGORY,
          name: this.translate.instant('INSTALLATION.CONFIGURATION_ENERGY_FLOW_METER.CONVERTER_RATIO'),
          value: energyFlowMeter.value.toString(),
        });
      }
    }

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

    for (let index = 0; index < this.pv.dc.length; index++) {
      const dc: DcPv = this.pv.dc[index];
      const label: string = 'MPPT';
      const dcNr: number = index + 1;

      // DC-PV
      if (dc.isSelected) {
        protocol.items.push(
          {
            category: Category.DC_PV_INSTALLATION,
            name: this.translate.instant('INSTALLATION.ALIAS_WITH_LABEL', { label: label, number: dcNr }),
            value: dc.alias,
          },
          {
            category: Category.DC_PV_INSTALLATION,
            name: this.translate.instant('INSTALLATION.VALUE_WITH_LABEL', { label: label, number: dcNr, symbol: '[Wp]' }),
            value: dc.value ? dc.value.toString() : '',
          },
        );

        dc.orientation && protocol.items.push({
          category: Category.DC_PV_INSTALLATION,
          name: this.translate.instant('INSTALLATION.PROTOCOL_PV.ORIENTATION_WITH_LABEL', { label: label, number: dcNr }),
          value: dc.orientation,
        });

        dc.moduleType && protocol.items.push({
          category: Category.DC_PV_INSTALLATION,
          name: this.translate.instant('INSTALLATION.PROTOCOL_PV.MODULE_TYPE_WITH_LABEL', { label: label, number: dcNr }),
          value: dc.moduleType,
        });

        dc.modulesPerString && protocol.items.push({
          category: Category.DC_PV_INSTALLATION,
          name: this.translate.instant('INSTALLATION.PROTOCOL_PV.NUMBER_OF_MODULES_WITH_LABEL', { label: label, number: dcNr }),
          value: dc.modulesPerString ? dc.modulesPerString.toString() : '',
        });

        if (this.maxNumberOfMppt !== -1) {
          protocol.items.push({
            category: Category.DC_PV_INSTALLATION,
            name: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.BOTH_SELECTED_LABEL', { pv1: dcNr * 2 - 1, pv2: dcNr * 2 }),
            value: dc.portsConnected ? this.translate.instant('General.yes') : this.translate.instant('General.no'),
          });
        }
      }
    }

    protocol.items.push({
      category: Category.FEED_IN_MANAGEMENT,
      name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.EXTERNAL_LIMITATION_ACTIVATED'),
      value: feedInLimitation.feedInType == FeedInType.EXTERNAL_LIMITATION
        ? this.translate.instant('General.yes')
        : this.translate.instant('General.no'),
    });

    protocol.items.push({
      category: Category.FEED_IN_MANAGEMENT,
      name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.DYNAMIC_LIMITATION_ACTIVATED'),

      value: feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
        ? this.translate.instant('General.yes')
        : this.translate.instant('General.no'),
    });

    protocol.items.push({
      category: Category.FEED_IN_MANAGEMENT,
      name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.NO_LIMITATION'),

      value: feedInLimitation.feedInType == FeedInType.NO_LIMITATION
        ? this.translate.instant('General.yes')
        : this.translate.instant('General.no'),
    });

    if (feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION) {
      protocol.items.push(
        {
          category: Category.FEED_IN_MANAGEMENT,
          name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.MAXIMUM_FEED_IN_VALUE'),
          value: feedInLimitation.maximumFeedInPower
            ? feedInLimitation.maximumFeedInPower.toString()
            : (0).toString(),
        },
        {
          category: Category.FEED_IN_MANAGEMENT,
          name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CHOOSE'),
          value: feedInLimitation.feedInSetting,
        },
      );

      if (feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor) {
        protocol.items.push({
          category: Category.FEED_IN_MANAGEMENT,
          name: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CONSTANT_VALUE'),
          value: feedInLimitation.fixedPowerFactor,
        });
      }
    }

    // Subsequent towers will have only 2 static components. Paralell box and BMS box.
    const subsequentStaticTowerComponents: number = 2;
    const categoryElement: string = 'INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BATTERY_TOWER';

    super.addProtocolSerialNumbers(protocol, this.numberOfModulesPerTower, subsequentStaticTowerComponents, categoryElement);

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

  public override getSystemVariantFields(): FormlyFieldConfig[] {

    const label = [
      { value: SystemId.FENECON_HOME_10, label: SystemId.FENECON_HOME_10 },
      { value: SystemId.FENECON_HOME_20, label: SystemId.FENECON_HOME_20 },
      { value: SystemId.FENECON_HOME_30, label: SystemId.FENECON_HOME_30 },
    ];

    const fields: FormlyFieldConfig[] = [{
      key: 'system',
      type: 'radio',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.CONFIGURATION_SYSTEM.PRODUCT_NAME'),
        type: 'radio',
        options: label,
        required: true,
      },
    }];

    return fields;
  }

  public override getComponentConfigurator(edge: Edge, config: EdgeConfig, websocket: Websocket, service?: Service) {
    const componentConfigurator: ComponentConfigurator =
      new ComponentConfigurator(edge, config, websocket);

    // Determine safety country
    const safetyCountry: SafetyCountry = this.location.isEqualToCustomerData
      ? SafetyCountry.getSafetyCountry(this.customer.country)
      : SafetyCountry.getSafetyCountry(this.location.country);

    // Determine feed-in-setting
    let feedInSetting: FeedInSetting;
    const feedInLimitation = this.feedInLimitation;
    feedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor
      ? feedInSetting = feedInLimitation.fixedPowerFactor
      : feedInSetting = feedInLimitation.feedInSetting;

    // TODO remove
    // system not updated => newest appManager not available
    const isAppManagerAvailable: boolean = AppCenterUtil.isAppManagerAvailable(edge);
    const baseMode = isAppManagerAvailable ? BaseMode.AppManager : BaseMode.UI;

    if (isAppManagerAvailable) {
      const homeAppProperties = this.getHomeAppProperties(safetyCountry, feedInSetting);

      componentConfigurator.addInstallAppCallback(() => {
        return AppCenterUtil.createOrUpdateApp(edge, websocket, this.homeAppId, this.homeAppAlias, homeAppProperties, AppCenterUtil.keyForIntegratedSystems());
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
      baseMode: baseMode,
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
      baseMode: baseMode,
    });

    // meter0
    const goodweMeter = {
      factoryId: 'GoodWe.Grid-Meter',
      componentId: 'meter0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.GRID_METER'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus1' },
        { name: 'modbusUnitId', value: 247 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode,
    };

    if (this.energyFlowMeter) {
      goodweMeter.properties.push({ name: 'goodWeMeterCategory', value: this.energyFlowMeter.meter });
      if (this.energyFlowMeter.meter === Meter.GridMeterCategory.COMMERCIAL_METER) {
        goodweMeter.properties.push({ name: 'externalMeterRatioValueA', value: this.energyFlowMeter.value });
      }
    }

    componentConfigurator.add(goodweMeter);

    // io0
    componentConfigurator.add({
      factoryId: this.relayFactoryId,
      componentId: 'io0',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.RELAY_BOARD'),
      properties: [
        { name: 'enabled', value: true },
        { name: 'modbus.id', value: 'modbus0' },
        { name: 'modbusUnitId', value: 2 },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode,
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
      baseMode: baseMode,
    });

    // batteryInverter0
    const goodweconfig = {
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
        {
          name: 'rcrEnable',
          value: feedInLimitation.feedInType === FeedInType.EXTERNAL_LIMITATION ? 'ENABLE' : 'DISABLE',
        },
      ],
      mode: ConfigurationMode.RemoveAndConfigure,
      baseMode: baseMode,
    };

    feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION
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

    this.addHomeDcConfiguration(componentConfigurator, baseMode);

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
      baseMode: baseMode,
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
      baseMode: baseMode,
    });

    // ctrlGridOptimizedCharge0
    const gridOptimizedCharge = {
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
      baseMode: baseMode,
    };

    feedInLimitation.feedInType == FeedInType.DYNAMIC_LIMITATION
      ? gridOptimizedCharge.properties.push(
        { name: 'maximumSellToGridPower', value: feedInLimitation.maximumFeedInPower },
        { name: "sellToGridLimitEnabled", value: true },
        { name: 'mode', value: 'AUTOMATIC' },
      )
      :
      gridOptimizedCharge.properties.push(
        { name: "sellToGridLimitEnabled", value: false },
        { name: 'mode', value: 'OFF' },
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
      baseMode: baseMode,
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
      baseMode: baseMode,
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
      baseMode: baseMode,
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
          value: this.emergencyReserve.value ?? 5, /* minimum allowed value */
        },
      ],
      mode: this.emergencyReserve.isEnabled ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly,
      baseMode: baseMode,
    });

    componentConfigurator.add({
      factoryId: 'Ess.Power',
      componentId: '_power',
      alias: this.translate.instant('INSTALLATION.CONFIGURATION_EXECUTE.POWER'),
      properties: [
        { name: 'enablePid', value: false },
      ],
      mode: ConfigurationMode.UpdateOnly,
      baseMode: baseMode,
    });

    return componentConfigurator;
  }

  public override setNonAbstractFields(ibnString: any) {

    // Configuration mppt selection
    if ('mppt' in ibnString) {
      this.mppt = ibnString.mppt;
    }

    // protocol pv
    if ('pv' in ibnString) {
      this.pv = ibnString.pv;
    }

    // energyFlowMeter for Home 20 & 30
    if ('energyFlowMeter' in ibnString) {
      this.energyFlowMeter = ibnString.energyFlowMeter;
    }
  }

  /**
   * Adds the specific DC configuration for the Home variant.
   *
   * @param componentConfigurator configuration object.
   * @param baseMode BaseMode
   */
  public addHomeDcConfiguration(componentConfigurator: ComponentConfigurator, baseMode: BaseMode) {

    // Chargers
    for (let i = 0; i < (this.maxNumberOfMppt === -1 ? this.maxNumberOfPvStrings : this.maxNumberOfMppt); i++) {
      const dc = this.pv.dc[i];
      let factoryId: string;
      let alias: string;

      // Home 20. Home 30 has more than 2 pv strings.
      if (this.maxNumberOfPvStrings > 2) {
        factoryId = 'GoodWe.Charger.Two-String';
        alias = "PV " + i;
      } else {
        factoryId = 'GoodWe.Charger-PV' + (i + 1);
        alias = "MPPT " + (i + 1);
      }

      if (dc.isSelected) {
        componentConfigurator.add({
          factoryId: factoryId,
          componentId: 'charger' + i,
          alias: dc.alias ?? alias,
          properties: [
            { name: 'enabled', value: true },
            { name: 'essOrBatteryInverter.id', value: 'batteryInverter0' },
            { name: 'modbus.id', value: 'modbus1' },
            { name: 'modbusUnitId', value: 247 },
          ],
          mode: dc.isSelected ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly,
          baseMode: baseMode,
        });
      }
    }
  }

  public override addCustomMeterData(): ComponentData[] {
    const meterData: ComponentData[] = [];
    const energyFlowMeter = this.energyFlowMeter;

    if (energyFlowMeter) {
      meterData.push({
        label: this.translate.instant('INSTALLATION.CONFIGURATION_ENERGY_FLOW_METER.METER.LABEL'),
        value: Meter.toGridMeterCategoryLabelString(energyFlowMeter.meter, this.translate),
      });
      if (energyFlowMeter.meter === Meter.GridMeterCategory.COMMERCIAL_METER) {
        meterData.push({
          label: this.translate.instant('INSTALLATION.CONFIGURATION_ENERGY_FLOW_METER.CONVERTER_RATIO'),
          value: energyFlowMeter.value.toString(),
        });
      }
    }

    return meterData;
  }

  /**
   * Retrieves the common properties for Home20 and Home30 configurations.
   *
   * @param safetyCountry - The safety country configuration.
   * @param feedInSetting - The feed-in setting configuration.
   * @returns An object containing the common properties for Home 20/30 application.
   */
  public getCommonPropertiesForHome2030(safetyCountry: SafetyCountry, feedInSetting: FeedInSetting): Home2030CommonApp {

    const home20_30CommonApp: Home2030CommonApp = {
      SAFETY_COUNTRY: safetyCountry,
      FEED_IN_TYPE: this.feedInLimitation.feedInType,
      ...(this.feedInLimitation.feedInType === FeedInType.DYNAMIC_LIMITATION && { MAX_FEED_IN_POWER: this.feedInLimitation.maximumFeedInPower }),
      FEED_IN_SETTING: feedInSetting,
      GRID_METER_CATEGORY: this.energyFlowMeter.meter,
      ...(this.energyFlowMeter.meter === Meter.GridMeterCategory.COMMERCIAL_METER && { CT_RATIO_FIRST: this.energyFlowMeter.value }),
      HAS_EMERGENCY_RESERVE: this.emergencyReserve.isEnabled,
      ...(this.emergencyReserve.isEnabled && { EMERGENCY_RESERVE_ENABLED: this.emergencyReserve.isReserveSocEnabled }),
      ...(this.emergencyReserve.isReserveSocEnabled && { EMERGENCY_RESERVE_SOC: this.emergencyReserve.value }),
      ...(this.batteryInverter?.shadowManagementDisabled && { SHADOW_MANAGEMENT_DISABLED: true }),
    };

    return home20_30CommonApp;
  }
}
