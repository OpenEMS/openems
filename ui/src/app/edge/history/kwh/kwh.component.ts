import { Component, OnInit, OnChanges, Input } from '@angular/core';
import { ChannelAddress, Edge, Service, Utils } from '../../../shared/shared';
import { Cummulated, QueryHistoricTimeseriesEnergyResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { QueryHistoricTimeseriesEnergyRequest } from '../../../shared/jsonrpc/request/queryHistoricTimeseriesEnergyRequest'
import { JsonrpcResponseError } from 'src/app/shared/jsonrpc/base';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'kwh',
  templateUrl: './kwh.component.html'
})
export class KwhComponent implements OnInit, OnChanges {

  @Input() private fromDate: Date;
  @Input() private toDate: Date;

  public data: Cummulated = null;
  public values: any;
  public edge: Edge = null;

  constructor(
    protected service: Service,
    private route: ActivatedRoute,
    public translate: TranslateService
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
  }

  ngOnChanges() {
    this.updateValues();
  };

  updateValues() {
    this.queryEnergy(this.fromDate, this.toDate).then(response => {
      this.data = response.result.data;
      console.log("Response:", this.data)
    });
  };

  protected getChannelAddresses(edge: Edge): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      resolve([
        new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
        new ChannelAddress('_sum', 'GridSell_ActiveEnergy'),
        new ChannelAddress('_sum', 'ProductionActiveEnergy'),
        new ChannelAddress('_sum', 'ConsumptionActiveEnergy')
      ]);
    });
  };


  protected queryEnergy(fromDate: Date, toDate: Date): Promise<QueryHistoricTimeseriesEnergyResponse> {
    return new Promise((resolve, reject) => {
      this.service.getCurrentEdge().then(edge => {
        this.getChannelAddresses(edge).then(channelAddresses => {
          let request = new QueryHistoricTimeseriesEnergyRequest(fromDate, toDate, channelAddresses);
          edge.sendRequest(this.service.websocket, request).then(response => {
            let result = (response as QueryHistoricTimeseriesEnergyResponse).result;
            if (Object.keys(result.data).length != 0) {
              resolve(response as QueryHistoricTimeseriesEnergyResponse);
            } else {
              reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
            }
          }).catch(reason => reject(reason));
        }).catch(reason => reject(reason));
      })
    })
  }

}
