/*
 * Copyright (c) 2013 GraphAware
 *
 * This file is part of GraphAware.
 *
 * GraphAware is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.module.timetree.api;

import com.graphaware.module.timetree.domain.Resolution;
import com.graphaware.module.timetree.domain.TimeInstant;
import com.graphaware.test.integration.GraphAwareApiTest;
import com.graphaware.test.unit.GraphUnit;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.util.Collections;

import static com.graphaware.test.unit.GraphUnit.assertSameGraph;
import static org.junit.Assert.assertEquals;

/**
 * Integration test for {@link TimeTreeApi}.
 */
public class TimedEventsApiTest extends GraphAwareApiTest {

    @Test
    public void shouldComplainWhenInputIllegal() throws IOException {

        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().createNode();
            tx.success();
        }

        String eventJson = "{" +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + System.currentTimeMillis() +
                "    }";

        httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_BAD_REQUEST);

        eventJson = "{" +
                "        \"node\": {\"id\":0}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"time\": " + System.currentTimeMillis() +
                "    }";

        httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_CREATED);

        eventJson = "{" +
                "        \"node\": {\"id\":0, \"labels\":[\"L1\"]}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"time\": " + System.currentTimeMillis() +
                "    }";

        httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_BAD_REQUEST);

        eventJson = "{" +
                "        \"node\": {\"id\":100}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"time\": " + System.currentTimeMillis() +
                "    }";

        httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_NOT_FOUND);

        eventJson = "{" +
                "        \"node\": {\"id\":0}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"" +
                "    }";

        httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_BAD_REQUEST);

        eventJson = "{" +
                "        \"node\": {\"id\":0}," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + System.currentTimeMillis() +
                "    }";

        httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_BAD_REQUEST);

        eventJson = "{" +
                "        \"node\": {\"id\":0}," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"ILLEGAL\"," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"time\": " + System.currentTimeMillis() +
                "    }";

        httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_BAD_REQUEST);

        eventJson = "{" +
                "        \"node\": {\"id\":123}," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"time\": " + System.currentTimeMillis() +
                "    }";

        httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_NOT_FOUND);

        eventJson = "{" +
                "        \"node\": {\"id\":0}," +
                "        \"timezone\": \"ILLEGAL\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"time\": " + (System.currentTimeMillis() - 1000 * 3600 * 24 * 2) + //two days less, we already have an event for this day
                "    }";

        httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_CREATED); //with default timezone

        httpClient.get(getUrl() + "range/2/1/events", HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void eventAndTimeInstantShouldBeCreatedWhenEventIsAttached() throws IOException {
        //Given
        DateTime now = DateTime.now(DateTimeZone.UTC);
        TimeInstant timeInstant = TimeInstant
                .now()
                .with(DateTimeZone.UTC)
                .with(Resolution.DAY);

        Node event1, event2, event3;
        long eventId1, eventId2, eventId3;
        try (Transaction tx = getDatabase().beginTx()) {
            event1 = getDatabase().createNode();
            event1.setProperty("name", "eventA");
            eventId1 = event1.getId();
            tx.success();

            event2 = getDatabase().createNode();
            event2.setProperty("name", "eventB");
            eventId2 = event2.getId();
            tx.success();

            event3 = getDatabase().createNode();
            event3.setProperty("name", "eventC");
            eventId3 = event3.getId();
            tx.success();
        }


        //When
        String eventJson1 = "{" +
                "        \"node\": {\"id\":" + eventId1 + "}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant.getTime() +
                "    }";

        String eventJson2 = "{" +
                "        \"node\": {\"id\":" + eventId2 + "}," +
                "        \"relationshipType\": \"AT_OTHER_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant.getTime() +
                "    }";


        String eventJson3 = "{" +
                "        \"node\": {\"id\":" + eventId3 + "}," +
                "        \"relationshipType\": \"AT_BAD_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant.getTime() +
                "    }";

        httpClient.post(getUrl() + "single/event", eventJson1, HttpStatus.SC_CREATED);
        httpClient.post(getUrl() + "single/event", eventJson2, HttpStatus.SC_CREATED);
        String postResult = httpClient.post(getUrl() + "single/event", eventJson3, HttpStatus.SC_CREATED);

        String getResult = httpClient.get(getUrl() + "single/" + timeInstant.getTime() + "/events?relationshipTypes=AT_TIME", HttpStatus.SC_OK);
        String getMultipleResult = httpClient.get(getUrl() + "single/" + timeInstant.getTime() + "/events?relationshipTypes=AT_TIME,AT_OTHER_TIME", HttpStatus.SC_OK);
        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:TimeTreeRoot)," +
                "(root)-[:FIRST]->(year:Year {value:" + now.getYear() + "})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:" + now.getMonthOfYear() + "})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:FIRST]->(day:Day {value:" + now.getDayOfMonth() + "})," +
                "(month)-[:CHILD]->(day)," +
                "(month)-[:LAST]->(day)," +
                "(day)<-[:AT_TIME]-(event {name:'eventA'})," +
                "(day)<-[:AT_OTHER_TIME]-(event2 {name:'eventB'})," +
                "(day)<-[:AT_BAD_TIME]-(event3 {name:'eventC'})");

        assertEquals("{\"id\":2,\"properties\":{\"name\":\"eventC\"},\"labels\":[]}", postResult);
        assertEquals("[{\"node\":{\"id\":0,\"properties\":{\"name\":\"eventA\"},\"labels\":[]},\"relationshipType\":\"AT_TIME\"}]", getResult);
        assertEquals("[{\"node\":{\"id\":0,\"properties\":{\"name\":\"eventA\"},\"labels\":[]},\"relationshipType\":\"AT_TIME\"}," +
                "{\"node\":{\"id\":1,\"properties\":{\"name\":\"eventB\"},\"labels\":[]},\"relationshipType\":\"AT_OTHER_TIME\"}]", getMultipleResult);
    }

    @Test
    public void eventAndTimeInstantShouldBeCreatedWhenEventIsCreatedFromScratch() throws IOException {
        //Given
        DateTime now = DateTime.now(DateTimeZone.UTC);
        TimeInstant timeInstant = TimeInstant
                .now()
                .with(DateTimeZone.UTC)
                .with(Resolution.DAY);

        //When
        String eventJson1 = "{" +
                "        \"node\": {\"labels\":[\"Event\",\"Email\"], \"properties\":{\"name\":\"eventA\"}}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant.getTime() +
                "    }";

        String eventJson2 = "{" +
                "        \"node\": {\"labels\":[\"Event\"], \"properties\":{\"name\":\"eventB\"}}," +
                "        \"relationshipType\": \"AT_OTHER_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant.getTime() +
                "    }";


        String eventJson3 = "{" +
                "        \"node\": {\"labels\":[], \"properties\":{\"name\":\"eventC\"}}," +
                "        \"relationshipType\": \"AT_BAD_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant.getTime() +
                "    }";

        String eventJson4 = "{" +
                "        \"node\": {\"properties\":{\"name\":\"eventD\",\"value\":1}}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant.getTime() +
                "    }";

        String postResult = httpClient.post(getUrl() + "single/event", eventJson1, HttpStatus.SC_CREATED);
        httpClient.post(getUrl() + "single/event", eventJson2, HttpStatus.SC_CREATED);
        httpClient.post(getUrl() + "single/event", eventJson3, HttpStatus.SC_CREATED);
        httpClient.post(getUrl() + "single/event", eventJson4, HttpStatus.SC_CREATED);

        String getResult = httpClient.get(getUrl() + "single/" + timeInstant.getTime() + "/events", HttpStatus.SC_OK);

        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:TimeTreeRoot)," +
                "(root)-[:FIRST]->(year:Year {value:" + now.getYear() + "})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:" + now.getMonthOfYear() + "})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:FIRST]->(day:Day {value:" + now.getDayOfMonth() + "})," +
                "(month)-[:CHILD]->(day)," +
                "(month)-[:LAST]->(day)," +
                "(day)<-[:AT_TIME]-(event:Event:Email {name:'eventA'})," +
                "(day)<-[:AT_TIME]-(event4 {name:'eventD',value:1})," +
                "(day)<-[:AT_OTHER_TIME]-(event2:Event {name:'eventB'})," +
                "(day)<-[:AT_BAD_TIME]-(event3 {name:'eventC'})");

        assertEquals("{\"id\":0,\"properties\":{\"name\":\"eventA\"},\"labels\":[\"Event\",\"Email\"]}", postResult);

        assertEquals("[{\"node\":{\"id\":7,\"properties\":{\"name\":\"eventD\",\"value\":1},\"labels\":[]},\"relationshipType\":\"AT_TIME\"}," +
                "{\"node\":{\"id\":0,\"properties\":{\"name\":\"eventA\"},\"labels\":[\"Event\",\"Email\"]},\"relationshipType\":\"AT_TIME\"}," +
                "{\"node\":{\"id\":5,\"properties\":{\"name\":\"eventB\"},\"labels\":[\"Event\"]},\"relationshipType\":\"AT_OTHER_TIME\"}," +
                "{\"node\":{\"id\":6,\"properties\":{\"name\":\"eventC\"},\"labels\":[]},\"relationshipType\":\"AT_BAD_TIME\"}]", getResult);
    }

    @Test
    public void eventShouldOnlyBeAttachedOnce() throws IOException {
        //Given
        DateTime now = DateTime.now(DateTimeZone.UTC);
        TimeInstant timeInstant = TimeInstant
                .now()
                .with(DateTimeZone.UTC)
                .with(Resolution.DAY);

        Node event;
        long eventId;
        try (Transaction tx = getDatabase().beginTx()) {
            event = getDatabase().createNode();
            event.setProperty("name", "eventA");
            eventId = event.getId();
            tx.success();
        }


        //When
        String eventJson = "{" +
                "        \"node\": {\"id\":" + eventId + "}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant.getTime() +
                "    }";

        String postResult1 = httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_CREATED);
        String postResult2 = httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_OK);
        httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_OK);

        String getResult = httpClient.get(getUrl() + "single/" + timeInstant.getTime() + "/events?relationshipType=AT_TIME", HttpStatus.SC_OK);

        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:TimeTreeRoot)," +
                "(root)-[:FIRST]->(year:Year {value:" + now.getYear() + "})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:" + now.getMonthOfYear() + "})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:FIRST]->(day:Day {value:" + now.getDayOfMonth() + "})," +
                "(month)-[:CHILD]->(day)," +
                "(month)-[:LAST]->(day)," +
                "(day)<-[:AT_TIME]-(event {name:'eventA'})");

        assertEquals("{\"id\":0,\"properties\":{\"name\":\"eventA\"},\"labels\":[]}", postResult1);
        assertEquals("{\"id\":0,\"properties\":{\"name\":\"eventA\"},\"labels\":[]}", postResult2);
        assertEquals("[{\"node\":{\"id\":0,\"properties\":{\"name\":\"eventA\"},\"labels\":[]},\"relationshipType\":\"AT_TIME\"}]", getResult);
    }


    @Test
    public void eventAndTimeInstantAtCustomRootShouldBeCreatedWhenEventIsAttached() {
        //Given
        DateTime now = DateTime.now(DateTimeZone.UTC);
        TimeInstant timeInstant = TimeInstant
                .now()
                .with(DateTimeZone.UTC)
                .with(Resolution.DAY);

        Node event;
        long eventId;
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().createNode(DynamicLabel.label("CustomRoot"));
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            event = getDatabase().createNode(DynamicLabel.label("Event"));
            event.setProperty("name", "eventA");
            eventId = event.getId();
            tx.success();
        }

        //When
        String eventJson = "{" +
                "        \"node\": {\"id\":" + eventId + "}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant.getTime() +
                "    }";

        String postResult1 = httpClient.post(getUrl() + "0/single/event", eventJson, HttpStatus.SC_CREATED);
        String postResult2 = httpClient.post(getUrl() + "0/single/event", eventJson, HttpStatus.SC_OK);

        String getResult = httpClient.get(getUrl() + "0/single/" + timeInstant.getTime() + "/events", HttpStatus.SC_OK);

        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:CustomRoot)," +
                "(root)-[:FIRST]->(year:Year {value:" + now.getYear() + "})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:" + now.getMonthOfYear() + "})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:FIRST]->(day:Day {value:" + now.getDayOfMonth() + "})," +
                "(month)-[:CHILD]->(day)," +
                "(month)-[:LAST]->(day)," +
                "(day)<-[:AT_TIME]-(event:Event {name:'eventA'})");

        assertEquals("{\"id\":1,\"properties\":{\"name\":\"eventA\"},\"labels\":[\"Event\"]}", postResult1);
        assertEquals("{\"id\":1,\"properties\":{\"name\":\"eventA\"},\"labels\":[\"Event\"]}", postResult2);
        assertEquals("[{\"node\":{\"id\":1,\"properties\":{\"name\":\"eventA\"},\"labels\":[\"Event\"]},\"relationshipType\":\"AT_TIME\"}]", getResult);
    }

    @Test
    public void eventAndTimeInstantAtCustomRootShouldBeCreatedWhenEventIsCreatedFromScratch() {
        //Given
        DateTime now = DateTime.now(DateTimeZone.UTC);
        TimeInstant timeInstant = TimeInstant
                .now()
                .with(DateTimeZone.UTC)
                .with(Resolution.DAY);

        Node event;
        long eventId;
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().createNode(DynamicLabel.label("CustomRoot"));
            tx.success();
        }

        //When
        String eventJson = "{" +
                "        \"node\": {\"labels\":[\"Event\"], \"properties\":{\"name\":\"eventA\"}}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant.getTime() +
                "    }";

        String postResult = httpClient.post(getUrl() + "0/single/event", eventJson, HttpStatus.SC_CREATED);

        String getResult = httpClient.get(getUrl() + "0/single/" + timeInstant.getTime() + "/events", HttpStatus.SC_OK);

        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:CustomRoot)," +
                "(root)-[:FIRST]->(year:Year {value:" + now.getYear() + "})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:" + now.getMonthOfYear() + "})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:FIRST]->(day:Day {value:" + now.getDayOfMonth() + "})," +
                "(month)-[:CHILD]->(day)," +
                "(month)-[:LAST]->(day)," +
                "(day)<-[:AT_TIME]-(event:Event {name:'eventA'})");

        assertEquals("{\"id\":1,\"properties\":{\"name\":\"eventA\"},\"labels\":[\"Event\"]}", postResult);
        assertEquals("[{\"node\":{\"id\":1,\"properties\":{\"name\":\"eventA\"},\"labels\":[\"Event\"]},\"relationshipType\":\"AT_TIME\"}]", getResult);
    }

    @Test
    public void multipleEventsAndTimeInstantsShouldBeCreatedWhenEventsAreAttached() {
        //Given
        TimeInstant timeInstant1 = TimeInstant.instant(dateToMillis(2012, 11, 1)).with(Resolution.DAY).with(DateTimeZone.UTC);
        TimeInstant timeInstant2 = TimeInstant.instant(dateToMillis(2012, 11, 3)).with(Resolution.DAY).with(DateTimeZone.UTC);

        Node event1, event2;

        long eventId1, eventId2;
        try (Transaction tx = getDatabase().beginTx()) {
            event1 = getDatabase().createNode();
            event1.setProperty("name", "eventA");
            eventId1 = event1.getId();

            event2 = getDatabase().createNode();
            event2.setProperty("name", "eventB");
            eventId2 = event2.getId();
            tx.success();
        }


        //When
        String eventJson1 = "{" +
                "        \"node\": {\"id\":" + eventId1 + "}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant1.getTime() +
                "    }";
        String eventJson2 = "{" +
                "        \"node\": {\"id\":" + eventId2 + "}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant2.getTime() +
                "    }";

        String postResult1 = httpClient.post(getUrl() + "single/event", eventJson1, HttpStatus.SC_CREATED);
        String postResult2 = httpClient.post(getUrl() + "single/event", eventJson2, HttpStatus.SC_CREATED);


        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:TimeTreeRoot)," +
                "(root)-[:FIRST]->(year:Year {value:2012})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:11})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:FIRST]->(day:Day {value:1})," +
                "(month)-[:CHILD]->(day)," +
                "(month)-[:CHILD]->(day2:Day {value:3})," +
                "(day)-[:NEXT]->(day2)," +
                "(month)-[:LAST]->(day2)," +
                "(day)<-[:AT_TIME]-(event1 {name:'eventA'})," +
                "(day2)<-[:AT_TIME]-(event2 {name:'eventB'})");

        String getResult = httpClient.get(getUrl() + "range/" + timeInstant1.getTime() + "/" + timeInstant2.getTime() + "/events", HttpStatus.SC_OK);

        assertEquals("{\"id\":0,\"properties\":{\"name\":\"eventA\"},\"labels\":[]}", postResult1);
        assertEquals("{\"id\":1,\"properties\":{\"name\":\"eventB\"},\"labels\":[]}", postResult2);
        assertEquals("[{\"node\":{\"id\":0,\"properties\":{\"name\":\"eventA\"},\"labels\":[]},\"relationshipType\":\"AT_TIME\"}," +
                "{\"node\":{\"id\":1,\"properties\":{\"name\":\"eventB\"},\"labels\":[]},\"relationshipType\":\"AT_TIME\"}]", getResult);
    }

    @Test
    public void multipleEventsAndTimeInstantsForCustomRootShouldBeCreatedWhenEventsAreAttached() {
        //Given
        TimeInstant timeInstant1 = TimeInstant.instant(dateToMillis(2012, 11, 1)).with(Resolution.DAY).with(DateTimeZone.UTC);
        TimeInstant timeInstant2 = TimeInstant.instant(dateToMillis(2012, 11, 3)).with(Resolution.DAY).with(DateTimeZone.UTC);

        Node event1, event2;

        long eventId1, eventId2;
        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().createNode(DynamicLabel.label("CustomRoot"));
            tx.success();
        }

        try (Transaction tx = getDatabase().beginTx()) {
            event1 = getDatabase().createNode();
            event1.setProperty("name", "eventA");
            eventId1 = event1.getId();

            event2 = getDatabase().createNode();
            event2.setProperty("name", "eventB");
            eventId2 = event2.getId();
            tx.success();
        }


        //When
        String eventJson1 = "{" +
                "        \"node\": {\"id\":" + eventId1 + "}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant1.getTime() +
                "    }";
        String eventJson2 = "{" +
                "        \"node\": {\"id\":" + eventId2 + "}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": " + timeInstant2.getTime() +
                "    }";

        String postResult1 = httpClient.post(getUrl() + "0/single/event", eventJson1, HttpStatus.SC_CREATED);
        String postResult2 = httpClient.post(getUrl() + "0/single/event", eventJson2, HttpStatus.SC_CREATED);


        //Then
        assertSameGraph(getDatabase(), "CREATE" +
                "(root:CustomRoot)," +
                "(root)-[:FIRST]->(year:Year {value:2012})," +
                "(root)-[:CHILD]->(year)," +
                "(root)-[:LAST]->(year)," +
                "(year)-[:FIRST]->(month:Month {value:11})," +
                "(year)-[:CHILD]->(month)," +
                "(year)-[:LAST]->(month)," +
                "(month)-[:FIRST]->(day:Day {value:1})," +
                "(month)-[:CHILD]->(day)," +
                "(month)-[:CHILD]->(day2:Day {value:3})," +
                "(day)-[:NEXT]->(day2)," +
                "(month)-[:LAST]->(day2)," +
                "(day)<-[:AT_TIME]-(event1 {name:'eventA'})," +
                "(day2)<-[:AT_TIME]-(event2 {name:'eventB'})");

        String getResult = httpClient.get(getUrl() + "0/range/" + timeInstant1.getTime() + "/" + timeInstant2.getTime() + "/events", HttpStatus.SC_OK);

        assertEquals("{\"id\":1,\"properties\":{\"name\":\"eventA\"},\"labels\":[]}", postResult1);
        assertEquals("{\"id\":2,\"properties\":{\"name\":\"eventB\"},\"labels\":[]}", postResult2);
        assertEquals("[{\"node\":{\"id\":1,\"properties\":{\"name\":\"eventA\"},\"labels\":[]},\"relationshipType\":\"AT_TIME\"}," +
                "{\"node\":{\"id\":2,\"properties\":{\"name\":\"eventB\"},\"labels\":[]},\"relationshipType\":\"AT_TIME\"}]", getResult);
    }

    @Test //issue https://github.com/graphaware/neo4j-timetree/issues/12
    public void shouldBeAbleToAttachEventsInARunningTx() {
        httpClient.post(baseNeoUrl() + "/db/data/transaction", "{\n" +
                "  \"statements\" : [ {\n" +
                "    \"statement\" : \"CREATE (e:Email {props}) RETURN id(e)\",\n" +
                "    \"parameters\" : {\n" +
                "      \"props\" : {\n" +
                "        \"subject\" : \"Neo4j\"\n" +
                "      }\n" +
                "    }\n" +
                "  } ]\n" +
                "}", HttpStatus.SC_CREATED);

        //When
        String eventJson1 = "{" +
                "        \"node\": {\"id\":0}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"DAY\"," +
                "        \"time\": 123343242132" +
                "    }";

        //we should parse out the tx id, but we know it's 1 on a new database:

        httpClient.post(getUrl() + "single/event", eventJson1, Collections.singletonMap("_GA_TX_ID", "1"), HttpStatus.SC_CREATED);

        httpClient.post(baseNeoUrl() + "/db/data/transaction/1/commit", HttpStatus.SC_OK);

        GraphUnit.assertSameGraph(getDatabase(), "CREATE " +
                "(e:Email {subject: 'Neo4j'})," +
                "(r:TimeTreeRoot)," +
                "(y:Year {value: 1973})," +
                "(m:Month {value: 11})," +
                "(d:Day {value: 28})," +
                "(r)-[:CHILD]->(y)," +
                "(r)-[:FIRST]->(y)," +
                "(r)-[:LAST]->(y)," +
                "(y)-[:CHILD]->(m)," +
                "(y)-[:FIRST]->(m)," +
                "(y)-[:LAST]->(m)," +
                "(m)-[:CHILD]->(d)," +
                "(m)-[:FIRST]->(d)," +
                "(m)-[:LAST]->(d)," +
                "(e)-[:AT_TIME]->(d)");
    }

    @Test //issue https://github.com/graphaware/neo4j-timetree/issues/41
    public void shouldGetEventsInRangeWithEqualOrLowerResolution() {
        String eventJson = "{" +
                "        \"node\": {\"labels\":[\"Event\"]}," +
                "        \"relationshipType\": \"AT_TIME\"," +
                "        \"timezone\": \"UTC\"," +
                "        \"resolution\": \"HOUR\"," +
                "        \"time\": 123343242132" +
                "    }";

        httpClient.post(getUrl() + "single/event", eventJson, HttpStatus.SC_CREATED);

        assertEquals("[{\"node\":{\"id\":0,\"labels\":[\"Event\"]},\"relationshipType\":\"AT_TIME\"}]", httpClient.get(getUrl() + "range/122343242132/124343242132/events", HttpStatus.SC_OK));
        assertEquals("[]", httpClient.get(getUrl() + "range/122343242132/124343242132/events?resolution=millisecond", HttpStatus.SC_OK));
        assertEquals("[]", httpClient.get(getUrl() + "range/122343242132/124343242132/events?resolution=second", HttpStatus.SC_OK));
        assertEquals("[]", httpClient.get(getUrl() + "range/122343242132/124343242132/events?resolution=minute", HttpStatus.SC_OK));
        assertEquals("[{\"node\":{\"id\":0,\"labels\":[\"Event\"]},\"relationshipType\":\"AT_TIME\"}]", httpClient.get(getUrl() + "range/122343242132/124343242132/events?resolution=hour", HttpStatus.SC_OK));
        assertEquals("[{\"node\":{\"id\":0,\"labels\":[\"Event\"]},\"relationshipType\":\"AT_TIME\"}]", httpClient.get(getUrl() + "range/122343242132/124343242132/events?resolution=day", HttpStatus.SC_OK));
        assertEquals("[{\"node\":{\"id\":0,\"labels\":[\"Event\"]},\"relationshipType\":\"AT_TIME\"}]", httpClient.get(getUrl() + "range/122343242132/124343242132/events?resolution=month", HttpStatus.SC_OK));
        assertEquals("[{\"node\":{\"id\":0,\"labels\":[\"Event\"]},\"relationshipType\":\"AT_TIME\"}]", httpClient.get(getUrl() + "range/122343242132/124343242132/events?resolution=year", HttpStatus.SC_OK));
    }

    @Test //issue https://github.com/graphaware/neo4j-timetree/issues/41
    public void shouldGetEventsDownToSeconds() {
        String Cypher = "create (_294:`User` {`uid`:\"cib5wl02w00023l5kl0k65q2t\", `username`:\"Brett\"})\n" +
                "create (_295:`User` {`uid`:\"cib5wlv1k00033l5kva7fmfba\", `username`:\"Julie\"})\n" +
                "create (_296:`User` {`uid`:\"cib5wmki400043l5kkmzcjx5s\", `username`:\"Marc\"})\n" +
                "create (_304:`TimeTreeRoot`)\n" +
                "create (_305:`Item` {`content`:\"sdfasdf\", `created`:1439380520793, `modified`:1439380521090, `name`:\"Test\", `uid`:\"cid8pzxrx00053k5lmlz3661j\"})\n" +
                "create (_306:`Year` {`value`:2015})\n" +
                "create (_307:`Month` {`value`:8})\n" +
                "create (_308:`Day` {`value`:12})\n" +
                "create (_309:`Hour` {`value`:22})\n" +
                "create (_310:`Minute` {`value`:55})\n" +
                "create (_311:`Second` {`value`:20})\n" +
                "create (_312:`Second` {`value`:21})\n" +
                "create _304-[:`CHILD`]->_306\n" +
                "create _304-[:`FIRST`]->_306\n" +
                "create _304-[:`LAST`]->_306\n" +
                "create _305-[:`ofType`]->_220\n" +
                "create _305-[:`Created`]->_311\n" +
                "create _305-[:`Modified`]->_312\n" +
                "create _306-[:`CHILD`]->_307\n" +
                "create _306-[:`FIRST`]->_307\n" +
                "create _306-[:`LAST`]->_307\n" +
                "create _307-[:`CHILD`]->_308\n" +
                "create _307-[:`FIRST`]->_308\n" +
                "create _307-[:`LAST`]->_308\n" +
                "create _308-[:`CHILD`]->_309\n" +
                "create _308-[:`FIRST`]->_309\n" +
                "create _308-[:`LAST`]->_309\n" +
                "create _309-[:`CHILD`]->_310\n" +
                "create _309-[:`FIRST`]->_310\n" +
                "create _309-[:`LAST`]->_310\n" +
                "create _310-[:`CHILD`]->_312\n" +
                "create _310-[:`CHILD`]->_311\n" +
                "create _310-[:`FIRST`]->_311\n" +
                "create _310-[:`LAST`]->_312\n" +
                "create _311-[:`NEXT`]->_312";

        try (Transaction tx = getDatabase().beginTx()) {
            getDatabase().execute(Cypher);
            tx.success();
        }
        String expected = "[{\"node\":{\"id\":4,\"properties\":{\"content\":\"sdfasdf\",\"uid\":\"cid8pzxrx00053k5lmlz3661j\",\"created\":1439380520793,\"name\":\"Test\",\"modified\":1439380521090},\"labels\":[\"Item\"]},\"relationshipType\":\"Created\"},{\"node\":{\"id\":4,\"properties\":{\"content\":\"sdfasdf\",\"uid\":\"cid8pzxrx00053k5lmlz3661j\",\"created\":1439380520793,\"name\":\"Test\",\"modified\":1439380521090},\"labels\":[\"Item\"]},\"relationshipType\":\"Modified\"}]";
        String responseWithDayResolution = httpClient.get(getUrl() + "range/1439380520593/1439380521290/events?resolution=day", HttpStatus.SC_OK);
        assertEquals(expected, responseWithDayResolution);

        String responseWithSecondResolution = httpClient.get(getUrl() + "range/1439380520593/1439380521290/events?resolution=second", HttpStatus.SC_OK);
        assertEquals(expected, responseWithSecondResolution);

    }

    private long dateToMillis(int year, int month, int day) {
        return dateToDateTime(year, month, day).getMillis();
    }

    private DateTime dateToDateTime(int year, int month, int day) {
        return new DateTime(year, month, day, 0, 0, DateTimeZone.UTC);
    }

    private String getUrl() {
        return baseUrl() + "/timetree/";
    }
}
