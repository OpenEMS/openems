// @ts-strict-ignore
import { Component } from '@angular/core';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { ChannelAddress, CurrentData, Utils } from 'src/app/shared/shared';
import { Icon } from 'src/app/shared/type/widget';
import { Controller_Io_ChannelSingleThresholdModalComponent } from './modal/modal.component';

@Component({
  selector: 'Controller_Io_ChannelSingleThresholdComponent',
  templateUrl: './Io_ChannelSingleThreshold.html',
})
export class Controller_Io_ChannelSingleThresholdComponent extends AbstractFlatWidget {

  public inputChannel: ChannelAddress;
  public outputChannel: ChannelAddress;
  public state: string;
  public mode: string;
  public modeValue: string;
  public icon: Icon = {
    name: '',
    color: '',
    size: '',
  };
  public dependendOn: string;
  public dependendOnValue: any;
  public isOtherInputAddress: boolean;
  public unitOfInputChannel: string | null = null;
  public outputChannelValue: number | null = null;
  public switchState: string;
  public switchValue: number | string;
  public switchConverter = Utils.CONVERT_WATT_TO_KILOWATT;

  protected override afterIsInitialized(): void {
    this.inputChannel = ChannelAddress.fromString(
      this.component.properties['inputChannelAddress']);

    this.edge.getChannel(this.websocket, this.inputChannel)
      .then(channel => {
        this.unitOfInputChannel = channel.unit;
      });
  }

  protected override getChannelAddresses() {
    const outputChannelAddress: string | string[] = this.component.properties['outputChannelAddress'];
    if (typeof outputChannelAddress === 'string') {
      this.outputChannel = ChannelAddress.fromString(outputChannelAddress);
    } else {
      // Takes only the first output for simplicity reasons
      this.outputChannel = ChannelAddress.fromString(outputChannelAddress[0]);
    }
    return [
      this.outputChannel,
      this.inputChannel,
      ChannelAddress.fromString(this.component.id + '/_PropertyMode')];
  }

  protected override onCurrentData(currentData: CurrentData) {

    this.switchValue = this.component.properties['threshold'];

    // Icon, State
    this.outputChannelValue = currentData.allComponents[this.outputChannel.toString()];
    switch (this.outputChannelValue) {
      case 0:
        this.icon.name = 'radio-button-off-outline';
        this.state = this.translate.instant('General.off');
        break;
      case 1:
        this.icon.name = 'aperture-outline';
        this.state = this.translate.instant('General.on');
        break;
    }

    // Mode
    this.modeValue = currentData.allComponents[this.component.id + '/_PropertyMode'];
    switch (this.modeValue) {
      case 'ON':
        this.mode = this.translate.instant('General.on');
        break;
      case 'OFF':
        this.mode = this.translate.instant('General.off');
        break;
      case 'AUTOMATIC':
        this.mode = this.translate.instant('General.automatic');
    }


    // 'AUTOMATIC'-Mode dependend on
    this.dependendOnValue = currentData.allComponents[this.inputChannel.toString()];

    // Set dependendOn Value for different inputChannel && Set the switchConverter and switchValue
    switch (this.inputChannel.toString()) {
      case '_sum/EssSoc':
        this.dependendOn = this.translate.instant('General.soc');
        this.switchConverter = Utils.CONVERT_TO_PERCENT;
        break;
      case '_sum/ProductionActivePower':
        this.dependendOn = this.translate.instant('General.production');
        break;
      case '_sum/GridActivePower':
        if (this.component.properties.threshold < 0) {
          if (this.outputChannelValue == 0) {
            this.switchValue = this.component.properties['threshold'] * -1;
          } else if (this.outputChannelValue == 1) {
            this.switchValue = this.component.properties['threshold'] * -1 - this.component.properties['switchedLoadPower'];
          }

          this.dependendOn = this.translate.instant('General.gridSell');
        } else if (this.component.properties.threshold > 0) {
          if (this.outputChannelValue == 1) {
            this.switchValue = this.component.properties['threshold'] - this.component.properties['switchedLoadPower'];
          }

          this.dependendOn = this.translate.instant('General.gridBuy');
        }
        break;
      default:
        if (this.component.properties.threshold < 0) {
          this.switchValue = Utils.multiplySafely(this.component.properties['threshold'], -1)
            + this.unitOfInputChannel !== '' ? this.unitOfInputChannel : '';
        } else if (this.component.properties.threshold > 0) {
          this.switchValue += this.unitOfInputChannel !== '' ? this.unitOfInputChannel : '';
        }

        this.dependendOn = this.translate.instant('Edge.Index.Widgets.Singlethreshold.other')
          + ' (' + this.component.properties.inputChannelAddress + ')';
        break;
    }

    // True when InputAddress doesnt match any of the following channelIds
    this.isOtherInputAddress = this.inputChannel.toString() != (null && '_sum/EssSoc' && '_sum/GridActivePower' && '_sum/ProductionActivePower') ? false : true;

    // Switch ON / OFF && BELOW / ABOVE
    // Threshold greater 0
    if (this.component.properties.threshold > 0) {

      // Check if invert is false
      if (!this.component.properties.invert) {
        if (this.outputChannelValue == 0) {
          this.switchState = this.translate.instant('Edge.Index.Widgets.Singlethreshold.switchOnAbove');
        } else if (this.outputChannelValue == 1) {
          this.switchState = this.translate.instant('Edge.Index.Widgets.Singlethreshold.switchOffBelow');
        }

        // Check if invert is true
      } else if (this.component.properties.invert) {
        if (this.outputChannelValue == 0) {
          this.switchState = this.translate.instant('Edge.Index.Widgets.Singlethreshold.switchOnBelow');
        } else if (this.outputChannelValue == 1) {
          this.switchState = this.translate.instant('Edge.Index.Widgets.Singlethreshold.switchOffAbove');
        }
      }

      // Threshold smaller 0
    } else if (this.component.properties.threshold < 0) {
      // Check if invert is false
      if (!this.component.properties.invert) {
        if (this.outputChannelValue == 0) {
          this.switchState = this.translate.instant('Edge.Index.Widgets.Singlethreshold.switchOnBelow');
        } else if (this.outputChannelValue == 1) {
          this.switchState = this.translate.instant('Edge.Index.Widgets.Singlethreshold.switchOffAbove');
        }

        // Check if invert is true
      } else if (this.component.properties.invert) {
        if (this.outputChannelValue == 0) {
          this.switchState = this.translate.instant('Edge.Index.Widgets.Singlethreshold.switchOnAbove');
        } else if (this.outputChannelValue == 1) {
          this.switchState = this.translate.instant('Edge.Index.Widgets.Singlethreshold.switchOffBelow');
        }
      }
    }

  }

  async presentModal() {
    const modal = await this.modalController.create({
      component: Controller_Io_ChannelSingleThresholdModalComponent,
      componentProps: {
        component: this.component,
        config: this.config,
        edge: this.edge,
        outputChannel: this.outputChannel,
        inputChannel: this.inputChannel,
        inputChannelUnit: this.unitOfInputChannel,
      },
    });
    return await modal.present();
  }
}

