import os
import requests

# Zenodo record ID for the dataset 
ZENODO_RECORD = "8123547"

def download_files(record_id, output_folder):
    """Download all files from a Zenodo record into the output folder.

    Args:
        record_id (str): The ID of the Zenodo record to download files from.
        output_folder (str): The path to the output folder where files will be saved.

    Returns:
        None
    """
    # Fetch the Zenodo record details
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
        file_path = os.path.join(output_folder, file_name)

        # Download the file
        response = requests.get(file_url)
        if response.status_code == 200:
            with open(file_path, 'wb') as file:
                file.write(response.content)
            print(f"Downloaded {file_name}")
        else:
            print(f"Failed to download {file_name}")

def main():
    output_folder = "dataset"

    # Create the output folder if it doesn't exist
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    download_files(ZENODO_RECORD, output_folder)

if __name__ == '__main__':
    main()