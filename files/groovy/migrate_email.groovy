import groovy.json.JsonOutput
import org.sonatype.nexus.email.EmailManager

def emailMgr = container.lookup(EmailManager.class.getName())

Map scriptResults = [changed: false, error: false, 'action_details': [:]]

def content = [:]
def config = emailMgr.getConfiguration()

content.put('nexus_email_server_enabled', config.isEnabled())
content.put('nexus_email_server_host', config.getHost())
content.put('nexus_email_server_port', config.getPort())
content.put('nexus_email_server_username', config.getUsername())
content.put('nexus_email_server_password', config.getPassword())
content.put('nexus_email_from_address', config.getFromAddress())
content.put('nexus_email_subject_prefix', config.getSubjectPrefix())
content.put('nexus_email_tls_enabled', config.isStartTlsEnabled())
content.put('nexus_email_tls_required', config.isStartTlsRequired())
content.put('nexus_email_ssl_on_connect_enabled', config.isSslOnConnectEnabled())
content.put('nexus_email_ssl_check_server_identity_enabled', config.isSslCheckServerIdentityEnabled())
content.put('nexus_email_trust_store_enabled', config.isNexusTrustStoreEnabled())

scriptResults['action_details'].put('email_server.yml', content)
return JsonOutput.toJson(scriptResults)
