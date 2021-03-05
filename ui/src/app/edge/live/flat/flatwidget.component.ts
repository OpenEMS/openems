import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { ModalController } from '@ionic/angular';

@Component({
  selector: FlatWidgetComponent.SELECTOR,
  templateUrl: './flatwidget.component.html'
})
export class FlatWidgetComponent {

  // @Output() newItemEvent = new EventEmitter<string>();

  // addNewItem(value: string) {
  //   this.newItemEvent.emit(value);
  // }

  @Input() public channels: [] = null;
  // @Input() public componentId: string;
  @Input() public essComponents: EdgeConfig.Component[] = null;
  @Input() public title: string;
  @Input() public icon: string;
  @Input() public color: string;
  @Input() public channelAdresses: ChannelAddress[] = [];
  // @Input() public firstparam: string;
  // @Input() public secondparam: string;


  // @Input() public howtochart: string;
  @Input() public ChannelAddress: string;
  @Input() public outputChannel: string = null;

  @Output() public chart: string;
  public edge: Edge = null;
  public component: EdgeConfig.Component = null;
  static SELECTOR: string = 'flat-widget';

  constructor(
    public service: Service,
    private websocket: Websocket,
    private route: ActivatedRoute,
    private modalController: ModalController
  ) { }

  ngOnInit() {
    console.log("SIND DIE ESS COMPS DDA?", this.essComponents)
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;

      edge.subscribeChannels(this.websocket, FlatWidgetComponent.SELECTOR, this.channelAdresses);
      console.log("give me channeladresses", this.channelAdresses)
    })
  }




  ngOnDestroy() {
    if (this.edge != null) {
      this.edge.unsubscribeChannels(this.websocket, FlatWidgetComponent.SELECTOR);
    }
  }

  // if (this.howtochart == 1) {
  //   console.log(this.category);
  //   this.someHTMLCode = '<percentagebar [value]="currentData.summary.system.totalPower"></percentagebar>'
  // } else {
  //   console.log("tut es nicht")
  // }
  // this.someHTMLCode = "";
  // console.log("test", this.category)
  // });

  // this.service.setCurrentComponent('', this.route).then(edge => {
  //   this.edge = edge;
  //   this.service.getConfig().then(config => {
  //     this.component = config.components[this.componentId];
  //     this.outputChannel = this.component.properties['outputChannelAddress']
  //     edge.subscribeChannels(this.websocket, FlatWidgetComponent.SELECTOR + this.componentId, [
  //       ChannelAddress.fromString(this.outputChannel)
  //     ]);
  //   });
  // });




  //   async presentModal() {
  //     const modal = await this.modalController.create({
  //       component: FixDigitalOutputModalComponent,
  //       componentProps: {
  //         component: this.component,
  //         edge: this.edge
  //       }
  //     });
  //     return await modal.present();
  //   }

}
// }