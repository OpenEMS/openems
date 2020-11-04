import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { environment } from '../../environments';
import { Language, LanguageTag } from '../shared/translate/language';
import { Service } from '../shared/shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'user',
  templateUrl: './user.component.html'
})
export class UserComponent {

  public env = environment;

  public readonly languages: LanguageTag[];
  public currentLanguage: LanguageTag;

  constructor(
    private translate: TranslateService,
    private route: ActivatedRoute,
    private service: Service,
  ) {
    this.languages = Language.getLanguageTags();
    this.currentLanguage = translate.currentLang as LanguageTag;
  }

  ngOnInit() {
    this.service.setCurrentComponent(this.translate.instant('Menu.user'), this.route);
  }

  public toggleDebugMode(event: CustomEvent) {
    this.env.debugMode = event.detail['checked'];
  }

  public setLanguage(language: LanguageTag): void {
    this.currentLanguage = language;
    this.translate.use(language);
  }

}
