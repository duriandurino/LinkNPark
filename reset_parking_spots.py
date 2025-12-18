"""
Reset Parking Spots to Available
Resets spot_001, spot_002, and spot_003 to AVAILABLE status
Standalone script - does NOT depend on firebase_integration.py
"""

import firebase_admin
from firebase_admin import credentials, firestore

print("=" * 60)
print("Resetting Parking Spots to AVAILABLE")
print("=" * 60)

try:
    # Initialize Firebase Admin SDK
    cred_path = r'c:\github shenanigans\LinkNPark\linknpark-a9074-firebase-adminsdk-fbsvc-63c039119f.json'
    
    # Check if already initialized
    try:
        app = firebase_admin.get_app()
        print("✓ Using existing Firebase connection\n")
    except ValueError:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
        print("✓ Firebase initialized\n")
    
    db = firestore.client()
    
    # Define the reset data
    spots_to_reset = ['spot_001', 'spot_002', 'spot_003']
    
    print(f"Resetting {len(spots_to_reset)} spots...\n")
    
    for spot_id in spots_to_reset:
        spot_ref = db.collection('parking_spots').document(spot_id)
        spot_doc = spot_ref.get()
        
        if not spot_doc.exists:
            print(f"⚠️  {spot_id}: Not found, skipping")
            continue
        
        spot_data = spot_doc.to_dict()
        
        # Show current status
        current_status = spot_data.get('status', 'UNKNOWN')
        current_car = spot_data.get('current_car_label', 'None')
        spot_code = spot_data.get('spot_code', 'N/A')
        
        print(f"📍 {spot_id} ({spot_code}):")
        print(f"   Current: {current_status}, Car: {current_car}")
        
        # Reset to AVAILABLE
        update_data = {
            'status': 'AVAILABLE',
            'is_available': True,
            'is_occupied': False,
            'is_reserved': False,
            'current_car_label': None,
            'currentCarLabel': None,  # Also clear camelCase version if exists
            'occupied_by_session_id': None,
            'currentSessionId': None,  # Also clear camelCase version if exists
            'reserved_by_user_id': None
        }
        
        spot_ref.update(update_data)
        print(f"   ✅ Reset to AVAILABLE\n")
    
    print("=" * 60)
    print("✅ All spots reset successfully!")
    print("=" * 60)
    print("\nVerify in Firebase Console:")
    print("https://console.firebase.google.com/project/linknpark-a9074/firestore/databases/-default-/data/~2Fparking_spots")
    
except Exception as e:
    print(f"\n❌ Error: {e}")
    import traceback
    traceback.print_exc()

