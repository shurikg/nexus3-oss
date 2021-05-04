import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Field
import org.sonatype.nexus.security.role.RoleIdentifier
import org.sonatype.nexus.security.user.InvalidCredentialsException
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.user.UserNotFoundException
import org.sonatype.nexus.security.user.User

List<Map<String, String>> actionDetails = []
@Field Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)
authManager = security.securitySystem.getAuthorizationManager(UserManager.DEFAULT_SOURCE)

parsed_args = new JsonSlurper().parseText(args)

def migrationUsers = ['nexus_local_users': []]

def excludeUsers = ['admin']

security.securitySystem.listUsers().each { rtUser ->
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
scriptResults['action_details'].add(migrationUsers)
return JsonOutput.toJson(scriptResults)
