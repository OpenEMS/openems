import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { ButtonLabel } from "src/app/shared/components/modal/modal-button/modal-button";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, ViewContext, } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { ControllerEvseSingleShared } from "../../shared/shared";

@Component({
    selector: "oe-controller-evse-pages-phase-switching",
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
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
export class EvsePhaseSwitchingComponent extends AbstractFormlyComponent {
    public static formControlName: string = "phaseSwitching";
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    private controller: EdgeConfig.Component | null = null;
    private phaseSwitchingChannel: ChannelAddress | null = null;
    constructor(
        protected override service: Service,
        private route: ActivatedRoute,
    ) {
        super();
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
                type: "image-line",
                img: {
                    url: "assets/img/phasenumschaltung.svg",
                    width: 100,
                    style: {
                        maxWidth: "30rem",
                        justifySelf: "center",
                        paddingBottom: "var(--ion-padding)",
                    },
                },
            },
            {
                type: "info-line",
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.PHASE_SWITCHING_INFO"),
                style: {
                    name: {
                        fontWeight: "bold",
                        textAlign: "center",
                        fontSize: "1rem",
                        paddingBottom: "var(--ion-padding)",
                    },
                },
            },
            {
                type: "radio-buttons-from-form-control-line",
                name: "phase-switching",
                controlName: EvsePhaseSwitchingComponent.formControlName, // propertyname
                buttons: EvsePhaseSwitchingComponent.getPhaseSwitchingButtons(translate, edge),
            },
        ];

        return {
            title: Name.METER_ALIAS_OR_ID(component),
            lines: lines,
            component: component,
            edge: edge,
        };
    }

    public static getPhaseSwitchingButtons = (translate: TranslateService, edge: Edge): ButtonLabel[] => {
        const buttons: ButtonLabel[] = [
            {
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.FORCE_SINGLE_PHASE"),
                value: PhaseSwitching.FORCE_SINGLE_PHASE,
                style: {
                    color: "red",
                    fontWeight: "bold",
                },
                icon: { name: "oe-phase-switching-1", color: "var(--ion-color-text)", size: "large" },
            },
            {
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.FORCE_THREE_PHASE"),
                value: PhaseSwitching.FORCE_THREE_PHASE,
                icon: { name: "oe-phase-switching-3", color: "var(--ion-color-text)", size: "large" },
            },
        ];

        if (ControllerEvseSingleShared.hasAutomaticPhaseSwitching(edge)) {
            buttons.push({
                name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.AUTOMATIC_SWITCHING"),
                value: PhaseSwitching.AUTOMATIC_SWITCHING,
                description: translate.instant("EDGE.INDEX.WIDGETS.EVCS.AUTOMATIC_SWITCHING_DESCRIPTION"),
                icon: [
                    { name: "oe-phase-switching-1", color: "var(--ion-color-text)", size: "large" },
                    { name: "oe-phase-switching-3", color: "var(--ion-color-text)", size: "large" },
                ],
            });
        }

        return buttons;
    };

    protected override onCurrentData(currentData: CurrentData): void {
        this.setFormControlSafelyWithChannel<number>(
            this.form,
            EvsePhaseSwitchingComponent.formControlName,
            currentData,
            this.phaseSwitchingChannel,
        );
    }

    protected override generateView(viewContext: ViewContext): OeFormlyView {
        this.controller = viewContext.config.getComponent(this.route.snapshot.params.componentId);
        return EvsePhaseSwitchingComponent.generateView(this.translate, this.controller, viewContext.edge);
    }

    protected override getFormGroup(): FormGroup {
        return new FormGroup({
            [EvsePhaseSwitchingComponent.formControlName]: new FormControl(null),
        });
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const config = await this.service.getConfig();
        this.controller = config.getComponent(this.route.snapshot.params.componentId);
        if (!this.controller || !this.controller.id) {
            return [];
        }
        this.phaseSwitchingChannel = new ChannelAddress(this.controller.id, "_PropertyPhaseSwitching");
        return [this.phaseSwitchingChannel];
    }
}

export enum PhaseSwitching {
    /** Phase-Switching is disabled. */
    DISABLE = "DISABLE", //
    /** Phase-Switching forced to SINGLE_PHASE. */
    FORCE_SINGLE_PHASE = "FORCE_SINGLE_PHASE", //
    /** Phase-Switching force to THREE_PHASE. */
    FORCE_THREE_PHASE = "FORCE_THREE_PHASE", //
    /** Phase-Switching in AUTOMATIC mode. (not implemented!). */
    AUTOMATIC_SWITCHING = "AUTOMATIC", //
}
