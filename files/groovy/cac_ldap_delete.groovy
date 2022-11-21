import groovy.json.JsonOutput
import org.sonatype.nexus.ldap.persist.LdapConfigurationManager
import org.sonatype.nexus.ldap.persist.entity.Connection
import org.sonatype.nexus.ldap.persist.entity.Mapping
import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)
Map<String, String> currentResult = [:]

def ldapConfigMgr = container.lookup(LdapConfigurationManager.class.getName())

ldapConfigMgr.listLdapServerConfigurations().each { rtLdap ->
    boolean needToDelete = true
    parsed_args.details.any { ldapDef ->
        if (rtLdap.name == ldapDef.ldap_name) {
            needToDelete = false
            return true
        }
    }
    if (needToDelete){
        currentResult.put('change_in_git', "N/A")
        currentResult.put('change_in_runtime', "${rtLdap.name}")
        currentResult.put('change_type', 'delete')
        currentResult.put('description', "the ${rtLdap.name} ldap will be deleted")
        currentResult.put('resource', 'ldap')
        currentResult.put('downtime', false)

        scriptResults['action_details'].add(currentResult)

        if (! parsed_args.dry_run) {
            ldapConfigMgr.deleteLdapServerConfiguration(rtLdap.getId())
        }
    }
}

return JsonOutput.toJson(scriptResults)