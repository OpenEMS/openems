package io.openems.backend.uiwebsocket.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class OnRequestTest {

    private static class DummyUiWebsocketImpl extends UiWebsocketImpl {
        private final Set<String> removed = new HashSet<>();
        private final Set<String> added = new HashSet<>();

        @Override
        public void removeEdgeSubscriptions(WsData wsData) {
            this.removed.addAll(wsData.getSubscribedEdges());
        }

        @Override
        public void removeEdgeSubscriptions(WsData wsData, Set<String> edgeIds) {
            this.removed.addAll(edgeIds);
        }

        @Override
        public void addEdgeSubscriptions(WsData wsData, Set<String> edgeIds) {
            this.added.addAll(edgeIds);
        }
    }

    @Test
    public void updateEdgeSubscriptionsTest_Update() {
        var parent = new DummyUiWebsocketImpl();
        var sut = new OnRequest(parent);
        var wsData = new WsData(null, 10);
        wsData.addSubscribedEdges(Set.of("edge0", "edge1", "edge2"));

        sut.updateEdgeSubscriptions(wsData, Set.of("edge1", "edge3"));

        assertEquals(Set.of("edge0", "edge2"), parent.removed);
        assertEquals(Set.of("edge3"), parent.added);

        assertEquals(Set.of("edge1", "edge3"), wsData.getSubscribedEdges());
    }

    @Test
    public void updateEdgeSubscriptionsTest_Add() {
        var parent = new DummyUiWebsocketImpl();
        var sut = new OnRequest(parent);
        var wsData = new WsData(null, 10);

        sut.updateEdgeSubscriptions(wsData, Set.of("edge1", "edge3"));

        assertEquals(Set.of(), parent.removed);
        assertEquals(Set.of("edge1", "edge3"), parent.added);

        assertEquals(Set.of("edge1", "edge3"), wsData.getSubscribedEdges());
    }

    @Test
    public void updateEdgeSubscriptionsTest_Clear() {
        var parent = new DummyUiWebsocketImpl();
        var sut = new OnRequest(parent);
        var wsData = new WsData(null, 10);
        wsData.addSubscribedEdges(Set.of("edge0", "edge1", "edge2"));

        sut.updateEdgeSubscriptions(wsData, Set.of());

        assertEquals(Set.of("edge0", "edge1", "edge2"), parent.removed);
        assertEquals(Set.of(), parent.added);

        assertEquals(Set.of(), wsData.getSubscribedEdges());
    }
}