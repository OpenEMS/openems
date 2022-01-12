import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { GetNetworkConfigRequest } from 'src/app/edge/settings/network/getNetworkConfigRequest';
import { GetNetworkConfigResponse } from 'src/app/edge/settings/network/getNetworkConfigResponse';
import { SetNetworkConfigRequest } from 'src/app/edge/settings/network/setNetworkConfigRequest';
import { NetworkInterface } from 'src/app/edge/settings/network/shared';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { Service, Websocket } from 'src/app/shared/shared';
import { environment } from 'src/environments';
import { InstallationData } from '../../installation.component';
import { EmsAppId } from '../heckert-app-installer/heckert-app-installer.component';
import { FeedInSetting } from '../protocol-dynamic-feed-in-limitation/protocol-dynamic-feed-in-limitation.component';
import { ComponentConfigurator, ConfigurationMode, ConfigurationObject, ConfigurationState, FunctionState } from './component-configurator';
import { SafetyCountry } from './safety-country';

export interface Interface {
  name: string,
  model: NetworkInterface,
};

@Component({
  selector: ConfigurationExecuteComponent.SELECTOR,
  templateUrl: './configuration-execute.component.html'
})
export class ConfigurationExecuteComponent implements OnInit {

  private static readonly SELECTOR = "configuration-execute";

  @Input() public installationData: InstallationData;

  @Output() public previousViewEvent: EventEmitter<any> = new EventEmitter();
  @Output() public nextViewEvent: EventEmitter<any> = new EventEmitter();

  public isWaiting: boolean = false;

  public componentConfigurator: ComponentConfigurator;

  public configurationObjectsToBeConfigured: ConfigurationObject[];
  public isAnyConfigurationObjectPreConfigured: boolean;

  constructor(private service: Service, private websocket: Websocket) { }

  public ngOnInit() {
    let stopOnRequest: Subject<void> = new Subject<void>();

    this.installationData.edge.getConfig(this.websocket).pipe(
      takeUntil(stopOnRequest),
      filter(config => config !== null)
    ).subscribe((config) => {
      stopOnRequest.next();
      stopOnRequest.complete();

      this.componentConfigurator = new ComponentConfigurator(this.installationData.edge, config, this.websocket);

      //#region Add objects to component configurator

      // Change EssPower's enablePid to false
      if (this.installationData.edge != null) {
        this.installationData.edge.updateComponentConfig(this.websocket, '_power', [
          { name: "enablePid", value: false },
        ]).then(() => {
        }).catch(reason => {
          this.service.toast('Changing PID failed' + '\n' + reason.error.message, 'danger');
          console.warn(reason);
        });
      }

      // modbus1
      this.componentConfigurator.add({
        factoryId: "Bridge.Modbus.Serial",
        componentId: "modbus1",
        alias: "Kommunikation mit dem Batterie-Wechselrichter",
        properties: [
          { name: "enabled", value: true },
          { name: "portName", value: "/dev/busUSB2" },
          { name: "baudRate", value: 9600 },
          { name: "databits", value: 8 },
          { name: "stopbits", value: "ONE" },
          { name: "parity", value: "NONE" },
          { name: "logVerbosity", value: "NONE" },
          { name: "invalidateElementsAfterReadErrors", value: 1 }
        ],
        mode: ConfigurationMode.RemoveAndConfigure
      });

      // modbus0
      this.componentConfigurator.add({
        factoryId: "Bridge.Modbus.Serial",
        componentId: "modbus0",
        alias: "Kommunikation mit der Batterie",
        properties: [
          { name: "enabled", value: true },
          { name: "portName", value: "/dev/busUSB1" },
          { name: "baudRate", value: 19200 },
          { name: "databits", value: 8 },
          { name: "stopbits", value: "ONE" },
          { name: "parity", value: "NONE" },
          { name: "logVerbosity", value: "NONE" },
          { name: "invalidateElementsAfterReadErrors", value: 1 }
        ],
        mode: ConfigurationMode.RemoveAndConfigure
      });

      // meter0
      this.componentConfigurator.add({
        factoryId: "GoodWe.Grid-Meter",
        componentId: "meter0",
        alias: "Netzzähler",
        properties: [
          { name: "enabled", value: true },
          { name: "modbus.id", value: "modbus1" },
          { name: "modbusUnitId", value: 247 }
        ],
        mode: ConfigurationMode.RemoveAndConfigure
      });

      // io0
      this.componentConfigurator.add({
        factoryId: "IO.KMtronic.4Port",
        componentId: "io0",
        alias: "Relaisboard",
        properties: [
          { name: "enabled", value: true },
          { name: "modbus.id", value: "modbus0" },
          { name: "modbusUnitId", value: 2 }
        ],
        mode: ConfigurationMode.RemoveAndConfigure
      });

      // battery0
      this.componentConfigurator.add({
        factoryId: "Battery.Fenecon.Home",
        componentId: "battery0",
        alias: "Batterie",
        properties: [
          { name: "enabled", value: true },
          { name: "startStop", value: "AUTO" },
          { name: "modbus.id", value: "modbus0" },
          { name: "modbusUnitId", value: 1 }
        ],
        mode: ConfigurationMode.RemoveAndConfigure
      });


      // Determine safety country
      let safetyCountry: SafetyCountry;

      if (this.installationData.location.isEqualToCustomerData) {
        safetyCountry = SafetyCountry.getSafetyCountry(this.installationData.customer.country);
      } else {
        safetyCountry = SafetyCountry.getSafetyCountry(this.installationData.location.country);
      }

      // Determine feed-in-setting
      let feedInSetting: FeedInSetting;

      if (this.installationData.dynamicFeedInLimitation.feedInSetting === FeedInSetting.FixedPowerFactor) {
        feedInSetting = this.installationData.dynamicFeedInLimitation.fixedPowerFactor;
      } else {
        feedInSetting = this.installationData.dynamicFeedInLimitation.feedInSetting
      }

      // batteryInverter0
      this.componentConfigurator.add({
        factoryId: "GoodWe.BatteryInverter",
        componentId: "batteryInverter0",
        alias: "Batterie-Wechselrichter",
        properties: [
          { name: "enabled", value: true },
          { name: "modbus.id", value: "modbus1" },
          { name: "modbusUnitId", value: 247 },
          { name: "safetyCountry", value: safetyCountry },
          { name: "backupEnable", value: this.installationData.battery.emergencyReserve.isEnabled ? "ENABLE" : "DISABLE" },
          { name: "feedPowerEnable", value: "ENABLE" },
          { name: "feedPowerPara", value: this.installationData.dynamicFeedInLimitation.maximumFeedInPower },
          { name: "setfeedInPowerSettings", value: feedInSetting },
          { name: "emsPowerMode", value: "UNDEFINED" },
          { name: "emsPowerSet", value: -1 },
        ],
        mode: ConfigurationMode.RemoveAndConfigure
      });

      // meter1
      let acArray = this.installationData.pv.ac;
      let isAcCreated: boolean = acArray.length >= 1;

      // TODO if more than 1 meter should be created, this logic must be changed
      let acAlias = isAcCreated ? acArray[0].alias : "";
      let acModbusUnitId = isAcCreated ? acArray[0].modbusCommunicationAddress : 0;

      this.componentConfigurator.add({
        factoryId: "Meter.Socomec.Threephase",
        componentId: "meter1",
        alias: acAlias,
        properties: [
          { name: "enabled", value: true },
          { name: "type", value: "PRODUCTION" },
          { name: "modbus.id", value: "modbus1" },
          { name: "modbusUnitId", value: acModbusUnitId },
          { name: "invert", value: false },

        ],
        mode: isAcCreated ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
      });

      // charger0
      this.componentConfigurator.add({
        factoryId: "GoodWe.Charger-PV1",
        componentId: "charger0",
        alias: this.installationData.pv.dc1.alias,
        properties: [
          { name: "enabled", value: true },
          { name: "essOrBatteryInverter.id", value: "batteryInverter0" },
          { name: "modbus.id", value: "modbus1" },
          { name: "modbusUnitId", value: 247 }
        ],
        mode: this.installationData.pv.dc1.isSelected ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
      });

      // charger1
      this.componentConfigurator.add({
        factoryId: "GoodWe.Charger-PV2",
        componentId: "charger1",
        alias: this.installationData.pv.dc2.alias,
        properties: [
          { name: "enabled", value: true },
          { name: "essOrBatteryInverter.id", value: "batteryInverter0" },
          { name: "modbus.id", value: "modbus1" },
          { name: "modbusUnitId", value: 247 }
        ],
        mode: this.installationData.pv.dc2.isSelected ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
      });

      // ess0
      this.componentConfigurator.add({
        factoryId: "Ess.Generic.ManagedSymmetric",
        componentId: "ess0",
        alias: "Speichersystem",
        properties: [
          { name: "enabled", value: true },
          { name: "startStop", value: "START" },
          { name: "batteryInverter.id", value: "batteryInverter0" },
          { name: "battery.id", value: "battery0" }
        ],
        mode: ConfigurationMode.RemoveAndConfigure
      });

      // predictor0
      this.componentConfigurator.add({
        factoryId: "Predictor.PersistenceModel",
        componentId: "predictor0",
        alias: "Prognose",
        properties: [
          { name: "enabled", value: true },
          {
            name: "channelAddresses", value: [
              "_sum/ProductionActivePower",
              "_sum/ConsumptionActivePower"
            ]
          },
        ],
        mode: ConfigurationMode.RemoveAndConfigure
      });

      // ctrlGridOptimizedCharge0
      this.componentConfigurator.add({
        factoryId: "Controller.Ess.GridOptimizedCharge",
        componentId: "ctrlGridOptimizedCharge0",
        alias: "Netzdienliche Beladung",
        properties: [
          { name: "enabled", value: true },
          { name: "ess.id", value: "ess0" },
          { name: "meter.id", value: "meter0" },
          { name: "sellToGridLimitEnabled", value: true },
          { name: "maximumSellToGridPower", value: this.installationData.dynamicFeedInLimitation.maximumFeedInPower },
          { name: "delayChargeRiskLevel", value: "MEDIUM" },
          { name: "mode", value: "AUTOMATIC" },
          { name: "manualTargetTime", value: "17:00" },
          { name: "debugMode", value: false },
          { name: "sellToGridLimitRampPercentage", value: 2 }

        ],
        mode: ConfigurationMode.RemoveAndConfigure
      });

      // ctrlEssSurplusFeedToGrid0
      this.componentConfigurator.add({
        factoryId: "Controller.Ess.Hybrid.Surplus-Feed-To-Grid",
        componentId: "ctrlEssSurplusFeedToGrid0",
        alias: "Überschusseinspeisung",
        properties: [
          { name: "enabled", value: true },
          { name: "ess.id", value: "ess0" }
        ],
        mode: ConfigurationMode.RemoveAndConfigure
      });

      // ctrlBalancing0
      this.componentConfigurator.add({
        factoryId: "Controller.Symmetric.Balancing",
        componentId: "ctrlBalancing0",
        alias: "Eigenverbrauchsoptimierung",
        properties: [
          { name: "enabled", value: true },
          { name: "ess.id", value: "ess0" },
          { name: "meter.id", value: "meter0" },
          { name: "targetGridSetpoint", value: 0 }
        ],
        mode: ConfigurationMode.RemoveAndConfigure
      });

      let emergencyReserve = this.installationData.battery.emergencyReserve;
      if (emergencyReserve != undefined) {
        this.componentConfigurator.add({
          factoryId: "GoodWe.EmergencyPowerMeter",
          componentId: "meter2",
          alias: "Notstromverbraucher",
          properties: [
            { name: "enabled", value: true },
            { name: "modbus.id", value: "modbus1" },
            { name: "modbusUnitId", value: 247 }
          ],
          mode: emergencyReserve.isEnabled ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
        });

        if (emergencyReserve.isEnabled) {
          // TODO shouldn't this controller always be active? otherwise 'mode' below has no sense.
          this.componentConfigurator.add({
            factoryId: "Controller.Ess.EmergencyCapacityReserve",
            componentId: "ctrlEmergencyCapacityReserve0",
            alias: "Ansteuerung der Notstromreserve",
            properties: [
              { name: "enabled", value: true },
              { name: "ess.id", value: "ess0" },
              { name: "mode", value: emergencyReserve.isReserveSocEnabled },
              { name: "reserveSoc", value: emergencyReserve.value }
            ],
            mode: ConfigurationMode.RemoveAndConfigure
          });
        }
      }

      //#endregion

      //#region [Heckert] Add objects for the free app to component configurator

      if (environment.theme === "Heckert") {
        let freeAppId: EmsAppId = this.installationData.selectedFreeApp.id;
        let isAppEvcs: boolean = [
          EmsAppId.HardyBarthSingle,
          EmsAppId.HardyBarthDouble,
          EmsAppId.Keba
        ].includes(freeAppId);

        // Add ip address to network configuration if EVCS gets configured
        if (isAppEvcs) {
          if (!this.addIpAddress("eth0", "192.168.25.10/24")) {
            this.service.toast("Eine für die Ladestation notwendige IP-Adresse konnte nicht zur Netzwerkkonfiguration hinzugefügt werden.", "danger");
          }
        }

        // Add components depending on the selected app
        this.componentConfigurator.add({
          factoryId: "Evcs.HardyBarth",
          componentId: "evcs0",
          alias: "Ladestation",
          properties: [
            { name: "enabled", value: true },
            { name: "debugMode", value: false },
            { name: "ip", value: "192.168.25.30" },
            { name: "minHwCurrent", value: 6000 },
            { name: "maxHwCurrent", value: 32000 }
          ],
          mode: freeAppId === EmsAppId.HardyBarthSingle ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
        });

        this.componentConfigurator.add({
          factoryId: "Evcs.HardyBarth",
          componentId: "evcs0",
          alias: "Ladestation 1",
          properties: [
            { name: "enabled", value: true },
            { name: "debugMode", value: false },
            { name: "ip", value: "192.168.25.30" },
            { name: "minHwCurrent", value: 6000 },
            { name: "maxHwCurrent", value: 16000 }
          ],
          mode: freeAppId === EmsAppId.HardyBarthDouble ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
        });

        this.componentConfigurator.add({
          factoryId: "Evcs.HardyBarth",
          componentId: "evcs1",
          alias: "Ladestation 2",
          properties: [
            { name: "enabled", value: true },
            { name: "debugMode", value: false },
            { name: "ip", value: "192.168.25.31" },
            { name: "minHwCurrent", value: 6000 },
            { name: "maxHwCurrent", value: 16000 }
          ],
          mode: freeAppId === EmsAppId.HardyBarthDouble ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
        });

        this.componentConfigurator.add({
          factoryId: "Evcs.Keba.KeContact",
          componentId: "evcs0",
          alias: "Ladestation",
          properties: [
            { name: "enabled", value: true },
            { name: "debugMode", value: false },
            { name: "ip", value: "192.168.25.11" },
            { name: "minHwCurrent", value: 6000 }
          ],
          mode: freeAppId === EmsAppId.Keba ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
        });

        this.componentConfigurator.add({
          factoryId: "Controller.IO.HeatingElement",
          componentId: "ctrlIoHeatingElement0",
          alias: "Heizstab",
          properties: [
            { name: "enabled", value: true },
            { name: "mode", value: "AUTOMATIC" },
            { name: "outputChannelPhaseL1", value: "io0/Relay1" },
            { name: "outputChannelPhaseL2", value: "io0/Relay2" },
            { name: "outputChannelPhaseL3", value: "io0/Relay3" },
            { name: "defaultLevel", value: "LEVEL_1" },
            { name: "endTime", value: "17:00" },
            { name: "workMode", value: "TIME" },
            { name: "minTime", value: 1 },
            { name: "powerPerPhase", value: 2000 },
            { name: "minimumSwitchingTime", value: 60 }
          ],
          mode: freeAppId === EmsAppId.HeatingElement ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
        });

        this.componentConfigurator.add({
          factoryId: "Controller.Io.HeatPump.SgReady",
          componentId: "ctrlIoHeatPump0",
          alias: "Wärmepumpe",
          properties: [
            { name: "enabled", value: true },
            { name: "debugMode", value: false },
            { name: "outputChannel1", value: "io0/Relay1" },
            { name: "outputChannel2", value: "io0/Relay2" },
            { name: "mode", value: "AUTOMATIC" },
            { name: "manualState", value: "REGULAR" },
            { name: "automaticRecommendationCtrlEnabled", value: true },
            { name: "automaticRecommendationSurplusPower", value: 3000 },
            { name: "automaticForceOnCtrlEnabled", value: true },
            { name: "automaticForceOnSurplusPower", value: 5000 },
            { name: "automaticForceOnSoc", value: 10 },
            { name: "automaticLockCtrlEnabled", value: false },
            { name: "automaticLockGridBuyPower", value: 5000 },
            { name: "automaticLockSoc", value: 20 },
            { name: "minimumSwitchingTime", value: 60 }
          ],
          mode: freeAppId === EmsAppId.HeatPump ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
        });

        // Add EVCS-Controller if selected app is an EVCS
        this.componentConfigurator.add({
          factoryId: "Controller.Evcs",
          componentId: "ctrlEvcs0",
          alias: "Ansteuerung der Ladestation" + (freeAppId === EmsAppId.HardyBarthDouble ? " 1" : ""),
          properties: [
            { name: "enabled", value: true },
            { name: "debugMode", value: false },
            { name: "evcs.id", value: "evcs0" },
            { name: "enabledCharging", value: true },
            { name: "chargeMode", value: "FORCE_CHARGE" },
            { name: "forceChargeMinPower", value: 7360 },
            { name: "defaultChargeMinPower", value: 0 },
            { name: "priority", value: "CAR" },
            { name: "ess.id", value: "ess0" },
            { name: "energySessionLimit", value: 0 }
          ],
          mode: isAppEvcs ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
        });

        // Add second EVCS-Controller for HardyBarthDouble if selected
        this.componentConfigurator.add({
          factoryId: "Controller.Evcs",
          componentId: "ctrlEvcs1",
          alias: "Ansteuerung der Ladestation 2",
          properties: [
            { name: "enabled", value: true },
            { name: "debugMode", value: false },
            { name: "evcs.id", value: "evcs1" },
            { name: "enabledCharging", value: true },
            { name: "chargeMode", value: "FORCE_CHARGE" },
            { name: "forceChargeMinPower", value: 7360 },
            { name: "defaultChargeMinPower", value: 0 },
            { name: "priority", value: "CAR" },
            { name: "ess.id", value: "ess0" },
            { name: "energySessionLimit", value: 0 }
          ],
          mode: freeAppId === EmsAppId.HardyBarthDouble ? ConfigurationMode.RemoveAndConfigure : ConfigurationMode.RemoveOnly
        });
      }

      //#endregion

      this.configurationObjectsToBeConfigured = this.componentConfigurator.getConfigurationObjectsToBeConfigured();
      this.isAnyConfigurationObjectPreConfigured = this.componentConfigurator.anyHasConfigurationState(ConfigurationState.PreConfigured);

      // Auto-start configuration when no components pre-configured
      if (this.isAnyConfigurationObjectPreConfigured) {
        this.service.toast("Es wurden eine bestehende Konfiguration gefunden. Sie können diese überschreiben, indem Sie den Konfigurationsvorgang manuell starten.", "warning");
      } else {
        this.startConfiguration();
      }
    });
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {
    this.nextViewEvent.emit();
  }

  /**
   * Adds an ip address to the given interface.
   * Returns false if an error occurs.
   * 
   * @param interfaceName Interface default "eth0"
   * @param ip Ip that should be added
   */
  public addIpAddress(interfaceName: string, ip: string): boolean {
    let iface: Interface;

    this.installationData.edge.sendRequest(
      this.websocket,
      new ComponentJsonApiRequest({ componentId: "_host", payload: new GetNetworkConfigRequest() })
    ).then((response) => {
      let result = (response as GetNetworkConfigResponse).result;

      // Get interface
      for (let name of Object.keys(result.interfaces)) {
        if (name === interfaceName) {
          iface = { name: name, model: result.interfaces[name] };
        }
      }

      // No interface with given name found
      if (!iface) {
        console.log("Network interface with name ''" + interfaceName + "'' was not found.");
        return false;
      }

      // Unset Gateway and DNS if DHCP is activated
      if (iface.model.dhcp) {
        iface.model.gateway = null;
        iface.model.dns = null;
      }

      // Set the ip in the model of the interface
      // or return if it already exists
      if (iface.model.addresses === null) {
        iface.model.addresses = new Array(ip);
      } else {
        if (iface.model.addresses.includes(ip)) {
          return true;
        }

        iface.model.addresses.push(ip);
      }

      // Unset Gateway and DNS if DHCP is activated
      if (iface.model.dhcp) {
        iface.model.gateway = null;
        iface.model.dns = null;
      }

      let params = {
        interfaces: {}
      };
      params.interfaces[iface.name] = iface.model;

      this.installationData.edge.sendRequest(
        this.websocket,
        new ComponentJsonApiRequest({ componentId: "_host", payload: new SetNetworkConfigRequest(params) })
      ).then(() => {
        return true;
      }).catch((reason) => {
        console.log(reason);
      })
    }).catch(reason => {
      console.log(reason);
    })
    return false;
  }

  public startConfiguration() {
    this.isWaiting = true;

    // Starts the configuration
    this.componentConfigurator.start().then(() => {
      this.service.toast("Konfiguration erfolgreich.", "success");
    }).catch((reason) => {
      console.log(reason);

      if (!this.componentConfigurator.allHaveConfigurationState(ConfigurationState.Configured)) {
        this.service.toast("Es konnten nicht alle Komponenten richtig konfiguriert werden.", "danger");
        return;
      }

      if (!this.componentConfigurator.allHaveFunctionState(FunctionState.Ok)) {
        this.service.toast("Funktionstest mit Fehlern abgeschlossen.", "warning");
        return;
      }
    }).finally(() => {
      this.isWaiting = false;
    });
  }
}