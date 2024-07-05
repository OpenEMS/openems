import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractFormlyComponent, OeFormlyView } from 'src/app/shared/genericComponents/shared/oe-formly-component';
import { EdgeConfig } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

@Component({
    templateUrl: '../../../../../shared/formly/formly-field-modal/template.html',
})
export class ModalComponent extends AbstractFormlyComponent {
    protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
        return ModalComponent.generateView(this.translate);
    }

    public static generateView(translate: TranslateService): OeFormlyView {
        return {
            title: translate.instant('GENERAL.SELF_CONSUMPTION'),
            lines: [{
                type: 'info-line',
                name: translate.instant("EDGE.INDEX.WIDGETS.SELFCONSUMPTION_INFO"),
            }],
        };
    }
}
