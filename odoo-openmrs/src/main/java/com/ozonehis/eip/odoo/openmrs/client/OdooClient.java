/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.client;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import com.ozonehis.eip.odoo.openmrs.Constants;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@NoArgsConstructor
@Component
public class OdooClient {

    @Value("${odoo.baseUrl}")
    private String url;

    @Value("${odoo.database}")
    private String database;

    @Value("${odoo.username}")
    private String username;

    @Value("${odoo.password}")
    private String password;

    private Integer uid;

    private XmlRpcClientConfigImpl xmlRpcClientConfig;

    private XmlRpcClient client;

    private static final String SERVER_OBJECT_URL = "%s/xmlrpc/2/object";

    private static final String SERVER_COMMON_URL = "%s/xmlrpc/2/common";

    public OdooClient(String url, String database, String username, String password) {
        this.url = url;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void init() {
        if (xmlRpcClientConfig == null) {
            try {
                xmlRpcClientConfig = new XmlRpcClientConfigImpl();
                xmlRpcClientConfig.setEnabledForExtensions(true);
                xmlRpcClientConfig.setServerURL(new URL(String.format(SERVER_OBJECT_URL, getUrl())));
            } catch (MalformedURLException e) {
                throw new RuntimeException(
                        String.format(
                                "Error occurred while building odoo server url %s error %s", getUrl(), e.getMessage()),
                        e);
            }
        }
        if (client == null) {
            client = new XmlRpcClient();
            client.setConfig(xmlRpcClientConfig);
        }
        // authenticate
        if (uid == null) {
            try {
                XmlRpcClientConfigImpl xmlRpcClientCommonConfig = new XmlRpcClientConfigImpl();
                xmlRpcClientCommonConfig.setServerURL(new URL(String.format(SERVER_COMMON_URL, getUrl())));
                uid = (Integer) client.execute(
                        xmlRpcClientCommonConfig,
                        "authenticate",
                        asList(getDatabase(), getUsername(), getPassword(), emptyMap()));
            } catch (XmlRpcException | MalformedURLException e) {
                throw new RuntimeException("Cannot authenticate to Odoo server", e);
            }
        }
    }

    public Integer create(String model, List<Map<String, Object>> dataParams) {
        init();

        try {
            return (Integer) client.execute(
                    "execute_kw",
                    asList(getDatabase(), uid, getPassword(), model, Constants.CREATE_METHOD, dataParams));
        } catch (XmlRpcException e) {
            throw new RuntimeException("Error occurred while creating in odoo server error", e);
        }
    }

    public Boolean write(String model, List<Object> dataParams) {
        init();

        try {
            return (Boolean) client.execute(
                    "execute_kw", asList(getDatabase(), uid, getPassword(), model, Constants.WRITE_METHOD, dataParams));
        } catch (XmlRpcException e) {
            throw new RuntimeException("Error occurred while writing to odoo server error", e);
        }
    }

    public Boolean delete(String model, List<Object> dataParams) {
        init();

        try {
            return (Boolean) client.execute(
                    "execute_kw",
                    asList(getDatabase(), uid, getPassword(), model, Constants.UNLINK_METHOD, dataParams));
        } catch (XmlRpcException e) {
            throw new RuntimeException("Error occurred while deleting from odoo server error", e);
        }
    }

    public Object[] searchAndRead(String model, List<Object> criteria, List<String> fields) {
        init();

        try {
            List<Object> params = new ArrayList<>();
            params.add(getDatabase());
            params.add(uid);
            params.add(getPassword());
            params.add(model);
            params.add(Constants.SEARCH_READ_METHOD);
            params.add(singletonList(criteria));
            if (fields != null) {
                params.add(singletonMap("fields", fields));
            }

            return (Object[]) client.execute("execute_kw", params);
        } catch (XmlRpcException e) {
            throw new RuntimeException("Error occurred while searchAndRead from odoo server error", e);
        }
    }

    public Object[] search(String model, List<Object> criteria) {
        init();

        try {
            return (Object[]) client.execute(
                    "execute_kw",
                    asList(
                            getDatabase(),
                            uid,
                            getPassword(),
                            model,
                            Constants.SEARCH_METHOD,
                            singletonList(singletonList(criteria))));
        } catch (XmlRpcException e) {
            throw new RuntimeException("Error occurred while searching from odoo server error", e);
        }
    }
}
