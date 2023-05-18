import { HiGenAHint } from "../../lib/collections/higenaHint"

Meteor.methods({
    genHiGenAHint(code, predicate, currentModelId) {
        const originalId = Model.findOne(currentModelId).original

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
                    expression: content.expression,
                    code: content.code,
                    mapping: content.mapping,
                    type: content.type,
                    isNewNode: content.isNewNode,
                    createdShorterPath: content.createdShorterPath,
                    sourceExpr: content.sourceExpr,
                    sourceAST: content.sourceAST,
                    targetExpr: content.targetExpr,
                    targetAST: content.targetAST,
                    nextExpr: content.nextExpr,
                    nextAST: content.nextAST,
                    totalTED: content.totalTED,
                    srcDstTED: content.srcDstTED,
                    operations: JSON.parse(content.operations.replace(/\\/g, "")),
                    hint: content.hint,
                    time: content.time,
                    model_id: currentModelId
                }
                HiGenAHint.insert(new_hint) 

                resolve(content)
            })
        })
    },
})