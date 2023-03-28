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
  channel?: string, channelCondition?: string | number, converter?: Function, name?: string, nameSuffix?: Function, indentation?: TextIndentation, roleIsAtLeast?: Role, children?: FormlyFieldLine[]
}

@Component({
  selector: 'modal',
  templateUrl: './modal.html',
  styles: [`
  
`]
})
export class ModalComponent extends AbstractModal {

  protected form: FormGroup = new FormGroup({});
  protected fields: FormlyFieldConfig[] = [];

  protected readonly GridMode = GridMode;
  protected grid: { mode: GridMode, buyFromGrid: number, sellToGrid: number } = { mode: GridMode.UNDEFINED, buyFromGrid: null, sellToGrid: null };

  protected meters: { component: EdgeConfig.Component, isAsymmetric: boolean }[] = []

  protected override getChannelAddresses(): ChannelAddress[] {

    let channelAddresses: ChannelAddress[] = [];

    const asymmetricMeters = this.config.getComponentsImplementingNature("io.openems.edge.meter.api.AsymmetricMeter")
      .filter(comp => comp.isEnabled && this.config.isTypeGrid(comp))

    this.config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
      .filter(component => component.isEnabled && this.config.isTypeGrid(component))
      .forEach(component => {
        var isAsymmetric = asymmetricMeters.filter(element => component.id == element.id).length > 0;
        this.meters.push({ component: component, isAsymmetric: isAsymmetric });
      })

    channelAddresses.push(
      new ChannelAddress("_sum", 'GridMode'),
      new ChannelAddress('_sum', 'GridActivePower'),
      new ChannelAddress('_sum', 'GridActivePowerL1'),
      new ChannelAddress('_sum', 'GridActivePowerL2'),
      new ChannelAddress('_sum', 'GridActivePowerL3'),
    )
    this.edge.getConfig(this.websocket).pipe(filter(config => !!config)).subscribe((config) => {
      sessionStorage.setItem("EdgeConfi2", JSON.stringify(config.components['meter0']))
      this.fields = ModalComponent.generateGridModal(this.edge.id, config, Role.getRole(this.edge.getRoleString()), this.translate);
    })
    return channelAddresses;
  }

  public static generateModalMeterPhases(component: EdgeConfig.Component, translate: TranslateService, userRole: Role) {
    let fields: FormlyFieldLine[] = [];

    for (let phase of ['L1', 'L2', 'L3']) {
      fields.push(
        {
          type: 'line',
          name: translate.instant('General.phase') + " " + phase + " ",
          nameSuffix: Utils.ADD_NAME_SUFFIX_FOR_GRIDSELL_OR_GRIDBUY(translate),
          indentation: TextIndentation.SIMPLE,
          channel: '_sum/GridActivePower' + phase,
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
    let fields: FormlyFieldConfig[] = [];
    for (let [key, component] of Object.entries(edgeConfig.components)) {
      let type = edgeConfig.components[key]?.['properties']?.['type'] ?? null;

      if ((type && type == 'GRID') || edgeConfig.isTypeGrid(component)) {
        fields.push({
          key: edgeId,
          type: "input",

          templateOptions: {
            attributes: {
              title: translate.instant('General.grid')
            },
            required: true,
            options: [
              {
                lines: [

                  // Show if offgrid
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
                  ...this.generateModalMeterPhases(component, translate, userRole),
                  {
                    type: 'line-info',
                    name: translate.instant("Edge.Index.Widgets.phasesInfo"),
                  },
                ]
              }
            ]
          },
          wrappers: ['formly-field-modal'],
        })
      }
    }

    sessionStorage.setItem("test", JSON.stringify(fields))
    return fields;
  }
}


// type json -> done
// unit test json
// Github actions ng test dazu
// PR auf Github nicht FEMS