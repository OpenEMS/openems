import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Utils, Websocket, EdgeConfig } from '../../../../shared/shared';
import { IGNORE_NATURES } from '../shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit {

  private static readonly SELECTOR = "indexComponentInstall";

  public list: {
    readonly nature: EdgeConfig.Nature,
    isNatureClicked: Boolean,
    readonly allFactories: EdgeConfig.Factory[]
    filteredFactories: EdgeConfig.Factory[]
  }[] = [];
  public showAllFactories = false;
  public components: DefaultTypes.ListComponent[] = [];


  constructor(
    private route: ActivatedRoute,
    private service: Service,
    private translate: TranslateService
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent(this.translate.instant('Edge.Config.Index.AddComponents'), this.route);
    this.service.getConfig().then(config => {
      let categorizedComponentIds: string[] = ["_componentManager", "_cycle", "_meta", "_power", "_sum"];

      for (let natureId in config.natures) {

        if (natureId.includes('bridge.modbus.api')) {
          this.service.listComponents("Modbus-Verbindungen", categorizedComponentIds, [
            config.getComponentsImplementingNature(natureId),
          ], this.components);
        }
      }

      // this.service.listComponents("Zähler", categorizedComponentIds, [
      //   config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
      // ], this.components);
      // this.service.listComponents("Speichersysteme", categorizedComponentIds, [
      //   config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss"),
      //   config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
      // ], this.components);
      // this.service.listComponents("Batterien", categorizedComponentIds, [
      //   config.getComponentsImplementingNature("io.openems.edge.battery.api.Battery")
      // ], this.components);
      // this.service.listComponents("I/Os", categorizedComponentIds, [
      //   config.getComponentsImplementingNature("io.openems.edge.io.api.DigitalOutput"),
      //   config.getComponentsImplementingNature("io.openems.edge.io.api.DigitalInput")
      // ], this.components);
      // this.service.listComponents("E-Auto Ladestationen", categorizedComponentIds, [
      //   config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs"),
      // ], this.components);
      // this.service.listComponents("Standard-Controller", categorizedComponentIds, [
      //   config.getComponentsByFactory("Controller.Api.Backend"),
      //   config.getComponentsByFactory("Controller.Api.Rest"),
      //   config.getComponentsByFactory("Controller.Api.Websocket"),
      //   config.getComponentsByFactory("Controller.Debug.Log")
      // ], this.components);
      // this.service.listComponents("Spezial-Controller", categorizedComponentIds, [
      //   config.getComponentsImplementingNature("io.openems.edge.controller.api.Controller")
      // ], this.components);
      // this.service.listComponents("Scheduler", categorizedComponentIds, [
      //   config.getComponentsImplementingNature("io.openems.edge.scheduler.api.Scheduler")
      // ], this.components);
      // this.service.listComponents("Sonstige", categorizedComponentIds, [
      //   config.getComponentsImplementingNature("io.openems.edge.common.component.OpenemsComponent")
      // ], this.components);

      for (let natureId in config.natures) {
        if (IGNORE_NATURES.includes(natureId)) {
          continue;
        }

        let nature = config.natures[natureId];
        let factories = [];
        for (let factoryId of nature.factoryIds) {
          factories.push(config.factories[factoryId]);
        }
        this.list.push({
          nature: nature,
          isNatureClicked: false,
          allFactories: factories,
          filteredFactories: factories
        });
      }
      console.log("list", this.list)
      this.updateFilter("");
    });
  }

  updateFilter(completeFilter: string) {
    // take each space-separated string as an individual and-combined filter
    let filters = completeFilter.split(' ');
    let countFilteredFactories = 0;
    for (let entry of this.list) {
      entry.filteredFactories = entry.allFactories.filter(factory =>
        // Search for filter strings
        Utils.matchAll(filters, [
          factory.id.toLowerCase(),
          factory.name.toLowerCase(),
          factory.description.toLowerCase()]),
      );
      countFilteredFactories += entry.filteredFactories.length;
    }
    // If not more than 10 Factories survived filtering -> show all of them immediately
    if (countFilteredFactories > 10) {
      this.showAllFactories = false;
    } else {
      this.showAllFactories = true;
    }
  }
}