import { ActivatedRoute } from '@angular/router';
import { AutarchyModalComponent } from './modal/modal.component';
import { Component } from '@angular/core';
import { ChannelAddress, Edge, Service, Websocket } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { BehaviorSubject } from 'rxjs';
import { CurrentData } from 'src/app/shared/edge/currentdata';

@Component({
  selector: AutarchyComponent.SELECTOR,
  templateUrl: './autarchy.component.html'
})
export class AutarchyComponent {

  private static readonly SELECTOR = "autarchy";

  private edge: Edge = null;
  public autarchy: number | null;
  public currentData = CurrentData;
  public sum: number = this.currentData.summary;
  public channelAddresses: ChannelAddress[] = [];

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
    private websocket: Websocket,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route);
    console.log("??????????????????????????????????????????", this.currentData['_sum/GridActivePower']);
    // this.channelAddresses
    // this.edge.subscribeChannels(this.websocket, AutarchyComponent.SELECTOR, this.channelAddresses);

  }

  async presentModal() {
    const modal = await this.modalCtrl.create({
      component: AutarchyModalComponent,

    });
    return await modal.present();
  }

  //, ...inputChannels: ChannelAddress[]
  // insertSuggestion(currentData: BehaviorSubject<CurrentData>): string {
  //   console.log("##################################################################");
  //   //console.log("Inputchannels: ", inputChannels)
  //   console.log("currentData", currentData);
  //   console.log("currentData", currentData);
  //   console.log("currentData", currentData);
  //   console.log("currentData", currentData.value);
  //   // let component = "_sum";
  //   //let channel = "EssSoc";

  //   return "This ist the result";

  //currentData.subscribe();

  //console.log("a"), currentData['_value'];
  //console.log("a"), currentData['value'];
  //console.log("b", currentData);
  //console.log("c", currentData.value['channel']);
  //console.log("d", currentData.value.summary.system);
  //console.log("e", currentData[channel]);
  //console.log("f", currentData[channel]);
  //console.log("g", currentData[channel]);

  // console.log("test", currentData.channel[component + "/" + channel]);
  //console.log("test", currentData.value.channel[component + "/" + channel]);
  //console.log("autarkie", currentData['summary'].system.autarchy);
}
