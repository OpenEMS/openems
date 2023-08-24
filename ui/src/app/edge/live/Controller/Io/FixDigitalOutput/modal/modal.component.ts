import { Component, Input } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Converter } from 'src/app/shared/genericComponents/shared/converter';
import { Name } from 'src/app/shared/genericComponents/shared/name';
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from 'src/app/shared/genericComponents/shared/oe-formly-component';
import { EdgeConfig } from 'src/app/shared/shared';
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
        type: 'name-line',
        name: translate.instant('General.mode')
      },
      {
        type: 'buttons-from-channel-line',
        buttons: [
          {
            name: translate.instant('General.on'),
            value: "true",
            icon: { color: "success", name: "power-outline" }
          },
          {
            name: translate.instant('General.off'),
            value: "false",
            icon: { color: "danger", name: "power-outline" }
          }
        ],
        controlName: 'isOn',
        channel: component.id + '/_PropertyIsOn',
        converter: Converter.IS_RELAY_ON
      }
    ];

    return {
      title: Name.METER_ALIAS_OR_ID(component),
      lines: lines,
      component: component
    };
  }
}