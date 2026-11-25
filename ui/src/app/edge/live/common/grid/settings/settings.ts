import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { FormlyModule } from "@ngx-formly/core";
import { MetaComponent } from "src/app/shared/components/edge/config-components/meta/meta";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyAdvancedFields, OeFormlyField, OeFormlyView, ViewContext, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { LiveDataService } from "../../../livedataservice";
import cz from "../i18n/cz.json";
import de from "../i18n/de.json";
import en from "../i18n/en.json";
import es from "../i18n/es.json";
import fr from "../i18n/fr.json";
import ja from "../i18n/ja.json";
import lt from "../i18n/lt.json";
import nl from "../i18n/nl.json";
import sv from "../i18n/sv.json";

@Component({
    selector: "oe-common-grid-settings",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [FormlyModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [{ provide: DataService, useClass: LiveDataService }],
})
export class CommonGridSettingsComponent extends AbstractFormlyComponent {
    private static readonly CHANNEL_ID_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED = "IsEssChargeFromGridAllowed";
    private static readonly CHANNEL_ID_META_IS_ESS_DISCHARGE_TO_GRID_ALLOWED = "IsEssDischargeToGridAllowed";
    private static readonly FORM_CONTROL_IS_ESS_CHARGE_FROM_GRID_ALLOWED = "isEssChargeFromGridAllowed";
    private static readonly FORM_CONTROL_IS_ESS_DISCHARGE_TO_GRID_ALLOWED = "isEssDischargeToGridAllowed";

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    protected override getFormGroup(): FormGroup {
        return new FormGroup({
            [CommonGridSettingsComponent.FORM_CONTROL_IS_ESS_CHARGE_FROM_GRID_ALLOWED]: new FormControl(null),
            [CommonGridSettingsComponent.FORM_CONTROL_IS_ESS_DISCHARGE_TO_GRID_ALLOWED]: new FormControl(null),
        });
    }

    protected override async generateView(viewContext: ViewContext): Promise<OeFormlyView> {
        const translations = await Language.normalizeAdditionalTranslationFiles({
            cs: cz,
            de: de,
            en: en,
            es: es,
            fr: fr,
            ja: ja,
            lt: lt,
            nl: nl,
            sv: sv,
        });
        for (const { lang, translation, shouldMerge } of translations) {
            viewContext.translate.setTranslation(lang, translation, shouldMerge);
        }

        const lines: OeFormlyField[] = [
            OeFormlyAdvancedFields.SECTION_TITLE_LINE(viewContext.translate.instant("GRID_SETTINGS.PERMISSIONS")),
            {
                type: "toggle-line",
                controlName: CommonGridSettingsComponent.FORM_CONTROL_IS_ESS_CHARGE_FROM_GRID_ALLOWED,
                name: viewContext.translate.instant("EDGE.INDEX.GRID.CHARGE_FROM_GRID_ACTIVATE"),
            },
            {
                type: "info-line",
                html: viewContext.translate.instant("EDGE.INDEX.GRID.CHARGE_FROM_GRID_DESCRIPTION"),
                lineStyle: "color: var(--ion-text-color-secondary);",
                hide: (field) =>
                    field[CommonGridSettingsComponent.FORM_CONTROL_IS_ESS_CHARGE_FROM_GRID_ALLOWED] !== true,
            },
            {
                type: "toggle-line",
                controlName: CommonGridSettingsComponent.FORM_CONTROL_IS_ESS_DISCHARGE_TO_GRID_ALLOWED,
                name: viewContext.translate.instant("EDGE.INDEX.GRID.DISCHARGE_TO_GRID_ACTIVATE"),
            },
            {
                type: "info-line",
                html: viewContext.translate.instant("EDGE.INDEX.GRID.DISCHARGE_TO_GRID_DESCRIPTION"),
                lineStyle: "color: var(--ion-text-color-secondary);",
                hide: (field) =>
                    field[CommonGridSettingsComponent.FORM_CONTROL_IS_ESS_DISCHARGE_TO_GRID_ALLOWED] !== true,
            },
        ];

        return {
            title: viewContext.translate.instant("MENU.SETTINGS"),
            useCiFont: true,
            lines,
            component: new MetaComponent(viewContext.config),
            edge: viewContext.edge,
        };
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return [
            new ChannelAddress("_meta", CommonGridSettingsComponent.CHANNEL_ID_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED),
            new ChannelAddress("_meta", CommonGridSettingsComponent.CHANNEL_ID_META_IS_ESS_DISCHARGE_TO_GRID_ALLOWED),
        ];
    }

    protected override onCurrentData(currentData: CurrentData): void {
        this.setFormControlSafelyWithBooleanChannel(
            this.form(),
            CommonGridSettingsComponent.FORM_CONTROL_IS_ESS_CHARGE_FROM_GRID_ALLOWED,
            currentData,
            new ChannelAddress("_meta", CommonGridSettingsComponent.CHANNEL_ID_META_IS_ESS_CHARGE_FROM_GRID_ALLOWED),
        );
        this.setFormControlSafelyWithBooleanChannel(
            this.form(),
            CommonGridSettingsComponent.FORM_CONTROL_IS_ESS_DISCHARGE_TO_GRID_ALLOWED,
            currentData,
            new ChannelAddress("_meta", CommonGridSettingsComponent.CHANNEL_ID_META_IS_ESS_DISCHARGE_TO_GRID_ALLOWED),
        );
    }
}
