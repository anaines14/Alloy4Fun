/**
 * This file specifies the syntax highlighting rules for the codemirror editor
 * each rule is composed by regex and token, which allows for css rules on the tokens
 * such as .cm.comment {...} and also to refer to those tokens after parsing the syntax
 */

import CodeMirror from 'codemirror';
import * as simpleMode from 'codemirror/addon/mode/simple';
export {
    defineAlloyMode
};

function defineAlloyMode() {
    CodeMirror.defineSimpleMode("alloy", {
        start: [{
            regex: /(\W)(abstract|fun|all|iff|check|but|else|assert|extends|set|fact|implies|module|open|sig|and|disj|for|in|no|or|as|Int|pred|sum|exactly|iden|let|not|run|univ)(?:\b)/,
            //Token value refers to regex captured matching groups.
            token: [null, "keyword"]
        }, {
            //For some reason " ^ " doesn't work as supposed (line start). A workaround was necessary to correctly identify keywords on the beginning of the line.
            regex: /(abstract|fun|all|iff|check|but|else|assert|extends|set|fact|implies|module|open|sig|and|disj|for|in|no|or|as|Int|pred|sum|exactly|iden|let|not|run|univ)(?:\b)/,
            token: "keyword",
            //Rule that only applies if the match is at the start of some line(workaround).
            sol: true
        }, {
            regex: /(\W)(one|lone|none|some)(?:\b)/,
            token: [null, "atom"]
        }, {
            //For some reason " ^ " doesn't work as supposed (line start). A workaround was necessary to correctly identify atoms on the beginning of the line.
            regex: /(one|lone|none|some)(?:\b)/,
            token: "atom",
            //Rule that only applies if the match is at the start of some line(workaround).
            sol: true
        }, {
            regex: /^\/\/SECRET$/mg,
            token: "secret",
            sol: true
        }, {
            regex: /\/\*/,
            token: "comment",
            //Jump to comment mode.
            next: "comment"
        }, {
            regex: /(\+\+ )|=>|=<|->|>=|\|\||<:|:>/,
            token: "operator"
        }, {
            //Line comment.
            regex: /\/\/.*/,
            token: "comment"
        }, {
            regex: /(\s+|\||{|})[0-9]+](?:\b)/,
            token: [null, "number"]
        }, {
            regex: /(\s+|\||{|})[0-9]+](?:\b)/,
            token: "number",
            //Rule that only applies if the match is at the start of some line(workaround).
            sol: true
        }, {
            regex: /[{(]/,
            indent: true
        }, {
            regex: /[})]/,
            dedent: true
        }],
        //Modes allow applying a different set of rules to different contexts.
        comment: [{
                //When the comment block end tag is found...
                regex: /.*?\*\//,
                token: "comment",
                //...go back to start mode.
                next: "start"
            }, {
                regex: /.*/,
                token: "comment"
            }

        ],
        //Simple Mode additional settings, check documentation for more information.
        meta: {
            //Prevent indentation inside comments.
            dontIndentStates: ["comment"],
            lineComment: "//"
        }
    });
}