package org.jenkinsci.test.acceptance.global

import org.jenkinsci.test.acceptance.po.Page

/**
 * Geb Page Object for the global config page
 * Currently it contains only the relevant Artifactory plugin config elements and the page Save/Apply button
 */
class GlobalConfigurationPage extends Page {
    static url = 'configure'
    static at = { title == 'Configure System [Jenkins]' }
    static content = {
        saveButton { $('button', text: 'Save') }
        applyButton { $('button', text: 'Apply') }

        addArtifactoryButton {$('button', path: contains('Artifactory')) }
        artifactoryUrl(wait: true, required: false){ $('input', name: '_.artifactoryUrl')[0]}
        artifactoryUsername(wait: true, required: false){$('input', path: '/org-jfrog-hudson-ArtifactoryBuilder/artifactoryServer/deployerCredentials/username')[0]}
        artifactoryPassword(wait: true, required: false){$('input', path: '/org-jfrog-hudson-ArtifactoryBuilder/artifactoryServer/deployerCredentials/password')[0]}
        artifactoryTestConnectionButton(wait: true, required: false){$('button', text: 'Test Connection')[0]}
        goodConnectionFeedback(required: false){$('div.ok')}
        errorConnectionFeedback(required: false){$('div.error')}

    }



}
