import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { JsonrpcResponseError } from 'src/app/shared/jsonrpc/base';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { QueryHistoricTimeseriesEnergyRequest } from '../../../shared/jsonrpc/request/queryHistoricTimeseriesEnergyRequest';
import { Cumulated, QueryHistoricTimeseriesEnergyResponse } from '../../../shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { ChannelAddress, Edge, Service } from '../../../shared/shared';

@Component({
  selector: 'kwh',
  templateUrl: './kwh.component.html'
})
export class KwhComponent implements OnInit, OnChanges {

  @Input() public period: DefaultTypes.HistoryPeriod;

  public data: Cumulated = null;
  public values: any;
  public edge: Edge = null;

  constructor(
    protected service: Service,
    private route: ActivatedRoute,
    public translate: TranslateService
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(response => {
      this.edge = response;
    });
  }

  ngOnChanges() {
    this.updateValues();
  };

  updateValues() {
    this.queryEnergy(this.period.from, this.period.to).then(response => {
      this.data = response.result.data;
    });
  };


  /**
   * Gets the ChannelAdresses that should be queried.
   * 
   * @param edge the current Edge
   */
  protected getChannelAddresses(edge: Edge): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      resolve([
        new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
        new ChannelAddress('_sum', 'GridSellActiveEnergy'),
        new ChannelAddress('_sum', 'ProductionActiveEnergy'),
        new ChannelAddress('_sum', 'ConsumptionActiveEnergy'),
        new ChannelAddress('_sum', 'EssActiveChargeEnergy'),
        new ChannelAddress('_sum', 'EssActiveDischargeEnergy')
      ]);
    });
  };

  /**
   * Sends the Historic Timeseries Data Query and makes sure the result is not empty.
   * 
   * @param fromDate the From-Date
   * @param toDate   the To-Date
   * @param edge     the current Edge
   * @param ws       the websocket
   */
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

  /**
   * Returns the given date's unix-milliseconds value
   * 
   * @param date the date to format as unix-milliseconds
   */
  public toUnix(date: Date): number {
    return date.getTime();
  }

  /**
   * Returns a new date, which represents the beginning of the given date's day
   * 
   * @param date the date to process
   */
  public startOfDay(date: Date): Date {
    return new Date(date.getUTCFullYear(), date.getMonth(), date.getDate());
  }

  /**
   * Returns a new date, which represents the end of the given date's day/the 
   * beginning of the following day
   * 
   * @param date the date to process
   */
  public endOfDay(date: Date): Date {
    return new Date(date.getUTCFullYear(), date.getMonth(), date.getDate(), 24);
  }

}
