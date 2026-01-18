/*
 * LinkNPark Gate Control with FirebaseClient (New Library)
 * ESP32 + Ultrasonic + 4 Servos
 * 
 * SERVO LAYOUT:
 * 1. AXIS - Camera rotation (for plate scanning)
 * 2. BARRIER1 - First entry barrier
 * 3. BARRIER2 - Second entry barrier (after verification)
 * 4. EXIT - Exit gate barrier
 * 
 * Using mobizt/FirebaseClient (new async library)
 */

#include <ESP32Servo.h>
#include <WiFi.h>

// New FirebaseClient library
#define ENABLE_DATABASE
#define ENABLE_LEGACY_TOKEN
#include <FirebaseClient.h>
#include <WiFiClientSecure.h>

// ===== WiFi =====
#define WIFI_SSID "abcdefgh"
#define WIFI_PASSWORD "12345678"

// ===== Firebase =====
#define DATABASE_URL "https://linknpark-a9074-default-rtdb.asia-southeast1.firebasedatabase.app"
#define DATABASE_SECRET "tYFRceH1SnKZ5vlZaerAzwsUyFg4YKagrtGMeOMA"

// ===== Pins =====
#define TRIG_PIN 5
#define ECHO_PIN 18

// 4 Servo Pins
#define SERVO_AXIS_PIN 13      // Servo 1: Camera rotation
#define SERVO_BARRIER1_PIN 14  // Servo 2: First entry barrier
#define SERVO_BARRIER2_PIN 25  // Servo 3: Second entry barrier
#define SERVO_EXIT_PIN 27      // Servo 4: Exit barrier

// LEDs
#define RED_LED 4
#define YELLOW_LED 16
#define GREEN_LED 17

// ===== Servo Positions =====
#define AXIS_FRONT 0
#define AXIS_BACK 210     // Rotated 15 degrees more for better angle
#define BARRIER_CLOSED 180
#define BARRIER_OPEN 90

// Barrier 1 is mounted in opposite direction, so angles are inverted
#define BARRIER1_CLOSED 0
#define BARRIER1_OPEN 90

// ===== RTDB Paths =====
#define PATH_ENTRY_TRIGGER "iot/entry_trigger"
#define PATH_GATE_COMMAND "iot/gate_command"
#define PATH_ENTRY_STATUS "iot/entry_status"

// ===== Objects =====
Servo axisServo;
Servo barrier1Servo;
Servo barrier2Servo;
Servo exitServo;

// ===== Firebase Objects (New Library) =====
WiFiClientSecure ssl_client;
WiFiClientSecure stream_ssl_client;

using AsyncClient = AsyncClientClass;
AsyncClient aClient(ssl_client);
AsyncClient streamClient(stream_ssl_client);

LegacyToken dbSecret(DATABASE_SECRET);
FirebaseApp app;
RealtimeDatabase Database;

// ===== Variables =====
bool firebaseConnected = false;
bool streamStarted = false;
unsigned long lastTriggerTime = 0;

// Track current states to prevent redundant movements
String currentAxisState = "unknown";
String currentBarrier1State = "unknown";
String currentBarrier2State = "unknown";
String currentExitState = "unknown";

// ===== Prototypes =====
void connectWiFi();
void initFirebase();
void processStream(AsyncResult &aResult);
void processResult(AsyncResult &aResult);
void sendTrigger(int distance);
void moveServo(Servo& servo, int pin, int from, int to, int delayMs);
void setLED(char c);
int getDistance();
void reportStatus(String state, String message = "");
void resetGateCommand();
void processAction(const String& action);

void setup() {
  Serial.begin(115200);
  Serial.println("LinkNPark Gate - FirebaseClient Library");
  
  // Display initial heap for monitoring
  Serial.printf("💾 Initial free heap: %d bytes\n", ESP.getFreeHeap());
  
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  pinMode(RED_LED, OUTPUT);
  pinMode(YELLOW_LED, OUTPUT);
  pinMode(GREEN_LED, OUTPUT);
  setLED('Y');
  
  // Set initial states
  currentAxisState = "FRONT";
  currentBarrier1State = "CLOSED";
  currentBarrier2State = "CLOSED";
  currentExitState = "CLOSED";

  // Init all servos to default positions
  axisServo.attach(SERVO_AXIS_PIN); axisServo.write(AXIS_FRONT); delay(200); axisServo.detach();
  barrier1Servo.attach(SERVO_BARRIER1_PIN); barrier1Servo.write(BARRIER1_CLOSED); delay(200); barrier1Servo.detach();
  barrier2Servo.attach(SERVO_BARRIER2_PIN); barrier2Servo.write(BARRIER_CLOSED); delay(200); barrier2Servo.detach();
  exitServo.attach(SERVO_EXIT_PIN); exitServo.write(BARRIER_CLOSED); delay(200); exitServo.detach();
  
  connectWiFi();
  if (WiFi.status() == WL_CONNECTED) {
    initFirebase();
  }
  
  setLED('R');
  Serial.println("✅ Ready!");
}

void loop() {
  // CRITICAL: Must call app.loop() to process async tasks
  app.loop();
  
  int distance = getDistance();
  
  // Periodic debug output (every 3 seconds)
  static unsigned long lastDebug = 0;
  if (millis() - lastDebug > 3000) {
    if (distance > 0) {
      String fbStatus = firebaseConnected ? "🟢 FB:OK" : "🔴 FB:FAIL";
      String streamStatus = streamStarted ? "📡 Stream:ON" : "📡 Stream:OFF";
      Serial.printf("📏 Distance: %d cm | %s | %s | Heap: %d\n", 
                    distance, fbStatus.c_str(), streamStatus.c_str(), ESP.getFreeHeap());
    }
    lastDebug = millis();
  }
  
  // Trigger on car detection (only if Firebase is ready)
  if (app.ready() && distance > 0 && distance < 20 && (millis() - lastTriggerTime > 20000)) {
    lastTriggerTime = millis();
    Serial.println("🚗 CAR DETECTED at " + String(distance) + "cm!");
    setLED('Y');
    sendTrigger(distance);
  }
  
  delay(100);
}

void connectWiFi() {
  Serial.print("📶 Connecting to WiFi");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  for (int i = 0; i < 20 && WiFi.status() != WL_CONNECTED; i++) {
    delay(500);
    Serial.print(".");
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println(" ✅ Connected!");
    Serial.println("IP: " + WiFi.localIP().toString());
  } else {
    Serial.println(" ❌ Failed!");
  }
}

void initFirebase() {
  Serial.println("🔥 Initializing Firebase (New Library)...");
  
  // Configure SSL clients (insecure for simplicity - production should use certs)
  ssl_client.setInsecure();
  stream_ssl_client.setInsecure();
  
  Serial.println("Using legacy token (database secret) authentication...");
  
  // Initialize app with legacy token
  initializeApp(aClient, app, getAuth(dbSecret), processResult, "authTask");
  
  // Get Database reference
  app.getApp<RealtimeDatabase>(Database);
  
  // Set database URL
  Database.url(DATABASE_URL);
  
  firebaseConnected = true;
  Serial.println("✅ Firebase initialized successfully!");
  Serial.println("Database: " + String(DATABASE_URL));
  
  // Start stream on gate_command path
  startCommandStream();
  
  // Reset gate command to IDLE
  resetGateCommand();
}

void startCommandStream() {
  Serial.println("🎯 Starting command stream on: " + String(PATH_GATE_COMMAND));
  
  // Configure SSE filters
  streamClient.setSSEFilters("get,put,patch,keep-alive,cancel,auth_revoked");
  
  // Start stream with SSE mode (true = HTTP streaming)
  Database.get(streamClient, PATH_GATE_COMMAND, processStream, true, "commandStream");
  
  streamStarted = true;
  Serial.println("✅ Command stream started!");
}

void processStream(AsyncResult &aResult) {
  // Monitor heap during stream processing
  uint32_t freeHeap = ESP.getFreeHeap();
  if (freeHeap < 40000) {
    Serial.printf("⚠️ Low heap during stream: %d bytes\n", freeHeap);
  }
  
  if (!aResult.isResult())
    return;
  
  if (aResult.isError()) {
    Serial.printf("❌ Stream error: %s, code: %d\n", 
                  aResult.error().message().c_str(), aResult.error().code());
    return;
  }
  
  if (aResult.available()) {
    RealtimeDatabaseResult &stream = aResult.to<RealtimeDatabaseResult>();
    
    if (stream.isStream()) {
      Serial.println("\n🔔 === STREAM EVENT RECEIVED ===");
      Serial.printf("Event: %s\n", stream.event().c_str());
      Serial.printf("Path: %s\n", stream.dataPath().c_str());
      Serial.printf("Data: %s\n", stream.to<const char *>());
      
      // Parse JSON data to get action
      String jsonStr = stream.to<String>();
      
      // Simple JSON parsing for action field
      int actionStart = jsonStr.indexOf("\"action\":\"");
      if (actionStart >= 0) {
        actionStart += 10; // Length of "action":"
        int actionEnd = jsonStr.indexOf("\"", actionStart);
        if (actionEnd > actionStart) {
          String action = jsonStr.substring(actionStart, actionEnd);
          
          if (action == "IDLE") {
            Serial.println("ℹ️ IDLE command, ignoring");
            return;
          }
          
          Serial.println("✅ Processing action: " + action);
          processAction(action);
        }
      }
    }
  }
}

void processResult(AsyncResult &aResult) {
  if (!aResult.isResult())
    return;
    
  if (aResult.isEvent()) {
    Serial.printf("🔔 Event: %s, msg: %s\n", aResult.uid().c_str(), aResult.eventLog().message().c_str());
  }
  
  if (aResult.isError()) {
    Serial.printf("❌ Error: %s, msg: %s, code: %d\n", 
                  aResult.uid().c_str(), aResult.error().message().c_str(), aResult.error().code());
  }
  
  if (aResult.available()) {
    Serial.printf("✅ Task: %s, result: %s\n", aResult.uid().c_str(), aResult.c_str());
  }
}

void processAction(const String& action) {
  // ===== COMPOSITE COMMANDS =====
  if (action == "ENTRY_SEQUENCE") {
    executeEntrySequence();
    return;
  }
  else if (action == "ROTATE_BACK") {
    executeRotateBack();
    return;
  }
  else if (action == "ROTATE_FRONT") {
    executeRotateFront();
    return;
  }
  
  // ===== ATOMIC COMMANDS =====
  if (action == "OPEN_BARRIER1") {
    Serial.println("🔓 Barrier1 command received. Current state: " + currentBarrier1State);
    if (currentBarrier1State != "OPEN") {
      moveServo(barrier1Servo, SERVO_BARRIER1_PIN, BARRIER1_CLOSED, BARRIER1_OPEN, 15);
      currentBarrier1State = "OPEN";
      setLED('G');
      Serial.println("✅ Barrier1 OPENED");
    }
  } 
  else if (action == "CLOSE_BARRIER1") {
    Serial.println("🔒 Barrier1 CLOSE command received");
    if (currentBarrier1State != "CLOSED") {
      moveServo(barrier1Servo, SERVO_BARRIER1_PIN, BARRIER1_OPEN, BARRIER1_CLOSED, 15);
      currentBarrier1State = "CLOSED";
      setLED('R');
      Serial.println("✅ Barrier1 CLOSED");
    }
  }
  else if (action == "OPEN_BARRIER2") {
    if (currentBarrier2State != "OPEN") {
      moveServo(barrier2Servo, SERVO_BARRIER2_PIN, BARRIER_CLOSED, BARRIER_OPEN, 15);
      currentBarrier2State = "OPEN";
    }
  } 
  else if (action == "CLOSE_BARRIER2") {
    if (currentBarrier2State != "CLOSED") {
      moveServo(barrier2Servo, SERVO_BARRIER2_PIN, BARRIER_OPEN, BARRIER_CLOSED, 15);
      currentBarrier2State = "CLOSED";
    }
  }
  else if (action == "OPEN_EXIT") {
    if (currentExitState != "OPEN") {
      moveServo(exitServo, SERVO_EXIT_PIN, BARRIER_CLOSED, BARRIER_OPEN, 15);
      currentExitState = "OPEN";
    }
  } 
  else if (action == "CLOSE_EXIT") {
    if (currentExitState != "CLOSED") {
      moveServo(exitServo, SERVO_EXIT_PIN, BARRIER_OPEN, BARRIER_CLOSED, 15);
      currentExitState = "CLOSED";
    }
  }
}

// ===== COMPOSITE COMMANDS =====

void executeEntrySequence() {
  Serial.println("🎬 Starting ENTRY_SEQUENCE...");
  
  // Step 1: Open Barrier 1
  reportStatus("barrier1_opening", "Opening entry barrier");
  if (currentBarrier1State != "OPEN") {
    moveServo(barrier1Servo, SERVO_BARRIER1_PIN, BARRIER1_CLOSED, BARRIER1_OPEN, 15);
    currentBarrier1State = "OPEN";
    setLED('G');
  }
  reportStatus("barrier1_opened", "Barrier 1 open - waiting for car");
  
  // Step 2: Wait 5 seconds for car to pass (non-blocking)
  unsigned long waitStart = millis();
  while (millis() - waitStart < 5000) {
    app.loop();  // Keep Firebase alive during wait
    delay(100);
  }
  
  // Step 3: Close Barrier 1
  reportStatus("barrier1_closing", "Closing entry barrier");
  if (currentBarrier1State != "CLOSED") {
    moveServo(barrier1Servo, SERVO_BARRIER1_PIN, BARRIER1_OPEN, BARRIER1_CLOSED, 15);
    currentBarrier1State = "CLOSED";
    setLED('R');
  }
  reportStatus("barrier1_closed", "Entry sequence complete");
  
  Serial.println("✅ ENTRY_SEQUENCE complete");
}

void executeRotateBack() {
  Serial.println("🔄 Rotating camera to BACK...");
  reportStatus("rotating_back", "Rotating camera");
  
  if (currentAxisState != "BACK") {
    moveServo(axisServo, SERVO_AXIS_PIN, AXIS_FRONT, AXIS_BACK, 10);
    currentAxisState = "BACK";
  }
  
  reportStatus("rotated_back", "Camera at back position");
  Serial.println("✅ Rotation to BACK complete");
}

void executeRotateFront() {
  Serial.println("🔄 Rotating camera to FRONT...");
  reportStatus("rotating_front", "Rotating camera");
  
  if (currentAxisState != "FRONT") {
    moveServo(axisServo, SERVO_AXIS_PIN, AXIS_BACK, AXIS_FRONT, 10);
    currentAxisState = "FRONT";
  }
  
  reportStatus("rotated_front", "Camera at front position");
  Serial.println("✅ Rotation to FRONT complete");
}

// ===== Firebase Operations =====

void sendTrigger(int distance) {
  if (!app.ready()) {
    Serial.println("❌ Firebase not ready, cannot send trigger");
    return;
  }
  
  // Create JSON object with proper JsonWriter usage
  // Each create() makes a separate object, join() combines them
  JsonWriter writer;
  
  object_t obj1, obj2, obj3, ts, json;
  
  // Create individual field objects
  writer.create(obj1, "detected", true);
  writer.create(obj2, "distance_cm", distance);
  
  // Create server timestamp: {".sv": "timestamp"}
  writer.create(ts, ".sv", "timestamp");
  writer.create(obj3, "timestamp", ts);
  
  // Join all fields into one JSON object
  writer.join(json, 3, obj1, obj2, obj3);
  
  Serial.println("📤 Sending trigger to Python...");
  Database.set<object_t>(aClient, PATH_ENTRY_TRIGGER, json, processResult, "sendTrigger");
}

void resetGateCommand() {
  if (!app.ready()) {
    Serial.println("⚠️ Firebase not ready for reset");
    return;
  }
  
  Serial.println("🔄 Resetting gate command to IDLE...");
  
  object_t json;
  JsonWriter writer;
  writer.create(json, "action", "IDLE");
  
  Database.set<object_t>(aClient, PATH_GATE_COMMAND, json, processResult, "resetGateCommand");
  Serial.println("✅ Gate command reset to IDLE");
}

void reportStatus(String state, String message) {
  if (!app.ready()) return;
  
  // Use join() pattern to include both fields
  JsonWriter writer;
  object_t obj1, obj2, json;
  
  writer.create(obj1, "state", state);
  writer.create(obj2, "message", message);
  writer.join(json, 2, obj1, obj2);
  
  Database.set<object_t>(aClient, PATH_ENTRY_STATUS, json, processResult, "reportStatus");
}

// ===== Move Servo with async task processing =====
void moveServo(Servo& servo, int pin, int from, int to, int delayMs) {
  servo.attach(pin);
  
  int step = (to > from) ? 1 : -1;
  for (int pos = from; pos != to; pos += step) {
    servo.write(pos);
    
    // Process Firebase async tasks during servo movement
    // This keeps SSL connections alive and processes stream events
    app.loop();
    
    delay(delayMs);
  }
  servo.write(to);
  
  delay(150);  
  servo.detach();
}

void setLED(char c) {
  digitalWrite(RED_LED, c == 'R' ? HIGH : LOW);
  digitalWrite(YELLOW_LED, c == 'Y' ? HIGH : LOW);
  digitalWrite(GREEN_LED, c == 'G' ? HIGH : LOW);
}

int getDistance() {
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  long d = pulseIn(ECHO_PIN, HIGH, 30000);
  return d == 0 ? -1 : d * 0.034 / 2;
}
