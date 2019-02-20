import { Component, OnInit, OnChanges, Input } from '@angular/core';
import { AbstractHistoryChart } from '../abstracthistorychart';
import { ChannelAddress, Edge, Service, Utils } from '../../../shared/shared';

@Component({
  selector: 'kwh',
  templateUrl: './kwh.component.html'
})
export class KwhComponent extends AbstractHistoryChart implements OnInit {

  @Input() private fromDate: Date;
  @Input() private toDate: Date;



  constructor(protected service: Service, ) {
    super(service);
  }

  ngOnInit() { }

  ngOnChanges() {
    this.updateValues();
  };

  updateValues() {
    this.querykWh(this.fromDate, this.toDate).then(response => {
      let result = response.result
      let values = [];

    });
  }

  protected getChannelAddresses(edge: Edge): Promise<ChannelAddress[]> {
    return new Promise((resolve, reject) => {
      if (edge.isVersionAtLeast('2018.8')) {
        resolve([new ChannelAddress('_sum', 'EssSoc')]);

      } else {
        // TODO: remove after full migration
        this.service.getConfig().then(config => {
          // get 'Soc'-Channel of all 'EssNatures'
          let channeladdresses = [];
          for (let componentId of config.getComponentsImplementingNature("EssNature")) {
            channeladdresses.push(new ChannelAddress(componentId, 'Soc'));
          }
          resolve(channeladdresses);
        }).catch(reason => reject(reason));
      }
    });
  }

}
