import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field

import org.sonatype.nexus.security.authz.AuthorizationManager
import org.sonatype.nexus.security.role.Role
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.role.NoSuchRoleException
import org.sonatype.nexus.common.atlas.SystemInformationGenerator

List<Map<String, String>> actionDetails = []

@Field Map scriptResults = [changed: false, error: false, runner: 'general']
scriptResults.put('action_details', actionDetails)

def systemInfo = container.lookup(SystemInformationGenerator.class.getName())

def currentDetails = systemInfo.report()

parsed_args = new JsonSlurper().parseText(args)

def addChange(gitChange, runtimeChange, description) {
    Map<String, String> currentResult = [:]
    currentResult.put('change_in_git', gitChange)
    currentResult.put('change_in_runtime', runtimeChange)
    currentResult.put('change_type', 'change')
    currentResult.put('description', description)
    currentResult.put('resource', 'general')
    currentResult.put('downtime', true)
    scriptResults['action_details'].add(currentResult)
}

def compareNexusVersion(currentVersion, requireVersion) {
    if (currentVersion != requireVersion) {
        addChange(requireVersion, currentVersion, 'nexus version will be change')
    }
}

def compareNexusPort(currentPort, requirePort) {
    if (currentPort.toInteger() != requirePort.toInteger()) {
        addChange(requirePort, currentPort, 'nexus port will be change')
    }
}

def compareContextPath(currentContextPath, requireContextPath) {
    if (currentContextPath != requireContextPath) {
        addChange(requireContextPath, currentContextPath, 'nexus context path will be change')
    }
}

def compareTimezone(currentTimezone, requireTimezone) {
    if (currentTimezone != requireTimezone) {
        addChange(requireTimezone, currentTimezone, 'nexus timezone will be change')
    }
}

def compareInstallationDir(currentDir, requireDir) {
    if (currentDir != requireDir) {
        addChange(requireDir, currentDir, 'nexus installation dir will be change')
    }
}

def compareTempDir(currentDir, requireDir) {
    if (currentDir != requireDir) {
        addChange(requireDir, currentDir, 'nexus temp dir will be change')
    }
}

def compareUserName(currentUserName, requireUserName) {
    if (currentUserName != requireUserName) {
        addChange(requireUserName, currentUserName, 'nexus user name will be change')
    }
}

def compareUserHome(currentUserHome, requireUserHome) {
    if (currentUserHome != requireUserHome) {
        addChange(requireUserHome, currentUserHome, 'nexus user home will be change')
    }
}

def compareMinHeapSize(currentSize, requireSize) {
    if (currentSize != requireSize) {
        addChange(requireSize, currentSize, 'nexus min memory heap size will be change')
    }
}

def compareMaxHeapSize(currentSize, requireSize) {
    if (currentSize != requireSize) {
        addChange(requireSize, currentSize, 'nexus max memory heap size will be change')
    }
}

def compareMaxDirectSize(currentSize, requireSize) {
    if (currentSize != requireSize) {
        addChange(requireSize, currentSize, 'nexus max direct memory size will be change')
    }
}

def compareJettyHttpsEnable(currentJetty, requireJetty) {
    def currentJettyEnable = currentJetty.contains('jetty-https.xml')
    if (currentJettyEnable != Boolean.valueOf(requireJetty)) {
        addChange(requireJetty, currentJettyEnable, 'jetty https configuration will be change')
    }
    if (currentJettyEnable && Boolean.valueOf(requireJetty)) {
        return true
    }
    return false
}

def compareJettyKeyStorePassword(currentPassword, requirePassword) {
    if (currentPassword != requirePassword) {
        addChange(requirePassword, currentPassword, 'keystore password will be change')
    }
}

def compareJettyKeyManagerPassword(currentPassword, requirePassword) {
    if (currentPassword != requirePassword) {
        addChange(requirePassword, currentPassword, 'key manager password will be change')
    }
}

def compareJettyTrustStorePassword(currentPassword, requirePassword) {
    if (currentPassword != requirePassword) {
        addChange(requirePassword, currentPassword, 'trust store password will be change')
    }
}

def compareJettyKeyStoreFile(currentCheckSum, requireCheckSum) {
    if (currentCheckSum != requireCheckSum) {
        addChange(requireCheckSum, currentCheckSum, 'keystore file will be change')
    }
}

// def compareAnonymousAccess(requireAccess) {

//     security.setAnonymousAccess(Boolean.valueOf(parsed_args.anonymous_access))

//     if (currentSize != requireSize) {
//         addChange(requireSize, currentSize, "nexus max direct memory size will be change")
//     }
// }

//  ----- M A I N -------
compareNexusVersion(currentDetails['nexus-status']['version'], parsed_args['require']['nexus_version'])
compareNexusPort(currentDetails['nexus-properties']['application-port'], parsed_args['require']['nexus_default_port'])
compareContextPath(currentDetails['nexus-properties']['nexus-context-path'], parsed_args['require']['nexus_default_context_path'])
compareTimezone(currentDetails['nexus-properties']['user.timezone'], parsed_args['require']['nexus_timezone'])
compareInstallationDir(currentDetails['nexus-configuration']['installDirectory'], parsed_args['require']['nexus_installation_dir'] + '/nexus-' + parsed_args['require']['nexus_version'])
compareTempDir(currentDetails['nexus-configuration']['temporaryDirectory'], parsed_args['require']['nexus_tmp_dir'])
compareUserName(currentDetails['nexus-properties']['user.name'], parsed_args['require']['nexus_os_user'])
compareUserHome(currentDetails['nexus-properties']['user.home'], parsed_args['require']['nexus_os_user_home_dir'])
compareMinHeapSize(parsed_args['current']['nexus_min_heap_size'], parsed_args['require']['nexus_min_heap_size'])
compareMaxHeapSize(parsed_args['current']['nexus_max_heap_size'], parsed_args['require']['nexus_max_heap_size'])
compareMaxDirectSize(parsed_args['current']['nexus_max_direct_memory'], parsed_args['require']['nexus_max_direct_memory'])

def isJettyCompareRequire = compareJettyHttpsEnable(currentDetails['nexus-properties']['nexus-args'], parsed_args['require']['jetty_https_enable'])
if ( isJettyCompareRequire ) {
    compareJettyKeyStorePassword(parsed_args['current']['jetty_keystore_password'], parsed_args['require']['jetty_keystore_password'])
    compareJettyKeyManagerPassword(parsed_args['current']['jetty_keymanager_password'], parsed_args['require']['jetty_keymanager_password'])
    compareJettyTrustStorePassword(parsed_args['current']['jetty_truststore_password'], parsed_args['require']['jetty_truststore_password'])
    compareJettyKeyStoreFile(parsed_args['current']['keystore_checksum'], parsed_args['require']['keystore_checksum'])
    compareNexusPort(currentDetails['nexus-properties']['application-port-ssl'], parsed_args['require']['nexus_default_ssl_port'])
}

return JsonOutput.toJson(scriptResults)

//   "nexus-configuration" : {
//     "workingDirectory" : "/users/kube/certification_nexus_data",
//   }
// nexus_installation_dir: /users/kube/certification_nexus
// nexus_data_dir: /users/kube/certification_nexus_data

// --- httpd ----
// httpd_config_dir
// certificate_file_dest
// httpd_copy_ssl_files
// httpd_ssl_certificate_file
// httpd_ssl_certificate_chain_file
// httpd_ssl_certificate_key_file
// certificate_key_dest
// httpd_server_name
// httpd_default_admin_email
// httpd_ssl_cert_file_location
// httpd_ssl_cert_key_location
// httpd_ssl_cert_chain_file_location is defined
// httpd_package_name
// nexus_public_hostname
// nexus_config_npm

// ----------------------------
// nexus_download_url
// nexus_package
// nexus_download_dir
// nexus_os_group
// nexus_data_dir
// nexus_default_settings_file
// nexus_application_host
// nexus_os_max_filedescriptors

// - include_tasks: call_script.yml
//   vars:
//     script_name: setup_base_url
//     args:
//       base_url: "{{ nexus_public_scheme }}://{{ nexus_public_hostname }}/"
