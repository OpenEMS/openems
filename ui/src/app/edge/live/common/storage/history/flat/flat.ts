import { Component, ChangeDetectionStrategy } from "@angular/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";

@Component({
    selector: "common-storage-widget",
    templateUrl: "./flat.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, ComponentsBaseModule],
})
export class FlatComponent extends AbstractFlatWidget {}
