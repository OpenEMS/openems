import { Component } from '@angular/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { AbstractFlatWidget } from '../abstract-flat-widget';


@Component({
  selector: 'fixdigitaloutput',
  templateUrl: './fixdigitaloutput.component.html'
})
export class FixDigitalOutputComponent extends AbstractFlatWidget {

  protected getComponentId(componentId: string) {
    return this.componentId = componentId
  }

  protected onCurrentData(currentData: CurrentData) {
  }

  // protected onCurrentData(currentData: CurrentData) {
  //   console.log("channel", this.outputChannel)
  //   let channel = currentData.channel[this.outputChannel];
  //   if (channel != null) {
  //     if (channel == 1) {
  //       this.state = this.translate.instant('General.on');
  //     } else if (channel == 0) {
  //       this.state = this.translate.instant('General.off');
  //     } else {
  //       this.state = '-';
  //     }
  //   }
  // }
  // Subscribe to CurrentData
  // this.service.setCurrentComponent('', this.route).then(edge => {
  //   this.edge = edge;
  //   this.service.getConfig().then(config => {
  //     this.component = config.components[this.componentId];
  //     this.outputChannel = this.component.properties['outputChannelAddress']

  //     /** Subscribe on CurrentData to get the channel */
  //     this.edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {

  /** Proving state variable with following content setting */


  // async presentModal() {
  //   const modal = await this.modalController.create({
  //     component: FixDigitalOutputModalComponent,
  //     componentProps: {
  //       component: this.component,
  //       edge: this.edge
  //     }
  //   });
  //   return await modal.present();
  // }
}
