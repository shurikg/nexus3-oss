import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.Repository

def migrationRepositories = ['nexus_repos_docker_hosted': [], 'nexus_repos_docker_proxy': [], 'nexus_repos_docker_group': [],
                             'nexus_repos_raw_proxy': [], 'nexus_repos_raw_hosted': [], 'nexus_repos_raw_group': [],
                             'nexus_repos_helm_hosted': [], 'nexus_repos_helm_proxy': []]
Map scriptResults = [changed: false, error: false, 'action_details': [:]]

repositoryManager = repository.repositoryManager

def migrateHelmRepository(rtRepository, migrationRepositories) {
    Map<String, String> currentRepository = [name: rtRepository.getName()]

    repoAttributes = rtRepository.getConfiguration().getAttributes()

    currentRepository.put('blob_store', repoAttributes['storage']['blobStoreName'])
    currentRepository.put('strict_content_validation', repoAttributes['storage']['strictContentTypeValidation'])

    if (rtRepository.getType() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
    }

    if (rtRepository.getType() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
        currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
    }

    switch(rtRepository.getType()) {
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

    if (rtRepository.getType() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
    }

    if (rtRepository.getType() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
        currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
    }

    if (rtRepository.getType() == 'group') {
        currentRepository.put('member_repos', repoAttributes['group']['memberNames'])
    }

    switch(rtRepository.getType()) {
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
    if (repoAttributes['docker']['httpPort'] != null) {
        currentRepository.put('http_port', repoAttributes['docker']['httpPort'])
    }
    if (repoAttributes['docker']['httpsPort'] != null) {
        currentRepository.put('https_port', repoAttributes['docker']['httpsPort'])
    }
    currentRepository.put('v1_enabled', repoAttributes['docker']['v1Enabled'])
    currentRepository.put('force_basic_auth', repoAttributes['docker']['forceBasicAuth'])

    if (rtRepository.getType() == 'hosted') {
        currentRepository.put('write_policy', repoAttributes['storage']['writePolicy'])
    }

    if (rtRepository.getType() == 'proxy') {
        currentRepository.put('remote_url', repoAttributes['proxy']['remoteUrl'])
        currentRepository.put('maximum_component_age', repoAttributes['proxy']['contentMaxAge'])
        currentRepository.put('maximum_metadata_age', repoAttributes['proxy']['metadataMaxAge'])
        currentRepository.put('negative_cache_enabled', repoAttributes['negativeCache']['enabled'])
        currentRepository.put('negative_cache_ttl', repoAttributes['negativeCache']['timeToLive'])
        currentRepository.put('index_type', repoAttributes['dockerProxy']['indexType'])
        currentRepository.put('use_nexus_certificates_to_access_index', repoAttributes['dockerProxy']['useTrustStoreForIndexAccess'])
        currentRepository.put('foreign_layer_url_whitelist', repoAttributes['dockerProxy']['foreignLayerUrlWhitelist'])
        currentRepository.put('cache_foreign_layers', repoAttributes['dockerProxy']['cacheForeignLayers'])
        currentRepository.put('remote_username', repoAttributes?.httpclient?.authentication?.username)
        currentRepository.put('remote_password', repoAttributes?.httpclient?.authentication?.password)
    }

    if (rtRepository.getType() == 'group') {
        currentRepository.put('member_repos', repoAttributes['group']['memberNames'])
    }

    switch(rtRepository.getType()) {
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

repositoryManager.browse().each { rtRepo ->
    switch(rtRepo.getFormat()) {
        case 'docker':
            migrateDockerRepository(rtRepo, migrationRepositories)
            break
        case 'raw':
            migrateRawRepository(rtRepo, migrationRepositories)
            break
        case 'helm':
            migrateHelmRepository(rtRepo, migrationRepositories)
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
