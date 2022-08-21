
Hint = new Meteor.Collection('Hint')

Hint.attachSchema(new SimpleSchema({
    _id: {
        type: String
    },
    /** Time elapsed during the repair **/
    elapsed: {
        type: Number
    },
    /** true iff the API was able to produce a repair **/
    repaired: {
        type: Boolean
    },
    /** Hints given by the api server **/
    hints: {
        type: [String],
        optional: true
    },
    /** The repair generated, in Alloy code format **/
    repair: {
        type: String,
        optional: true
    },
    /** id of the model that generated the instance. */
    model_id: {
        type: String
    },
    /** the timestamp. */
    time: {
        type: String
    }
}))

export { Hint }
