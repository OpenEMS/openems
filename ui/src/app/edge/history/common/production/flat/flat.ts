import { ChannelAddress, CurrentData, EdgeConfig, Utils } from '../../../../../shared/shared';
import { Component } from '@angular/core';
import { AbstractHistoryWidget } from 'src/app/shared/genericComponents/abstracthistorywidget';

@Component({
  selector: 'productionWidget',
  templateUrl: './flat.html'
})
export class FlatComponent extends AbstractHistoryWidget {

  public productionMeterComponents: EdgeConfig.Component[] = [];
  public chargerComponents: EdgeConfig.Component[] = [];
  public readonly CONVERT_TO_KILO_WATTHOURS = Utils.CONVERT_TO_KILO_WATTHOURS;

  protected getChannelAddresses(): ChannelAddress[] {
    //  Get Chargers
    this.chargerComponents =
      this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
        .filter(component => component.isEnabled);

    // Get productionMeters
    this.productionMeterComponents =
      this.config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
        .filter(component => component.isEnabled && this.config.isProducer(component));
    return [];
  }

  protected onCurrentData(currentData: CurrentData) {

  }
}
