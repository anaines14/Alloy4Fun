/**
 * Module that handles model and instance sharing.
 *
 * @module client/lib/editor/genUrl
 */

 import {
    zeroclipboard,
} from "./clipboard"
import {
    displayError,
} from "./feedback"
import {
    secretTag 
} from "../../../lib/editor/text"
import {
    modelShared,
    getCommandIndex,
    instShared,
} from "./state"

/**
 * Store and share the current model and generate the sharing URLs.
 */
export function shareModel() {
    const themeData = {
        atomSettings,
        relationSettings,
        generalSettings,
        currentFramePosition,
        currentlyProjectedSigs,
        metaPrimSigs,
        metaSubsetSigs,
    };

    let modelToShare = textEditor.getValue();
    Meteor.call('genURL', modelToShare, Session.get("last_id"), themeData, handleShareModel);
}

/**
 * Store and share the current instance and generate the sharing URL. The
 * respective model is already stored due to the execution.
 */
export function shareInstance() {
    const themeData = {
        atomSettings,
        relationSettings,
        generalSettings,
        currentFramePosition,
        currentlyProjectedSigs,
        metaPrimSigs,
        metaSubsetSigs,
    };
    Meteor.call("storeInstance", Session.get("last_id"), getCommandIndex(), cy.json(), themeData, handleShareInstance)
}

/** 
 * Handles the response to the model sharing request. 
 *
 * @param {Error} err the possible meteor error
 * @param {Object} result the result to the genURL meteor call
 */
function handleShareModel(err, result) {
    if (err) return displayError(err)

    Session.set('public-model-url',`${window.location.origin}/`+result['public']);
    Session.set('private-model-url',`${window.location.origin}/`+result['private']);

    modelShared();
}

/** 
 * Handles the response to the instance sharing request.
 *
 * @param {Error} err the possible meteor error
 * @param {Object} result the result to the storeInstance meteor call
 */
function handleShareInstance(err, result) {
    if (err) return displayError(err)

    Session.set('inst-url',`${window.location.origin}/`+result);

    instShared();
}