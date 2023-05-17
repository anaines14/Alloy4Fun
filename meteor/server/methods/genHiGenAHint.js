import { HiGenAHint } from "../../lib/collections/higenaHint"

Meteor.methods({
    genHiGenAHint(code, predicate, currentModelId) {
        const originalId = Model.findOne(currentModelId).original

        console.log("CODE: ", code)
        console.log("PREDICATE: ", predicate)
        console.log("ORIGINAL ID: ", originalId)

        return new Promise((resolve, reject) => {
            HTTP.call('POST', `${Meteor.settings.env.API_URL}/higena`, {
                data: {
                    challenge: originalId,
                    predicate: predicate,
                    model: code
                }
            }, (error, result) => {
                if (error) reject(error)
                const content = JSON.parse(result.content)

                const new_hint = {
                    message: content.hint,
                    model_id: currentModelId
                }
                HiGenAHint.insert(new_hint) 

                resolve(content)
            })
        })
    },
})