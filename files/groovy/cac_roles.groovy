import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.security.authz.AuthorizationManager
import org.sonatype.nexus.security.role.Role
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.role.NoSuchRoleException

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false, runner: 'roles']
scriptResults.put('action_details', actionDetails)

parsed_args = new JsonSlurper().parseText(args)

AuthorizationManager authManager = security.securitySystem.getAuthorizationManager(UserManager.DEFAULT_SOURCE)

parsed_args.details.each { roleDef ->

    Map<String, String> currentResult = [:]

    privileges = (roleDef.privileges == null ? new HashSet() : roleDef.privileges.toSet())
    roles = (roleDef.roles == null ? new HashSet() : roleDef.roles.toSet())
    def gitChangeMessage = []
    def runtimeChangeMessage = []

    try {
        Role existingRole = authManager.getRole(roleDef.id)

        if (existingRole.getName() != roleDef.name) {
            gitChangeMessage.add("name = ${roleDef.name}")
            runtimeChangeMessage.add("name = ${existingRole.getName()}")
        }
        if (existingRole.getDescription() != roleDef.description) {
            gitChangeMessage.add("description = ${roleDef.description}")
            runtimeChangeMessage.add("description = ${existingRole.getDescription()}")
        }

        def existingPrivileges = existingRole.getPrivileges()
        privileges.each { privilege ->
            if (! (privilege in existingPrivileges) ) {
                gitChangeMessage.add("add ${privilege} privilege to role")
                runtimeChangeMessage.add("N/A")
            }
        }

        existingPrivileges.each { privilege ->
            if (! (privilege in privileges) ) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete ${privilege} privilege from role")
            }
        }

        def existingRolesInRole = existingRole.getRoles()
        roles.each { role ->
            if (! (role in existingRolesInRole) ) {
                gitChangeMessage.add("add ${role} role to role")
                runtimeChangeMessage.add("N/A")
            }
        }

        existingRolesInRole.each { role ->
            if (! (role in roles) ) {
                gitChangeMessage.add("N/A")
                runtimeChangeMessage.add("delete ${role} role from role")
            }
        }

        if (gitChangeMessage) {
            currentResult.put('change_in_git', gitChangeMessage.join('\n'))
            currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
            currentResult.put('change_type', 'change')
            currentResult.put('description', "the ${roleDef.id} role will be update")
            currentResult.put('resource', 'role')
            currentResult.put('downtime', false)
            scriptResults['action_details'].add(currentResult)
        }
    } catch (NoSuchRoleException ignored) {
            currentResult.put('change_in_git', "definition of new ${roleDef.id} role")
            currentResult.put('change_in_runtime', 'N/A')
            currentResult.put('change_type', 'add')
            currentResult.put('description', "the ${roleDef.id} role will be added")
            currentResult.put('resource', 'role')
            currentResult.put('downtime', false)
            scriptResults['action_details'].add(currentResult)
    }
}

def excludeRoles = ['nx-admin', 'nx-anonymous']

authManager.listRoles().each { rtRole ->
    if (! (rtRole.getRoleId() in excludeRoles) ) {
        def needToDelete = true
        Map<String, String> currentResult = [:]

        parsed_args.details.any { roleDef ->
            if (rtRole.getRoleId() == roleDef.id) {
                needToDelete = false
                return true
            }
        }
        if (needToDelete){
            currentResult.put('change_in_git', 'N/A')
            currentResult.put('change_in_runtime', "${rtRole.getRoleId()} role exist")
            currentResult.put('change_type', 'delete')
            currentResult.put('description', "the ${rtRole.getRoleId()} role will be deleted")
            currentResult.put('resource', 'role')
            currentResult.put('downtime', false)

            scriptResults['action_details'].add(currentResult)

            if (! parsed_args.dry_run) {
                authManager.deleteRole(rtRole.getRoleId())
            }
        }
    }
}

return JsonOutput.toJson(scriptResults)
