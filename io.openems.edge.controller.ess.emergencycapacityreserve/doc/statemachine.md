# State-Machine

```mermaid
graph TD
NO_LIMIT -->|discharge, SoC <= 21 %| ABOVE_RESERVE_SOC
ABOVE_RESERVE_SOC -->|discharge, SoC <= 20 %| AT_RESERVE_SOC
ABOVE_RESERVE_SOC -->|charge, SoC > 21| NO_LIMIT
AT_RESERVE_SOC -->|discharge, SoC < 20| BELOW_RESERVE_SOC["BELOW_RESERVE_SOC<br>[infinity, 0]<br>5 %p/sec"]
AT_RESERVE_SOC -->|charge, <br>if last state FORCE_CHARGE_GRID: <br> SoC > 21,<br>else: SoC > 20| ABOVE_RESERVE_SOC
BELOW_RESERVE_SOC -->|discharge, SoC <= 19| FORCE_CHARGE_PV
BELOW_RESERVE_SOC -->|discharge, SoC < 18| FORCE_CHARGE_GRID
FORCE_CHARGE_PV -->|charge, SoC >= 20| AT_RESERVE_SOC
FORCE_CHARGE_PV --> |discharge, SoC < 18| FORCE_CHARGE_GRID
FORCE_CHARGE_GRID -->|charge, SoC >= 21| AT_RESERVE_SOC
UNDEFINED["UNDEFINED<br>Initial State<br>[Start of Process]"]  -->|SoC < 18 %| FORCE_CHARGE_GRID["FORCE_CHARGE_GRID<br>Force-Charge with Grid Power<br>[infinity, max[10 % mAP if SoC >= 17%, else 50 % mAP]]<br>1 %p/sec"]
UNDEFINED -->|SoC == 19 %| FORCE_CHARGE_PV["FORCE_CHARGE_PV<br>Force-Charge with AC-PV<br>[infinity, AC-PV * -1]<br>1 %p/sec"]
UNDEFINED -->|SoC == 20 %| AT_RESERVE_SOC["AT_RESERVE_SOC<br>[infinity, max(0, DC-PV)]<br>1 %p/sec"]
UNDEFINED -->|SoC == 21 %| ABOVE_RESERVE_SOC["ABOVE_RESERVE_SOC<br>[infinity, max(50 % mAP, DC-PV)]<br>1 %p/sec"]
UNDEFINED -->|SoC > 21 %| NO_LIMIT["NO_LIMIT<br>SoC > 21 %<br>No Limit<br>[infinity, infinity]<br>1 %p/sec"]
DESCRIPTION["Example ReserveSoc = 20%<br>ReserveSoc in [5;100]<br>%p = Percentage Point<br>mAP = MaxApparentPower<br>Ramp increase/decrease by 1 %p/sec of MaxApparentPower"]

```

View using Mermaid, e.g. https://mermaid-js.github.io/mermaid-live-editor