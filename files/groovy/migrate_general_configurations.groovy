import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

import org.sonatype.nexus.security.authz.AuthorizationManager
import org.sonatype.nexus.security.role.Role
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.role.NoSuchRoleException
import org.sonatype.nexus.common.atlas.SystemInformationGenerator

@Field Map scriptResults = [changed: false, error: false, 'action_details': [:]]

def systemInfo = container.lookup(SystemInformationGenerator.class.getName())

def currentDetails = systemInfo.report()

def migratePmxFile(currentVersion) {
    scriptResults['action_details'].put('chi/pmx.yml', ['nexus_version': currentVersion])
}

def migrateGenericConfigurationFile(details) {
    def content = [:]

    content.put('nexus_default_port', details['nexus-properties']['application-port'].toInteger())
    content.put('nexus_delete_default_repos', true)
    content.put('nexus_delete_default_blobstore', true)

    content.put('nexus_default_context_path', details['nexus-properties']['nexus-context-path'])
    content.put('nexus_timezone', details['nexus-properties']['user.timezone'])
    content.put('nexus_installation_dir', details['nexus-configuration']['installDirectory'].split('/')[0..-2].join('/'))
    content.put('nexus_data_dir', details['nexus-configuration']['workingDirectory'])
    content.put('nexus_tmp_dir', details['nexus-configuration']['temporaryDirectory'])
    content.put('nexus_os_user', details['nexus-properties']['user.name'])
    content.put('nexus_os_user_home_dir', details['nexus-properties']['user.home'])

    def jettySslEnable = details['nexus-properties']['nexus-args'].contains('jetty-https.xml')
    content.put('jetty_https_setup_enable', jettySslEnable)

    scriptResults['action_details'].put('generic_configuration.yml', content)
}

//  ----- M A I N -------
migratePmxFile(currentDetails['nexus-status']['version'])
migrateGenericConfigurationFile(currentDetails)

return JsonOutput.toJson(scriptResults)
