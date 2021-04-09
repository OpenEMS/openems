import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { Component, Input, ViewContainerRef } from '@angular/core';
import { FixDigitalOutputModalComponent } from './modal/modal.component';
import { ModalController } from '@ionic/angular';
import { UUID } from 'angular2-uuid';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { AbstractFlatWidgetComponent } from '../abstractFlatWidget.component';
import { CurrencyPipe } from '@angular/common';
import { CurrentData } from 'src/app/shared/edge/currentdata';


@Component({
  selector: 'fixdigitaloutput',
  templateUrl: './fixdigitaloutput.component.html'
})
export class FixDigitalOutputComponent extends AbstractFlatWidgetComponent {
  public selector = 'fixdigitaloutput';
  /** componentId needs to be set to get the components */
  @Input() protected componentId: string;

  public edge: Edge = null;
  public component: EdgeConfig.Component = null;
  public outputChannel: string;
  public state: string;
  public channelAddress: ChannelAddress[] | null = null;

  constructor(
    public translate: TranslateService,
    public service: Service,
    public route: ActivatedRoute,
    private modalController: ModalController,
    public viewContainerRef: ViewContainerRef,

  ) {
    super()
  }
  protected getComponentId(componentId: string) {
    return this.componentId = componentId
  }
  public subscribeOnChannels() {
    this.component = this.config.components[this.componentId];
    this.outputChannel = this.component.properties['outputChannelAddress'];
    return this.channelAddress.push(ChannelAddress.fromString(this.outputChannel))
  }
  protected onCurrentData(currentData: CurrentData) {
    console.log("channel", this.outputChannel)
    let channel = currentData.channel[this.outputChannel];
    if (channel != null) {
      if (channel == 1) {
        this.state = this.translate.instant('General.on');
      } else if (channel == 0) {
        this.state = this.translate.instant('General.off');
      } else {
        this.state = '-';
      }
    }
  }
  protected getChannelAddressess() {

  }
  // Subscribe to CurrentData
  // this.service.setCurrentComponent('', this.route).then(edge => {
  //   this.edge = edge;
  //   this.service.getConfig().then(config => {
  //     this.component = config.components[this.componentId];
  //     this.outputChannel = this.component.properties['outputChannelAddress']

  //     /** Subscribe on CurrentData to get the channel */
  //     this.edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {

  /** Proving state variable with following content setting */


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
