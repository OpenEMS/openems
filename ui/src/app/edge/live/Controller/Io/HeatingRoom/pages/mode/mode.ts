import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, ViewContext, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedControllerIoHeatingRoom } from "../../shared/shared";

@Component({
    selector: "oe-controller-io-heating-room-mode",
    templateUrl: "../../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    changeDetection: ChangeDetectionStrategy.Eager,
    styles: [
        `
            ::ng-deep formly-form {
                height: 100% !important;
            }
        `,
    ],
})
export class ControllerIoHeatingRoomModeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    protected component: EdgeConfig.Component | null = null;
    private readonly route: ActivatedRoute = inject(ActivatedRoute);

    private get componentId(): string {
        return this.route.snapshot.params.componentId;
    }

    public static generateView(
        translate: TranslateService,
        component: EdgeConfig.Component | null,
        edge: Edge | null,
    ): OeFormlyView {
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);

        const lines: OeFormlyField[] = [
            {
                type: "info-line",
                name: translate.instant("IO_HEATING_ROOM.MODE.TITLE"),
                style: {
                    name: {
                        fontWeight: "bold",
                        textAlign: "center",
                        fontSize: "1rem",
                        paddingBottom: "calc(var(--ion-padding) * 4)",
                    },
                },
            },
            {
                type: "radio-buttons-from-form-control-line",
                name: "heating-mode",
                controlName: "mode",
                buttons: SharedControllerIoHeatingRoom.getModeButtons(translate),
            },
        ];

        return {
            title: Name.METER_ALIAS_OR_ID(component),
            lines: lines,
            component: component,
            edge: edge,
        };
    }

    protected override onCurrentData(currentData: CurrentData): void {
        if (this.component == null) {
            return;
        }
        this.setFormControlSafelyWithChannel<string>(
            this.form(),
            "mode",
            currentData,
            new ChannelAddress(this.component.id, SharedControllerIoHeatingRoom.PROPERTY_MODE),
        );
    }

    protected override generateView(viewContext: ViewContext): OeFormlyView {
        this.component = viewContext.config.getComponent(this.componentId);
        return ControllerIoHeatingRoomModeComponent.generateView(
            viewContext.translate,
            this.component,
            viewContext.edge,
        );
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerIoHeatingRoom.getFormGroup(this.component);
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const config = await this.service.getConfig();
        const componentId = this.component?.id ?? this.componentId;
        this.component = config.getComponent(componentId);

        if (this.component?.id == null) {
            return [];
        }

        return SharedControllerIoHeatingRoom.getChannelAddresses(this.component);
    }
}
