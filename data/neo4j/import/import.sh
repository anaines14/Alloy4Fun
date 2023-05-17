#!/bin/sh

for challenge in prepared_data/*/; do
    for predicate in "$challenge"/*; do
        challenge=$(basename "$challenge")
        predicate=$(basename "${predicate%.csv}")
        java -jar higena-1.0.0.jar $challenge $predicate
        break
    done
    break
done 
