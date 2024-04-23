// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { SetChannelValueRequest } from 'src/app/shared/jsonrpc/request/setChannelValueRequest';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';

@Component({
  selector: ServiceAssistentComponent.SELECTOR,
  templateUrl: './index.component.html',
})
export class ServiceAssistentComponent implements OnInit, OnDestroy {

  //private static readonly SELECTOR = "serviceSoltaro";
  private static readonly SELECTOR = "serviceassistent";

  public edge: Edge = null;
  public config: EdgeConfig = null;
  public subscribedChannels: ChannelAddress[] = [];
  public importantWriteChannelsDescriptions: ChannelDescription[] = [
    { channelName: "StopParameterCellUnderVoltageProtection", register: "0x2046", description: "Untere Zuschaltspannung(Protection)", requiredInputs: "" },
    { channelName: "StopParameterCellUnderVoltageRecover", register: "0x2047", description: "Untere Zuschaltspannung(Recover)", requiredInputs: "" },
    { channelName: "StopParameterCellOverVoltageProtection", register: "0x2040", description: "Obere Abschaltspannung(Protection)", requiredInputs: "" },
    { channelName: "StopParameterCellOverVoltageRecover", register: "0x2041", description: "Obere Abschaltspannung(Recover)", requiredInputs: "" },
    { channelName: "StopParameterCellVoltageDifferenceProtection", register: "0x2058", description: "Maximale Zellspannungsdifferenz(Protection)", requiredInputs: "" },
    { channelName: "StopParameterCellVoltageDifferenceProtectionRecover", register: "0x2059", description: "Maximale Zellspannungsdifferenz(Recover)", requiredInputs: "" },
    { channelName: "AutoSetSlavesId", register: "0x2014", description: "Neuadressierung aller Zellspannungssensoren", requiredInputs: "1" },
    { channelName: "AutoSetSlavesTemperatureId", register: "0x2019", description: "Neuadressierung aller Zelltemperatursensoren", requiredInputs: "1" },
    { channelName: "SystemReset", register: "0x2004", description: "BMS neustart", requiredInputs: "1" },
    { channelName: "VoltageLowProtection", register: "0x201E", description: "Abschaltspannung der Batterie", requiredInputs: "" },
    { channelName: "SetSoc", register: "0x20DF", description: "Soc", requiredInputs: "0 - 100" },
    { channelName: "WorkParameterNumberOfModules", register: "0x20C1", description: "Anzahl der Module", requiredInputs: "Anzahl" },
    { channelName: "BmsContactorControl", register: "0x2010", description: "Precharge-Control", requiredInputs: "1" },
    { channelName: "SetEmsAddress", register: "0x201B", description: "Aktuell nur lesbar. Zukünftig Unit-ID setzen (Vorsicht: BMS danach nicht mehr erreichbar - In BMS Komponente ändern)", requiredInputs: "" },
    { channelName: "WarnParameterInsulationAlarm", register: "0x2096", description: "Isolationsüberwachung deaktivieren (Warnung Lvl 2)", requiredInputs: "0" },
    { channelName: "WarnParameterInsulationAlarmRecover", register: "0x2097", description: "Isolationsüberwachung deaktivieren (Warnung Lvl 1)", requiredInputs: "0" },
    { channelName: "StopParameterInsulationProtection", register: "0x2056", description: "Isolationsüberwachung deaktivieren (Stop Lvl 2)", requiredInputs: "0" },
    { channelName: "StopParameterInsulationProtectionRecover", register: "0x2057", description: "Isolationsüberwachung deaktivieren (Stop Lvl 1)", requiredInputs: "0" },
    { channelName: "EmsCommunicationTimeout", register: "0x201C", description: "Watchdog (EMS timeout protection) in sekunden", requiredInputs: "90" },
  ];

  public batteries: EdgeConfig.Component[];

  constructor(
    private service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
  ) { }

  public customAlertOptions: any = {
    cssClass: 'wide-alert',
  };

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;

      this.service.getConfig().then(config => {
        this.config = config;
        this.batteries = config.getComponentsImplementingNature("io.openems.edge.battery.api.Battery");

        const channelAddresses = [];
        this.batteries.forEach(battery => {
          for (const channel in config.components[battery.id].channels) {
            channelAddresses.push(new ChannelAddress(battery.id, channel));
          }
        });
        this.edge.subscribeChannels(this.websocket, ServiceAssistentComponent.SELECTOR, channelAddresses);
      });
    });
  }

  getChannelAddress(componentId: string, channel: string): ChannelAddress {
    return new ChannelAddress(componentId, channel);
  }

  subscribeChannel(componentId: string, channelId: string) {
    this.subscribedChannels.forEach((item, index) => {
      if (item.componentId === componentId && item.channelId === channelId) {
        // had already been in the list
        return;
      }
    });

    const address = new ChannelAddress(componentId, channelId);
    this.subscribedChannels.push(address);

    if (this.config) {
      const channelConfig = this.config.getChannel(address);
      if (channelConfig) {
        if (channelConfig.accessMode == "WO") {
          // do not subscribe Write-Only Channels
          return;
        }
      }
    }

    if (this.edge) {
      this.edge.subscribeChannels(this.websocket, ServiceAssistentComponent.SELECTOR, this.subscribedChannels);
    }
  }

  unsubscribeChannel(address: ChannelAddress) {
    this.subscribedChannels.forEach((item, index) => {
      if (item.componentId === address.componentId && item.channelId === address.channelId) {
        this.subscribedChannels.splice(index, 1);
      }
    });
  }

  setChannelValue(address: ChannelAddress, value: any) {
    if (this.edge) {
      this.edge.sendRequest(
        this.service.websocket,
        new SetChannelValueRequest({
          componentId: address.componentId,
          channelId: address.channelId,
          value: value,
        }),
      ).then(response => {
        this.service.toast("Successfully set " + address.toString() + " to [" + value + "]", "success");
      }).catch(reason => {
        this.service.toast("Error setting " + address.toString() + " to [" + value + "]", 'danger');
      });
    }
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, ServiceAssistentComponent.SELECTOR);
    }
  }

}

export type ChannelDescription = {
  channelName: string,
  register: string,
  description: string,
  requiredInputs: string
}
