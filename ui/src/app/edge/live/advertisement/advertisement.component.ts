import { ActivatedRoute } from '@angular/router';
import { Component, ViewChild } from '@angular/core';
import { Edge, Service } from '../../../shared/shared';
import { ModalController, IonSlides } from '@ionic/angular';

@Component({
  selector: AdvertisementComponent.SELECTOR,
  templateUrl: './advertisement.component.html'
})


export class AdvertisementComponent {

  @ViewChild('slider', { static: true }) slides: IonSlides;

  private static readonly SELECTOR = "advertisement";

  private edge: Edge = null;


  slideOpts = {
    initialSlide: 1,
    speed: 1600
  };

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
  ) { }

  ngOnInit() {
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    })
  }

  slidesDidLoad(slides: IonSlides) {
    slides.startAutoplay();
  }

  swipeNext() {
    this.slides.slideNext();
    this.slides.startAutoplay();
  }

  swipePrevious() {
    this.slides.slidePrev()
    this.slides.startAutoplay();
  }
}
