package lk.novalink.zerotrace

import lk.novalink.zerotrace.core.XrayConfigGenerator
import lk.novalink.zerotrace.data.model.ProxyProtocol
import lk.novalink.zerotrace.data.repository.ConfigRepository
import lk.novalink.zerotrace.parser.ConfigParser
import lk.novalink.zerotrace.parser.VlessParser
import lk.novalink.zerotrace.parser.VmessParser
import lk.novalink.zerotrace.parser.TrojanParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigParserTest {

    @Test
    fun testVlessRealityParsing() {
        val link = "vless://a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d@sg.novalink.lk:443?security=reality&sni=sg.novalink.lk&fp=chrome&pbk=test_pub_key_123&sid=1a2b3c4d&type=tcp&flow=xtls-rprx-vision#NovaLink%20SG%20Reality"
        
        val config = ConfigParser.parseSingle(link)
        assertNotNull(config)
        assertEquals(ProxyProtocol.VLESS, config!!.protocol)
        assertEquals("sg.novalink.lk", config.server)
        assertEquals(443, config.port)
        assertEquals("a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d", config.uuid)
        assertEquals("reality", config.security)
        assertEquals("xtls-rprx-vision", config.flow)
        assertEquals("test_pub_key_123", config.publicKey)
        assertEquals("1a2b3c4d", config.shortId)
        assertEquals("NovaLink SG Reality", config.name)
    }

    @Test
    fun testTrojanParsing() {
        val link = "trojan://secretpassword123@hk.novalink.lk:443?security=tls&sni=hk.novalink.lk&type=ws&path=%2Ftrojan-ws#NovaLink%20HK%20Trojan"
        
        val config = ConfigParser.parseSingle(link)
        assertNotNull(config)
        assertEquals(ProxyProtocol.TROJAN, config!!.protocol)
        assertEquals("hk.novalink.lk", config.server)
        assertEquals(443, config.port)
        assertEquals("secretpassword123", config.uuid)
        assertEquals("tls", config.security)
        assertEquals("ws", config.network)
        assertEquals("/trojan-ws", config.path)
        assertEquals("NovaLink HK Trojan", config.name)
    }

    @Test
    fun testXrayConfigGeneration() {
        val link = "vless://a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d@sg.novalink.lk:443?security=reality&sni=sg.novalink.lk&fp=chrome&pbk=test_pub_key_123&sid=1a2b3c4d&type=tcp&flow=xtls-rprx-vision#NovaLink%20SG%20Reality"
        val config = ConfigParser.parseSingle(link)!!

        val jsonOutput = XrayConfigGenerator.generateRuntimeJson(config)
        assertNotNull(jsonOutput)
        assertTrue(jsonOutput.contains("inbounds"))
        assertTrue(jsonOutput.contains("outbounds"))
        assertTrue(jsonOutput.contains("vless"))
        assertTrue(jsonOutput.contains("realitySettings"))
        assertTrue(jsonOutput.contains("test_pub_key_123"))
    }

    // Regression: "host:port/?query" (trailing slash before the query) made the port parse as
    // "45535/", fail, and silently fall back to 443, so the tunnel dialed the wrong port.
    @Test
    fun testPortIsKeptWhenAPathSlashPrecedesTheQuery() {
        val vless = ConfigParser.parseSingle(
            "vless://a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d@node1.example.lk:45535/?encryption=none&security=tls&sni=api.zoom.us&type=tcp#Zoom%20Plan"
        )!!
        assertEquals("node1.example.lk", vless.server)
        assertEquals(45535, vless.port)
        assertEquals("api.zoom.us", vless.sni)

        val trojan = ConfigParser.parseSingle("trojan://secret@node2.example.lk:8443/?sni=x.example.lk#T")!!
        assertEquals(8443, trojan.port)

        val plain = ConfigParser.parseSingle("vless://a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d@sg.example.lk:2053?security=tls#P")!!
        assertEquals(2053, plain.port)
    }

    @Test
    fun testRepairWrongPortFixesOnly443ConfigsWhoseLinkSaysOtherwise() {
        val link = "vless://a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d@node1.example.lk:45535/?security=tls&sni=api.zoom.us#Z"
        val good = ConfigParser.parseSingle(link)!!
        // What v1.4.1 stored: same config but port 443.
        val broken = good.copy(port = 443)
        assertEquals(45535, ConfigRepository.repairWrongPort(broken).port)
        // A port edited by hand (not 443) is never overwritten.
        assertEquals(8443, ConfigRepository.repairWrongPort(good.copy(port = 8443)).port)
        // A genuine 443 link stays 443.
        val real443 = ConfigParser.parseSingle("vless://a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d@sg.example.lk:443/?security=tls#P")!!
        assertEquals(443, ConfigRepository.repairWrongPort(real443).port)
    }
}
