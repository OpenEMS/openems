import { Location } from "@angular/common";
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, HostListener, Input, QueryList, ViewChild, ViewChildren } from "@angular/core";
import { PopoverController } from "@ionic/angular";

export type NavigationOption = {
  id: string,
  callback: () => void,
  alias?: string,
  isEnabled?: boolean,
};

@Component({
  selector: "oe-footer-subnavigation",
  templateUrl: "FOOTER_NAVIGATION.HTML",
  standalone: false,
})
export class FooterNavigationComponent implements AfterViewInit {

  private static readonly INTERVAL: number = 1000;

  @ViewChildren("subnavigationbuttons", { read: ElementRef })
  public subnavigationbuttons!: QueryList<ElementRef>;
  @ViewChild("container", { read: ElementRef }) public container!: ElementRef;
  @Input() public backButton: boolean = false;

  protected areButtonsReadyToShow: boolean = false;
  protected buttons: NavigationOption[] = [];
  protected popoverButtons: NavigationOption[] | null = [];
  protected showPopover: boolean = false;

  private _buttons: NavigationOption[] = [];

  constructor(
    protected location: Location,
    protected popoverCtrl: PopoverController,
    private cdr: ChangeDetectorRef,
  ) {
  }

  @Input() public set navigationOptions(nodes: NavigationOption[]) {
    this._buttons = nodes;
    THIS.BUTTONS = nodes;
  }
  @HostListener("window:resize", ["$EVENT.TARGET.INNER_WIDTH"])
  private onResize(width: number) {
    THIS.INITIALIZE_FOOTER_SUBNAVIGATION();
  }

  ngAfterViewInit() {
    THIS.CDR.DETECT_CHANGES();
    THIS.INITIALIZE_FOOTER_SUBNAVIGATION();
  }

  protected togglePopover(popoverbtn: NavigationOption) {
    POPOVERBTN.CALLBACK();
    THIS.SHOW_POPOVER = false;
  }

  /**
   * Initializes sub-navigation
   */
  private initializeFooterSubnavigation(): void {
    THIS.BUTTONS = this._buttons;
    THIS.GET_SPLIT_INDEX()
      .then((indexToSplit) => {

        if (indexToSplit == null) {
          return;
        }

        const allowedButtons = this._buttons.filter(el => EL.IS_ENABLED == null ? true : EL.IS_ENABLED);
        THIS.BUTTONS = ALLOWED_BUTTONS.SLICE(0, indexToSplit);
        THIS.POPOVER_BUTTONS = ALLOWED_BUTTONS.SLICE(indexToSplit);
        THIS.ARE_BUTTONS_READY_TO_SHOW = true;
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
        if (THIS.SUBNAVIGATIONBUTTONS && THIS.CONTAINER) {

          const colLeftPadding = 16;
          const paddingLeftRight = 24;
          const ionItemWidth = THIS.CONTAINER?.NATIVE_ELEMENT.OFFSET_WIDTH - colLeftPadding;
          if (ionItemWidth) {

            let sum: number = colLeftPadding;
            THIS.SUBNAVIGATIONBUTTONS.FOR_EACH((b, index, el) => {
              sum += B.NATIVE_ELEMENT.OFFSET_WIDTH + paddingLeftRight;
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
      }, FOOTER_NAVIGATION_COMPONENT.INTERVAL);
    });
  }
}
