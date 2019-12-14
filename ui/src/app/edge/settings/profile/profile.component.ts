import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { environment } from '../../../../environments';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

@Component({
  selector: ProfileComponent.SELECTOR,
  templateUrl: './profile.component.html'
})
export class ProfileComponent {

  private static readonly SELECTOR = "profile";

  public env = environment;

  public edge: Edge = null;
  public config: EdgeConfig = null;
  public subscribedChannels: ChannelAddress[] = [];

  public components: DefaultTypes.ListComponent[] = [];

  constructor(
    private service: Service,
    private route: ActivatedRoute
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent("Anlagenprofil" /* TODO translate */, this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      this.config = config;
      let categorizedComponentIds: string[] = ["_componentManager", "_cycle", "_meta", "_power", "_sum"];

      this.service.listComponents("Modbus-Verbindungen", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.bridge.modbus.api.BridgeModbus")
      ], this.components);
      this.service.listComponents("Zähler", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
      ], this.components);
      this.service.listComponents("Speichersysteme", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss"),
        config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
      ], this.components);
      this.service.listComponents("Batterien", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.battery.api.Battery")
      ], this.components);
      this.service.listComponents("I/Os", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.io.api.DigitalOutput"),
        config.getComponentsImplementingNature("io.openems.edge.io.api.DigitalInput")
      ], this.components);
      this.service.listComponents("E-Auto Ladestationen", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs"),
      ], this.components);
      this.service.listComponents("Standard-Controller", categorizedComponentIds, [
        config.getComponentsByFactory("Controller.Api.Backend"),
        config.getComponentsByFactory("Controller.Api.Rest"),
        config.getComponentsByFactory("Controller.Api.Websocket"),
        config.getComponentsByFactory("Controller.Debug.Log")
      ], this.components);
      this.service.listComponents("Spezial-Controller", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.controller.api.Controller")
      ], this.components);
      this.service.listComponents("Scheduler", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.scheduler.api.Scheduler")
      ], this.components);
      this.service.listComponents("Sonstige", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.common.component.OpenemsComponent")
      ], this.components);
    })
  }
}