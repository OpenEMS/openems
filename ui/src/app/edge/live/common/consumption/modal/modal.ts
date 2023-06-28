import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { TextIndentation } from 'src/app/shared/genericComponents/modal/modal-line/modal-line';
import { Converter } from 'src/app/shared/genericComponents/shared/converter';
import { Name } from 'src/app/shared/genericComponents/shared/name';
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from 'src/app/shared/genericComponents/shared/oe-formly-component';
import { Phase } from 'src/app/shared/genericComponents/shared/phase';
import { Role } from 'src/app/shared/type/role';

import { ChannelAddress, CurrentData, EdgeConfig } from '../../../../../shared/shared';

@Component({
  templateUrl: '../../../../../shared/formly/formly-field-modal/template.html'
})
export class ModalComponent extends AbstractFormlyComponent {

  protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
    return ModalComponent.generateView(config, role, this.translate);
  }

  public static generateView(config: EdgeConfig, role: Role, translate: TranslateService): OeFormlyView {

    const evcss: EdgeConfig.Component[] | null = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
      .filter(component => !(component.factoryId == 'Evcs.Cluster.SelfConsumption') &&
        !(component.factoryId == 'Evcs.Cluster.PeakShaving') && !component.isEnabled == false);

    const consumptionMeters: EdgeConfig.Component[] | null = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component));

    let lines: OeFormlyField[] = [];

    // Total
    lines.push({
      type: 'channel-line',
      name: translate.instant('General.TOTAL'),
      channel: '_sum/ConsumptionActivePower',
      converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO
    });

    Phase.THREE_PHASE.forEach(phase => {
      lines.push({
        type: 'channel-line',
        name: translate.instant('General.phase') + ' ' + phase,
        indentation: TextIndentation.SINGLE,
        channel: '_sum/ConsumptionActivePower' + phase,
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO
      });
    });

    if (evcss.length > 0) {
      lines.push({
        type: 'horizontal-line'
      });
    }

    // Evcss
    evcss.forEach((evcs, index) => {
      lines.push({
        type: 'channel-line',
        name: Name.METER_ALIAS_OR_ID(evcs),
        channel: evcs.id + '/ChargePower',
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO
      });

      if (index < (evcss.length - 1)) {
        lines.push({ type: 'horizontal-line' });
      }
    });

    if (consumptionMeters.length > 0) {
      lines.push({ type: 'horizontal-line' });
    }

    // Consumptionmeters
    consumptionMeters.forEach((meter, index) => {
      lines.push({
        type: 'channel-line',
        name: Name.METER_ALIAS_OR_ID(meter),
        channel: meter.id + '/ActivePower',
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO
      });
      Phase.THREE_PHASE.forEach(phase => {
        lines.push({
          type: 'channel-line',
          name: 'Phase ' + phase,
          channel: meter.id + '/ActivePower' + phase,
          indentation: TextIndentation.SINGLE,
          converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO
        });
      });

      if (index < (consumptionMeters.length - 1)) {
        lines.push({
          type: 'horizontal-line'
        });
      }
    });

    lines.push({ type: 'horizontal-line' });

    // OtherPower
    let channelsToSubscribe: ChannelAddress[] = [new ChannelAddress('_sum', 'ConsumptionActivePower')];

    evcss.forEach(evcs => channelsToSubscribe.push(new ChannelAddress(evcs.id, 'ChargePower')));
    consumptionMeters.forEach(meter => {
      channelsToSubscribe.push(...[new ChannelAddress(meter.id, 'ActivePower')]);
    });

    lines.push({
      type: 'value-from-channels-line',
      name: translate.instant('General.otherConsumption'),
      value: (currentData: CurrentData) => Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO(Converter.CALCULATE_CONSUMPTION_OTHER_POWER(evcss, consumptionMeters, currentData)),
      channelsToSubscribe: channelsToSubscribe
    });

    lines.push({
      type: 'info-line',
      name: translate.instant('Edge.Index.Widgets.phasesInfo')
    });

    return {
      title: translate.instant('General.consumption'),
      lines: lines
    };
  }



  // public readonly CONVERT_TO_WATT = Utils.CONVERT_TO_WATT;

  // public activePower: { total: number, phases: Array<{ value: number, name: string }> } = {
  //   total: 0,
  //   phases: [
  //     { name: "Phase L1", value: 0 },
  //     { name: "Phase L2", value: 0 },
  //     { name: "Phase L3", value: 0 }
  //   ]
  // };
  // public evcss: EdgeConfig.Component[] | null = null;
  // public evcsChargePower: { total: number, component: { name: string, value: number }[] } = { total: 0, component: [] };
  // public consumptionMeters: EdgeConfig.Component[] = null;
  // public consumptionMetersActivePower: { total: number, component: { name: string, value: number, phases: { name: string, value: number }[] }[] } = { total: 0, component: [] };
  // public otherPower: number = null;

  // protected override getChannelAddresses() {

  //   let channelAddresses: ChannelAddress[] = [];

  //   // Get ConsumptionMeters
  //   this.consumptionMeters = this.config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
  //     .filter(component => component.isEnabled && this.config.isTypeConsumptionMetered(component));

  //   for (let meter of this.consumptionMeters) {
  //     channelAddresses.push(
  //       new ChannelAddress(meter.id, 'ActivePower'),
  //       new ChannelAddress(meter.id, 'ActivePowerL1'),
  //       new ChannelAddress(meter.id, 'ActivePowerL2'),
  //       new ChannelAddress(meter.id, 'ActivePowerL3')
  //     );
  //     this.consumptionMetersActivePower.component.push({
  //       name: meter.alias ?? meter.id,
  //       value: 0,
  //       phases: [{
  //         name: "Phase L1",
  //         value: 0
  //       },
  //       {
  //         name: "Phase L2",
  //         value: 0
  //       },
  //       {
  //         name: "Phase L3",
  //         value: 0
  //       }]
  //     });
  //   }
  //   // Get EVCSs
  //   this.evcss = this.config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
  //     .filter(component => !(component.factoryId == 'Evcs.Cluster.SelfConsumption') &&
  //       !(component.factoryId == 'Evcs.Cluster.PeakShaving') && !component.isEnabled == false);

  //   for (let component of this.evcss) {
  //     channelAddresses.push(
  //       new ChannelAddress(component.id, 'ChargePower')
  //     );
  //   }
  //   for (let i = 0; i < this.evcss.length; i++) {
  //     this.evcsChargePower.component.push({ name: this.evcss[i].alias ?? this.evcss[i].id, value: 0 });
  //   }

  //   channelAddresses.push(
  //     new ChannelAddress("_sum", "ConsumptionActivePower"),
  //     new ChannelAddress('_sum', 'ConsumptionActivePowerL1'),
  //     new ChannelAddress('_sum', 'ConsumptionActivePowerL2'),
  //     new ChannelAddress('_sum', 'ConsumptionActivePowerL3')
  //   );
  //   return channelAddresses;
  // }

  // protected override onCurrentData(currentData: CurrentData) {
  //   this.activePower.total = 0;
  //   this.consumptionMetersActivePower.total = 0;
  //   this.evcsChargePower.total = 0;

  //   this.activePower.total = currentData.allComponents['_sum/ConsumptionActivePower'];
  //   for (let i of [0, 1, 2]) {
  //     this.activePower.phases[i].value = currentData.allComponents['_sum/ConsumptionActivePowerL' + (i + 1)] ?? 0;
  //   }
  //   for (let i = 0; i < this.evcss.length; i++) {
  //     this.evcsChargePower.total += (currentData.allComponents[this.evcss[i].id + '/ChargePower']) ?? 0;
  //     this.evcsChargePower.component[i].value = currentData.allComponents[this.evcss[i].id + '/ChargePower'] ?? 0;
  //   }

  //   for (let i = 0; i < this.consumptionMeters.length; i++) {
  //     this.consumptionMetersActivePower.total += currentData.allComponents[this.consumptionMeters[i].id + '/ActivePower'] ?? 0;
  //     this.consumptionMetersActivePower.component[i].value = currentData.allComponents[this.consumptionMeters[i].id + '/ActivePower'] ?? 0;
  //     for (let n = 0; n < 3; n++) {
  //       this.consumptionMetersActivePower.component[i].phases[n].value = currentData.allComponents[this.consumptionMeters[i].id + '/ActivePowerL' + (n + 1)] ?? 0;
  //     }
  //   }

  //   this.otherPower = Utils.subtractSafely(this.activePower.total,
  //     Utils.addSafely(this.evcsChargePower.total, this.consumptionMetersActivePower.total));
  // }
}