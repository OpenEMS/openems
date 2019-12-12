import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { environment } from '../../../../environments';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';

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

  public components: { title: string, components: EdgeConfig.Component[] }[] = [];

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

      this.listComponents("Modbus-Verbindungen", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.bridge.modbus.api.BridgeModbus")
      ]);
      this.listComponents("Zähler", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
      ]);
      this.listComponents("Speichersysteme", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss"),
        config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
      ]);
      this.listComponents("Batterien", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.battery.api.Battery")
      ]);
      this.listComponents("I/Os", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.io.api.DigitalOutput"),
        config.getComponentsImplementingNature("io.openems.edge.io.api.DigitalInput")
      ]);
      this.listComponents("E-Auto Ladestationen", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs"),
      ]);
      this.listComponents("Standard-Controller", categorizedComponentIds, [
        config.getComponentsByFactory("Controller.Api.Backend"),
        config.getComponentsByFactory("Controller.Api.Rest"),
        config.getComponentsByFactory("Controller.Api.Websocket"),
        config.getComponentsByFactory("Controller.Debug.Log")
      ]);
      this.listComponents("Spezial-Controller", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.controller.api.Controller")
      ]);
      this.listComponents("Scheduler", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.scheduler.api.Scheduler")
      ]);
      this.listComponents("Sonstige", categorizedComponentIds, [
        config.getComponentsImplementingNature("io.openems.edge.common.component.OpenemsComponent")
      ]);
    })
  }

  private listComponents(category: string, categorizedComponentIds: string[], componentsArray: EdgeConfig.Component[][]) {
    let components =
      // create one flat array
      [].concat(...componentsArray)
        // remove Components from list that have already been listed before
        .filter(component => {
          return !categorizedComponentIds.includes(component.id);
        })
        // remove duplicates
        .filter((e, i, arr) => arr.indexOf(e) === i);
    if (components.length > 0) {
      components.forEach(component => {
        categorizedComponentIds.push(component.id);
      });
      this.components.push({ title: category, components: components });
    }
  }
}