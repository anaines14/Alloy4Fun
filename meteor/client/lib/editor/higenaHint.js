import { displayError } from "./feedback"
import { getCommandLabel, modelExecuted } from "./state"

export function getHiGenAHint() {
    Session.set('is_running', true)
    Session.set('hint-generated', true)
    const currentModelId = Session.get('last_id')
    const commandLabel = getCommandLabel()

    // no command to run
    if (!commandLabel)
        displayError('There are no commands to execute', '')

    // execute command
    else {
        const predicate = commandLabel.replace(/ok$/i, '').replace(/check /, '')
        modelExecuted()
        const model = textEditor.getValue()
        Meteor.call('genHiGenAHint', model, predicate, currentModelId, handleHiGenAHint)
    }
}

function handleHiGenAHint(err, result) {
    Session.set('is_running', false)
    if (err) {
        return displayError(err)
    }

    if (result.error) {
        Session.set('log-message', result.error)
        Session.set('log-class', 'log-error')
    }
    else {
        Session.set('log-message', result.hint)
        Session.set('log-class', 'log-hint')
    }

}