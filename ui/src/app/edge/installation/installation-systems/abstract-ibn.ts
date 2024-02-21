import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { SetupProtocol } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { Country } from 'src/app/shared/type/country';
import { environment } from 'src/environments';
import { Category } from '../shared/category';
import { FeedInSetting, FeedInType, View, WebLinks } from '../shared/enums';
import { ComponentData, SerialNumberFormData } from '../shared/ibndatatypes';
import { Meter } from '../shared/meter';
import { FEED_IN_POWER_FACTOR_OPTIONS } from '../shared/options';
import { System, SystemId, SystemType } from '../shared/system';
import { ComponentConfigurator } from '../views/configuration-execute/component-configurator';
import { EmsApp } from '../views/heckert-app-installer/heckert-app-installer.component';

export type SerialNumberData = {
  formGroup: FormGroup;
  fieldSettings: FormlyFieldConfig[];
  model: any;
  header: string;
};

export type SchedulerId = {
  componentId: string,
  behaviour: SchedulerIdBehaviour
}

export enum SchedulerIdBehaviour {
  MANAGED_BY_APP_MANAGER,
  ALWAYS_INCLUDE
}

export abstract class AbstractIbn {
  // Battery type
  public readonly type: SystemType;

  // Id
  public readonly id: SystemId;

  // protocol-installer
  public installer?: {
    companyName: string;
    lastName: string;
    firstName: string;
    street: string;
    zip: string;
    city: string;
    country: Country;
    email: string;
    phone: string;
  };

  // protocol-customer
  public customer?: {
    isCorporateClient: boolean;
    companyName: string;
    lastName: string;
    firstName: string;
    street: string;
    zip: string;
    city: string;
    country: Country;
    email: string;
    emailConfirm: string;
    phone: string;
  };

  // protocol-system
  public location?: {
    isEqualToCustomerData: boolean;
    isCorporateClient: boolean;
    companyName: string;
    lastName: string;
    firstName: string;
    street: string;
    zip: string;
    city: string;
    country: Country;
    email?: string;
    phone?: string;
  };

  // configuration-line-side-meter-fuse
  public lineSideMeterFuse?: {
    category: Category;
    fixedValue?: number;
    otherValue?: number;
    meterType?: Meter;
  };

  // protocol-dynamic-feed-in-limitation
  public feedInLimitation?: {
    feedInType: FeedInType,
    maximumFeedInPower?: number;
  };

  // configuration-emergency-reserve
  public emergencyReserve?: {
    isEnabled: boolean;
    isReserveSocEnabled: boolean;
    minValue: number;
    value: number;
  };

  // Protocol Serial Numbers.
  public serialNumbers: {
    modules: ComponentData[];
  };

  // Configuration summary
  public gtcAndWarrantyLinks: {
    gtcLink: WebLinks;
    warrantyLink: WebLinks;
  };

  //Controller-Id's
  public requiredControllerIds: SchedulerId[];

  // Heckert-app-installer
  public selectedFreeApp?: EmsApp;

  // Configuration-summary
  public setupProtocol?: SetupProtocol;

  // Protocol-serial-numbers
  public setupProtocolId?: string;

  // Rundsteuerempfaenger manual in feed in limitation
  public readonly showRundSteuerManual: boolean;

  // Show view count along with Schritt number on top of page.
  public showViewCount: boolean;

  // Contains default number of battery modules per tower based on system.
  public readonly defaultNumberOfModules: number;

  // Label used for device check in configuration summary. It is different for Industrial systems.
  public readonly isDevicesActiveCheckedLabel: string = this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.DEVICE_ACTIVE_CHECKED');

  constructor(public views: View[], public translate: TranslateService) { }

  /**
   * Returns the number of towers and modules per tower.
   *
   * @param edge the current edge.
   * @param websocket the Websocket connection.
   */
  public abstract getPreSettingInformationFromEdge(edge: Edge, websocket: Websocket): Promise<{ numberOfTowers: number; numberOfModulesPerTower: number }>;

  /**
   * Returns the component fields for serial numbers based on system being installed.
   *
   * @param towerNr number of towers.
   * @param numberOfModulesPerTower number of modules per tower.
   */
  public abstract getSerialNumberFields(towerNr: number, numberOfModulesPerTower: number): FormlyFieldConfig[];

  /**
   * Returns the fields to enter number of towers and modules, manually.
   *
   * @param numberOfModulesPerTower number of modules per tower.
   * @param numberOfTowers number of towers.
   */
  public abstract getPreSettingsFields(numberOfModulesPerTower: number, numberOfTowers: number): FormlyFieldConfig[];

  /**
   * Fills the entire fields.
   *
   * @param numberOfTowers number of towers.
   * @param numberOfModulesPerTower number of modules per tower.
   * @param models form specific data.
   * @param forms Array of form data to display.
   */
  public abstract fillSerialNumberForms(
    numberOfTowers: number,
    numberOfModulesPerTower: number,
    models: any,
    forms: SerialNumberFormData[]): SerialNumberFormData[];

  /**
   * Retrieves the Serial numbers of the battery modules and components.
   *
   * @param towerNr number of towers.
   * @param edge the current edge.
   * @param websocket the Websocket connection.
   * @param numberOfModulesPerTower number of modules per tower.
   */
  public abstract getSerialNumbersFromEdge(towerNr: number, edge: Edge, websocket: Websocket, numberOfModulesPerTower?: number): Promise<Object>;

  /**
   * View Configuration Dynamic Feed-In limitation.
   * Returns the fields for the views based on the system.
   */
  public abstract getFeedInLimitFields(): FormlyFieldConfig[];

  /**
   * Returns the updated ibn after filling Dynamic-Feed-In-Limit fields from the model.
   *
   * @param model the model containing the user input for the Dynamic-Feed-In-Limit fields.
   */
  public abstract setFeedInLimitFields(model: any);

  /**
   * View Configuration-execute.
   * Returns the required configuration object with components specific to the system.
   *
   * @param edge the current edge.
   * @param config the EdgeConfig.
   * @param websocket the Websocket connection.
   * @param service the Service.
   */
  public abstract getComponentConfigurator(
    edge: Edge,
    config: EdgeConfig,
    websocket: Websocket,
    service?: Service
  ): ComponentConfigurator;

  /**
   * View Protocol-serial-numbers.
   * Returns the Protocol information of the system.
   *
   * @param edge the current edge.
   * @param websocket the Websocket connection.
   */
  public abstract getProtocol(edge: Edge, websocket: Websocket): Promise<string>;

  /**
   * Returns the set of controller for updateScheduler in component configurator.
   */
  public abstract setRequiredControllers();

  /**
   * Returns the fields that has system variants specific to the 'System Type' selected in previous step.
   */
  public abstract getSystemVariantFields(): FormlyFieldConfig[];

  /**
   * Returns the fields that has sub systems specific to the 'System Type' selected in previous step.
   */
  public getSubSystemFields(): FormlyFieldConfig[] {
    return [];
  };

  /**
   * View Emergency-reserve
   * Adds and returns the specific battery information based on Ibn to view in summary.
   *
   */
  public addCustomBatteryData(): ComponentData[] {
    return [];
  };

  /**
   * View Dynamic limitation
   * Adds and returns the specific Battery-Inverter information based on Ibn to view in summary.
   *
   */
  public addCustomBatteryInverterData(): ComponentData[] {
    return [];
  };

  /**
   * View Protocol pv Data
   * Adds and returns the PV information based on Ibn to view in summary.
   *
   */
  public addCustomPvData(): ComponentData[] {
    return [];
  };

  /**
   * Sets the Non abstract fields for the IBN object from session storage or from specific views.
   *
   * for eg: commercial 50 features, modbus bridge type from commercial systems and many more.
   *
   * @param model model information from the view.
   */
  public setNonAbstractFields(model: any) { }

  /**
   * Gets the additional Emergency reserve fields.
   *
   * eg: Coupler fields from Commercial 30 Netztrenstelle variant.
   *
   * @param fields fields for the componenet.
   * @returns The fields to be displayed.
   */
  public getAdditionalEmergencyReserveFields(fields: FormlyFieldConfig[]): FormlyFieldConfig[] {
    return fields;
  }

  /**
   * Adds the emergency reserve model to the IBN.
   *
   * @param model The model.
   */
  public setEmergencyReserve(model: any) {
    this.emergencyReserve = model;
  }

  /**
   * Returns the common fields for all systems in Feed in limitation view.
   *
   * @param totalPvPower The total pv power configured.
   * @returns common fields for feed in limits fields.
   */
  public getCommonFeedInLimitsFields(totalPvPower: number): FormlyFieldConfig[] {
    const fields: FormlyFieldConfig[] = [];

    fields.push({
      key: "feedInType",
      type: "select",
      className: "white-space-initial",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CHOOSE'),
        placeholder: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.PLACE_HOLDER'),
        options: [
          { label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.DYNAMIC_LIMITATION'), value: FeedInType.DYNAMIC_LIMITATION },
          { label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.EXTERNAL_LIMITATION'), value: FeedInType.EXTERNAL_LIMITATION },
          { label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.NO_LIMITATION'), value: FeedInType.NO_LIMITATION },
        ],
        required: true,
      },
    });

    fields.push({
      key: 'maximumFeedInPower',
      type: 'input',
      templateOptions: {
        type: 'number',
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.MAXIMUM_FEED_IN_VALUE'),
        description: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.MAXIMUM_VALUE_DESCRIPTION'),
        required: true,
        max: 29999, // max feed in power limit value.
      },
      parsers: [Number],
      defaultValue: totalPvPower,
      hideExpression: model => model.feedInType != FeedInType.DYNAMIC_LIMITATION,
    });

    return fields;
  }

  /**
   * Returns the additional fields for feed in limitation view.
   *
   * @param fields The common fields already existing.
   */
  public addAdditionalFeedInLimitsFields(fields: FormlyFieldConfig[]) {
    fields.push({
      key: 'feedInSetting',
      type: 'radio',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CHOOSE'),
        description: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.FEED_IN_SETTING_DESCRIPTION'),
        options: [
          { label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.QU_ENABLED_CURVE'), value: FeedInSetting.QuEnableCurve },
          { label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.PU_ENABLED_CURVE'), value: FeedInSetting.PuEnableCurve },
          { label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.FIXED_POWER_FACTOR'), value: FeedInSetting.FixedPowerFactor },
        ],
        required: true,
      },
    });

    fields.push({
      key: 'fixedPowerFactor',
      type: 'select',
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.CONSTANT_VALUE'),
        options: FEED_IN_POWER_FACTOR_OPTIONS(),
        required: true,
      },
      hideExpression: model => model.feedInSetting !== FeedInSetting.FixedPowerFactor,
    });

    fields.push({
      key: "isManualProperlyFollowedAndRead",
      type: "checkbox",
      templateOptions: {
        label: this.translate.instant('INSTALLATION.PROTOCOL_FEED_IN_MANAGEMENT.EXTERNAL_CONTROLLER_CHECK'),
        required: true,
      },
      hideExpression: model => model.feedInType != FeedInType.EXTERNAL_LIMITATION,
    });
  }

  /**
   * Retrieves the fields for View Line side meter Fuse,
   * which are different for Home and Commercial systems.
   */
  public getLineSideMeterFuseFields(): FormlyFieldConfig[] {
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
          { label: "100 A", value: 100 },
          { label: "120 A", value: 120 },
          { label: this.translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.OTHER'), value: -1 },
        ],
        required: true,
      },
      parsers: [Number],
    });

    fields.push({
      key: "otherValue",
      type: "input",
      templateOptions: {
        type: "number",
        label: this.translate.instant('INSTALLATION.CONFIGURATION_LINE_SIDE_METER_FUSE.OTHER_VALUE'),
        min: 0,
        required: true,
      },
      parsers: [Number],
      validators: {
        validation: ["onlyPositiveInteger"],
      },
      hideExpression: model => model.fixedValue !== -1,
    });
    return fields;
  }

  /**
   * Returns Setup Protocol with common objects for all systems.
   *
   * @param edge The current Edge object
   * @returns new SetupProtocol with objects added.
   */
  public getCommonProtocolItems(edge: Edge): SetupProtocol {
    const installer = this.installer;
    const customer = this.customer;
    const lineSideMeterFuse = this.lineSideMeterFuse;

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
      oem: environment.theme,
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

    protocol.items.push({
      category: Category.EMS_DETAILS,
      name: this.translate.instant('INSTALLATION.CONFIGURATION_SUMMARY.EDGE_NUMBER', { edgeShortName: environment.edgeShortName }),
      value: edge.id,
    });

    return protocol;
  }

  /**
   * Adds the serial numbers of the Battery modules and other components installed to the setup protocol given.
   *
   * @param protocol The SetupProtocol with already exisiting elements.
   * @param numberOfModulesPerTower The configured number of moduler per tower for the system.
   * @param subsequentStaticComponents The default number of fixed components per subsequent Towers/Strings.
   * @param categoryElement the label string for category.
   */
  public addProtocolSerialNumbers(protocol: SetupProtocol, numberOfModulesPerTower: number, subsequentStaticComponents: number, categoryElement: string) {
    const serialNumbers = this.serialNumbers;

    // Speichersystemkomponenten
    protocol.lots = [];

    // Initial tower has 3 static components other than modules such as Welcherischter, BMS and EMS box.
    const initialStaticTowerComponents = 3;

    // Number of towers/strings
    const numTowers = 4;

    // Total number of components each tower contains.
    const staticComponents: number = numberOfModulesPerTower + subsequentStaticComponents;
    const numberOfComponentsTower1: number = numberOfModulesPerTower + initialStaticTowerComponents;

    // Calculate the number of components in each tower
    const numComponentsPerTower: number[] = [0];
    for (let i = 1; i <= 4; i++) {
      numComponentsPerTower.push(numComponentsPerTower[i - 1] + (i === 1 ? numberOfComponentsTower1 : staticComponents));
    }

    // Tower/String is valid for both Home and Industrial. String alone is valid for Commercial.
    for (let componentCount = 0; componentCount < serialNumbers.modules.length; componentCount++) {
      const module = serialNumbers.modules[componentCount];
      if (module.value !== null && module.value !== '') {
        const name: string = this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.SINGLE_SERIAL_NUMBER', { label: module.label });
        const serialNumber: string = module.value;

        // Determine the tower/string number based on the componentCount
        for (let tower = 1; tower <= numTowers; tower++) {
          if (componentCount < numComponentsPerTower[tower]) {
            const category: string = tower === 1
              ? this.translate.instant('INSTALLATION.PROTOCOL_SERIAL_NUMBERS.BESS_COMPONENTS')
              : this.translate.instant(categoryElement, { number: tower });

            // Push the data to protocol.lots
            protocol.lots.push({
              category: category,
              name: name,
              serialNumber: serialNumber,
            });

            break;
          }
        }
      }
    }
  }

  /**
     * Loads the appropriate Ibn object.
     */
  public setIbn(system: any): AbstractIbn {
    return System.getSystemObjectFromSystemId(system, this.translate);
  }
}
