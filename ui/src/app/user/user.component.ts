import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { environment } from 'src/environments';
import { Service, Websocket } from '../shared/shared';
import { Language, LanguageTag } from '../shared/translate/language';

@Component({
  selector: 'user',
  templateUrl: './user.component.html'
})
export class UserComponent {

  public environment = environment;

  public readonly languages: LanguageTag[];
  public currentLanguage: LanguageTag;

  constructor(
    public translate: TranslateService,
    public service: Service,
    private route: ActivatedRoute,
    private websocket: Websocket,
  ) {
    this.languages = Language.getLanguageTags();
  }

  ngOnInit() {
    this.currentLanguage = this.translate.currentLang as LanguageTag;
    this.service.setCurrentComponent(this.translate.instant('Menu.user'), this.route);
  }

  /**
   * Logout from OpenEMS Edge or Backend.
   */
  public doLogout() {
    this.websocket.logout();
  }

  public toggleDebugMode(event: CustomEvent) {

    sessionStorage.setItem("DEBUGMODE", event.detail['checked'])
    this.environment.debugMode = event.detail['checked'];
  }

  public setLanguage(language: LanguageTag): void {
    this.currentLanguage = language;
    this.translate.use(language);
  }
}
