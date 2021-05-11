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

def updateUser(userDef, currentResult) {
    User user = security.securitySystem.getUser(userDef.username)

    def gitChangeMessage = []
    def runtimeChangeMessage = []

    if (user.getFirstName() != userDef.first_name) {
        gitChangeMessage.add("First name = ${userDef.first_name}")
        runtimeChangeMessage.add("First name = ${user.getFirstName()}")
    }
    if (user.getLastName() != userDef.last_name) {
        gitChangeMessage.add("Last name = ${userDef.last_name}")
        runtimeChangeMessage.add("Last name = ${user.getLastName()}")
    }
    if (user.getEmailAddress() != userDef.email) {
        gitChangeMessage.add("email = ${userDef.email}")
        runtimeChangeMessage.add("email = ${user.getEmailAddress()}")
    }

    def isRoleDefined
    Set<RoleIdentifier> existingRoles = user.getRoles()
    userDef.roles.each { roleDef ->
        isRoleDefined = false
        existingRoles.any { currentRole ->
            if (currentRole.getRoleId() == roleDef) {
                isRoleDefined = true
                return true
            }
        }
        if (! isRoleDefined) {
            gitChangeMessage.add("Add role: ${roleDef}")
            runtimeChangeMessage.add("N/A")
        }
    }

    existingRoles.each { currentRole ->
        isRoleDefined = false
        userDef.roles.any { gitRole ->
            if (currentRole.getRoleId() == gitRole) {
                isRoleDefined = true
                return true
            }
        }
        if (! isRoleDefined) {
            gitChangeMessage.add("N/A")
            runtimeChangeMessage.add("Delete role: ${currentRole.getRoleId()}")
        }
    }

    if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the ${userDef.username} user will be update")
        currentResult.put('resource', 'user')
        currentResult.put('downtime', false)
    }

//     try {
//         security.securitySystem.changePassword(userDef.username, userDef.password, userDef.password)
//     } catch (InvalidCredentialsException ignored) {
//         security.securitySystem.changePassword(userDef.username, userDef.password)
//         currentResult.put('status', 'updated')
//         scriptResults['changed'] = true
//     }
//     log.info("Updated user {}", userDef.username)
}

def addUser(userDef, currentResult) {
    currentResult.put('change_in_git', "definition of new ${userDef.username} user")
    currentResult.put('change_in_runtime', 'N/A')
    currentResult.put('change_type', 'add')
    currentResult.put('description', "the ${userDef.username} user will be added")
    currentResult.put('resource', 'user')
    currentResult.put('downtime', false)
}

def deleteUser(userDef, currentResult) {
    try {
        User runtimeUser = security.securitySystem.getUser(userDef.username)

        currentResult.put('change_in_git', "state == 'absent' for user ${userDef.username}")
        currentResult.put('change_in_runtime', 'N/A')
        currentResult.put('change_type', 'delete')
        currentResult.put('description', "the ${userDef.username} user will be deleted")
        currentResult.put('resource', 'user')
        currentResult.put('downtime', false)
    } catch (UserNotFoundException ignored) {
        log.info("Delete user: user {} does not exist", userDef.username)
    }
}

/* Main */

parsed_args = new JsonSlurper().parseText(args)

// GIT -> Runtime
parsed_args.details.each { userDef ->

    state = userDef.get('state', 'present')

    Map<String, String> currentResult = [:]

    if (state == 'absent') {
        deleteUser(userDef, currentResult)
    }
    else {
        try {
            updateUser(userDef, currentResult)
        } catch (UserNotFoundException ignored) {
            addUser(userDef, currentResult)
        }
    }
    if (currentResult['change_type']) {
        scriptResults['action_details'].add(currentResult)
    }
}

// Runtime -> GIT
def excludeUsers = ['admin']

security.securitySystem.listUsers().each { rtUser ->
    if (! (rtUser.getUserId() in excludeUsers) ) {
        Map<String, String> currentResult = [:]
        def needToDelete = true
        parsed_args.details.any { userDef ->
            if (rtUser.getUserId() == userDef.username) {
                needToDelete = false
                return true
            }
        }
        if (needToDelete){
            currentResult.put('change_in_git', 'N/A')
            currentResult.put('change_in_runtime', "${rtUser.getUserId()} user exist")
            currentResult.put('change_type', 'delete')
            currentResult.put('description', "the ${rtUser.getUserId()} user will be deleted")
            currentResult.put('resource', 'user')
            currentResult.put('downtime', false)

            scriptResults['action_details'].add(currentResult)

            if (! parsed_args.dry_run) {
                try {
                    security.securitySystem.deleteUser(rtUser.getUserId(), UserManager.DEFAULT_SOURCE)
                    log.info("Deleted user {}", rtUser.getUserId())
                    scriptResults['changed'] = true
                } catch (UserNotFoundException ignored) {
                    log.info("Delete user: user {} does not exist", rtUser.getUserId())
                } catch (Exception e) {
                    scriptResults['error'] = true
                }
            }
        }
    }
}
return JsonOutput.toJson(scriptResults)
