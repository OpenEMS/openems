package eu.chargetime.ocpp.utilities;
/*
   ChargeTime.eu - Java-OCA-OCPP

   MIT License

   Copyright (C) 2016-2018 Thomas Volden <tv@chargetime.eu>

   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in all
   copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
   SOFTWARE.
*/

import java.util.Timer;
import java.util.TimerTask;

public class TimeoutTimer extends Timer {

	private TimerTask timerTask;
	private long timeout;
	private TimeoutHandler handler;

	public TimeoutTimer(long timeout, TimeoutHandler handler) {
		this.timeout = timeout;
		this.handler = handler;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void begin() {
		timerTask = new TimerTask() {
			@Override
			public void run() {
				handler.timeout();
			}
		};
		this.schedule(timerTask, timeout);
	}

	public void end() {
		timerTask.cancel();
	}

	public void reset() {
		end();
		begin();
	}
}
