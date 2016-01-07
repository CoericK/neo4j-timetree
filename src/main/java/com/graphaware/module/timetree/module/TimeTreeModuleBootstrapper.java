/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.module.timetree.module;

import com.graphaware.common.policy.NodeInclusionPolicy;
import com.graphaware.module.timetree.domain.Resolution;
import com.graphaware.runtime.config.function.StringToNodeInclusionPolicy;
import com.graphaware.runtime.module.BaseRuntimeModuleBootstrapper;
import com.graphaware.runtime.module.RuntimeModule;
import org.joda.time.DateTimeZone;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TimeZone;

/**
 * Bootstraps the {@link com.graphaware.module.timetree.module.TimeTreeModule} in server mode.
 */
public class TimeTreeModuleBootstrapper extends BaseRuntimeModuleBootstrapper<TimeTreeConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(TimeTreeModuleBootstrapper.class);

    private static final String EVENT = "event";
    private static final String TIMESTAMP_PROPERTY = "timestamp";
    private static final String CUSTOM_TIMETREE_ROOT_PROPERTY = "customTimeTreeRootProperty";
    private static final String RESOLUTION = "resolution";
    private static final String TIME_ZONE = "timezone";
    private static final String RELATIONSHIP = "relationship";
    private static final String DIRECTION = "direction";
    private static final String AUTO_ATTACH = "autoAttach";

    @Override
    protected TimeTreeConfiguration defaultConfiguration() {
        return TimeTreeConfiguration.defaultConfiguration();
    }

    @Override
    protected TimeTreeConfiguration configureInclusionPolicies(Map<String, String> config, TimeTreeConfiguration configuration) {
        //do nothing here
        return configuration;
    }

    @Override
    protected RuntimeModule doBootstrapModule(String moduleId, Map<String, String> config, GraphDatabaseService database, TimeTreeConfiguration configuration) {
        if (configExists(config, EVENT)) {
            NodeInclusionPolicy policy = StringToNodeInclusionPolicy.getInstance().apply(config.get(EVENT));
            LOG.info("Node Inclusion Strategy set to {}", policy);
            configuration = configuration.with(policy);
        }

        if (configExists(config, TIMESTAMP_PROPERTY)) {
            String timestampProperty = config.get(TIMESTAMP_PROPERTY);
            LOG.info("Timestamp Property set to {}", timestampProperty);
            configuration = configuration.withTimestampProperty(timestampProperty);
        }

        if (configExists(config, CUSTOM_TIMETREE_ROOT_PROPERTY)) {
            String customTimeTreeRootProperty = config.get(CUSTOM_TIMETREE_ROOT_PROPERTY);
            LOG.info("Custom TimeTree Root Property set to {}", customTimeTreeRootProperty);
            configuration = configuration.withCustomTimeTreeRootProperty(customTimeTreeRootProperty);
        }

        if (configExists(config, RESOLUTION)) {
            Resolution resolution = Resolution.valueOf(config.get(RESOLUTION).toUpperCase());
            LOG.info("Resolution set to {}", resolution);
            configuration = configuration.withResolution(resolution);
        }

        if (configExists(config, TIME_ZONE)) {
            DateTimeZone timeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone(config.get(TIME_ZONE)));
            LOG.info("Time zone set to {}", timeZone);
            configuration = configuration.withTimeZone(timeZone);
        }

        if (configExists(config, RELATIONSHIP)) {
            RelationshipType relationshipType = DynamicRelationshipType.withName(config.get(RELATIONSHIP));
            LOG.info("Relationship type set to {}", relationshipType);
            configuration = configuration.withRelationshipType(relationshipType);
        }

        if (configExists(config, DIRECTION)) {
            Direction direction = Direction.valueOf(config.get(DIRECTION));
            LOG.info("Direction set to {}", direction);
            configuration = configuration.withDirection(direction);
        }

        if (configExists(config, AUTO_ATTACH)) {
            boolean autoAttach = Boolean.valueOf(config.get(AUTO_ATTACH));
            LOG.info("AutoAttach set to {}", autoAttach);
            configuration = configuration.withAutoAttach(autoAttach);
        }

        return new TimeTreeModule(moduleId, configuration, database);
    }
}
