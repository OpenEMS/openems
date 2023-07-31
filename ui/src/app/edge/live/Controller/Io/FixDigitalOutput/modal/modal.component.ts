import { Component, Input } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from 'src/app/shared/genericComponents/shared/oe-formly-component';
import { CurrentData, EdgeConfig } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

@Component({
  templateUrl: '../../../../../../shared/formly/formly-field-modal/template.html'
})
export class ModalComponent extends AbstractFormlyComponent {

  @Input() public component: EdgeConfig.Component;

  protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
    return ModalComponent.generateView(config, role, this.translate, this.component.id);
  }

  public static generateView(config: EdgeConfig, role: Role, translate: TranslateService, componentId: string): OeFormlyView {
    let component = config.components[componentId];
    let lines: OeFormlyField[] = [
      {
        type: 'only-name-line',
        name: translate.instant('General.mode')
      },
      {
        type: 'buttons-from-channel-line',
        buttons: [
          {
            name: translate.instant('General.on'),
            value: "true",
            icon: { color: "success", size: "small", name: "power-outline" }
          },
          {
            name: translate.instant('General.off'),
            value: "false",
            icon: { color: "danger", size: "small", name: "power-outline" }
          }
        ],
        controlName: 'isOn',
        channel: component.id + '/_PropertyIsOn',
        converter: (currentData: CurrentData) => currentData.allComponents[component.id + '/_PropertyIsOn'] == 1
      }
    ];

    return {
      title: component.alias,
      lines: lines,
      component: component
    };
  }
}