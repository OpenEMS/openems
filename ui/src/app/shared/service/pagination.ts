import { Directive } from '@angular/core';
import { Router } from '@angular/router';
import { SubscribeEdgesRequest } from '../jsonrpc/request/subscribeEdgesRequest';
import { ChannelAddress, Edge } from '../shared';
import { Service } from './service';

@Directive()
export class Pagination {

  private edge: Edge | null = null;

  constructor(
    public service: Service,
    private router: Router,
  ) { }

  getAndSubscribeEdge(edge: Edge): Promise<void> {
    return new Promise<void>((resolve) => {

      this.service.updateCurrentEdge(edge.id).then((edge) => {
        this.edge = edge;
        this.service.websocket.sendRequest(new SubscribeEdgesRequest({ edges: [edge.id] }));
      }).then(() => {
        this.edge.subscribeChannels(this.service.websocket, '', [
          new ChannelAddress('_sum', 'State'),
        ]);
      })
        .finally(resolve)
        .catch(() => {
          this.router.navigate(['index']);
        });
    });
  }
}
