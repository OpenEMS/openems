import { ActivatedRoute } from '@angular/router';
import { Component, ViewChild } from '@angular/core';
import { Edge, Service, EdgeConfig, Widgets, WidgetNature } from '../../../shared/shared';
import { ModalController, IonSlides } from '@ionic/angular';
import { Observable, BehaviorSubject } from 'rxjs';

@Component({
  selector: AdvertisementComponent.SELECTOR,
  templateUrl: './advertisement.component.html'
})


export class AdvertisementComponent {

  @ViewChild('slider', { static: true }) slides: IonSlides;

  private static readonly SELECTOR = "advertisement";

  public edge: Edge = null;
  // public enableButtons: BehaviorSubject<boolean> = new BehaviorSubject(false);
  public config: EdgeConfig = null;
  public didInit: boolean = false;

  slideOpts = {
    initialSlide: 1,
    speed: 5000
  };

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
      this.service.getConfig().then(config => {
        this.config = config;
      });
    })
  }

  ngOnDestroy() {
    // this.enableButtons.complete();
  }

  ngAfterViewInit() {
    this.didInit = true;
  }

  slidesDidLoad(slider: IonSlides) {
    slider.startAutoplay();
    // slider.length().then(length => {
    //   if (length == 1) {
    //     this.enableButtons.next(false);
    //   } else {
    //     this.enableButtons.next(true);
    //   }
    // })
  }

  swipeNext() {
    // this.slides.updateAutoHeight();
    console.log("yup")
    this.slides.slideNext()
  }

  swipePrevious() {
    // this.slides.updateAutoHeight();
    this.slides.slidePrev()
  }
}
