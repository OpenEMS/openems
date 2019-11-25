import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service, Websocket, ChannelAddress } from 'src/app/shared/shared';

@Component({
  selector: CorrentlyComponent.SELECTOR,
  templateUrl: './corrently.component.html'
})
export class CorrentlyComponent {

  private static readonly SELECTOR = "corrently";

  public edge: Edge = null;

  constructor(
    public service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
  ) { }

  ngOnInit() {
    let channels = [];
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      channels.push(
        new ChannelAddress('corrently0', 'BestHourEpochtime'),
        new ChannelAddress('corrently0', 'BestHourGsi'),
      )
      this.edge.subscribeChannels(this.websocket, CorrentlyComponent.SELECTOR, channels);
    });
  }

  public getEpochTime() {
    return new Date(this.edge.currentData['_value'].channel['corrently0/BestHourEpochtime'] * 1000).toLocaleString('de-DE', { weekday: 'long', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' }).toString() + ' Uhr';
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, CorrentlyComponent.SELECTOR);
    }
  }
}
