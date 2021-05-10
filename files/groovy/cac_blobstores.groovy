import groovy.json.JsonOutput
import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

parsed_args.details.each { blobstoreDef ->

    Map<String, String> currentResult = [:]

    existingBlobStore = blobStore.getBlobStoreManager().get(blobstoreDef.name)
    if (existingBlobStore == null) {
        currentResult.put('change_in_git', "definition of new ${blobstoreDef.name} blobstore")
        currentResult.put('change_in_runtime', 'N/A')
        currentResult.put('change_type', 'add')
        currentResult.put('description', "the ${blobstoreDef.name} blobstore will be added")
        currentResult.put('resource', 'blobstore')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    // }
        // try {
        //     if (blobstoreDef.type == "S3") {
        //         blobStore.createS3BlobStore(blobstoreDef.name, blobstoreDef.config)
        //         msg = "S3 blobstore {} created"
        //     } else {
        //         blobStore.createFileBlobStore(blobstoreDef.name, blobstoreDef.path)
        //         msg = "File blobstore {} created"
        //     }
        //     log.info(msg, blobstoreDef.name)
        //     currentResult.put('status', 'created')
        //     scriptResults['changed'] = true
        // } catch (Exception e) {
        //     log.error('Could not create blobstore {}: {}', blobstoreDef.name, e.toString())
        //     currentResult.put('status', 'error')
        //     scriptResults['error'] = true
        //     currentResult.put('error_msg', e.toString())
        // }
    } else {
        log.info("Blobstore {} already exists. Left untouched", blobstoreDef.name)
        currentResult.put('status', 'exists')
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
        currentResult.put('description', "the ${rtBlobName} blobstore will be deleted ${atr}")
        currentResult.put('resource', 'blobstore')
        currentResult.put('downtime', false)

        scriptResults['action_details'].add(currentResult)
    }
}

return JsonOutput.toJson(scriptResults)
