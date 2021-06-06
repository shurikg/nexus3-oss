import groovy.json.JsonOutput
import org.sonatype.nexus.security.realm.RealmManager

def fileName = "chi/security.yml"
Map scriptResults = [changed: false, error: false, 'action_details': [:]]

realmManager = container.lookup(RealmManager.class.getName())

def content = [:]

content.put('nexus_nuget_api_key_realm', realmManager.isRealmEnabled("NuGetApiKey"))
content.put('nexus_npm_bearer_token_realm', realmManager.isRealmEnabled("NuGetApiKey"))
content.put('nexus_rut_auth_realm', realmManager.isRealmEnabled("rutauth-realm"))
content.put('nexus_ldap_realm', realmManager.isRealmEnabled("LdapRealm"))
content.put('nexus_docker_bearer_token_realm', realmManager.isRealmEnabled("DockerToken"))

scriptResults['action_details'].put(fileName, content)

return JsonOutput.toJson(scriptResults)
