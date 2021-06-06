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

    scriptResults['action_details'].put('generic_configuration.yml', content)
// nexus_os_group: kube
// # nexus_os_user: kube

// nexus_installation_dir: /users/kube/certification_nexus
// nexus_data_dir: /users/kube/certification_nexus_data
}

// def compareContextPath(currentContextPath, requireContextPath) {
//     if (currentContextPath != requireContextPath) {
//         addChange(requireContextPath, currentContextPath, "nexus context path will be change")
//     }
// }

// def compareTimezone(currentTimezone, requireTimezone) {
//     if (currentTimezone != requireTimezone) {
//         addChange(requireTimezone, currentTimezone, "nexus timezone will be change")
//     }
// }

// def compareInstallationDir(currentDir, requireDir) {
//     if (currentDir != requireDir) {
//         addChange(requireDir, currentDir, "nexus installation dir will be change")
//     }
// }

// def compareTempDir(currentDir, requireDir) {
//     if (currentDir != requireDir) {
//         addChange(requireDir, currentDir, "nexus temp dir will be change")
//     }
// }

// def compareUserName(currentUserName, requireUserName) {
//     if (currentUserName != requireUserName) {
//         addChange(requireUserName, currentUserName, "nexus user name will be change")
//     }
// }

// def compareUserHome(currentUserHome, requireUserHome) {
//     if (currentUserHome != requireUserHome) {
//         addChange(requireUserHome, currentUserHome, "nexus user home will be change")
//     }
// }
//  ----- M A I N -------
migratePmxFile(currentDetails['nexus-status']['version'])
migrateGenericConfigurationFile(currentDetails)
// compareContextPath(currentDetails['nexus-properties']['nexus-context-path'], parsed_args['require']['nexus_default_context_path'])
// compareTimezone(currentDetails['nexus-properties']['user.timezone'], parsed_args['require']['nexus_timezone'])
// compareInstallationDir(currentDetails['nexus-configuration']['installDirectory'], parsed_args['require']['nexus_installation_dir'])
// compareTempDir(currentDetails['nexus-configuration']['temporaryDirectory'], parsed_args['require']['nexus_tmp_dir'])
// compareUserName(currentDetails['nexus-properties']['user.name'], parsed_args['require']['nexus_os_user'])
// compareUserHome(currentDetails['nexus-properties']['user.home'], parsed_args['require']['nexus_os_user_home_dir'])


return JsonOutput.toJson(scriptResults)
