import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { RouteService } from "src/app/shared/service/route.service";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { SharedControllerBraiins } from "../shared/shared";

@Component({
    selector: "oe-braiins",
    templateUrl:
        "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    providers: [{ provide: DataService, useClass: LiveDataService }],
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
    ],
})
export class ControllerBraiinsHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper:
        | "formly-field-modal"
        | "formly-field-navigation" = "formly-field-navigation";
    private component: EdgeConfig.Component | null = null;

    private readonly routeService: RouteService = inject(RouteService);

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();

        AssertionUtils.assertIsDefined(config);
        const component = config.getComponentSafely(
            this.routeService.getRouteParam("componentId"),
        );
        AssertionUtils.assertIsDefined(component);
        this.component = component;

        return SharedControllerBraiins.getFormlyView(
            this.translate,
            component,
            edge,
        );
    }

    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        return SharedControllerBraiins.getChannelAddresses(
            this.service,
            this.routeService,
            this.component,
        );
    }

    protected override onCurrentData(currentData: CurrentData): void {
        const braiinsComponent = this.component;
        AssertionUtils.assertIsDefined(braiinsComponent);

        this.setFormControlSafelyWithChannel(
            this.form,
            "mode",
            currentData,
            new ChannelAddress(braiinsComponent.id, "_PropertyMode"),
        );
    }

    protected override getFormGroup(): FormGroup {
        return SharedControllerBraiins.getFormGroup();
    }
}
