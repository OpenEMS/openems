import { ActivatedRoute } from '@angular/router';
import { Component, ViewChild } from '@angular/core';
import { Edge, Service, EdgeConfig } from '../../../shared/shared';
import { ModalController, IonSlides } from '@ionic/angular';

@Component({
  selector: AdvertisementComponent.SELECTOR,
  templateUrl: './advertisement.component.html'
})

export class AdvertisementComponent {

  @ViewChild('slides', { static: true }) slides: IonSlides;

  public edge: Edge;
  public config: EdgeConfig;

  private static readonly SELECTOR = "advertisement";

  public enableBtn: boolean = false;
  public disablePrevBtn: boolean = true;
  public disableNextBtn: boolean = false;

  slideOpts = {
    initialSlide: 1,
    speed: 5000,
    allowTouchMove: false,
    preventClicks: false,
    preventClicksPropagation: false,
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
        // gets the length of slides (slides.length() sometimes returns 0 after initializing)
        let length: number = 0;
        if (edge.producttype == 'MiniES 3-3') {
          length += 1;
        }
        if (config.widgets.names.includes('io.openems.edge.evcs.api.Evcs') == false) {
          length += 1;
        }
        // sets the prev/next button
        if (length > 1) {
          this.enableBtn = true;
        } else {
          this.enableBtn = false;
        }
        this.slides.update();
      });
    })
  }

  ngOnDestroy() {
    this.slides.stopAutoplay();
  }

  slidesDidLoad(slider: IonSlides) {
    slider.startAutoplay();
  }

  changeSlides() {
    this.slides.update();

    // checks if slide is first/last element and sets the next/previous button accordingly
    let isBeginning = this.slides.isBeginning();
    let isEnd = this.slides.isEnd();
    Promise.all([isBeginning, isEnd]).then((data) => {
      data[0] ? this.disablePrevBtn = true : this.disablePrevBtn = false;
      data[1] ? this.disableNextBtn = true : this.disableNextBtn = false;
    });
  }

  swipeNext() {
    this.slides.slideNext()
  }

  swipePrevious() {
    this.slides.slidePrev()
  }
}
