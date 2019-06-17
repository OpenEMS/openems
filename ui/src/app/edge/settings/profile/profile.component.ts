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

      this.listComponents("Zähler", config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter"), categorizedComponentIds);
      this.listComponents("Speichersysteme", config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss"), categorizedComponentIds);
      this.listComponents("Controller", config.getComponentsImplementingNature("io.openems.edge.controller.api.Controller"), categorizedComponentIds);
      this.listComponents("Scheduler", config.getComponentsImplementingNature("io.openems.edge.scheduler.api.Scheduler"), categorizedComponentIds);

      this.addRemainingComponents(config, categorizedComponentIds);
    })
  }

  private listComponents(category: string, components: EdgeConfig.Component[], categorizedComponentIds: string[]) {
    if (components.length > 0) {
      this.components.push({ title: category, components: components });
      for (let component of components) {
        categorizedComponentIds.push(component.id);
      }
    }
  }

  private addRemainingComponents(config: EdgeConfig, categorizedComponentIds: string[]) {
    let result: EdgeConfig.Component[] = [];
    for (let key of Object.keys(config.components)) {
      if (!categorizedComponentIds.includes(key)) {
        result.push(config.components[key]);
      }
    }
    this.components.push({ title: "Sonstige", components: result });
  }

}