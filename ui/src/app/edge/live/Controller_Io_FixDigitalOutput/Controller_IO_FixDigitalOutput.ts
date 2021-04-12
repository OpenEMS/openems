import { Component } from '@angular/core';
import { ChannelAddress } from 'src/app/shared/shared';
import { AbstractFlatWidget } from '../flat/abstract-flat-widget';
import { FixDigitalOutputModalComponent } from './modal/modal.component';


@Component({
  selector: 'Controller_Io_FixDigitalOutput',
  templateUrl: './Controller_Io_FixDigitalOutput.html'
})
export class FixDigitalOutputComponent extends AbstractFlatWidget {

  public state: string;
  public outputChannel: string;

  protected getChannelAddresses() {
    this.outputChannel = this.component.properties['outputChannelAddress']
    let channelAddresses: ChannelAddress[] = [ChannelAddress.fromString(this.outputChannel)]
    return channelAddresses
  }

  protected onCurrentData(data: { [channelId: string]: any }, allComponents: { [channelAddress: string]: any }) {
    let channel = allComponents[this.outputChannel];
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

  async presentModal() {
    if (!this.isInitialized) {
      return;
    }
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
