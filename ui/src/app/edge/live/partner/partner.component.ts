// @ts-strict-ignore
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Edge, Service, Websocket } from '../../../shared/shared';

@Component({
  selector: PartnerComponent.SELECTOR,
  templateUrl: './partner.component.html',
})
export class PartnerComponent implements OnInit, OnDestroy {

  private static readonly SELECTOR = "partner";

  private edge: Edge = null;

  constructor(
    public service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    });
  }

  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, PartnerComponent.SELECTOR);
    }
  }
}
