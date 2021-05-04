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

    // def privileges = (roleDef.privileges == null ? new HashSet() : roleDef.privileges.toSet())
    // roles = (roleDef.roles == null ? new HashSet() : roleDef.roles.toSet())
    def gitChangeMessage = []
    def runtimeChangeMessage = []

    try {
        Role newRole = authManager.getRole(roleDef.id)
        // newRole.setName(roleDef.name)
        // newRole.setDescription(roleDef.description)
        // newRole.setPrivileges(privileges)
        // newRole.setRoles(roles)
        // Role currentRole = authManager.getRole(roleDef.id)
        if (newRole.getName() != roleDef.name) {
            gitChangeMessage.add("name = ${newRole.getName()}")
            runtimeChangeMessage.add("name = ${roleDef.name}")
        }
        if (newRole.getDescription() != roleDef.description) {
            gitChangeMessage.add("description = ${newRole.getDescription()}")
            runtimeChangeMessage.add("description = ${roleDef.description}")
        }

        if (gitChangeMessage) {
            currentResult.put('change_in_git', gitChangeMessage.join(' -- '))
            currentResult.put('change_in_runtime', runtimeChangeMessage.join(' -- '))
            currentResult.put('change_type', 'change')
            currentResult.put('description', "the ${roleDef.id} role will be update")
            currentResult.put('action', '')
            currentResult.put('downtime', false)
        }
    } catch (NoSuchRoleException ignored) {
            currentResult.put('change_in_git', "definition of new ${roleDef.id} role")
            currentResult.put('change_in_runtime', 'N/A')
            currentResult.put('change_type', 'add')
            currentResult.put('description', "the ${roleDef.id} role will be added")
            currentResult.put('action', '')
            currentResult.put('downtime', false)
    }
    scriptResults['action_details'].add(currentResult)
}

authManager.listRoles().each { rtRole ->
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
        currentResult.put('action', '')
        currentResult.put('downtime', false)

        scriptResults['action_details'].add(currentResult)
    }
}

return JsonOutput.toJson(scriptResults)
// return scriptResults