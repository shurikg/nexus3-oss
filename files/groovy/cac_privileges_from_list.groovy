import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.privilege.Privilege

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

authManager = security.getSecuritySystem().getAuthorizationManager(UserManager.DEFAULT_SOURCE)

parsed_args.details.each { privilegeDef ->

    Map<String, String> currentResult = [:]
    def privilege
    boolean update = true

    try {
        privilege = authManager.getPrivilege(privilegeDef.name)
    } catch (NoSuchPrivilegeException ignored) {
        currentResult.put('change_in_git', "definition of new ${privilegeDef.name} privilege")
        currentResult.put('change_in_runtime', 'N/A')
        currentResult.put('change_type', 'add')
        currentResult.put('description', "the ${privilegeDef.name} privilege will be added")
        currentResult.put('resource', 'privilege')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
        update = false
    }
    if (update) {
        def gitChangeMessage = []
        def runtimeChangeMessage = []

        if (privilege.getDescription() != privilegeDef.description) {
                gitChangeMessage.add("description = ${privilegeDef.description}")
                runtimeChangeMessage.add("description = ${privilege.getDescription()}")
        }
        if (privilege.getType() != privilegeDef.type) {
                gitChangeMessage.add("type = ${privilegeDef.type}")
                runtimeChangeMessage.add("type = ${privilege.getType()}")
        }
        if (privilege.getName() != privilegeDef.name) {
                gitChangeMessage.add("name = ${privilegeDef.name}")
                runtimeChangeMessage.add("name = ${privilege.getName()}")
        }
        if (privilege.getPrivilegeProperty('format') != privilegeDef.format) {
                gitChangeMessage.add("property format = ${privilegeDef.format}")
                runtimeChangeMessage.add("property format = ${privilege.getPrivilegeProperty('format')}")
        }
        if (privilege.getPrivilegeProperty('contentSelector') != privilegeDef.contentSelector) {
                gitChangeMessage.add("property contentSelector = ${privilegeDef.contentSelector}")
                runtimeChangeMessage.add("property contentSelector = ${privilege.getPrivilegeProperty('contentSelector')}")
        }
        if (privilege.getPrivilegeProperty('repository') != privilegeDef.repository) {
                gitChangeMessage.add("property repository = ${privilegeDef.repository}")
                runtimeChangeMessage.add("property repository = ${privilege.getPrivilegeProperty('repository')}")
        }
        if (privilege.getPrivilegeProperty('pattern') != privilegeDef.pattern) {
                gitChangeMessage.add("property pattern = ${privilegeDef.pattern}")
                runtimeChangeMessage.add("property pattern = ${privilege.getPrivilegeProperty('pattern')}")
        }
        if (privilege.getPrivilegeProperty('domain') != privilegeDef.domain) {
                gitChangeMessage.add("property domain = ${privilegeDef.domain}")
                runtimeChangeMessage.add("property domain = ${privilege.getPrivilegeProperty('domain')}")
        }
        if (privilege.getPrivilegeProperty('name') != privilegeDef.script_name) {
                gitChangeMessage.add("property name = ${privilegeDef.script_name}")
                runtimeChangeMessage.add("property name = ${privilege.getPrivilegeProperty('name')}")
        }
        if (privilege.getPrivilegeProperty('actions') != privilegeDef.actions.join(',')) {
                gitChangeMessage.add("property actions = ${privilegeDef.actions.join(',')}")
                runtimeChangeMessage.add("property format = ${privilege.getPrivilegeProperty('actions')}")
        }

        if (gitChangeMessage) {
            currentResult.put('change_in_git', gitChangeMessage.join('\n'))
            currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
            currentResult.put('change_type', 'change')
            currentResult.put('description', "the privilege configuration will be update")
            currentResult.put('resource', 'privilelege')
            currentResult.put('downtime', false)
            scriptResults['action_details'].add(currentResult)
        }
    }
}

authManager.listPrivileges().each { rtPrivilege ->
    if (! rtPrivilege.isReadOnly()) {
        def needToDelete = true
        Map<String, String> currentResult = [:]

        parsed_args.details.any { privilegeDef ->
            if (rtPrivilege.getName() == privilegeDef.name) {
                needToDelete = false
                return true
            }
        }
        if (needToDelete){
            currentResult.put('change_in_git', 'N/A')
            currentResult.put('change_in_runtime', "${rtPrivilege.getName()} privilege exist")
            currentResult.put('change_type', 'delete')
            currentResult.put('description', "the ${rtPrivilege.getName()} privilege will be deleted")
            currentResult.put('resource', 'privilege')
            currentResult.put('downtime', false)

            scriptResults['action_details'].add(currentResult)

            if (! parsed_args.dry_run) {
                authManager.deletePrivilege(rtPrivilege.getName())
            }
        }
    }
}

return JsonOutput.toJson(scriptResults)
