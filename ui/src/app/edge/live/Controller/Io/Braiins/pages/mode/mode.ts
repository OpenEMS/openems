import { Component, inject, ChangeDetectionStrategy } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, ViewContext, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ControllerBraiinsShared } from "../../shared/shared";

@Component({
    selector: "oe-controller-braiins-mode",
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
export class ControllerBraiinsModeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    protected component: EdgeConfig.Component | null = null;
    protected modeChannel: ChannelAddress | null = null;
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
                name: translate.instant("BRAIINS_SINGLE.MODE.SELECT_PROMPT"),
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
                name: "mode-selection",
                controlName: "mode",
                buttons: [
                    {
                        name: translate.instant("BRAIINS_SINGLE.MODE.ON"),
                        value: ControllerBraiinsShared.Mode.ON,
                    },
                    {
                        name: translate.instant("BRAIINS_SINGLE.MODE.OFF"),
                        value: ControllerBraiinsShared.Mode.OFF,
                    },
                ],
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
        this.setFormControlSafelyWithChannel<number>(this.form, "mode", currentData, this.modeChannel);
    }

    protected override generateView(viewContext: ViewContext): OeFormlyView {
        this.component = viewContext.config.getComponent(this.componentId);
        return ControllerBraiinsModeComponent.generateView(viewContext.translate, this.component, viewContext.edge);
    }

    protected override getFormGroup(): FormGroup {
        AssertionUtils.assertIsDefined(this.component);
        return new FormGroup({
            mode: new FormControl(this.component.properties.mode),
        });
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const componentId = this.component?.id ?? this.componentId;
        if (componentId == null) {
            return [];
        }

        this.modeChannel = new ChannelAddress(componentId, ControllerBraiinsShared.PROPERTY_MODE);
        return [this.modeChannel];
    }
}
