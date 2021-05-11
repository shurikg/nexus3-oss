import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.sonatype.nexus.email.EmailManager

parsed_args = new JsonSlurper().parseText(args)

def emailMgr = container.lookup(EmailManager.class.getName())

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false, runner: 'email']
scriptResults.put('action_details', actionDetails)

def config = emailMgr.getConfiguration()

def gitChangeMessage = []
def runtimeChangeMessage = []

if (config.isEnabled() != parsed_args.email_server_enabled) {
        gitChangeMessage.add("enable = ${parsed_args.email_server_enabled}")
        runtimeChangeMessage.add("enable = ${config.isEnabled()}")
}

if (config.getHost() != parsed_args.email_server_host) {
        gitChangeMessage.add("host = ${parsed_args.email_server_host}")
        runtimeChangeMessage.add("host = ${config.getHost()}")
}

def port = Integer.valueOf(parsed_args.email_server_port)
if (config.getPort() != port) {
        gitChangeMessage.add("port = ${port}")
        runtimeChangeMessage.add("port = ${config.getPort()}")
}

if (config.getUsername() != parsed_args.email_server_username) {
        gitChangeMessage.add("email user name = ${parsed_args.email_server_username}")
        runtimeChangeMessage.add("email user name = ${config.getUsername()}")
}

if (config.getPassword() != parsed_args.email_server_password) {
        gitChangeMessage.add("email server password = ${parsed_args.email_server_password}")
        runtimeChangeMessage.add("email server password = ${config.getPassword()}")
}

if (config.getFromAddress() != parsed_args.email_from_address) {
        gitChangeMessage.add("from address = ${parsed_args.email_from_address}")
        runtimeChangeMessage.add("from address = ${config.getFromAddress()}")
}

if (config.getSubjectPrefix() != parsed_args.email_subject_prefix) {
        gitChangeMessage.add("subject prefix = ${parsed_args.email_subject_prefix}")
        runtimeChangeMessage.add("subject prefix = ${config.getSubjectPrefix()}")
}

if (config.isStartTlsEnabled() != parsed_args.email_tls_enabled) {
        gitChangeMessage.add("tls enabled = ${parsed_args.email_tls_enabled}")
        runtimeChangeMessage.add("tls enabled = ${config.isStartTlsEnabled()}")
}

if (config.isStartTlsRequired() != parsed_args.email_tls_required) {
        gitChangeMessage.add("tls required = ${parsed_args.email_tls_required}")
        runtimeChangeMessage.add("tls required = ${config.isStartTlsRequired()}")
}

if (config.isSslOnConnectEnabled() != parsed_args.email_ssl_on_connect_enabled) {
        gitChangeMessage.add("ssl enabled = ${parsed_args.email_ssl_on_connect_enabled}")
        runtimeChangeMessage.add("ssl enabled = ${config.isSslOnConnectEnabled()}")
}

if (config.isSslCheckServerIdentityEnabled() != parsed_args.email_ssl_check_server_identity_enabled) {
        gitChangeMessage.add("ssl check server identity = ${parsed_args.email_ssl_check_server_identity_enabled}")
        runtimeChangeMessage.add("ssl check server identity = ${config.isSslCheckServerIdentityEnabled()}")
}

if (config.isNexusTrustStoreEnabled() != parsed_args.email_trust_store_enabled) {
        gitChangeMessage.add("trust store = ${parsed_args.email_trust_store_enabled}")
        runtimeChangeMessage.add("trust store = ${config.isNexusTrustStoreEnabled()}")
}

if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the email configuration will be update")
        currentResult.put('resource', 'email')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
}

return JsonOutput.toJson(scriptResults)
