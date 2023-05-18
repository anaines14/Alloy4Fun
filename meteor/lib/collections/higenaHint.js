HiGenAHint = new Meteor.Collection('HigenaHint');

HiGenAHint.attachSchema(new SimpleSchema({
    _id: {
        type: String
    },
    /** expression that the student is currently working on. */
    expression: {
        type: String
    },
    /** the student's full code. */
    code: {
        type: String
    },
    /** mapping used to generate the hint. */
    mapping: {
        type: String,
    },
    /** type of hint generation used. */
    type: {
        type: String
    },
    /** whether the student expression is new on the graph. */
    isNewNode: {
        type: Boolean
    },
    /** whether the hint generation algorithm created a shorter path. */
    createdShorterPath: {
        type: Boolean
    },
    /** the expression of the source node */
    sourceExpr: {
        type: String
    },
    /** the AST of the source node */
    sourceAST: {
        type: String
    },
    /** the expression of the target node */
    targetExpr: {
        type: String
    },
    /** the AST of the target node */
    targetAST: {
        type: String
    },
    /** the expression of the next node */
    nextExpr: {
        type: String
    },
    /** the AST of the next node */
    nextAST: {
        type: String
    },
    /** Total TED of the path */
    totalTED: {
        type: Number
    },
    /** TED between source and target */
    srcDstTED: {
        type: Number
    },
    /** the the tree edit operations used to generate the hint. */
    operations: {
        type: [String]
    },
    /** the hint generated. */
    hint: {
        type: String
    },
    /** the time it took to generate the hint (ms). */
    time: {
        type: String
    },
    /** the id of the model that originated the hint. */
    model_id: {
        type: String
    }
}))

export { HiGenAHint }