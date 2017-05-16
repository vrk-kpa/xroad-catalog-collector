package fi.vrk.xroad.monitor;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Class for parsing X-Road shared-params.xml
 */
@Slf4j
public class SharedParamsParser {

  private final String filename;

  public SharedParamsParser(String filename){
    this.filename = filename;
  }

  public List<SecurityServerInfo> parse() throws ParserConfigurationException, IOException, SAXException {
    File inputFile = new File(filename);
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document document = documentBuilder.parse(inputFile);
    document.setXmlVersion("1.0");
    document.getDocumentElement().normalize();
    Element root = document.getDocumentElement();
    log.info("root: {}", root.getTagName());

    NodeList members = root.getElementsByTagName("member");
    NodeList securityServers = root.getElementsByTagName("securityServer");
    List<SecurityServerInfo> securityServerInfoList = Collections.emptyList();

    for (int i=0; i<securityServers.getLength(); i++) {
      Node securityServer = securityServers.item(i);
      if (securityServer.getNodeType() == Node.ELEMENT_NODE) {
        SecurityServerInfo info = null;
        Element securityServerElement = (Element) securityServer;
        String owner =  securityServerElement.getElementsByTagName("owner").item(0).getTextContent();
        String serverCode = securityServerElement.getElementsByTagName("serverCode").item(0).getTextContent();
        String address = securityServerElement.getElementsByTagName("address").item(0).getTextContent();
        for (int j=0; j<members.getLength(); j++) {
          Node member = members.item(i);
          if (member.getNodeType() == Node.ELEMENT_NODE) {
            Element memberElement = (Element) member;
            if (memberElement.getAttribute("id").equals(owner)) {
              Element memberClass = (Element) memberElement.getElementsByTagName("memberClass").item(0);
              log.info("memberClass: {}", memberClass.getElementsByTagName("code").item(0).getTextContent());
            }
          }
        }
      }
    }
    return securityServerInfoList;
  }
}
