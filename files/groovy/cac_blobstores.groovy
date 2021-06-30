import groovy.json.JsonOutput
import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

parsed_args.details.each { blobstoreDef ->
    Map<String, String> currentResult = [:]
    def gitChangeMessage = []
    def runtimeChangeMessage = []
    def blobDefType = 'File'

    existingBlobStore = blobStore.getBlobStoreManager().get(blobstoreDef.name)
    if (existingBlobStore == null) {
        currentResult.put('change_in_git', "definition of new ${blobstoreDef.name} blobstore")
        currentResult.put('change_in_runtime', 'N/A')
        currentResult.put('change_type', 'add')
        currentResult.put('description', "the ${blobstoreDef.name} blobstore will be added")
        currentResult.put('resource', 'blobstore')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    } else {
        if (blobstoreDef.type == 'S3')
        {
            blobDefType = 'S3'
        }
        if (blobDefType != existingBlobStore.getBlobStoreConfiguration().getType()) {
            gitChangeMessage.add("type = ${blobDefType}")
            runtimeChangeMessage.add("type = ${existingBlobStore.getBlobStoreConfiguration().getType()}")
        }
        if (blobDefType == existingBlobStore.getBlobStoreConfiguration().getType() && blobDefType == 'File') {
            def blobAttributes = existingBlobStore.getBlobStoreConfiguration().getAttributes()

            if (blobstoreDef.path != blobAttributes['file']['path']) {
                gitChangeMessage.add("path = ${blobstoreDef.path}")
                runtimeChangeMessage.add("path = ${blobAttributes['file']['path']}")
            }
        }
        if (gitChangeMessage) {
            currentResult.put('change_in_git', gitChangeMessage.join('\n'))
            currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
            currentResult.put('change_type', 'change')
            currentResult.put('description', "the blob changes - NOT SUPPORTED")
            currentResult.put('resource', 'blobstore')
            currentResult.put('downtime', false)
            scriptResults['action_details'].add(currentResult)
        }
    }
}

blobStore.getBlobStoreManager().browse().each { rtBlob ->
    def needToDelete = true
    Map<String, String> currentResult = [:]
    def rtBlobName = rtBlob.getBlobStoreConfiguration().getName()
    def atr = rtBlob.getBlobStoreConfiguration().getAttributes()

    parsed_args.details.any { blobDef ->
        if (rtBlobName == blobDef.name) {
            needToDelete = false
            return true
        }
    }
    if (needToDelete){
        currentResult.put('change_in_git', "N/A")
        currentResult.put('change_in_runtime', "${rtBlobName}")
        currentResult.put('change_type', 'delete')
        currentResult.put('description', "the ${rtBlobName} blobstore will be deleted")
        currentResult.put('resource', 'blobstore')
        currentResult.put('downtime', false)

        scriptResults['action_details'].add(currentResult)

        if (! parsed_args.dry_run) {
            rtBlob.remove()
        }
    }
}

return JsonOutput.toJson(scriptResults)
