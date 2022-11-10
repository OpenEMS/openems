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

// import { ChannelAddress, EdgeConfig, Utils } from 'src/app/shared/shared';
// import { Component } from '@angular/core';
// import { AbstractFlatWidget } from 'src/app/shared/genericComponents/flat/abstract-flat-widget';
// import { ModalComponent } from '../modal/modal';

// @Component({
//     selector: 'Common_Production',
//     templateUrl: './flat.html'
// })
// export class FlatComponent extends AbstractFlatWidget {


//     protected override getChannelAddresses() {
//         // Get Chargers
//         this.chargerComponents =
//             this.config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
//                 .filter(component => component.isEnabled);

//         // Get productionMeters
//         this.productionMeterComponents =
//             this.config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
//                 .filter(component => component.isEnabled && this.config.isProducer(component));

//         return []
//     }

//     async presentModal() {
//         const modal = await this.modalController.create({
//             component: ModalComponent,
//         });
//         return await modal.present();
//     }
// }
