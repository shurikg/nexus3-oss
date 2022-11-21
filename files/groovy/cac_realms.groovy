import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import org.sonatype.nexus.security.realm.RealmManager

parsed_args = new JsonSlurper().parseText(args)

Map scriptResults = [changed: false, error: false, runner: 'realm']
scriptResults.put('action_details', [])
Map<String, String> currentResult = [:]

realmManager = container.lookup(RealmManager.class.getName())

def gitChangeMessage = []
def runtimeChangeMessage = []

if (realmManager.isRealmEnabled("NuGetApiKey") != parsed_args.nuget_api_key_realm) {
    gitChangeMessage.add("NuGetApiKey = ${parsed_args.nuget_api_key_realm}")
    runtimeChangeMessage.add("NuGetApiKey = ${realmManager.isRealmEnabled("NuGetApiKey")}")
}
if (realmManager.isRealmEnabled("NpmToken") != parsed_args.npm_bearer_token_realm) {
    gitChangeMessage.add("NpmToken = ${parsed_args.npm_bearer_token_realm}")
    runtimeChangeMessage.add("NpmToken = ${realmManager.isRealmEnabled("NpmToken")}")
}
if (realmManager.isRealmEnabled("rutauth-realm") != parsed_args.rut_auth_realm) {
    gitChangeMessage.add("rutauth-realm = ${parsed_args.rut_auth_realm}")
    runtimeChangeMessage.add("rutauth-realm = ${realmManager.isRealmEnabled("rutauth-realm")}")
}
if (realmManager.isRealmEnabled("LdapRealm") != parsed_args.ldap_realm) {
    gitChangeMessage.add("LdapRealm = ${parsed_args.ldap_realm}")
    runtimeChangeMessage.add("LdapRealm = ${realmManager.isRealmEnabled("LdapRealm")}")
}
if (realmManager.isRealmEnabled("DockerToken") != parsed_args.docker_bearer_token_realm) {
    gitChangeMessage.add("DockerToken = ${parsed_args.docker_bearer_token_realm}")
    runtimeChangeMessage.add("DockerToken = ${realmManager.isRealmEnabled("DockerToken")}")
}
if (realmManager.isRealmEnabled("Crowd") != parsed_args.crowd_realm) {
    gitChangeMessage.add("Crowd = ${parsed_args.crowd_realm}")
    runtimeChangeMessage.add("Crowd = ${realmManager.isRealmEnabled("Crowd")}")
}
if (realmManager.isRealmEnabled("DefaultRole") != parsed_args.default_role_realm) {
    gitChangeMessage.add("DefaultRole = ${parsed_args.default_role_realm}")
    runtimeChangeMessage.add("DefaultRole = ${realmManager.isRealmEnabled("DefaultRole")}")
}
if (realmManager.isRealmEnabled("NexusAuthenticatingRealm") != parsed_args.local_authenticating_realm) {
    gitChangeMessage.add("NexusAuthenticatingRealm = ${parsed_args.local_authenticating_realm}")
    runtimeChangeMessage.add("NexusAuthenticatingRealm = ${realmManager.isRealmEnabled("NexusAuthenticatingRealm")}")
}
if (realmManager.isRealmEnabled("NexusAuthorizingRealm") != parsed_args.local_authorizing_realm) {
    gitChangeMessage.add("NexusAuthorizingRealm = ${parsed_args.local_authorizing_realm}")
    runtimeChangeMessage.add("NexusAuthorizingRealm = ${realmManager.isRealmEnabled("NexusAuthorizingRealm")}")
}
if (realmManager.isRealmEnabled("SamlRealm") != parsed_args.saml_realm) {
    gitChangeMessage.add("SamlRealm = ${parsed_args.saml_realm}")
    runtimeChangeMessage.add("SamlRealm = ${realmManager.isRealmEnabled("SamlRealm")}")
}
if (realmManager.isRealmEnabled("User-Token-Realm") != parsed_args.user_token_realm) {
    gitChangeMessage.add("User-Token-Realm = ${parsed_args.user_token_realm}")
    runtimeChangeMessage.add("User-Token-Realm = ${realmManager.isRealmEnabled("User-Token-Realm")}")
}

if (gitChangeMessage) {
    currentResult.put('change_in_git', gitChangeMessage.join('\n'))
    currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
    currentResult.put('change_type', 'change')
    currentResult.put('description', "the realm configuration will be update")
    currentResult.put('resource', 'realm')
    currentResult.put('downtime', false)
    scriptResults['action_details'].add(currentResult)
}

return JsonOutput.toJson(scriptResults)
