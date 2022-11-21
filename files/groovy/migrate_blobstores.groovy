import groovy.json.JsonOutput

def fileName = "blob_stores.yml"
def migrationBlobs = ['nexus_blobstores': []]
Map scriptResults = [changed: false, error: false, 'action_details': [:]]

blobStore.getBlobStoreManager().browse().each { rtBlob ->
    Map<String, String> currentBlob = [:]

    currentBlob.put('name', rtBlob.getBlobStoreConfiguration().getName())
    currentBlob.put('type', rtBlob.getBlobStoreConfiguration().getType())
    if (rtBlob.getBlobStoreConfiguration().getType() == 'File') {
        def blobAttributes = rtBlob.getBlobStoreConfiguration().getAttributes()
        currentBlob.put('path', blobAttributes['file']['path'])
    }
    migrationBlobs['nexus_blobstores'].add(currentBlob)
}
scriptResults['action_details'].put(fileName, migrationBlobs)

return JsonOutput.toJson(scriptResults)
