// @ts-strict-ignore
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/components/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData } from 'src/app/shared/shared';

import { Controller_Io_FixDigitalOutputModalComponent } from './modal/modal.component';

@Component({
  selector: 'Controller_Io_FixDigitalOutput',
  templateUrl: './Io_FixDigitalOutput.html',
})
export class Controller_Io_FixDigitalOutputComponent extends AbstractFlatWidget {

  public state: string = '-';
  public outputChannel: string;

  async presentModal() {
    if (!this.isInitialized) {
      return;
    }
    const modal = await this.modalController.create({
      component: Controller_Io_FixDigitalOutputModalComponent,
      componentProps: {
        component: this.component,
        edge: this.edge,
      },
    });
    return await modal.present();
  }

  protected override getChannelAddresses(): ChannelAddress[] {
    this.outputChannel = this.component.properties['outputChannelAddress'];
    return [ChannelAddress.fromString(this.outputChannel)];
  }

  protected override onCurrentData(currentData: CurrentData) {
    const channel = currentData.allComponents[this.outputChannel];
    if (channel != null) {
      if (channel == 1) {
        this.state = this.translate.instant('General.on');
      } else if (channel == 0) {
        this.state = this.translate.instant('General.off');
      }
    }
  }

}
