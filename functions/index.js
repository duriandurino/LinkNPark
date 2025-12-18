const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();
const db = admin.firestore();
const rtdb = admin.database();
// ============================================
// ENTRY QUEUE PROCESSOR
// ============================================
// Triggers when new entry is added to RTDB
// Creates parking session in Firestore
// Sends barrier open command back to ESP32
exports.processEntryQueue = functions.database
  .ref('/iot/entry_queue/{entryId}')
  .onCreate(async (snapshot, context) => {
    const entryId = context.params.entryId;
    const data = snapshot.val();
    
    console.log('🚗 Processing entry:', entryId, data);
    
    try {
      // Check if already processed
      if (data.processed) {
        console.log('Already processed, skipping');
        return null;
      }
      
      const licensePlate = data.licensePlate;
      const deviceId = data.deviceId;
      
      // 1. Check for active reservation
      const reservationsSnapshot = await db.collection('reservations')
        .where('licensePlate', '==', licensePlate)
        .where('status', '==', 'ACTIVE')
        .orderBy('reservedFrom', 'desc')
        .limit(1)
        .get();
      
      let sessionData = {
        licensePlate: licensePlate,
        enteredAt: admin.firestore.FieldValue.serverTimestamp(),
        status: 'ACTIVE',
        paymentStatus: 'PENDING',
        entryMethod: 'IoT_LPR',
        deviceId: deviceId,
        imageUrl: data.imageUrl || null,
        confidence: data.confidence || 0.0
      };
      
      if (!reservationsSnapshot.empty) {
        // Has reservation - use reserved spot
        const reservation = reservationsSnapshot.docs[0].data();
        const reservationId = reservationsSnapshot.docs[0].id;
        
        sessionData.userId = reservation.userId;
        sessionData.spotId = reservation.spotId;
        sessionData.spotCode = reservation.spotCode;
        sessionData.hourlyRate = reservation.hourlyRate || 50.0;
        sessionData.reservationId = reservationId;
        sessionData.lotId = reservation.lotId || 'main_lot';
        
        // Update reservation status
        await db.collection('reservations').doc(reservationId).update({
          status: 'IN_USE',
          actualEntryTime: admin.firestore.FieldValue.serverTimestamp()
        });
        
        console.log('✓ Reservation found:', reservationId);
      } else {
        // Walk-in - find first available spot
        const availableSpotsSnapshot = await db.collection('parking_spots')
          .where('status', '==', 'AVAILABLE')
          .where('lotId', '==', 'main_lot')
          .limit(1)
          .get();
        
        if (!availableSpotsSnapshot.empty) {
          const spotDoc = availableSpotsSnapshot.docs[0];
          const spot = spotDoc.data();
          
          sessionData.spotId = spotDoc.id;
          sessionData.spotCode = spot.code;
          sessionData.hourlyRate = spot.hourly_rate || 50.0;
          sessionData.lotId = 'main_lot';
          
          console.log('✓ Assigned walk-in spot:', spot.code);
        } else {
          // No spots available
          console.log('✗ No available spots');
          await snapshot.ref.update({ 
            processed: true, 
            error: 'NO_SPOTS_AVAILABLE',
            processedAt: Date.now()
          });
          
          // Send denial command to ESP32
          await rtdb.ref(`/iot/commands/${deviceId}`).set({
            action: 'DENY_ENTRY',
            timestamp: Date.now(),
            executed: false,
            reason: 'No available spots'
          });
          
          return null;
        }
      }
      
      // 2. Create parking session in Firestore
      const sessionRef = await db.collection('parking_sessions').add(sessionData);
      console.log('✓ Session created:', sessionRef.id);
      
      // 3. Update spot status to OCCUPIED
      if (sessionData.spotId) {
        await db.collection('parking_spots').doc(sessionData.spotId).update({
          status: 'OCCUPIED',
          currentSessionId: sessionRef.id,
          currentCarLabel: licensePlate,
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        console.log('✓ Spot marked occupied:', sessionData.spotCode);
      }
      
      // 4. Send OPEN_BARRIER command to ESP32
      await rtdb.ref(`/iot/commands/${deviceId}`).set({
        action: 'OPEN_BARRIER',
        timestamp: Date.now(),
        executed: false,
        sessionId: sessionRef.id,
        spotCode: sessionData.spotCode || 'N/A',
        reason: 'entry_approved'
      });
      console.log('✓ Barrier command sent');
      
      // 5. Mark entry as processed
      await snapshot.ref.update({ 
        processed: true,
        sessionId: sessionRef.id,
        spotCode: sessionData.spotCode,
        processedAt: Date.now()
      });
      
      // 6. Log to RTDB
      await rtdb.ref('/logs').push({
        level: 'INFO',
        message: `Entry processed: ${licensePlate} → ${sessionData.spotCode}`,
        deviceId: deviceId,
        timestamp: Date.now(),
        sessionId: sessionRef.id,
        licensePlate: licensePlate
      });
      
      console.log('✅ Entry processing complete');
      return null;
      
    } catch (error) {
      console.error('❌ Error processing entry:', error);
      
      // Mark as processed with error
      await snapshot.ref.update({ 
        processed: true, 
        error: error.message,
        processedAt: Date.now()
      });
      
      // Log error
      await rtdb.ref('/logs').push({
        level: 'ERROR',
        message: `Entry processing failed: ${error.message}`,
        deviceId: data.deviceId,
        timestamp: Date.now(),
        licensePlate: data.licensePlate,
        errorDetails: error.stack
      });
      
      return null;
    }
  });
// ============================================
// EXIT QUEUE PROCESSOR
// ============================================
// Triggers when vehicle exits
// Calculates fee, checks payment
// Opens barrier if paid, marks spot available
exports.processExitQueue = functions.database
  .ref('/iot/exit_queue/{exitId}')
  .onCreate(async (snapshot, context) => {
    const exitId = context.params.exitId;
    const data = snapshot.val();
    
    console.log('🚙 Processing exit:', exitId, data);
    
    try {
      if (data.processed) {
        return null;
      }
      
      const licensePlate = data.licensePlate;
      const deviceId = data.deviceId;
      
      // 1. Find active session
      const sessionSnapshot = await db.collection('parking_sessions')
        .where('licensePlate', '==', licensePlate)
        .where('status', '==', 'ACTIVE')
        .limit(1)
        .get();
      
      if (sessionSnapshot.empty) {
        console.log('✗ No active session found');
        await snapshot.ref.update({ 
          processed: true, 
          error: 'NO_ACTIVE_SESSION',
          processedAt: Date.now()
        });
        
        // Deny exit
        await rtdb.ref(`/iot/commands/${deviceId}`).set({
          action: 'DENY_EXIT',
          timestamp: Date.now(),
          executed: false,
          reason: 'No active session'
        });
        
        return null;
      }
      
      const sessionDoc = sessionSnapshot.docs[0];
      const session = sessionDoc.data();
      const sessionId = sessionDoc.id;
      
      // 2. Calculate fee
      const entryTime = session.enteredAt?.toDate() || new Date();
      const exitTime = new Date();
      const durationMs = exitTime - entryTime;
      const durationMinutes = Math.floor(durationMs / 60000);
      const durationHours = durationMs / (1000 * 60 * 60);
      const billableHours = Math.ceil(durationHours);
      const totalAmount = billableHours * (session.hourlyRate || 50.0);
      
      console.log(`Duration: ${durationMinutes} min, Fee: ₱${totalAmount}`);
      
      // 3. Update session with exit time and fee
      await sessionDoc.ref.update({
        exitedAt: admin.firestore.FieldValue.serverTimestamp(),
        totalAmount: totalAmount,
        durationMinutes: durationMinutes,
        status: 'PENDING_PAYMENT'
      });
      
      // 4. Check if payment already confirmed
      if (session.paymentStatus === 'PAID') {
        // Payment confirmed - complete session
        await sessionDoc.ref.update({
          status: 'COMPLETED',
          completedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        
        // Mark spot as available
        if (session.spotId) {
          await db.collection('parking_spots').doc(session.spotId).update({
            status: 'AVAILABLE',
            currentSessionId: null,
            currentCarLabel: null,
            updatedAt: admin.firestore.FieldValue.serverTimestamp()
          });
          console.log('✓ Spot marked available');
        }
        
        // Open barrier
        await rtdb.ref(`/iot/commands/${deviceId}`).set({
          action: 'OPEN_BARRIER',
          timestamp: Date.now(),
          executed: false,
          sessionId: sessionId,
          reason: 'payment_confirmed'
        });
        
        console.log('✓ Exit approved (paid)');
      } else {
        // Payment pending - notify staff
        console.log('⏳ Waiting for payment confirmation');
        
        // Don't open barrier yet
        await rtdb.ref(`/iot/commands/${deviceId}`).set({
          action: 'WAIT_PAYMENT',
          timestamp: Date.now(),
          executed: false,
          sessionId: sessionId,
          totalAmount: totalAmount,
          reason: 'payment_pending'
        });
      }
      
      // 5. Mark as processed
      await snapshot.ref.update({ 
        processed: true,
        sessionId: sessionId,
        totalAmount: totalAmount,
        durationMinutes: durationMinutes,
        processedAt: Date.now()
      });
      
      // 6. Log
      await rtdb.ref('/logs').push({
        level: 'INFO',
        message: `Exit processed: ${licensePlate}, Fee: ₱${totalAmount}`,
        deviceId: deviceId,
        timestamp: Date.now(),
        sessionId: sessionId,
        licensePlate: licensePlate
      });
      
      console.log('✅ Exit processing complete');
      return null;
      
    } catch (error) {
      console.error('❌ Error processing exit:', error);
      
      await snapshot.ref.update({ 
        processed: true, 
        error: error.message,
        processedAt: Date.now()
      });
      
      await rtdb.ref('/logs').push({
        level: 'ERROR',
        message: `Exit processing failed: ${error.message}`,
        deviceId: data.deviceId,
        timestamp: Date.now(),
        errorDetails: error.stack
      });
      
      return null;
    }
  });
// ============================================
// SPOT STATUS SYNC
// ============================================
// Syncs real-time spot status from ESP32 sensors
// to Firestore parking_spots
exports.syncSpotStatus = functions.database
  .ref('/iot/spot_status/{spotId}')
  .onWrite(async (change, context) => {
    const spotId = context.params.spotId;
    const newData = change.after.val();
    
    if (!newData) {
      console.log('Spot status deleted:', spotId);
      return null;
    }
    
    try {
      // Find spot by code
      const spotSnapshot = await db.collection('parking_spots')
        .where('code', '==', spotId)
        .limit(1)
        .get();
      
      if (spotSnapshot.empty) {
        console.log('Spot not found in Firestore:', spotId);
        return null;
      }
      
      const spotDoc = spotSnapshot.docs[0];
      
      // Update status
      await spotDoc.ref.update({
        status: newData.status,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        lastSensorUpdate: newData.updatedAt || Date.now()
      });
      
      console.log('✓ Spot status synced:', spotId, newData.status);
      return null;
      
    } catch (error) {
      console.error('Error syncing spot status:', error);
      return null;
    }
  });
// ============================================
// PAYMENT CONFIRMATION TRIGGER
// ============================================
// When payment is confirmed in Firestore
// Send barrier open command if vehicle at exit
exports.onPaymentConfirmed = functions.firestore
  .document('parking_sessions/{sessionId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    const sessionId = context.params.sessionId;
    
    // Check if payment status changed to PAID
    if (before.paymentStatus !== 'PAID' && after.paymentStatus === 'PAID') {
      console.log('💰 Payment confirmed for session:', sessionId);
      
      try {
        // If status is PENDING_PAYMENT or ACTIVE (vehicle at exit or staff confirmed), complete session
        if (after.status === 'PENDING_PAYMENT' || after.status === 'ACTIVE') {
          // Complete session
          await change.after.ref.update({
            status: 'COMPLETED',
            completedAt: admin.firestore.FieldValue.serverTimestamp()
          });
          
          // Mark spot available
          if (after.spotId) {
            await db.collection('parking_spots').doc(after.spotId).update({
              status: 'AVAILABLE',
              currentSessionId: '',
              currentCarLabel: '',
              updatedAt: admin.firestore.FieldValue.serverTimestamp()
            });
            console.log('✓ Spot marked available:', after.spotId);
          }
          
          // Send barrier command (if device ID available)
          if (after.deviceId) {
            await rtdb.ref(`/iot/commands/${after.deviceId}`).set({
              action: 'OPEN_BARRIER',
              timestamp: Date.now(),
              executed: false,
              sessionId: sessionId,
              reason: 'payment_confirmed'
            });
            
            console.log('✓ Barrier open command sent');
          }
        }
        
        // Log successful payment
        await rtdb.ref('/logs').push({
          level: 'INFO',
          message: `Payment confirmed: ${after.licensePlate}, ₱${after.totalAmount}`,
          timestamp: Date.now(),
          sessionId: sessionId,
          paymentMethod: after.paymentMethod || 'UNKNOWN'
        });
        
        return null;
      } catch (error) {
        console.error('Error handling payment confirmation:', error);
        return null;
      }
    }
    
    return null;
  });

// ============================================
// AUTO-PAY PROCESSOR
// ============================================
// Triggers when Camera 2 (Overseer) detects vehicle leaving
// Checks if auto-pay enabled, auto-charges and completes session
exports.processAutoPay = functions.database
  .ref('/iot/auto_pay_trigger/{triggerId}')
  .onCreate(async (snapshot, context) => {
    const triggerId = context.params.triggerId;
    const data = snapshot.val();
    
    console.log('💳 Processing auto-pay trigger:', triggerId, data);
    
    try {
      const licensePlate = data.licensePlate;
      const spotCode = data.spotCode;
      
      // 1. Find active session
      const sessionSnapshot = await db.collection('parking_sessions')
        .where('licensePlate', '==', licensePlate)
        .where('status', '==', 'ACTIVE')
        .limit(1)
        .get();
      
      if (sessionSnapshot.empty) {
        console.log('No active session for auto-pay:', licensePlate);
        await snapshot.ref.update({
          processed: true,
          error: 'NO_ACTIVE_SESSION',
          processedAt: Date.now()
        });
        return null;
      }
      
      const sessionDoc = sessionSnapshot.docs[0];
      const session = sessionDoc.data();
      const sessionId = sessionDoc.id;
      
      // 2. Check if user has auto-pay enabled
      const userId = session.userId;
      if (!userId) {
        console.log('No userId - cannot check auto-pay');
        await snapshot.ref.update({
          processed: true,
          autoPayEnabled: false,
          reason: 'NO_USER_ID',
          processedAt: Date.now()
        });
        return null;
      }
      
      const userDoc = await db.collection('users').doc(userId).get();
      if (!userDoc.exists) {
        console.log('User not found:', userId);
        await snapshot.ref.update({
          processed: true,
          autoPayEnabled: false,
          reason: 'USER_NOT_FOUND',
          processedAt: Date.now()
        });
        return null;
      }
      
      const userData = userDoc.data();
      if (!userData.autoPayEnabled) {
        console.log('Auto-pay not enabled for user:', userId);
        await snapshot.ref.update({
          processed: true,
          autoPayEnabled: false,
          reason: 'AUTO_PAY_DISABLED',
          processedAt: Date.now()
        });
        return null;
      }
      
      // 3. Calculate fee
      const entryTime = session.enteredAt?.toDate() || new Date();
      const exitTime = new Date();
      const durationMs = exitTime - entryTime;
      const durationMinutes = Math.floor(durationMs / 60000);
      const durationHours = durationMs / (1000 * 60 * 60);
      const billableHours = Math.ceil(durationHours);
      const totalAmount = billableHours * (session.hourlyRate || 50.0);
      
      console.log(`Auto-pay: ${licensePlate}, Duration: ${durationMinutes}min, Fee: ₱${totalAmount}`);
      
      // 4. Auto-complete session
      await sessionDoc.ref.update({
        status: 'COMPLETED',
        exitedAt: admin.firestore.FieldValue.serverTimestamp(),
        totalAmount: totalAmount,
        durationMinutes: durationMinutes,
        paymentStatus: 'PAID',
        paymentMethod: 'AUTO_PAY',
        paidAt: admin.firestore.FieldValue.serverTimestamp(),
        confirmedBy: 'SYSTEM_AUTO_PAY'
      });
      
      console.log('✓ Session auto-completed:', sessionId);
      
      // 5. Free spot
      if (session.spotId) {
        await db.collection('parking_spots').doc(session.spotId).update({
          status: 'AVAILABLE',
          currentSessionId: '',
          currentCarLabel: '',
          updatedAt: admin.firestore.FieldValue.serverTimestamp()
        });
        console.log('✓ Spot marked available:', session.spotCode);
      }
      
      // 6. Send barrier open command
      const deviceId = session.deviceId || data.deviceId || 'CAM2_OVERSEER';
      await rtdb.ref(`/iot/commands/${deviceId}`).set({
        action: 'OPEN_BARRIER',
        timestamp: Date.now(),
        executed: false,
        sessionId: sessionId,
        reason: 'auto_pay_completed'
      });
      
      // 7. Mark trigger as processed
      await snapshot.ref.update({
        processed: true,
        autoPayEnabled: true,
        sessionId: sessionId,
        totalAmount: totalAmount,
        processedAt: Date.now()
      });
      
      // 8. Log
      await rtdb.ref('/logs').push({
        level: 'INFO',
        message: `Auto-pay completed: ${licensePlate}, ₱${totalAmount}`,
        sessionId: sessionId,
        userId: userId,
        timestamp: Date.now()
      });
      
      console.log('✅ Auto-pay processing complete');
      return null;
      
    } catch (error) {
      console.error('❌ Error processing auto-pay:', error);
      
      await snapshot.ref.update({
        processed: true,
        error: error.message,
        processedAt: Date.now()
      });
      
      await rtdb.ref('/logs').push({
        level: 'ERROR',
        message: `Auto-pay failed: ${error.message}`,
        timestamp: Date.now(),
        licensePlate: data.licensePlate,
        errorDetails: error.stack
      });
      
      return null;
    }
  });
// ============================================
// DEVICE HEALTH MONITOR
// ============================================
// Checks device last seen timestamps
// Marks devices offline if no heartbeat
exports.monitorDeviceHealth = functions.pubsub
  .schedule('every 5 minutes')
  .onRun(async (context) => {
    console.log('🔍 Checking device health...');
    
    try {
      const devicesSnapshot = await rtdb.ref('/iot/devices').once('value');
      const devices = devicesSnapshot.val();
      
      if (!devices) {
        return null;
      }
      
      const now = Date.now();
      const offlineThreshold = 5 * 60 * 1000; // 5 minutes
      
      for (const [deviceId, device] of Object.entries(devices)) {
        const lastSeen = device.lastSeen || 0;
        const timeSinceLastSeen = now - lastSeen;
        
        if (timeSinceLastSeen > offlineThreshold && device.status === 'ONLINE') {
          // Mark as offline
          await rtdb.ref(`/iot/devices/${deviceId}`).update({
            status: 'OFFLINE',
            offlineSince: now
          });
          
          // Log
          await rtdb.ref('/logs').push({
            level: 'WARNING',
            message: `Device went offline: ${deviceId}`,
            deviceId: deviceId,
            timestamp: now,
            lastSeen: lastSeen
          });
          
          console.log('⚠️ Device offline:', deviceId);
        }
      }
      
      console.log('✓ Device health check complete');
      return null;
      
    } catch (error) {
      console.error('Error monitoring device health:', error);
      return null;
    }
  });
// ============================================
// CLEANUP OLD LOGS
// ============================================
// Removes processed queue items and old logs
// Runs daily at midnight
exports.cleanupOldData = functions.pubsub
  .schedule('every day 00:00')
  .timeZone('Asia/Manila')
  .onRun(async (context) => {
    console.log('🧹 Cleaning up old data...');
    
    try {
      const now = Date.now();
      const oneDayAgo = now - (24 * 60 * 60 * 1000);
      
      // Clean entry queue
      const entryQueueSnapshot = await rtdb.ref('/iot/entry_queue')
        .orderByChild('processedAt')
        .endAt(oneDayAgo)
        .once('value');
      
      const entryUpdates = {};
      entryQueueSnapshot.forEach(child => {
        entryUpdates[child.key] = null;
      });
      
      if (Object.keys(entryUpdates).length > 0) {
        await rtdb.ref('/iot/entry_queue').update(entryUpdates);
        console.log(`✓ Removed ${Object.keys(entryUpdates).length} old entry queue items`);
      }
      
      // Clean exit queue
      const exitQueueSnapshot = await rtdb.ref('/iot/exit_queue')
        .orderByChild('processedAt')
        .endAt(oneDayAgo)
        .once('value');
      
      const exitUpdates = {};
      exitQueueSnapshot.forEach(child => {
        exitUpdates[child.key] = null;
      });
      
      if (Object.keys(exitUpdates).length > 0) {
        await rtdb.ref('/iot/exit_queue').update(exitUpdates);
        console.log(`✓ Removed ${Object.keys(exitUpdates).length} old exit queue items`);
      }
      
      // Clean old logs (keep 7 days)
      const sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000);
      const logsSnapshot = await rtdb.ref('/logs')
        .orderByChild('timestamp')
        .endAt(sevenDaysAgo)
        .once('value');
      
      const logUpdates = {};
      logsSnapshot.forEach(child => {
        logUpdates[child.key] = null;
      });
      
      if (Object.keys(logUpdates).length > 0) {
        await rtdb.ref('/logs').update(logUpdates);
        console.log(`✓ Removed ${Object.keys(logUpdates).length} old log entries`);
      }
      
      console.log('✅ Cleanup complete');
      return null;
      
    } catch (error) {
      console.error('Error during cleanup:', error);
      return null;
    }
  });