import { Component, OnInit, OnChanges, Input } from '@angular/core';
import { ChannelAddress, Edge, Service, Utils } from '../../../shared/shared';
import { Cummulated, QuerykWhResponse } from '../../../shared/jsonrpc/response/querykWhResponse';
import { JsonrpcResponseError } from 'src/app/shared/jsonrpc/base';
import { QuerykWhRequest } from 'src/app/shared/jsonrpc/request/querykWhRequest';
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

  constructor(
    protected service: Service,
    private route: ActivatedRoute,
    public translate: TranslateService
  ) { }

  ngOnInit() {
    this.service.setCurrentEdge(this.route);
    this.updateValues();
  }

  ngOnChanges() {
    this.updateValues();
  };

  updateValues() {
    this.querykWh(this.fromDate, this.toDate).then(response => {
      this.data = response.result.data;
      console.log("Response: ", this.data)
    });
  }

  protected getChannelAddresses(edge: Edge): Promise<ChannelAddress[]> {
    return new Promise((resolve) => {
      resolve([new ChannelAddress('_sum', 'EssSoc')]);
    });
  }


  protected querykWh(fromDate: Date, toDate: Date): Promise<QuerykWhResponse> {
    return new Promise((resolve, reject) => {
      this.service.getCurrentEdge().then(edge => {
        this.getChannelAddresses(edge).then(channelAddresses => {
          let request = new QuerykWhRequest(fromDate, toDate, channelAddresses);
          edge.sendRequest(this.service.websocket, request).then(response => {
            let result = (response as QuerykWhResponse).result;
            if (Object.keys(result.data).length != 0) {
              resolve(response as QuerykWhResponse);
            } else {
              reject(new JsonrpcResponseError(response.id, { code: 0, message: "Result was empty" }));
            }
          }).catch(reason => reject(reason));
        }).catch(reason => reject(reason));
      })
    })
  }

}
