import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { TextIndentation } from 'src/app/shared/genericComponents/modal/modal-line/modal-line';
import { AbstractFormlyComponent, OeFormlyField } from 'src/app/shared/genericComponents/shared/oe-formly-component';
import { EdgeConfig, Utils } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

@Component({
  templateUrl: '../../../../../shared/formly/formly-field-modal/generalModal.html'
})
export class ModalComponent extends AbstractFormlyComponent {

  protected generateView(edgeId: string, config: EdgeConfig, role: Role): { title: string; lines: OeFormlyField[]; } {
    const meters = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(comp => comp.isEnabled && config.isTypeGrid(comp));

    // Grid-Mode
    let lines: OeFormlyField[] = [{
      type: 'line',
      name: this.translate.instant("General.offGrid"),
      channel: '_sum/GridMode',
      filter: Utils.isGridModeOffGrid()
    }]

    var gridMeters = Object.values(config.components).filter(component => config?.isTypeGrid(component));

    // Sum Channels (if more than one meter)
    if (gridMeters.length > 1) {
      lines.push({
        type: 'line',
        name: this.translate.instant("General.gridBuyAdvanced"),
        channel: '_sum/GridActivePower',
        converter: Utils.CONVERT_TO_GRID_BUY_POWER
      }, {
        type: 'line',
        name: this.translate.instant("General.gridSellAdvanced"),
        channel: '_sum/GridActivePower',
        converter: Utils.CONVERT_TO_GRID_SELL_POWER
      }, {
        type: 'line-horizontal'
      });
      // TODO show phase values from _sum
    }

    // Individual Meters
    for (var meter of gridMeters) {
      if (gridMeters.length == 1) {
        // Two lines if there is only one meter (= same visualization as with Sum Channels)
        lines.push({
          type: 'line',
          name: this.translate.instant("General.gridBuyAdvanced"),
          channel: meter.id + '/ActivePower',
          converter: Utils.CONVERT_TO_GRID_BUY_POWER,
        }, {
          type: 'line',
          name: this.translate.instant("General.gridSellAdvanced"),
          channel: meter.id + '/ActivePower',
          converter: Utils.CONVERT_TO_GRID_SELL_POWER,
        });

      } else {
        // More than one meter? Show only one line per meter.
        lines.push({
          type: 'line',
          name: Utils.ADD_NAME_SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(this.translate, meter.alias),
          channel: meter.id + '/ActivePower',
          converter: Utils.CONVERT_TO_GRID_SELL_OR_GRID_BUY_POWER
        });
      }

      lines.push(
        // Individual phases: Voltage, Current and Power
        ...ModalComponent.generateModalMeterPhases(meter, this.translate, role), {
        // Line separator
        type: 'line-horizontal'
      });
    }

    // Technical info
    lines.push({
      type: 'line-info',
      name: this.translate.instant("Edge.Index.Widgets.phasesInfo")
    });

    return {
      title: this.translate.instant('General.grid'),
      lines: lines
    };
  }

  private static generateModalMeterPhases(component: EdgeConfig.Component, translate: TranslateService, role: Role): OeFormlyField[] {
    return ['L1', 'L2', 'L3']
      .map(phase => <OeFormlyField>{
        type: 'line',
        name: Utils.ADD_NAME_SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(translate, translate.instant('General.phase') + " " + phase),
        indentation: TextIndentation.SINGLE,
        channel: component.id + '/ActivePower' + phase,
        children: [
          Role.isAtLeast(role, Role.INSTALLER) && {
            type: 'line-item',
            channel: component.id + '/Voltage' + phase,
            converter: Utils.CONVERT_TO_VOLT
          },
          Role.isAtLeast(role, Role.INSTALLER) && {
            type: 'line-item',
            channel: component.id + '/Current' + phase,
            converter: Utils.CONVERT_TO_CURRENT,
          },
          {
            type: 'line-item',
            channel: component.id + '/ActivePower' + phase,
            converter: Utils.CONVERT_TO_GRID_SELL_OR_GRID_BUY_POWER,
          }
        ]
      });
  }

}
