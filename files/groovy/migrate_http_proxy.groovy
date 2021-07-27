import groovy.json.JsonOutput
import org.sonatype.nexus.internal.httpclient.HttpClientManagerImpl
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration
import org.sonatype.nexus.httpclient.config.ProxyConfiguration
import org.sonatype.nexus.httpclient.config.ProxyServerConfiguration

def getValue(value) {
    if (value == null) {
        return ''
    }
    return value
}

def httpClientManager = container.lookup(HttpClientManagerImpl.class.getName())
def httpConfiguration = httpClientManager.getConfiguration()
ProxyConfiguration proxyConfiguration = httpConfiguration.getProxy()
ProxyServerConfiguration rtHttpProxy = proxyConfiguration?.getHttp()
ProxyServerConfiguration rtHttpsProxy = proxyConfiguration?.getHttps()

Map scriptResults = [changed: false, error: false, 'action_details': [:]]

def content = [:]

if (rtHttpProxy?.isEnabled()) {
    content.put('nexus_with_http_proxy', true)
    content.put('nexus_http_proxy_host', getValue(rtHttpProxy?.getHost()))
    content.put('nexus_http_proxy_port', getValue(rtHttpProxy?.getPort()))
    content.put('nexus_http_proxy_username', getValue(rtHttpProxy?.getAuthentication()?.getUsername()))
    content.put('nexus_http_proxy_password', getValue(rtHttpProxy?.getAuthentication()?.getPassword()))
}
else {
    content.put('nexus_with_http_proxy', false)
}

if (rtHttpsProxy?.isEnabled()) {
    content.put('nexus_with_https_proxy', true)
    content.put('nexus_https_proxy_host', getValue(rtHttpsProxy?.getHost()))
    content.put('nexus_https_proxy_port', getValue(rtHttpsProxy?.getPort()))
    content.put('nexus_https_proxy_username', getValue(rtHttpsProxy?.getAuthentication()?.getUsername()))
    content.put('nexus_https_proxy_password', getValue(rtHttpsProxy?.getAuthentication()?.getPassword()))
}
else {
    content.put('nexus_with_https_proxy', false)
}
content.put('nexus_proxy_exclude_hosts', proxyConfiguration.getNonProxyHosts())

scriptResults['action_details'].put('http.yml', content)
return JsonOutput.toJson(scriptResults)
