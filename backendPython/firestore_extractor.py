"""
LinkNPark Firestore Database Extractor and ERD Generator

This script connects to the LinkNPark Firestore database using Firebase Admin SDK,
extracts all collections and their documents, analyzes the schema, and generates
an ERD diagram in dbdiagram.io format.

Author: Antigravity AI
Date: 2026-01-10
"""

import firebase_admin
from firebase_admin import credentials, firestore
import json
from datetime import datetime
from collections import defaultdict
from typing import Dict, List, Set, Any


class FirestoreSchemaAnalyzer:
    """Analyzes Firestore database schema and generates ERD"""
    
    def __init__(self, cred_path: str):
        """Initialize Firebase Admin SDK"""
        print(f"🔑 Loading credentials from: {cred_path}")
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
        self.db = firestore.client()
        
        # Schema storage
        self.collections_data: Dict[str, List[Dict]] = {}
        self.schema: Dict[str, Dict[str, Set]] = defaultdict(lambda: defaultdict(set))
        
    def extract_all_data(self):
        """Extract all data from Firestore collections"""
        # Known collections from Android app analysis
        collections = [
            'users',
            'parking_lots',
            'parking_spots',
            'parking_sessions',
            'reservations',
            'vehicles',
            'notifications'
        ]
        
        print("\n📊 Extracting data from Firestore...")
        print("=" * 60)
        
        for collection_name in collections:
            print(f"\n📁 Collection: {collection_name}")
            try:
                docs = self.db.collection(collection_name).stream()
                documents = []
                
                for doc in docs:
                    doc_data = doc.to_dict()
                    doc_data['_id'] = doc.id  # Preserve document ID
                    documents.append(doc_data)
                    
                    # Analyze schema
                    self._analyze_document(collection_name, doc_data)
                
                self.collections_data[collection_name] = documents
                print(f"   ✓ Extracted {len(documents)} documents")
                
            except Exception as e:
                print(f"   ✗ Error: {e}")
                self.collections_data[collection_name] = []
        
        print("\n" + "=" * 60)
        print(f"✓ Extraction complete!\n")
        
    def _analyze_document(self, collection_name: str, doc_data: Dict):
        """Analyze a single document to build schema"""
        for field, value in doc_data.items():
            # Determine field type
            field_type = self._get_field_type(value)
            self.schema[collection_name][field].add(field_type)
    
    def _get_field_type(self, value: Any) -> str:
        """Determine the SQL-like type for a field value"""
        if value is None:
            return "NULL"
        elif isinstance(value, bool):
            return "BOOLEAN"
        elif isinstance(value, int):
            return "INTEGER"
        elif isinstance(value, float):
            return "DOUBLE"
        elif isinstance(value, str):
            return "VARCHAR"
        elif isinstance(value, datetime):
            return "TIMESTAMP"
        elif hasattr(value, 'seconds'):  # Firebase Timestamp
            return "TIMESTAMP"
        elif isinstance(value, dict):
            return "MAP"
        elif isinstance(value, list):
            return "ARRAY"
        else:
            return "VARCHAR"
    
    def print_schema_summary(self):
        """Print a summary of the database schema"""
        print("\n" + "=" * 60)
        print("📋 DATABASE SCHEMA SUMMARY")
        print("=" * 60)
        
        for collection, fields in sorted(self.schema.items()):
            print(f"\n📦 {collection.upper()}")
            print("-" * 60)
            
            for field, types in sorted(fields.items()):
                type_str = " | ".join(sorted(types))
                print(f"   • {field:30s} : {type_str}")
    
    def generate_dbdiagram_erd(self) -> str:
        """Generate ERD in dbdiagram.io format"""
        print("\n" + "=" * 60)
        print("🎨 GENERATING ERD FOR dbdiagram.io")
        print("=" * 60)
        
        erd_lines = []
        erd_lines.append("// LinkNPark Database Schema")
        erd_lines.append("// Generated: " + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
        erd_lines.append("// Smart Parking Management System")
        erd_lines.append("")
        
        # Define tables
        for collection, fields in sorted(self.schema.items()):
            table_name = collection
            erd_lines.append(f"Table {table_name} {{")
            
            # Add fields
            for field, types in sorted(fields.items()):
                # Use the most common type (or first one)
                field_type = self._map_to_sql_type(list(types)[0])
                
                # Mark primary key
                if field == '_id' or field.endswith('_id') or field.endswith('Id'):
                    if field == '_id' or field == f'{collection[:-1]}_id' or field == f'{table_name[:-1]}Id':
                        erd_lines.append(f"  {field} {field_type} [pk]")
                    else:
                        erd_lines.append(f"  {field} {field_type}")
                else:
                    erd_lines.append(f"  {field} {field_type}")
            
            erd_lines.append("}")
            erd_lines.append("")
        
        # Define relationships based on foreign keys
        erd_lines.append("// Relationships")
        relationships = self._identify_relationships()
        
        for rel in relationships:
            erd_lines.append(f"Ref: {rel['from_table']}.{rel['from_field']} > {rel['to_table']}.{rel['to_field']}")
        
        return "\n".join(erd_lines)
    
    def _map_to_sql_type(self, firestore_type: str) -> str:
        """Map Firestore types to SQL types for dbdiagram"""
        type_mapping = {
            "VARCHAR": "varchar",
            "INTEGER": "int",
            "DOUBLE": "decimal",
            "BOOLEAN": "boolean",
            "TIMESTAMP": "timestamp",
            "MAP": "json",
            "ARRAY": "json",
            "NULL": "varchar"
        }
        return type_mapping.get(firestore_type, "varchar")
    
    def _identify_relationships(self) -> List[Dict]:
        """Identify foreign key relationships between collections"""
        relationships = []
        
        # Known relationships from Android app analysis
        known_rels = [
            # Users relations
            {"from_table": "parking_sessions", "from_field": "user_id", "to_table": "users", "to_field": "_id"},
            {"from_table": "reservations", "from_field": "user_id", "to_table": "users", "to_field": "_id"},
            {"from_table": "vehicles", "from_field": "user_id", "to_table": "users", "to_field": "_id"},
            {"from_table": "notifications", "from_field": "recipient_id", "to_table": "users", "to_field": "_id"},
            
            # Parking lots relations
            {"from_table": "parking_spots", "from_field": "lot_id", "to_table": "parking_lots", "to_field": "_id"},
            {"from_table": "parking_sessions", "from_field": "lot_id", "to_table": "parking_lots", "to_field": "_id"},
            {"from_table": "reservations", "from_field": "lot_id", "to_table": "parking_lots", "to_field": "_id"},
            
            # Parking spots relations
            {"from_table": "parking_sessions", "from_field": "spot_id", "to_table": "parking_spots", "to_field": "_id"},
            
            # Reservations relations
            {"from_table": "parking_sessions", "from_field": "session_id", "to_table": "reservations", "to_field": "_id"},
        ]
        
        # Filter to only include relationships where both fields exist
        for rel in known_rels:
            from_table = rel["from_table"]
            to_table = rel["to_table"]
            from_field = rel["from_field"]
            to_field = rel["to_field"]
            
            if (from_table in self.schema and 
                to_table in self.schema and
                from_field in self.schema[from_table] and
                to_field in self.schema[to_table]):
                relationships.append(rel)
        
        return relationships
    
    def save_data_to_json(self, output_file: str = "firestore_data_dump.json"):
        """Save all extracted data to JSON file"""
        print(f"\n💾 Saving data to {output_file}...")
        
        # Convert Firebase timestamps to strings
        data_to_save = {}
        for collection, documents in self.collections_data.items():
            clean_docs = []
            for doc in documents:
                clean_doc = {}
                for key, value in doc.items():
                    if hasattr(value, 'seconds'):  # Firebase Timestamp
                        clean_doc[key] = datetime.fromtimestamp(value.seconds).isoformat()
                    else:
                        clean_doc[key] = value
                clean_docs.append(clean_doc)
            data_to_save[collection] = clean_docs
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(data_to_save, f, indent=2, ensure_ascii=False, default=str)
        
        print(f"   ✓ Data saved successfully!\n")
    
    def generate_report(self):
        """Generate a comprehensive database report"""
        print("\n" + "=" * 60)
        print("📊 DATABASE STATISTICS")
        print("=" * 60)
        
        total_docs = sum(len(docs) for docs in self.collections_data.values())
        
        print(f"\n   Total Collections: {len(self.collections_data)}")
        print(f"   Total Documents:   {total_docs}\n")
        
        for collection, documents in sorted(self.collections_data.items()):
            print(f"   • {collection:20s} : {len(documents):4d} documents")
        
        print("\n" + "=" * 60)


def main():
    """Main execution function"""
    print("=" * 60)
    print("  🚗 LinkNPark Firestore Database Analyzer")
    print("  📊 Extract, Analyze & Generate ERD")
    print("=" * 60)
    
    # Path to Firebase Admin SDK credentials
    # Update this path if needed
    cred_path = r"c:\github shenanigans\LinkNPark\backendPython\Multi-Camera-Live-Object-Tracking\linknpark-a9074-firebase-adminsdk-fbsvc-63c039119f.json"
    
    try:
        # Initialize analyzer
        analyzer = FirestoreSchemaAnalyzer(cred_path)
        
        # Extract all data
        analyzer.extract_all_data()
        
        # Print schema summary
        analyzer.print_schema_summary()
        
        # Generate statistics
        analyzer.generate_report()
        
        # Save data to JSON
        analyzer.save_data_to_json("firestore_data_dump.json")
        
        # Generate ERD
        erd_content = analyzer.generate_dbdiagram_erd()
        
        # Save ERD to file
        erd_filename = "linknpark_erd.dbdiagram"
        with open(erd_filename, 'w', encoding='utf-8') as f:
            f.write(erd_content)
        
        print(f"\n✅ ERD saved to: {erd_filename}")
        print(f"\n📌 Next steps:")
        print(f"   1. Open https://dbdiagram.io/d")
        print(f"   2. Paste the contents of {erd_filename}")
        print(f"   3. Visualize your database schema!")
        
        print("\n" + "=" * 60)
        print("✨ Analysis Complete!")
        print("=" * 60)
        
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()
