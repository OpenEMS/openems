import { SetupProtocol } from 'src/app/shared/jsonrpc/request/submitSetupProtocolRequest';
import { Edge, EdgeConfig, Service, Websocket } from 'src/app/shared/shared';
import { ComponentData } from 'src/app/shared/type/componentData';

import { ComponentConfigurator } from '../views/configuration-execute/component-configurator';
import { EmsApp } from '../views/heckert-app-installer/heckert-app-installer.component';
import { AcPv } from '../views/protocol-additional-ac-producers/protocol-additional-ac-producers.component';
import { FeedInSetting, FeedInType } from '../views/protocol-feed-in-limitation/protocol-feed-in-limitation.component';
import { DcPv } from '../views/protocol-pv/protocol-pv.component';

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
  ConfigurationFeaturesStorageSystemComponent,
}

export abstract class Ibn {
  // Battery type
  public type: string;

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
    feedInSetting?: FeedInSetting;
    fixedPowerFactor?: FeedInSetting;
  };

  // protocol-pv
  public pv?: {
    dc1?: DcPv;
    dc2?: DcPv;
    ac?: AcPv[];
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

  constructor(public views: View[]) { }

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
}
