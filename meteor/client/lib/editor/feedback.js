/**
 * Module with feedback functions.
 *
 * @module client/lib/editor/feedback
 */

/**
 * Display meteor error to the user and report in the console.
 *
 * @param {Error} err the meteor error
 * @returns {Error} the meteor error
 */
export function displayError(err) {
    console.error(err)
    swal({
        type: 'error',
        title: err.error,
        text: err.reason
    })
    return err
}

/**
 * Display error message to the user and report in the console.
 *
 * @param {String} title the title of the message
 * @param {String} text the actual message
 */
export function displayErrorMsg(title, text) {
    console.error(`${title}\n${text}`)
    swal({
        type: 'error',
        title,
        text
    })
}

/**
 * Display warning message to the user and report in the console.
 *
 * @param {String} title the title of the message
 * @param {String} text the actual message
 */
export function displayWarningMsg(title, text) {
    console.error(`${title}\n${text}`)
    swal({
        type: 'warning',
        title,
        text
    })
}

/**
 * Display information message to the user.
 *
 * @param {String} title the title of the message
 * @param {String} text the actual message
 */
export function displayInfoMsg(title, text) {
    swal({
        type: 'info',
        title,
        text
    })
}

/**
 * Mark the text in the editor
 *
 * @param {Object} from the starting position to mark
 * @param {Object} to the ending position to mark
 * @param {String} className the className that will be applied to the mark
 */
export function markEditor(from, to, className) {
    const options = {
        className: className,
        clearWhenEmpty: true,
        inclusiveLeft: true,
        inclusiveRight: true
    }
    textEditor.markText(from, to, options)
}

/**
 * Mark the text in the editor with an error
 *
 * @param {Number} line the line of the first character to mark
 * @param {Number} col the collumn of the first character to mark
 * @param {Number} line2 the line of the last character to mark
 * @param {Number} col2 the collumn of the last character to mark
 */
export function markEditorError(line, col, line2, col2) {
    markEditor({ line: line, ch: col }, { line: line2, ch: col2 + 1 }, "editor-error-mark")
}

/**
 * Mark the text in the editor with a warning
 *
 * @param {Number} line the line of the first character to mark
 * @param {Number} col the collumn of the first character to mark
 * @param {Number} line2 the line of the last character to mark
 * @param {Number} col2 the collumn of the last character to mark
 */
export function markEditorWarning(line, col, line2, col2) {
    markEditor({ line: line, ch: col }, { line: line2, ch: col2 + 1 }, "editor-warning-mark")
}

/**
 * Mark the text in the editor with a hint
 *
 * @param {Number} line the line of the first character to mark
 * @param {Number} col the collumn of the first character to mark
 * @param {Number} line2 the line of the last character to mark
 * @param {Number} col2 the collumn of the last character to mark
 */
export function markEditorHint(line, col, line2, col2) {
    markEditor({ line: line, ch: col }, { line: line2, ch: col2 + 1 }, "editor-hint-mark")
}