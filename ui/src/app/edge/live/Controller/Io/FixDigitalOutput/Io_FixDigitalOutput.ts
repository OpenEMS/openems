import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData } from 'src/app/shared/shared';
import { ModalComponent } from './modal/modal.component';


@Component({
  selector: 'Controller_Io_FixDigitalOutput',
  templateUrl: './Io_FixDigitalOutput.html'
})
export class Controller_Io_FixDigitalOutputComponent extends AbstractFlatWidget {

  public state: string = '-';
  public outputChannel: string;

  protected override getChannelAddresses(): ChannelAddress[] {
    this.outputChannel = this.component.properties['outputChannelAddress'];
    return [ChannelAddress.fromString(this.outputChannel)];
  }

  protected override onCurrentData(currentData: CurrentData) {
    let channel = currentData.allComponents[this.outputChannel];
    if (channel != null) {
      if (channel == 1) {
        this.state = this.translate.instant('General.on');
      } else if (channel == 0) {
        this.state = this.translate.instant('General.off');
      }
    }
  }

  async presentModal() {
    if (!this.isInitialized) {
      return;
    }
    const modal = await this.modalController.create({
      component: ModalComponent,
      componentProps: {
        component: this.component,
        edge: this.edge
      }
    });
    return await modal.present();
  }
}
