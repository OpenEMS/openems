import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Service, Websocket } from 'src/app/shared/shared';
import { InstallationData } from '../../installation.component';
import { ComponentConfigurator, ConfigurationMode, ConfigurationObject, ConfigurationStatus } from './component-configurator';
import { SafetyCountry } from './configuration-enums';

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
  public isConfigurationStarted: boolean;

  constructor(private service: Service, private websocket: Websocket) { }

  public ngOnInit() {

    this.componentConfigurator = new ComponentConfigurator(this.installationData.edge, this.installationData.config, this.websocket);

    //#region Add objects to component configurator

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

    // battery0
    this.componentConfigurator.add({
      factoryId: "Battery.Fenecon.Home",
      componentId: "battery0",
      alias: "Batterie",
      properties: [
        { name: "enabled", value: true },
        { name: "startStop", value: "START" },
        { name: "modbus.id", value: "modbus0" },
        { name: "modbusUnitId", value: 1 }
      ],
      mode: ConfigurationMode.RemoveAndConfigure
    });

    // batteryInverter0
    this.componentConfigurator.add({
      factoryId: "GoodWe.BatteryInverter",
      componentId: "batteryInverter0",
      alias: "Batterie-Wechselrichter",
      properties: [
        { name: "enabled", value: true },
        { name: "modbus.id", value: "modbus1" },
        { name: "modbusUnitId", value: 247 },
        { name: "safetyCountry", value: SafetyCountry.Germany },
        { name: "backupEnable", value: this.installationData.battery.emergencyReserve.isEnabled ? "ENABLE" : "DISABLE" },
        { name: "feedPowerEnable", value: "ENABLE" },
        { name: "feedPowerPara", value: this.installationData.batteryInverter.dynamicFeedInLimitation.maximumFeedInPower },
        { name: "setfeedInPowerSettings", value: this.installationData.batteryInverter.dynamicFeedInLimitation.feedInSetting },
        { name: "emsPowerMode", value: "UNDEFINED" },
        { name: "emsPowerSet", value: -1 },
        { name: "blockWrites", value: false }
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
        { name: "modbusUnitId", value: 1 }
      ],
      mode: ConfigurationMode.RemoveAndConfigure
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

    //#endregion

    this.configurationObjectsToBeConfigured = this.componentConfigurator.getConfigurationObjectsToBeConfigured();
    this.isAnyConfigurationObjectPreConfigured = this.componentConfigurator.anyHasStatus(ConfigurationStatus.PreConfigured);

    // Auto-start configuration when no components pre-configured
    if (!this.isAnyConfigurationObjectPreConfigured) {
      this.startConfiguration();
    }
  }

  public onPreviousClicked() {
    this.previousViewEvent.emit();
  }

  public onNextClicked() {

    // If all components have been configured,
    // the submit-event is triggered and no further
    // action is done
    if (this.componentConfigurator.allHaveStatus(ConfigurationStatus.Configured)) {
      this.nextViewEvent.emit();
      return;
    }

    // Start Configuration
    this.startConfiguration();
  }

  public startConfiguration() {
    this.isWaiting = true;
    this.isConfigurationStarted = true;

    // Starts the configuration
    this.componentConfigurator.start().then(() => {
      this.service.toast("Konfiguration erfolgreich.", "success");
    }).catch((reason) => {
      this.service.toast("Bei der Konfiguration ist ein Fehler aufgetreten. Versuchen Sie diese erneut zu starten.", "danger");
      console.log(reason);
    }).finally(() => {
      this.isWaiting = false;
    });
  }

}

