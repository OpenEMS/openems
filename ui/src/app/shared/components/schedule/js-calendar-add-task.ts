import { ChangeDetectorRef, Directive, inject } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController, ViewWillEnter } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { RouteService } from "src/app/shared/service/route.service";
import { Websocket, Service } from "../../shared";
import { Language } from "../../type/language";
import { AbstractModal } from "../modal/abstractModal";
import { NavigationService } from "../navigation/service/navigation.service";
import { NavigationTree } from "../navigation/shared";
import de from "./i18n/de.json";
import en from "./i18n/en.json";

@Directive()
export class JsCalendarAddTaskComponent extends AbstractModal implements ViewWillEnter {
    protected routeService = inject(RouteService);
    protected navigationService = inject(NavigationService);

    constructor(
        protected override websocket: Websocket,
        protected override route: ActivatedRoute,
        protected override service: Service,
        public override modalController: ModalController,
        protected override translate: TranslateService,
        public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef,
    ) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }
    public async ionViewWillEnter(): Promise<void> {
        const componentId = this.routeService.getRouteParam<string>("componentId");
        const config = await this.service.getConfig();
        this.component = config.getComponentSafely(componentId);

        const newNavigationTree = new NavigationTree(
            "add-task",
            { baseString: "schedule/add-task" },
            { name: "add-outline" },
            this.translate.instant("JS_SCHEDULE.ADD"),
            "label",
            [],
            null
        );

        this.navigationService.setChildNavigationToCurrentNavigation(newNavigationTree);
    }
}
