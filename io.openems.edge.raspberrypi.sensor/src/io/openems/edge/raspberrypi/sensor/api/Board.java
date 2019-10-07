package io.openems.edge.raspberrypi.sensor.api;

public enum Board {

        //TODO Change Numbers and make names easier for user
        LEAFLET_1_00(0.00001, 0.0438, -44.135);

        private final double a;
        private final double b;
        private final double c;

        private Board(double a, double b, double c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        public double getA() {
            return a;
        }

        public double getB() {
            return b;
        }

        public double getC() {
            return c;
        }

}

