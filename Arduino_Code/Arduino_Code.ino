/////////////////////////////////////////////////////////////////////////////////////////
//Developed for GPS location for the blind project for group 2 EDP.
//
//Adafruit example code for Rx/Tx BLE communication was very helpful in creating this. 
/////////////////////////////////////////////////////////////////////////////////////////

#include <Arduino.h>
#include <SPI.h>
#include "Adafruit_BLE.h"
#include "Adafruit_BluefruitLE_SPI.h"
#include "Adafruit_BluefruitLE_UART.h"
#include "BluefruitConfig.h"

#define MINIMUM_FIRMWARE_VERSION    "0.6.6"
#define MODE_LED_BEHAVIOUR          "MODE"

Adafruit_BluefruitLE_SPI ble(BLUEFRUIT_SPI_CS, BLUEFRUIT_SPI_IRQ, BLUEFRUIT_SPI_RST);

void error(const __FlashStringHelper*err) {
  Serial.println(err);
  while (1);
}

void setup(void)
{
  Serial.begin(115200);
  Serial.println(F("Serial Started..."));
  pinMode(6, INPUT);
  pinMode(5, INPUT);
  pinMode(3, OUTPUT);
  pinMode(2, OUTPUT);
  /* Initialise the module */
  Serial.print(F("Initialising the Bluefruit LE module: "));

  if( !ble.begin(VERBOSE_MODE)){
    error(F("Couldn't find bluefruit, check for wiring and command mode"));
  }
  
  /* Disable command echo from Bluefruit */
  ble.echo(false);

  Serial.println("Requesting Bluefruit info:");
  /* Print Bluefruit information */
  ble.info();

  ble.verbose(false);  // All remaining serial information is not important

  /* Wait for connection */
  while (! ble.isConnected()) {
      delay(500);
  }

  // LED Activity command is only supported from 0.6.6
  if ( ble.isVersionAtLeast(MINIMUM_FIRMWARE_VERSION) )
  {
    // Change Mode LED Activity
    Serial.println(F("******************************"));
    Serial.println(F("Change LED activity to " MODE_LED_BEHAVIOUR));
    ble.sendCommandCheckOK("AT+HWModeLED=" MODE_LED_BEHAVIOUR);
    Serial.println(F("******************************"));
  }
}


void loop(void)
{
  // Check for user input
  char inputs[BUFSIZE+1];
  if(digitalRead(6)==HIGH){
    ble.print("AT+BLEUARTTX=");
    ble.println("LButton");
    if(!ble.waitForOK()){
      Serial.println(F("Failed to send"));
    }
  }
  if(digitalRead(5)==HIGH){
    ble.print("AT+BLEUARTTX=");
    ble.println("RButton");
    if(!ble.waitForOK()){
      Serial.println(F("Failed to send"));
    }
  }
  // Check for incoming characters from Bluefruit
  ble.println("AT+BLEUARTRX");
  ble.readline();
  if (strcmp(ble.buffer, "OK") == 0) {
    // no data
    return;
  }
  // Some data was found, its in the buffer
  Serial.print(F("[Recv] ")); Serial.println(ble.buffer);
  String temp = ble.buffer;  
  ble.waitForOK();
  Serial.println(temp);
  if(temp == "FlashL"){
    digitalWrite(3, HIGH);
    delay(1000);
    digitalWrite(3, LOW);
  }
  if(temp == "FlashR"){
    digitalWrite(2, HIGH);
    delay(1000);
    digitalWrite(2, LOW);
  }
}
