# LinkNPark Enhanced Firestore Schema Design

**Version:** 2.0  
**Date:** January 11, 2026  
**Status:** Design Phase - Pending Implementation  

---

## Design Goals

1. ✅ **Standardized Naming** - All fields use `snake_case`
2. ✅ **PWD Support** - Dedicated PWD parking spots with enforcement
3. ✅ **Reserved Enforcement** - Penalty system for wrong vehicle parking
4. ✅ **Wallet Simulation** - Driver/Staff balance tracking for payment demo
5. ✅ **Recurring Reservations** - Support for repeated bookings
6. ✅ **Auto-Expiry** - Automatic reservation cancellation
7. ✅ **History/Logs** - Complete audit trail for all actions
8. ✅ **Clean Structure** - Remove duplicate fields and data redundancy

---

## Schema Overview

```
LinkNPark Firestore Database (Enhanced)
├── users/                      # User accounts with wallet
├── parking_facilities/         # Parking lot information
├── parking_spaces/             # Individual spots (including PWD)
├── parking_sessions/           # Active & completed parking sessions
├── reservations/               # Parking reservations (with recurring)
├── vehicles/                   # User vehicles (with PWD status)
├── payments/                   # Payment transaction records
├── penalties/                  # Violation/penalty records
├── notifications/              # User notifications
└── session_logs/              # Historical logs for tracking
```

---

## Collection Schemas

### 1. `users` Collection

**Purpose:** Store user account information with wallet balance

```javascript
{
  // Document ID = Auto-generated Firebase ID (user's UID)
  
  // Basic Info
  "email": "string",                    // User email
  "password": "string",                 // Plain text (exhibit only)
  "name": "string",                     // Full name
  "role": "string",                     // "DRIVER" | "STAFF"
  
  // Wallet Simulation (NEW)
  "wallet_balance": "number",           // Current balance in PHP
  "wallet_currency": "string",          // "PHP"
  
  // PWD Status (NEW)
  "is_pwd": "boolean",                  // Person with Disability status
  "pwd_id_number": "string | null",    // PWD ID number if applicable
  
  // Contact Info
  "phone_number": "string | null",     // Phone number
  
  // Metadata
  "created_at": "timestamp",
  "updated_at": "timestamp",
  "last_login_at": "timestamp | null"
}
```

**Indexes:**
- `email` (for login)
- `role` (for filtering)

**Sample Data (Diorama Setup):**
```javascript
// Driver 1 - with PWD status (owns PWD toy car)
{
  "email": "driver1@demo.com",
  "password": "driver123",
  "name": "Juan Dela Cruz",
  "role": "DRIVER",
  "wallet_balance": 500.00,
  "wallet_currency": "PHP",
  "is_pwd": true,
  "pwd_id_number": "PWD-2024-001234",
  "phone_number": "+639123456789",
  "created_at": "2026-01-01T00:00:00Z"
}

// Driver 2 - normal driver (NEW - owns regular toy car)
{
  "email": "driver2@demo.com",
  "password": "driver123",
  "name": "Pedro Santos",
  "role": "DRIVER",
  "wallet_balance": 300.00,
  "wallet_currency": "PHP",
  "is_pwd": false,
  "pwd_id_number": null,
  "phone_number": "+639987654321",
  "created_at": "2026-01-01T00:00:00Z"
}

// Staff member for demo
{
  "email": "staff@demo.com",
  "password": "staff123",
  "name": "Maria Santos",
  "role": "STAFF",
  "wallet_balance": 0.00,        // Staff doesn't pay, receives payments
  "wallet_currency": "PHP",
  "is_pwd": false,
  "phone_number": "+639111222333",
  "created_at": "2026-01-01T00:00:00Z"
}
```

> **Note:** 3 users total for diorama demo (2 drivers + 1 staff)

---

### 2. `parking_facilities` Collection

**Purpose:** Parking lot/facility information

```javascript
{
  // Document ID = Auto-generated or custom (e.g., "facility_001")
  
  // Basic Info
  "name": "string",                     // Facility name
  "code": "string",                     // Short code (e.g., "MAIN")
  "address": "string",                  // Full address
  "location": {                         // GeoPoint
    "latitude": "number",
    "longitude": "number"
  },
  
  // Capacity (Calculated from parking_spaces)
  "total_spaces": "number",             // Total parking spaces
  "available_spaces": "number",         // Currently available
  "occupied_spaces": "number",          // Currently occupied
  "reserved_spaces": "number",          // Currently reserved
  "pwd_spaces": "number",               // Total PWD spaces
  
  // Pricing
  "base_hourly_rate": "number",         // Base rate in PHP
  "pwd_hourly_rate": "number",          // Discounted rate for PWD
  "currency": "string",                 // "PHP"
  
  // Operating Schedule
  "operating_hours": {
    "monday": {"open": "string", "close": "string"},
    "tuesday": {"open": "string", "close": "string"},
    // ... etc
    "is_24_7": "boolean"
  },
  
  // Status
  "status": "string",                   // "ACTIVE" | "INACTIVE" | "MAINTENANCE"
  
  // Metadata
  "created_at": "timestamp",
  "updated_at": "timestamp"
}
```

**Sample Data (Diorama Setup):**
```javascript
{
  "name": "Demo Parking Facility",
  "code": "DEMO",
  "address": "Exhibit Hall, Manila",
  "location": {
    "latitude": 14.5995,
    "longitude": 120.9842
  },
  "total_spaces": 4,              // Only 4 spots in diorama
  "available_spaces": 1,          // 1 available, 2 occupied, 1 reserved
  "occupied_spaces": 2,           // 2 toy cars currently parked
  "reserved_spaces": 1,
  "pwd_spaces": 1,                // 1 PWD spot out of 4
  "base_hourly_rate": 50.00,
  "pwd_hourly_rate": 30.00,
  "currency": "PHP",
  "operating_hours": {
    "is_24_7": true
  },
  "status": "ACTIVE"
}
```

> **Note:** Diorama has only 4 physical parking spots

---

### 3. `parking_spaces` Collection

**Purpose:** Individual parking space records with PWD support

```javascript
{
  // Document ID = Auto-generated
  
  // Identification
  "space_code": "string",               // Human-readable (e.g., "A1", "PWD-1")
  "space_number": "number",             // Numeric identifier
  "facility_id": "string",              // FK → parking_facilities._id
  
  // Position/Layout
  "row": "string",                      // Row identifier (e.g., "A")
  "column": "number",                   // Column number (e.g., 1)
  "floor": "string | null",             // Floor level if multi-story
  
  // Space type (IMPORTANT)
  "space_type": "string",               // "STANDARD" | "PWD"
  "vehicle_type": "string",             // "STANDARD" (cars only, no motorcycles)
  
  // Current Status
  "status": "string",                   // "AVAILABLE" | "OCCUPIED" | "RESERVED" | "OUT_OF_SERVICE"
  "is_available": "boolean",
  "is_occupied": "boolean", 
  "is_reserved": "boolean",
  
  // Current Occupancy
  "current_session_id": "string | null",      // FK → parking_sessions._id
  "current_license_plate": "string | null",   // Current vehicle
  "occupied_since": "timestamp | null",
  
  // Reservation
  "reserved_by_user_id": "string | null",     // FK → users._id
  "reserved_by_license_plate": "string | null",
  "reserved_until": "timestamp | null",
  "reservation_id": "string | null",          // FK → reservations._id
  
  // Metadata
  "created_at": "timestamp",
  "updated_at": "timestamp"
}
```

**Sample Data (Diorama Setup - 4 Spots Total):**
```javascript
// Spot 1 - Available standard space
{
  "space_code": "A1",
  "space_number": 1,
  "facility_id": "demo_facility",
  "row": "A",
  "column": 1,
  "space_type": "STANDARD",
  "vehicle_type": "STANDARD",
  "status": "AVAILABLE",
  "is_available": true,
  "is_occupied": false,
  "is_reserved": false
}

// Spot 2 - Occupied by unregistered car
{
  "space_code": "A2",
  "space_number": 2,
  "facility_id": "demo_facility",
  "row": "A",
  "column": 2,
  "space_type": "STANDARD",
  "vehicle_type": "STANDARD",
  "status": "OCCUPIED",
  "is_available": false,
  "is_occupied": true,
  "is_reserved": false,
  "current_session_id": "session_001",
  "current_license_plate": "XYZ-9999",  // Unregistered car
  "occupied_since": "2026-01-11T03:00:00Z"
}

// Spot 3 - Occupied by another unregistered car
{
  "space_code": "A3",
  "space_number": 3,
  "facility_id": "demo_facility",
  "row": "A",
  "column": 3,
  "space_type": "STANDARD",
  "vehicle_type": "STANDARD",
  "status": "OCCUPIED",
  "is_available": false,
  "is_occupied": true,
  "is_reserved": false,
  "current_session_id": "session_002",
  "current_license_plate": "ABC-8888",  // Unregistered car
  "occupied_since": "2026-01-11T02:30:00Z"
}

// Spot 4 - PWD space (reserved for driver's PWD vehicle)
{
  "space_code": "PWD-1",
  "space_number": 4,
  "facility_id": "demo_facility",
  "row": "PWD",
  "column": 1,
  "space_type": "PWD",
  "vehicle_type": "STANDARD",
  "status": "RESERVED",
  "is_available": false,
  "is_occupied": false,
  "is_reserved": true,
  "reserved_by_license_plate": "DEMO-PWD1",  // Driver's registered PWD car
  "reserved_until": "2026-01-11T18:00:00Z",
  "reservation_id": "reservation_001"
}
```

> **Note:** 
> - Total: 4 spots (3 standard + 1 PWD)
> - Currently: 1 available, 2 occupied (unregistered cars), 1 reserved (driver's PWD vehicle)
> - The 3 toy cars: 2 unregistered (XYZ-9999, ABC-8888) + 1 registered PWD (DEMO-PWD1)

**Important:** 
- `space_type: "PWD"` indicates this is a PWD-only spot
- Enforcement happens in backend/app when creating session

---

### 4. `parking_sessions` Collection

**Purpose:** Track all parking sessions with violation detection

```javascript
{
  // Document ID = Auto-generated (this IS the session_id)
  
  // User & Vehicle
  "user_id": "string | null",           // FK → users._id (null for camera-only)
  "license_plate": "string",            // Vehicle license plate
  "vehicle_id": "string | null",        // FK → vehicles._id (if registered)
  "vehicle_is_pwd": "boolean",          // Was vehicle PWD-registered at entry?
  
  // Location
  "facility_id": "string",              // FK → parking_facilities._id
  "space_id": "string",                 // FK → parking_spaces._id
  "space_code": "string",               // Snapshot of space code
  "space_type": "string",               // Snapshot of space type at entry
  
  // Entry Time
  "entered_at": "timestamp",
  "parked_at": "timestamp | null",      // When actually parked
  "exited_at": "timestamp | null",      // When exited
  "duration_minutes": "number",         // Total duration
  
  // Exit Method (replaces entry_method for demo)
  "exit_method": "string | null",       // "CASH_TO_STAFF" | "WALLET_TO_STAFF" | "AUTO_PAYMENT"
  
  // Pricing
  "hourly_rate": "number",              // Rate applied (base or PWD)
  "base_amount": "number",              // Calculated parking fee
  "penalty_amount": "number",           // Total penalties
  "total_amount": "number",             // base_amount + penalty_amount
  "currency": "string",                 // "PHP"
  
  // Payment (simplified for demo)
  "payment_status": "string",           // "UNPAID" | "PAID"
  "payment_method": "string | null",    // "WALLET" | "CASH" | "AUTO_PAY"
  "payment_id": "string | null",        // FK → payments._id
  "paid_at": "timestamp | null",
  
  // Session Status
  "status": "string",                   // "ACTIVE" | "COMPLETED" | "CANCELLED"
  
  // Violation/Warning (NEW)
  "has_violation": "boolean",           // Flag for violations
  "violation_type": "string | null",    // "PWD_MISMATCH" | "RESERVED_MISMATCH" | null
  "violation_details": "string | null", // Description of violation
  
  // Reservation Link
  "reservation_id": "string | null",    // FK → reservations._id (if from reservation)
  
  // Staff Actions
  "confirmed_by_staff_id": "string | null",  // FK → users._id (staff)
  "confirmed_at": "timestamp | null",
  
  // Metadata
  "created_at": "timestamp",
  "updated_at": "timestamp"
}
```

**Business Logic:**
- **PWD Enforcement:** If `space_type == "PWD"` and `vehicle_is_pwd == false`, set:
  - `has_violation = true`
  - `violation_type = "PWD_MISMATCH"`
  - Create penalty record
  
- **Reserved Enforcement:** If space is reserved for another vehicle:
  - `has_violation = true`
  - `violation_type = "RESERVED_MISMATCH"`
  - Create penalty record

---

### 5. `reservations` Collection

**Purpose:** Parking space reservations with recurring support

```javascript
{
  // Document ID = Auto-generated
  
  // User Info
  "user_id": "string",                  // FK → users._id
  "license_plate": "string",            // Reserved for this vehicle
  "vehicle_id": "string | null",        // FK → vehicles._id
  
  // Location
  "facility_id": "string",              // FK → parking_facilities._id
  "space_id": "string | null",          // FK → parking_spaces._id (can be null for "any available")
  "space_code": "string | null",        // Reserved space code
  
  // Reservation Time
  "start_time": "timestamp",            // Reservation start
  "end_time": "timestamp",              // Reservation end
  "duration_hours": "number",           // Total hours reserved
  
  // Recurring (NEW)
  "is_recurring": "boolean",            // Is this a recurring reservation?
  "recurrence_pattern": "string | null", // "DAILY" | "WEEKLY" | "MONTHLY" | null
  "recurrence_days": "array | null",    // [1,3,5] for Mon, Wed, Fri
  "recurrence_end_date": "timestamp | null", // When to stop recurring
  "parent_reservation_id": "string | null",  // FK to parent if this is a recurrence instance
  
  // Pricing
  "hourly_rate": "number",
  "total_amount": "number",
  "currency": "string",
  
  // Payment
  "payment_status": "string",           // "UNPAID" | "PAID"
  "payment_id": "string | null",        // FK → payments._id
  "paid_at": "timestamp | null",
  
  // Status
  "status": "string",                   // "ACTIVE" | "COMPLETED" | "CANCELLED" | "EXPIRED" | "NO_SHOW"
  "cancelled_at": "timestamp | null",
  "cancellation_reason": "string | null",
  
  // Session Link
  "session_id": "string | null",        // FK → parking_sessions._id (when user arrives)
  "checked_in_at": "timestamp | null",  // When user actually arrived
  
  // Auto-Expiry (NEW)
  "expires_at": "timestamp",            // Auto-cancel after this time if not checked in
  "grace_period_minutes": "number",     // Minutes after start_time before auto-cancel (e.g., 15)
  
  // Metadata
  "created_at": "timestamp",
  "updated_at": "timestamp"
}
```

**Business Logic:**
- If current time > `expires_at` and `status == "ACTIVE"` and `session_id == null`:
  - Set `status = "EXPIRED"`
  - Release parking space
  - Create notification

---

### 6. `vehicles` Collection

**Purpose:** User vehicle registry with PWD status

```javascript
{
  // Document ID = Auto-generated
  
  // Owner
  "user_id": "string",                  // FK → users._id
  
  // Vehicle Info
  "license_plate": "string",            // Unique identifier
  "make": "string",                     // Manufacturer
  "model": "string",                    // Model name
  "color": "string",                    // Vehicle color
  "year": "number",                     // Model year
  
  // Vehicle Type
  "vehicle_type": "string",             // "SEDAN" | "SUV" | "VAN" | "MOTORCYCLE" | "TRUCK"
  
  // PWD Status (NEW)
  "is_pwd_vehicle": "boolean",          // Is this vehicle registered with PWD sticker?
  "pwd_sticker_number": "string | null", // PWD vehicle sticker number
  "pwd_verified": "boolean",            // Has staff verified PWD status?
  
  // Preferences
  "is_primary": "boolean",              // User's default vehicle
  
  // Metadata
  "registered_at": "timestamp",
  "updated_at": "timestamp"
}
```

**Sample Data (Diorama Setup - 2 Registered Vehicles):**
```javascript
// Driver 1's PWD vehicle (toy car with PWD sticker)
{
  "user_id": "driver1_user_id",
  "license_plate": "DEMO-PWD1",
  "make": "Toy",
  "model": "Car Model A",
  "color": "Blue",
  "year": 2024,
  "vehicle_type": "SEDAN",
  "is_pwd_vehicle": true,
  "pwd_sticker_number": "PWD-V-DEMO-001",
  "pwd_verified": true,
  "is_primary": true,
  "registered_at": "2026-01-01T00:00:00Z",
  "updated_at": "2026-01-01T00:00:00Z"
}

// Driver 2's regular vehicle (toy car, normal)
{
  "user_id": "driver2_user_id",
  "license_plate": "DEMO-CAR2",
  "make": "Toy",
  "model": "Car Model B",
  "color": "Red",
  "year": 2024,
  "vehicle_type": "SEDAN",
  "is_pwd_vehicle": false,
  "pwd_sticker_number": null,
  "pwd_verified": false,
  "is_primary": true,
  "registered_at": "2026-01-01T00:00:00Z",
  "updated_at": "2026-01-01T00:00:00Z"
}
```

> **Note:** 
> - 2 vehicle records (Driver 1's PWD car + Driver 2's regular car)
> - The unregistered toy car (XYZ-9999) has no vehicle record
> - License plates are printed and pasted on toy cars

---

### 7. `payments` Collection (NEW)

**Purpose:** Dedicated payment transaction records

```javascript
{
  // Document ID = Auto-generated
  
  // Transaction Info
  "transaction_id": "string",           // Unique transaction ID
  "amount": "number",                   // Amount in PHP
  "currency": "string",                 // "PHP"
  
  // User
  "payer_user_id": "string",           // FK → users._id (who paid)
  "payer_name": "string",              // Snapshot of payer name
  "recipient_user_id": "string | null", // FK → users._id (staff who received, if cash)
  
  // Payment Method (simplified for demo)
  "payment_method": "string",           // "WALLET" | "CASH" | "AUTO_PAY"
  "payment_source": "string",           // "PARKING_SESSION" | "RESERVATION" | "PENALTY"
  
  // Related Records
  "session_id": "string | null",        // FK → parking_sessions._id
  "reservation_id": "string | null",    // FK → reservations._id
  "penalty_id": "string | null",        // FK → penalties._id
  
  // Status
  "status": "string",                   // "PENDING" | "COMPLETED" | "FAILED" | "REFUNDED"
  
  // Metadata
  "processed_at": "timestamp",
  "created_at": "timestamp"
}
```

---

### 8. `penalties` Collection (NEW)

**Purpose:** Track parking violations and penalty fees

```javascript
{
  // Document ID = Auto-generated
  
  // Violation Info
  "violation_type": "string",           // "PWD_MISMATCH" | "RESERVED_MISMATCH" | "OVERSTAY" | "OTHER"
  "violation_title": "string",          // Human-readable title
  "violation_description": "string",    // Detailed description
  
  // User
  "user_id": "string | null",          // FK → users._id (violator)
  "license_plate": "string",           // Vehicle license plate
  
  // Related Session/Reservation
  "session_id": "string | null",        // FK → parking_sessions._id
  "reservation_id": "string | null",    // FK → reservations._id
  
  // Location
  "facility_id": "string",              // FK → parking_facilities._id
  "space_id": "string",                 // FK → parking_spaces._id
  "space_code": "string",               // Space where violation occurred
  
  // Penalty Amount
  "penalty_amount": "number",           // Penalty in PHP
  "currency": "string",                 // "PHP"
  
  "detected_by": "string",              // "SYSTEM" | "STAFF" | "CAMERA"
  "detected_by_user_id": "string | null", // FK → users._id (if staff reported)
  
  // Status
  "status": "string",                   // "PENDING" | "PAID" | "WAIVED" | "DISPUTED"
  "payment_id": "string | null",        // FK → payments._id
  "paid_at": "timestamp | null",
  "waived_by_staff_id": "string | null",
  "waived_reason": "string | null",
  
  // Metadata
  "created_at": "timestamp",
  "updated_at": "timestamp"
}
```

**Penalty Amounts (configurable):**
- PWD Mismatch: ₱200
- Reserved Mismatch: ₱150
- Overstay (>15 min grace): ₱100
- Other: Variable

---

### 9. `notifications` Collection

**Purpose:** User notifications for events

```javascript
{
  // Document ID = Auto-generated
  
  // Notification Content
  "type": "string",                     // "PENALTY" | "RESERVATION_EXPIRING" | "PAYMENT_SUCCESS" | etc.
  "title": "string",
  "message": "string",
  
  // Additional Data
  "data": {                             // JSON object with context
    "session_id": "string",
    "penalty_id": "string",
    // ... etc
  },
  
  // Recipient
  "recipient_type": "string",           // "DRIVER" | "STAFF"
  "recipient_id": "string",             // FK → users._id or "ALL"
  
  // Status
  "read": "boolean",
  "read_at": "timestamp | null",
  
  // Metadata
  "created_at": "timestamp"
}
```

---

### 10. `session_logs` Collection (NEW)

**Purpose:** Complete history/audit trail for app's History screen

```javascript
{
  // Document ID = Auto-generated
  
  // Log Type
  "log_type": "string",                 // "SESSION_START" | "SESSION_END" | "PAYMENT" | "PENALTY" | "RESERVATION"
  "action": "string",                   // "VEHICLE_ENTERED" | "VEHICLE_EXITED" | "PAYMENT_RECEIVED" | etc.
  
  // User
  "user_id": "string | null",          // FK → users._id
  "user_name": "string | null",        // Snapshot
  
  // Related Entities
  "session_id": "string | null",
  "reservation_id": "string | null",
  "payment_id": "string | null",
  "penalty_id": "string | null",
  
  // Details
  "facility_id": "string",
  "space_code": "string | null",
  "license_plate": "string | null",
  "amount": "number | null",
  
  // Description
  "description": "string",              // Human-readable log entry
  
  // Metadata
  "timestamp": "timestamp"
}
```

**Sample Logs:**
```javascript
// Vehicle entry
{
  "log_type": "SESSION_START",
  "action": "VEHICLE_ENTERED",
  "user_id": "user_001",
  "session_id": "session_123",
  "facility_id": "facility_001",
  "space_code": "A1",
  "license_plate": "ABC-1234",
  "description": "Vehicle ABC-1234 entered space A1",
  "timestamp": "2026-01-11T10:30:00Z"
}

// Payment
{
  "log_type": "PAYMENT",
  "action": "PAYMENT_RECEIVED",
  "user_id": "user_001",
  "session_id": "session_123",
  "payment_id": "payment_456",
  "amount": 150.00,
  "description": "Payment of ₱150.00 received from Juan Dela Cruz",
  "timestamp": "2026-01-11T12:30:00Z"
}
```

---

## Key Features Summary

### 🅿️ PWD Enforcement
1. User sets `is_pwd = true` in profile
2. Vehicle registered with `is_pwd_vehicle = true`
3. PWD spaces marked with `space_type = "PWD"`
4. Backend checks at entry:
   - If `space_type == "PWD"` and vehicle NOT PWD → Create penalty
5. PWD users get discounted rate (`pwd_hourly_rate`)

### 🚗 Reserved Spot Enforcement
1. User creates reservation with specific `space_id`
2. Space marked as `is_reserved = true` with `reserved_by_license_plate`
3. Backend checks at entry:
   - If space reserved and license plate doesn't match → Create penalty
4. Violation added to session and penalty charged on exit

### 💰 Wallet System
1. Users have `wallet_balance` in PHP
2. Payments deduct from driver wallet
3. Cash payments add to staff wallet (simulation)
4. All transactions recorded in `payments` collection
5. Payment flow:
   ```
   Driver Balance: ₱500 → ₱350 (after ₱150 payment)
   Staff Balance: ₱0 → ₱150 (receives payment)
   ```

### 🔄 Recurring Reservations
1. User creates reservation with `is_recurring = true`
2. Specify pattern: `DAILY`, `WEEKLY`, `MONTHLY`
3. System auto-creates future reservations
4. Each instance links to `parent_reservation_id`

### ⏰ Auto-Expiry
1. Reservation has `grace_period_minutes` (e.g., 15)
2. If user doesn't check in by `start_time + grace_period`:
   - Status → `EXPIRED`
   - Space released
   - Notification sent

### 📊 History/Logs
1. Every action creates `session_logs` entry
2. App's History screen queries this collection
3. Shows: entries, exits, payments, penalties
4. Filterable by date, type, user

---

## Data Migration Notes

### Changes from Old Schema

| Old Collection | New Collection | Changes |
|----------------|----------------|---------|
| `users` | `users` | Added `wallet_balance`, `is_pwd`, `pwd_id_number` |
| `parking_lots` | `parking_facilities` | Renamed, added `pwd_hourly_rate` |
| `parking_spots` | `parking_spaces` | Renamed, added `space_type`, PWD fields |
| `parking_sessions` | `parking_sessions` | Standardized naming, added violation fields |
| `reservations` | `reservations` | Added recurring fields, auto-expiry |
| `vehicles` | `vehicles` | Added `is_pwd_vehicle`, `pwd_sticker_number` |
| `notifications` | `notifications` | Minimal changes |
| N/A | `payments` | **NEW** dedicated payment tracking |
| N/A | `penalties` | **NEW** violation tracking |
| N/A | `session_logs` | **NEW** for history screen |

---

## Android App Changes Required

After implementing this schema, you'll need to update:

### Repository Layer
- [ ] Update collection names: `parking_lots` → `parking_facilities`, `parking_spots` → `parking_spaces`
- [ ] Update all field names to `snake_case` (remove camelCase versions)
- [ ] Add wallet balance read/write operations
- [ ] Add PWD status handling in user profile
- [ ] Add penalty fetching and display
- [ ] Add recurring reservation creation
- [ ] Add session logs fetching for History screen

### Model Classes
- [ ] [`User.kt`](file:///c:/github%20shenanigans/LinkNPark/app/src/main/java/com/example/linknpark/model/User.kt) - Add `wallet_balance`, `is_pwd`, `pwd_id_number`
- [ ] [`ParkingLot.kt`](file:///c:/github%20shenanigans/LinkNPark/app/src/main/java/com/example/linknpark/model/ParkingLot.kt) → Rename to `ParkingFacility.kt`, add `pwd_hourly_rate`
- [ ] [`ParkingSpot.kt`](file:///c:/github%20shenanigans/LinkNPark/app/src/main/java/com/example/linknpark/model/ParkingSpot.kt) → Rename to `ParkingSpace.kt`, add `space_type`
- [ ] [`ParkingSession.kt`](file:///c:/github%20shenanigans/LinkNPark/app/src/main/java/com/example/linknpark/model/ParkingSession.kt) - Add violation fields, standardize names
- [ ] [`Reservation.kt`](file:///c:/github%20shenanigans/LinkNPark/app/src/main/java/com/example/linknpark/model/Reservation.kt) - Add recurring fields
- [ ] [`Vehicle.kt`](file:///c:/github%20shenanigans/LinkNPark/app/src/main/java/com/example/linknpark/model/Vehicle.kt) - Add PWD fields
- [ ] **NEW:** `Payment.kt`
- [ ] **NEW:** `Penalty.kt`
- [ ] **NEW:** `SessionLog.kt`

### UI Updates
- [ ] Profile screen - Add PWD toggle and wallet display
- [ ] Vehicle management - Add PWD vehicle registration
- [ ] Parking spot selection - Filter/show PWD spaces
- [ ] Payment screen - Show wallet balance, penalties
- [ ] History screen - Fetch from `session_logs` instead of `parking_sessions`
- [ ] Reservation screen - Add recurring options

### Repository Files to Update
- [ ] [`FirebaseDriverRepository.kt`](file:///c:/github%20shenanigans/LinkNPark/app/src/main/java/com/example/linknpark/data/FirebaseDriverRepository.kt)
- [ ] [`FirebaseStaffRepository.kt`](file:///c:/github%20shenanigans/LinkNPark/app/src/main/java/com/example/linknpark/data/FirebaseStaffRepository.kt)
- [ ] [`FirebaseAuthRepository.kt`](file:///c:/github%20shenanigans/LinkNPark/app/src/main/java/com/example/linknpark/data/FirebaseAuthRepository.kt)

---

## Next Steps

1. **Review this schema design** - Confirm all requirements are met
2. **Create Python scripts:**
   - Database clear script
   - Data seeding script with sample data
   - Validation script
3. **Generate updated ERD**
4. **Create migration guide** for Android app updates

---

**Questions or changes needed? Let me know before I proceed with script creation!**
