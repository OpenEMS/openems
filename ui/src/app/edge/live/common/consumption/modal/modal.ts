import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { TextIndentation } from 'src/app/shared/components/modal/modal-line/modal-line';
import { Converter } from 'src/app/shared/components/shared/converter';
import { Name } from 'src/app/shared/components/shared/name';
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from 'src/app/shared/components/shared/oe-formly-component';
import { Phase } from 'src/app/shared/components/shared/phase';

import { ChannelAddress, CurrentData, EdgeConfig } from '../../../../../shared/shared';

@Component({
  templateUrl: '../../../../../shared/components/formly/formly-field-modal/template.html',
})
export class ModalComponent extends AbstractFormlyComponent {

  public static generateView(config: EdgeConfig, translate: TranslateService): OeFormlyView {

    const evcss: EdgeConfig.Component[] | null = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
      .filter(component => !(component.factoryId == 'Evcs.Cluster.SelfConsumption') &&
        !(component.factoryId == 'Evcs.Cluster.PeakShaving') && !component.isEnabled == false);

    const consumptionMeters: EdgeConfig.Component[] | null = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component));

    const lines: OeFormlyField[] = [];

    // Total
    lines.push({
      type: 'channel-line',
      name: translate.instant('General.TOTAL'),
      channel: '_sum/ConsumptionActivePower',
      converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
    });

    Phase.THREE_PHASE.forEach(phase => {
      lines.push({
        type: 'channel-line',
        name: translate.instant('General.phase') + ' ' + phase,
        indentation: TextIndentation.SINGLE,
        channel: '_sum/ConsumptionActivePower' + phase,
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
      });
    });

    if (evcss.length > 0) {
      lines.push({
        type: 'horizontal-line',
      });
    }

    // Evcss
    evcss.forEach((evcs, index) => {
      lines.push({
        type: 'channel-line',
        name: Name.METER_ALIAS_OR_ID(evcs),
        channel: evcs.id + '/ChargePower',
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
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
        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
      });
      Phase.THREE_PHASE.forEach(phase => {
        lines.push({
          type: 'channel-line',
          name: 'Phase ' + phase,
          channel: meter.id + '/ActivePower' + phase,
          indentation: TextIndentation.SINGLE,
          converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO,
        });
      });

      if (index < (consumptionMeters.length - 1)) {
        lines.push({
          type: 'horizontal-line',
        });
      }
    });

    lines.push({ type: 'horizontal-line' });

    // OtherPower
    const channelsToSubscribe: ChannelAddress[] = [new ChannelAddress('_sum', 'ConsumptionActivePower')];

    evcss.forEach(evcs => channelsToSubscribe.push(new ChannelAddress(evcs.id, 'ChargePower')));
    consumptionMeters.forEach(meter => {
      channelsToSubscribe.push(...[new ChannelAddress(meter.id, 'ActivePower')]);
    });

    lines.push({
      type: 'value-from-channels-line',
      name: translate.instant('General.otherConsumption'),
      value: (currentData: CurrentData) => Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO(Converter.CALCULATE_CONSUMPTION_OTHER_POWER(evcss, consumptionMeters, currentData)),
      channelsToSubscribe: channelsToSubscribe,
    });

    lines.push({
      type: 'info-line',
      name: translate.instant('Edge.Index.Widgets.phasesInfo'),
    });

    return {
      title: translate.instant('General.consumption'),
      lines: lines,
    };
  }

  protected override generateView(config: EdgeConfig): OeFormlyView {
    return ModalComponent.generateView(config, this.translate);
  }

}
