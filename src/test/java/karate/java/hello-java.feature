Feature: Hello Java

  # Calling Java code from Karate

  Background:
    * def HelloKarate = Java.type('karate.java.HelloKarate')
    * def hk = new HelloKarate()

  Scenario: Hello world

    * def output = hk.get()
    * match output == 1234
    * print output;

  Scenario: Getting a map

    * def output = hk.getMap()
    * match output == { key : 'KKK', value : 'VVV'}


  Scenario: Getting a list

    * def output = hk.getList()
    * match output == [ { value : '10'}  { value : '20'} { value : '30'} ]

  Scenario: Passing an number

    # Karate uses Nashorn to convert Javascript numbers to Java integer. Due to the way
    # Nashorn works, the largest number that can be safely converted to Integer or Long is
    # 2147483647

    * def param = 2147483647
    * def out1 = hk.echoInt(param)
    * def out2 = hk.echoLong(param)
    * match out1 == param
    * match out2 == param

  Scenario: Passing big numbers.

    # Use java.math.BigDecimal to pass in really big numbers, as described https://github.com/intuit/karate#large-numbers

    * def param = new java.math.BigDecimal(123456789012345567890)
    * def out = hk.echoBigDecimal(param)
    * match out == param
