import { Component } from '@angular/core';
import { AbstractHistoryChartOverview } from '../../../../../shared/genericComponents/chart/abstractHistoryChartOverview';
import { ChannelAddress, EdgeConfig } from 'src/app/shared/shared';

@Component({
  templateUrl: './overview.html'
})
export class OverviewComponent extends AbstractHistoryChartOverview {
  public essComponents: EdgeConfig.Component[] = null;
  public chargerComponents: EdgeConfig.Component[] = null;

  protected override getChannelAddresses(): ChannelAddress[] {

    this.essComponents = this.config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
      .filter(component => !component.factoryId.includes("Ess.Cluster"));

    //  Get Chargers
    this.chargerComponents = this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
      .filter(component => component.isEnabled);

    return [];
  }


}  