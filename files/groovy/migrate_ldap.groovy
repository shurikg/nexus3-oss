import groovy.json.JsonOutput
import org.sonatype.nexus.ldap.persist.LdapConfigurationManager
import org.sonatype.nexus.ldap.persist.entity.Connection
import org.sonatype.nexus.ldap.persist.entity.Mapping

def fileName = "chi/ldap.yml"
def migrationLdaps = ['ldap_connections': []]
Map scriptResults = [changed: false, error: false, 'action_details': [:]]

def ldapConfigMgr = container.lookup(LdapConfigurationManager.class.getName())

ldapConfigMgr.listLdapServerConfigurations().each { rtLdap ->
    def ldapConnection = rtLdap.getConnection()
    Map<String, String> currentLdap = [:]

    currentLdap.put('name', rtLdap.name)
    currentLdap.put('ldap_protocol', ldapConnection.getHost().getProtocol())
    currentLdap.put('ldap_hostname', ldapConnection.getHost().getHostName())
    currentLdap.put('ldap_port', ldapConnection.getHost().getPort())
    currentLdap.put('ldap_use_trust_store', ldapConnection.getUseTrustStore())
    currentLdap.put('ldap_auth', ldapConnection.getAuthScheme())
    currentLdap.put('ldap_auth_username', ldapConnection.getSystemUsername())
    currentLdap.put('ldap_auth_password', ldapConnection.getSystemPassword())
    currentLdap.put('ldap_search_base', ldapConnection.getSearchBase())

    def ldapMapping = ldapConfig.getMapping()
    currentLdap.put('ldap_user_base_dn', ldapMapping.getUserBaseDn())
    currentLdap.put('ldap_user_filter', ldapMapping.getLdapFilter())
    currentLdap.put('ldap_user_object_class', ldapMapping.getUserObjectClass())
    currentLdap.put('ldap_user_id_attribute', ldapMapping.getUserIdAttribute())
    currentLdap.put('ldap_user_real_name_attribute', ldapMapping.getUserRealNameAttribute())
    currentLdap.put('ldap_user_email_attribute', ldapMapping.getEmailAddressAttribute())
    currentLdap.put('ldap_map_groups_as_roles', ldapMapping.isLdapGroupsAsRoles())
    // currentLdap.put('ldap_map_groups_as_roles_type', ldapMapping.isLdapGroupsAsRoles())
    currentLdap.put('ldap_user_memberof_attribute', ldapMapping.getUserMemberOfAttribute())
    currentLdap.put('ldap_group_base_dn', ldapMapping.getGroupBaseDn())
    currentLdap.put('ldap_group_object_class', ldapMapping.getGroupObjectClass())
    currentLdap.put('ldap_group_id_attribute', ldapMapping.getGroupIdAttribute())
    currentLdap.put('ldap_group_member_attribute', ldapMapping.getGroupMemberAttribute())
    currentLdap.put('ldap_group_member_format', ldapMapping.getGroupMemberFormat())
    currentLdap.put('ldap_user_subtree', ldapMapping.isUserSubtree())
    currentLdap.put('ldap_group_subtree', ldapMapping.isGroupSubtree())

    migrationLdaps['ldap_connections'].add(currentUser)
}

scriptResults['action_details'].put(fileName, migrationLdaps)

return JsonOutput.toJson(scriptResults)
