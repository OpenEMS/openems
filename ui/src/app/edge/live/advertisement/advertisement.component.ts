import { ActivatedRoute } from '@angular/router';
import { AdvertWidgets } from 'src/app/shared/type/widget';
import { Component, ViewChild, Input, ChangeDetectorRef } from '@angular/core';
import { Edge, Service, EdgeConfig } from '../../../shared/shared';
import { ModalController, IonSlides } from '@ionic/angular';
import { environment } from 'src/environments';

@Component({
  selector: 'advertisement',
  templateUrl: './advertisement.component.html'
})

export class AdvertisementComponent {

  @Input() public advertWidgets: AdvertWidgets;

  @ViewChild('slides', { static: true }) public slides: IonSlides;

  public edge: Edge;
  public config: EdgeConfig;
  public environment = environment;
  public activeIndex: number;
  public title: string = environment.edgeShortName + ' - App';

  public enableBtn: boolean = false;
  public disablePrevBtn: boolean = null;
  public disableNextBtn: boolean = null;

  slideOpts = {
    allowTouchMove: false,
    initialSlide: 0,
    preventClicks: false,
    preventClicksPropagation: false,
    speed: 5000,
  }

  constructor(
    private route: ActivatedRoute,
    public modalCtrl: ModalController,
    public service: Service,
    private cdref: ChangeDetectorRef
  ) { }

  ngAfterContentChecked() {
    this.cdref.detectChanges();
  }

  ngOnInit() {
    this.slides.getActiveIndex().then((index: number) => {
      this.activeIndex = index;
    });
    this.service.setCurrentComponent('', this.route).then(edge => {
      this.edge = edge;
    })
    // enables or disables nav buttons generally
    if (this.advertWidgets.names.length > 1) {
      this.enableBtn = true;
      this.disablePrevBtn = true;
      this.disableNextBtn = false;
    } else {
      this.enableBtn = false;
    }
  }

  ngOnDestroy() {
    this.slides.stopAutoplay();
  }

  slidesDidLoad(slider: IonSlides) {
    slider.startAutoplay();
  }

  changeSlides() {
    this.slides.getActiveIndex().then((index: number) => {
      this.activeIndex = index;
    });
    this.slides.update();
  }

  changeNextPrevButtons() {
    // checks if slide is first/last element and sets the next/previous button accordingly
    if (this.enableBtn == true) {
      this.slides.getSwiper().then(swiper => {
        // not using isEnd/Beginning Promise because at 2 slides it sometimes returns false for both options
        let index = swiper.realIndex
        if (index == 0) {
          this.disablePrevBtn = true
          this.disableNextBtn = false
        } else if (index == this.advertWidgets.names.length - 1) {
          this.disablePrevBtn = false
          this.disableNextBtn = true
        } else {
          this.disablePrevBtn = false
          this.disableNextBtn = false
        }
      })
    }
  }

  swipeNext() {
    this.slides.slideNext()
  }

  swipePrevious() {
    this.slides.slidePrev()
  }
}
