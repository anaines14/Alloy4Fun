import { displayError, markEditorHint } from "./feedback"
import { getCommandIndex, modelExecuted } from "./state"


export function hintModel() {
    Session.set('is_running', true)
    const commandIndex = getCommandIndex()

    // no command to run
    if (commandIndex < 0) displayError('There are no commands to execute', '')

    // execute command
    else {
        modelExecuted()
        const model = textEditor.getValue()
        Meteor.call('getHint', model, commandIndex, Session.get('from_private'), Session.get('last_id'), handleHintModel)
    }
}

function handleHintModel(err, result) {
    console.debug(result)
    Session.set('is_running', false)
    if (err) {
        maxInstanceNumber = -1
        return displayError(err)
    }

    if (result.repaired) {
        let log = ""
        result.mutators.forEach(mutator => {
            markEditorHint(mutator.line - 1, mutator.column - 1, mutator.line2 - 1, mutator.column2 - 1)
            console.debug(mutator.line - 1, mutator.column - 1, mutator.line2 - 1, mutator.column2 - 1)
            log += "(" + mutator.line + ":" + mutator.column + ") - " + (mutator.hint ?? "No hint available.")
            log += "\n"
        });
            console.debug(log)
        Session.set('log-message', log)
        Session.set('log-class', 'log-hint')
    } else {
        Session.set('log-message', "Unable to generate hint, this might mean there is no correct solution close enough to the submission.")
        Session.set('log-class', 'log-error')
    }
}