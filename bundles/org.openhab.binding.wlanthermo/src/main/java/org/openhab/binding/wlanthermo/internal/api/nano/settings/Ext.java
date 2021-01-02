/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.wlanthermo.internal.api.nano.settings;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * This DTO is used to parse the JSON
 * Class is auto-generated from JSON using http://www.jsonschema2pojo.org/
 *
 * @author Christian Schlipp - Initial contribution
 */
public class Ext {

    @SerializedName("on")
    @Expose
    public Integer on;
    @SerializedName("token")
    @Expose
    public String token;
    @SerializedName("id")
    @Expose
    public String id;
    @SerializedName("repeat")
    @Expose
    public Integer repeat;
    @SerializedName("service")
    @Expose
    public Integer service;
    @SerializedName("services")
    @Expose
    public List<String> services = new ArrayList<String>();
}
