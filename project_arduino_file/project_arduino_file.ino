#include <Wire.h>
#include <SoftwareSerial.h>
#include <math.h>

#define ADXL345_ADDRESS 0x53

// ADXL345 Register Addresses
#define ADXL345_REG_DATA_FORMAT 0x31
#define ADXL345_REG_POWER_CTL 0x2D
#define ADXL345_REG_DATAX0 0x32

SoftwareSerial bluetooth(1, 0);
// RX, TX pins for HC-05
const int motorPins[5] = { 3, 5, 6, 9, 10 };  // Motor control pins (example pins, modify as per your setup)
const float targetSpeed = 100.0;              // Target motor speed
const float kp = 0.2;                         // Proportional gain
const float ki = 0.1;                         // Integral gain
const float kd = 0.1;                         // Derivative gain
const float dt = 0.1;
char command = 0;                   // Time interval (in seconds)
const int activationThreshold = 1;  // Threshold value to activate motors (modify as needed)

float previousError[5] = { 0.0 };  // Previous error for each motor
float integral[5] = { 0.0 };

void setup() {
  Wire.begin();
  Serial.begin(9600);
  bluetooth.begin(9600);      // Initialize Bluetooth serial connection
  Serial.println("X, Y, Z");  // Print header for Serial Plotter

  // Initialize ADXL345
  writeToRegister(ADXL345_REG_POWER_CTL, 0x08);  // Enable measurement mode
  writeToRegister(ADXL345_REG_DATA_FORMAT, 0x08);
  // Set full resolution mode (±2g range)
  for (int i = 0; i < 5; i++) {
    pinMode(motorPins[i], OUTPUT);
    if (bluetooth.available()) {
      command = char(bluetooth.read());










      Serial.println(command);

    } else {
      Serial.println("hello not connect");
    }
  }
}

void loop() {
  int x, y, z;
  readAccelerometerData(x, y, z);
  int amplitude = sqrt(x + y + z) - 102;
  Serial.println(abs(amplitude));

  // Print the accelerometer data

  // Send data to Android app via Bluetooth
  bluetooth.println(amplitude-64);
  if (amplitude >= activationThreshold) {

    // Calculate motor speeds using PID control
    float motorSpeeds[5];
    for (int i = 0; i < 5; i++) {
    float error = amplitude;  // Error is the amplitude of tremors
      integral[i] += error * dt;
      float derivative = (error - previousError[i]) / dt;

      float controlSignal = kp * error + ki * integral[i] + kd * derivative;
      motorSpeeds[i] = targetSpeed + controlSignal;

      // Update previous error for next iteration
      previousError[i] = error;

      // Set motor speed
      setMotorSpeed(i, motorSpeeds[i]);
    }

  } else {
    // Stop motors if amplitude is below threshold
    stopMotors();
  }

  delay(1000);
}

void writeToRegister(byte reg, byte value) {
  Wire.beginTransmission(ADXL345_ADDRESS);
  Wire.write(reg);
  Wire.write(value);
  Wire.endTransmission();
}

void readAccelerometerData(int& x, int& y, int& z) {
  Wire.beginTransmission(ADXL345_ADDRESS);
  Wire.write(ADXL345_REG_DATAX0);
  Wire.endTransmission();
  Wire.requestFrom(ADXL345_ADDRESS, 6);

  if (Wire.available() >= 6) {
    x = pow(Wire.read() | (Wire.read() << 8), 2);
    y = pow(Wire.read() | (Wire.read() << 8), 2);
    z = pow(Wire.read() | (Wire.read() << 8), 2) * -1;
  }
}
void setMotorSpeed(int motorIndex, float speed) {
  // Perform any necessary calculations or mappings for motor speed control
  // and implement the actual motor control logic using PWM, H-bridge, or any
  // other motor control mechanism specific to your setup.
  // This function is just a placeholder to demonstrate the concept.

  // Example: Controlling motor speed using analogWrite()
  if (speed >= 0 && speed <= 255) {
    analogWrite(motorPins[motorIndex], speed);
  } else if (speed < 0) {
    analogWrite(motorPins[motorIndex], 0);
  } else if (speed > 255) {
    analogWrite(motorPins[motorIndex], 255);
  }
}

void stopMotors() {
  // Stop all motors
  for (int i = 0; i < 5; i++) {
    analogWrite(motorPins[i], 0);
    previousError[i] = 0.0;
    integral[i] = 0.0;
  }
}