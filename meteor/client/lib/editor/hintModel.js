import { displayError, markEditorInfo } from './feedback'
import { download } from './downloadTree'


function handleHintGet(err, result) {
    if (err) {
        console.error(err)
    } else if (result && result.alloy_hint && !result.alloy_error) {
        Session.set('hint-data', result)
        Session.set('hint-enabled', true)
        Session.set('hint-available', true)
    } else {
        Session.set('hint-enabled', true)
    }
}

export function displayHint() {
    const result = Session.get('hint-data')
    Session.set('hint-data', null)
    Session.set('hint-available', false)

    if (result.msg) {
        let log = ''
        if (result.line && result.column) {
            log += `(${result.line}:${result.column}) - `
        }
        log += `${result.msg}`

        const log_messages = Session.get('log-message')
        log_messages.push(log)
        Session.set('log-message', log_messages)

        const log_classes = Session.get('log-class')
        log_classes.push('log-info')
        Session.set('log-class', log_classes)
        if (result.line && result.column && result.line2 && result.column2) {
            markEditorInfo(result.line - 1, result.column - 1, result.line2 - 1, result.column2 - 1)
        }
    } else if (result.line && result.column && result.line2 && result.column2) {
        markEditorInfo(result.line - 1, result.column - 1, result.line2 - 1, result.column2 - 1)
    } else {
        const log_messages = Session.get('log-message')
        log_messages.push(result.msg ?? 'Unable to generate hint')
        Session.set('log-message', log_messages)

        const log_classes = Session.get('log-class')
        log_classes.push('log-unavailable')
        Session.set('log-class', log_classes)
    }
}

export function bufferHint() {
    Session.set('hint-data', null)
    Meteor.call('getHint', Session.get('last_id'), handleHintGet)
}
