import groovy.json.JsonOutput
import org.sonatype.nexus.security.authz.AuthorizationManager
import org.sonatype.nexus.security.role.Role
import org.sonatype.nexus.security.user.UserManager

def fileName = "roles.yml"
def migrationRoles = ['nexus_roles': []]

Map scriptResults = [changed: false, error: false, 'action_details': [:]]

AuthorizationManager authManager = security.securitySystem.getAuthorizationManager(UserManager.DEFAULT_SOURCE)

def excludeRoles = ['nx-admin', 'nx-anonymous']

authManager.listRoles().each { rtRole ->
    if (! (rtRole.getRoleId() in excludeRoles) ) {
        Map<String, String> currentResult = [:]

        def privilageToAdd = []
        def roleToAdd = []
        currentResult.put('id', rtRole.getRoleId())
        currentResult.put('name', rtRole.getName())
        currentResult.put('description', rtRole.getDescription())

        def existingPrivileges = rtRole.getPrivileges()
        existingPrivileges.each { privilage ->
            privilageToAdd.add(privilage)
        }

        def existingRolesInRole = rtRole.getRoles()
        existingRolesInRole.each { role ->
            roleToAdd.add(role)
        }

        currentResult.put('privileges', privilageToAdd)
        currentResult.put('roles', roleToAdd)

        migrationRoles['nexus_roles'].add(currentResult)
    }
}
scriptResults['action_details'].put(fileName, migrationRoles)

return JsonOutput.toJson(scriptResults)
