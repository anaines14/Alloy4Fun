import { containsValidSecret, extractRepairTargets, extractSecrets } from "../../lib/editor/text"

Meteor.methods({

    getHint(code, commandIndex, fromPrivate, currentModelId) {

        // if no secrets, try to extract from original
        let code_with_secrets = code
        if (currentModelId && !containsValidSecret(code) && !fromPrivate) {
            const o = Model.findOne(currentModelId).original
            code_with_secrets = code + extractSecrets(Model.findOne(o).code).secret
        }

        let repairTargets = extractRepairTargets(code_with_secrets)

        return new Promise((resolve, reject) => {
            HTTP.call('POST', `${Meteor.settings.env.API_URL}/getHint`, {
                data: {
                    model: code_with_secrets,
                    commandIndex: commandIndex,
                    repairTargets: repairTargets,
                }
            }, (error, result) => {
                if (error) reject(error)

                const content = JSON.parse(result.content)

                resolve(content)
            })
        })
    },
})