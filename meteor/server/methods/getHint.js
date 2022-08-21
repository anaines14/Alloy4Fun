import { Hint } from "../../lib/collections/hint"
import { containsValidSecret, extractRepairTargets, extractSecrets, getCommandsFromCode } from "../../lib/editor/text"

Meteor.methods({
    /**
      * Meteor method to ask for hints for the current model.
      * This will call the Alloy API (webService).
      * The response from the API will also be registered in the database.
      *
      * @param {String} code the Alloy model to execute
      * @param {Number} commandIndex the index of the command to execute
      * @param {String} currentModelId the id of the current model (from which
      *     the new will derive)
      *
      * @returns An error message or the result from the request to the API
      */
    getHint(code, commandIndex, fromPrivate, currentModelId) {

        // if no secrets, try to extract from original
        let code_with_secrets = code
        if (currentModelId && !containsValidSecret(code) && !fromPrivate) {
            const o = Model.findOne(currentModelId).original
            code_with_secrets = code + extractSecrets(Model.findOne(o).code).secret
        }

        let repairTargets = extractRepairTargets(code_with_secrets)

        const cmd = getCommandsFromCode(code_with_secrets)[commandIndex]
        const cmdName = cmd.split(/\s+/)[1]

        return new Promise((resolve, reject) => {

            if(!cmd.match(/check\s+/)) {
                resolve({ error: `The hint feature is only available for 'check' commands.` })
            } else if(repairTargets[cmdName] === undefined) {
                resolve({ error: `The command '${cmdName}' does not have defined repair targets, hints are not available for it.` })
            }

            HTTP.call('POST', `${Meteor.settings.env.API_URL}/getHint`, {
                data: {
                    model: code_with_secrets,
                    commandIndex: commandIndex,
                    repairTargets: repairTargets,
                }
            }, (error, result) => {
                if (error) reject(error)

                const content = JSON.parse(result.content)

                const new_hint = {
                    time: new Date().toLocaleString(),
                    model_id: currentModelId,
                    elapsed: content.elapsed,
                    repaired: content.repaired,
                }
                if (content.repair) new_hint.repair = content.repair
                if (content.hints) new_hint.hints = content.mutators.filter(m => m.hint !== undefined).map(m => m.hint)

                Hint.insert(new_hint)

                resolve(content)
            })
        })
    },
})