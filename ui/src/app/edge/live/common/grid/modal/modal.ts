import { Component } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { CustomViewComponent } from 'src/app/shared/decorators';
import { TextIndentation } from 'src/app/shared/genericComponents/modal/modal-line/modal-line';
import { ModalField } from 'src/app/shared/genericComponents/shared/types';
import { EdgeConfig, Utils } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

@Component({
  templateUrl: '../../../../../shared/formly/formly-field-modal/generalModal.html',
})
@CustomViewComponent
export class ModalComponent {

  protected fields: FormlyFieldConfig[] = [];
  protected form: FormGroup = new FormGroup({});

  protected meters: { component: EdgeConfig.Component, isAsymmetric: boolean }[] = [];

  private static generateModalMeterPhases(component: EdgeConfig.Component, translate: TranslateService, role: Role): ModalField[] {

    return ['L1', 'L2', 'L3']
      .map(phase => {
        return {
          type: 'line',
          name: Utils.ADD_NAME_SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(translate, phase),
          indentation: TextIndentation.SINGLE,
          channel: component.id + '/ActivePower' + phase,
          children: [
            Role.isAtLeast(role, Role.INSTALLER) && {
              type: 'line-item',
              channel: component.id + '/Voltage' + phase,
              converter: Utils.CONVERT_TO_VOLT,
              indentation: TextIndentation.SINGLE,
            },
            Role.isAtLeast(role, Role.INSTALLER) && {
              type: 'line-item',
              channel: component.id + '/Current' + phase,
              converter: Utils.CONVERT_TO_CURRENT,
              indentation: TextIndentation.SINGLE,
            },
            {
              type: 'line-item',
              channel: component.id + '/ActivePower' + phase,
              converter: Utils.CONVERT_TO_GRID_SELL_OR_GRID_BUY_POWER,
              indentation: TextIndentation.SINGLE
            }
          ]
        }
      })
  }

  public static generateView(edgeId: string, edgeConfig: EdgeConfig, userRole: Role, translate: TranslateService): FormlyFieldConfig[] {
    const meters = edgeConfig.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
      .filter(comp => comp.isEnabled && edgeConfig.isTypeGrid(comp));
    let lines: ModalField[] = [
      {
        type: 'line',
        name: translate.instant("General.offGrid"),
        channel: '_sum/GridMode',
        filter: Utils.isGridModeOffGrid()
      },
      {
        type: 'line',
        name: translate.instant("General.gridBuyAdvanced"),
        channel: '_sum/GridActivePower',
        converter: Utils.CONVERT_TO_GRID_BUY_POWER,
      },
      {
        type: 'line',
        name: translate.instant("General.gridSellAdvanced"),
        channel: '_sum/GridActivePower',
        converter: Utils.CONVERT_TO_GRID_SELL_POWER,
      },
    ];

    for (let component of Object.values(edgeConfig.components)) {
      if (edgeConfig?.isTypeGrid(component)) {

        if (meters.length > 1) {
          lines.push(
            {
              type: 'line',
              name: component.id,
              channel: component.id + '/ActivePower',
              converter: Utils.CONVERT_TO_WATT,
            });
        }

        lines.push(...ModalComponent.generateModalMeterPhases(component, translate, userRole),
        );

      }
    }

    lines.push(
      {
        type: 'line-info',
        name: translate.instant("Edge.Index.Widgets.phasesInfo"),
      },
    );

    let fields: FormlyFieldConfig[] = [{
      key: edgeId,
      type: "input",

      templateOptions: {
        attributes: {
          title: translate.instant('General.grid')
        },
        required: true,
        options: [{ lines: lines }],
      },
      wrappers: ['formly-field-modal'],
    }];
    return fields;
  }
}