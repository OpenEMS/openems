import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { TranslateService } from "@ngx-translate/core";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, } from "src/app/shared/components/shared/oe-formly-component";
import { Edge, EdgeConfig, Websocket } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { WeatherPlainComponent } from "./plain-modal";

@Component({
    selector: "oe-weather",
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false,
})
export class WeatherHomeComponent extends AbstractFormlyComponent {
    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private route: ActivatedRoute = inject(ActivatedRoute);
    private websocket: Websocket = inject(Websocket);

    public static getFormlyGeneralView(
        translate: TranslateService,
        component: EdgeConfig.Component,
        edge: Edge,
        websocket: Websocket,
    ): OeFormlyView {
        const lines: OeFormlyField[] = [];

        const meta = edge.getConfig(websocket).value.getComponentsByFactory("Core.Meta")[0];
        const placeName = meta.getPropertyFromComponent("placeName") ?? null;
        const pageTitle = placeName
            ? translate.instant("TITLE_WITH_LOCATION", { location: placeName })
            : translate.instant("TITLE");

        lines.push({
            type: "component-line",
            component: WeatherPlainComponent,
            inputs: {
                component: component,
            },
        });

        return {
            title: pageTitle,
            icon: { name: "oe-partly-cloudy-day", color: "normal", size: "large" },
            helpKey: "REDIRECT.WEATHER_WIDGET",
            lines: lines,
            component: component,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = config.getComponentSafely(this.route.snapshot.params.componentId);
        AssertionUtils.assertIsDefined(component);

        return WeatherHomeComponent.getFormlyGeneralView(this.translate, component, edge, this.websocket);
    }
}
