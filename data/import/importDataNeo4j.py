import os
from dotenv import load_dotenv

PREPARED_DATA_DIR = "../neo4j/import/prepared_data"
JAR_PATH = "./higena-1.0.0-shaded.jar"

def run_higena_for_challenges():
    # Iterate over all challenges
    for challenge_dir in os.listdir(PREPARED_DATA_DIR):
        challenge_path = os.path.join(PREPARED_DATA_DIR, challenge_dir)
        if not os.path.isdir(challenge_path):
            continue
        
        # Iterate over all predicates
        for predicate_file in os.listdir(challenge_path):
            predicate_path = os.path.join(challenge_path, predicate_file)
            if not os.path.isfile(predicate_path):
                continue

            # Get challenge and predicate name
            challenge = os.path.basename(challenge_path)
            predicate = os.path.splitext(predicate_file)[0]
            
            command = f"java -jar {JAR_PATH} {challenge} {predicate}"
            os.system(command)

if __name__ == "__main__":
    load_dotenv("../../.env")
    run_higena_for_challenges()