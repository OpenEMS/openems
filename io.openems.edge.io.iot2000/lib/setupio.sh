#!/bin/sh

#setup userled
echo -n 7 >/sys/class/gpio/unexport
echo -n 7 >/sys/class/gpio/export

echo -n 46 >/sys/class/gpio/unexport
echo -n 46 >/sys/class/gpio/export

echo -n 30 >/sys/class/gpio/unexport
echo -n 30 >/sys/class/gpio/export

echo -n 31 >/sys/class/gpio/unexport
echo -n 31 >/sys/class/gpio/export

echo -n low >/sys/class/gpio/gpio46/direction
echo -n low >/sys/class/gpio/gpio30/direction
echo -n in >/sys/class/gpio/gpio31/direction

echo -n 0 >/sys/class/gpio/gpio46/value
echo -n 0 >/sys/class/gpio/gpio30/value

echo -n low >/sys/class/gpio/gpio7/direction

#setup IO
#DI0 D12
echo -n 15 >/sys/class/gpio/unexport
echo -n 15 >/sys/class/gpio/export
echo -n 42 >/sys/class/gpio/unexport
echo -n 42 >/sys/class/gpio/export
echo -n 43 >/sys/class/gpio/unexport
echo -n 43 >/sys/class/gpio/export
echo -n in >/sys/class/gpio/gpio15/direction
echo -n high >/sys/class/gpio/gpio42/direction
echo -n in >/sys/class/gpio/gpio43/direction
echo -n hiz >/sys/class/gpio/gpio43/drive
echo -n 1 >/sys/class/gpio/gpio42/value

#DI1 D11
echo -n 5 >/sys/class/gpio/unexport
echo -n 5 >/sys/class/gpio/export
echo -n 44 >/sys/class/gpio/unexport
echo -n 44 >/sys/class/gpio/export
echo -n 72 >/sys/class/gpio/unexport
echo -n 72 >/sys/class/gpio/export
echo -n 24 >/sys/class/gpio/unexport
echo -n 24 >/sys/class/gpio/export
echo -n 25 >/sys/class/gpio/unexport
echo -n 25 >/sys/class/gpio/export
echo -n in >/sys/class/gpio/gpio5/direction
echo -n low >/sys/class/gpio/gpio44/direction
echo -n low >/sys/class/gpio/gpio72/direction
echo -n high >/sys/class/gpio/gpio24/direction
echo -n in >/sys/class/gpio/gpio25/direction
echo -n hiz >/sys/class/gpio/gpio25/drive
echo -n 1 >/sys/class/gpio/gpio24/value

#DI2 D10
echo -n 10 >/sys/class/gpio/unexport
echo -n 10 >/sys/class/gpio/export
echo -n 74 >/sys/class/gpio/unexport
echo -n 74 >/sys/class/gpio/export
echo -n 26 >/sys/class/gpio/unexport
echo -n 26 >/sys/class/gpio/export
echo -n 27 >/sys/class/gpio/unexport
echo -n 27 >/sys/class/gpio/export
echo -n in >/sys/class/gpio/gpio10/direction
echo -n low >/sys/class/gpio/gpio74/direction
echo -n high >/sys/class/gpio/gpio26/direction
echo -n in >/sys/class/gpio/gpio25/direction
echo -n hiz >/sys/class/gpio/gpio25/drive
echo -n 1 >/sys/class/gpio/gpio26/value

#DI3 D9
echo -n 4 >/sys/class/gpio/unexport
echo -n 4 >/sys/class/gpio/export
echo -n 70 >/sys/class/gpio/unexport
echo -n 70 >/sys/class/gpio/export
echo -n 22 >/sys/class/gpio/unexport
echo -n 22 >/sys/class/gpio/export
echo -n 23 >/sys/class/gpio/unexport
echo -n 23 >/sys/class/gpio/export
echo -n in >/sys/class/gpio/gpio4/direction
echo -n low >/sys/class/gpio/gpio70/direction
echo -n high >/sys/class/gpio/gpio22/direction
echo -n in >/sys/class/gpio/gpio23/direction
echo -n hiz >/sys/class/gpio/gpio23/drive
echo -n 1 >/sys/class/gpio/gpio22/value

#DI4 D4
echo -n 6 >/sys/class/gpio/unexport
echo -n 6 >/sys/class/gpio/export
echo -n 36 >/sys/class/gpio/unexport
echo -n 36 >/sys/class/gpio/export
echo -n 37 >/sys/class/gpio/unexport
echo -n 37 >/sys/class/gpio/export
echo -n in >/sys/class/gpio/gpio6/direction
echo -n high >/sys/class/gpio/gpio36/direction
echo -n in >/sys/class/gpio/gpio37/direction
echo -n hiz >/sys/class/gpio/gpio37/drive
echo -n 1 >/sys/class/gpio/gpio36/value

#DQ0 D8
echo -n 40 >/sys/class/gpio/unexport
echo -n 40 >/sys/class/gpio/export
echo -n 41 >/sys/class/gpio/unexport
echo -n 41 >/sys/class/gpio/export
echo -n low >/sys/class/gpio/gpio40/direction
echo -n in >/sys/class/gpio/gpio41/direction
echo -n hiz >/sys/class/gpio/gpio41/drive

#DQ1 D7
echo -n 38 >/sys/class/gpio/unexport
echo -n 38 >/sys/class/gpio/export
echo -n 39 >/sys/class/gpio/unexport
echo -n 39 >/sys/class/gpio/export
echo -n low >/sys/class/gpio/gpio38/direction
echo -n in >/sys/class/gpio/gpio39/direction
echo -n hiz >/sys/class/gpio/gpio39/drive