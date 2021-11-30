import groovy.json.JsonOutput
import org.sonatype.nexus.security.realm.RealmManager
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration
import org.sonatype.nexus.security.anonymous.AnonymousManager

def fileName = "chi/security.yml"
Map scriptResults = [changed: false, error: false, 'action_details': [:]]

realmManager = container.lookup(RealmManager.class.getName())

def content = [:]

content.put('nexus_admin_password', "xxxx")

content.put('nexus_nuget_api_key_realm', realmManager.isRealmEnabled("NuGetApiKey"))
content.put('nexus_npm_bearer_token_realm', realmManager.isRealmEnabled("NuGetApiKey"))
content.put('nexus_rut_auth_realm', realmManager.isRealmEnabled("rutauth-realm"))
content.put('nexus_ldap_realm', realmManager.isRealmEnabled("LdapRealm"))
content.put('nexus_docker_bearer_token_realm', realmManager.isRealmEnabled("DockerToken"))

content.put('nexus_crowd_realm', realmManager.isRealmEnabled("Crowd"))
content.put('nexus_default_role_realm', realmManager.isRealmEnabled("DefaultRole"))
content.put('nexus_local_authenticating_realm', realmManager.isRealmEnabled("NexusAuthenticatingRealm"))
content.put('nexus_local_authorizing_realm', realmManager.isRealmEnabled("NexusAuthorizingRealm"))
content.put('nexus_saml_realm', realmManager.isRealmEnabled("SamlRealm"))
content.put('nexus_user_token_realm', realmManager.isRealmEnabled("User-Token-Realm"))

def anonymousManager = container.lookup(AnonymousManager.class.getName())
content.put('nexus_anonymous_access', anonymousManager.isEnabled())

scriptResults['action_details'].put(fileName, content)

return JsonOutput.toJson(scriptResults)