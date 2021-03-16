import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { ModalController } from '@ionic/angular';
import { AbstractWidget } from '../abstractWidget';
import { FixDigitalOutputModalComponent } from '../fixdigitaloutput/modal/modal.component';

@Component({
  selector: FlatWidgetComponent.SELECTOR,
  templateUrl: './flatwidget.component.html'
})
export class FlatWidgetComponent extends AbstractWidget {

  /** SELECTOR defines, how to call this Widget */
  static SELECTOR: string = 'flat-widget';

  /** Title in Header */
  @Input() public title: string;

  /** Image in Header */
  @Input() public img: string;

  /** Icon in Header */
  @Input() public icon: Icon = null;

  /** BackgroundColor of the Header (light or dark) */
  @Input() public color: string;

  /** ChannelAdresses sends all the channels from the Widget to FlatWidget */
  @Input() public channelAdresses: ChannelAddress[];

  /** Selector sends the Widget's selector to FlatWidget */
  @Input() public selector: string;

  /** Title_Type specifies if there is a title to translate */
  @Input() public title_type: string;

  public edge: Edge = null;
  public component: EdgeConfig.Component = null;

  constructor(
    /** Constructor responsible for initializing the variables, that are needed for subscribe */
    public service: Service,
    websocket: Websocket,
    route: ActivatedRoute,
    private modalController: ModalController
  ) {
    /** super() calls the Parentclass AbstractWidget and sends him service websocket and route */
    super(service, websocket, route);

  }

  ngOnInit() {

    this.edge = this.edge;

    /** SubscribeOnChannels() sends collected selector`s and channelAdresses to AbstractWidget */
    this.subscribeOnChannels(this.selector, this.channelAdresses);
  }
  ngOnDestroy() {
    if (this.edge != null) {
    }
  }
  async presentModal() {
    const modal = await this.modalController.create({
      component: FixDigitalOutputModalComponent,
      componentProps: {
        component: this.component,
        edge: this.edge
      }
    });
    return await modal.present();
  }
}

export type Icon = {
  color: string;
  size: string;
  name: string;
}
