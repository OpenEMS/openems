import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { SetupProtocol } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { ComponentData, SerialNumberFormData } from 'src/app/shared/type/componentData';
import { FeedInType } from 'src/app/shared/type/feedinsettings';
import { ComponentConfigurator } from '../views/configuration-execute/component-configurator';
import { EmsApp } from '../views/heckert-app-installer/heckert-app-installer.component';
import { AcPv } from '../views/protocol-additional-ac-producers/protocol-additional-ac-producers.component';

export enum View {
  Completion,
  ConfigurationEmergencyReserve,
  ConfigurationExecute,
  ConfigurationLineSideMeterFuse,
  ConfigurationSummary,
  ConfigurationSystem,
  PreInstallation,
  ProtocolAdditionalAcProducers,
  ProtocolCustomer,
  ProtocolFeedInLimitation,
  ProtocolInstaller,
  ProtocolPv,
  ProtocolSerialNumbers,
  ProtocolSystem,
  HeckertAppInstaller,
  ConfigurationFeaturesStorageSystem,
  ConfigurationCommercialComponent
}

export type SerialNumberData = {
  formGroup: FormGroup;
  fieldSettings: FormlyFieldConfig[];
  model: any;
  header: string;
};

export abstract class AbstractIbn {
  // Battery type
  public readonly type: string;

  // Id
  public readonly id: string;

  // protocol-installer
  public installer?: {
    companyName: string;
    lastName: string;
    firstName: string;
    street: string;
    zip: string;
    city: string;
    country: string;
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
    country: string;
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
    country: string;
    email?: string;
    phone?: string;
  };

  // configuration-line-side-meter-fuse
  public lineSideMeterFuse?: {
    fixedValue: number;
    otherValue: number;
  };

  // protocol-dynamic-feed-in-limitation
  public feedInLimitation?: {
    feedInType: FeedInType,
    maximumFeedInPower?: number;
  };

  // protocol-pv
  public pv?: {
    ac?: AcPv[];
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

  //Controller-Id's
  public requiredControllerIds: string[];

  // Heckert-app-installer
  public selectedFreeApp?: EmsApp;

  // Configuration-summary
  public setupProtocol?: SetupProtocol;

  // Protocol-serial-numbers
  public setupProtocolId?: string;

  // Image for system to be configured in Pre installation
  public readonly imageUrl: string;

  // Url for system information
  public readonly manualLink: string;

  // Title for line side meter fuse view.
  public readonly lineSideMeterFuseTitle: string;

  // Rundsteuerempfaenger manual in feed in limitation
  public readonly showRundSteuerManual: boolean;

  // Show view count along with Schritt number on top of page.
  public showViewCount: boolean;

  constructor(public views: View[]) { }

  /**
   * Retrieves the fields for View Line side meter Fuse,
   * which are different for Home and Commercial systems.
   */
  public abstract getLineSideMeterFuseFields(): FormlyFieldConfig[];

  /**
   * Returns the number of towers and modules per tower.
   *
   * @param edge the current edge.
   * @param websocket the Websocket connection.
   */
  public abstract getSettings(edge: Edge, websocket: Websocket): Promise<{ numberOfTowers: number; numberOfModulesPerTower: number }>;

  /**
   * Returns the component fields for serial numbers based on system being installed.
   *
   * @param towerNr number of towers.
   * @param numberOfModulesPerTower number of modules per tower.
   */
  public abstract getFields(towerNr: number, numberOfModulesPerTower: number): FormlyFieldConfig[];

  /**
   * Returns the fields to enter number of towers and modules, manually.
   *
   * @param numberOfModulesPerTower number of modules per tower.
   * @param numberOfTowers number of towers.
   */
  public abstract getSettingsFields(numberOfModulesPerTower: number, numberOfTowers: number): FormlyFieldConfig[];

  /**
   * Fills the entire fields.
   *
   * @param numberOfTowers number of towers.
   * @param numberOfModulesPerTower number of modules per tower.
   * @param models form specific data.
   * @param forms Array of form data to display.
   */
  public abstract fillForms(
    numberOfTowers: number,
    numberOfModulesPerTower: number,
    models: any,
    forms: Array<SerialNumberFormData>): Array<SerialNumberFormData>;

  /**
   * Retrives the Serial numbers of the battery modules and components.
   *
   * @param towerNr number of towers.
   * @param edge the current edge.
   * @param websocket the Websocket connection.
   * @param numberOfModulesPerTower number of modules per tower.
   */
  public abstract getSerialNumbers(towerNr: number, edge: Edge, websocket: Websocket, numberOfModulesPerTower?: number): Promise<Object>;

  /**
   * View Configuration Dynamic Feed-In limitation.
   * Returns the fields for the views based on the system.
   */
  public abstract getFeedInLimitFields(): FormlyFieldConfig[];

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
   * View Emergency-reserve
   * Adds and returns the specific battery information based on Ibn to view in summary.
   *
   * @param batteryData the battery data.
   */
  public abstract addCustomBatteryData(batteryData: ComponentData[]): ComponentData[];

  /**
   * View Dynamic limitation
   * Adds and returns the specific Battery-Inverter information based on Ibn to view in summary.
   *
   * @param batteryInverterData the battery inverter data.
   */
  public abstract addCustomBatteryInverterData(batteryInverterData: ComponentData[]): ComponentData[];

  /**
   * View Protocol pv Data
   * Adds and returns the PV information based on Ibn to view in summary.
   *
   * @param pvData the photovoltaic data.
   */
  public abstract addCustomPvData(pvData: ComponentData[]): ComponentData[];

  /**
   * Returns the set of controller for updateScheduler in component configurator.
   */
  public abstract setRequiredControllers();

  /**
   * Returns the updated ibn after filling Dynamic-Feed-In-Limit fields from the model.
   * 
   * @param ibn The IBN.
   * @param model the model containing the user input for the Dynamic-Feed-In-Limit fields.
   */
  public abstract setFeedInLimitsFields(model: any);
}
