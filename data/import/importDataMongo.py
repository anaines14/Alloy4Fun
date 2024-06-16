import os
import json
from argparse import Namespace
from argparse import ArgumentParser

try:
    import pymongo
    from pymongo.database import Database
except ImportError:
    print('Error: pymongo not found. Run \'pip install pymongo\' to install pymongo.')
    exit()

def insert_data(file_path: str, collection_name: str='meteor') -> None:
    # Insert Alloy4Fun model data into the MongoDB collections Model and Link
    # Args:
    #   file_path: path to JSON file containing Alloy4Fun model data
    #   collection_name: name of MongoDB collection to insert JSON objects into (default: "meteor")
    # Returns:
    #   None

    # Get the file ID
    model_id = os.path.basename(file_path).split('.')[0]
    # Connect to the MongoDB client
    client = pymongo.MongoClient()
    db = client[collection_name]
    # Insert data into the MongoDB collections
    insert_models(file_path, db)
    insert_link(model_id, db)

def insert_models(file_path: str, db: Database) -> None:
    # Insert the model data into the Model collection
    # Args:
    #   file_path: path to JSON file containing Alloy4Fun model data
    #   db: MongoDB database object
    # Returns:
    #   None

    collection = db['Model']
    # Open the JSON file
    with open(file_path, 'r') as file:
        for line in file:
            # Each line in the JSON file is a JSON object
            json_data = json.loads(line.strip())
            try:
                # Insert the JSON object into the MongoDB collection
                collection.insert_one(json_data)
            except pymongo.errors.DuplicateKeyError:
                print('Error: Duplicate key found in file ' + file_path + '. Skipping...')
                continue
    print('[INFO] Insert \'' + file_path + '\' into MongoDB collection \'Model\'.')

def insert_recursive(root_path: str, collection_name: str='meteor') -> None:
    # Insert all JSON files in a directory and its subdirectories into MongoDB
    # Args:
    #   root_path: path to directory to search for JSON files
    #   collection_name: name of MongoDB collection to insert JSON objects into (default: "meteor")
    # Returns:
    #   None

    # Iterate through all files in the directory and its subdirectories
    for subdir, dirs, files in os.walk(root_path):
        for file in files:
            file_path = os.path.join(subdir, file)
            if file.endswith('.json'):
                # Insert the JSON file into MongoDB
                insert_models(file_path, collection_name)
   
def insert_link(model_id: str, db: Database) -> None:
    # Insert the link to the model into the Link collection
    # Args:
    #   model_id: ID of the model
    #   db: MongoDB database object
    # Returns:
    #   None

    db['Link'].insert_one({
        '_id': model_id,
        'private': False,
        'model_id': model_id
    })
    print('[INFO] Insert \'' + model_id + '\' into MongoDB collection \'Link\'.')

def parse_args() -> Namespace:
    # Parse command line arguments
    # Args:
    #   None
    # Returns:
    #   args: command line arguments

    parser = ArgumentParser(description='Insert Alloy4Fun model data into MongoDB.\nData can be inserted from a JSON file or a directory containing JSON files.'
                                     + ' If a directory is specified, all JSON files in the directory and its subdirectories will be inserted into MongoDB.'
                                     + ' If no arguments are specified, the current directory will be searched for JSON files and the data will be inserted into MongoDB.'
                                     + ' Each JSON file should contain one Alloy4Fun model per line.')
    parser.add_argument('path', nargs='?', default='dataset', help='path to JSON file(s) or directory to search for JSON files (default: dataset/)')
    parser.add_argument('collection_name', nargs='?', default='meteor', help='name of MongoDB collection to insert JSON objects into (default: "meteor")')
    parser.add_argument('-r', '--recursive', action='store_true', help='recursively search for JSON files in subdirectories')
    return parser.parse_args()

def main():
    args = parse_args()
    path = args.path
    collection_name = args.collection_name

    if args.recursive:
        insert_recursive(path, collection_name)
    else:
        if os.path.isdir(path):
            files = [os.path.join(path, f) for f in os.listdir(path) if f.endswith('.json')]
        else:
            files = [path]

        # Check if any JSON files were found
        if files.__len__() == 0:
            if path == '.':
                print('Error: No JSON files found the in current directory.')
            else:
                print('Error: No JSON files found in directory \'' + path + '\'.')
            exit()

        for file in files:
            insert_data(file, collection_name)

if __name__ == '__main__':
    main()