# BloodPressureAndRespiratoryRate

A non-invasive method to accurately estimate Blood Pressure and Respiratory Rate continously and in real time that can be recorded and seen on an Android Application.
The data acquisition is done via MAX30100 for now, soon to be upgraded with a MAX30101. The microtroller of choice is the ESP32 and FreeRTOS is used to achieve fixed sampling rate of a 125Hz over Bluetooth.
