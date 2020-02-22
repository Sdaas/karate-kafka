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
