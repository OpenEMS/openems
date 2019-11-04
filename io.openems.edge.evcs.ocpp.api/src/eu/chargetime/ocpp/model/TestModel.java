package eu.chargetime.ocpp.model;

import java.util.Calendar;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/*
ChargeTime.eu - Java-OCA-OCPP
Copyright (C) 2015-2016 Thomas Volden <tv@chargetime.eu>

MIT License

Copyright (C) 2016-2018 Thomas Volden

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

/** Test model used to check conversion of different field types. Used for tests. */
@XmlRootElement
public class TestModel {
  private String stringTest;
  private Calendar calendarTest;
  private Integer integerTest;
  private int intTest;
  private Long longTest;
  private long genericLongTest;
  private Double doubleTest;
  private double genericDoubleTest;
  private Boolean booleanTest;
  private boolean genericBoleanTest;
  private TestModel objectTest;
  private Integer[] arrayTest;

  public Integer[] getArrayTest() {
    return arrayTest;
  }

  @XmlElement
  public void setArrayTest(Integer[] arrayTest) {
    this.arrayTest = arrayTest;
  }

  public TestModel getObjectTest() {
    return objectTest;
  }

  @XmlElement
  public void setObjectTest(TestModel objectTest) {
    this.objectTest = objectTest;
  }

  public boolean isGenericBoleanTest() {
    return genericBoleanTest;
  }

  @XmlElement
  public void setGenericBoleanTest(boolean genericBoleanTest) {
    this.genericBoleanTest = genericBoleanTest;
  }

  public Boolean getBooleanTest() {
    return booleanTest;
  }

  @XmlElement
  public void setBooleanTest(Boolean booleanTest) {
    this.booleanTest = booleanTest;
  }

  public double getGenericDoubleTest() {
    return genericDoubleTest;
  }

  @XmlElement
  public void setGenericDoubleTest(double genericDoubleTest) {
    this.genericDoubleTest = genericDoubleTest;
  }

  public Double getDoubleTest() {
    return doubleTest;
  }

  @XmlElement
  public void setDoubleTest(Double doubleTest) {
    this.doubleTest = doubleTest;
  }

  public long getGenericLongTest() {
    return genericLongTest;
  }

  @XmlElement
  public void setGenericLongTest(long genericLongTest) {
    this.genericLongTest = genericLongTest;
  }

  public Long getLongTest() {
    return longTest;
  }

  @XmlElement
  public void setLongTest(Long longTest) {
    this.longTest = longTest;
  }

  public int getIntTest() {
    return intTest;
  }

  @XmlElement
  public void setIntTest(int intTest) {
    this.intTest = intTest;
  }

  public Integer getIntegerTest() {
    return integerTest;
  }

  @XmlElement
  public void setIntegerTest(Integer integerTest) {
    this.integerTest = integerTest;
  }

  public Calendar getCalendarTest() {
    return calendarTest;
  }

  @XmlElement
  public void setCalendarTest(Calendar calendarTest) {
    this.calendarTest = calendarTest;
  }

  public String getStringTest() {
    return stringTest;
  }

  @XmlElement
  public void setStringTest(String stringTest) {
    this.stringTest = stringTest;
  }
}
