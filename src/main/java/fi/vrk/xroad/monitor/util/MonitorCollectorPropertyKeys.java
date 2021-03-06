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

package fi.vrk.xroad.monitor.util;


/**
 * Property access class
 */
public final class MonitorCollectorPropertyKeys {
    public static final String CLIENT_URL = "xroad-monitor-collector-url.client-url";
    
    public static final String CLIENT_MEMBER_CLASS = "xroad-monitor-collector-client.client-member-class";

    public static final String CLIENT_MEMBER_CODE = "xroad-monitor-collector-client.client-member-code";

    public static final String CLIENT_SUBSYSTEM = "xroad-monitor-collector-client.client-subsystem";

    public static final String INSTANCE = "xroad-monitor-collector-client.instance";

    public static final String QUERY_PARAMETERS = "xroad-monitor-collector.query-parameters";

    private MonitorCollectorPropertyKeys() { }
}
