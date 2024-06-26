#!/usr/bin/env python
# coding: utf-8

import pandas as pd, os 
import collections
import jpype # Java
import re # Regular expression

IMPORT_DIR = "dataset"
OUTPUT_DIR = "../neo4j/import/prepared_data/"

def addJAR(jar_path):
    if os.path.exists(jar_path):
        jpype.addClassPath(jar_path)
    else:
        raise FileNotFoundError(f"The JAR file '{jar_path}' does not exist.")

def initializeJpype():
    if not jpype.isJVMStarted():
        jpype.startJVM(jpype.getDefaultJVMPath(), "-ea")


# Add the JAR files to the classpath
addJAR("./higena-1.0.0-shaded.jar")

# Initialize JPype
initializeJpype()

# Import the Java classes
CompUtil = jpype.JClass('edu.mit.csail.sdg.parser.CompUtil')
Reporter = jpype.JClass('edu.mit.csail.sdg.alloy4.A4Reporter')
ExprParser = jpype.JClass('org.higena.parser.ExprExtractor')
ASTParser = jpype.JClass('org.higena.parser.A4FParser')
SyntaxError = jpype.JClass('edu.mit.csail.sdg.alloy4.ErrorSyntax')
TypeError = jpype.JClass('edu.mit.csail.sdg.alloy4.ErrorType')

def importDataFromDir(dir):
    # Import all the data from a directory
    # dir: directory with the data
    # return: a dictionary of dataframes
    dict = {}

    for file in os.listdir(dir):
        if file.endswith('.json'):
            df = pd.read_json(f'{dir}/' + file, lines=True)
            dict[file.removesuffix(".json")] = df
            print(f"Imported {file}.")

    return dict

def removeFile(file):
    # Remove the log file
    try:
        os.remove(file)
    except OSError:
        pass

# Remove the log file if exists
os.makedirs("logs", exist_ok=True)
log_file_ast = "logs/astError.txt"
log_file_drops = "logs/drops.txt"
removeFile(log_file_ast)
removeFile(log_file_drops)
print("Log files removed.")

# Import files from the data directory
dict = importDataFromDir(IMPORT_DIR)
print("Data imported.")


# ## Data Cleaning
# ### Separate challenge code from the rest of submissions

def separateChallengeRows(dict):
    # Separate the original row of each dataframe
    # dict: dictionary of dataframes
    # return: a dictionary of dataframes
    challengeRows = {}

    for key, df in dict.items():
        # Store the index of the original row of each dataframe
        index = df[df["_id"] == key].index.values.astype(int)[0]
        row = df.iloc[index]
        # store the original row in a new dataframe
        challengeRows[key] = pd.DataFrame([row.values], columns=row.index)
        # Remove the original row from the dataframe
        df.drop(index=index, inplace=True)

    return challengeRows

challengeRows = separateChallengeRows(dict) 
print("Challenge rows separated.")

# ### Check null values
# Whenever cmd_i is null it means that there was no execution. It is the sharing of a model. This cases might be irrelevant to the problem so they can be dropped.

def logDrop(id, reason):
    # Log an error
    # id: id of the submission
    # challenge: id of the challenge
    # error: error message
    # expr: expression that failed
    file = open(log_file_drops, "a")
    file.write("DROPPED " + id + " FOR " + reason + "\n")
    file.close()

def shortCircuit(df, index):
    # Short circuit the derivation of a row.
    # If A derives B and B derives C, then A derives C and B is removed.
    # df: dataframe
    # index: index of the row to be removed
    # return: dataframe with the row short circuited

    # Get row information
    row = df.loc[[index]]
    id = row["_id"].values[0]
    derivation = row["derivationOf"].values[0]

    # Remove row
    df.drop(index, inplace=True)

    # Short circuit derivations
    df.loc[df["derivationOf"] == id, "derivationOf"] = derivation

    return df

def dropNulls(dict, col):
    # Remove rows with col null
    # dict: dictionary of dataframes
    # return: dataframe with rows with col null removed
    total = 0
    for challengeID, df in dict.items():
        # Filter rows with col= null
        nullDF = df[df[col].isnull()]
        # Short circuit derivations
        for index, row in nullDF.iterrows():
            df = shortCircuit(df, index)
            logDrop(row["_id"], "having null " + col)
            total += 1            
    return dict

# Drop rows with cmd_i null
dict = dropNulls(dict, "cmd_i")
print("Null values dropped for cmd_i.")


# #### cmd_c and cmd_n null values
# Whenever cmd_c is null, cmd_n is also null. These values are null for cases where a syntactic error is thrown and a msg appears.
# The code in these case is not parseable. We can drop these rows.

# drop rows with cmd_c null
dict = dropNulls(dict, "cmd_c")
print("Null values dropped for cmd_c.")

# Dealing with mgs and theme null values is not important in these context.

# ### Remove extra features

# #### cmd_c feature
# The number of 0 values for the column cmd_c is irrelevant (when the executed command is not a check). For that reason, entries with these values should be dropped.

def dropNonChecks(dict):
    # Drop the rows that are not checks
    # dict: dictionary of dataframes
    # return: dataframe with the rows that are not checks removed
    total = 0
    for df in dict.values():
        # Filter rows with cmd_c != 0
        dfToDrop = df[df["cmd_c"] == 0]
        # Short circuit derivations
        for index, row in dfToDrop.iterrows():
            df = shortCircuit(df, index)
            logDrop(row["_id"], "being a non check (cmd_c = 0)")
            total += 1
    return dict

# Remove the rows with the value 0 in the cmd_c column
dropNonChecks(dict)
print("Non checks dropped.")

# #### original feature
def operateDFs(dict, op, arg):
    # Operate on each dataframe in a dictionary
    # dict: dictionary of dataframes
    # op: operation to perform
    # arg: argument to pass to the operation
    for df in dict.values():
        op(df, arg)

# Now we will drop the irrelevant columns. 
# Drop columns that are not needed
dropColOp = (lambda df, arg: df.drop(columns=arg, axis=1, inplace=True))
colsToDrop = ["cmd_c", "cmd_i", "original", "msg", "theme"]

operateDFs(dict, dropColOp, colsToDrop)
operateDFs(challengeRows, dropColOp, colsToDrop) 
print("Irrelevant columns dropped.")

# ### Code cleaning
# The code in this dataset comes with comments that can be removed.

def cleanCode(code):
    # Remove comments and empty lines
    # code: string with the code
    # return: string with the code without comments and empty lines
    code = re.sub(r"(/\*(.|\n)*?\*/)|(//.*)|(--.*)", "", code) # remove comments
    code = re.sub(r"\n\n(?=\n)", "", code) # remove empty lines
    code = code.replace("\n", " ").replace("\t", " ")
    
    return " ".join(code.split()).strip()

def applyToCol(df, col, op):
    # Apply an operation to a column
    # df: dataframe
    # col: column to apply the operation
    # op: operation to apply
    # return: dataframe with the operation applied
    df[col] = df[col].apply(op)
    return df

# Clean the code column
cleanCodeOp = (lambda df, arg: applyToCol(df, arg, cleanCode))
operateDFs(dict, cleanCodeOp, "code")
operateDFs(challengeRows, cleanCodeOp, "code")
print("Code cleaned.")

# ### cmd_n rename
# Rename cmd_n so that it equals the predicate completed by the student.

def removeExtraSuffix(pred):
    return re.sub("OK|Ok|ok", "", pred)

remSuffixOp = (lambda df, arg: applyToCol(df, arg, removeExtraSuffix))
operateDFs(dict, remSuffixOp, "cmd_n")
print("cmd_n renamed.")

# # Feature Engineering
# Generate Expr column that contains the expression written by the student to the respective predicate.
def getExpr(code: str, cmd: str):
    # Get the expression of a predicate
    # code: string with the code
    # cmd: predicate name
    # return: string with the expression of the predicate
    parser = ExprParser(code)
    return str(parser.parse(cmd)).strip()

def genExprColum(dict):
    # Iterate datasets 
    for df in dict.values():
        df["expr"] = df.apply(lambda x: getExpr(x["code"], x["cmd_n"]), axis=1)

genExprColum(dict)
print("Expr column generated.")

# ## Add challenge code to dataset
# Add submissions to the dataset that represent the initial state of the challenge (the empty submission) and the teacher's solution.

def genEmptySubmission(challengeDf): 
    # Generate an empty submission
    # challengeDf: dataframe with one row with the challenge
    # return: dataframe with one row with the empty submission for the challenge

    challengeRow = challengeDf # Copy the dictionary
    challengeRow["sat"] = [1.0] # Empty submission is incorrect
    challengeRow["expr"] = ["EMPTY"] # Empty submission has an empty expression
    challengeRow["derivationOf"] = [""] # Does not derive from any other challenge

    return challengeRow

def getSolutions(code):
    # Get the solutions of a challenge
    # code: string with the code
    # return: dictionary with the solutions where the key is the predicate name and the value is the solution expression

    result = {} # Initialize dictionary
    module = CompUtil.parseEverything_fromString(Reporter(), code)

    # Set keys of the dictionary: predicates to be completed by students (empty predicates)
    for fun in module.getAllFunc():
        if "$$Default" not in fun.label and fun.getBody().toString().equals("true"): # Empty predicate has the body "true"
            result[str(fun.label).removeprefix("this/")] = "" 

    # Get the solutions
    for fun in module.getAllFunc():
        label = str(fun.label).removeprefix("this/")
        if label in result: # Skip if predicate in dictionary
            continue

        # Check for variations of 'oracle' in label
        possible_keys = [label.removesuffix(suffix) for suffix in ["o", "O", "oracle"] if label.endswith(suffix)]
        for key in possible_keys:
            if key in result:
                result[key] = getExpr(code, label)
                break

    return result

def addChallengeRows(dict, challengeRows):
    # Add the challenge rows 
    # dict: dictionary of dataframes
    # challengeRows: dictionary of dataframes with the challenge rows
    # return: dictionary of dataframes with the challenge rows added

    # Iterate datasets
    for challenge in dict.keys():
        # Get the initial empty submission for the challenge and set its fields 
        challenge_row = genEmptySubmission(challengeRows[challenge])
        # Get the solutions of the challenge
        challenge_solutions = getSolutions(challenge_row["code"].values[0])

        # Gen the teacher solution
        solution_row = challenge_row.copy()
        solution_row["sat"] = [0.0] # Correct
        solution_row["derivationOf"] = [challenge]

        for pred, solution in challenge_solutions.items():
            # Set up and append the challenge empty row to the dataframe
            challenge_row["cmd_n"] = [pred]
            dict[challenge] = pd.concat([dict[challenge], challenge_row], ignore_index=True)

            # Set up and append the teacher solution row to the dataframe
            solution_row["expr"] = [solution] 
            solution_row["cmd_n"] = [pred]
            solution_row["_id"] = ["sol_" + pred]
            dict[challenge] = pd.concat([dict[challenge], solution_row], ignore_index=True)
    
    return dict

dict = addChallengeRows(dict, challengeRows)
print("Challenge rows added.")

# ## Remove Empty Submissions
# Remove submissions with empty expression except for the inital submission.
def dropEmptySubmissions(dict):
    # Drop rows that are empty submissions
    # dict: dictionary of dataframes
    # return: dataframe without empty submissions
    for df in dict.values():
        # Filter rows with expr = ""
        rowsToDrop = df[df["expr"] == ""]
        # Short circuit derivations
        for index, row in rowsToDrop.iterrows():
            df = shortCircuit(df, index)
            logDrop(row["_id"], "being an empty submission.")

def fixEmpty(expr):
    # Turn EMPTY expr to ""
    # expr: string with the expression
    # return: string with the expression fixed
    if expr == "EMPTY":
        return ""
    else:
        return expr

dropEmptySubmissions(dict)
print("Empty submissions removed.")

# Turn EMPTY expr to "" 
fixEmptyOp = (lambda df, arg: applyToCol(df, arg, fixEmpty))
operateDFs(dict, fixEmptyOp, "expr")
print("EMPTY expr fixed.")

# ## Separate DFs by predicate
# Parse the challenge files first.
def parseChallenges(path):
    # Parse the challenges
    # path: path to the challenges
    # return: dictionary of parsed challenges with the name as key 
    challenges = {}
    for challenge in os.listdir(path):
        if challenge.endswith(".als"):
            challenges[challenge[:-4]] = CompUtil.parseEverything_fromFile(Reporter(), None, path + challenge)
    return challenges

parsed_challenges = parseChallenges("challenges/")
print("Challenges parsed.")

# Get predicates to be completed by students for each challenge.
challenge_functions = {}
for challenge, module in parsed_challenges.items():
    challenge_functions[challenge] = [str(fun.label).removeprefix("this/") for fun in module.getAllFunc() if str(fun.label) != "this/$$Default"]


# Separate by predicate.
def separateDerivations(df, pred, challenge):
    # Separate derivations that belong to different predicates using short circuiting
    # df: dataframe
    # pred: predicate
    # return: dataframe with independent derivations

    predDf = df[df["cmd_n"] == pred].copy() # Copy df filtered by predicate
    
    # Iterate over predDf 
    for index, row in predDf.iterrows():
        derId = row["derivationOf"] # Get the derivation of the row
        derDf = df[df["_id"] == derId] # Get the derivation row
        
        # Get nearest derivation with the same predicate
        # Loop ends upon finding a row/submission with the cmd_n == pred
        # or when the derivation is not present in the dataframe
        # or when the derivation is the challenge row
        while not derDf.empty and derDf.iloc[0]["cmd_n"] != pred and derDf.iloc[0]["_id"] != challenge: 
            # get previous derivation
            derDf = df[df["_id"] == derDf.iloc[0]["derivationOf"]] 

        
        # Update the derivationOf column
        if not derDf.empty:
            predDf.at[index, "derivationOf"] = derDf.iloc[0]["_id"]

    return predDf


def separateDFbyPred(dict, challenge_functions):
    # Separate the dataframes by predicate
    # dict: dictionary of dataframes for each challenge
    # return: dictionary of dataframes for each challenge and each predicate: dict[challenge][predicate]
     
    # Dictionary of dataframes for each exercise
    allDfs = {}
    # Iterate over the dataframes
    for challenge, df in dict.items():
        allDfs[challenge] = {} # init the challenge dictionary
        # Iterate over the exercises
        for pred in challenge_functions[challenge]:
            # Separate derivations that belong to different predicates using short circuiting 
            allDfs[challenge][pred] = separateDerivations(df, pred, challenge).copy()
            
    return allDfs

allDfs = separateDFbyPred(dict, challenge_functions)
print("Dataframes separated by predicate.")

# ## Add AST column
def logError(id, challenge, error, expr):
    # Log an error
    # id: id of the submission
    # challenge: id of the challenge
    # error: error message
    # expr: expression that failed
    file = open(log_file_ast, "a")
    file.write("\n- Submission: " + id + " Challenge: " + challenge + " Expr: " + expr + "\n\t" + str(error) + "\n")
    file.close()

def parseExpr(id, expr, fullCode, challenge, parsedChallenge):
    # Parse an expression
    # expr: string with the expression
    # fullCode: string with the full code
    # challenge: id of the challenge
    # parsedChallenge: parsed challenge module
    # return: parsed expression

    # Handle empty submissions (challenge code)
    if expr == "":
        return ""

    parsed = None
    try:
        parsed = str(ASTParser.parse(expr, parsedChallenge).toTreeString())
    except Exception as e:
        # Try to parse the full code
        try:
            parsed = str(ASTParser.parse(expr, fullCode).toTreeString())
        except Exception as e:
            logError(id, challenge, e, expr)

    return parsed

def addASTsColumns(allDfs, parsed_challenges):
    # Add the ASTs columns
    # allDfs: dictionary of dataframes for each challenge and each predicate
    # parsed_challenges: dictionary of parsed challenges
    # return: dictionary of dataframes for each challenge and each predicate with the ASTs columns

    # Iterate over the challenges
    for challenge, cmdDfs in allDfs.items():
        # Get parsed challenge

        # Iterate over the submission for each command
        for cmd, df in cmdDfs.items():
            df["ast"] = df[["_id", "expr", "code", "cmd_n"]].apply(lambda x: parseExpr(x["_id"], x["expr"], x["code"], challenge, parsed_challenges[challenge]), axis=1)


# Add the ASTs columns
ASTParser.ANONYMIZE = True
ASTParser.SORT_COMMUTATIVE = True
addASTsColumns(allDfs, parsed_challenges)
print("ASTs added.")

# Clean submissions without ASTs. (With syntax errors)

def removeEmptyASTs(allDfs):
    # Remove the empty ASTs
    # allDfs: dictionary of dataframes for each challenge and each predicate
    # return: dictionary of dataframes for each challenge and each predicate

    # Iterate over the challenges
    for cmdDfs in allDfs.values():
       cmdDfs = dropNulls(cmdDfs, "ast") 
           
    return allDfs

allDfs = removeEmptyASTs(allDfs)
print("Empty ASTs removed.")

# # Export data
def exportData(allDfs, path, fileType="csv"):
    # Export the dataframes to csv files
    # allDfs: dictionary of dataframes for each challenge and each predicate 
    # path: path to export the dataframes

    # Iterate over the challenges
    for challenge, cmdDfs in allDfs.items():
        # Create folder if it does not exist
        folder = path + challenge + "/"
        if os.path.exists(folder):
            # Delete all files
            fileList = [f for f in os.listdir(folder)]
            for f in fileList:
                os.remove(os.path.join(folder, f))
        else:
            # Create folder
            os.makedirs(folder)

        # Iterate over the submission for each commantime/timed
        for cmd, df in cmdDfs.items():
            # Skip empty dataframes
            if df.empty:
                continue
            # Export dataframe to csv
            if fileType == "csv":
                file = folder + cmd + ".csv"
                df.to_csv(file, index=False)
            elif fileType == "json":
                file = folder + cmd + ".json"
                df.to_json(file, orient='records', lines=True)
            else: 
                print("File type not supported")
                return

# Export the dataframes to db
exportData(allDfs, OUTPUT_DIR)
print("Data exported.")

if jpype.isJVMStarted():
    jpype.shutdownJVM()