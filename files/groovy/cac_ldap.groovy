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
def ldapConfig = null

boolean update = false
ldapConfigMgr.listLdapServerConfigurations().each {
    if (it.name == parsed_args.name) {
        ldapConfig = it
        update = true
    }
}

if (update) {
    def ldapConnection = ldapConfig.getConnection()
    def gitChangeMessage = []
    def runtimeChangeMessage = []

    if ( ldapConnection.getAuthScheme() != parsed_args.auth) {
        gitChangeMessage.add("authentication method = ${parsed_args.auth}")
        runtimeChangeMessage.add("authentication method = ${ldapConnection.getAuthScheme()}")
    }
    if ( ldapConnection.getHost().getProtocol().name() != parsed_args.protocol) {
        gitChangeMessage.add("protocol = ${parsed_args.protocol}")
        runtimeChangeMessage.add("protocol = ${ldapConnection.getHost().getProtocol().name()}")
    }
    if ( ldapConnection.getHost().getHostName()  != parsed_args.hostname) {
        gitChangeMessage.add("hostname = ${parsed_args.hostname}")
        runtimeChangeMessage.add("hostname = ${ldapConnection.getHost().getHostName()}")
    }
    if ( ldapConnection.getHost().getPort() != parsed_args.port.toInteger()) {
        gitChangeMessage.add("port = ${parsed_args.port.toInteger()}")
        runtimeChangeMessage.add("port = ${ldapConnection.getHost().getPort()}")
    }
    if ( ldapConnection.getUseTrustStore() != parsed_args.use_trust_store) {
        gitChangeMessage.add("use trust store = ${parsed_args.use_trust_store}")
        runtimeChangeMessage.add("use trust store = ${ldapConnection.getUseTrustStore()}")
    }
    if (parsed_args.auth == "simple") {
        if ( ldapConnection.getSystemUsername() != parsed_args.username) {
            gitChangeMessage.add("username = ${parsed_args.username}")
            runtimeChangeMessage.add("username = ${ldapConnection.getSystemUsername()}")
        }
        if ( ldapConnection.getSystemPassword() != parsed_args.password) {
            gitChangeMessage.add("password = ${parsed_args.password}")
            runtimeChangeMessage.add("password = ${ldapConnection.getSystemPassword()}")
        }
    }
    if ( ldapConnection.getSearchBase() != parsed_args.search_base) {
        gitChangeMessage.add("search base = ${parsed_args.search_base}")
        runtimeChangeMessage.add("search base = ${ldapConnection.getSearchBase()}")
    }
    if ( ldapConnection.getConnectionTimeout() != 30) {
        gitChangeMessage.add("connection timeout (hardcoded) = 30")
        runtimeChangeMessage.add("connection timeout = ${ldapConnection.getConnectionTimeout()}")
    }
    if ( ldapConnection.getConnectionRetryDelay() != 300) {
        gitChangeMessage.add('connection retry delay (hardcoded) = 300')
        runtimeChangeMessage.add("connection retry delay = ${ldapConnection.getConnectionRetryDelay()}")
    }
    if ( ldapConnection.getMaxIncidentsCount() != 3) {
        gitChangeMessage.add('max incidents count (hardcoded) = 3')
        runtimeChangeMessage.add("max incidents count = ${ldapConnection.getMaxIncidentsCount()}")
    }

    def ldapMapping = ldapConfig.getMapping()
    if ( ldapMapping.getUserBaseDn() != parsed_args.user_base_dn) {
        gitChangeMessage.add("user base dn = ${parsed_args.user_base_dn}")
        runtimeChangeMessage.add("user base dn = ${ldapMapping.getUserBaseDn()}")
    }
    if ( ldapMapping.getLdapFilter() != parsed_args.user_ldap_filter) {
        gitChangeMessage.add("user ldap filter = ${parsed_args.user_ldap_filter}")
        runtimeChangeMessage.add("user ldap filter = ${ldapMapping.getLdapFilter()}")
    }
    if ( ldapMapping.getUserObjectClass() != parsed_args.user_object_class) {
        gitChangeMessage.add("user object class = ${parsed_args.user_object_class}")
        runtimeChangeMessage.add("user object class = ${ldapMapping.getUserObjectClass()}")
    }
    if ( ldapMapping.getUserIdAttribute() != parsed_args.user_id_attribute) {
        gitChangeMessage.add("user id attribute = ${parsed_args.user_id_attribute}")
        runtimeChangeMessage.add("user id attribute = ${ldapMapping.getUserIdAttribute()}")
    }
    if ( ldapMapping.getUserRealNameAttribute() != parsed_args.user_real_name_attribute) {
        gitChangeMessage.add("user real name attribute = ${parsed_args.user_real_name_attribute}")
        runtimeChangeMessage.add("user real name attribute = ${ldapMapping.getUserRealNameAttribute()}")
    }
    if ( ldapMapping.getEmailAddressAttribute() != parsed_args.user_email_attribute) {
        gitChangeMessage.add("user email attribute = ${parsed_args.user_email_attribute}")
        runtimeChangeMessage.add("user email attribute = ${ldapMapping.getEmailAddressAttribute()}")
    }
    if ( ldapMapping.isUserSubtree() != parsed_args.user_subtree) {
        gitChangeMessage.add("user subtree = ${parsed_args.user_subtree}")
        runtimeChangeMessage.add("user subtree = ${ldapMapping.isUserSubtree()}")
    }
    if ( ldapMapping.isGroupSubtree() != parsed_args.group_subtree) {
        gitChangeMessage.add("group subtree = ${parsed_args.group_subtree}")
        runtimeChangeMessage.add("group subtree = ${ldapMapping.isGroupSubtree()}")
    }
    if ( ldapMapping.isLdapGroupsAsRoles() != parsed_args.map_groups_as_roles) {
        gitChangeMessage.add("map groups as roles = ${parsed_args.map_groups_as_roles}")
        runtimeChangeMessage.add("map groups as roles = ${ldapMapping.isLdapGroupsAsRoles()}")
    }
    if (parsed_args.map_groups_as_roles) {
        if ( parsed_args.map_groups_as_roles_type == "dynamic" && ldapMapping.getUserMemberOfAttribute() != parsed_args.user_memberof_attribute) {
            gitChangeMessage.add("user memberof attribute = ${parsed_args.user_memberof_attribute}")
            runtimeChangeMessage.add("user memberof attribute = ${ldapMapping.getUserMemberOfAttribute()}")
        }
        if ( ldapMapping.getGroupBaseDn() != parsed_args.group_base_dn) {
            gitChangeMessage.add("group base dn = ${parsed_args.group_base_dn}")
            runtimeChangeMessage.add("group base dn = ${ldapMapping.getGroupBaseDn()}")
        }
        if ( ldapMapping.getGroupObjectClass() != parsed_args.group_object_class) {
            gitChangeMessage.add("group object class = ${parsed_args.group_object_class}")
            runtimeChangeMessage.add("group object class = ${ldapMapping.getGroupObjectClass()}")
        }
        if ( ldapMapping.getGroupIdAttribute() != parsed_args.group_id_attribute) {
            gitChangeMessage.add("group id attribute = ${parsed_args.group_id_attribute}")
            runtimeChangeMessage.add("group id attribute = ${ldapMapping.getGroupIdAttribute()}")
        }
        if ( ldapMapping.getGroupMemberAttribute() != parsed_args.group_member_attribute) {
            gitChangeMessage.add("group member attribute = ${parsed_args.group_member_attribute}")
            runtimeChangeMessage.add("group member attribute = ${ldapMapping.getGroupMemberAttribute()}")
        }
        if ( ldapMapping.getGroupMemberFormat() != parsed_args.group_member_format) {
            gitChangeMessage.add("group member format = ${parsed_args.group_member_format}")
            runtimeChangeMessage.add("group member format = ${ldapMapping.getGroupMemberFormat()}")
        }
    }
    if (gitChangeMessage) {
        currentResult.put('change_in_git', gitChangeMessage.join('\n'))
        currentResult.put('change_in_runtime', runtimeChangeMessage.join('\n'))
        currentResult.put('change_type', 'change')
        currentResult.put('description', "the ${parsed_args.name} ldap configuration will be update")
        currentResult.put('resource', 'ldap')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
} else {
    if (parsed_args.name != null) {
        currentResult.put('change_in_git', "definition of new ${parsed_args.name} ldap")
        currentResult.put('change_in_runtime', 'N/A')
        currentResult.put('change_type', 'add')
        currentResult.put('description', "the ${parsed_args.name} ldap will be added")
        currentResult.put('resource', 'ldap')
        currentResult.put('downtime', false)
        scriptResults['action_details'].add(currentResult)
    }
}

return JsonOutput.toJson(scriptResults)
