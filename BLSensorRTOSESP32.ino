#Real time PPG retreival and sending over Bluetooth at 125Hz fixed using Round Robin architecture



#include <DoubleEMAFilterLib.h>
//#include <SingleEMAFilterLib.h>
#include <Wire.h>
#include "MAX30100_PulseOximeter.h"
#include "BluetoothSerial.h"
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif
PulseOximeter pox;
MAX30100 sensor;
int LED_BUILTIN = 2;
unsigned long t1 = 0, t2, t_final;
#include "esp_adc_cal.h"
#define SAMPLING_RATE                       MAX30100_SAMPRATE_200HZ
#define LM35_Sensor1                        35
#define IR_LED_CURRENT                      MAX30100_LED_CURR_50MA
#define RED_LED_CURRENT                     MAX30100_LED_CURR_27_1MA
#define PULSE_WIDTH                         MAX30100_SPC_PW_800US_15BITS
#define HIGHRES_MODE                        true
int LM35_Raw_Sensor1 = 0;
float LM35_TempC_Sensor1 = 0.0;
float Voltage = 0.0;
uint16_t ir, red;
BluetoothSerial SerialBT;
TaskHandle_t Task1, Task2, Task3;
//SingleEMAFilter<float> singleEMAFilter(0.1);
DoubleEMAFilter<float> doubleEMAFilter(0.025, 0.1);
void setup(){
  Serial.begin(115200);
  pinMode (LED_BUILTIN, OUTPUT);
  SerialBT.begin("ESP32test");
  if (!pox.begin()) {
    //Serial.println("FAILED");
    for (;;);
  } else {
    //Serial.println("SUCCESS");
  }
  pox.setIRLedCurrent(MAX30100_LED_CURR_7_6MA);
  sensor.setMode(MAX30100_MODE_SPO2_HR);
  sensor.setLedsCurrent(IR_LED_CURRENT, RED_LED_CURRENT);
  sensor.setLedsPulseWidth(PULSE_WIDTH);
  sensor.setSamplingRate(SAMPLING_RATE);
  sensor.setHighresModeEnabled(HIGHRES_MODE);

  // Register a callback for the beat detection
  //pox.setOnBeatDetectedCallback(onBeatDetected);
  xTaskCreatePinnedToCore(HR_sensor, "Heart Rate Sensor", 5000, NULL, 1, &Task1, 1);
  xTaskCreatePinnedToCore(BL_transmit, "BL Transmission", 10240, NULL, 2, &Task2, 1);
  xTaskCreatePinnedToCore(Temp, "Temperature", 1000, NULL, 1, &Task3, 0);
}

void HR_sensor(void * parameters) {
  for ( ;; ) {
    TickType_t xLastWakeTime;
    xLastWakeTime = xTaskGetTickCount();
    pox.update();
    sensor.update();
    sensor.getRawValues(&ir, &red);
    digitalWrite(LED_BUILTIN, HIGH);
    vTaskDelayUntil(&xLastWakeTime, pdMS_TO_TICKS(8));
  }
}

void Temp(void * parameters) {
  for ( ;; ) {
    TickType_t xLastWakeTime;
    xLastWakeTime = xTaskGetTickCount();
    // Read LM35_Sensor1 ADC Pin
    LM35_Raw_Sensor1 = analogRead(LM35_Sensor1);
    // Calibrate ADC & Get Voltage (in mV)
    Voltage = readADC_Cal(LM35_Raw_Sensor1);
    // TempC = Voltage(mV) / 10
    LM35_TempC_Sensor1 = Voltage / 10;
    vTaskDelayUntil(&xLastWakeTime, pdMS_TO_TICKS(1000));
  }
}

uint32_t readADC_Cal(int ADC_Raw){
  esp_adc_cal_characteristics_t adc_chars;
  esp_adc_cal_characterize(ADC_UNIT_1, ADC_ATTEN_DB_11, ADC_WIDTH_BIT_12, 1100, &adc_chars);
  return (esp_adc_cal_raw_to_voltage(ADC_Raw, &adc_chars));
}



//Make the TXQueue = 18000 in Bluetooth Serial.cpp for ESP32
void BL_transmit(void * parameters) {
  for ( ;; ) {
    TickType_t xLastWakeTime;
    xLastWakeTime = xTaskGetTickCount();
    //    if (Serial.available()) {
    //      SerialBT.write(Serial.read());
    //    }
    if (SerialBT.available()) {
      Serial.write(SerialBT.read());
      //delay(10000);
    }
    String ch = "";
    doubleEMAFilter.AddValue(ir-red);
    ch.concat(doubleEMAFilter.GetBandPass()*-1);
    ch.concat(":");
    ch.concat(pox.getSpO2());
    ch.concat(":");
    ch.concat(pox.getHeartRate());
    ch.concat(":");
    ch.concat(LM35_TempC_Sensor1);
    ch.concat(":#");
    SerialBT.println(ch);
    digitalWrite(LED_BUILTIN, LOW);
    t2 = millis();
    t_final = t2 - t1;
    //Serial.print("Time Taken = ");
    Serial.println (t_final);
    Serial.print(",");
    Serial.println(doubleEMAFilter.GetBandPass()*-1);
    //Serial.print(",\t");
    //Serial.println(singleEMAFilter.GetLowPass());
    //Serial.print(",\t");
    //Serial.println(singleEMAFilter.GetHighPass());
    //Serial.println(xPortGetCoreID());
    t1 = t2;
    vTaskDelayUntil(&xLastWakeTime, pdMS_TO_TICKS(8));
  }
}

void loop() {
}
