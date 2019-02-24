import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Utils, Websocket, EdgeConfig } from '../../../shared/shared';

@Component({
  selector: IndexComponent.SELECTOR,
  templateUrl: './index.component.html'
})
export class IndexComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "indexComponentInstall";

  public list: {
    nature: EdgeConfig.Nature,
    factories: EdgeConfig.Factory[]
  }[] = [];

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
  ) {
  }

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
    this.service.getConfig().then(config => {
      for (let natureId in config.natures) {
        switch (natureId) {
          case "io.openems.edge.common.modbusslave.ModbusSlave":
          case "io.openems.edge.common.component.OpenemsComponent":
          case "io.openems.edge.bridge.modbus.api.BridgeModbusSerial":
          case "io.openems.edge.bridge.modbus.api.BridgeModbusTcp":
          case "io.openems.edge.common.jsonapi.JsonApi":
            // ignore Nature
            break;
          default:
            let nature = config.natures[natureId];
            let factories = [];
            for (let factoryId of nature.factoryIds) {
              factories.push(config.factories[factoryId]);
            }
            this.list.push({
              nature: nature,
              factories: factories
            });
        }
      }
    });
  }

  ngOnDestroy() {
  }
}