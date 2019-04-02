package org.cloudfoundry.credhub.controllers.v1.regenerate

import org.cloudfoundry.credhub.generate.RegenerateHandler
import org.cloudfoundry.credhub.views.BulkRegenerateResults
import org.cloudfoundry.credhub.views.CredentialView

class SpyRegenerateHandler : RegenerateHandler {

    var handleRegenerate__calledWith_credentialName: String? = null
    override fun handleRegenerate(credentialName: String): CredentialView {
        handleRegenerate__calledWith_credentialName = credentialName

        return CredentialView()
    }

    var handleBulkRegenerate_calledWith_signerName: String? = null
    override fun handleBulkRegenerate(signerName: String): BulkRegenerateResults {
        handleBulkRegenerate_calledWith_signerName = signerName

        return BulkRegenerateResults()
    }
}
