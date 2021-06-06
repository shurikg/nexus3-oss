import groovy.json.JsonOutput
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.privilege.Privilege

def fileName = "privileges.yml"
def migrationPrivileges = ['nexus_privileges': []]
Map scriptResults = [changed: false, error: false, 'action_details': [:]]

authManager = security.securitySystem.getAuthorizationManager(UserManager.DEFAULT_SOURCE)


authManager.listPrivileges().each { rtPrivilege ->
    if (! rtPrivilege.isReadOnly()) {

        Map<String, String> currentPrivilege = [:]

        currentPrivilege.put('name', rtPrivilege.getName())
        currentPrivilege.put('description', rtPrivilege.getDescription())
        currentPrivilege.put('type', rtPrivilege.getType())
        if (rtPrivilege.getPrivilegeProperty('format') != null) {
            currentPrivilege.put('format', rtPrivilege.getPrivilegeProperty('format'))
        }
        if (rtPrivilege.getPrivilegeProperty('contentSelector') != null) {
            currentPrivilege.put('contentSelector', rtPrivilege.getPrivilegeProperty('contentSelector'))
        }
        if (rtPrivilege.getPrivilegeProperty('repository') != null) {
            currentPrivilege.put('repository', rtPrivilege.getPrivilegeProperty('repository'))
        }
        if (rtPrivilege.getPrivilegeProperty('pattern') != null) {
            currentPrivilege.put('pattern', rtPrivilege.getPrivilegeProperty('pattern'))
        }
        if (rtPrivilege.getPrivilegeProperty('domain') != null) {
            currentPrivilege.put('domain', rtPrivilege.getPrivilegeProperty('domain'))
        }
        if (rtPrivilege.getPrivilegeProperty('name') != null) {
            currentPrivilege.put('script_name', rtPrivilege.getPrivilegeProperty('name'))
        }
        if (rtPrivilege.getPrivilegeProperty('actions') != null) {
            currentPrivilege.put('actions', rtPrivilege.getPrivilegeProperty('actions'))
        }

        migrationPrivileges['nexus_privileges'].add(currentPrivilege)
    }
}
scriptResults['action_details'].put(fileName, migrationPrivileges)

return JsonOutput.toJson(scriptResults)
