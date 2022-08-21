/**
 * Module that handles model execution and navigation.
 *
 * @module client/lib/editor/genUrl
 */

import { displayError,
    displayInfoMsg,
    markEditorError,
    markEditorWarning} from './feedback'
import { getCommandIndex,
    storeInstances,
    modelExecuted,
    getNextInstance,
    getPreviousInstance,
    instChanged,
    isUnsatInstance,
    getCommandLabel,
    resetState,
    currentState } from './state'
import { resetPositions,newInstanceSetup } from '../visualizer/projection'

/**
 * Execute the model through the selected command. Will call the Alloy API.
 */
export function executeModel() {
    Session.set('is_running',true)
    const commandIndex = getCommandIndex()

    // no command to run
    if (commandIndex < 0) displayError('There are no commands to execute', '')

    // execute command
    else {
        modelExecuted()
        const model = textEditor.getValue()
        Meteor.call('getInstances', model, commandIndex, Session.get('from_private'), Session.get('last_id'), handleExecuteModel)
    }
}

/**
 * Show the next instance for the executed command or requests additional
 * instances if no more cached, unless already unsat. May call the Alloy API.
 */
export function nextInstance() {
    const instanceIndex = Session.get('currentInstance')
    const maxInstanceNumber = Session.get('maxInstance')
    // no more local instances but still not unsat
    if (instanceIndex == maxInstanceNumber - 1 && !isUnsatInstance(instanceIndex)) {
        const model = textEditor.getValue()
        Meteor.call('nextInstances', model, getCommandIndex(), Session.get('last_id'), handleExecuteModel)
    }
    const ni = getNextInstance()
    if (typeof ni !== 'undefined') {
        if (ni.unsat) {
            Session.set('currentInstance', instanceIndex)
            displayInfoMsg('No more satisfying instances!', '')
        } else {
            resetPositions()
            updateGraph(ni)
            newInstanceSetup()
            instChanged()
            Meteor.call('navInstance', 0, Session.get('currentInstance'), Session.get('last_id'))
        }
    }
}

/**
 * Show the previous instance for the executed command, always cached if any.
 */
export function prevInstance() {
    const ni = getPreviousInstance()
    if (typeof ni !== 'undefined') {
        resetPositions()
        updateGraph(ni)
        newInstanceSetup()
        instChanged()
        Meteor.call('navInstance', 1, Session.get('currentInstance'), Session.get('last_id'))
    }
}

/**
 * Handles the response of the Alloy API for the execution of a command,
 * either a fresh execution or a new batch of instances when iterating.
 *
 * @param {Error} err the possible meteor error
 * @param {Object} result the result to the getInstances or nextInstance
 *     meteor calls
 */
function handleExecuteModel(err, result) {
    Session.set('is_running',false)
    Session.set('model-executed', true)
    if (err) {
        maxInstanceNumber = -1
        return displayError(err)
    }
    Session.set('last_id', result.newModelId) // update the last_id for next derivations

    if (result.instances)
        result = result.instances

    storeInstances(result)
    if (Array.isArray(result)) result = result[0]

    // if there error returned by Alloy
    if (result.alloy_error) {
        let resmsg = result.msg
        if (result.line) {
            resmsg = `${resmsg} (${result.line}:${result.column})-(${result.line2}:${result.column2})`
            markEditorError(result.line - 1, result.column - 1, result.line2 - 1, result.column2 - 1)
        }
        resmsg = `There was a problem running the model!\n${resmsg}\nPlease validate your model.`
        console.error(resmsg)
        Session.set('log-message', resmsg)
        Session.set('log-class', 'log-error')
    }
    // else, show instance or unsat
    else {
        const command = getCommandLabel()

        log_messages = []
        log_classes = []

        if (result.warning_error) {
            let resmsg = result.msg
            if (result.line) resmsg = `${resmsg} (${result.line}:${result.column})-(${result.line2}:${result.column2})`
            log_messages.push(`There is a possible problem with the model!\n${resmsg}\n`)
            log_classes.push('log-warning')
            markEditorWarning(result.line - 1, result.column - 1, result.line2 - 1, result.column2 - 1)
        }
        if (result.unsat) {
            log_messages.push(result.check ? `No counter-examples. ${command} may be valid.` : `No instance found. ${command} may be inconsistent.`)
            log_classes.push(result.check ? 'log-complete' : 'log-wrong')
        } else {
            log_messages.push(result.check ? `Counter-example found. ${command} is invalid.` : `Instance found. ${command} is consistent.`)
            log_classes.push(result.check ? 'log-wrong' : 'log-complete')
            initGraphViewer('instance')
            resetPositions()
            resetState()
            updateGraph(result.instance[currentState()])
            newInstanceSetup()
        }

        Session.set('log-message', log_messages)
        Session.set('log-class', log_classes)
    }
}
