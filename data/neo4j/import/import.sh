#!/bin/sh

for challenge in datasets/*/; do
    for predicate in "$challenge"/*; do
        challenge=$(basename "$challenge")
        predicate=$(basename "${predicate%.csv}")
        java -cp higena-1.0.0.jar org.higena.graph.Graph $challenge $predicate
    done
done 
