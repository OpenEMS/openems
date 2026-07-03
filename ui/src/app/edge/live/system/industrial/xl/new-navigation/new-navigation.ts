import { Component, inject } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, ViewContext } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, Edge, EdgeConfig, EdgePermission, Websocket } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SystemIndustrialJsonRpcHelper } from "../../../system-industrial-json-rpc-helper";
import { ClearErrorRequest } from "../../jsonrpc/clearErrorRequest";
import { F2BUpdate } from "../../jsonrpc/f2bUpdate";
import { SystemErrorAcknowledge } from "../../jsonrpc/systemErrorAcknowledge";

@Component({
    selector: "oe-system-industrial-xl-home",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    imports: [
        CommonUiModule,
        FormlyModule,
    ],
})
export class SystemIndustrialXlHomeComponent extends AbstractFormlyComponent {

    private static readonly FACTORY_ID = "System.Fenecon.Industrial.Xl";
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    protected startStopChannel: ChannelAddress | null = null;

    private websocket: Websocket = inject(Websocket);
    private component: EdgeConfig.Component | null = null;

    public static getFormlyGeneralView(translate: TranslateService, edge: Edge, self: SystemIndustrialXlHomeComponent): OeFormlyView {

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
                        callback: self.clearErrorRequest(self),
                    },
                    name: translate.instant("EDGE.INDEX.WIDGETS.SYSTEM.INDUSTRIAL.CLEAR_COOLING_UNIT_ERRORS_HEADER") + " " + translate.instant("EDGE.INDEX.WIDGETS.SYSTEM.INDUSTRIAL.CLEAR_COOLING_UNIT_ERRORS_TEXT"),
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

        if (edge.roleIsAtLeast(Role.ADMIN)) {
            lines.push(
                { type: "horizontal-line" },
                {
                    type: "button-from-form-control-line",
                    button: {
                        icon: { name: "send-outline", color: "primary", size: "small" },
                        name: translate.instant("EDGE.INDEX.WIDGETS.SYSTEM.EXECUTE"),
                        value: true,
                        callback: self.f2bUpdate(self),
                    },
                    name: translate.instant("EDGE.INDEX.WIDGETS.SYSTEM.INDUSTRIAL.F2B_UPDATE_HEADER"),
                });
        }

        return {
            title: "FENECON Industrial XL",
            helpKey: "REDIRECT.SYSTEM.INDUSTRIAL_XL_MANUAL",
            lines: lines,
            component: self.component!,
            edge,
        };
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const config = await this.service.getConfig();
        this.component = config.getFirstComponentByFactoryId(SystemIndustrialXlHomeComponent.FACTORY_ID);

        AssertionUtils.assertIsDefined(this.component);
        this.startStopChannel = new ChannelAddress(this.component.id, "_PropertyStartStop");
        return Promise.resolve([this.startStopChannel]);
    }

    protected override async generateView(viewContext: ViewContext): Promise<OeFormlyView> {
        AssertionUtils.assertIsDefined(viewContext.config);
        this.component = viewContext.config.getFirstComponentByFactoryId(SystemIndustrialXlHomeComponent.FACTORY_ID);
        return SystemIndustrialXlHomeComponent.getFormlyGeneralView(this.translate, viewContext.edge, this);
    }

    protected override getFormGroup(): FormGroup {
        const config = this.service.currentEdge()?.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);
        this.component = config.getFirstComponentByFactoryId(SystemIndustrialXlHomeComponent.FACTORY_ID);

        return new FormGroup({
            startStop: new FormControl(this.component?.properties.startStop),
        });
    }

    protected clearErrorRequest(self: SystemIndustrialXlHomeComponent): () => void {
        return SystemIndustrialJsonRpcHelper.createRpcCallback(
            self.service,
            self.translate,
            self.websocket,
            self.service.currentEdge(),
            self.component,
            new ClearErrorRequest()
        );
    }

    protected systemErrorAcknowledge(self: SystemIndustrialXlHomeComponent): () => void {
        return SystemIndustrialJsonRpcHelper.createRpcCallback(
            self.service,
            self.translate,
            self.websocket,
            self.service.currentEdge(),
            self.component,
            new SystemErrorAcknowledge()
        );
    }

    protected f2bUpdate(self: SystemIndustrialXlHomeComponent): () => void {
        return SystemIndustrialJsonRpcHelper.createRpcCallback(
            self.service,
            self.translate,
            self.websocket,
            self.service.currentEdge(),
            self.component,
            new F2BUpdate()
        );
    }
}
