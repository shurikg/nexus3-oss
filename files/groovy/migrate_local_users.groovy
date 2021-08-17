import groovy.json.JsonOutput
import org.sonatype.nexus.security.role.RoleIdentifier
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.user.User
import org.sonatype.nexus.security.user.UserSearchCriteria

def fileName = "users.yml"
def migrationUsers = ['nexus_local_users': []]
Map scriptResults = [changed: false, error: false, 'action_details': [:]]

// authManager = security.securitySystem.getAuthorizationManager(UserManager.DEFAULT_SOURCE)

def searchCriteria = UserSearchCriteria()
searchCriteria.setSource(UserManager.DEFAULT_SOURCE)

def excludeUsers = ['admin', 'anonymous']

// security.securitySystem.listUsers().each { rtUser ->
security.securitySystem.searchUsers(searchCriteria).each { rtUser ->
    if (! (rtUser.getUserId() in excludeUsers) ) {
        Map<String, String> currentUser = [:]

        currentUser.put('username', rtUser.getUserId())
        currentUser.put('state', 'present')
        currentUser.put('first_name', rtUser.getFirstName())
        currentUser.put('last_name', rtUser.getLastName())
        currentUser.put('email', rtUser.getEmailAddress())
        currentUser.put('password', 'xxxx')

        def curentUserRoles = []

        Set<RoleIdentifier> existingRoles = rtUser.getRoles()
        existingRoles.each { currentRole ->
            curentUserRoles.add(currentRole.getRoleId())
        }
        currentUser.put('roles', curentUserRoles)

        migrationUsers['nexus_local_users'].add(currentUser)
    }
}
scriptResults['action_details'].put(fileName, migrationUsers)

return JsonOutput.toJson(scriptResults)