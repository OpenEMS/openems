import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { IonicModule } from "@ionic/angular";
import { FormlyModule } from "@ngx-formly/core";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { Converter } from "src/app/shared/components/shared/converter";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, EdgeConfig } from "src/app/shared/shared";
import { CurrentDataUtils } from "src/app/shared/type/currentdata";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

@Component({
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: true,
    imports: [
        CommonModule,
        IonicModule,
        ReactiveFormsModule,
        FormlyModule,
        TranslateModule,
    ],
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class ChannelthresholdHomeComponent extends AbstractFormlyComponent {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";
    private route: ActivatedRoute = inject(ActivatedRoute);

    public static getFormlyGeneralView(translate: TranslateService, component: EdgeConfig.Component): OeFormlyView {
        const lines: OeFormlyField[] = [];
        const outputChannelAddress = component.getPropertyFromComponent<string>("outputChannelAddress");

        if (outputChannelAddress != null) {
            lines.push(
                {
                    type: "value-from-channels-line",
                    name: component.alias,
                    value: (currentData: CurrentData) => {
                        const outputChannelAddress = component.getPropertyFromComponent<string>("outputChannelAddress");
                        const channel = CurrentDataUtils.getChannel(ChannelAddress.fromStringSafely(outputChannelAddress), currentData.allComponents) as string;

                        return Converter.ON_OFF(translate)(channel);
                    },
                    channelsToSubscribe: [
                        ChannelAddress.fromString(outputChannelAddress),
                    ],
                }
            );
        }

        return {
            title: component.alias,
            lines: lines,
            icon: { name: "radio-button-on-outline", color: "normal", size: "normal" },
            component: component,
        };
    }

    protected override generateView(): OeFormlyView {
        const edge = this.service.currentEdge();
        const config = edge.getCurrentConfig();
        AssertionUtils.assertIsDefined(config);

        const component = config.getComponentSafely(this.route.snapshot.params.componentId);
        AssertionUtils.assertIsDefined(component);

        return ChannelthresholdHomeComponent.getFormlyGeneralView(this.translate, component);
    }
}
