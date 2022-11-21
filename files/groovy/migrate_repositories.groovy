import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.Repository

def migrationRepositories = ['nexus_repos_docker_hosted': [], 'nexus_repos_docker_proxy': [], 'nexus_repos_docker_group': [],
                             'nexus_repos_raw_proxy': [], 'nexus_repos_raw_hosted': [], 'nexus_repos_raw_group': [],
                             'nexus_repos_maven_proxy': [], 'nexus_repos_maven_hosted': [], 'nexus_repos_maven_group': [],
                             'nexus_repos_npm_proxy': [], 'nexus_repos_npm_hosted': [], 'nexus_repos_npm_group': [],
                             'nexus_repos_yum_proxy': [], 'nexus_repos_yum_hosted': [], 'nexus_repos_yum_group': [],
                             'nexus_repos_helm_hosted': [], 'nexus_repos_helm_proxy': [],
                             'nexus_repos_nuget_proxy': [], 'nexus_repos_nuget_hosted': [], 'nexus_repos_nuget_group': [],
                             'nexus_repos_go_proxy': [], 'nexus_repos_go_group': [],
                             'nexus_repos_apt_hosted': [], 'nexus_repos_apt_proxy': []]
Map scriptResults = [changed: false, error: false, 'action_details': [:]]

repositoryManager = repository.repositoryManager

def migrateHelmRepository(rtRepository, migrationRepositories) {
    Map<String, String> currentRepository = [name: rtRepository.getName()]

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    currentRepository.put('blob_store', repoAttributes['storage']['blobStoreName'])
    currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])
    if (repoAttributes?.cleanup?.policyName != null) {
        currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
    }

    if (rtRepository.getType().getValue() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
    }

    if (rtRepository.getType().getValue() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            currentRepository.put('use_nexus_truststore', repoAttributes?.httpclient?.connection?.useTrustStore)
        }
        currentRepository.put('blocked', repoAttributes?.httpclient?.blocked)
        currentRepository.put('auto_blocking_enabled', repoAttributes?.httpclient?.autoBlock)
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        if (repoAttributes?.httpclient?.authentication?.username != null) {
            currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
            currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
        }
    }

    switch(rtRepository.getType().getValue()) {
        case 'hosted':
            migrationRepositories['nexus_repos_helm_hosted'].add(currentRepository)
            break
        case 'proxy':
            migrationRepositories['nexus_repos_helm_proxy'].add(currentRepository)
            break
    }
}

def migrateRawRepository(rtRepository, migrationRepositories) {
    Map<String, String> currentRepository = [name: rtRepository.getName()]

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    currentRepository.put('blob_store', repoAttributes['storage']['blobStoreName'])
    currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])

    if (rtRepository.getType().getValue() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            currentRepository.put('use_nexus_truststore', repoAttributes?.httpclient?.connection?.useTrustStore)
        }
        currentRepository.put('blocked', repoAttributes?.httpclient?.blocked)
        currentRepository.put('auto_blocking_enabled', repoAttributes?.httpclient?.autoBlock)
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        if (repoAttributes?.httpclient?.authentication?.username != null) {
            currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
            currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'group') {
        currentRepository.put('member_repos', repoAttributes['group']['memberNames'])
    }

    switch(rtRepository.getType().getValue()) {
        case 'hosted':
            migrationRepositories['nexus_repos_raw_hosted'].add(currentRepository)
            break
        case 'proxy':
            migrationRepositories['nexus_repos_raw_proxy'].add(currentRepository)
            break
        case 'group':
            migrationRepositories['nexus_repos_raw_group'].add(currentRepository)
            break
    }
}

def migrateDockerRepository(rtRepository, migrationRepositories) {
    Map<String, String> currentRepository = [name: rtRepository.getName()]

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    currentRepository.put('blob_store', repoAttributes['storage']['blobStoreName'])
    currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])
    if (repoAttributes['docker']['httpPort'] != null && repoAttributes['docker']['httpPort'] != '' ) {
        currentRepository.put('http_port', repoAttributes['docker']['httpPort'].toInteger())
    }
    if (repoAttributes['docker']['httpsPort'] != null && repoAttributes['docker']['httpsPort'] != '') {
        currentRepository.put('https_port', repoAttributes['docker']['httpsPort'].toInteger())
    }
    if (repoAttributes['docker']['v1Enabled'] != null )
    {
        currentRepository.put('v1_enabled', repoAttributes['docker']['v1Enabled'])
    }
    currentRepository.put('force_basic_auth', repoAttributes['docker']['forceBasicAuth'])

    if (rtRepository.getType().getValue()  == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue()  == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            currentRepository.put('use_nexus_truststore', repoAttributes?.httpclient?.connection?.useTrustStore)
        }
        currentRepository.put('blocked', repoAttributes?.httpclient?.blocked)
        currentRepository.put('auto_blocking_enabled', repoAttributes?.httpclient?.autoBlock)
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        currentRepository.put('index_type', repoAttributes['dockerProxy']['indexType'])
        if (repoAttributes['dockerProxy']['useTrustStoreForIndexAccess'] != null) {
            currentRepository.put('use_nexus_certificates_to_access_index', repoAttributes['dockerProxy']['useTrustStoreForIndexAccess'])
        }
        currentRepository.put('foreign_layer_url_whitelist', repoAttributes['dockerProxy']['foreignLayerUrlWhitelist'])
        currentRepository.put('cache_foreign_layers', repoAttributes['dockerProxy']['cacheForeignLayers'])
        if (repoAttributes?.httpclient?.authentication?.username != null) {
            currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
            currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'group') {
        currentRepository.put('member_repos', repoAttributes['group']['memberNames'])
    }

    switch(rtRepository.getType().getValue()) {
        case 'hosted':
            migrationRepositories['nexus_repos_docker_hosted'].add(currentRepository)
            break
        case 'proxy':
            migrationRepositories['nexus_repos_docker_proxy'].add(currentRepository)
            break
        case 'group':
            migrationRepositories['nexus_repos_docker_group'].add(currentRepository)
            break
    }
}

def migrateMavenRepository(rtRepository, migrationRepositories) {
    Map<String, String> currentRepository = [name: rtRepository.getName()]

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    currentRepository.put('blob_store', repoAttributes['storage']['blobStoreName'])
    if (repoAttributes['storage']['strictContentTypeValidation'] == null ) {
        currentRepository.put('strict_content_validation', true)
    }
    else {
        currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])
    }

    if (rtRepository.getType().getValue() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
        currentRepository.put('layout_policy', repoAttributes['maven']['layoutPolicy'])
        currentRepository.put('version_policy', repoAttributes['maven']['versionPolicy'])
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            currentRepository.put('use_nexus_truststore', repoAttributes?.httpclient?.connection?.useTrustStore)
        }
        currentRepository.put('blocked', repoAttributes?.httpclient?.blocked)
        currentRepository.put('auto_blocking_enabled', repoAttributes?.httpclient?.autoBlock)
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        currentRepository.put('layout_policy', repoAttributes['maven']['layoutPolicy'])
        currentRepository.put('version_policy', repoAttributes['maven']['versionPolicy'])
        if (repoAttributes?.httpclient?.authentication?.username != null) {
            currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
            currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'group') {
        currentRepository.put('member_repos', repoAttributes['group']['memberNames'])
    }

    switch(rtRepository.getType().getValue()) {
        case 'hosted':
            migrationRepositories['nexus_repos_maven_hosted'].add(currentRepository)
            break
        case 'proxy':
            migrationRepositories['nexus_repos_maven_proxy'].add(currentRepository)
            break
        case 'group':
            migrationRepositories['nexus_repos_maven_group'].add(currentRepository)
            break
    }
}

def migrateNpmRepository(rtRepository, migrationRepositories) {
    Map<String, String> currentRepository = [name: rtRepository.getName()]

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    currentRepository.put('blob_store', repoAttributes['storage']['blobStoreName'])
    if (repoAttributes['storage']['strictContentTypeValidation'] == null ) {
        currentRepository.put('strict_content_validation', true)
    }
    else {
        currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])
    }

    if (rtRepository.getType().getValue() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            currentRepository.put('use_nexus_truststore', repoAttributes?.httpclient?.connection?.useTrustStore)
        }
        currentRepository.put('blocked', repoAttributes?.httpclient?.blocked)
        currentRepository.put('auto_blocking_enabled', repoAttributes?.httpclient?.autoBlock)
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        if (repoAttributes?.httpclient?.authentication?.username != null) {
            currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
            currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'group') {
        currentRepository.put('member_repos', repoAttributes['group']['memberNames'])
    }

    switch(rtRepository.getType().getValue()) {
        case 'hosted':
            migrationRepositories['nexus_repos_npm_hosted'].add(currentRepository)
            break
        case 'proxy':
            migrationRepositories['nexus_repos_npm_proxy'].add(currentRepository)
            break
        case 'group':
            migrationRepositories['nexus_repos_npm_group'].add(currentRepository)
            break
    }
}

def migrateYumRepository(rtRepository, migrationRepositories) {
    Map<String, String> currentRepository = [name: rtRepository.getName()]

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    currentRepository.put('blob_store', repoAttributes['storage']['blobStoreName'])
    if (repoAttributes['storage']['strictContentTypeValidation'] == null ) {
        currentRepository.put('strict_content_validation', true)
    }
    else {
        currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])
    }

    if (rtRepository.getType().getValue() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
        currentRepository.put('layout_policy', repoAttributes['yum']['layoutPolicy'])
        currentRepository.put('repodata_depth', repoAttributes['yum']['repodataDepth'])
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            currentRepository.put('use_nexus_truststore', repoAttributes?.httpclient?.connection?.useTrustStore)
        }
        currentRepository.put('blocked', repoAttributes?.httpclient?.blocked)
        currentRepository.put('auto_blocking_enabled', repoAttributes?.httpclient?.autoBlock)
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        if (repoAttributes?.httpclient?.authentication?.username != null) {
            currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
            currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'group') {
        currentRepository.put('member_repos', repoAttributes['group']['memberNames'])
    }

    switch(rtRepository.getType().getValue()) {
        case 'hosted':
            migrationRepositories['nexus_repos_yum_hosted'].add(currentRepository)
            break
        case 'proxy':
            migrationRepositories['nexus_repos_yum_proxy'].add(currentRepository)
            break
        case 'group':
            migrationRepositories['nexus_repos_yum_group'].add(currentRepository)
            break
    }
}

def migrateNugetRepository(rtRepository, migrationRepositories) {
    Map<String, String> currentRepository = [name: rtRepository.getName()]

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    currentRepository.put('blob_store', repoAttributes['storage']['blobStoreName'])
    if (repoAttributes['storage']['strictContentTypeValidation'] == null ) {
        currentRepository.put('strict_content_validation', true)
    }
    else {
        currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])
    }

    if (rtRepository.getType().getValue() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            currentRepository.put('use_nexus_truststore', repoAttributes?.httpclient?.connection?.useTrustStore)
        }
        currentRepository.put('blocked', repoAttributes?.httpclient?.blocked)
        currentRepository.put('auto_blocking_enabled', repoAttributes?.httpclient?.autoBlock)
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        if (repoAttributes?.httpclient?.authentication?.username != null) {
            currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
            currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'group') {
        currentRepository.put('member_repos', repoAttributes['group']['memberNames'])
    }

    switch(rtRepository.getType().getValue()) {
        case 'hosted':
            migrationRepositories['nexus_repos_nuget_hosted'].add(currentRepository)
            break
        case 'proxy':
            migrationRepositories['nexus_repos_nuget_proxy'].add(currentRepository)
            break
        case 'group':
            migrationRepositories['nexus_repos_nuget_group'].add(currentRepository)
            break
    }
}

def migrateGoRepository(rtRepository, migrationRepositories) {
    Map<String, String> currentRepository = [name: rtRepository.getName()]

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    currentRepository.put('blob_store', repoAttributes['storage']['blobStoreName'])
    if (repoAttributes['storage']['strictContentTypeValidation'] == null ) {
        currentRepository.put('strict_content_validation', true)
    }
    else {
        currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])
    }

    if (rtRepository.getType().getValue() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            currentRepository.put('use_nexus_truststore', repoAttributes?.httpclient?.connection?.useTrustStore)
        }
        currentRepository.put('blocked', repoAttributes?.httpclient?.blocked)
        currentRepository.put('auto_blocking_enabled', repoAttributes?.httpclient?.autoBlock)
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        if (repoAttributes?.httpclient?.authentication?.username != null) {
            currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
            currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
        }
        if (repoAttributes?.cleanup?.policyName != null) {
            currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
        }
    }

    if (rtRepository.getType().getValue() == 'group') {
        currentRepository.put('member_repos', repoAttributes['group']['memberNames'])
    }

    switch(rtRepository.getType().getValue()) {
        case 'proxy':
            migrationRepositories['nexus_repos_go_proxy'].add(currentRepository)
            break
        case 'group':
            migrationRepositories['nexus_repos_go_group'].add(currentRepository)
            break
    }
}

def migrateAptRepository(rtRepository, migrationRepositories) {
    Map<String, String> currentRepository = [name: rtRepository.getName()]

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    currentRepository.put('distribution', repoAttributes['apt']['distribution'])
    currentRepository.put('blob_store', repoAttributes['storage']['blobStoreName'])
    if (repoAttributes?.cleanup?.policyName != null) {
        currentRepository.put('cleanup_policies', repoAttributes?.cleanup?.policyName)
    }

    if (repoAttributes['storage']['strictContentTypeValidation'] == null ) {
        currentRepository.put('strict_content_validation', true)
    }
    else {
        currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])
    }

    if (rtRepository.getType().getValue() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
        currentRepository.put('keypair', repoAttributes['aptSigning']['keypair'])
        currentRepository.put('passphrase', repoAttributes['aptSigning']['passphrase'])
    }

    if (rtRepository.getType().getValue() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        if (repoAttributes?.httpclient?.connection?.useTrustStore != null) {
            currentRepository.put('use_nexus_truststore', repoAttributes?.httpclient?.connection?.useTrustStore)
        }
        currentRepository.put('blocked', repoAttributes?.httpclient?.blocked)
        currentRepository.put('auto_blocking_enabled', repoAttributes?.httpclient?.autoBlock)
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        if (repoAttributes?.httpclient?.authentication?.username != null) {
            currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
            currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
        }
        currentRepository.put('flat', repoAttributes['apt']['flat'])
    }

    switch(rtRepository.getType().getValue()) {
        case 'hosted':
            migrationRepositories['nexus_repos_apt_hosted'].add(currentRepository)
            break
        case 'proxy':
            migrationRepositories['nexus_repos_apt_proxy'].add(currentRepository)
            break
    }
}

repositoryManager.browse().each { rtRepo ->
    switch(rtRepo.getFormat().getValue()) {
        case 'docker':
            migrateDockerRepository(rtRepo, migrationRepositories)
            break
        case 'raw':
            migrateRawRepository(rtRepo, migrationRepositories)
            break
        case 'helm':
            migrateHelmRepository(rtRepo, migrationRepositories)
            break
        case 'maven2':
            migrateMavenRepository(rtRepo, migrationRepositories)
            break
        case 'npm':
            migrateNpmRepository(rtRepo, migrationRepositories)
            break
        case 'yum':
            migrateYumRepository(rtRepo, migrationRepositories)
            break
        case 'nuget':
            migrateNugetRepository(rtRepo, migrationRepositories)
            break
        case 'go':
            migrateGoRepository(rtRepo, migrationRepositories)
            break
        case 'apt':
            migrateAptRepository(rtRepo, migrationRepositories)
            break
    }
}

if (migrationRepositories['nexus_repos_docker_hosted'].size() > 0 || migrationRepositories['nexus_repos_docker_proxy'].size() || migrationRepositories['nexus_repos_docker_group'].size()) {
    result = ['nexus_config_docker': true]
    if (migrationRepositories['nexus_repos_docker_hosted'].size() > 0) {
        result.put('nexus_repos_docker_hosted', migrationRepositories['nexus_repos_docker_hosted'])
    }
    if (migrationRepositories['nexus_repos_docker_proxy'].size() > 0) {
        result.put('nexus_repos_docker_proxy', migrationRepositories['nexus_repos_docker_proxy'])
    }
    if (migrationRepositories['nexus_repos_docker_group'].size() > 0) {
        result.put('nexus_repos_docker_group', migrationRepositories['nexus_repos_docker_group'])
    }
    scriptResults['action_details'].put('repositories/docker.yml', result)
}

if (migrationRepositories['nexus_repos_raw_proxy'].size() > 0 || migrationRepositories['nexus_repos_raw_hosted'].size() || migrationRepositories['nexus_repos_raw_group'].size()) {
    result = ['nexus_config_raw': true]
    if (migrationRepositories['nexus_repos_raw_proxy'].size() > 0) {
        result.put('nexus_repos_raw_proxy', migrationRepositories['nexus_repos_raw_proxy'])
    }
    if (migrationRepositories['nexus_repos_raw_hosted'].size() > 0) {
        result.put('nexus_repos_raw_hosted', migrationRepositories['nexus_repos_raw_hosted'])
    }
    if (migrationRepositories['nexus_repos_raw_group'].size() > 0) {
        result.put('nexus_repos_raw_group', migrationRepositories['nexus_repos_raw_group'])
    }
    scriptResults['action_details'].put('repositories/raw.yml', result)
}

if (migrationRepositories['nexus_repos_maven_proxy'].size() > 0 || migrationRepositories['nexus_repos_maven_hosted'].size() || migrationRepositories['nexus_repos_maven_group'].size()) {
    result = ['nexus_config_maven': true]
    if (migrationRepositories['nexus_repos_maven_proxy'].size() > 0) {
        result.put('nexus_repos_maven_proxy', migrationRepositories['nexus_repos_maven_proxy'])
    }
    if (migrationRepositories['nexus_repos_maven_hosted'].size() > 0) {
        result.put('nexus_repos_maven_hosted', migrationRepositories['nexus_repos_maven_hosted'])
    }
    if (migrationRepositories['nexus_repos_maven_group'].size() > 0) {
        result.put('nexus_repos_maven_group', migrationRepositories['nexus_repos_maven_group'])
    }
    scriptResults['action_details'].put('repositories/maven.yml', result)
}

if (migrationRepositories['nexus_repos_npm_proxy'].size() > 0 || migrationRepositories['nexus_repos_npm_hosted'].size() || migrationRepositories['nexus_repos_npm_group'].size()) {
    result = ['nexus_config_npm': true]
    if (migrationRepositories['nexus_repos_npm_proxy'].size() > 0) {
        result.put('nexus_repos_npm_proxy', migrationRepositories['nexus_repos_npm_proxy'])
    }
    if (migrationRepositories['nexus_repos_npm_hosted'].size() > 0) {
        result.put('nexus_repos_npm_hosted', migrationRepositories['nexus_repos_npm_hosted'])
    }
    if (migrationRepositories['nexus_repos_npm_group'].size() > 0) {
        result.put('nexus_repos_npm_group', migrationRepositories['nexus_repos_npm_group'])
    }
    scriptResults['action_details'].put('repositories/npm.yml', result)
}

if (migrationRepositories['nexus_repos_yum_proxy'].size() > 0 || migrationRepositories['nexus_repos_yum_hosted'].size() || migrationRepositories['nexus_repos_yum_group'].size()) {
    result = ['nexus_config_yum': true]
    if (migrationRepositories['nexus_repos_yum_proxy'].size() > 0) {
        result.put('nexus_repos_yum_proxy', migrationRepositories['nexus_repos_yum_proxy'])
    }
    if (migrationRepositories['nexus_repos_yum_hosted'].size() > 0) {
        result.put('nexus_repos_yum_hosted', migrationRepositories['nexus_repos_yum_hosted'])
    }
    if (migrationRepositories['nexus_repos_yum_group'].size() > 0) {
        result.put('nexus_repos_yum_group', migrationRepositories['nexus_repos_yum_group'])
    }
    scriptResults['action_details'].put('repositories/yum.yml', result)
}

if (migrationRepositories['nexus_repos_helm_hosted'].size() > 0 || migrationRepositories['nexus_repos_helm_proxy'].size()) {
    result = ['nexus_config_helm': true]
    if (migrationRepositories['nexus_repos_helm_hosted'].size() > 0) {
        result.put('nexus_repos_helm_hosted', migrationRepositories['nexus_repos_helm_hosted'])
    }
    if (migrationRepositories['nexus_repos_helm_proxy'].size() > 0) {
        result.put('nexus_repos_helm_proxy', migrationRepositories['nexus_repos_helm_proxy'])
    }
    scriptResults['action_details'].put('repositories/helm.yml', result)
}

if (migrationRepositories['nexus_repos_nuget_proxy'].size() > 0 || migrationRepositories['nexus_repos_nuget_hosted'].size() || migrationRepositories['nexus_repos_nuget_group'].size()) {
    result = ['nexus_config_nuget': true]
    if (migrationRepositories['nexus_repos_nuget_proxy'].size() > 0) {
        result.put('nexus_repos_nuget_proxy', migrationRepositories['nexus_repos_nuget_proxy'])
    }
    if (migrationRepositories['nexus_repos_nuget_hosted'].size() > 0) {
        result.put('nexus_repos_nuget_hosted', migrationRepositories['nexus_repos_nuget_hosted'])
    }
    if (migrationRepositories['nexus_repos_nuget_group'].size() > 0) {
        result.put('nexus_repos_nuget_group', migrationRepositories['nexus_repos_nuget_group'])
    }
    scriptResults['action_details'].put('repositories/nuget.yml', result)
}

if (migrationRepositories['nexus_repos_go_proxy'].size() > 0 || migrationRepositories['nexus_repos_go_group'].size()) {
    result = ['nexus_config_go': true]
    if (migrationRepositories['nexus_repos_go_proxy'].size() > 0) {
        result.put('nexus_repos_go_proxy', migrationRepositories['nexus_repos_go_proxy'])
    }
    if (migrationRepositories['nexus_repos_go_group'].size() > 0) {
        result.put('nexus_repos_go_group', migrationRepositories['nexus_repos_go_group'])
    }
    scriptResults['action_details'].put('repositories/go.yml', result)
}

if (migrationRepositories['nexus_repos_apt_hosted'].size() > 0 || migrationRepositories['nexus_repos_apt_proxy'].size()) {
    result = ['nexus_config_apt': true]
    if (migrationRepositories['nexus_repos_apt_hosted'].size() > 0) {
        result.put('nexus_repos_apt_hosted', migrationRepositories['nexus_repos_apt_hosted'])
    }
    if (migrationRepositories['nexus_repos_apt_proxy'].size() > 0) {
        result.put('nexus_repos_apt_proxy', migrationRepositories['nexus_repos_apt_proxy'])
    }
    scriptResults['action_details'].put('repositories/apt.yml', result)
}

return JsonOutput.toJson(scriptResults)
