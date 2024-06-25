import os
import requests
import json
import tempfile
try:
    import pymongo
except ImportError:
    print('Error: pymongo not found. Run \'pip install pymongo\' to install pymongo.')
    exit()

# Zenodo record ID for the dataset 
ZENODO_RECORD = "8123547"


def get_models_to_links_pipeline():
    return [
        {'$project':{'_id': '$_id','model_id': '$_id','private': {'$ne': [None, None]}}},
        {'$merge':{'into': 'Link','on': '_id','whenMatched': 'replace','whenNotMatched': 'insert'}}
    ]

def get_originals_to_links_pipeline():
    return [
        {'$match': {'$expr': {'$eq': ['$original', '$_id']}}},
        {'$project':{'_id': {"$concat": ['$_id',"original"]},'model_id': '$_id','private': {'$eq': [None, None]}}},
        {'$merge':{'into': 'Link','on': '_id','whenMatched': 'replace','whenNotMatched': 'insert'}}
    ]


def populate_database():
    client = pymongo.MongoClient()
    collection = client["meteor"]["Model"]
    record_id = ZENODO_RECORD

    collection.delete_many({})
    client["meteor"]["Link"].delete_many({})

    base_url = f"https://zenodo.org/api/records/{record_id}"
    response = requests.get(base_url)
    if response.status_code != 200:
        print(f"Failed to fetch record details for {record_id}")
        return

    record_data = response.json()
    files = record_data.get("files")

    if not files:
        print("No files found in the Zenodo record.")
        return


    # Download each file from the record
    for file_info in files:
        file_url = file_info["links"]["self"]
        file_name = file_info["key"]

        print("Downloading "+file_name)

        # Download the file
        response = requests.get(file_url)
        if response.status_code == 200:
            with tempfile.TemporaryFile() as file:
                file.write(response.content)
                file.seek(0)
                collection.insert_many([json.loads(line.strip())  for line in file.readlines()])
        else:
            print(f"Failed to download {file_name}")

    print("Generating Links")
    collection.aggregate(get_models_to_links_pipeline())
    collection.aggregate(get_originals_to_links_pipeline())

        
if __name__ == '__main__':
    populate_database()