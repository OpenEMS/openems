import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../../environments';
import { LanguageTag, Language } from '../shared/translate/language';
import { Router } from '@angular/router';
import { Alerts } from '../shared/service/alerts';

@Component({
  selector: 'settings',
  templateUrl: './settings.component.html'
})
export class SettingsComponent {

  public env = environment;

  public readonly languages: LanguageTag[];
  public currentLanguage: LanguageTag;

  constructor(
    public translate: TranslateService,
    private alerts: Alerts,
  ) {
    this.languages = Language.getLanguageTags();
    this.currentLanguage = translate.currentLang as LanguageTag;
  }

  public toggleDebugMode(event: CustomEvent) {
    this.env.debugMode = event.detail['checked'];
  }

  public setLanguage(language: LanguageTag): void {
    this.currentLanguage = language;
    this.translate.use(language);
  }

  public clearLogin() {
    this.alerts.confirmLoginDelete();
  }
}
