import { Component } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';
import { filter } from 'rxjs/operators';
import { AbstractModal } from 'src/app/shared/genericComponents/modal/abstractModal';
import { TextIndentation } from 'src/app/shared/genericComponents/modal/modal-line/modal-line';
import { ChannelAddress, Edge, EdgeConfig, GridMode, Utils } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

export type FormlyFieldLine = {
  type: string,
  channel?: string,
  channelCondition?: string | number,
  converter?: Function,
  name?: string,
  nameSuffix?: Function,
  indentation?: TextIndentation,
  roleIsAtLeast?: Role,
  children?: FormlyFieldLine[]
}

@Component({
  selector: 'modal',
  templateUrl: './modal.html'
})
export class ModalComponent extends AbstractModal {

  protected form: FormGroup = new FormGroup({});
  protected fields: FormlyFieldConfig[] = [];

  protected override getChannelAddresses(): ChannelAddress[] {
    this.edge.getConfig(this.websocket).pipe(filter(config => !!config)).subscribe((config) => {
      this.fields = ModalComponent.generateGridModal(this.edge.id, config, this.edge.role, this.translate);
    })
    return [];
  }

  public static generateModalMeterPhases(component: EdgeConfig.Component, translate: TranslateService, userRole: Role): FormlyFieldLine[] {
    let fields: FormlyFieldLine[] = [];

    for (let phase of ['L1', 'L2', 'L3']) {
      fields.push(
        {
          type: 'line',
          name: translate.instant('General.phase') + " " + phase + " ",
          nameSuffix: Utils.ADD_NAME_SUFFIX_FOR_GRIDSELL_OR_GRIDBUY(translate),
          indentation: TextIndentation.SIMPLE,
          channel: component.id + '/ActivePower' + phase,
          children: [
            Role.isAtLeast(userRole, Role.INSTALLER) && {
              type: 'line-item',
              channel: component.id + '/Voltage' + phase,
              converter: Utils.CONVERT_TO_VOLT,
              indentation: TextIndentation.SIMPLE,
            },
            Role.isAtLeast(userRole, Role.INSTALLER) && {
              type: 'line-item',
              channel: component.id + '/Current' + phase,
              converter: Utils.CONVERT_TO_CURRENT,
              indentation: TextIndentation.SIMPLE,
            },
            {
              type: 'line-item',
              channel: component.id + '/ActivePower' + phase,
              converter: Utils.CONVERT_TO_GRIDSELL_OR_GRIDBUY_POWER,
              indentation: TextIndentation.SIMPLE
            }
          ]
        })
    };

    return fields;
  }

  public static generateGridModal(edgeId: string, edgeConfig: EdgeConfig, userRole: Role, translate: TranslateService): FormlyFieldConfig[] {

    const hasMultipleGridMeters = edgeConfig.getComponentsImplementingNature("io.openems.edge.meter.api.AsymmetricMeter")
      .filter(comp => comp.isEnabled && edgeConfig.isTypeGrid(comp)).length > 1;

    let lines: FormlyFieldLine[] = [
      {
        type: 'line',
        name: translate.instant("General.offGrid"),
        channel: '_sum/GridMode',
        channelCondition: GridMode.OFF_GRID
      },
      {
        type: 'line',
        name: translate.instant("General.gridBuyAdvanced"),
        channel: '_sum/GridActivePower',
        converter: Utils.CONVERT_TO_GRIDBUY_POWER,
      },
      {
        type: 'line',
        name: translate.instant("General.gridSellAdvanced"),
        channel: '_sum/GridActivePower',
        converter: Utils.CONVERT_TO_GRIDSELL_POWER,
      },
    ]

    for (let [key, component] of Object.entries(edgeConfig.components)) {
      let isMeterAsymmetric: boolean = edgeConfig
        .getComponentsImplementingNature("io.openems.edge.meter.api.AsymmetricMeter")
        .filter(element => element.id === component.id).length > 0;
      let type = edgeConfig.components[key]?.['properties']?.['type'] ?? null;

      if ((type && type == 'GRID') || edgeConfig?.isTypeGrid(component)) {
        lines.push(
          // Show if multiple GridMeters are installed
          hasMultipleGridMeters &&
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