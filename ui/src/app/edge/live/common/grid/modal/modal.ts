import { Component } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { CustomViewComponent } from 'src/app/shared/decorators';
import { TextIndentation } from 'src/app/shared/genericComponents/modal/modal-line/modal-line';
import { EdgeConfig, Utils } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

export type FormlyFieldLine = {
  type: string,
  channel?: string,
  filter?: Function,
  converter?: Function,
  name?: string | Function,
  indentation?: TextIndentation,
  children?: FormlyFieldLine[]
}

@Component({
  templateUrl: '../../../../../shared/formly/formly-field-modal/generalModal.html',
})
@CustomViewComponent
export class ModalComponent {

  protected fields: FormlyFieldConfig[] = [];
  protected form: FormGroup = new FormGroup({});

  public static generateModalMeterPhases(component: EdgeConfig.Component, translate: TranslateService, userRole: Role): FormlyFieldLine[] {
    let fields: FormlyFieldLine[] = [];

    ['L1', 'L2', 'L3'].forEach(phase => {
      fields.push(
        {
          type: 'line',
          name: Utils.ADD_NAME_SUFFIX_FOR_GRID_SELL_OR_GRID_BUY(translate, phase),
          indentation: TextIndentation.SINGLE,
          channel: component.id + '/ActivePower' + phase,
          children: [
            Role.isAtLeast(userRole, Role.INSTALLER) && {
              type: 'line-item',
              channel: component.id + '/Voltage' + phase,
              converter: Utils.CONVERT_TO_VOLT,
              indentation: TextIndentation.SINGLE,
            },
            Role.isAtLeast(userRole, Role.INSTALLER) && {
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
        })
    });

    return fields;
  }

  public static generateView(edgeId: string, edgeConfig: EdgeConfig, userRole: Role, translate: TranslateService): FormlyFieldConfig[] {

    const asymmetricMeters = edgeConfig.getComponentsImplementingNature("io.openems.edge.meter.api.AsymmetricMeter")
      .filter(comp => comp.isEnabled && edgeConfig.isTypeGrid(comp));

    let lines: FormlyFieldLine[] = [
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
    ]

    for (let component of Object.values(edgeConfig.components)) {
      let isMeterAsymmetric: boolean = asymmetricMeters.some((meter) => meter.id == component.id)

      if (edgeConfig?.isTypeGrid(component)) {
        lines.push(
          // Show if multiple GridMeters are installed
          asymmetricMeters.length > 1 &&
          {
            type: 'line',
            name: component.id,
            channel: component.id + '/ActivePower',
            converter: Utils.CONVERT_TO_WATT,
          },
          ...(isMeterAsymmetric ?
            this.generateModalMeterPhases(component, translate, userRole) : []),
        )

      }
    }

    lines.push(
      {
        type: 'line-info',
        name: translate.instant("Edge.Index.Widgets.phasesInfo"),
      },
    )

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
    }]
    return fields;
  }
}