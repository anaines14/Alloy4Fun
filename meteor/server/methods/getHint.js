import { Model } from '../../lib/collections/model'

Meteor.methods({
    /**
     *
     * @param currentModelId
     * @returns {Promise<unknown>}
     */
    getHint(currentModelId) {
        return new Promise((resolve, reject) => {
            HTTP.call('POST', `${Meteor.settings.env.API_URL}/hint/spec-higena`, {
                data: {
                    currentModelId
                }
            }, (error, result) => {
                if (error) reject(error)

                resolve(JSON.parse(result.content))
            })
        })
    }
})
