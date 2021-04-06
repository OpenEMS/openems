import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, Input, ViewContainerRef } from '@angular/core';
import { FixDigitalOutputModalComponent } from './modal/modal.component';
import { ModalController } from '@ionic/angular';
import { FlatWidgetLine } from '../flat/flat-widget-line/flatwidget-line';
import { UUID } from 'angular2-uuid';

@Component({
  selector: 'fixdigitaloutput',
  templateUrl: './fixdigitaloutput.component.html'
})
export class FixDigitalOutputComponent extends FlatWidgetLine {




  public selector = 'fixdigitaloutput';
  /** componentId needs to be set to get the components */
  @Input() private componentId: string;

  public edge: Edge = null;
  public component: EdgeConfig.Component = null;
  public outputChannel: string;
  public state: string;
  public channelAddress: ChannelAddress[] | null = null;
  public randomselector: string = UUID.UUID().toString();

  constructor(
    public service: Service,
    public websocket: Websocket,
    public route: ActivatedRoute,
    private modalController: ModalController,
    public viewContainerRef: ViewContainerRef,

  ) {
    super(route, service, viewContainerRef, websocket)
  }

  ngOnInit() {
    // Subscribe to CurrentData
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        this.component = config.components[this.componentId];
        this.outputChannel = this.component.properties['outputChannelAddress']

        this.subscribing(this.outputChannel, this.randomselector);


        /** Subscribe on CurrentData to get the channel */
        this.edge.currentData.subscribe(currentData => {

          /** Prooving state variable with following content setting */
          let channel = currentData.channel[this.outputChannel];
          if (channel == null) {
            this.state = '-';
          } else if (channel == 1) {
            this.state = 'General.on'
          } else if (channel == 0) {
            this.state = 'General.off'
          }
        });
      });
    });
  }

  ngOnDestroy() {
    this.unsubcribing(this.randomselector);
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
