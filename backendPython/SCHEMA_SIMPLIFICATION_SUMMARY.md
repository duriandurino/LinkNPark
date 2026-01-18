# Schema Simplification Summary

## Changes Made for Demo/Prototype

Based on user feedback, the enhanced schema has been simplified to remove production-level features not needed for the diorama demo.

---

## Fields Removed

### 1. Image Storage (No Storage Buckets)
- ❌ `parking_sessions.entry_image_url` deleted
- ❌ `penalties.evidence_image_url` - Removed

### 2. AI Detection Fields (No AI Processing Saved to DB)
- ❌ `parking_sessions.entry_confidence` - Removed
- ❌ `parking_sessions.entry_device_id` - Removed

### 3. Motorcycle Support (Cars Only)
- ❌ `parking_spaces.space_type` - Removed "MOTORCYCLE" and "VIP" options
- ❌ `parking_spaces.vehicle_type` - Now only "STANDARD"
- ❌ `vehicles.vehicle_type` - Removed "MOTORCYCLE" option

---

## Fields Modified

### 1. Entry/Exit Methods
**Before:**
```javascript
entry_method: "CAMERA" | "APP" | "MANUAL"  // How car entered
```

**After:**
```javascript
exit_method: "CASH_TO_STAFF" | "WALLET_TO_STAFF" | "AUTO_PAYMENT"  // How car exits/pays
```

**Rationale:** The exit process is what varies in demonstration:
- **CASH_TO_STAFF** - Driver pays cash directly to staff
- **WALLET_TO_STAFF** - Driver pays via wallet simulation to staff
- **AUTO_PAYMENT** - Auto-pay enabled, payment deducted automatically on exit detection

### 2. Payment Methods Simplified
**Before:**
```javascript
payment_method: "WALLET" | "CASH" | "GCASH" | "MAYA"
```

**After:**
```javascript
payment_method: "WALLET" | "CASH" | "AUTO_PAY"
```

**Rationale:** No real payment gateway integration needed for demo.

### 3. Payment Status Simplified
**Before:**
```javascript
payment_status: "UNPAID" | "PENDING_CONFIRMATION" | "PAID"
```

**After:**
```javascript
payment_status: "UNPAID" | "PAID"
```

**Rationale:** No complex payment flow needed for prototype.

---

## Diorama Setup Updated

### Users (3 total, was 2)
1. **Driver 1** - PWD status, owns "DEMO-PWD1", ₱500 wallet
2. **Driver 2** - Regular driver (NEW), owns "DEMO-CAR2", ₱300 wallet
3. **Staff** - ₱0 wallet (receives payments)

### Vehicles (2 registered, was 1)
1. **DEMO-PWD1** - Driver 1's PWD toy car (blue)
2. **DEMO-CAR2** - Driver 2's regular toy car (red) (NEW)
3. **XYZ-9999** - Unregistered toy car (no vehicle record)

### Parking Spaces (4 spots in one row)
```
[A1-Available] [A2-Occupied by XYZ-9999] [A3-Available] [PWD-1-Reserved]
```

### License Plates
- Printed labels pasted on toy cars for visual demo

---

## Exit Payment Flow (New Feature)

The exit process now supports 3 methods:

### 1. CASH_TO_STAFF
- Driver exits
- Staff manually processes payment
- Cash goes to staff wallet (simulation)
- Gate opens after confirmation

### 2. WALLET_TO_STAFF  
- Driver exits
- Driver pays via wallet in app
- Amount transferred to staff wallet (simulation)
- Gate opens after payment confirmed

### 3. AUTO_PAYMENT
- Driver enables auto-pay in settings
- Overseer detects vehicle in exit area
- Payment automatically deducted from driver wallet
- Gate opens automatically
- Staff receives payment in wallet (simulation)

---

## Updated Document Counts

| Collection | Count | Notes |
|------------|-------|-------|
| users | 3 | 2 drivers + 1 staff |
| parking_facilities | 1 | Demo facility |
| parking_spaces | 4 | [A1][A2][A3][PWD-1] |
| parking_sessions | 3-4 | 1 active + 2-3 historical |
| reservations | 1 | Driver 1's PWD spot reservation |
| vehicles | 2 | 2 registered cars |
| payments | 2-3 | Historical payments |
| penalties | 1 | 1 violation for demo |
| session_logs | 10-15 | Enough for History screen |
| notifications | 2-3 | Payment/penalty alerts |
| **TOTAL** | **~30-35** | **Simplified from 215** |

---

## Files Updated

1. ✅ [`linknpark_enhanced_schema.dbdiagram`](file:///c:/github%20shenanigans/LinkNPark/backendPython/linknpark_enhanced_schema.dbdiagram) - Updated ERD
2. ✅ [`ENHANCED_SCHEMA_DESIGN.md`](file:///c:/github%20shenanigans/LinkNPark/backendPython/ENHANCED_SCHEMA_DESIGN.md) - Updated schema doc
3. ✅ [`implementation_plan.md`](file:///C:/Users/boveda/.gemini/antigravity/brain/7b84ee8b-7433-4c28-8148-caea448a8d3b/implementation_plan.md) - Updated seeding plan

---

## Next Steps

1. ✅ Schema design finalized
2. ✅ ERD file ready for visualization
3. ⏳ Create Python clearing script
4. ⏳ Create Python seeding script with simplified data
5. ⏳ Update Android app (after database setup)

**Ready to proceed with script creation!**
