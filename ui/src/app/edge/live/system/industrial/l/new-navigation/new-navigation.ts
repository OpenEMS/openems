import { Component, inject } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, ViewContext } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, Edge, EdgeConfig, EdgePermission, Websocket } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import { Role } from "src/app/shared/type/role";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SystemIndustrialJsonRpcHelper } from "../../../system-industrial-json-rpc-helper";
import { BmsHardResetRequest } from "../../jsonrpc/bmsHardReset";
import { SystemErrorAcknowledge } from "../../jsonrpc/systemErrorAcknowledge";
import de from "../i18n/de.json";
import en from "../i18n/en.json";

@Component({
    selector: "oe-system-industrial-lhome",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    imports: [
        CommonUiModule,
        FormlyModule,
    ],
})
export class SystemIndustrialLHomeComponent extends AbstractFormlyComponent {

    private static readonly FACTORY_ID = "System.Fenecon.Industrial.L";
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    protected startStopChannel: ChannelAddress | null = null;

    private websocket: Websocket = inject(Websocket);
    private component: EdgeConfig.Component | null = null;

    constructor() {
        super();
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }

    public static getFormlyGeneralView(translate: TranslateService, edge: Edge, self: SystemIndustrialLHomeComponent): OeFormlyView {

        const lines: OeFormlyField[] = [
            {
                type: "name-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.SYSTEM.OVERALL_SYSTEM"),
            },
            {
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.SYSTEM.INDUSTRIAL.NOTE"),
                icon: { name: "information-outline", color: "primary", size: "small" },
                style: { name: { fontSize: "small" } },
            },
            {
                type: "buttons-from-form-control-line",
                name: translate.instant("GENERAL.MODE"),
                controlName: "startStop",
                buttons: [
                    {
                        name: "START",
                        value: "START",
                        icon: { color: "success", name: "play-outline", size: "medium" },
                    },
                    {
                        name: "STOP",
                        value: "STOP",
                        icon: { color: "danger", name: "stop-outline", size: "medium" },
                    },
                ],
            },
        ];

        if (edge.roleIsAtLeast(Role.INSTALLER)) {
            lines.push(
                { type: "horizontal-line" },
                {
                    type: "button-from-form-control-line",
                    button: {
                        icon: { name: "send-outline", color: "primary", size: "small" },
                        name: translate.instant("GENERAL.RESET"),
                        value: true,
                        callback: self.bmsHardReset(self),
                    },
                    name: translate.instant("EDGE.INDEX.WIDGETS.SYSTEM.INDUSTRIAL.BMS_HARD_RESET"),
                });
        }

        if (EdgePermission.hasSystemErrorAcknowledge(edge) && edge.roleIsAtLeast(Role.INSTALLER)) {
            lines.push(
                { type: "horizontal-line" },
                {
                    type: "button-from-form-control-line",
                    button: {
                        icon: { name: "send-outline", color: "primary", size: "small" },
                        name: translate.instant("GENERAL.RESET"),
                        value: true,
                        callback: self.systemErrorAcknowledge(self),
                    },
                    name: translate.instant("EDGE.INDEX.WIDGETS.SYSTEM.INDUSTRIAL.SYSTEM_ERROR_ACKNOWLEDGE_HEADER") + " " + translate.instant("EDGE.INDEX.WIDGETS.SYSTEM.INDUSTRIAL.SYSTEM_ERROR_ACKNOWLEDGE_TEXT"),
                });
        }

        return {
            title: "FENECON Industrial L",
            helpKey: "REDIRECT.SYSTEM.INDUSTRIAL_L_MANUAL",
            lines: lines,
            component: self.component!,
            edge,
        };
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const config = await this.service.getConfig();
        this.component = config.getFirstComponentByFactoryId(SystemIndustrialLHomeComponent.FACTORY_ID);

        AssertionUtils.assertIsDefined(this.component);
        this.startStopChannel = new ChannelAddress(this.component.id, "_PropertyStartStop");
        return Promise.resolve([this.startStopChannel]);
    }

    protected override generateView(viewContext: ViewContext): OeFormlyView {
        AssertionUtils.assertIsDefined(viewContext.config);
        this.component = viewContext.config.getFirstComponentByFactoryId(SystemIndustrialLHomeComponent.FACTORY_ID);
        return SystemIndustrialLHomeComponent.getFormlyGeneralView(this.translate, viewContext.edge, this);
    }

    protected override getFormGroup(): FormGroup {
        const config = this.service.currentEdge()?.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        this.component = config.getFirstComponentByFactoryId(SystemIndustrialLHomeComponent.FACTORY_ID);

        return new FormGroup({
            startStop: new FormControl(this.component?.properties.startStop),
        });
    }

    protected systemErrorAcknowledge(self: SystemIndustrialLHomeComponent): () => void {
        return SystemIndustrialJsonRpcHelper.createRpcCallback(
            self.service,
            self.translate,
            self.websocket,
            self.service.currentEdge(),
            self.component,
            new SystemErrorAcknowledge()
        );
    }

    protected bmsHardReset(self: SystemIndustrialLHomeComponent): () => void {
        return SystemIndustrialJsonRpcHelper.createRpcCallback(
            self.service,
            self.translate,
            self.websocket,
            self.service.currentEdge(),
            self.component,
            new BmsHardResetRequest()
        );
    }
}
