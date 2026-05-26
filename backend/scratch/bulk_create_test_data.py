import requests
import json
import time
from datetime import datetime, timedelta

BASE_URL = "http://localhost:8080/api/blockchain/drugs"
RECEIVER_ADDRESS = "0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC"

def create_sample_data(index):
    # 1. Create Batch
    now = datetime.now()
    batch_number = f"BT{now.strftime('%Y%m%d%H%M')}{index:02d}"
    expiry_date = (now + timedelta(days=365)).strftime("%Y-%m-%dT23:59:59")
    
    batch_payload = {
        "drugName": "Azithromycin 500mg",
        "manufacturer": "Dược Hậu Giang",
        "batchNumber": batch_number,
        "quantity": 5,
        "expiryDate": expiry_date,
        "storageConditions": "Nơi khô ráo, thoáng mát"
    }
    
    print(f"[{index}] Creating batch: {batch_number}...", flush=True)
    try:
        response = requests.post(f"{BASE_URL}/batches", json=batch_payload)
        if response.status_code != 200:
            print(f"Error creating batch: {response.text}", flush=True)
            return
        
        batch_data = response.json().get("data", {})
        batch_id = batch_data.get("batchId")
        print(f"[{index}] Batch created! ID: {batch_id}", flush=True)
        
        # 2. Create Shipment
        shipment_payload = {
            "batchId": str(batch_id),
            "toAddress": RECEIVER_ADDRESS,
            "quantity": 5,
            "trackingInfo": f"Vận chuyển lô {batch_number} tới NPP",
            "notes": "Hàng dễ vỡ, bảo quản lạnh"
        }
        
        print(f"[{index}] Creating shipment for batch {batch_id}...", flush=True)
        response = requests.post(f"{BASE_URL}/shipments", json=shipment_payload)
        if response.status_code != 200:
            print(f"Error creating shipment: {response.text}", flush=True)
            return
        
        shipment_data = response.json().get("data", {})
        print(f"[{index}] Shipment created! ID: {shipment_data.get('shipmentId')}", flush=True)
        
    except Exception as e:
        print(f"Exception: {str(e)}", flush=True)

if __name__ == "__main__":
    print("🚀 Bulk creation script (Corrected Format)")
    # I won't run this automatically anymore to save your gas.
    # You can run it manually if you have enough ETH:
    # python3 scratch/bulk_create_test_data.py
