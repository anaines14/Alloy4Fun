import re

def get_graph_id_dict_pipeline():
    """Targets "Graph" collection"""
    return [
        {'$addFields': {'reg': {'$regexFind': {'input': '$name', 'regex': re.compile(r"([^-]*)-(.*)")}}}}, # Apply a regex find using the provided regex
        {'$addFields': {'super_name': {'$first': '$reg.captures'}}}, # Place the first regex group on field super_name
        {'$group': {'_id': '$super_name', 'graph_ids': {'$push': '$_id'}}} # Group based on super_name, accumulate the graph ids in the arrays graph_ids
    ]


def get_graph_node_statistics():
    """Targets "Node" collection"""
    return [
        {'$group': { # Group
            '_id': '$graph_id', #By graph_id
            'valid_nodes': {'$sum': {'$cond': ['$valid', 1, 0]}}, # Count valids
            'invalid_nodes': {'$sum': {'$cond': ['$valid', 0, 1]}},  # Count invalids
            'valid_submissions': {'$sum': {'$cond': ['$valid', '$visits', 0]}}, # Sum valid frequencies
            'invalid_submissions': {'$sum': {'$cond': ['$valid', 0, '$visits']}} # Sum invalid frequencies
        }},
        {'$lookup': {'from': 'Graph', 'localField': '_id', 'foreignField': '_id', 'as': 'graph'}}, # Lookup the graph's specification and place it in an array
        {'$unwind': '$graph'}, # Unwind the graph array
        {'$project': {  # Rewrite the object as the defined fields
            '_id': 0, # means remove id field
            'name': '$graph.name', 
            'valid_formulas': '$valid_nodes', 
            'invalid_formulas': '$invalid_nodes', 
            'valid_submissions': '$valid_submissions', 
            'invalid_submissions': '$invalid_submissions'
        }},
        {'$sort': {'name': 1}} # Sort from biggest to smallest based on the name
    ]


def get_popular_nodes_pipeline(graph_ids):
    """Targets "Node" collection"""
    return [
        {'$match': {'valid': False, 'graph_id': {"$in":graph_ids}}}, # Match invalid nodes with one of the provided graph ids
        {'$addFields': {'formula': {'$objectToArray': '$formula'}}}, # Covert the formula object into an array of key value objects.
        {'$unwind': '$formula'}, # Unwind object the formula (since every array is a singleton this will just update each document)
        {'$match': {'formula.v': {'$ne': ''}}}, # Match non blank formulas (aka remove initial nodes)
        {'$lookup': {'from': 'Node', 'localField': 'minSolution', 'foreignField': '_id', 'as': 'minSolutionFormula', 
          # A "join" with the document Node, values are placed in the array minSolutionFormula
            'pipeline': [ # Pipeline applied to the "joined" document
                {'$addFields': {'formula': {'$objectToArray': '$formula'}}}, # Covert the formula object into an array of key value objects.
                {'$unwind': '$formula'}, # Unwind object the formula (since every array is a singleton this will just update each document)
                {'$replaceRoot': {'newRoot': '$formula'}} # replace the object with its formula key value object (k,v)
            ]
        }},
        {'$unwind': '$minSolutionFormula'}, # Unwind the minSolutionFormula array (since only one object can be present there wont be any new documents)
        {'$project': { # Rewrite the object as the defined fields
            '_id':0, # means remove id field
            'predicate': '$formula.k', 
            'formula': '$formula.v',
            'frequency': '$visits',
            'closest_solution': '$minSolutionFormula.v', 
            'closest_solution_edit_distance': '$minSolutionTed'
        }},
        {'$sort': {'frequency': -1}} # Sort from biggest to smallest based on the defined frequency
    ]



def get_min_solutions_pipeline(graph_ids):
    """Targets "Node" collection"""
    return [
        {'$match': { 'graph_id': {"$in":graph_ids}}}, # Match nodes with one of the provided graph ids
        {'$group': {'_id': '$minSolution','count': {'$sum': 1}}}, # Count the minimum solution frequency
        {'$lookup': {'from': 'Node', 'localField': '_id', 'foreignField': '_id', 'as': 'node', 
            # A "join" with the document Node, values are placed in the array minSolutionFormula
            'pipeline': [ # Pipeline applied to the "joined" document
                {'$addFields': {'formula': {'$objectToArray': '$formula'}}}, # Covert the formula object into an array of key value objects.
                {'$unwind': '$formula'}, # Unwind object the formula (since every array is a singleton this will just update each document)
            ]
        }},
        {'$unwind': '$node'},
        {'$project': { # Rewrite the object as the defined fields
            '_id': 0,  # means remove id field
            'predicate': '$node.formula.k', 
            'formula': '$node.formula.v', 
            'frequency': '$node.visits',
            'frequency_as_the_closest_solution':'$count'
        }},
        {'$sort': {'frequency': -1}} # Sort from biggest to smallest based on the defined frequency
    ]
