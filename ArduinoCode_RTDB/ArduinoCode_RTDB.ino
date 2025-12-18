/*
 * LinkNPark Gate Control with Firebase RTDB
 * ESP32 Dev Module + Ultrasonic Sensor + 4 Servo Motors
 * 
 * SERVO LAYOUT:
 * - Servo 1 (Axis): Rotates camera for front/back plate scanning
 * - Servo 2 (Barrier 1): First entry barrier (opens/closes)
 * - Servo 3 (Barrier 2): Second entry barrier (after verification)
 * - Servo 4 (Exit Barrier): Exit gate barrier
 * 
 * Communicates with Python backend via Firebase Realtime Database:
 * - Writes ultrasonic triggers to RTDB
 * - Reads servo commands from RTDB
 */

#include <ESP32Servo.h>
#include <WiFi.h>
#include <FirebaseESP32.h>

// ===== WiFi Configuration =====
#define WIFI_SSID "realme 10"
#define WIFI_PASSWORD "123456789"

// ===== Firebase Configuration =====
#define DATABASE_URL "https://linknpark-a9074-default-rtdb.asia-southeast1.firebasedatabase.app"
#define DATABASE_SECRET ""  // Optional: Legacy database secret

// ===== Ultrasonic Sensor Pins =====
#define TRIG_PIN 5
#define ECHO_PIN 18

// ===== Servo Pins (4 Servos) =====
#define SERVO_AXIS_PIN 13       // Servo 1: Camera axis rotation
#define SERVO_BARRIER1_PIN 14   // Servo 2: First entry barrier
#define SERVO_BARRIER2_PIN 15   // Servo 3: Second entry barrier (after verification)
#define SERVO_EXIT_PIN 27       // Servo 4: Exit gate barrier

Servo axisServo;      // Camera rotation
Servo barrier1Servo;  // First entry barrier
Servo barrier2Servo;  // Second entry barrier
Servo exitServo;      // Exit barrier

// ===== LED Pins =====
#define RED_LED 4
#define YELLOW_LED 16
#define GREEN_LED 17

// ===== Servo Positions =====
#define AXIS_FRONT 0
#define AXIS_BACK 180
#define BARRIER_CLOSED 180
#define BARRIER_OPEN 90

// ===== RTDB Paths =====
#define PATH_ENTRY_TRIGGER "/iot/entry_trigger"
#define PATH_ENTRY_STATUS "/iot/entry_status"
#define PATH_SERVO_COMMANDS "/iot/servo_commands"
#define PATH_EXIT_GATE "/iot/exit_gate"

// ===== Variables =====
FirebaseData firebaseData;
FirebaseData streamData;
FirebaseData exitStreamData;
FirebaseAuth auth;
FirebaseConfig config;

int currentAxisPos = AXIS_FRONT;
int currentBarrier1Pos = BARRIER_CLOSED;
int currentBarrier2Pos = BARRIER_CLOSED;
int currentExitPos = BARRIER_CLOSED;
int carCount = 0;
bool wifiConnected = false;
bool firebaseConnected = false;
unsigned long lastTriggerTime = 0;
const unsigned long triggerCooldown = 3000;  // 3 second cooldown

// ===== Function Prototypes =====
int getDistance();
void connectWiFi();
void initFirebase();
void streamCallback(StreamData data);
void exitStreamCallback(StreamData data);
void streamTimeoutCallback(bool timeout);
void sendTrigger(int distance);
void moveServo(Servo& servo, int& currentPos, int targetPos, int stepDelay = 10);
void setStatusLED(char status);  // 'R'=Red, 'Y'=Yellow, 'G'=Green

void setup() {
  Serial.begin(115200);
  Serial.println("LinkNPark Gate - Initializing...");

  // --- Ultrasonic Sensor ---
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);

  // --- LEDs ---
  pinMode(RED_LED, OUTPUT);
  pinMode(YELLOW_LED, OUTPUT);
  pinMode(GREEN_LED, OUTPUT);
  setStatusLED('Y');  // Yellow during setup

  // --- Servos (4 total) ---
  axisServo.attach(SERVO_AXIS_PIN);
  barrier1Servo.attach(SERVO_BARRIER1_PIN);
  barrier2Servo.attach(SERVO_BARRIER2_PIN);
  exitServo.attach(SERVO_EXIT_PIN);
  
  // Initialize all servos to closed/front position
  axisServo.write(AXIS_FRONT);
  barrier1Servo.write(BARRIER_CLOSED);
  barrier2Servo.write(BARRIER_CLOSED);
  exitServo.write(BARRIER_CLOSED);
  
  Serial.println("Servos initialized: Axis=" + String(AXIS_FRONT) + 
                 ", B1=" + String(BARRIER_CLOSED) +
                 ", B2=" + String(BARRIER_CLOSED) +
                 ", Exit=" + String(BARRIER_CLOSED));

  // --- Connect WiFi ---
  Serial.println("Connecting to WiFi...");
  connectWiFi();

  if (wifiConnected) {
    // --- Initialize Firebase ---
    Serial.println("Connecting to Firebase...");
    initFirebase();
  }

  if (firebaseConnected) {
    Serial.println("✅ Ready! Waiting for vehicles...");
    setStatusLED('R');  // Red = barrier closed
  } else {
    Serial.println("⚠️ Offline Mode - No Firebase connection");
    setStatusLED('R');
  }
}

void loop() {
  // Read ultrasonic sensor
  int distance = getDistance();
  
  // Check for vehicle trigger (distance < 5cm, with cooldown)
  unsigned long now = millis();
  if (distance > 0 && distance < 5 && (now - lastTriggerTime > triggerCooldown)) {
    lastTriggerTime = now;
    
    Serial.println("🚗 Vehicle detected!");
    setStatusLED('Y');  // Yellow = processing
    
    if (firebaseConnected) {
      // Send trigger to RTDB
      sendTrigger(distance);
    } else {
      // Offline mode - just wait
      delay(2000);
      setStatusLED('R');
    }
  }
  
  // Process Firebase stream data
  if (firebaseConnected && Firebase.ready()) {
    if (!Firebase.readStream(streamData)) {
      Serial.println("Stream read failed: " + streamData.errorReason());
    }
  }
  
  delay(100);  // Small delay for stability
}

// ===== WiFi Connection =====
void connectWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Connecting to WiFi");
  
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  
  if (WiFi.status() == WL_CONNECTED) {
    wifiConnected = true;
    Serial.println("\n✅ WiFi connected!");
    Serial.println("IP: " + WiFi.localIP().toString());
  } else {
    wifiConnected = false;
    Serial.println("\n❌ WiFi connection failed!");
  }
}

// ===== Firebase Initialization =====
void initFirebase() {
  config.database_url = DATABASE_URL;
  
  if (strlen(DATABASE_SECRET) > 0) {
    config.signer.tokens.legacy_token = DATABASE_SECRET;
  }
  
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  
  // Set up stream for servo commands (entry gate)
  if (Firebase.beginStream(streamData, PATH_SERVO_COMMANDS)) {
    Serial.println("✅ Entry servo stream started");
    Firebase.setStreamCallback(streamData, streamCallback, streamTimeoutCallback);
    firebaseConnected = true;
  } else {
    Serial.println("❌ Entry servo stream failed: " + streamData.errorReason());
    firebaseConnected = false;
  }
  
  // Set up stream for exit gate commands
  if (Firebase.beginStream(exitStreamData, PATH_EXIT_GATE)) {
    Serial.println("✅ Exit gate stream started");
    Firebase.setStreamCallback(exitStreamData, exitStreamCallback, streamTimeoutCallback);
  } else {
    Serial.println("⚠️ Exit gate stream failed: " + exitStreamData.errorReason());
  }
}

// ===== Firebase Stream Callback (Entry Gate) =====
void streamCallback(StreamData data) {
  Serial.println("📡 Entry servo command received!");
  Serial.println("Path: " + data.dataPath());
  
  if (data.dataType() == "json") {
    FirebaseJson &json = data.jsonObject();
    FirebaseJsonData jsonData;
    
    // Read axis command (camera rotation)
    if (json.get(jsonData, "axis")) {
      int axisTarget = jsonData.intValue;
      Serial.println("Axis target: " + String(axisTarget));
      
      if (axisTarget != currentAxisPos) {
        moveServo(axisServo, currentAxisPos, axisTarget, 10);
        
        if (axisTarget == AXIS_BACK) {
          Firebase.setString(firebaseData, String(PATH_ENTRY_STATUS) + "/state", "rotated");
          Serial.println("Camera rotated - scanning back plate...");
        }
      }
    }
    
    // Read barrier1 command (first entry barrier)
    if (json.get(jsonData, "barrier")) {
      int barrierTarget = jsonData.intValue;
      Serial.println("Barrier1 target: " + String(barrierTarget));
      
      if (barrierTarget != currentBarrier1Pos) {
        moveServo(barrier1Servo, currentBarrier1Pos, barrierTarget, 15);
        
        if (barrierTarget == BARRIER_OPEN) {
          setStatusLED('G');  // Green = open
          carCount++;
          Serial.println("Entry OPEN - Cars: " + String(carCount));
        } else {
          setStatusLED('R');  // Red = closed
          Serial.println("Entry CLOSED - Ready");
        }
      }
    }
    
    // Read barrier2 command (second entry barrier)
    if (json.get(jsonData, "barrier2")) {
      int barrier2Target = jsonData.intValue;
      Serial.println("Barrier2 target: " + String(barrier2Target));
      
      if (barrier2Target != currentBarrier2Pos) {
        moveServo(barrier2Servo, currentBarrier2Pos, barrier2Target, 15);
        Serial.println("Barrier 2: " + String(barrier2Target == BARRIER_OPEN ? "OPEN" : "CLOSED"));
      }
    }
  }
}

// ===== Firebase Stream Callback (Exit Gate) =====
void exitStreamCallback(StreamData data) {
  Serial.println("📡 Exit gate command received!");
  
  if (data.dataType() == "json") {
    FirebaseJson &json = data.jsonObject();
    FirebaseJsonData jsonData;
    
    if (json.get(jsonData, "action")) {
      String action = jsonData.stringValue;
      Serial.println("Exit gate action: " + action);
      
      if (action == "OPEN" && currentExitPos != BARRIER_OPEN) {
        moveServo(exitServo, currentExitPos, BARRIER_OPEN, 15);
        Serial.println("EXIT GATE OPEN - Drive safely!");
        
        // Auto-close after 5 seconds
        delay(5000);
        moveServo(exitServo, currentExitPos, BARRIER_CLOSED, 15);
        Serial.println("Exit gate closed");
      } else if (action == "CLOSE" && currentExitPos != BARRIER_CLOSED) {
        moveServo(exitServo, currentExitPos, BARRIER_CLOSED, 15);
        Serial.println("Exit gate closed");
      }
    }
  }
}

void streamTimeoutCallback(bool timeout) {
  if (timeout) {
    Serial.println("⚠️ Stream timeout!");
  }
  if (!streamData.httpConnected()) {
    Serial.println("⚠️ Stream disconnected - reconnecting...");
    Firebase.beginStream(streamData, PATH_SERVO_COMMANDS);
  }
}

// ===== Send Ultrasonic Trigger to RTDB =====
void sendTrigger(int distance) {
  FirebaseJson json;
  json.set("detected", true);
  json.set("distance_cm", distance);
  json.set("timestamp", (unsigned long)(millis()));
  
  if (Firebase.setJSON(firebaseData, PATH_ENTRY_TRIGGER, json)) {
    Serial.println("✅ Trigger sent to RTDB");
  } else {
    Serial.println("❌ Trigger send failed: " + firebaseData.errorReason());
  }
}

// ===== Move Single Servo Smoothly =====
void moveServo(Servo& servo, int& currentPos, int targetPos, int stepDelay) {
  if (targetPos == currentPos) return;
  
  Serial.println("Moving servo: " + String(currentPos) + " -> " + String(targetPos));
  
  int step = (targetPos > currentPos) ? 1 : -1;
  for (int pos = currentPos; pos != targetPos; pos += step) {
    servo.write(pos);
    delay(stepDelay);
  }
  servo.write(targetPos);
  currentPos = targetPos;
}

// ===== LED Control =====
void setStatusLED(char status) {
  digitalWrite(RED_LED, status == 'R' ? HIGH : LOW);
  digitalWrite(YELLOW_LED, status == 'Y' ? HIGH : LOW);
  digitalWrite(GREEN_LED, status == 'G' ? HIGH : LOW);
}

// ===== Distance Measurement =====
int getDistance() {
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);

  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);

  long duration = pulseIn(ECHO_PIN, HIGH, 30000);  // 30ms timeout
  if (duration == 0) return -1;  // Timeout
  
  int dist = duration * 0.034 / 2;
  return dist;
}
