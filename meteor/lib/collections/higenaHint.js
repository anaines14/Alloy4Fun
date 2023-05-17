HiGenAHint = new Meteor.Collection('HigenaHint');

HiGenAHint.attachSchema(new SimpleSchema({
    _id: {
        type: String
    },
    message: {
        type: String
    },
    model_id: {
        type: String
    }
}))

export { HiGenAHint }