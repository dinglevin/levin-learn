package seda.sandstorm.timer;

import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.EventSink;

class TimerNode implements TimerHandle {
    private final long happening;
    private final EventElement event;
    private final EventSink queue;
    private volatile TimerNode next;
    private volatile TimerNode prev;

    public TimerNode(long happening, EventElement event, EventSink queue) {
        this.happening = happening;
        this.event = event;
        this.queue = queue;
        this.next = this.prev = null;
    }
    
    public void eventReady() {
        queue.enqueue(event);
    }

    @Override
    public long getHappening() {
        return happening;
    }

    @Override
    public EventSink getEventSink() {
        return queue;
    }

    @Override
    public EventElement getEvent() {
        return event;
    }
    
    @Override
    public boolean isActive() {
        return prev == null && next == null;
    }
    
    public void setNext(TimerNode next) {
        this.next = next;
        if (next != null) {
            next.prev = this;
        }
    }
    
    public TimerNode getNext() {
        return next;
    }
    
    public void setPrev(TimerNode prev) {
        this.prev = prev;
        if (prev != null) {
            prev.next = this;
        }
    }
    
    public TimerNode getPrev() {
        return prev;
    }
    
    public void unlink() {
        this.prev = null;
        this.next = null;
    }
    
    public String toString() {
        return "TimerNode<" + hashCode() + ">";
    }
}
