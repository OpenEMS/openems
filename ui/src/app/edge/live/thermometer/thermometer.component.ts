import { Component } from '@angular/core';
import { ChannelAddress, CurrentData, EdgeConfig, Utils } from 'src/app/shared/shared';
import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
import { formatNumber } from '@angular/common';

@Component({
  selector: 'Thermometer',
  templateUrl: './thermometer.component.html'
})
export class ThermometerComponent extends AbstractFlatWidget {

  public sensors: EdgeConfig.Component[] = null;
  public readonly CONVERT_TO_Degree = Utils.CONVERT_TO_Degree;

  protected override getChannelAddresses() {

    let channelAddresses: ChannelAddress[] = [];

    // Get Thermometer Components
    this.sensors = this.config.getComponentsImplementingNature('io.openems.edge.thermometer.api.Thermometer')
      .filter(component => component.isEnabled)
      .sort((c1, c2) => c1.alias.localeCompare(c2.alias));

    for (let sensor of this.sensors) {
      channelAddresses.push(
        new ChannelAddress(sensor.id, 'Temperature')
      )
    }
    return channelAddresses;
  }

  protected override onCurrentData(currentData: CurrentData) {
  }

}