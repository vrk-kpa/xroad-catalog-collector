/**
 * The MIT License
 * Copyright (c) 2017, Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.vrk.xroad.monitor.extractor;

import ee.ria.xroad.proxymonitor.message.*;
import fi.vrk.xroad.monitor.parser.SecurityServerInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Handles responseParser and returns only body
 */
@Slf4j
@Component
public class MonitorDataResponseParser {

    @Getter
    private String lastErrorDescription;

    /**
     * Parse metric information from xml response string and return json data
     * @param xmlResponse xml string what is gotten from securityserver
     * @return metric data in xml string, or null in case of fault
     */
    public String getMetricInformation(String xmlResponse, SecurityServerInfo securityServerInfo,
                                       String xroadInstance) {
        lastErrorDescription = "";

        Document root = parseResponseDocument(xmlResponse);
        String resultString = null;

        if (root != null) {
            root.normalizeDocument();

            NodeList nodeList = root.getElementsByTagName("m:getSecurityServerMetricsResponse");
            if (nodeList.getLength() == 0) {
                NodeList faultCode = root.getElementsByTagName("faultcode");
                NodeList faultString = root.getElementsByTagName("faultstring");
                log.debug("Faultcode in responseParser: {} faultstring: {} responseParser: {}",
                        nodeToString(faultCode.item(0)), nodeToString(faultString.item(0)), xmlResponse);
                lastErrorDescription = String.format("%s %s", nodeToString(faultCode.item(0)),
                    nodeToString(faultString.item(0)));
            } else {
                try {
                    Unmarshaller jaxbUnmarshaller =
                            JAXBContext.newInstance(GetSecurityServerMetricsResponse.class).createUnmarshaller();
                    GetSecurityServerMetricsResponse responseObject
                            = (GetSecurityServerMetricsResponse) jaxbUnmarshaller.unmarshal(nodeList.item(0));
                    resultString = getFormattedJSONObject(responseObject, securityServerInfo, xroadInstance).toString();
                } catch (JAXBException e) {
                    log.error("Failed unmarshalling XML to POJO", e);
                }
            }
            return resultString;
        }
        return null;
    }

    /**
     * Create default JSON format environmental monitoring data
     * @param info security server information
     * @param xroadInstance X-Road instance
     * @return JSON formatted string containing the default data
     */
    public String getDefaultJSON(SecurityServerInfo info, String xroadInstance, String errorString) {
        JSONObject json = new JSONObject();
        json.put("serverCode", info.getServerCode());
        json.put("memberCode", info.getMemberCode());
        json.put("memberClass", info.getMemberClass());
        json.put("xroadInstance", xroadInstance);
        json.put("name", String.format("SERVER:%s/%s/%s/%s", xroadInstance, info.getMemberClass(),
            info.getMemberCode(), info.getServerCode()));
        json.put("error", errorString);
        return json.toString();
    }

    /**
     * Function for formating JSON object to more usable form
     * @param responseObject response object to be formated
     * @param securityServerInfo information of security server
     * @param xroadInstance xroadInstance identifier
     * @return formated JSON object
     */
    private JSONObject getFormattedJSONObject(GetSecurityServerMetricsResponse responseObject,
                                              SecurityServerInfo securityServerInfo, String xroadInstance) {
        JSONObject json = new JSONObject();
        json.put("serverCode", securityServerInfo.getServerCode());
        json.put("memberCode", securityServerInfo.getMemberCode());
        json.put("memberClass", securityServerInfo.getMemberClass());
        json.put("xroadInstance", xroadInstance);

        MetricSetType rootMetric = responseObject.getMetricSet();
        json.put("name", rootMetric.getName());

        List<MetricType> metricList = rootMetric.getMetrics();

        return makeJSONObject(json, metricList);
    }

    /**
     * Function what will create wanted json object
     * @param json object to formated
     * @param metricList rest of object not to formated
     * @return formated json
     */
    private JSONObject makeJSONObject(JSONObject json, List<MetricType> metricList) {
        for (MetricType metricType : metricList) {
            if (metricType instanceof HistogramMetricType) {
                json.put(metricType.getName(), createHistogramJson((HistogramMetricType) metricType));
            } else if (metricType instanceof NumericMetricType) {
                json.put(metricType.getName(), ((NumericMetricType) metricType).getValue());
            } else if (metricType instanceof StringMetricType) {
                json.put(metricType.getName(), ((StringMetricType) metricType).getValue());
            } else if (metricType instanceof MetricSetType) {
                List<MetricType> subList = ((MetricSetType) metricType).getMetrics();
                if (Arrays.asList("Processes", "Xroad Processes", "Certificates", "Packages")
                        .contains(metricType.getName())) {
                    json.put(metricType.getName(), subList.stream().map(m -> {
                        if (m instanceof StringMetricType) {
                            return m.getName() + " " + ((StringMetricType) m).getValue();
                        } else {
                            return makeJSONObject(new JSONObject(), ((MetricSetType) m).getMetrics());
                        }
                    }).toArray());
                } else {
                    json = makeJSONObject(json, subList);
                }
            }
        }
        return json;
    }

    /**
     * Helper method to create histogram json object
     * @param metric HistogramMetricType
     * @return histogram json object
     */
    private JSONObject createHistogramJson(HistogramMetricType metric) {
        HistogramMetricType histogram = (HistogramMetricType) metric;
        JSONObject histogramJson = new JSONObject();
        histogramJson.put("updated", histogram.getUpdated());
        histogramJson.put("min", histogram.getMin());
        histogramJson.put("max", histogram.getMax());
        histogramJson.put("mean", histogram.getMean());
        histogramJson.put("median", histogram.getMedian());
        histogramJson.put("stddev", histogram.getStddev());
        return histogramJson;
    }

    /**
     * Parse xml node to string
     *
     * @param item xml node
     * @return xml string
     */
    private String nodeToString(Node item) {
        try {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.transform(new DOMSource(item), result);
            return writer.toString();
        } catch (TransformerException e) {
            log.error("Failed to parse string from metric node: {}", e);
            return "";
        }
    }

    /**
     * Parse respose string to xml document
     *
     * @param response string
     * @return xml document
     */
    private Document parseResponseDocument(String response) {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response));
            return builder.parse(is);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.error("Failed to parse responseParser document from string: {}", e);
            return null;
        }
    }
}
