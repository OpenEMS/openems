import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { TextIndentation } from 'src/app/shared/genericComponents/modal/modal-line/modal-line';
import { Converter } from 'src/app/shared/genericComponents/shared/converter';
import { Meter, Name } from 'src/app/shared/genericComponents/shared/name';
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from 'src/app/shared/genericComponents/shared/oe-formly-component';
import { Phase } from 'src/app/shared/genericComponents/shared/phase';
import { EdgeConfig } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

@Component({
    templateUrl: '../../../../../shared/formly/formly-field-modal/template.html'
})
export class ModalComponent extends AbstractFormlyComponent {

    protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
        return ModalComponent.generateView(config, role, this.translate);
    }

    public static generateView(config: EdgeConfig, role: Role, translate: TranslateService): OeFormlyView {
        let lines: OeFormlyField[] = [];

        let chargerComponents =
            config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
                .filter(component => component.isEnabled);
        let productionMeters = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
            .filter(component => component.isEnabled && config.isProducer(component));

        if (productionMeters.length == 0 && chargerComponents.length == 0) {
            return {
                title: translate.instant('General.production'),
                lines: lines
            };
        }

        if (productionMeters?.length > 0 && chargerComponents?.length > 0) {
            lines.push({
                type: 'channel-line',
                name: translate.instant('General.TOTAL'),
                channel: '_sum/ProductionActivePower',
                converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO
            });

            lines.push({
                type: 'horizontal-line'
            });
        }

        // Total
        if (productionMeters.length > 1) {
            lines.push({
                type: 'channel-line',
                name: translate.instant('General.TOTAL') + (chargerComponents.length > 0 ? ' AC' : ''),
                channel: '_sum/ProductionAcActivePower',
                converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO
            });
            Phase.THREE_PHASE.forEach((phase, index) => {
                lines.push({
                    type: 'children-line',
                    name: translate.instant('General.phase') + " " + phase,
                    children: [{
                        type: 'item',
                        channel: '_sum/ProductionAcActivePower' + phase,
                        converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO
                    }],
                    indentation: TextIndentation.SINGLE
                });
            });
            lines.push({ type: 'horizontal-line' });
        }

        // ProductionMeters
        productionMeters.forEach((meter, index) => {
            lines.push({ type: 'channel-line', name: Name.METER_ALIAS_OR_ID(meter), channel: meter.id + '/ActivePower', converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO });
            lines.push(...Meter.METER_PHASES(meter, translate, role));

            if (index < (productionMeters.length - 1)) {
                lines.push({ type: 'horizontal-line' });
            }
        });

        if (productionMeters.length > 0 && chargerComponents.length > 0) {
            lines.push({ type: 'horizontal-line' });
        }

        if (chargerComponents.length > 1) {
            // lines.push({ type: 'horizontal-line' });
            lines.push({
                type: 'channel-line',
                name: translate.instant('General.TOTAL') + (productionMeters?.length > 0 ? ' DC' : ''),
                channel: '_sum/ProductionDcActualPower',
                converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO
            });

            lines.push({ type: 'horizontal-line' });
        }

        // Charger
        chargerComponents.forEach((charger, index) => {
            function getLineItems(): OeFormlyField.Item[] {
                let children: OeFormlyField.Item[] = [];
                if (Role.isAtLeast(role, Role.INSTALLER)) {
                    children.push(
                        {
                            type: 'item',
                            channel: charger.id + '/Voltage',
                            converter: Converter.VOLTAGE_IN_MILLIVOLT_TO_VOLT
                        }, {
                        type: 'item',
                        channel: charger.id + '/Current',
                        converter: Converter.CURRENT_IN_MILLIAMPERE_TO_AMPERE
                    });
                }

                children.push({
                    type: 'item',
                    channel: charger.id + '/ActualPower',
                    converter: Converter.ONLY_POSITIVE_POWER_AND_NEGATIVE_AS_ZERO
                });

                return children;
            }
            lines.push({
                type: 'children-line',
                name: Name.METER_ALIAS_OR_ID(charger),
                children: getLineItems()
            });

            if (index < (chargerComponents.length - 1)) {
                lines.push({ type: 'horizontal-line' });
            }
        });

        lines.push({ type: 'horizontal-line' });
        lines.push({
            type: 'info-line',
            name: translate.instant("Edge.Index.Widgets.phasesInfo")
        });

        return {
            title: translate.instant('General.production'),
            lines: lines
        };
    }
}
