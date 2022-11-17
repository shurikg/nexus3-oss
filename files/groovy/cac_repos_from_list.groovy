import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.Repository

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

repositoryManager = repository.repositoryManager

def compareValue(gitValue, rtValue, messagePrefix, gitMessages, rtMessages) {
    if (gitValue != rtValue) {
        gitMessages.add("${messagePrefix} = ${gitValue}")
        rtMessages.add("${messagePrefix} = ${rtValue}")
    }
}

def compareHelmRepository(requireRepository, rtRepository, scriptResults) {
    Map<String, String> currentResult = [:]
    def gitChangeMessage = []
    def runtimeChangeMessage = []
    def authType = null

    compareValue(requireRepository.format, rtRepository.getFormat().getValue(), "format", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.type, rtRepository.getType().getValue(), "type", gitChangeMessage, runtimeChangeMessage )

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    compareValue(requireRepository.blob_store, repoAttributes['storage']['blobStoreName'], "blob store", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.strict_content_validation, repoAttributes['storage']['strictContentTypeValidation'],
                "strict content type validation", gitChangeMessage, runtimeChangeMessage )

    requireRepository.cleanup_policies.each { currentPolicy ->
        if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
            gitChangeMessage.add("new cleanup policy ${currentPolicy}")
            runtimeChangeMessage.add("N/A")
        }
    }
    if (repoAttributes?.cleanup?.policyName != null) {
        repoAttributes['cleanup']['policyName'].each { currentPolicy ->
            if (!(currentPolicy in requireRepository.cleanup_policies)) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
            }
        }
    }

    if (requireRepository.type == 'hosted') {
        compareValue(requireRepository.write_policy.toUpperCase(), repoAttributes['storage']['writePolicy'].toUpperCase(), "deployment policy", gitChangeMessage, runtimeChangeMessage )
    }

    if (requireRepository.type == 'proxy') {
        compareValue(requireRepository.remote_url, repoAttributes['proxy']['remoteUrl'], "remote url", gitChangeMessage, runtimeChangeMessage )
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            compareValue(requireRepository.use_nexus_truststore, repoAttributes?.httpclient?.connection?.useTrustStore, "use the nexus truststore", gitChangeMessage, runtimeChangeMessage)
        }
        compareValue(requireRepository.blocked, repoAttributes?.httpclient?.blocked, "blocked", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.auto_blocking_enabled, repoAttributes?.httpclient?.autoBlock, "auto blocking enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_component_age, repoAttributes['proxy']['contentMaxAge'], "maximum component age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_metadata_age, repoAttributes['proxy']['metadataMaxAge'], "maximum metadata age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_enabled, repoAttributes['negativeCache']['enabled'], "negative cache enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_ttl, repoAttributes['negativeCache']['timeToLive'], "negative cache ttl", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_username, repoAttributes?.httpclient?.authentication?.username, "remote username", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_password, repoAttributes?.httpclient?.authentication?.password, "remote password", gitChangeMessage, runtimeChangeMessage )
        if (requireRepository.remote_username != null) {
            authType = "username"
        }
        compareValue(authType, repoAttributes?.httpclient?.authentication?.type, "authentication type", gitChangeMessage, runtimeChangeMessage )
    }

    if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the ${requireRepository.name} repository will be updated")
        currentResult.put('resource', 'repository')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
}

def compareRawRepository(requireRepository, rtRepository, scriptResults) {
    Map<String, String> currentResult = [:]
    def gitChangeMessage = []
    def runtimeChangeMessage = []
    def authType = null

    compareValue(requireRepository.format, rtRepository.getFormat().getValue(), "format", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.type, rtRepository.getType().getValue(), "type", gitChangeMessage, runtimeChangeMessage )

    repoAttributes = rtRepository.getConfiguration().getAttributes()
    compareValue(requireRepository.blob_store, repoAttributes['storage']['blobStoreName'], "blob store", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.strict_content_validation, repoAttributes['storage']['strictContentTypeValidation'], "strict content type validation", gitChangeMessage, runtimeChangeMessage )

    if (requireRepository.type == 'hosted') {
        compareValue(requireRepository.write_policy.toUpperCase(), repoAttributes['storage']['writePolicy'].toUpperCase(), "deployment policy", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'proxy') {
        compareValue(requireRepository.remote_url, repoAttributes['proxy']['remoteUrl'], "remote url", gitChangeMessage, runtimeChangeMessage )
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            compareValue(requireRepository.use_nexus_truststore, repoAttributes?.httpclient?.connection?.useTrustStore, "use the nexus truststore", gitChangeMessage, runtimeChangeMessage)
        }
        compareValue(requireRepository.blocked, repoAttributes?.httpclient?.blocked, "blocked", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.auto_blocking_enabled, repoAttributes?.httpclient?.autoBlock, "auto blocking enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_component_age, repoAttributes['proxy']['contentMaxAge'], "maximum component age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_metadata_age, repoAttributes['proxy']['metadataMaxAge'], "maximum metadata age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_enabled, repoAttributes['negativeCache']['enabled'], "negative cache enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_ttl, repoAttributes['negativeCache']['timeToLive'], "negative cache ttl", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_username, repoAttributes?.httpclient?.authentication?.username, "remote username", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_password, repoAttributes?.httpclient?.authentication?.password, "remote password", gitChangeMessage, runtimeChangeMessage )
        if (requireRepository.remote_username != null) {
            authType = "username"
        }
        compareValue(authType, repoAttributes?.httpclient?.authentication?.type, "authentication type", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'group') {
        requireRepository.member_repos.each { currentMember ->
            if ( ! (currentMember in repoAttributes['group']['memberNames'])) {
                gitChangeMessage.add("new member group ${currentMember}")
                runtimeChangeMessage.add("N/A")
            }
        }
        repoAttributes['group']['memberNames'].each { currentMember ->
            if ( ! (currentMember in requireRepository.member_repos)) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete member group ${currentMember}")
            }
        }
    }

    if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the ${requireRepository.name} repository will be updated")
        currentResult.put('resource', 'repository')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
}

def compareDockerRepository(requireRepository, rtRepository, scriptResults) {
    Map<String, String> currentResult = [:]
    def gitChangeMessage = []
    def runtimeChangeMessage = []
    def authType = null

    compareValue(requireRepository.format, rtRepository.getFormat().getValue(), "format", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.type, rtRepository.getType().getValue(), "type", gitChangeMessage, runtimeChangeMessage )

    repoAttributes = rtRepository.getConfiguration().getAttributes()
    compareValue(requireRepository.blob_store, repoAttributes['storage']['blobStoreName'], "blob store", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.strict_content_validation, repoAttributes['storage']['strictContentTypeValidation'], "strict content type validation", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.http_port, repoAttributes['docker']['httpPort'], "http port", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.https_port, repoAttributes['docker']['httpsPort'], "https port", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.v1_enabled, repoAttributes['docker']['v1Enabled'], "v1 API enabled", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.force_basic_auth, repoAttributes['docker']['forceBasicAuth'], "force basic auth", gitChangeMessage, runtimeChangeMessage )

    if (requireRepository.type == 'hosted') {
        compareValue(requireRepository.write_policy.toUpperCase(), repoAttributes['storage']['writePolicy'].toUpperCase(), "deployment policy", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'proxy') {
        compareValue(requireRepository.remote_url, repoAttributes['proxy']['remoteUrl'], "remote url", gitChangeMessage, runtimeChangeMessage )
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            compareValue(requireRepository.use_nexus_truststore, repoAttributes?.httpclient?.connection?.useTrustStore, "use the nexus truststore", gitChangeMessage, runtimeChangeMessage)
        }
        compareValue(requireRepository.blocked, repoAttributes?.httpclient?.blocked, "blocked", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.auto_blocking_enabled, repoAttributes?.httpclient?.autoBlock, "auto blocking enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_component_age, repoAttributes['proxy']['contentMaxAge'], "maximum component age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_metadata_age, repoAttributes['proxy']['metadataMaxAge'], "maximum metadata age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_enabled, repoAttributes['negativeCache']['enabled'], "negative cache enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_ttl, repoAttributes['negativeCache']['timeToLive'], "negative cache ttl", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.index_type, repoAttributes['dockerProxy']['indexType'], "index type", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.use_nexus_certificates_to_access_index, repoAttributes['dockerProxy']['useTrustStoreForIndexAccess'],
                                    "use nexus certificates to access index", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.foreign_layer_url_whitelist, repoAttributes['dockerProxy']['foreignLayerUrlWhitelist'], "foreign layer url whitelist", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.cache_foreign_layers, repoAttributes['dockerProxy']['cacheForeignLayers'], "cache foreign layers", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_username, repoAttributes?.httpclient?.authentication?.username, "remote username", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_password, repoAttributes?.httpclient?.authentication?.password, "remote password", gitChangeMessage, runtimeChangeMessage )
        if (requireRepository.remote_username != null) {
            authType = "username"
        }
        compareValue(authType, repoAttributes?.httpclient?.authentication?.type, "authentication type", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }
    if (requireRepository.type == 'group') {
        requireRepository.member_repos.each { currentMember ->
            if ( ! (currentMember in repoAttributes['group']['memberNames'])) {
                gitChangeMessage.add("new member group ${currentMember}")
                runtimeChangeMessage.add("N/A")
            }
        }
        repoAttributes['group']['memberNames'].each { currentMember ->
            if ( ! (currentMember in requireRepository.member_repos)) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete member group ${currentMember}")
            }
        }
    }

    if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the ${requireRepository.name} repository will be updated")
        currentResult.put('resource', 'repository')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
}

def compareMavenRepository(requireRepository, rtRepository, scriptResults) {
    Map<String, String> currentResult = [:]
    def gitChangeMessage = []
    def runtimeChangeMessage = []
    def authType = null

    compareValue(requireRepository.format, rtRepository.getFormat().getValue(), "format", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.type, rtRepository.getType().getValue(), "type", gitChangeMessage, runtimeChangeMessage )

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    compareValue(requireRepository.blob_store, repoAttributes['storage']['blobStoreName'], "blob store", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.strict_content_validation, repoAttributes['storage']['strictContentTypeValidation'],
                "strict content type validation", gitChangeMessage, runtimeChangeMessage )

    if (requireRepository.type == 'hosted') {
        compareValue(requireRepository.write_policy.toUpperCase(), repoAttributes['storage']['writePolicy'].toUpperCase(), "deployment policy", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.layout_policy.toUpperCase(), repoAttributes['maven']['layoutPolicy'].toUpperCase(), "layout policy", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.version_policy.toUpperCase(), repoAttributes['maven']['versionPolicy'].toUpperCase(), "version policy", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null && !repoAttributes?.cleanup?.policyName.isEmpty()) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'proxy') {
        compareValue(requireRepository.remote_url, repoAttributes['proxy']['remoteUrl'], "remote url", gitChangeMessage, runtimeChangeMessage )
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            compareValue(requireRepository.use_nexus_truststore, repoAttributes?.httpclient?.connection?.useTrustStore, "use the nexus truststore", gitChangeMessage, runtimeChangeMessage)
        }
        compareValue(requireRepository.blocked, repoAttributes?.httpclient?.blocked, "blocked", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.auto_blocking_enabled, repoAttributes?.httpclient?.autoBlock, "auto blocking enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_component_age, repoAttributes['proxy']['contentMaxAge'], "maximum component age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_metadata_age, repoAttributes['proxy']['metadataMaxAge'], "maximum metadata age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_enabled, repoAttributes['negativeCache']['enabled'], "negative cache enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_ttl, repoAttributes['negativeCache']['timeToLive'], "negative cache ttl", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.layout_policy.toUpperCase(), repoAttributes['maven']['layoutPolicy'].toUpperCase(), "layout policy", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.version_policy.toUpperCase(), repoAttributes['maven']['versionPolicy'].toUpperCase(), "version policy", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_username, repoAttributes?.httpclient?.authentication?.username, "remote username", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_password, repoAttributes?.httpclient?.authentication?.password, "remote password", gitChangeMessage, runtimeChangeMessage )
        if (requireRepository.remote_username != null) {
            authType = "username"
        }
        compareValue(authType, repoAttributes?.httpclient?.authentication?.type, "authentication type", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'group') {
        requireRepository.member_repos.each { currentMember ->
            if ( ! (currentMember in repoAttributes['group']['memberNames'])) {
                gitChangeMessage.add("new member group ${currentMember}")
                runtimeChangeMessage.add("N/A")
            }
        }
        repoAttributes['group']['memberNames'].each { currentMember ->
            if ( ! (currentMember in requireRepository.member_repos)) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete member group ${currentMember}")
            }
        }
    }

    if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the ${requireRepository.name} repository will be updated")
        currentResult.put('resource', 'repository')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
}

def compareNpmRepository(requireRepository, rtRepository, scriptResults) {
    Map<String, String> currentResult = [:]
    def gitChangeMessage = []
    def runtimeChangeMessage = []
    def authType = null

    compareValue(requireRepository.format, rtRepository.getFormat().getValue(), "format", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.type, rtRepository.getType().getValue(), "type", gitChangeMessage, runtimeChangeMessage )

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    compareValue(requireRepository.blob_store, repoAttributes['storage']['blobStoreName'], "blob store", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.strict_content_validation, repoAttributes['storage']['strictContentTypeValidation'],
            "strict content type validation", gitChangeMessage, runtimeChangeMessage )

    if (requireRepository.type == 'hosted') {
        compareValue(requireRepository.write_policy.toUpperCase(), repoAttributes['storage']['writePolicy'].toUpperCase(), "deployment policy", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'proxy') {
        compareValue(requireRepository.remote_url, repoAttributes['proxy']['remoteUrl'], "remote url", gitChangeMessage, runtimeChangeMessage )
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            compareValue(requireRepository.use_nexus_truststore, repoAttributes?.httpclient?.connection?.useTrustStore, "use the nexus truststore", gitChangeMessage, runtimeChangeMessage)
        }
        compareValue(requireRepository.blocked, repoAttributes?.httpclient?.blocked, "blocked", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.auto_blocking_enabled, repoAttributes?.httpclient?.autoBlock, "auto blocking enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_component_age, repoAttributes['proxy']['contentMaxAge'], "maximum component age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_metadata_age, repoAttributes['proxy']['metadataMaxAge'], "maximum metadata age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_enabled, repoAttributes['negativeCache']['enabled'], "negative cache enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_ttl, repoAttributes['negativeCache']['timeToLive'], "negative cache ttl", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_username, repoAttributes?.httpclient?.authentication?.username, "remote username", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_password, repoAttributes?.httpclient?.authentication?.password, "remote password", gitChangeMessage, runtimeChangeMessage )
        if (requireRepository.remote_username != null) {
            authType = "username"
        }
        compareValue(authType, repoAttributes?.httpclient?.authentication?.type, "authentication type", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'group') {
        requireRepository.member_repos.each { currentMember ->
            if ( ! (currentMember in repoAttributes['group']['memberNames'])) {
                gitChangeMessage.add("new member group ${currentMember}")
                runtimeChangeMessage.add("N/A")
            }
        }
        repoAttributes['group']['memberNames'].each { currentMember ->
            if ( ! (currentMember in requireRepository.member_repos)) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete member group ${currentMember}")
            }
        }
    }

    if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the ${requireRepository.name} repository will be updated")
        currentResult.put('resource', 'repository')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
}

def compareYumRepository(requireRepository, rtRepository, scriptResults) {
    Map<String, String> currentResult = [:]
    def gitChangeMessage = []
    def runtimeChangeMessage = []
    def authType = null

    compareValue(requireRepository.format, rtRepository.getFormat().getValue(), "format", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.type, rtRepository.getType().getValue(), "type", gitChangeMessage, runtimeChangeMessage )

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    compareValue(requireRepository.blob_store, repoAttributes['storage']['blobStoreName'], "blob store", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.strict_content_validation, repoAttributes['storage']['strictContentTypeValidation'],
            "strict content type validation", gitChangeMessage, runtimeChangeMessage )

    if (requireRepository.type == 'hosted') {
        compareValue(requireRepository.write_policy.toUpperCase(), repoAttributes['storage']['writePolicy'].toUpperCase(), "deployment policy", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.layout_policy.toUpperCase(), repoAttributes['yum']['layoutPolicy'].toUpperCase(), "layout policy", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.repodata_depth, repoAttributes['yum']['repodataDepth'], "deployment policy", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'proxy') {
        compareValue(requireRepository.remote_url, repoAttributes['proxy']['remoteUrl'], "remote url", gitChangeMessage, runtimeChangeMessage )
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            compareValue(requireRepository.use_nexus_truststore, repoAttributes?.httpclient?.connection?.useTrustStore, "use the nexus truststore", gitChangeMessage, runtimeChangeMessage)
        }
        compareValue(requireRepository.blocked, repoAttributes?.httpclient?.blocked, "blocked", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.auto_blocking_enabled, repoAttributes?.httpclient?.autoBlock, "auto blocking enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_component_age, repoAttributes['proxy']['contentMaxAge'], "maximum component age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_metadata_age, repoAttributes['proxy']['metadataMaxAge'], "maximum metadata age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_enabled, repoAttributes['negativeCache']['enabled'], "negative cache enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_ttl, repoAttributes['negativeCache']['timeToLive'], "negative cache ttl", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_username, repoAttributes?.httpclient?.authentication?.username, "remote username", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_password, repoAttributes?.httpclient?.authentication?.password, "remote password", gitChangeMessage, runtimeChangeMessage )
        if (requireRepository.remote_username != null) {
            authType = "username"
        }
        compareValue(authType, repoAttributes?.httpclient?.authentication?.type, "authentication type", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'group') {
        requireRepository.member_repos.each { currentMember ->
            if ( ! (currentMember in repoAttributes['group']['memberNames'])) {
                gitChangeMessage.add("new member group ${currentMember}")
                runtimeChangeMessage.add("N/A")
            }
        }
        repoAttributes['group']['memberNames'].each { currentMember ->
            if ( ! (currentMember in requireRepository.member_repos)) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete member group ${currentMember}")
            }
        }
    }

    if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the ${requireRepository.name} repository will be updated")
        currentResult.put('resource', 'repository')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
}

def compareNugetRepository(requireRepository, rtRepository, scriptResults) {
    Map<String, String> currentResult = [:]
    def gitChangeMessage = []
    def runtimeChangeMessage = []
    def authType = null

    compareValue(requireRepository.format, rtRepository.getFormat().getValue(), "format", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.type, rtRepository.getType().getValue(), "type", gitChangeMessage, runtimeChangeMessage )

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    compareValue(requireRepository.blob_store, repoAttributes['storage']['blobStoreName'], "blob store", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.strict_content_validation, repoAttributes['storage']['strictContentTypeValidation'],
            "strict content type validation", gitChangeMessage, runtimeChangeMessage )

    if (requireRepository.type == 'hosted') {
        compareValue(requireRepository.write_policy.toUpperCase(), repoAttributes['storage']['writePolicy'].toUpperCase(), "deployment policy", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'proxy') {
        compareValue(requireRepository.remote_url, repoAttributes['proxy']['remoteUrl'], "remote url", gitChangeMessage, runtimeChangeMessage )
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            compareValue(requireRepository.use_nexus_truststore, repoAttributes?.httpclient?.connection?.useTrustStore, "use the nexus truststore", gitChangeMessage, runtimeChangeMessage)
        }
        compareValue(requireRepository.blocked, repoAttributes?.httpclient?.blocked, "blocked", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.auto_blocking_enabled, repoAttributes?.httpclient?.autoBlock, "auto blocking enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_component_age, repoAttributes['proxy']['contentMaxAge'], "maximum component age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_metadata_age, repoAttributes['proxy']['metadataMaxAge'], "maximum metadata age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_enabled, repoAttributes['negativeCache']['enabled'], "negative cache enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_ttl, repoAttributes['negativeCache']['timeToLive'], "negative cache ttl", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_username, repoAttributes?.httpclient?.authentication?.username, "remote username", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_password, repoAttributes?.httpclient?.authentication?.password, "remote password", gitChangeMessage, runtimeChangeMessage )
        if (requireRepository.remote_username != null) {
            authType = "username"
        }
        compareValue(authType, repoAttributes?.httpclient?.authentication?.type, "authentication type", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'group') {
        requireRepository.member_repos.each { currentMember ->
            if ( ! (currentMember in repoAttributes['group']['memberNames'])) {
                gitChangeMessage.add("new member group ${currentMember}")
                runtimeChangeMessage.add("N/A")
            }
        }
        repoAttributes['group']['memberNames'].each { currentMember ->
            if ( ! (currentMember in requireRepository.member_repos)) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete member group ${currentMember}")
            }
        }
    }

    if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the ${requireRepository.name} repository will be updated")
        currentResult.put('resource', 'repository')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
}

def compareGoRepository(requireRepository, rtRepository, scriptResults) {
    Map<String, String> currentResult = [:]
    def gitChangeMessage = []
    def runtimeChangeMessage = []
    def authType = null

    compareValue(requireRepository.format, rtRepository.getFormat().getValue(), "format", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.type, rtRepository.getType().getValue(), "type", gitChangeMessage, runtimeChangeMessage )

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    compareValue(requireRepository.blob_store, repoAttributes['storage']['blobStoreName'], "blob store", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.strict_content_validation, repoAttributes['storage']['strictContentTypeValidation'],
            "strict content type validation", gitChangeMessage, runtimeChangeMessage )

    if (requireRepository.type == 'proxy') {
        compareValue(requireRepository.remote_url, repoAttributes['proxy']['remoteUrl'], "remote url", gitChangeMessage, runtimeChangeMessage )
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            compareValue(requireRepository.use_nexus_truststore, repoAttributes?.httpclient?.connection?.useTrustStore, "use the nexus truststore", gitChangeMessage, runtimeChangeMessage)
        }
        compareValue(requireRepository.blocked, repoAttributes?.httpclient?.blocked, "blocked", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.auto_blocking_enabled, repoAttributes?.httpclient?.autoBlock, "auto blocking enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_component_age, repoAttributes['proxy']['contentMaxAge'], "maximum component age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_metadata_age, repoAttributes['proxy']['metadataMaxAge'], "maximum metadata age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_enabled, repoAttributes['negativeCache']['enabled'], "negative cache enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_ttl, repoAttributes['negativeCache']['timeToLive'], "negative cache ttl", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_username, repoAttributes?.httpclient?.authentication?.username, "remote username", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_password, repoAttributes?.httpclient?.authentication?.password, "remote password", gitChangeMessage, runtimeChangeMessage )
        if (requireRepository.remote_username != null) {
            authType = "username"
        }
        compareValue(authType, repoAttributes?.httpclient?.authentication?.type, "authentication type", gitChangeMessage, runtimeChangeMessage )

        requireRepository.cleanup_policies.each { currentPolicy ->
            if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
                gitChangeMessage.add("new cleanup policy ${currentPolicy}")
                runtimeChangeMessage.add("N/A")
            }
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            repoAttributes['cleanup']['policyName'].each { currentPolicy ->
                if (!(currentPolicy in requireRepository.cleanup_policies)) {
                    gitChangeMessage.add("N/A")
                    runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
                }
            }
        }
    }

    if (requireRepository.type == 'group') {
        requireRepository.member_repos.each { currentMember ->
            if ( ! (currentMember in repoAttributes['group']['memberNames'])) {
                gitChangeMessage.add("new member group ${currentMember}")
                runtimeChangeMessage.add("N/A")
            }
        }
        repoAttributes['group']['memberNames'].each { currentMember ->
            if ( ! (currentMember in requireRepository.member_repos)) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete member group ${currentMember}")
            }
        }
    }

    if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the ${requireRepository.name} repository will be updated")
        currentResult.put('resource', 'repository')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
}

def compareAptRepository(requireRepository, rtRepository, scriptResults) {
    Map<String, String> currentResult = [:]
    def gitChangeMessage = []
    def runtimeChangeMessage = []
    def authType = null

    compareValue(requireRepository.format, rtRepository.getFormat().getValue(), "format", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.type, rtRepository.getType().getValue(), "type", gitChangeMessage, runtimeChangeMessage )

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    compareValue(requireRepository.blob_store, repoAttributes['storage']['blobStoreName'], "blob store", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.strict_content_validation, repoAttributes['storage']['strictContentTypeValidation'],
            "strict content type validation", gitChangeMessage, runtimeChangeMessage )
    compareValue(requireRepository.distribution, repoAttributes['apt']['distribution'], "distribution", gitChangeMessage, runtimeChangeMessage )

    requireRepository.cleanup_policies.each { currentPolicy ->
        if (repoAttributes?.cleanup == null || repoAttributes?.cleanup?.policyName == null || ! (currentPolicy in repoAttributes['cleanup']['policyName'])) {
            gitChangeMessage.add("new cleanup policy ${currentPolicy}")
            runtimeChangeMessage.add("N/A")
        }
    }
    if (repoAttributes?.cleanup?.policyName != null) {
        repoAttributes['cleanup']['policyName'].each { currentPolicy ->
            if (!(currentPolicy in requireRepository.cleanup_policies)) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete cleanup policy ${currentPolicy}")
            }
        }
    }

    if (requireRepository.type == 'hosted') {
        compareValue(requireRepository.write_policy.toUpperCase(), repoAttributes['storage']['writePolicy'].toUpperCase(), "deployment policy", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.keypair, repoAttributes['aptSigning']['keypair'], "key pair", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.passphrase, repoAttributes['aptSigning']['passphrase'], "passphrase", gitChangeMessage, runtimeChangeMessage )
    }

    if (requireRepository.type == 'proxy') {
        compareValue(requireRepository.flat, repoAttributes['apt']['flat'], "flat", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_url, repoAttributes['proxy']['remoteUrl'], "remote url", gitChangeMessage, runtimeChangeMessage )
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            compareValue(requireRepository.use_nexus_truststore, repoAttributes?.httpclient?.connection?.useTrustStore, "use the nexus truststore", gitChangeMessage, runtimeChangeMessage)
        }
        compareValue(requireRepository.blocked, repoAttributes?.httpclient?.blocked, "blocked", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.auto_blocking_enabled, repoAttributes?.httpclient?.autoBlock, "auto blocking enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_component_age, repoAttributes['proxy']['contentMaxAge'], "maximum component age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.maximum_metadata_age, repoAttributes['proxy']['metadataMaxAge'], "maximum metadata age", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_enabled, repoAttributes['negativeCache']['enabled'], "negative cache enabled", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.negative_cache_ttl, repoAttributes['negativeCache']['timeToLive'], "negative cache ttl", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_username, repoAttributes?.httpclient?.authentication?.username, "remote username", gitChangeMessage, runtimeChangeMessage )
        compareValue(requireRepository.remote_password, repoAttributes?.httpclient?.authentication?.password, "remote password", gitChangeMessage, runtimeChangeMessage )
        if (requireRepository.remote_username != null) {
            authType = "username"
        }
        compareValue(authType, repoAttributes?.httpclient?.authentication?.type, "authentication type", gitChangeMessage, runtimeChangeMessage )
    }

    if (requireRepository.type == 'group') {
        requireRepository.member_repos.each { currentMember ->
            if ( ! (currentMember in repoAttributes['group']['memberNames'])) {
                gitChangeMessage.add("new member group ${currentMember}")
                runtimeChangeMessage.add("N/A")
            }
        }
        repoAttributes['group']['memberNames'].each { currentMember ->
            if ( ! (currentMember in requireRepository.member_repos)) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete member group ${currentMember}")
            }
        }
    }

    if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the ${requireRepository.name} repository will be updated")
        currentResult.put('resource', 'repository')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
}

parsed_args.details.each { currentRepo ->

    Map<String, String> currentResult = [:]
    existingRepository = repositoryManager.get(currentRepo.name)

    if (existingRepository == null) {
        currentResult.put('change_in_git', "definition of new ${currentRepo.name} repository")
        currentResult.put('change_in_runtime', 'N/A')
        currentResult.put('change_type', 'add')
        currentResult.put('description', "the ${currentRepo.name} repository (format: ${currentRepo.format}, type: ${currentRepo.type}) will be added")
        currentResult.put('resource', 'repository')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
    else {
        switch(currentRepo.format) {
            case 'helm':
                compareHelmRepository(currentRepo, existingRepository, scriptResults)
                break
            case 'raw':
                compareRawRepository(currentRepo, existingRepository, scriptResults)
                break
            case 'docker':
                compareDockerRepository(currentRepo, existingRepository, scriptResults)
                break
            case 'maven2':
                compareMavenRepository(currentRepo, existingRepository, scriptResults)
                break
            case 'npm':
                compareNpmRepository(currentRepo, existingRepository, scriptResults)
                break
            case 'yum':
                compareYumRepository(currentRepo, existingRepository, scriptResults)
                break
            case 'nuget':
                compareNugetRepository(currentRepo, existingRepository, scriptResults)
                break
            case 'go':
                compareGoRepository(currentRepo, existingRepository, scriptResults)
                break
            case 'apt':
                compareAptRepository(currentRepo, existingRepository, scriptResults)
                break
        }
    }
}

repositoryManager.browse().each { rtRepo ->
    def needToDelete = true
    Map<String, String> currentResult = [:]

    parsed_args.details.any { repoDef ->
        if (rtRepo.getName() == repoDef.name) {
            needToDelete = false
            return true
        }
    }
    if (needToDelete){
        currentResult.put('change_in_git', 'N/A')
        currentResult.put('change_in_runtime', "${rtRepo.getName()} repository exist")
        currentResult.put('change_type', 'delete')
        currentResult.put('description', "the ${rtRepo.getName()} repository (format: ${rtRepo.getFormat()}, type: ${rtRepo.getType()}) will be deleted")
        currentResult.put('resource', 'repository')
        currentResult.put('downtime', false)

        scriptResults['action_details'].add(currentResult)

        if (! parsed_args.dry_run) {
            repositoryManager.delete(rtRepo.getName())
        }
    }
}
return JsonOutput.toJson(scriptResults)
