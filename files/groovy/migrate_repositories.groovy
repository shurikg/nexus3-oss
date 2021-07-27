import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.Repository

def migrationRepositories = ['nexus_repos_docker_hosted': [], 'nexus_repos_docker_proxy': [], 'nexus_repos_docker_group': [],
                             'nexus_repos_raw_proxy': [], 'nexus_repos_raw_hosted': [], 'nexus_repos_raw_group': [],
                             'nexus_repos_maven_proxy': [], 'nexus_repos_maven_hosted': [], 'nexus_repos_maven_group': [],
                             'nexus_repos_helm_hosted': [], 'nexus_repos_helm_proxy': []]
Map scriptResults = [changed: false, error: false, 'action_details': [:]]

repositoryManager = repository.repositoryManager

def migrateHelmRepository(rtRepository, migrationRepositories) {
    Map<String, String> currentRepository = [name: rtRepository.getName()]

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    currentRepository.put('blob_store', repoAttributes['storage']['blobStoreName'])
    currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])

    if (rtRepository.getType().getValue() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
    }

    if (rtRepository.getType().getValue() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
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
    }

    if (rtRepository.getType().getValue() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        if (repoAttributes?.httpclient?.authentication?.username != null) {
            currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
            currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
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
    }

    if (rtRepository.getType().getValue()  == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
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
    currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])

    if (rtRepository.getType().getValue() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
        currentRepository.put('layout_policy', repoAttributes['maven']['layoutPolicy'])
        currentRepository.put('version_policy', repoAttributes['maven']['versionPolicy'])
    }

    if (rtRepository.getType().getValue() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
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

return JsonOutput.toJson(scriptResults)
