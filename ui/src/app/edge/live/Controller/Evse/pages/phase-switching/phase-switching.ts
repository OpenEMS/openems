import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
    styles: [`
        ::ng-deep formly-form{
            height: 100% !important;
        }`,
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

    public static generateView(translate: TranslateService, component: EdgeConfig.Component | null, edge: Edge | null): OeFormlyView {
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
                style: "font-weight: bold; text-align: center; font-size: 1rem; padding-bottom: calc(var(--ion-padding) * 4)",
            },
            {
                type: "radio-buttons-from-form-control-line",
                name: "phase-switching",
                controlName: EvsePhaseSwitchingComponent.formControlName, // propertyname
                buttons: [
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.FORCE_SINGLE_PHASE"),
                        value: PhaseSwitching.FORCE_SINGLE_PHASE,
                        style: {
                            "color": "red",
                            "fontWeight": "bold",
                        },
                    },
                    {
                        name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.FORCE_THREE_PHASE"),
                        value: PhaseSwitching.FORCE_THREE_PHASE,
                    },
                    /* {
                        name: translate.instant("EDGE.INDEX.WIDGETS.EVCS.AUTOMATIC_SWITCHING"),
                        value: PhaseSwitching.AUTOMATIC_SWITCHING, // not implemented yet
                        disabled: true,
                    },*/
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
        this.setFormControlSafelyWithChannel<number>(this.form, EvsePhaseSwitchingComponent.formControlName, currentData, this.phaseSwitchingChannel);
    }

    protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
        this.controller = config.getComponent(this.route.snapshot.params.componentId);
        const edge = this.service.currentEdge();
        return EvsePhaseSwitchingComponent.generateView(this.translate, this.controller, edge);
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
    /**
     * Phase-Switching is disabled.
     */
    DISABLE = "DISABLE", //
    /**
     * Phase-Switching forced to SINGLE_PHASE.
     */
    FORCE_SINGLE_PHASE = "FORCE_SINGLE_PHASE", //
    /**
     * Phase-Switching force to THREE_PHASE.
     */
    FORCE_THREE_PHASE = "FORCE_THREE_PHASE", //
    /**
     * Phase-Switching in AUTOMATIC mode. (not implemented!).
     */
    AUTOMATIC_SWITCHING = "AUTOMATIC_SWITCHING", //
}
