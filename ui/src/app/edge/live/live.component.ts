import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Edge, Service, Utils, Widgets, EdgeConfig, ChannelAddress } from '../../shared/shared';

@Component({
  selector: 'live',
  templateUrl: './live.component.html'
})
export class LiveComponent {

  items = ['item1', 'item2', 'item3', 'item4'];

  addItem(newItem: string) {
    this.items.push(newItem);
  }

  public edge: Edge = null
  public config: EdgeConfig = null;
  public widgets: Widgets = null;
  public currentitle = "Television";
  public gridChannels: ChannelAddress[] = [
    new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
    new ChannelAddress('_sum', 'GridSellActiveEnergy'),
  ]

  constructor(
    private route: ActivatedRoute,
    private service: Service,
    protected utils: Utils,
  ) {
  }
  ionViewWillEnter() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    });
    this.service.getConfig().then(config => {
      this.config = config;
      this.widgets = config.widgets;
    })
  }
}
