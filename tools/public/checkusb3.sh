#!/bin/sh
URB_LINES=`dmesg -T | grep ttyUSB | grep failed | grep urb | wc -l`
CANNOT_RESET_LINES=`dmesg -T | grep usb | grep port | grep cannot | grep reset | grep err | wc -l`

if [ $CANNOT_RESET_LINES -le 0 ]
then
	if [ $URB_LINES -le 0 ]
	then
		echo "usbcheck.sh: USB OK, do nothing" >> /var/log/openems.log
	else
		echo "usbcheck.sh: USB DEFECT!" >> /var/log/openems.log	
		echo "output from dmesg" >> /var/log/openems.log
		dmesg -T | grep usb >> /var/log/openems.log
		echo "unbind usb" >> /var/log/openems.log
		echo 'usb1' > /sys/bus/usb/drivers/usb/unbind
		echo 'usb2' > /sys/bus/usb/drivers/usb/unbind
		echo 'usb3' > /sys/bus/usb/drivers/usb/unbind
		echo "clear dmesg" >> /var/log/openems.log
		dmesg -C
		echo "bind usb" >> /var/log/openems.log
		echo 'usb1' > /sys/bus/usb/drivers/usb/bind
		echo 'usb2' > /sys/bus/usb/drivers/usb/bind
		echo 'usb3' > /sys/bus/usb/drivers/usb/bind
	fi
else
	echo "usbcheck.sh: Cannot Reset USB found, do a reboot!" >> /var/log/openems.log	
	echo "output from dmesg" >> /var/log/openems.log
	dmesg -T | grep usb >> /var/log/openems.log
	reboot
fi