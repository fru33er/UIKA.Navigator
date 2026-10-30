import json
import os
import sys
import urllib.request
import urllib.error

PROJECT_ID = "uika-navigasi"
COLLECTION_NAME = "nodes"
JSON_FILE_PATH = "nodes.json"

def convert_to_firestore_fields(node):
    fields = {
        "id": {"integerValue": str(node["id"])},
        "x": {"doubleValue": float(node.get("x", 0.0))},
        "y": {"doubleValue": float(node.get("y", 0.0))},
        "z": {"doubleValue": float(node.get("z", 0.0))},
        "type": {"stringValue": str(node.get("type", "path"))},
        "neighbours": {
            "arrayValue": {
                "values": [{"integerValue": str(nid)} for nid in node.get("neighbours", [])]
            }
        }
    }
    
    if node.get("number"):
        fields["number"] = {"stringValue": str(node["number"])}
    else:
        fields["number"] = {"nullValue": None}

    if node.get("forwardVector"):
        fv = node["forwardVector"]
        fields["forwardVector"] = {
            "mapValue": {
                "fields": {
                    "x": {"doubleValue": float(fv.get("x", 0.0))},
                    "y": {"doubleValue": float(fv.get("y", 0.0))},
                    "z": {"doubleValue": float(fv.get("z", 0.0))},
                    "w": {"doubleValue": float(fv.get("w", 1.0))}
                }
            }
        }
    else:
        fields["forwardVector"] = {"nullValue": None}

    return fields

def upload_node(node):
    node_id = str(node["id"])
    url = f"https://firestore.googleapis.com/v1/projects/{PROJECT_ID}/databases/(default)/documents/{COLLECTION_NAME}/{node_id}"
    
    body = {
        "fields": convert_to_firestore_fields(node)
    }
    
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"}, method="PATCH")
    
    try:
        with urllib.request.urlopen(req) as response:
            return True, response.status
    except urllib.error.HTTPError as e:
        return False, f"HTTP {e.code}: {e.read().decode('utf-8')}"
    except Exception as e:
        return False, str(e)

def get_existing_firestore_doc_ids():
    url = f"https://firestore.googleapis.com/v1/projects/{PROJECT_ID}/databases/(default)/documents/{COLLECTION_NAME}?pageSize=300"
    try:
        req = urllib.request.Request(url)
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode('utf-8'))
            docs = data.get("documents", [])
            return [d.get("name", "").split("/")[-1] for d in docs]
    except Exception:
        return []

def delete_doc(doc_id):
    url = f"https://firestore.googleapis.com/v1/projects/{PROJECT_ID}/databases/(default)/documents/{COLLECTION_NAME}/{doc_id}"
    req = urllib.request.Request(url, method="DELETE")
    try:
        with urllib.request.urlopen(req) as response:
            return True
    except Exception:
        return False

def main():
    if not os.path.exists(JSON_FILE_PATH):
        print(f"[-] File '{JSON_FILE_PATH}' tidak ditemukan!")
        print(f"[!] Silakan ekspor file 'nodes.json' terlebih dahulu dari 'map_node_editor.html' dan letakkan di folder ini.")
        sys.exit(1)

    with open(JSON_FILE_PATH, "r", encoding="utf-8") as f:
        nodes = json.load(f)

    if not isinstance(nodes, list):
        print("[-] Format nodes.json harus berupa JSON Array!")
        sys.exit(1)

    print(f"[*] Memulai upload {len(nodes)} nodes ke Firebase Firestore ('{PROJECT_ID}')...\n")
    success_count = 0

    valid_ids = set()
    for node in nodes:
        node_id = str(node.get("id"))
        valid_ids.add(node_id)
        node_type = node.get("type")
        label = node.get("number") or f"Path #{node_id}"
        
        ok, res = upload_node(node)
        if ok:
            print(f"  [+] Upload Node #{node_id} ({node_type}: {label}) -> BERHASIL")
            success_count += 1
        else:
            print(f"  [-] Upload Node #{node_id} GAGAL: {res}")

    # Clean up orphan nodes if any
    existing_ids = get_existing_firestore_doc_ids()
    orphans = [did for did in existing_ids if did not in valid_ids]
    if orphans:
        print(f"\n[*] Membersihkan {len(orphans)} node lama dari Firestore...")
        for oid in orphans:
            delete_doc(oid)
            print(f"  [-] Menghapus node lama #{oid} -> BERHASIL")

    print(f"\n[V] Selesai! {success_count}/{len(nodes)} node berhasil disinkronkan ke Firebase.")

if __name__ == "__main__":
    main()
