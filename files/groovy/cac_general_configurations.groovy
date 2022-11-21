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
    def currentJettyHttpsEnable = currentJetty.contains('jetty-https.xml')

    if (currentJettyHttpsEnable && Boolean.valueOf(requireJetty)) {
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

def compareSwitchHttpToHttps(currentJetty, requireJettyHttps, requireHttpPort, requireHttpsPort) {
    def currentJettyHttpsEnable = currentJetty.contains('jetty-https.xml')

    if ( ! currentJettyHttpsEnable && requireJettyHttps && requireHttpPort.toString() == requireHttpsPort.toString() ) {
        addChange('jetty https', 'jetty http', 'switch between http to https in jetty configuration')
    }
}

def compareSwitchHttpsToHttp(currentJetty, requireJettyHttps, requireHttpPort, requireHttpsPort) {
    def currentJettyHttpsEnable = currentJetty.contains('jetty-https.xml')

    if ( currentJettyHttpsEnable && ! requireJettyHttps && requireHttpPort.toString() == requireHttpsPort.toString() ) {
        addChange('jetty http', 'jetty https', 'switch between https to http in jetty configuration')
    }
}

def compareAddHttps(currentJetty, requireJettyHttps, requireHttpPort, requireHttpsPort) {
    def currentJettyHttpsEnable = currentJetty.contains('jetty-https.xml')

    if ( ! currentJettyHttpsEnable && requireJettyHttps && requireHttpPort.toString() != requireHttpsPort.toString() ) {
        addChange('jetty https', 'jetty http', 'add jetty https access option')
    }
}

def compareAddHttp(currentJetty, requireJettyHttps, requireHttpPort, requireHttpsPort, currentHttpPort) {
    def currentJettyHttpsEnable = currentJetty.contains('jetty-https.xml')

    if ( currentJettyHttpsEnable && requireJettyHttps && requireHttpPort.toString() != requireHttpsPort.toString() && currentHttpPort.toString() != requireHttpPort.toString() ) {
        addChange('jetty http', 'jetty https', "add jetty http access option")
    }
}

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

compareSwitchHttpToHttps(currentDetails['nexus-properties']['nexus-args'], parsed_args['require']['jetty_https_enable'], parsed_args['require']['nexus_default_port'], parsed_args['require']['nexus_default_ssl_port'])
compareSwitchHttpsToHttp(currentDetails['nexus-properties']['nexus-args'], parsed_args['require']['jetty_https_enable'], parsed_args['require']['nexus_default_port'], parsed_args['require']['nexus_default_ssl_port'])
compareAddHttps(currentDetails['nexus-properties']['nexus-args'], parsed_args['require']['jetty_https_enable'], parsed_args['require']['nexus_default_port'], parsed_args['require']['nexus_default_ssl_port'])
compareAddHttp(currentDetails['nexus-properties']['nexus-args'], parsed_args['require']['jetty_https_enable'], parsed_args['require']['nexus_default_port'], parsed_args['require']['nexus_default_ssl_port'], currentDetails['nexus-properties']['application-port'] )

def isJettyCompareRequire = compareJettyHttpsEnable(currentDetails['nexus-properties']['nexus-args'], parsed_args['require']['jetty_https_enable'])
if ( isJettyCompareRequire ) {
    compareJettyKeyStorePassword(parsed_args['current']['jetty_keystore_password'], parsed_args['require']['jetty_keystore_password'])
    compareJettyKeyManagerPassword(parsed_args['current']['jetty_keymanager_password'], parsed_args['require']['jetty_keymanager_password'])
    compareJettyTrustStorePassword(parsed_args['current']['jetty_truststore_password'], parsed_args['require']['jetty_truststore_password'])
    compareJettyKeyStoreFile(parsed_args['current']['keystore_checksum'], parsed_args['require']['keystore_checksum'])
    compareNexusPort(currentDetails['nexus-properties']['application-port-ssl'], parsed_args['require']['nexus_default_ssl_port'])
}

return JsonOutput.toJson(scriptResults)