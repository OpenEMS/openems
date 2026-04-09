import { ChangeDetectorRef, Directive, Inject, inject } from "@angular/core";
import { FormBuilder } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { RouteService } from "src/app/shared/service/route.service";
import { Service, Websocket } from "../../shared";
import { Language } from "../../type/language";
import { AbstractModal } from "../modal/abstractModal";
import { NavigationService } from "../navigation/service/navigation.service";
import de from "./i18n/de.json";
import en from "./i18n/en.json";

@Directive()
export class JsCalendarEditTaskComponent extends AbstractModal {
    protected routeService = inject(RouteService);
    protected navigationService = inject(NavigationService);

    constructor(
        @Inject(Websocket) protected override websocket: Websocket,
        @Inject(ActivatedRoute) protected override route: ActivatedRoute,
        @Inject(Service) protected override service: Service,
        @Inject(ModalController) public override modalController: ModalController,
        @Inject(TranslateService) protected override translate: TranslateService,
        @Inject(FormBuilder) public override formBuilder: FormBuilder,
        public override ref: ChangeDetectorRef,
    ) {
        super(websocket, route, service, modalController, translate, formBuilder, ref);
        Language.normalizeAdditionalTranslationFiles({ de: de, en: en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });
    }
}
