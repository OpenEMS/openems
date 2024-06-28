import { Location } from "@angular/common";
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, HostListener, Input, QueryList, ViewChild, ViewChildren } from "@angular/core";
import { PopoverController } from "@ionic/angular";

export type NavigationOption = {
  id: string,
  callback: () => void,
  alias?: string,
};

@Component({
  selector: 'oe-footer-subnavigation',
  templateUrl: 'footerNavigation.html',
})
export class FooterNavigationComponent implements AfterViewInit {

  @ViewChildren('subnavigationbuttons', { read: ElementRef })
  public subnavigationbuttons!: QueryList<ElementRef>;
  @ViewChild('container', { read: ElementRef }) public container!: ElementRef;

  @Input() public backButton: boolean = false;
  @Input() public set navigationOptions(nodes: NavigationOption[]) {
    this._buttons = nodes;
    this.buttons = nodes;
  }

  protected areButtonsReadyToShow: boolean = false;
  protected buttons: NavigationOption[] = [];
  protected popoverButtons: NavigationOption[] | null = [];
  protected showPopover: boolean = false;

  private _buttons: NavigationOption[] = [];

  private static readonly INTERVAL: number = 1000;

  constructor(
    protected location: Location,
    protected popoverCtrl: PopoverController,
    private cdr: ChangeDetectorRef,
  ) {
  }

  ngAfterViewInit() {
    this.cdr.detectChanges();
    this.initializeFooterSubnavigation();
  }

  protected togglePopover(popoverbtn: NavigationOption) {
    popoverbtn.callback();
    this.showPopover = false;
  }

  @HostListener('window:resize', ['$event.target.innerWidth'])
  private onResize(width: number) {
    this.initializeFooterSubnavigation();
  }


  /**
   * Initializes sub-navigation
   */
  private initializeFooterSubnavigation(): void {
    this.buttons = this._buttons;
    this.getSplitIndex()
      .then((indexToSplit) => {

        if (indexToSplit == null) {
          return;
        }

        this.buttons = this._buttons.slice(0, indexToSplit);
        this.popoverButtons = this._buttons.slice(indexToSplit);
        this.areButtonsReadyToShow = true;
      });
  }

  /**
   * Gets the split index for navigation buttons
   *
   * @returns a promise
   */
  private async getSplitIndex(): Promise<number> {
    return new Promise<number>((resolve) => {
      let indexToSplit: number = 0;

      const interval = setInterval(() => {
        if (this.subnavigationbuttons && this.container) {

          const colLeftPadding = 16;
          const paddingLeftRight = 24;
          const ionItemWidth = this.container?.nativeElement.offsetWidth - colLeftPadding;
          if (ionItemWidth) {

            let sum: number = colLeftPadding;
            this.subnavigationbuttons.forEach((b, index, el) => {
              sum += b.nativeElement.offsetWidth + paddingLeftRight;
              if ((ionItemWidth) > sum) {
                indexToSplit = index;
              }
            });

            // Workaround
            if (ionItemWidth > sum) {
              ++indexToSplit;
            }

            clearInterval(interval);
            resolve(indexToSplit);
          }
        }
      }, FooterNavigationComponent.INTERVAL);
    });
  }
}
