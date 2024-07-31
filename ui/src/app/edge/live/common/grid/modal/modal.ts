import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { TextIndentation } from 'src/app/shared/components/modal/modal-line/modal-line';
import { Converter } from 'src/app/shared/components/shared/converter';
import { Filter } from 'src/app/shared/components/shared/filter';
import { Name } from 'src/app/shared/components/shared/name';
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from 'src/app/shared/components/shared/oe-formly-component';
import { ChannelAddress, CurrentData, EdgeConfig } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';
import { GridSectionComponent } from '../../../energymonitor/chart/section/grid.component';

@Component({
  templateUrl: '../../../../../shared/components/formly/formly-field-modal/template.html',
})
export class ModalComponent extends AbstractFormlyComponent {

  protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
    return ModalComponent.generateView(config, role, this.translate);
  }

  public static generateView(config: EdgeConfig, role: Role, translate: TranslateService): OeFormlyView {

    const isActivated = GridSectionComponent.isControllerEnabled(config, 'Controller.Ess.Limiter14a');
    // Grid-Mode
    const lines: OeFormlyField[] = [{
      type: 'channel-line',
      name: translate.instant("General.offGrid"),
      channel: '_sum/GridMode',
      filter: Filter.GRID_MODE_IS_OFF_GRID,
      converter: Converter.HIDE_VALUE,
    }];

    const gridMeters = Object.values(config.components).filter(component => config?.isTypeGrid(component));

    // Sum Channels (if more than one meter)
    if (gridMeters.length > 1) {
      if (isActivated) {
        lines.push({
          type: 'value-from-channels-line',
          name: translate.instant("General.state"),
          value: (currentData: CurrentData) => Converter.GRID_STATE_TO_MESSAGE(translate, currentData),
          channelsToSubscribe: [
            new ChannelAddress("_sum", "GridMode"),
            new ChannelAddress("ctrlEssLimiter14a0", "RestrictionMode"),
          ],
        });
      }

      lines.push(
        {
          type: 'channel-line',
          name: translate.instant("General.gridSellAdvanced"),
          channel: '_sum/GridActivePower',
          converter: Converter.GRID_SELL_POWER_OR_ZERO,
        },
        {
          type: 'channel-line',
          name: translate.instant("General.gridBuyAdvanced"),
          channel: '_sum/GridActivePower',
          converter: Converter.GRID_BUY_POWER_OR_ZERO,
        },
        {
          type: 'horizontal-line',
        },
      );
    }


    // Individual Meters
    for (const meter of gridMeters) {
      if (gridMeters.length === 1) {
        // Two lines if there is only one meter (= same visualization as with Sum Channels)
        if (isActivated) {
          lines.push({
            type: 'value-from-channels-line',
            name: translate.instant("General.state"),
            value: (currentData: CurrentData) => Converter.GRID_STATE_TO_MESSAGE(translate, currentData),
            channelsToSubscribe: [
              new ChannelAddress("_sum", "GridMode"),
              new ChannelAddress("ctrlEssLimiter14a0", "RestrictionMode"),
            ],
          });
        }

        lines.push(
          {
            type: 'channel-line',
            name: translate.instant("General.gridSellAdvanced"),
            channel: meter.id + '/ActivePower',
            converter: Converter.GRID_SELL_POWER_OR_ZERO,
          },
          {
            type: 'channel-line',
            name: translate.instant("General.gridBuyAdvanced"),
            channel: meter.id + '/ActivePower',
            converter: Converter.GRID_BUY_POWER_OR_ZERO,
          },
        );

      } else {
        // More than one meter? Show only one line per meter.
        lines.push({
          type: 'channel-line',
          name: Name.SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(translate, meter.alias),
          channel: meter.id + '/ActivePower',
          converter: Converter.POWER_IN_WATT,
        });
      }

      lines.push(
        // Individual phases: Voltage, Current and Power
        ...ModalComponent.generatePhasesView(meter, translate, role),
        {
          // Line separator
          type: 'horizontal-line',
        },
      );
    }

    if (gridMeters.length > 0) {
      // Technical info
      lines.push({
        type: 'info-line',
        name: translate.instant("Edge.Index.Widgets.phasesInfo"),
      });
    }

    return {
      title: translate.instant('General.grid'),
      lines: lines,
    };
  }

  private static generatePhasesView(component: EdgeConfig.Component, translate: TranslateService, role: Role): OeFormlyField[] {
    return ['L1', 'L2', 'L3']
      .map(phase => <OeFormlyField>{
        type: 'children-line',
        name: {
          channel: ChannelAddress.fromString(component.id + '/ActivePower' + phase),
          converter: Name.SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(translate, translate.instant('General.phase') + " " + phase),
        },
        indentation: TextIndentation.SINGLE,
        children: ModalComponent.generatePhasesLineItems(role, phase, component),
      });
  }

  private static generatePhasesLineItems(role: Role, phase: string, component: EdgeConfig.Component) {
    const children: OeFormlyField[] = [];
    if (Role.isAtLeast(role, Role.INSTALLER)) {
      children.push({
        type: 'item',
        channel: component.id + '/Voltage' + phase,
        converter: Converter.VOLTAGE_IN_MILLIVOLT_TO_VOLT,
      }, {
        type: 'item',
        channel: component.id + '/Current' + phase,
        converter: Converter.CURRENT_IN_MILLIAMPERE_TO_AMPERE,
      });
    }

    children.push({
      type: 'item',
      channel: component.id + '/ActivePower' + phase,
      converter: Converter.POSITIVE_POWER,
    });

    return children;
  }
}
