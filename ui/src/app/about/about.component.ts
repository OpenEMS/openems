import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Edge, Service } from '../shared/shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'about',
  templateUrl: './about.component.html'
})
export class AboutComponent {

  constructor(
    private translate: TranslateService,
    private route: ActivatedRoute,
    private service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent(this.translate.instant('Menu.aboutUI'), this.route);
  }

}
