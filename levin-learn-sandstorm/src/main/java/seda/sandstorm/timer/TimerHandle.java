package seda.sandstorm.timer;

import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.EventSink;

public interface TimerHandle {
    public long getHappening();
    public EventSink getEventSink();
    public EventElement getEvent();
    public boolean isActive();
}
